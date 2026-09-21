package com.securevault.sdk

/** Base type for failures surfaced by the public SecureVault API. */
public open class SecureVaultException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/** Thrown before initialization when a configuration value is invalid. */
public class InvalidSecureVaultConfigurationException(
    public val field: String,
    public val reason: String,
) : SecureVaultException("Invalid SecureVault configuration for '$field': $reason")

/** Thrown when the SDK cannot finish initializing a valid configuration. */
public class SecureVaultInitializationException(
    cause: Throwable,
) : SecureVaultException("SecureVault initialization failed", cause)

/** Thrown when an operation requires a capability that is not attached. */
public class SecureVaultCapabilityUnavailableException(
    public val capability: SecureVaultCapability,
) : SecureVaultException("SecureVault capability '${capability.id}' is unavailable")
