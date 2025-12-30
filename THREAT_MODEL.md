# SecureVault Threat Model

## Overview
SecureVault is a sample Android application demonstrating:
- Encrypted local storage using SQLCipher
- Keystore-backed secret protection
- SSL certificate pinning with deterministic tests

This document outlines the threats considered and how they are mitigated.

---

## Assets
- User notes stored locally
- Database encryption key
- Network trust (TLS)

---

## Threats & Mitigations

### 1. Device compromise / file system access
**Threat:** Attacker extracts the app’s database file.

**Mitigation:**
- Database encrypted with SQLCipher
- Encryption key is random and never stored in plaintext
- Key is encrypted using Android Keystore (non-exportable AES key)

**Result:** Database contents are unreadable at rest.

---

### 2. Reverse engineering / static analysis
**Threat:** Attacker decompiles APK to extract secrets.

**Mitigation:**
- No hardcoded secrets
- Database passphrase generated at runtime
- Keystore keys are non-exportable

---

### 3. Man-in-the-middle (MITM) attacks
**Threat:** Attacker intercepts HTTPS traffic.

**Mitigation:**
- OkHttp certificate pinning (SPKI-based)
- Pinning enforced at runtime
- Pinning failures block all network access

**Validation:**
- Deterministic MockWebServer tests verify both success and failure cases

---

### 4. Accidental security regression
**Threat:** Encryption or pinning removed unintentionally.

**Mitigation:**
- Instrumentation test verifies database is encrypted at rest
- JVM tests verify certificate pinning behavior
- CI pipeline runs tests on every push

---

## Non-Goals
- Rooted device protection
- Runtime memory extraction
- Full disk encryption replacement

These are out of scope for this sample app.

---

## Conclusion
SecureVault demonstrates a realistic, production-style approach to Android app security, focusing on correctness, testability, and regression prevention.
