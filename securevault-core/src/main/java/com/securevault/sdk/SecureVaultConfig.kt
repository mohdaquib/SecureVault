package com.securevault.sdk

/** Configuration shared by all capabilities attached to a SecureVault instance. */
public data class SecureVaultConfig @JvmOverloads public constructor(
    public val namespace: String,
    public val securityLevelPolicy: SecureVaultSecurityLevelPolicy =
        SecureVaultSecurityLevelPolicy.ALLOW_SOFTWARE,
    public val keyAuthenticationPolicy: SecureVaultKeyAuthenticationPolicy =
        SecureVaultKeyAuthenticationPolicy.None,
) {
    init {
        validate()
    }

    internal fun validate() {
        if (namespace.isEmpty()) {
            throw InvalidSecureVaultConfigurationException(
                field = "namespace",
                reason = "must not be empty",
            )
        }
        if (namespace.length > MAX_NAMESPACE_LENGTH) {
            throw InvalidSecureVaultConfigurationException(
                field = "namespace",
                reason = "must contain at most $MAX_NAMESPACE_LENGTH characters",
            )
        }
        if (!VALID_NAMESPACE.matches(namespace)) {
            throw InvalidSecureVaultConfigurationException(
                field = "namespace",
                reason = "must start with an ASCII letter or digit and contain only letters, digits, '.', '_' or '-'",
            )
        }
    }

    private companion object {
        private const val MAX_NAMESPACE_LENGTH: Int = 64
        private val VALID_NAMESPACE: Regex = Regex("[A-Za-z0-9][A-Za-z0-9._-]*")
    }
}
