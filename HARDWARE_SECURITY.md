# Hardware security levels

SecureVault models the protection Android reports for an individual vault key. It does not assume
or claim that every Android device, every Android Keystore implementation, or every generated key
is hardware-backed.

The reported `SecureVaultSecurityLevel` is one of:

- `SOFTWARE`: Android reports that the key is implemented in software.
- `TRUSTED_EXECUTION_ENVIRONMENT`: Android reports TEE-backed key operations.
- `STRONGBOX`: Android reports StrongBox-backed key operations.
- `UNKNOWN_OR_UNAVAILABLE`: the key is absent or Android cannot reliably identify its protection.

On Android 12 / API 31 and later, `KeyInfo.securityLevel` distinguishes software, TEE and
StrongBox. Older Android releases only expose whether a key is inside secure hardware. That older
signal cannot distinguish TEE from StrongBox, so SecureVault reports `UNKNOWN_OR_UNAVAILABLE`
instead of making a stronger claim. A StrongBox key created successfully by an explicit StrongBox
request on API 28–30 is known to be StrongBox-backed for that creation result; a later lookup cannot
reconstruct that distinction from `KeyInfo` alone.

## Choosing a policy

The compatibility default is software-allowed:

```kotlin
val config = SecureVaultConfig(namespace = "customer-data")
```

Customers can choose a preference, which permits fallback, or a requirement, which does not:

```kotlin
val preferHardware = SecureVaultConfig(
    namespace = "customer-data",
    securityLevelPolicy = SecureVaultSecurityLevelPolicy.PREFER_TRUSTED_EXECUTION_ENVIRONMENT,
)

val requireStrongBox = SecureVaultConfig(
    namespace = "high-value-data",
    securityLevelPolicy = SecureVaultSecurityLevelPolicy.REQUIRE_STRONGBOX,
)
```

Policy behavior:

| Policy | Behavior |
| --- | --- |
| `ALLOW_SOFTWARE` | Accept a software, TEE, or StrongBox key. |
| `PREFER_TRUSTED_EXECUTION_ENVIRONMENT` | Accept the Android Keystore result even if it is software-backed. |
| `REQUIRE_TRUSTED_EXECUTION_ENVIRONMENT` | Accept TEE or StrongBox; otherwise fail with `UNSUPPORTED_HARDWARE`. |
| `PREFER_STRONGBOX` | Request StrongBox. If the OS is too old or reports `StrongBoxUnavailableException`, generate through the regular Android Keystore path instead. |
| `REQUIRE_STRONGBOX` | Request StrongBox. If it is unavailable, or the observed key cannot be verified as StrongBox-backed, fail with `UNSUPPORTED_HARDWARE`; do not generate a weaker fallback key. |

Changing a policy does not silently replace an existing key. Existing keys that fail a required
policy remain intact and access fails explicitly. If a newly created key fails a required policy,
that new noncompliant key is removed.
