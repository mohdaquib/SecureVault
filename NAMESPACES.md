# Vault namespaces and legacy demo upgrade

Use `VaultStorageConfig("customer-a")` when constructing a KeyStoreManager or
calling the demo data factory. The passphrase store takes its configuration from
the key manager, and the database takes it from the passphrase store. This avoids
accidentally combining one vault's key with another vault's preferences or file.
These are implementation-module contracts; the customer facade remains unchanged.

Namespaces use the same validation as SecureVaultConfig: 1–64 ASCII characters,
first character a letter or digit, remaining characters letters, digits, `.`, `_`
or `-`. No trimming, case folding or Unicode normalization occurs. Invalid input
throws InvalidSecureVaultConfigurationException with field `namespace` before
any storage is opened. Namespace values are identifiers, not secrets.

## Location and collision contract

Each ASCII character is encoded as two lowercase hexadecimal digits. The common
prefix is `securevault_v1_<encoded namespace>`. For example, namespace `A` uses:

- Keystore alias: `securevault_v1_41_db_key`
- Private SharedPreferences: `securevault_v1_41_crypto`
- App-private database: `securevault_v1_41.db` (including its own WAL/SHM files)

The encoding is reversible, untruncated and injective; `A` and `a` remain distinct
even on case-insensitive filesystems. Valid namespaces cannot escape the storage
directory or collide with legacy names. These names are a persistent format:
changing the encoding or prefix requires a migration.

Within one app installation, the same namespace intentionally reopens the same
vault. It is not a create-only request and does not generate a suffix or overwrite
an existing key. Different namespaces have independent aliases, preferences and
database files. The caller must assign different namespaces to different vaults.
Initial key/passphrase creation is serialized within one process. Multiple Android
processes accessing the same vault are not supported; use one owning process.
Partial or undecryptable preference records fail instead of replacing stored data.

## Existing demo data: in-place compatibility migration

The app explicitly uses `VaultStorageConfig.legacyDemo()`. It preserves all three
original locations together:

- `securevault_db_key` in Android Keystore
- `securevault_crypto` SharedPreferences, with `encrypted_passphrase` and `passphrase_iv`
- `securevault.db`, including SQLite sidecar files

This is an in-place upgrade: existing AES-GCM ciphertext and SQLCipher database
are reopened without copying, renaming, re-encrypting or deleting data. It is
idempotent and has no partial file-move state. Legacy mode represents exactly one
legacy vault; calling it twice reopens that vault. Fresh namespaces never discover
or claim legacy data. Even namespace `demo` creates independent storage.

For an installed demo, keep legacy mode until a separate explicit data-transfer
feature is implemented. Simply switching its configuration to a namespace opens
an empty independent vault; it does not migrate notes. Do not rename only the DB,
clear the preferences, or delete the old Keystore entry. Android Keystore material
must still be present under the same app identity; this is not a backup/restore or
cross-device migration. No fallback generates replacement credentials for corrupt
ciphertext.

## Tests

`./gradlew :securevault-crypto:testDebugUnitTest :securevault-core:testDebugUnitTest`
checks deterministic validation, injective location naming, separate passphrases,
reopening, concurrent initialization, incomplete-state failure and compatibility
with a hard-coded original encrypted preferences fixture (software AES key).

`./gradlew :demo-data:connectedDebugAndroidTest` runs the device tests.
VaultStorageMigrationTest creates an original-format fixture using a real Android
Keystore key and SQLCipher database at literal legacy locations, then opens it
through legacy mode, verifies notes and unchanged preferences, creates a separate
namespaced database/key, and verifies deleting that new vault leaves legacy data
readable. Tests run in the instrumentation app's sandbox and clean their fixtures.
