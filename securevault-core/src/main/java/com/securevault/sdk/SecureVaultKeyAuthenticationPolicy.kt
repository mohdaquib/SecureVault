package com.securevault.sdk

/** Authenticators that may authorize use of an authentication-protected vault key. */
public enum class SecureVaultAllowedAuthenticators {
    /** Require a strong biometric; a PIN, pattern, or password is not accepted. */
    BIOMETRIC_ONLY,

    /** Accept either a strong biometric or the device PIN, pattern, or password. */
    BIOMETRIC_OR_DEVICE_CREDENTIAL,
}

/**
 * Controls whether Android Keystore authorizes use of a vault key.
 *
 * Authentication is attached to the key itself. Passing an application screen does not authorize
 * cryptographic operations unless Android Keystore also accepts the configured authentication.
 */
public sealed class SecureVaultKeyAuthenticationPolicy {
    /** Key operations do not require user authentication. */
    public object None : SecureVaultKeyAuthenticationPolicy()

    /** Every key operation requires a fresh authentication. */
    public data class EveryOperation(
        public val allowedAuthenticators: SecureVaultAllowedAuthenticators,
    ) : SecureVaultKeyAuthenticationPolicy()

    /** Authentication authorizes key operations for [validityDurationSeconds]. */
    public data class ValidFor(
        public val validityDurationSeconds: Int,
        public val allowedAuthenticators: SecureVaultAllowedAuthenticators,
    ) : SecureVaultKeyAuthenticationPolicy() {
        init {
            require(validityDurationSeconds in 1..MAX_VALIDITY_DURATION_SECONDS) {
                "validityDurationSeconds must be between 1 and $MAX_VALIDITY_DURATION_SECONDS"
            }
        }
    }

    private companion object {
        private const val MAX_VALIDITY_DURATION_SECONDS: Int = 86_400
    }
}
