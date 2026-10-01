package com.securevault.sdk

/**
 * The protection Android reports for key material used by a vault.
 *
 * This is observed per key. It is not a claim that the device, Android Keystore, or every vault
 * key provides hardware protection.
 */
public enum class SecureVaultSecurityLevel {
    /** Key operations are implemented in software. */
    SOFTWARE,

    /** Key operations are isolated in a trusted execution environment. */
    TRUSTED_EXECUTION_ENVIRONMENT,

    /** Key operations are isolated in Android StrongBox. */
    STRONGBOX,

    /** The platform cannot reliably identify the key's protection level. */
    UNKNOWN_OR_UNAVAILABLE,
}

