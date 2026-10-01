package com.securevault.core.crypto

import android.security.keystore.KeyExpiredException
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.StrongBoxUnavailableException
import android.security.keystore.UserNotAuthenticatedException
import com.securevault.sdk.SecureVaultCryptoException
import com.securevault.sdk.SecureVaultCryptoFailure.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.security.ProviderException
import java.util.concurrent.CancellationException
import javax.crypto.AEADBadTagException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class CryptoFailureMapperTest {
    private fun mapped(error: Exception, operation: CryptoOperation = CryptoOperation.DECRYPT,
        auth: AuthenticationEvidence = AuthenticationEvidence()) = CryptoFailureMapper.map(error, operation, auth)

    @Test fun mapsPreciseFailuresEvenWhenWrapped() {
        assertEquals(KEY_PERMANENTLY_INVALIDATED,
            mapped(ProviderException("secret", KeyPermanentlyInvalidatedException())).failure)
        assertEquals(AUTHENTICATION_REQUIRED, mapped(UserNotAuthenticatedException()).failure)
        assertEquals(CORRUPT_CIPHERTEXT, mapped(AEADBadTagException("plaintext")).failure)
        assertEquals(UNSUPPORTED_HARDWARE, mapped(StrongBoxUnavailableException()).failure)
        assertEquals(KEYSTORE_UNAVAILABLE, mapped(IOException("alias"), CryptoOperation.LOAD_KEYSTORE).failure)
        assertEquals(UNEXPECTED_PROVIDER_FAILURE, mapped(ProviderException("private details")).failure)
        assertEquals(MISSING_KEY, mapped(SecureVaultCryptoException(MISSING_KEY)).failure)
    }

    @Test fun expiredRequiresSuccessfulTimedAuthenticationEvidence() {
        val auth = AuthenticationEvidence()
        auth.usesTimedAuthentication = true
        assertEquals(AUTHENTICATION_REQUIRED, mapped(UserNotAuthenticatedException(), auth = auth).failure)
        auth.recordSuccessfulUse()
        assertEquals(AUTHENTICATION_EXPIRED, mapped(UserNotAuthenticatedException(), auth = auth).failure)
        auth.reset()
        assertEquals(AUTHENTICATION_REQUIRED, mapped(UserNotAuthenticatedException(), auth = auth).failure)
        auth.usesTimedAuthentication = false
        auth.recordSuccessfulUse()
        assertEquals(AUTHENTICATION_REQUIRED, mapped(UserNotAuthenticatedException(), auth = auth).failure)
        // Key validity dates and operation expiry are not authentication expiry.
        assertEquals(UNEXPECTED_PROVIDER_FAILURE, mapped(KeyExpiredException()).failure)
    }

    @Test fun unknownErrorsAreNotMissingKeysAndEncryptionErrorsAreNotCorruptData() {
        assertEquals(UNEXPECTED_PROVIDER_FAILURE, mapped(java.security.UnrecoverableKeyException("missing?")).failure)
        assertEquals(UNEXPECTED_PROVIDER_FAILURE, mapped(AEADBadTagException(), CryptoOperation.ENCRYPT).failure)
        assertEquals(UNEXPECTED_PROVIDER_FAILURE, mapped(IOException(), CryptoOperation.STORAGE).failure)
    }

    @Test fun stripsProviderMessagesCausesAndSuppressedErrors() {
        val raw = ProviderException("SECRET_KEY", IllegalArgumentException("PLAINTEXT"))
        raw.addSuppressed(IOException("CIPHERTEXT"))
        val result = mapped(raw)
        assertNull(result.cause)
        assertTrue(result.suppressed.isEmpty())
        for (secret in listOf("SECRET_KEY", "PLAINTEXT", "CIPHERTEXT")) {
            assertFalse(result.stackTraceToString().contains(secret))
        }
        val classified = SecureVaultCryptoException(MISSING_KEY).apply { addSuppressed(raw) }
        assertTrue(mapped(classified).suppressed.isEmpty())
    }

    @Test fun cancellationIsNotConvertedToAProviderFailure() {
        val cancellation = CancellationException()
        assertSame(cancellation, assertThrows(CancellationException::class.java) {
            cryptoOperation(CryptoOperation.DECRYPT) { throw cancellation }
        })
    }

    @Test @Config(sdk = [24]) fun oldAndroidStillMapsTypedFailures() {
        assertEquals(KEY_PERMANENTLY_INVALIDATED, mapped(KeyPermanentlyInvalidatedException()).failure)
        assertEquals(AUTHENTICATION_REQUIRED, mapped(UserNotAuthenticatedException()).failure)
    }
}
