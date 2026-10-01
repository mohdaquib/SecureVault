package com.securevault.sdk

/** Stable reasons for cryptographic failures. Never infer recovery from provider message text. */
public enum class SecureVaultCryptoFailure {
    MISSING_KEY,
    KEY_PERMANENTLY_INVALIDATED,
    AUTHENTICATION_REQUIRED,
    AUTHENTICATION_EXPIRED,
    AUTHENTICATION_CANCELLED,
    AUTHENTICATION_LOCKED_OUT,
    UNSUPPORTED_AUTHENTICATION_POLICY,
    CORRUPT_CIPHERTEXT,
    UNSUPPORTED_HARDWARE,
    KEYSTORE_UNAVAILABLE,
    UNEXPECTED_PROVIDER_FAILURE,
}

/** Sanitized failure with no provider cause, key alias, ciphertext, or plaintext attached. */
public class SecureVaultCryptoException(
    public val failure: SecureVaultCryptoFailure,
) : SecureVaultException(
    when (failure) {
        SecureVaultCryptoFailure.MISSING_KEY -> "The vault key is missing"
        SecureVaultCryptoFailure.KEY_PERMANENTLY_INVALIDATED -> "The vault key is permanently invalidated"
        SecureVaultCryptoFailure.AUTHENTICATION_REQUIRED -> "User authentication is required"
        SecureVaultCryptoFailure.AUTHENTICATION_EXPIRED -> "Previous user authentication is no longer valid"
        SecureVaultCryptoFailure.AUTHENTICATION_CANCELLED -> "User authentication was cancelled"
        SecureVaultCryptoFailure.AUTHENTICATION_LOCKED_OUT -> "User authentication is locked out"
        SecureVaultCryptoFailure.UNSUPPORTED_AUTHENTICATION_POLICY ->
            "The requested key authentication policy is unsupported on this device"
        SecureVaultCryptoFailure.CORRUPT_CIPHERTEXT -> "Stored vault data failed integrity validation"
        SecureVaultCryptoFailure.UNSUPPORTED_HARDWARE -> "The requested cryptographic hardware capability is unavailable"
        SecureVaultCryptoFailure.KEYSTORE_UNAVAILABLE -> "Android Keystore is unavailable"
        SecureVaultCryptoFailure.UNEXPECTED_PROVIDER_FAILURE -> "An unexpected cryptographic provider or storage failure occurred"
    },
)
