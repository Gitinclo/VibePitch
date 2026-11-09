# VibePitch Studio

**VibePitch Studio** (`namespace = "com.gitinclo.vibepitch"`, `applicationId = "com.aistudio.vibepitch.kxpqmr"`) is a production-hardened Android mobile sales enablement, live white-label pitch studio, interactive quote calculator, pipeline CRM, and hardware-governed workbench built with Kotlin, Jetpack Compose, and Firebase Firestore. It empowers solo agencies, freelance developers, and AI prototype remediation consultants to pitch, scope, quote, and close clients across 7 high-margin verticals without writing fragile backend payment or raw PII handling code.

---

## 1. Decomposed Modular Architecture (`com.gitinclo.vibepitch`)

The codebase is organized into focused, single-responsibility packages under `app/src/main/java/com/gitinclo/vibepitch/`:

- **Entry & Navigation (`MainActivity.kt`, `ui/navigation/VibePitchNavGraph.kt`, `ui/VibePitchApp.kt`)**:
  - `MainActivity.kt` (20 lines) initializes edge-to-edge rendering and hosts `VibePitchNavGraph()`.
  - `VibePitchNavGraph.kt` manages top-level session state routing between `AuthScreen` and the main workspace via `FirebaseEnvValidator` and `LocalGuestSessionManager`.
  - `VibePitchApp.kt` orchestrates the adaptive `NavigationBar` / `NavigationRail` shell, real-time offline storage indicator (`StorageSyncStatusDotChip`), `OfflineStorageNoticeBanner`, and the live **Realme 8 5G Hardware Telemetry Badge (`HardwareTelemetryTopBarBadge`)**.
- **State, Domain & Validation (`ui/VibePitchViewModel.kt`, `ui/VibePitchViewModelDelegates.kt`, `domain/VibePitchWorkspaceCoordinator.kt`, `domain/InputBoundsValidator.kt`)**:
  - `VibePitchViewModel.kt` (< 300 lines) exposes `@Immutable` `StateFlow` snapshots while delegating domain persistence to `VibePitchWorkspaceCoordinator` and state transitions / memoized pitch-script caching to `VibePitchStateTransitions`.
  - `InputBoundsValidator.kt` enforces strict character-length and numeric bounds (`0–50,000` setup fee, `0–5,000` monthly retainer, `0–100%` deposit) matching `firestore.rules`.
- **Hardened Auth & Cloud/Local Repository (`auth/AuthGuardState.kt`, `data/repository/VibePitchRepository.kt`, `data/local/LocalGuestSessionManager.kt`)**:
  - Replaces runtime `requireUserId()` exceptions with explicit `AuthGuardState` (`Authenticated`, `GuestMode`, `Unauthenticated`), `UnauthenticatedSessionException`, and `DataResult<T>` / `Result<T>` return contracts.
  - Automatically routes mutations to local UFS 2.1 storage (`LocalGuestSessionManager`) when offline or in Guest Mode, and syncs with Cloud Firestore when online and authenticated.
- **Realme 8 5G Hardware & Performance Governor (`performance/RealmeHardwareOptimizer.kt`, `ui/features/performance/Realme8HardwareHudDialog.kt`)**:
  - Dedicated hardware optimization engine tuned for the **MediaTek Dimensity 700 (MT6833)** SoC, **90Hz 1080x2400 IPS LCD**, **ARM Mali-G57 MC2 GPU**, **4GB LPDDR4X RAM baseline**, and **UFS 2.1 storage**.
- **Feature-Scoped UI Modules (`ui/features/`)**:
  - `ui/features/auth/` — `AuthScreen.kt`, `GuestUpgradeComponents.kt`, `PermanentBusinessAccountDialog.kt`
  - `ui/features/livedemo/` — `LiveDemoScreen.kt`, `LiveDemoComponents.kt`
  - `ui/features/studio/` — `BrandStudioScreen.kt`, `BrandSwatchPricingCard.kt`
  - `ui/features/crm/` — `ProspectCrmScreen.kt`, `ProspectCrmComponents.kt`, `NetworkStorageStatusComponents.kt`
  - `ui/features/playbook/` — `PlaybookAndPitchScreen.kt`, `PlaybookComponents.kt`, `VibeGuardrailsExplorerCard.kt`
  - `ui/features/compliance/` — `ComplianceDocsDialog.kt`
  - `ui/features/performance/` — `Realme8HardwareHudDialog.kt`

---

## 2. Realme 8 5G (Dimensity 700 / 90Hz / 4GB RAM) Hardware Optimization Suite

VibePitch Studio is engineered with the **Realme 8 5G (Android 11–13 / Realme UI 4.0)** as its baseline hardware target:

1. **Cortex-A76 / Cortex-A55 Heterogeneous Thread Pinning (`DimensityCpuScheduler`)**:
   - Keeps UI/Compose frame traversal on the main thread (scheduled on the two **2.2 GHz Cortex-A76** performance cores).
   - Routes background workloads to a bounded 6-thread pool (`a55BackgroundDispatcher`) set to `Process.THREAD_PRIORITY_BACKGROUND` so Linux Energy Aware Scheduling (EAS) pins background tasks to the six **2.0 GHz Cortex-A55** efficiency cores.
   - Routes `LocalGuestSessionManager` JSON encoding and `SharedPreferences` writes through a dedicated single-thread sequential executor (`ufsSequentialIoDispatcher`) so UFS 2.1 disk I/O never blocks the UI thread.
2. **90Hz Display & Thermal Governor (`RealmeHardwareOptimizer`)**:
   - Negotiates native `1080x2400` display modes on Realme UI 4.0 by pairing `preferredDisplayModeId` with `preferredRefreshRate` (`90f` or `60f`) on `WindowManager.LayoutParams`.
   - Samples `Choreographer` frame durations against the **11.1ms (90 FPS)** and **16.6ms (60 FPS)** budgets while throttling UI state emissions to once every **500ms** to prevent secondary recomposition jank.
   - Binds `PowerManager.OnThermalStatusChangedListener` to automatically step down to a **60Hz Thermal Cap (`16.6ms` budget)** and **720p Fill-Rate Saver** when thermal status reaches `THERMAL_STATUS_MODERATE` or higher.
3. **ARM Mali-G57 MC2 GPU & Non-Destructive Render Scaling**:
   - Uses single-pass flat vector `Canvas` gauges in the top-bar badge and HUD dialog to protect dual-core Mali-G57 MC2 fill rate.
   - Supports toggling between `NATIVE_1080P` (`ASTC + 4x MSAA`) and `SAVER_720P_SCALE` (bypassing MSAA and dropping shader complexity without distorting `1080x2400` Compose layout density).
4. **4GB LPDDR4X RAM Guardrail & Non-Blocking Cache Eviction**:
   - Marks all UI and domain models `@Immutable` and enforces `key` + `contentType` recycling on `LazyColumn` items.
   - Wires `ComponentCallbacks2.onTrimMemory` (`TRIM_MEMORY_RUNNING_LOW`, `TRIM_MEMORY_UI_HIDDEN`) and a manual HUD action to flush memoized pitch script caches (`clearDomainCaches()`) without triggering stop-the-world `System.gc()` pauses.

---

## 3. Core Functional Capabilities

1. **7 Turnkey Vertical Niches (`NicheKey`) across 3 Revenue Streams (`RevenueStreamMode`)**:
   - **Local Business Solutions**: `HOME_TRADES`, `SALON_AND_CLEANERS`, `DENTAL_AND_TUTORS`, `AUTO_DETAILING`, `BOUTIQUE_FITNESS`
   - **Freelance & Micro-Tools (Upwork / Fiverr)**: `ECOMMERCE_SHOPIFY`
   - **Broken AI Prototype Remediation**: `SAAS_AI_REMEDIATION`
2. **Interactive Quote & Scope Guardrail Engine**:
   - Real-time calculation of Setup Fees (`0–50,000`), Monthly Retainers (`0–5,000`), and Deposit Splits (`0%–100%`).
   - Built-in **Scope Guardrails** enforcing third-party offloading for payments (Stripe Payment Links), PII intake (Google Forms / Zero-Trust Cloud Rules), and authentication (Managed Google OAuth).
3. **6-Part Pitch & Proposal Script Generator (`PitchScriptGenerator`)**:
   - Generates tailored IG/FB DMs, Upwork/Rescue Proposals, Cold Emails, 90-Second Loom Teleprompter Scripts, Plain-English Scope & Security Agreements, and Post-Delivery Testimonial & Referral requests.
4. **100 Vibe Coding Agent Guardrails Explorer (`VibeCodingGuardrailsCatalog`)**:
   - Interactive search, category filter (Categories I–X, Rules 1–100), and 1-tap system prompt exporter for `CLAUDE.md`, `.cursorrules`, `.windsurfrules`, and Markdown checklists.
5. **30-Day Guest Account to Permanent Business Account Lifecycle + Offline Indicator**:
   - **Days 1–19**: Frictionless on-device Guest Mode via `LocalGuestSessionManager`.
   - **Days 20–30**: Dismissible milestone prompt to bind a permanent Google Business Account.
   - **Day 31+**: Required upgrade gate that automatically migrates all local prospects, leads, playbook progress, and business profile to Cloud Firestore (`migrateGuestDataToCloud`).
   - **Visual Offline Status Dot**: Top-bar status dot (`StorageSyncStatusDotChip`) and banner (`OfflineStorageNoticeBanner`) indicate when the device is offline and saving to local storage vs. Cloud Firestore.

---

## 4. Automated Verification & Production Test Suites (31 Tests)

All test suites reside under `app/src/test/java/com/gitinclo/vibepitch/`:

- **`PitchAndPlaybookValidationTest.kt` (9 tests)**: Validates all 7 `NicheKey` catalogs, `QuotePricingCalculator` math, `PitchScriptGenerator` across all 35 `NicheKey × OfferArchetype` combinations, `PipelineStage` transitions, `InputBoundsValidator` bounds, and the 100-rule `VibeCodingGuardrailsCatalog`.
- **`AuthAndGuestUpgradeTest.kt` (6 tests)**: Validates the 30-day Guest lifecycle (`Day 1`, `Day 20`, `Day 31`), end-to-end Guest-to-Business-Account migration staging & consumption, offline-to-local-storage fallback routing in `VibePitchViewModel`, and Compose UI rendering of `StorageSyncStatusDotChip`, `OfflineStorageNoticeBanner`, and `VibeGuardrailsExplorerCard`.
- **`VibePitchRepositoryTest.kt` (5 tests)**: Validates `AuthGuardState` evaluation (`Authenticated`, `GuestMode`, `Unauthenticated`), `DataResult<T>` conversion, safe empty Flow emissions for blank user IDs, and non-crashing `UnauthenticatedSessionException` failure results across all repository write operations.
- **`VibePitchRepositoryRuleTest.kt` (6 tests) & `firestore.test.js`**: Validates zero-trust owner-isolated `firestore.rules` and repository CRUD operations against the Firebase Local Emulator Suite.
- **`RealmeHardwareOptimizationTest.kt` (5 tests)**: Validates `DimensityCpuScheduler` A55 background worker threads and UFS 2.1 sequential I/O threads, 500ms-throttled frame telemetry sampling, `THERMAL_STATUS_MODERATE` 60Hz + 720p fallback transitions, non-blocking 4GB RAM cache eviction, and `Realme8HardwareHudDialog` Compose interactions.

---

## 5. Build & Test Commands

```bash
# 1. Run Tier 1 Firestore Security Rules Emulator Tests
node --test firestore.test.js

# 2. Run Tier 2 Full Kotlin Unit, Robolectric & Firebase Emulator Suite (31 Tests)
firebase emulators:exec --only auth,firestore "gradle :app:testDebugUnitTest"

# 3. Run Individual Production Test Suites
gradle :app:testDebugUnitTest \
  --tests "com.gitinclo.vibepitch.PitchAndPlaybookValidationTest" \
  --tests "com.gitinclo.vibepitch.AuthAndGuestUpgradeTest" \
  --tests "com.gitinclo.vibepitch.VibePitchRepositoryTest" \
  --tests "com.gitinclo.vibepitch.RealmeHardwareOptimizationTest"

# 4. Assemble Debug APK
gradle :app:assembleDebug
```

---

## 6. Compliance & Governance Documentation

All repository governance, cybersecurity, and privacy documents are available in the root directory and inside the app via the **Docs & Compliance** button:
- [`SECURITY.md`](./SECURITY.md) — Vulnerability reporting, encryption, and zero-trust Firestore architecture.
- [`PRIVACY.md`](./PRIVACY.md) — Global privacy compliance (150+ jurisdictions as of October 10, 2026).
- [`TERMS_AND_CONDITIONS.md`](./TERMS_AND_CONDITIONS.md) — Commercial usage terms, white-label demo disclaimers, and liability limits.
- [`ACCOUNT.md`](./ACCOUNT.md) — 30-day Guest lifecycle, Google OAuth binding, RBAC, and account deletion flows.
- [`CONTRIBUTING.md`](./CONTRIBUTING.md) — Branching workflow, commit standards, and PR verification requirements.
- [`CODE_OF_CONDUCT.md`](./CODE_OF_CONDUCT.md) — Contributor Covenant v2.1 community guidelines.
- [`ROADMAP.md`](./ROADMAP.md) — Q4 2026, 2027, and Long-Term technical milestones.
- [`GUARDRAILS.md`](./GUARDRAILS.md) — Complete 100 Vibe Coding Agent Guardrails (Categories I–X) with in-app search & `CLAUDE.md` / `.cursorrules` / `.windsurfrules` exporter.
