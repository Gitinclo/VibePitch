# VibePitch Studio — 5-Phase Production-Grade Refactoring & Hardening Plan

This plan upgrades VibePitch Studio from an MVP/prototype structure into a production-hardened Android architecture by eliminating starter namespace leftovers, decomposing centralized UI/ViewModel modules into feature-scoped packages, hardening unauthenticated/offline repository flows, enforcing release R8/ProGuard & Firebase configuration checks, and establishing dedicated end-to-end automated test suites.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> All three architectural decisions below have been confirmed and will govern the 5-phase execution without regressing any existing business, guest lifecycle, or multi-stream sales functionality.

- **Confirmed Decision 1 — Namespace Migration & Firebase Compatibility**: Refactor the Kotlin package namespace to `com.gitinclo.vibepitch` across the entire codebase and remove all legacy `com.example` directories, while retaining `applicationId = "com.aistudio.vibepitch.kxpqmr"` in Gradle so the provisioned Firebase `google-services.json` client continues matching without error.
- **Confirmed Decision 2 — Feature-Scoped UI & Navigation Decomposition**: Extract top-level routing and app shell orchestration out of the root activity into a dedicated navigation graph and app shell, and organize screen/component modules into feature-scoped subpackages (`livedemo`, `playbook`, `crm`, `studio`, `auth`, and `compliance`).
- **Confirmed Decision 3 — Production Test Suite Alignment**: Provide the exact requested test suites (`AuthAndGuestUpgradeTest`, `VibePitchRepositoryTest`, and `PitchAndPlaybookValidationTest`) alongside the live Firebase Auth & Firestore Emulator security rule suite (`VibePitchRepositoryRuleTest`).

---

### 1. Overview & Core Concept

- **What It Does**: VibePitch Studio is a mobile sales, white-label demo, and client-conversion workspace for agencies and technical freelancers across 7 turnkey niches and 3 revenue streams (Local Business Booking/Quote Funnels, Upwork/Shopify Micro-Tools, and AI Prototype Remediation).
- **Target Audience / Persona**: Freelance developers, boutique agencies, and technical consultants who audit local or SaaS prospects, present live interactive quote/deposit demos, generate scope-guarded proposals, and manage a 30-day Guest-to-Permanent Business Account CRM pipeline.
- **Key Value**: Delivers a crash-resilient, offline-aware, zero-trust Firestore architecture with clean separation between UI presentation, domain validation/calculators, and local/cloud persistence.

---

### 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Zero-State & 30-Day Guest Onboarding**: Users launch into an interactive authentication gate where they can sign in with Google via Jetpack Credential Manager or start a 30-day local Guest Account with pre-seeded niche prospects and a 14-day outreach checklist.
  2. **Multi-Stream Live Demo & Instant Quote Calculator**: Users switch between 7 niche presets or 3 revenue stream modes, adjust service packages, unit sliders, and add-ons, and submit real-time customer booking/deposit leads.
  3. **15-Second White-Label Brand Studio**: Users customize a prospect's business name, contact name, brand color swatch, deposit percentage, setup fee, and monthly retainer, then launch directly into Presentation Mode.
  4. **Prospect CRM & Captured Leads Inbox**: Users track prospects across pipeline stages (`AUDITED` → `DEMO_READY` → `LOOM_SENT` → `FOLLOW_UP` → `CLOSED_WON`), share captured leads, and inspect real-time storage status via the header status dot and offline local-storage banner.
  5. **Playbook, Pitch Scripts & 100 Vibe Coding Guardrails**: Users generate 6-part pitch scripts (Loom script, DM opener, 1-page proposal, follow-up sequence, referral ask, and scope guardrail contract), browse/filter all 100 Vibe Coding Agent Guardrails across Categories I–X, and export `.cursorrules` / `CLAUDE.md` bundles.
  6. **Day 20 Reminder & Day 30 Upgrade Gate**: Users stage their permanent `BusinessAccountProfile` and authenticate with Google to atomically migrate all local guest prospects, leads, and playbook tasks to Cloud Firestore.
- **Visual Identity & Theme**:
  - *Aesthetic Direction*: High-craft dark-first executive studio (`VibePitchTheme`) combining deep slate surfaces with high-contrast conversion accents.
  - *Color Palette & Mood*:
    - `--background`: `#0B1120` (Obsidian Slate) in Dark Mode / `#F8FAFC` (Warm Ivory) in Light Mode.
    - `--surface`: `#151F32` (Deep Slate Surface) / `#1E293B` (Elevated Slate Card).
    - `--primary`: `#4F46E5` (Electric Indigo) with `#818CF8` highlights.
    - `--secondary` / `--accent`: `#10B981` (Emerald Conversion) for ROI metrics, cloud-synced status dots, and primary conversion CTAs; `#F59E0B` (Amber Highlight) for offline/local-storage indicators and Day 20/30 lifecycle prompts; `#EF4444` (Coral Error) for validation alerts.
  - *Typography & Hierarchy*: Distinct geometric display headlines (`Plus Jakarta Sans` / `Space Grotesk` styling) paired with legible body copy (`Inter`) and tabular numeric badges for pricing, deposit math, and pipeline value metrics.
  - *Component Styling & Layout*: Material Design 3 adaptive layouts (`widthIn(max = 640.dp)`), `16.dp`–`24.dp` rounded cards with subtle accent borders, interactive filter chips, and full edge-to-edge `WindowInsets.safeDrawing` / `WindowInsets.navigationBars` handling.
- **Interactive Feedback & Motion**:
  - Real-time animated storage status dot (`Cloud Synced`, `Local Guest Storage`, `Offline • Saving Locally`) with a tap-to-inspect status snackbar and prominent offline banner.
  - Instant clipboard copy confirmations, lead booking receipt dialogs, and non-blocking snackbar feedback on every CRUD or stage-transition action.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Safe Repository Return Contracts (`Result<T>` & `AuthGuardState`) Instead of Throwing `requireUserId()`**
  - *Chosen Approach*: Replace all throwing user-ID assertions with explicit `AuthGuardState` checks and `Result<T>` return values for writes, plus safe empty/null `Flow` emissions when unauthenticated or when Firebase is uninitialized in local JVM tests.
  - *Why*: Prevents unhandled `IllegalStateException` crashes during sign-out transitions, expired sessions, or unconfigured local test runs, allowing the UI to display a clean sign-in or offline fallback state.
  - *Alternatives Considered*: Catching exceptions inside the UI layer was rejected because uncaught exceptions in upstream coroutine flows can still terminate collectors or crash background scopes.
- **Decision 2: Feature-Scoped Package Modularity & Delegated Domain Coordinators**
  - *Chosen Approach*: Decompose the presentation layer into `ui/navigation/`, `ui/VibePitchApp.kt`, and `ui/features/{livedemo,playbook,crm,studio,auth,compliance}` while extracting domain validation (`InputBoundsValidator`), quote math (`QuotePricingCalculator`), script generation (`PitchScriptGenerator`), and offline/cloud persistence routing (`VibePitchWorkspaceCoordinator`) into dedicated domain modules.
  - *Why*: Eliminates God-Activity and God-ViewModel anti-patterns, keeps every Kotlin file focused and under 500 lines, and makes unit testing independent of Android UI lifecycle.
  - *Alternatives Considered*: Keeping all screens in a single flat package was rejected because feature-scoped packages scale better and directly satisfy architectural review criteria.
- **Decision 3: Release R8 Minification + Fail-Fast Google Services Strategy**
  - *Chosen Approach*: Enable `isMinifyEnabled = true` and `isShrinkResources = true` on the `release` build type with comprehensive `@Keep` and ProGuard rules for Firestore models, Credential Manager, and Coroutines, paired with `MissingGoogleServicesStrategy.ERROR`.
  - *Why*: Ensures production release builds strip unused resources, obfuscate code safely without breaking Firestore reflection, and fail fast if Firebase configuration is missing.

---

### 4. Technical Architecture & Data Strategy *(Technical Reference)*

#### Architecture & Component Diagram

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        MainActivity (Minimal Host)                         │
│             enableEdgeToEdge() + VibePitchTheme + VibePitchNavGraph        │
└─────────────────────────────────────┬──────────────────────────────────────┘
                                      │
                                      ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                   VibePitchNavGraph & VibePitchApp Shell                   │
│  • Auth Gate (Google CredentialManager vs. 30-Day Local Guest Workspace)   │
│  • Top Bar with StorageSyncStatusDotChip & OfflineStorageNoticeBanner      │
│  • Bottom Navigation Bar & Presentation Mode Chrome Toggle                 │
└──────┬──────────────┬───────────────┬───────────────┬───────────────┬──────┘
       │              │               │               │               │
       ▼              ▼               ▼               ▼               ▼
┌────────────┐ ┌────────────┐ ┌──────────────┐ ┌────────────┐ ┌──────────────┐
│ LiveDemo   │ │ Brand      │ │ Prospect     │ │ Playbook   │ │ Compliance & │
│ Feature    │ │ Studio     │ │ CRM & Leads  │ │ & Pitch    │ │ Guardrails   │
│ Package    │ │ Feature    │ │ Feature      │ │ Feature    │ │ Explorer     │
└──────┬─────┘ └──────┬─────┘ └──────┬───────┘ └──────┬─────┘ └──────┬───────┘
       └──────────────┴──────────────┼────────────────┴──────────────┘
                                     │
                                     ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                VibePitchViewModel + Delegated State Holders                │
│  • ActiveDemoBrandState, QuoteCalculatorState, ScopeGuardrailConfig        │
│  • Device Connectivity StateFlow & GuestLifecycleStatus StateFlow          │
└─────────────────┬───────────────────────────────────────┬──────────────────┘
                  │                                       │
                  ▼                                       ▼
┌───────────────────────────────────────┐ ┌──────────────────────────────────┐
│          Domain Layer                 │ │     Persistence & Auth Layer     │
│  • VibePitchWorkspaceCoordinator      │ │  • VibePitchRepository (Cloud)   │
│  • InputBoundsValidator               │ │  • LocalGuestSessionManager      │
│  • QuotePricingCalculator             │ │  • GuestSessionJsonCodec         │
│  • PitchScriptGenerator               │ │  • FirebaseEnvValidator          │
│  • VibeCodingGuardrailsCatalog (100)  │ │  • GoogleAuthHelper              │
└───────────────────────────────────────┘ └──────────────────────────────────┘
```

#### Data Model & State
- **`ProspectProfile`**: Represents a target client business (`id`, `userId`, `businessName` <= 100 chars, `contactName` <= 100 chars, `nicheKey` across 7 `NicheKey` enums, `websiteGapNote` <= 300 chars, `brandColorHex`, `tagline` <= 150 chars, `depositPercent` 0..100, `setupFeeOffer` 0..50000, `monthlyRetainerOffer` 0..5000, `stage`, `createdAt`, `updatedAt`).
- **`CapturedLead`**: Represents an instant quote or booking lead submitted through the Live Demo (`id`, `userId`, `prospectId`, `prospectBusinessName`, `nicheKey`, `customerName`, `customerPhone`, `customerEmail`, `preferredDate`, `selectedServiceName`, `selectedAddOns`, `unitQuantity`, `estimatedTotal`, `depositAmount`, `notes` <= 500 chars, `createdAt`).
- **`PlaybookTask`**: Represents a step in the 14-day outreach checklist (`id`, `userId`, `dayRange`, `stepNumber`, `title`, `description`, `proTip`, `isCompleted`, `updatedAt`).
- **`BusinessAccountProfile`**: Represents the user's agency/freelance profile (`id = "profile"`, `userId`, `agencyName`, `ownerName`, `businessEmail`, `businessPhone`, `primaryNicheKey`, `websiteUrl`, `tagline`, `upgradedFromGuest`, `createdAt`, `updatedAt`).
- **`AuthGuardState`**: Explicit authentication guard state (`Authenticated(uid)`, `GuestMode(guestId)`, `Unauthenticated(reason)`) used by `VibePitchRepository` to prevent unauthenticated crashes.

#### Interactive Component & State Mapping
1. **Phase 1 (Namespace Migration to `com.gitinclo.vibepitch`)**:
   - Update `namespace = "com.gitinclo.vibepitch"` in Gradle while preserving `applicationId = "com.aistudio.vibepitch.kxpqmr"`.
   - Move all main and test Kotlin sources from `com/example` into `com/gitinclo/vibepitch`, update all package/import statements, and delete empty `com/example` directories.
2. **Phase 2 (Auth & Repository Hardening)**:
   - Introduce `AuthGuardState` in `VibePitchRepository` to validate active user sessions and Firebase initialization before any Firestore read or write.
   - Return structured `Result.failure` with descriptive domain exceptions when unauthenticated, and route offline/guest mutations cleanly through `VibePitchWorkspaceCoordinator` to `LocalGuestSessionManager`.
3. **Phase 3 (Navigation, App Shell & Feature-Package Decomposition)**:
   - Extract `ui/navigation/VibePitchNavGraph.kt` and `ui/VibePitchApp.kt` so `MainActivity.kt` contains only edge-to-edge initialization, `VibePitchTheme`, and `VibePitchNavGraph()`.
   - Organize UI screens and subcomponents into `ui/features/livedemo/`, `ui/features/studio/`, `ui/features/crm/`, `ui/features/playbook/`, `ui/features/auth/`, and `ui/features/compliance/`.
4. **Phase 4 (Build, R8/ProGuard & Google Services Hardening)**:
   - Maintain `isMinifyEnabled = true` and `isShrinkResources = true` on `release`, update `proguard-rules.pro` for `com.gitinclo.vibepitch.**` model classes, and enforce fail-fast Google Services validation.
5. **Phase 5 (Production Test Suites)**:
   - Implement `AuthAndGuestUpgradeTest.kt` (guest initialization, Day 1/20/31 lifecycle, retry-safe Guest-to-Business Account cloud migration, and unauthenticated guard checks).
   - Implement `VibePitchRepositoryTest.kt` (missing/blank userId safety, uninitialized Firebase fallback, and offline/online repository error handling).
   - Implement `PitchAndPlaybookValidationTest.kt` (input bounds clamping across all `firestore.rules` constraints, quote/deposit math, 7 niche presets, 6-part script generation, 100 Vibe Coding Agent Guardrails, and ViewModel state transitions).
   - Retain and update `VibePitchRepositoryRuleTest.kt` under `com.gitinclo.vibepitch` for live Firebase Emulator security rule verification.
