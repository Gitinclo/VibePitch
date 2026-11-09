# AES-256-GCM Authenticated Encryption & AAD Context Binding Plan

This plan introduces end-to-end **AES-256-GCM (Galois/Counter Mode)** authenticated encryption (AEAD) across VibePitch Studio's local guest storage and sensitive Cloud Firestore PII fields, backed by a 256-bit Android Keystore master key and an interactive verification inspector in the Compliance & Security Hub.

## User Review & Critical Decisions

> [!IMPORTANT]
> All three architectural choices from our planning discussion have been locked in and incorporated into this specification:

- **Confirmed Decision 1 — Encryption Scope**: **Both Local Guest Storage & Sensitive Cloud Firestore PII Fields** will be protected using AES-256-GCM (`AES/GCM/NoPadding`) with 96-bit (12-byte) unique nonces, 128-bit authentication tags, and URL-safe Base64 `ENCv1:` envelopes.
- **Confirmed Decision 2 — Key Management**: **Android Keystore Hardware-Backed 256-bit Key with JVM Test Fallback** (`AndroidKeyStore` TEE/StrongBox when running on Android hardware such as the Realme 8 5G Dimensity 700, with automatic deterministic 256-bit `SecretKeySpec` fallback in local JVM/Robolectric unit tests, plus optional PBKDF2-HMAC-SHA256 passphrase key derivation).
- **Confirmed Decision 3 — UI & Verification Visibility**: **Transparent Storage Encryption + Interactive Compliance Hub Inspector** (`AES-256-GCM` tab inside the Docs & Global Compliance Hub allowing live AEAD payload inspection and AAD tamper verification).

---

## 1. Overview & Core Concept

- **What It Does**:
  - **Cryptographic Core (AES-256-GCM AEAD)**: Encrypts sensitive application data using a 256-bit key, generating a fresh cryptographically secure 12-byte (96-bit) nonce via `SecureRandom` for every encryption call and appending a 16-byte (128-bit) GCM authentication tag into a compact URL-safe Base64 token (`ENCv1:<base64url(nonce + ciphertext + tag)>`).
  - **Context Binding via AAD (Associated Authenticated Data)**: Binds every encrypted payload to its immutable owner and record scope (e.g., `user_id:<uid>:prospects` or `guest_prefs:local_guest_user:<key>`) so ciphertext cannot be transplanted across users, collections, or records without triggering an immediate authentication tag failure (`AEADBadTagException`).
  - **Dual-Layer Persistence Protection**:
    - **Local Guest Storage**: Encrypts serialized JSON payloads for guest prospects, captured leads, playbook tasks, and pending business upgrade drafts stored in local preferences.
    - **Cloud Firestore PII Fields**: Transparently encrypts sensitive PII fields (`businessEmail`, `businessPhone`, `contactName`, `websiteGapNote`, `customerName`, `customerPhone`, `customerEmail`, and `notes`) before cloud transmission and decrypts them upon snapshot observation, while keeping structural routing fields (`userId`, `nicheKey`, `stage`, numeric pricing bounds) queryable and validated by Firestore Security Rules.
- **Target Audience / Persona**: Agency owners, freelancers, and security-conscious B2B consultants running live client demos on Android devices (optimized for the Realme 8 5G baseline).
- **Key Value**: Guarantees both confidentiality and cryptographic integrity against tampering or cross-record ciphertext replay without degrading the 90Hz (`11.1ms` budget) UI render loop.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Zero-Friction Transparent Encryption**: Creating prospects in Brand Studio, capturing client leads in Live Demo, or staging a Business Account automatically encrypts sensitive fields using AES-256-GCM on background Cortex-A55 threads before writing to UFS 2.1 storage or Cloud Firestore.
  2. **Graceful Legacy Migration**: Existing unencrypted local or cloud records (lacking the `ENCv1:` prefix) are transparently read and upgraded to `ENCv1:` AES-256-GCM ciphertext on the next write operation.
  3. **Interactive AES-256-GCM Compliance Inspector**: Tapping the shield icon in the top app bar opens the **Docs & Global Compliance Hub**, which now features a dedicated **`AES-256-GCM`** tab with a live interactive verification card:
     - Displays active key provider status (`AndroidKeyStore 256-bit AES-GCM` vs. `JVM 256-bit Test Key`), nonce length (`96-bit / 12B`), and tag length (`128-bit / 16B`).
     - Lets the user run a live **Encrypt + AAD Verify** test or simulate an **AAD Context Tamper Attack** to observe real-time `AEADBadTagException` rejection.
- **Visual Identity & Theme**:
  - **Aesthetic Direction**: High-contrast dark fintech/security telemetry matching VibePitch's `ElectricIndigo` (`#6366F1`), `EmeraldConversion` (`#10B981`), and `AmberHighlight` (`#F59E0B`) palette.
  - **Component Styling & Layout**: Single-pass Material 3 cards with monospace Base64url ciphertext previews, status badges, and 48dp+ accessible touch targets that preserve Mali-G57 MC2 fill-rate budgets.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Field-Level PII Encryption in Firestore vs. Whole-Document Blob Encryption**
  - *Chosen Approach*: Selective field-level AES-256-GCM encryption for PII and free-text notes (`businessEmail`, `businessPhone`, `contactName`, `websiteGapNote`, `customerName`, `customerPhone`, `customerEmail`, `notes`), paired with `userId`/collection AAD binding.
  - *Why*: Preserves server-side zero-trust Firestore Security Rules validation on `userId`, `nicheKey`, `stage`, `depositPercent`, `setupFeeOffer`, and `monthlyRetainerOffer` while rendering all personal contact details and client notes unreadable at rest in the database.
  - *Alternatives Considered*: Encrypting the entire document into a single opaque blob would break Firestore query filters and server-side numeric bounds enforcement in `firestore.rules`.

- **Decision 2: Self-Describing `ENCv1:` Envelope Prefix**
  - *Chosen Approach*: Prefix all Base64url-encoded `(12-byte IV + Ciphertext + 16-byte GCM Tag)` payloads with `ENCv1:`.
  - *Why*: Allows deterministic detection of encrypted payloads, seamless backward compatibility with pre-existing starter/seed data, and zero-downtime migration.

- **Decision 3: Offloading AES-256-GCM Operations to Cortex-A55 Background Threads**
  - *Chosen Approach*: Execute local preference encryption on `DimensityCpuScheduler.ufsSequentialIoDispatcher` and repository payload encryption within coroutine pipelines outside the main UI render loop.
  - *Why*: Protects the 11.1ms (90Hz) frame-time budget on the Realme 8 5G's two Cortex-A76 cores.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Cryptographic Data Flow Diagram

```
┌────────────────────────────────────────────────────────────────────────────┐
│                   UI & Presentation Layer (90Hz A76 Loop)                  │
│  ┌──────────────────────────────┐      ┌────────────────────────────────┐  │
│  │ LiveDemo / BrandStudio / CRM │      │ Compliance & Security Hub      │  │
│  │ (Plaintext Domain Models)    │      │ + Live AES-256-GCM Inspector   │  │
│  └──────────────┬───────────────┘      └───────────────┬────────────────┘  │
└─────────────────┼──────────────────────────────────────┼───────────────────┘
                  │                                      │
                  ▼                                      ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                AES-256-GCM Cryptographic Engine (AEAD Core)                │
│  • Cipher: AES/GCM/NoPadding (256-bit Key, 96-bit Nonce, 128-bit Auth Tag) │
│  • Nonce RNG: Fresh 12-byte SecureRandom per encrypt() invocation          │
│  • Context Binding: cipher.updateAAD("userId:collection" UTF-8 bytes)      │
│  • Envelope Format: "ENCv1:" + Base64UrlNoPad(12B Nonce || Ciphertext+Tag) │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │ Key Provider: AndroidKeyStore ("vibe_pitch_aes256_master_key")       │  │
│  │ Fallback: Ephemeral/Deterministic 256-bit SecretKeySpec for JVM Test │  │
│  │ Optional KDF: PBKDF2WithHmacSHA256 (256-bit, 16B Salt)               │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────┬──────────────────────────────┬───────────────────┘
                          │                              │
        ┌─────────────────┘                              └─────────────────┐
        ▼                                                                  ▼
┌───────────────────────────────────────┐  ┌───────────────────────────────────────┐
│   Local Guest Storage (UFS 2.1 I/O)   │  │   Cloud Firestore Repository Layer    │
│ • Encrypts Prospects, Leads, Playbook │  │ • Encrypts PII fields before write:   │
│   & Pending Business Profile JSON     │  │   contactName, websiteGapNote,        │
│ • Bound to AAD:                       │  │   customerName/Phone/Email, notes,    │
│   "guest_prefs:local_guest_user:<key>"│  │   businessEmail, businessPhone        │
│ • Runs on ufsSequentialIoDispatcher   │  │ • Bound to AAD: "users/<uid>/<coll>"  │
└───────────────────────────────────────┘  └───────────────────────────────────────┘
```

### Interactive Component & State Mapping

- **AES-256-GCM Crypto Engine**:
  - `encrypt(plaintext: String, associatedData: String? = null): String`: Generates a fresh 12-byte IV via `SecureRandom`, initializes `AES/GCM/NoPadding` with `GCMParameterSpec(128, iv)`, binds `associatedData` via `cipher.updateAAD(...)` when non-empty, and returns `"ENCv1:" + Base64Url(iv + ciphertextAndTag)`.
  - `decrypt(token: String, associatedData: String? = null): Result<String>`: Validates and strips the `"ENCv1:"` prefix (or returns raw plaintext if unprefixed legacy data), extracts the 12-byte nonce and remaining `ciphertext + 16-byte tag`, binds `associatedData`, and verifies the 128-bit GCM authentication tag before returning UTF-8 plaintext.
  - `decryptOrFallback(token: String, associatedData: String? = null): String`: Safely decrypts `ENCv1:` tokens or falls back cleanly if a legacy record is encountered.
- **Firestore Schema & Security Rules Alignment**:
  - Updates maximum string size constraints on encrypted PII fields in `firestore.rules` (and `firebase-blueprint.json`) to accommodate the `ENCv1:` Base64url expansion (`12B Nonce + Ciphertext + 16B Tag`) while keeping strict ownership (`request.auth.uid == userId`), required keys, and numeric range checks intact.
- **Automated Verification Suite**:
  - Adds unit and Robolectric tests verifying:
    1. Unique 12-byte nonce generation across identical plaintexts (never reusing nonces).
    2. Round-trip encryption/decryption with AAD context binding.
    3. Cryptographic rejection (`Result.isFailure` / `AEADBadTagException`) when ciphertext bytes or AAD context (`user_id:attacker` vs. `user_id:owner`) are tampered with.
    4. End-to-end encryption at rest in `LocalGuestSessionManager` and `VibePitchRepository`.
