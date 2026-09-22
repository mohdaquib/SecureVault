package com.securevault.sdk

import org.junit.Assert.*
import org.junit.Test

class SecureVaultCryptoExceptionTest {
    @Test fun everyReasonHasASanitizedMessageWithoutProviderDetails() {
        assertEquals(8, SecureVaultCryptoFailure.entries.size)
        for (reason in SecureVaultCryptoFailure.entries) {
            val error = SecureVaultCryptoException(reason)
            assertSame(reason, error.failure)
            assertFalse(error.message.isNullOrBlank())
            assertNull(error.cause)
            assertTrue(error.suppressed.isEmpty())
        }
    }
}
