package com.securevault.core.crypto

import android.os.Build
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.StrongBoxUnavailableException
import android.security.keystore.UserNotAuthenticatedException
import com.securevault.sdk.SecureVaultCryptoException
import com.securevault.sdk.SecureVaultCryptoFailure
import java.io.IOException
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.CancellationException
import javax.crypto.BadPaddingException

internal enum class CryptoOperation { LOAD_KEYSTORE, LOOKUP_KEY, CREATE_KEY, DELETE_KEY, ENCRYPT, DECRYPT, STORAGE }

/** Evidence is session-local and only applies to keys with a timed authentication policy. */
internal class AuthenticationEvidence {
    @Volatile var usesTimedAuthentication: Boolean = false
    @Volatile private var timedAuthenticationSucceeded: Boolean = false

    fun recordSuccessfulUse() {
        if (usesTimedAuthentication) timedAuthenticationSucceeded = true
    }

    fun reset() { timedAuthenticationSucceeded = false }

    fun failure(): SecureVaultCryptoFailure =
        if (usesTimedAuthentication && timedAuthenticationSucceeded) SecureVaultCryptoFailure.AUTHENTICATION_EXPIRED
        else SecureVaultCryptoFailure.AUTHENTICATION_REQUIRED
}

internal object CryptoFailureMapper {
    fun map(error: Exception, operation: CryptoOperation, authentication: AuthenticationEvidence): SecureVaultCryptoException {
        val seen = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
        val chain = mutableListOf<Throwable>()
        var next: Throwable? = error
        while (next != null && seen.add(next)) {
            chain.add(next)
            next = next.cause
        }
        // Reconstruct even an already classified failure so causes/suppressed exceptions never leak.
        chain.filterIsInstance<SecureVaultCryptoException>().firstOrNull()?.let {
            return SecureVaultCryptoException(it.failure)
        }
        val reason = when {
            chain.any { it is KeyPermanentlyInvalidatedException } -> SecureVaultCryptoFailure.KEY_PERMANENTLY_INVALIDATED
            chain.any { it is UserNotAuthenticatedException } -> authentication.failure()
            Build.VERSION.SDK_INT >= 28 && chain.any { it is StrongBoxUnavailableException } -> SecureVaultCryptoFailure.UNSUPPORTED_HARDWARE
            else -> platformReason(chain, authentication) ?: when {
                operation == CryptoOperation.DECRYPT && chain.any { it is BadPaddingException } -> SecureVaultCryptoFailure.CORRUPT_CIPHERTEXT
                chain.any { it is NoSuchProviderException } -> SecureVaultCryptoFailure.KEYSTORE_UNAVAILABLE
                operation == CryptoOperation.LOAD_KEYSTORE && chain.any {
                    it is IOException || it is java.security.KeyStoreException || it is NoSuchAlgorithmException
                } -> SecureVaultCryptoFailure.KEYSTORE_UNAVAILABLE
                else -> SecureVaultCryptoFailure.UNEXPECTED_PROVIDER_FAILURE
            }
        }
        return SecureVaultCryptoException(reason)
    }

    private fun platformReason(chain: List<Throwable>, authentication: AuthenticationEvidence): SecureVaultCryptoFailure? {
        if (Build.VERSION.SDK_INT < 33) return null
        for (error in chain.filterIsInstance<android.security.KeyStoreException>()) {
            when (error.numericErrorCode) {
                android.security.KeyStoreException.ERROR_KEY_DOES_NOT_EXIST -> return SecureVaultCryptoFailure.MISSING_KEY
                android.security.KeyStoreException.ERROR_USER_AUTHENTICATION_REQUIRED -> return authentication.failure()
                android.security.KeyStoreException.ERROR_UNIMPLEMENTED -> return SecureVaultCryptoFailure.UNSUPPORTED_HARDWARE
                android.security.KeyStoreException.ERROR_KEYSTORE_UNINITIALIZED -> return SecureVaultCryptoFailure.KEYSTORE_UNAVAILABLE
            }
            if (error.isTransientFailure) return SecureVaultCryptoFailure.KEYSTORE_UNAVAILABLE
        }
        return null
    }
}

internal inline fun <T> cryptoOperation(
    operation: CryptoOperation,
    authentication: AuthenticationEvidence = AuthenticationEvidence(),
    block: () -> T,
): T = try {
    block()
} catch (error: CancellationException) {
    throw error
} catch (error: Exception) {
    throw CryptoFailureMapper.map(error, operation, authentication)
}
