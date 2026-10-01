package com.securevault.core.crypto

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.securevault.sdk.SecureVaultAllowedAuthenticators
import com.securevault.sdk.SecureVaultCryptoException
import com.securevault.sdk.SecureVaultCryptoFailure
import com.securevault.sdk.SecureVaultKeyAuthenticationPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeyAuthenticationPolicyInstrumentedTest {
    @Test
    fun noAuthenticationDoesNotGateKeyUse() {
        val spec = spec(SecureVaultKeyAuthenticationPolicy.None)

        assertFalse(spec.isUserAuthenticationRequired)
    }

    @Test
    @SdkSuppress(minSdkVersion = 30)
    fun everyOperationSupportsBiometricOnly() {
        val spec = spec(SecureVaultKeyAuthenticationPolicy.EveryOperation(
            SecureVaultAllowedAuthenticators.BIOMETRIC_ONLY,
        ))

        assertTrue(spec.isUserAuthenticationRequired)
        assertEquals(0, spec.userAuthenticationValidityDurationSeconds)
        assertEquals(KeyProperties.AUTH_BIOMETRIC_STRONG, spec.userAuthenticationType)
    }

    @Test
    @SdkSuppress(minSdkVersion = 30)
    fun everyOperationSupportsBiometricOrDeviceCredential() {
        val spec = spec(SecureVaultKeyAuthenticationPolicy.EveryOperation(
            SecureVaultAllowedAuthenticators.BIOMETRIC_OR_DEVICE_CREDENTIAL,
        ))

        assertTrue(spec.isUserAuthenticationRequired)
        assertEquals(0, spec.userAuthenticationValidityDurationSeconds)
        assertEquals(
            KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL,
            spec.userAuthenticationType,
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = 30)
    fun configuredPeriodSupportsBothAuthenticatorChoices() {
        for (allowed in SecureVaultAllowedAuthenticators.entries) {
            val spec = spec(SecureVaultKeyAuthenticationPolicy.ValidFor(45, allowed))

            assertTrue(spec.isUserAuthenticationRequired)
            assertEquals(45, spec.userAuthenticationValidityDurationSeconds)
            val expected = when (allowed) {
                SecureVaultAllowedAuthenticators.BIOMETRIC_ONLY ->
                    KeyProperties.AUTH_BIOMETRIC_STRONG
                SecureVaultAllowedAuthenticators.BIOMETRIC_OR_DEVICE_CREDENTIAL ->
                    KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
            }
            assertEquals(expected, spec.userAuthenticationType)
        }
    }

    @Test
    @SdkSuppress(maxSdkVersion = 29)
    fun legacyAndroidSupportsPerOperationBiometric() {
        val spec = spec(SecureVaultKeyAuthenticationPolicy.EveryOperation(
            SecureVaultAllowedAuthenticators.BIOMETRIC_ONLY,
        ))

        assertTrue(spec.isUserAuthenticationRequired)
        assertEquals(-1, spec.userAuthenticationValidityDurationSeconds)
    }

    @Test
    @SdkSuppress(maxSdkVersion = 29)
    fun legacyAndroidSupportsTimedDeviceAuthentication() {
        val spec = spec(SecureVaultKeyAuthenticationPolicy.ValidFor(
            45,
            SecureVaultAllowedAuthenticators.BIOMETRIC_OR_DEVICE_CREDENTIAL,
        ))

        assertTrue(spec.isUserAuthenticationRequired)
        assertEquals(45, spec.userAuthenticationValidityDurationSeconds)
    }

    @Test
    @SdkSuppress(maxSdkVersion = 29)
    fun legacyAndroidRejectsPoliciesItCannotFaithfullyRepresent() {
        val unsupported = listOf(
            SecureVaultKeyAuthenticationPolicy.EveryOperation(
                SecureVaultAllowedAuthenticators.BIOMETRIC_OR_DEVICE_CREDENTIAL,
            ),
            SecureVaultKeyAuthenticationPolicy.ValidFor(
                45,
                SecureVaultAllowedAuthenticators.BIOMETRIC_ONLY,
            ),
        )

        for (policy in unsupported) {
            val error = assertThrows(SecureVaultCryptoException::class.java) { spec(policy) }
            assertEquals(SecureVaultCryptoFailure.UNSUPPORTED_AUTHENTICATION_POLICY, error.failure)
        }
    }

    private fun spec(policy: SecureVaultKeyAuthenticationPolicy): KeyGenParameterSpec =
        KeyGenParameterSpec.Builder(
            "securevault-auth-policy-${Build.VERSION.SDK_INT}-${policy.hashCode()}",
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .applyKeyAuthenticationPolicy(policy)
            .build()
}
