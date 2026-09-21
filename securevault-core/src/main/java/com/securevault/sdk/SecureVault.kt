package com.securevault.sdk

import android.content.Context

/** Entry point for configuring and accessing the SecureVault SDK. */
public class SecureVault private constructor(
    private val applicationContext: Context,
    public val config: SecureVaultConfig,
) {
    /** Stable identifier that separates this vault from other vaults in the same app. */
    public val namespace: String
        get() = config.namespace

    /** Version of the SecureVault SDK that created this facade. */
    public val sdkVersion: String
        get() = SDK_VERSION

    /** Capabilities currently attached to this facade. */
    public val capabilities: Set<SecureVaultCapability> = emptySet()

    public companion object {
        /** Current public SDK version. */
        public const val SDK_VERSION: String = SecureVaultVersion.CURRENT

        /**
         * Creates a SecureVault facade without requiring a dependency-injection framework.
         *
         * Only the application context is retained, so passing an Activity or Service does not
         * extend the lifetime of that component.
         */
        @JvmStatic
        public fun create(
            context: Context,
            config: SecureVaultConfig,
        ): SecureVault {
            // Config validation also runs for instances produced by data-class copy(). Calling it
            // here keeps this boundary defensive if construction changes in the future.
            config.validate()
            val applicationContext = context.applicationContext ?: context
            return SecureVault(applicationContext, config)
        }
    }
}
