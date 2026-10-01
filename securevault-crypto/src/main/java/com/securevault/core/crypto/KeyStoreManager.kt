package com.securevault.core.crypto

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import com.securevault.sdk.SecureVaultCryptoException
import com.securevault.sdk.SecureVaultCryptoFailure
import com.securevault.sdk.SecureVaultAllowedAuthenticators
import com.securevault.sdk.SecureVaultKeyAuthenticationPolicy
import com.securevault.sdk.SecureVaultSecurityLevel
import com.securevault.sdk.SecureVaultSecurityLevelPolicy
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory

internal data class StoredKey(
    val key: SecretKey,
    val usesTimedAuthentication: Boolean = false,
    val securityLevel: SecureVaultSecurityLevel = SecureVaultSecurityLevel.UNKNOWN_OR_UNAVAILABLE,
    val matchesAuthenticationPolicy: Boolean = true,
)

internal interface KeyStoreBackend {
    fun find(): StoredKey?
    fun create(): StoredKey
    fun delete()
}

class KeyStoreManager internal constructor(
    val storageConfig: VaultStorageConfig,
    private val backend: KeyStoreBackend,
) {
    constructor(storageConfig: VaultStorageConfig) : this(
        storageConfig,
        AndroidKeyStoreBackend(
            storageConfig.keyAlias,
            storageConfig.securityLevelPolicy,
            storageConfig.keyAuthenticationPolicy,
        ),
    )

    internal val authentication = AuthenticationEvidence()

    /** Read-only lookup. Never generates a replacement for lost or unusable key material. */
    fun getSecretKey(): SecretKey = synchronized(keyLock) {
        cryptoOperation(CryptoOperation.LOOKUP_KEY, authentication) {
            select(requireAcceptable(
                backend.find() ?: throw SecureVaultCryptoException(SecureVaultCryptoFailure.MISSING_KEY),
            ))
        }
    }

    /** Only for new storage: creation is permitted solely after a successful absent-entry lookup. */
    fun getOrCreateSecretKey(): SecretKey = synchronized(keyLock) {
        val existing = cryptoOperation(CryptoOperation.LOOKUP_KEY, authentication) { backend.find() }
        if (existing != null) select(requireAcceptable(existing)) else {
            // If a newly generated key does not meet a required level, remove only that new key.
            val created = cryptoOperation(CryptoOperation.CREATE_KEY, authentication) { backend.create() }
            try {
                requireAcceptable(created)
            } catch (error: SecureVaultCryptoException) {
                cryptoOperation(CryptoOperation.DELETE_KEY, authentication) { backend.delete() }
                throw error
            }
            authentication.reset()
            select(created)
        }
    }

    /** Returns Android's reported protection for the stored key, without creating one. */
    fun getSecurityLevel(): SecureVaultSecurityLevel = synchronized(keyLock) {
        cryptoOperation(CryptoOperation.LOOKUP_KEY, authentication) {
            backend.find()?.securityLevel ?: SecureVaultSecurityLevel.UNKNOWN_OR_UNAVAILABLE
        }
    }

    private fun select(stored: StoredKey): SecretKey {
        authentication.usesTimedAuthentication = stored.usesTimedAuthentication
        return stored.key
    }

    /** Explicit destructive operation; never invoked by recovery or error mapping. */
    fun deleteKey(): Unit = synchronized(keyLock) {
        cryptoOperation(CryptoOperation.DELETE_KEY, authentication) { backend.delete() }
        authentication.reset()
    }

    private fun requireAcceptable(stored: StoredKey): StoredKey {
        if (!stored.matchesAuthenticationPolicy) {
            throw SecureVaultCryptoException(
                SecureVaultCryptoFailure.UNSUPPORTED_AUTHENTICATION_POLICY,
            )
        }
        val policy = storageConfig.securityLevelPolicy
        if (policy.isRequired && !stored.securityLevel.meets(policy.preferredLevel)) {
            throw SecureVaultCryptoException(SecureVaultCryptoFailure.UNSUPPORTED_HARDWARE)
        }
        return stored
    }

    private companion object { val keyLock = Any() }
}

private class AndroidKeyStoreBackend(
    private val alias: String,
    private val securityLevelPolicy: SecureVaultSecurityLevelPolicy,
    private val keyAuthenticationPolicy: SecureVaultKeyAuthenticationPolicy,
) : KeyStoreBackend {
    private val keyStore: KeyStore by lazy {
        cryptoOperation(CryptoOperation.LOAD_KEYSTORE) {
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        }
    }

    override fun find(): StoredKey? {
        val entry = keyStore.getEntry(alias, null)
        if (entry == null) {
            if (keyStore.containsAlias(alias)) {
                throw SecureVaultCryptoException(SecureVaultCryptoFailure.UNEXPECTED_PROVIDER_FAILURE)
            }
            return null
        }
        if (entry !is KeyStore.SecretKeyEntry) {
            throw SecureVaultCryptoException(SecureVaultCryptoFailure.UNEXPECTED_PROVIDER_FAILURE)
        }
        val info = SecretKeyFactory.getInstance(entry.secretKey.algorithm, "AndroidKeyStore")
            .getKeySpec(entry.secretKey, KeyInfo::class.java) as KeyInfo
        return StoredKey(
            key = entry.secretKey,
            usesTimedAuthentication = info.isUserAuthenticationRequired &&
                info.userAuthenticationValidityDurationSeconds > 0,
            securityLevel = info.secureVaultSecurityLevel(),
            matchesAuthenticationPolicy = info.matches(keyAuthenticationPolicy),
        )
    }

    override fun create(): StoredKey {
        val requestsStrongBox = securityLevelPolicy.preferredLevel == SecureVaultSecurityLevel.STRONGBOX
        if (requestsStrongBox && Build.VERSION.SDK_INT < 28) {
            if (securityLevelPolicy.isRequired) {
                throw SecureVaultCryptoException(SecureVaultCryptoFailure.UNSUPPORTED_HARDWARE)
            }
            return generate(strongBox = false)
        }
        if (!requestsStrongBox) return generate(strongBox = false)
        return try {
            generate(strongBox = true)
        } catch (error: StrongBoxUnavailableException) {
            if (securityLevelPolicy.isRequired) throw error
            generate(strongBox = false)
        }
    }

    private fun generate(strongBox: Boolean): StoredKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val builder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setKeySize(256)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .applyKeyAuthenticationPolicy(keyAuthenticationPolicy)
        if (strongBox && Build.VERSION.SDK_INT >= 28) builder.setIsStrongBoxBacked(true)
        generator.init(builder.build())
        val key = generator.generateKey()
        val info = SecretKeyFactory.getInstance(key.algorithm, "AndroidKeyStore")
            .getKeySpec(key, KeyInfo::class.java) as KeyInfo
        return StoredKey(
            key = key,
            usesTimedAuthentication = info.isUserAuthenticationRequired &&
                info.userAuthenticationValidityDurationSeconds > 0,
            // API 28-30 cannot distinguish TEE from StrongBox through KeyInfo, but successful
            // generation with setIsStrongBoxBacked(true) is an explicit StrongBox guarantee.
            securityLevel = if (strongBox && Build.VERSION.SDK_INT in 28..30) {
                SecureVaultSecurityLevel.STRONGBOX
            } else {
                info.secureVaultSecurityLevel()
            },
            matchesAuthenticationPolicy = info.matches(keyAuthenticationPolicy),
        )
    }

    override fun delete() { keyStore.deleteEntry(alias) }
}

private fun KeyInfo.matches(policy: SecureVaultKeyAuthenticationPolicy): Boolean = when (policy) {
    SecureVaultKeyAuthenticationPolicy.None -> !isUserAuthenticationRequired
    is SecureVaultKeyAuthenticationPolicy.EveryOperation ->
        isUserAuthenticationRequired &&
            userAuthenticationValidityDurationSeconds ==
            (if (Build.VERSION.SDK_INT >= 30) 0 else -1) &&
            matches(policy.allowedAuthenticators)
    is SecureVaultKeyAuthenticationPolicy.ValidFor ->
        isUserAuthenticationRequired &&
            userAuthenticationValidityDurationSeconds == policy.validityDurationSeconds &&
            matches(policy.allowedAuthenticators)
}

private fun KeyInfo.matches(allowed: SecureVaultAllowedAuthenticators): Boolean {
    if (Build.VERSION.SDK_INT < 30) {
        return when (allowed) {
            SecureVaultAllowedAuthenticators.BIOMETRIC_ONLY ->
                userAuthenticationValidityDurationSeconds == -1
            SecureVaultAllowedAuthenticators.BIOMETRIC_OR_DEVICE_CREDENTIAL ->
                userAuthenticationValidityDurationSeconds > 0
        }
    }
    val expected = when (allowed) {
        SecureVaultAllowedAuthenticators.BIOMETRIC_ONLY -> KeyProperties.AUTH_BIOMETRIC_STRONG
        SecureVaultAllowedAuthenticators.BIOMETRIC_OR_DEVICE_CREDENTIAL ->
            KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
    }
    return userAuthenticationType == expected
}

internal fun KeyGenParameterSpec.Builder.applyKeyAuthenticationPolicy(
    policy: SecureVaultKeyAuthenticationPolicy,
): KeyGenParameterSpec.Builder = apply {
    when (policy) {
        SecureVaultKeyAuthenticationPolicy.None -> setUserAuthenticationRequired(false)
        is SecureVaultKeyAuthenticationPolicy.EveryOperation -> configureAuthentication(
            timeoutSeconds = 0,
            allowedAuthenticators = policy.allowedAuthenticators,
        )
        is SecureVaultKeyAuthenticationPolicy.ValidFor -> configureAuthentication(
            timeoutSeconds = policy.validityDurationSeconds,
            allowedAuthenticators = policy.allowedAuthenticators,
        )
    }
}

private fun KeyGenParameterSpec.Builder.configureAuthentication(
    timeoutSeconds: Int,
    allowedAuthenticators: SecureVaultAllowedAuthenticators,
) {
    setUserAuthenticationRequired(true)
    if (Build.VERSION.SDK_INT >= 30) {
        val authenticationTypes = when (allowedAuthenticators) {
            SecureVaultAllowedAuthenticators.BIOMETRIC_ONLY -> KeyProperties.AUTH_BIOMETRIC_STRONG
            SecureVaultAllowedAuthenticators.BIOMETRIC_OR_DEVICE_CREDENTIAL ->
                KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
        }
        setUserAuthenticationParameters(timeoutSeconds, authenticationTypes)
        setInvalidatedByBiometricEnrollment(
            allowedAuthenticators == SecureVaultAllowedAuthenticators.BIOMETRIC_ONLY,
        )
        return
    }

    // Before API 30 Android cannot independently select authenticator type and timeout. The only
    // combinations it can faithfully enforce are per-use biometric or a timed credential window.
    when {
        timeoutSeconds == 0 &&
            allowedAuthenticators == SecureVaultAllowedAuthenticators.BIOMETRIC_ONLY -> {
            @Suppress("DEPRECATION")
            setUserAuthenticationValidityDurationSeconds(-1)
            setInvalidatedByBiometricEnrollment(true)
        }
        timeoutSeconds > 0 &&
            allowedAuthenticators == SecureVaultAllowedAuthenticators.BIOMETRIC_OR_DEVICE_CREDENTIAL -> {
            @Suppress("DEPRECATION")
            setUserAuthenticationValidityDurationSeconds(timeoutSeconds)
            setInvalidatedByBiometricEnrollment(false)
        }
        else -> throw SecureVaultCryptoException(
            SecureVaultCryptoFailure.UNSUPPORTED_AUTHENTICATION_POLICY,
        )
    }
}

private fun KeyInfo.secureVaultSecurityLevel(): SecureVaultSecurityLevel =
    if (Build.VERSION.SDK_INT >= 31) {
        when (securityLevel) {
            KeyProperties.SECURITY_LEVEL_SOFTWARE -> SecureVaultSecurityLevel.SOFTWARE
            KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT ->
                SecureVaultSecurityLevel.TRUSTED_EXECUTION_ENVIRONMENT
            KeyProperties.SECURITY_LEVEL_STRONGBOX -> SecureVaultSecurityLevel.STRONGBOX
            else -> SecureVaultSecurityLevel.UNKNOWN_OR_UNAVAILABLE
        }
    } else if (!isInsideSecureHardware) {
        SecureVaultSecurityLevel.SOFTWARE
    } else {
        // Before API 31 Android only reports that some secure hardware is used, not whether it is
        // TEE or StrongBox. Do not overstate the protection level.
        SecureVaultSecurityLevel.UNKNOWN_OR_UNAVAILABLE
    }

private fun SecureVaultSecurityLevel.meets(required: SecureVaultSecurityLevel): Boolean = when (required) {
    SecureVaultSecurityLevel.SOFTWARE -> this != SecureVaultSecurityLevel.UNKNOWN_OR_UNAVAILABLE
    SecureVaultSecurityLevel.TRUSTED_EXECUTION_ENVIRONMENT ->
        this == SecureVaultSecurityLevel.TRUSTED_EXECUTION_ENVIRONMENT ||
            this == SecureVaultSecurityLevel.STRONGBOX
    SecureVaultSecurityLevel.STRONGBOX -> this == SecureVaultSecurityLevel.STRONGBOX
    SecureVaultSecurityLevel.UNKNOWN_OR_UNAVAILABLE -> false
}
