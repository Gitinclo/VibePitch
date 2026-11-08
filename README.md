# VibePitch Studio

**VibePitch Studio** (`applicationId = "com.aistudio.vibepitch.kxpqmr"`) is a modular Android mobile sales enablement, live white-label pitch studio, quote calculator, and pipeline management platform built with Kotlin, Jetpack Compose, and Firebase Firestore. It empowers solo agencies, freelance developers, and AI remediation consultants to pitch, scope, quote, and close clients across 7 high-margin verticals without writing fragile backend payment or PII handling code.

## Modular Architecture Overview

The repository is structured into clean, single-responsibility layers where every source file is kept under 500 lines:

- **Entry & Navigation (`MainActivity.kt` & `ui/screens/VibePitchWorkspaceScreen.kt`)**:
  - `MainActivity.kt` handles edge-to-edge initialization and auth/guest state routing via `FirebaseEnvValidator`.
  - `VibePitchWorkspaceScreen.kt` orchestrates the adaptive `NavigationBar` / `NavigationRail` shell, real-time offline status dot (`StorageSyncStatusDotChip`), and `OfflineStorageNoticeBanner`.
- **Domain & Validation (`domain/InputBoundsValidator.kt`, `ui/PitchScriptGenerator.kt`)**:
  - `InputBoundsValidator.kt` enforces strict character-length and numeric bounds (`0–50,000` setup fee, `0–5,000` retainer, `0–100%` deposit) matching `firestore.rules`.
  - `QuotePricingCalculator` & `PitchScriptGenerator` compute live estimates/deposits and generate 6-channel outreach & scope agreements.
- **Data Models & Vertical Presets (`data/model/VibePitchModels.kt` & `data/model/NicheCatalog.kt`)**:
  - `@Keep`-annotated Firestore entities (`BusinessAccountProfile`, `ProspectProfile`, `CapturedLead`, `PlaybookTask`) and 7 turnkey vertical catalogs (`NicheCatalog`).
- **Resilient Persistence & Network Layer (`data/repository/VibePitchRepository.kt`, `data/local/LocalGuestSessionManager.kt`, `data/remote/NetworkConnectivityObserver.kt`, `data/remote/FirebaseEnvValidator.kt`)**:
  - Real-time `ConnectivityManager` observation automatically routes mutations to local on-device storage (`LocalGuestSessionManager`) when offline or in Guest Mode, and syncs with Cloud Firestore when online and authenticated.
  - `FirebaseEnvValidator` ensures clean CI/CD and external builds never crash if `google-services.json` is absent, falling back gracefully to local Guest Mode.
  - *Note on `namespace = "com.example"`*: The Gradle `namespace` is preserved as `com.example` for compatibility with the cloud streaming build container, while the production `applicationId` is uniquely configured as `com.aistudio.vibepitch.kxpqmr`.

## Core Capabilities & Multi-Stream Workbench

1. **7 Turnkey Vertical Niches (`NicheKey`) across 3 Revenue Streams (`RevenueStreamMode`)**:
   - **Local Business Solutions**: `HOME_TRADES`, `SALON_AND_CLEANERS`, `DENTAL_AND_TUTORS`, `AUTO_DETAILING`, `BOUTIQUE_FITNESS`
   - **Freelance & Micro-Tools (Upwork / Fiverr)**: `ECOMMERCE_SHOPIFY`
   - **Broken AI Prototype Remediation**: `SAAS_AI_REMEDIATION`
2. **Interactive Quote & Scope Guardrail Engine**:
   - Real-time calculation of Setup Fees (`0–50,000`), Monthly Retainers (`0–5,000`), and Deposit Splits (`0%–100%`).
   - Built-in **Scope Guardrails** enforcing third-party offloading for payments (Stripe Payment Links), PII intake (Google Forms / Zero-Trust Cloud Rules), and authentication (Managed Google OAuth).
3. **6-Part Pitch & Proposal Script Generator (`PitchScriptGenerator`)**:
   - Generates tailored IG/FB DMs, Upwork/Rescue Proposals, Cold Emails, 90-Second Loom Scripts, Plain-English Scope & Security Agreements, and Post-Delivery Testimonial & Referral requests.
4. **30-Day Guest Account to Permanent Business Account Lifecycle + Offline Indicator**:
   - **Days 1–19**: Frictionless on-device Guest Mode via `LocalGuestSessionManager`.
   - **Days 20–30**: Dismissible milestone prompt to bind a permanent Google Business Account.
   - **Day 31+**: Required upgrade gate that automatically migrates all local prospects, leads, playbook progress, and business profile to Cloud Firestore (`migrateGuestDataToCloud`).
   - **Visual Offline Status Dot**: Top-bar status dot (`StorageSyncStatusDotChip`) and banner (`OfflineStorageNoticeBanner`) indicate when the device is offline and saving to local storage vs. Cloud Firestore.

## Automated Verification & Test Suites

- **`VibePitchDomainUnitTest.kt`**: Validates all 7 `NicheKey` catalogs, `QuotePricingCalculator` math, `PitchScriptGenerator` across all 35 `NicheKey × OfferArchetype` combinations, `PipelineStage` transitions, and `InputBoundsValidator` bounds.
- **`VibePitchIntegrationRobolectricTest.kt`**: Validates the 30-day Guest lifecycle (`Day 1`, `Day 20`, `Day 31`), end-to-end Guest-to-Business-Account migration staging & consumption, offline-to-local-storage fallback routing in `VibePitchViewModel`, unauthenticated `VibePitchRepository` resilience, and Jetpack Compose UI rendering of `StorageSyncStatusDotChip` and `OfflineStorageNoticeBanner`.
- **`VibePitchRepositoryRuleTest.kt` & `firestore.test.js`**: Validates zero-trust owner-isolated `firestore.rules` and repository CRUD operations against the Firebase Local Emulator Suite.

## Build & Test Commands

```bash
# 1. Run Tier 1 Firestore Security Rules Emulator Tests
node --test firestore.test.js

# 2. Run Tier 2 Kotlin Domain Unit & Robolectric Integration Tests
gradle :app:testDebugUnitTest --tests "com.example.VibePitchDomainUnitTest" --tests "com.example.VibePitchIntegrationRobolectricTest"

# 3. Assemble Debug APK
gradle :app:assembleDebug
```

## Compliance & Governance Documentation

All repository governance, cybersecurity, and privacy documents are available in the root directory and inside the app via the **Docs & Compliance** button:
- [`SECURITY.md`](./SECURITY.md) — Vulnerability reporting, encryption, and zero-trust Firestore architecture.
- [`PRIVACY.md`](./PRIVACY.md) — Global privacy compliance (150+ jurisdictions as of October 10, 2026).
- [`TERMS_AND_CONDITIONS.md`](./TERMS_AND_CONDITIONS.md) — Commercial usage terms, white-label demo disclaimers, and liability limits.
- [`ACCOUNT.md`](./ACCOUNT.md) — 30-day Guest lifecycle, Google OAuth binding, RBAC, and account deletion flows.
- [`CONTRIBUTING.md`](./CONTRIBUTING.md) — Branching workflow, commit standards, and PR verification requirements.
- [`CODE_OF_CONDUCT.md`](./CODE_OF_CONDUCT.md) — Contributor Covenant v2.1 community guidelines.
- [`ROADMAP.md`](./ROADMAP.md) — Q4 2026, 2027, and Long-Term technical milestones.
- [`GUARDRAILS.md`](./GUARDRAILS.md) — Complete 100 Vibe Coding Agent Guardrails (Categories I–X) with in-app search & `CLAUDE.md` / `.cursorrules` / `.windsurfrules` exporter.
