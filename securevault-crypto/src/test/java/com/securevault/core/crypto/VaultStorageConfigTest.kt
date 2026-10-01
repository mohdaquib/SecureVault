package com.securevault.core.crypto

import com.securevault.sdk.InvalidSecureVaultConfigurationException
import com.securevault.sdk.SecureVaultConfig
import com.securevault.sdk.SecureVaultSecurityLevelPolicy
import org.junit.Assert.*
import org.junit.Test

class VaultStorageConfigTest {
    @Test fun namesAreStableCaseSensitiveAndDisjointFromLegacy() {
        val a = VaultStorageConfig("A")
        assertEquals("securevault_v1_41_db_key", a.keyAlias)
        assertEquals("securevault_v1_41_crypto", a.preferencesName)
        assertEquals("securevault_v1_41.db", a.databaseName)
        val configs = listOf(a, VaultStorageConfig("a"), VaultStorageConfig("a.b"),
            VaultStorageConfig("a-b"), VaultStorageConfig("a_b"), VaultStorageConfig.legacyDemo())
        assertEquals(configs.size, configs.map { it.keyAlias }.toSet().size)
        assertEquals(configs.size, configs.map { it.preferencesName.lowercase() }.toSet().size)
        assertEquals(configs.size, configs.map { it.databaseName.lowercase() }.toSet().size)
        assertEquals(a.keyAlias, VaultStorageConfig("A").keyAlias)
        assertTrue(VaultStorageConfig("a".repeat(64)).databaseName.length < 255)
    }

    @Test fun invalidNamespacesFailBeforeStorageIsOpened() {
        for (namespace in listOf("", " ", ".", "..", "../a", "a/b", "a\\b", "a b", "a\n", "é", "a".repeat(65))) {
            val error = assertThrows(InvalidSecureVaultConfigurationException::class.java) {
                VaultStorageConfig(namespace)
            }
            assertEquals("namespace", error.field)
        }
    }

    @Test fun legacyIsExplicitAndMatchesOriginalNames() {
        val legacy = VaultStorageConfig.legacyDemo()
        assertNull(legacy.namespace)
        assertEquals("securevault_db_key", legacy.keyAlias)
        assertEquals("securevault_crypto", legacy.preferencesName)
        assertEquals("securevault.db", legacy.databaseName)
        assertEquals(SecureVaultSecurityLevelPolicy.ALLOW_SOFTWARE, legacy.securityLevelPolicy)
    }

    @Test fun carriesCustomerSecurityPolicyToKeyGeneration() {
        val config = VaultStorageConfig(SecureVaultConfig(
            namespace = "hardware-policy",
            securityLevelPolicy = SecureVaultSecurityLevelPolicy.REQUIRE_STRONGBOX,
        ))

        assertEquals(SecureVaultSecurityLevelPolicy.REQUIRE_STRONGBOX, config.securityLevelPolicy)
    }
}
