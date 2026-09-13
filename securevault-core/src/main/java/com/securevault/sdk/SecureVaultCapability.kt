package com.securevault.sdk

/** Stable identifiers for independently available SecureVault features. */
public enum class SecureVaultCapability(
    public val id: String,
) {
    KEY_MANAGEMENT("key-management"),
    ENCRYPTED_STORAGE("encrypted-storage"),
    NETWORK_SECURITY("network-security"),
    AUDIT_EVIDENCE("audit-evidence"),
}
