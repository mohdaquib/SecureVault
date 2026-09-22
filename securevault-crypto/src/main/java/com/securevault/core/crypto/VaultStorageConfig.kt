package com.securevault.core.crypto

import com.securevault.sdk.SecureVaultConfig

/** Storage identity shared by the key, passphrase and database layers. */
class VaultStorageConfig private constructor(private val config: SecureVaultConfig?) {
    constructor(namespace: String) : this(SecureVaultConfig(namespace))

    val namespace: String? get() = config?.namespace

    // Fixed-width ASCII hex is injective and safe even on case-insensitive filesystems.
    private val prefix: String? = config?.namespace?.let { namespace ->
        "securevault_v1_" + namespace.map { it.code.toString(16).padStart(2, '0') }.joinToString("")
    }

    val keyAlias: String = prefix?.let { "${it}_db_key" } ?: "securevault_db_key"
    val preferencesName: String = prefix?.let { "${it}_crypto" } ?: "securevault_crypto"
    val databaseName: String = prefix?.let { "$it.db" } ?: "securevault.db"

    companion object {
        /** Explicit compatibility mode for the original single-vault demo; never a default. */
        fun legacyDemo(): VaultStorageConfig = VaultStorageConfig(null)
    }
}
