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
        assertEquals(SecureVaultKeyAuthenticationPolicy.None, config.keyAuthenticationPolicy)
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
        assertEquals(SecureVaultKeyAuthenticationPolicy.None, config.keyAuthenticationPolicy)
    }

    @Test
    fun acceptsEveryOperationAndTimedAuthenticationPolicies() {
        val everyUse = SecureVaultKeyAuthenticationPolicy.EveryOperation(
            SecureVaultAllowedAuthenticators.BIOMETRIC_ONLY,
        )
        val timed = SecureVaultKeyAuthenticationPolicy.ValidFor(
            validityDurationSeconds = 60,
            allowedAuthenticators =
                SecureVaultAllowedAuthenticators.BIOMETRIC_OR_DEVICE_CREDENTIAL,
        )

        assertEquals(
            everyUse,
            SecureVaultConfig("per-use", keyAuthenticationPolicy = everyUse)
                .keyAuthenticationPolicy,
        )
        assertEquals(
            timed,
            SecureVaultConfig("timed", keyAuthenticationPolicy = timed)
                .keyAuthenticationPolicy,
        )
    }

    @Test
    fun rejectsInvalidAuthenticationValidityPeriods() {
        for (seconds in listOf(0, -1, 86_401)) {
            assertThrows(IllegalArgumentException::class.java) {
                SecureVaultKeyAuthenticationPolicy.ValidFor(
                    seconds,
                    SecureVaultAllowedAuthenticators.BIOMETRIC_ONLY,
                )
            }
        }
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
