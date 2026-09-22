# Cryptographic failure contract

Catch `com.securevault.sdk.SecureVaultCryptoException` and branch on its `failure`
(`SecureVaultCryptoFailure`). The crypto module exports its dependency on core so
these SDK-owned types are visible to consumers. Do not match exception text or
Android/provider classes. This replaces the old generic crypto exceptions in the
implementation module. The customer facade still has no attached capabilities;
crypto operations currently originate in the crypto module and demo data factory.

| Failure | Meaning and safe response |
| --- | --- |
| MISSING_KEY | Read-only lookup found no key. Preserve encrypted data; offer a supported restore flow or an explicitly confirmed reset. A new key cannot decrypt old data. |
| KEY_PERMANENTLY_INVALIDATED | Android explicitly reports permanent invalidation. Re-authentication will not repair it. Preserve data and explain recovery/reset options. |
| AUTHENTICATION_REQUIRED | Android requires authentication, with no evidence of a previously successful timed authorization in this session. Request the authentication allowed by the key policy, then retry the operation. |
| AUTHENTICATION_EXPIRED | A timed-authentication key successfully completed a crypto operation through this manager, and Android subsequently rejected its authentication. Ask the user to authenticate again, then retry. |
| CORRUPT_CIPHERTEXT | Missing/invalid metadata, malformed encoding or lengths, or failed GCM integrity verification. Preserve data; offer restore/support. A wrong key also fails integrity verification and cannot be distinguished from tampering. |
| UNSUPPORTED_HARDWARE | An explicit StrongBox-unavailable error or Android's unsupported KeyMint feature code. Explain the capability requirement. Never silently weaken security or generate fallback keys. |
| KEYSTORE_UNAVAILABLE | Keystore could not be loaded, the provider is absent, or Android reports an uninitialized/transient service condition. Preserve storage; allow a bounded later retry after device unlock/setup or service recovery. This classification does not promise retry will succeed. |
| UNEXPECTED_PROVIDER_FAILURE | An unclassified provider, key-metadata, generation, or storage failure. Stop; report only the stable reason and seek support. Never reset automatically. |

A minimal customer handler can select a UI action without inspecting sensitive data:

```kotlin
try {
    // Perform the vault operation.
} catch (error: SecureVaultCryptoException) {
    when (error.failure) {
        SecureVaultCryptoFailure.AUTHENTICATION_REQUIRED,
        SecureVaultCryptoFailure.AUTHENTICATION_EXPIRED -> showAuthenticationPrompt()
        SecureVaultCryptoFailure.KEYSTORE_UNAVAILABLE -> showRetryLater()
        SecureVaultCryptoFailure.UNSUPPORTED_HARDWARE -> showUnsupportedDevice()
        else -> showRecoveryHelpWithoutDeletingData()
    }
}
```

These UI functions are application placeholders; recovery/reset is never automatic.

## Authentication distinction

Android's UserNotAuthenticatedException does not identify whether authentication
never happened or is no longer valid. The implementation uses KeyInfo to recognize
a timed policy and records successful crypto use within the manager's lifetime.
Without that evidence (including after process restart), it reports REQUIRED.
Per-operation authentication also reports REQUIRED. EXPIRED means a previously
accepted timed authorization no longer works; it does not prove a precise timeout.
Key validity expiry (`KeyExpiredException`) and operation expiry are not mislabeled
as authentication expiry. Existing generated demo keys do not require authentication;
this change does not enable biometrics or alter their key policy.

StrongBox errors are checked only on API 28+, and public numeric Keystore errors on
API 33+. Older versions use concrete exception types, otherwise the unknown reason.
Unknown numeric errors, generic InvalidKeyException/UnrecoverableKeyException and
provider message strings are never treated as proof that a key is missing.

## Data-preservation and disclosure guarantees

`getSecretKey()` is lookup-only. `getOrCreateSecretKey()` may create only after a
successful lookup reports absence; exceptions cannot reach its creation branch.
The passphrase decrypt path calls lookup-only. Existing ciphertext is not rewritten
on failures. Missing preferences alongside an existing database fail instead of
creating a new passphrase. Deletion remains an explicit separate method and is never
called by an error handler. Same-process locking is retained; multiprocess access
remains unsupported.

Mapped exceptions contain a fixed message and enum only. Provider causes, suppressed
exceptions, messages, aliases, ciphertext and plaintext are not attached or logged.
Cancellation and fatal VM errors are not converted into crypto failures. Applications
must also avoid logging secret arguments or returned passphrases themselves.

## Review and validation

Regression tests cover each failure reason, nested exceptions and redaction,
authentication evidence, missing/failed lookup without creation, corrupt preferences,
lost metadata with an existing database, and unchanged ciphertext after failures.
No app execution, compilation, test run or Gradle task was performed for this change.
The two new public types were added to the API baseline by hand; validate with
`:securevault-core:apiCheck` and run crypto/core tests when compilation is permitted.

Platform references:
- https://developer.android.com/reference/android/security/keystore/UserNotAuthenticatedException
- https://developer.android.com/reference/android/security/keystore/KeyPermanentlyInvalidatedException
- https://developer.android.com/reference/android/security/keystore/StrongBoxUnavailableException
- https://developer.android.com/reference/android/security/KeyStoreException
