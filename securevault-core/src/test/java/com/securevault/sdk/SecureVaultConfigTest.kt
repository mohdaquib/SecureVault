package com.securevault.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SecureVaultConfigTest {
    @Test
    fun acceptsDocumentedNamespace() {
        val config = SecureVaultConfig(namespace = "customer-data")

        assertEquals("customer-data", config.namespace)
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
