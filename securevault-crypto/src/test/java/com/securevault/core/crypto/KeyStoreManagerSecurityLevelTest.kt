package com.securevault.core.crypto

import com.securevault.sdk.SecureVaultCryptoException
import com.securevault.sdk.SecureVaultCryptoFailure
import com.securevault.sdk.SecureVaultSecurityLevel
import com.securevault.sdk.SecureVaultSecurityLevelPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test
import javax.crypto.spec.SecretKeySpec

class KeyStoreManagerSecurityLevelTest {
    private class Backend(
        var stored: StoredKey? = null,
        private val createdLevel: SecureVaultSecurityLevel = SecureVaultSecurityLevel.SOFTWARE,
    ) : KeyStoreBackend {
        var deletes: Int = 0
        override fun find(): StoredKey? = stored
        override fun create(): StoredKey = StoredKey(
            key = SecretKeySpec(ByteArray(32), "AES"),
            securityLevel = createdLevel,
        ).also { stored = it }
        override fun delete() {
            deletes++
            stored = null
        }
    }

    @Test fun reportsSoftwareTeeStrongBoxAndUnknown() {
        for (level in SecureVaultSecurityLevel.entries) {
            val backend = Backend(stored = stored(level))
            val manager = manager(SecureVaultSecurityLevelPolicy.ALLOW_SOFTWARE, backend)

            assertEquals(level, manager.getSecurityLevel())
        }
    }

    @Test fun missingKeyReportsUnknownOrUnavailableWithoutCreating() {
        val manager = manager(SecureVaultSecurityLevelPolicy.ALLOW_SOFTWARE, Backend())

        assertEquals(SecureVaultSecurityLevel.UNKNOWN_OR_UNAVAILABLE, manager.getSecurityLevel())
    }

    @Test fun preferredLevelsAcceptAReportedLowerLevel() {
        for (policy in listOf(
            SecureVaultSecurityLevelPolicy.PREFER_TRUSTED_EXECUTION_ENVIRONMENT,
            SecureVaultSecurityLevelPolicy.PREFER_STRONGBOX,
        )) {
            val key = stored(SecureVaultSecurityLevel.SOFTWARE)
            val backend = Backend(stored = key)

            assertSame(key.key, manager(policy, backend).getSecretKey())
            assertEquals(0, backend.deletes)
        }
    }

    @Test fun requiredTeeAcceptsTeeAndStrongBox() {
        for (level in listOf(
            SecureVaultSecurityLevel.TRUSTED_EXECUTION_ENVIRONMENT,
            SecureVaultSecurityLevel.STRONGBOX,
        )) {
            val key = stored(level)
            assertSame(
                key.key,
                manager(
                    SecureVaultSecurityLevelPolicy.REQUIRE_TRUSTED_EXECUTION_ENVIRONMENT,
                    Backend(stored = key),
                ).getSecretKey(),
            )
        }
    }

    @Test fun requirementsRejectLowerAndUnknownExistingKeysWithoutDeletingThem() {
        val cases = listOf(
            SecureVaultSecurityLevelPolicy.REQUIRE_TRUSTED_EXECUTION_ENVIRONMENT to
                SecureVaultSecurityLevel.SOFTWARE,
            SecureVaultSecurityLevelPolicy.REQUIRE_TRUSTED_EXECUTION_ENVIRONMENT to
                SecureVaultSecurityLevel.UNKNOWN_OR_UNAVAILABLE,
            SecureVaultSecurityLevelPolicy.REQUIRE_STRONGBOX to
                SecureVaultSecurityLevel.TRUSTED_EXECUTION_ENVIRONMENT,
        )
        for ((policy, level) in cases) {
            val existing = stored(level)
            val backend = Backend(stored = existing)

            assertUnsupported { manager(policy, backend).getSecretKey() }
            assertSame(existing, backend.stored)
            assertEquals(0, backend.deletes)
        }
    }

    @Test fun noncompliantNewKeyIsRemovedAndReportedAsUnsupported() {
        val backend = Backend(createdLevel = SecureVaultSecurityLevel.SOFTWARE)

        assertUnsupported {
            manager(
                SecureVaultSecurityLevelPolicy.REQUIRE_TRUSTED_EXECUTION_ENVIRONMENT,
                backend,
            ).getOrCreateSecretKey()
        }
        assertEquals(null, backend.stored)
        assertEquals(1, backend.deletes)
    }

    private fun manager(
        policy: SecureVaultSecurityLevelPolicy,
        backend: Backend,
    ): KeyStoreManager = KeyStoreManager(VaultStorageConfig("security-level", policy), backend)

    private fun stored(level: SecureVaultSecurityLevel): StoredKey = StoredKey(
        key = SecretKeySpec(ByteArray(32), "AES"),
        securityLevel = level,
    )

    private fun assertUnsupported(block: () -> Unit) {
        val error = assertThrows(SecureVaultCryptoException::class.java) { block() }
        assertEquals(SecureVaultCryptoFailure.UNSUPPORTED_HARDWARE, error.failure)
    }
}

