# Customer API contract

Only `:securevault-core` is the customer SDK. Its complete Kotlin/Java API is
reviewed in `securevault-core/api/securevault-core.api`. No packages are filtered
out of that module's dump. Strict Kotlin explicit API mode requires visibility
and return types to be deliberate.

Intentionally public types in `com.securevault.sdk`:

- `SecureVault`: final facade, private constructor, `create(Context, SecureVaultConfig)`
  factory, configuration, namespace, version and capability inspection.
- `SecureVaultConfig`: immutable namespace and key-protection policy. Its constructor, copy,
  destructuring and generated equality methods are part of the contract. The one-argument JVM
  constructor remains available and selects `ALLOW_SOFTWARE`.
- `SecureVaultSecurityLevel`: the per-key protection level reported by Android: software, trusted
  execution environment, StrongBox, or unknown/unavailable.
- `SecureVaultSecurityLevelPolicy`: whether software is allowed or TEE/StrongBox is preferred or
  required. See [HARDWARE_SECURITY.md](HARDWARE_SECURITY.md) for fallback and reporting behavior.
- `SecureVaultCapability`: stable feature identifiers (availability is reported
  separately; the current facade has no attached capabilities).
- `SecureVaultVersion`: version constant, also available as `SecureVault.SDK_VERSION`.
  Constants are inlined into consumers and require recompilation to refresh.
- `SecureVaultException`: extensible base failure, plus
  `InvalidSecureVaultConfigurationException`, `SecureVaultInitializationException`
  and `SecureVaultCapabilityUnavailableException`, with their documented fields.

Only Android Context, Kotlin/Java standard types, and SDK-owned types belong in
these signatures. Note models, repositories, DAOs, Room, Hilt, Retrofit and OkHttp
are not customer APIs. Future capability implementations must remain behind the
facade's private state and expose SDK-owned contracts. Do not add public concrete
implementation constructors or third-party types to SDK signatures.

## Demo-only module boundaries

`:app`, `:demo-domain`, `:demo-data`, `:securevault-crypto` and `:securevault-network` are application/demo
code, not SDK distribution artifacts or supported customer dependencies. They are
explicitly excluded from customer binary compatibility validation. Their public
cross-module declarations are not promises to SDK customers:

- Domain Note, repository and use cases connect demo UI and storage.
- SecureVaultDataFactory creates the demo repository. Room database, DAO, entity,
  converters, mappers and repository implementation are internal to data.
- KeyStoreManager, SecurePassphraseStore and VaultStorageConfig support data and
  instrumentation tests. Namespace and legacy storage behavior is documented in
  [NAMESPACES.md](NAMESPACES.md).
- SecurityHealthChecker and NetworkResult/NetworkError support the demo UI.
  Its public constructor takes no transport types; Retrofit, HealthApi, client
  builders, interceptor and mapping helpers are internal to networking.

The SDK has no dependency on these projects. `checkSdkBoundary` enforces this
separation and rejects implementation packages in its reviewed API dump. Do not
publish the demo modules as part of the SDK or add them as SDK dependencies.

## Review workflow

Run `./gradlew :securevault-core:checkSdkBoundary` (CI also runs this). Binary
Compatibility Validator compares the compiled API with the committed baseline;
any additions, removals or signature changes require review. Kotlin internal
members are excluded according to Kotlin metadata, even when JVM bytecode uses
public visibility. Internal is not a security boundary against reflection/Java.

For an intentional change, run `./gradlew :securevault-core:apiDump`, inspect the
resulting diff, and commit it with the change. Never regenerate the baseline in
CI. Review source compatibility and behavior too: binary dumps do not capture
all semantics, parameter names, constant values or implementation behavior.

Validator documentation: https://github.com/Kotlin/binary-compatibility-validator

The SDK also exposes SecureVaultCryptoException and SecureVaultCryptoFailure for safe crypto recovery. See [CRYPTO_FAILURES.md](CRYPTO_FAILURES.md).
