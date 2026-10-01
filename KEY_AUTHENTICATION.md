# Key authentication policies

SecureVault attaches authentication requirements to the Android Keystore key. An unlocked screen,
an application login, or access to the application UI does not bypass the key policy: encryption
and decryption succeed only when Android Keystore considers the configured authentication valid.

The default preserves existing behavior:

```kotlin
SecureVaultConfig(
    namespace = "customer-data",
    keyAuthenticationPolicy = SecureVaultKeyAuthenticationPolicy.None,
)
```

Require a strong biometric for every key operation:

```kotlin
SecureVaultConfig(
    namespace = "customer-data",
    keyAuthenticationPolicy = SecureVaultKeyAuthenticationPolicy.EveryOperation(
        SecureVaultAllowedAuthenticators.BIOMETRIC_ONLY,
    ),
)
```

Allow a strong biometric or the device PIN, pattern, or password, valid for 60 seconds:

```kotlin
SecureVaultConfig(
    namespace = "customer-data",
    keyAuthenticationPolicy = SecureVaultKeyAuthenticationPolicy.ValidFor(
        validityDurationSeconds = 60,
        allowedAuthenticators =
            SecureVaultAllowedAuthenticators.BIOMETRIC_OR_DEVICE_CREDENTIAL,
    ),
)
```

Validity periods must be between 1 second and 24 hours. On API 30 and later, every-operation and
timed policies support both authenticator choices. On API 24–29, Android can faithfully enforce
only every-operation biometric-only authentication or a timed biometric/device-credential window.
Other combinations fail with `UNSUPPORTED_AUTHENTICATION_POLICY`; they are never weakened.

If an existing key was created with a different authentication policy, it remains intact and use
fails with `UNSUPPORTED_AUTHENTICATION_POLICY`. SecureVault does not silently replace it.

## Prompt outcomes

Android Keystore reports `AUTHENTICATION_REQUIRED` when key use is not currently authorized, and
`AUTHENTICATION_EXPIRED` when a previously observed timed authorization no longer works. The host
application owns its biometric or credential prompt and should translate its terminal callbacks to:

- `AUTHENTICATION_CANCELLED` for user cancellation, a negative button, or system cancellation.
- `AUTHENTICATION_LOCKED_OUT` for temporary or permanent biometric lockout.

Cancellation and lockout do not delete keys or encrypted data. A temporary lockout can be retried
when Android permits it. A permanent lockout generally requires the device credential before the
application attempts biometric authentication again.

## Device security changes

Biometric-only keys are invalidated when Android reports that biometric enrollment changed.
Authentication-protected keys can also be invalidated if the secure lock screen is disabled or its
credential is reset. Android reports these cases as `KeyPermanentlyInvalidatedException`, which
SecureVault maps to `KEY_PERMANENTLY_INVALIDATED`. Re-authentication cannot repair that key;
encrypted data is preserved and reset or recovery must be an explicit customer decision.
