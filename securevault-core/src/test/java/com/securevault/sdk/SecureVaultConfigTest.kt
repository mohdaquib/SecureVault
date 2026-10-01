package com.securevault.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SecureVaultConfigTest {
    @Test
    fun acceptsDocumentedNamespace() {
        val config = SecureVaultConfig(namespace = "customer-data")

        assertEquals("customer-data", config.namespace)
        assertEquals(SecureVaultSecurityLevelPolicy.ALLOW_SOFTWARE, config.securityLevelPolicy)
    }

    @Test
    fun acceptsExplicitSecurityLevelPolicy() {
        val config = SecureVaultConfig(
            namespace = "customer-data",
            securityLevelPolicy = SecureVaultSecurityLevelPolicy.REQUIRE_STRONGBOX,
        )

        assertEquals(SecureVaultSecurityLevelPolicy.REQUIRE_STRONGBOX, config.securityLevelPolicy)
        assertEquals(SecureVaultSecurityLevel.STRONGBOX, config.securityLevelPolicy.preferredLevel)
        assertEquals(true, config.securityLevelPolicy.isRequired)
    }

    @Test
    fun keepsOneArgumentJvmConstructorWithSoftwareAllowedDefault() {
        val constructor = SecureVaultConfig::class.java.getConstructor(String::class.java)

        val config = constructor.newInstance("java-customer")

        assertEquals("java-customer", config.namespace)
        assertEquals(SecureVaultSecurityLevelPolicy.ALLOW_SOFTWARE, config.securityLevelPolicy)
    }

    @Test
    fun rejectsEmptyNamespace() {
        assertInvalidNamespace("")
    }

    @Test
    fun rejectsWhitespaceOnlyNamespace() {
        assertInvalidNamespace("   ")
    }

    @Test
    fun rejectsNamespaceStartingWithSeparator() {
        assertInvalidNamespace(".customer-data")
    }

    @Test
    fun rejectsNamespaceLongerThanSixtyFourCharacters() {
        assertInvalidNamespace("a".repeat(65))
    }

    private fun assertInvalidNamespace(namespace: String) {
        val error = assertThrows(InvalidSecureVaultConfigurationException::class.java) {
            SecureVaultConfig(namespace)
        }

        assertEquals("namespace", error.field)
    }
}
