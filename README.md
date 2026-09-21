# SecureVault 🔐

SecureVault is a sample Android app showcasing **production-grade mobile security practices**.

It focuses on **doing security correctly** — not just enabling features, but **proving** they work.

---

## ✨ Features

- 🔒 Encrypted local database (Room + SQLCipher)
- 🗝️ Keystore-backed secret protection
- 🌐 SSL certificate pinning (OkHttp)
- ✅ Deterministic security tests
- 🧪 Instrumentation test proving encryption at rest
- ⚙️ Hilt dependency injection
- 🚀 GitHub Actions CI

---

## 🧱 Architecture
![App Architecture](https://github.com/mohdaquib/SecureVault/blob/main/images/app_architecture.png)


# SecureVault 🔐

SecureVault is a sample Android app showcasing **production-grade mobile security practices**.

It focuses on **doing security correctly** — not just enabling features, but **proving** they work.

---

## ✨ Features

- 🔒 Encrypted local database (Room + SQLCipher)
- 🗝️ Keystore-backed secret protection
- 🌐 SSL certificate pinning (OkHttp)
- ✅ Deterministic security tests
- 🧪 Instrumentation test proving encryption at rest
- ⚙️ Hilt dependency injection
- 🚀 GitHub Actions CI

---

## 🔐 Security Highlights

### Encrypted Storage
- SQLCipher encrypts the database
- Encryption key is randomly generated
- Key is encrypted using Android Keystore
- Instrumentation test verifies no plaintext in DB file

### Network Security
- SSL certificate pinning using SPKI hashes
- Pinning enforced at runtime
- MockWebServer tests validate pin success & failure

---

## 🧪 Testing

```bash
./gradlew test
./gradlew connectedDebugAndroidTest

## SDK API boundary

See [API.md](API.md) for supported public types, demo-only boundaries, and the API dump review workflow.
