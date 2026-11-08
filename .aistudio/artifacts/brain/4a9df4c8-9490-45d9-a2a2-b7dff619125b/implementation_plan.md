# VibePitch Studio — 4-Phase Engineering, Testing & Global Compliance Execution Plan

This 4-phase engineering, testing, and global compliance execution covers the **VibePitch Studio** repository architecture in full detail without skipping any step.

---

## Phase 1: Codebase Audit, Bug Fixing & Refactoring

### 1. Static & Runtime Audit Summary
- **Inspected Modules**: Audit `MainActivity.kt`, `VibePitchViewModel.kt`, `VibePitchRepository.kt`, `LocalGuestSessionManager.kt`, `GoogleAuthHelper.kt`, `VibePitchModels.kt`, and all Jetpack Compose screens for unused imports, compiler warnings, null-safety edge cases, input bounds, and coroutine cancellation handling.
- **Schema & Bounds Hardening**: Enforce character length and numeric bounds in `VibePitchViewModel` and `LocalGuestSessionManager` so every prospect, lead, playbook task, and business profile created locally in Guest Mode passes `firestore.rules` schema validation during Day 20 or Day 30 cloud migration.
- **Legacy Test Cleanup**: Replace template placeholder tests in `ExampleUnitTest.kt` and `ExampleRobolectricTest.kt` with full domain, session lifecycle, and ViewModel test suites.

### 2. Input Sanitization & Schema Validation Utility (`InputBoundsValidator.kt`)
Create `/app/src/main/java/com/example/domain/InputBoundsValidator.kt` used by both `VibePitchViewModel.kt` and `LocalGuestSessionManager.kt` to enforce the exact `firestore.rules` constraints (`businessName <= 100`, `contactName <= 100`, `websiteGapNote <= 300`, `tagline <= 150`, `notes <= 500`, `depositPercent in 0..100`, `setupFeeOffer in 0..50000`, `monthlyRetainerOffer in 0..5000`):

```kotlin
package com.example.domain

object InputBoundsValidator {
    const val MAX_BUSINESS_NAME_LEN = 100
    const val MAX_CONTACT_NAME_LEN = 100
    const val MAX_WEBSITE_GAP_NOTE_LEN = 300
    const val MAX_TAGLINE_LEN = 150
    const val MAX_NOTES_LEN = 500

    const val MIN_DEPOSIT_PERCENT = 0
    const val MAX_DEPOSIT_PERCENT = 100

    const val MIN_SETUP_FEE = 0
    const val MAX_SETUP_FEE = 50_000

    const val MIN_MONTHLY_RETAINER = 0
    const val MAX_MONTHLY_RETAINER = 5_000

    fun sanitizeBusinessName(input: String): String =
        input.trim().take(MAX_BUSINESS_NAME_LEN)

    fun sanitizeContactName(input: String): String =
        input.trim().take(MAX_CONTACT_NAME_LEN)

    fun sanitizeWebsiteGapNote(input: String): String =
        input.trim().take(MAX_WEBSITE_GAP_NOTE_LEN)

    fun sanitizeTagline(input: String): String =
        input.trim().take(MAX_TAGLINE_LEN)

    fun sanitizeNotes(input: String): String =
        input.trim().take(MAX_NOTES_LEN)

    fun clampDepositPercent(value: Int): Int =
        value.coerceIn(MIN_DEPOSIT_PERCENT, MAX_DEPOSIT_PERCENT)

    fun clampSetupFeeOffer(value: Int): Int =
        value.coerceIn(MIN_SETUP_FEE, MAX_SETUP_FEE)

    fun clampMonthlyRetainerOffer(value: Int): Int =
        value.coerceIn(MIN_MONTHLY_RETAINER, MAX_MONTHLY_RETAINER)
}
```

### 3. Coroutine Cancellation & Bounds Integration in `VibePitchViewModel.kt` & `LocalGuestSessionManager.kt`
Ensure coroutine blocks rethrow `CancellationException` cleanly and apply `InputBoundsValidator` before persisting to local guest storage or Firestore (`normalizeProspectInput` and `normalizeBusinessProfileTagline`).

---

## Phase 2: Dependency & Architecture Optimization

### 1. Gradle & Version Catalog Audit (`app/build.gradle.kts` & `gradle/libs.versions.toml`)
- **SDK & Compatibility Matrix**: Preserve the verified Android SDK configuration (`compileSdk 36`, `targetSdk 36`, `minSdk 24`, `namespace = "com.example"`, `applicationId = "com.aistudio.vibepitch.kxpqmr"`) alongside the Kotlin, Jetpack Compose, and Firebase BOM version catalog.
- **Dependency Pruning**: Comment out unused third-party libraries in `app/build.gradle.kts` to reduce APK footprint and accelerate Gradle build times.
- **Deprecation Sweep**: Confirm zero deprecated Kotlin or Jetpack Compose API calls across all UI screens and helper classes.

---

## Phase 3: Comprehensive Function Testing & Validation

### 1. Unit & Domain Model Test Suite (`ExampleUnitTest.kt`)
Update `/app/src/test/java/com/example/ExampleUnitTest.kt` to test:
- `allSevenNicheKeys_haveCompleteCatalogAndValidRevenueStreamMappings` (`HOME_TRADES`, `SALON_AND_CLEANERS`, `DENTAL_AND_TUTORS`, `AUTO_DETAILING`, `BOUTIQUE_FITNESS`, `ECOMMERCE_SHOPIFY`, `SAAS_AI_REMEDIATION`)
- `pipelineStage_transitionsProgressSequentially`
- `quoteAndDepositMath_respectsFirestoreBoundsAndCalculatesAccurately`
- `scriptGenerators_produceNonBlankScriptsAcrossAllOfferArchetypesAndGuardrails`
- `inputBoundsValidator_enforcesExactFirestoreRuleLimits`

### 2. Robolectric Guest Session & ViewModel Suite (`ExampleRobolectricTest.kt`)
Update `/app/src/test/java/com/example/ExampleRobolectricTest.kt` to test:
- `guestSessionLifecycle_correctlyEvaluatesDay1_Day20_andDay31States`
- `guestCrudOperations_prospectsLeadsPlaybookAndPendingBusinessProfile` (verifying string truncation at 100/150/300/500 chars and numeric clamping at 50,000 / 5,000 / 100)

### 3. Firestore Security Rules & Emulator Test Suite (`firestore.test.js` & `VibePitchRepositoryRuleTest.kt`)
Verify all 7 `NicheKey` values (`HOME_TRADES`, `SALON_AND_CLEANERS`, `DENTAL_AND_TUTORS`, `AUTO_DETAILING`, `BOUTIQUE_FITNESS`, `ECOMMERCE_SHOPIFY`, `SAAS_AI_REMEDIATION`), unauthenticated denial, cross-user isolation, and schema bounds against the local Firestore emulator.

---

## Phase 4: Documentation & Global Compliance Suite (+ In-App Viewer)

Create all 8 root Markdown files in the repository root and add the interactive in-app `ComplianceDocsDialog.kt` viewer:
1. `/README.md` — Project overview, 7 turnkey verticals, interactive quote & scope guardrail engine, 6-part pitch & proposal script generator, 30-day Guest-to-Business Account lifecycle, tech stack, and build/test commands.
2. `/SECURITY.md` — Supported versions, zero-trust `firestore.rules` architecture, payment & PII offloading guardrails, TLS 1.3 & AES-256 encryption standards, and vulnerability disclosure SLAs (24h acknowledgment, 72h triage, 7d critical patch).
3. `/PRIVACY.md` — Global privacy policy aligned with 150+ jurisdictions as of October 10, 2026 (EU/UK GDPR, CCPA/CPRA, India DPDP Act, Brazil LGPD, China PIPL, Canada PIPEDA, Australia Privacy Act, and ISO/IEC 27701:2025), covering Guest Mode vs. Permanent Business Account processing, retention/erasure within 72 hours, cross-border transfers, and global user rights.
4. `/TERMS_AND_CONDITIONS.md` — License grant, white-label demo usage, lawful outreach obligations, external payment/PII offloading responsibilities, 30-day Guest conversion terms, IP ownership, warranty disclaimers, and dispute resolution.
5. `/ACCOUNT.md` — Dual-mode account architecture, 30-day Guest lifecycle table (`Days 1–19`, `Days 20–30`, `Day 31+`), Jetpack `CredentialManager` Google OAuth flow, `migrateGuestDataToCloud` atomic batch migration, RBAC tenant isolation, and account deletion/export procedures.
6. `/CONTRIBUTING.md` — Branching strategy, Conventional Commits format, Kotlin/Compose & Firestore schema synchronization rules, and pre-PR verification checklist.
7. `/CODE_OF_CONDUCT.md` — Contributor Covenant v2.1 standards, enforcement responsibilities, and 3-tier reporting guidelines.
8. `/ROADMAP.md` — Short-Term (Q4 2026 — Completed & Active), Mid-Term (2027), and Long-Term (2028 & Beyond) milestones.
9. `/app/src/main/java/com/example/ui/screens/ComplianceDocsDialog.kt` — Interactive **Docs & Global Compliance Hub** modal accessible from the top bar and Playbook screen so users can browse all 8 documents inside the Android app.
