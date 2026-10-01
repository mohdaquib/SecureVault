package com.securevault.sdk

/**
 * Controls the requested and minimum acceptable protection for a vault key.
 *
 * A preference may fall back to a lower level. A requirement fails with
 * [SecureVaultCryptoFailure.UNSUPPORTED_HARDWARE] rather than weakening the requirement.
 */
public enum class SecureVaultSecurityLevelPolicy(
    public val preferredLevel: SecureVaultSecurityLevel,
    public val isRequired: Boolean,
) {
    /** Accept software-backed keys. This is the compatibility default. */
    ALLOW_SOFTWARE(SecureVaultSecurityLevel.SOFTWARE, false),

    /** Prefer hardware isolation, but accept a software-backed key. */
    PREFER_TRUSTED_EXECUTION_ENVIRONMENT(
        SecureVaultSecurityLevel.TRUSTED_EXECUTION_ENVIRONMENT,
        false,
    ),

    /** Reject keys that Android does not identify as TEE- or StrongBox-backed. */
    REQUIRE_TRUSTED_EXECUTION_ENVIRONMENT(
        SecureVaultSecurityLevel.TRUSTED_EXECUTION_ENVIRONMENT,
        true,
    ),

    /** Request StrongBox, explicitly falling back when StrongBox is unavailable. */
    PREFER_STRONGBOX(SecureVaultSecurityLevel.STRONGBOX, false),

    /** Require StrongBox; unsupported devices fail instead of falling back. */
    REQUIRE_STRONGBOX(SecureVaultSecurityLevel.STRONGBOX, true),
}

