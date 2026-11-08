# VibePitch Studio

**VibePitch Studio** is an Android mobile sales enablement, live white-label pitch studio, quote calculator, and pipeline management platform built with Kotlin, Jetpack Compose, and Firebase Firestore. It empowers solo agencies, freelance developers, and AI remediation consultants to pitch, scope, quote, and close clients across 7 high-margin verticals without writing fragile backend payment or PII handling code.

## Core Capabilities & Multi-Stream Architecture

1. **7 Turnkey Vertical Niches (`NicheKey`)**:
   - `HOME_TRADES`: HVAC, plumbing, roofing, and electrical dispatch & instant quote funnels.
   - `SALON_AND_CLEANERS`: Recurring appointment booking and SMS reminder micro-sites.
   - `DENTAL_AND_TUTORS`: High-trust patient/student intake and scheduling portals.
   - `AUTO_DETAILING`: Tiered ceramic coating & mobile detailing package configurators.
   - `BOUTIQUE_FITNESS`: Class pass, trial membership, and trainer booking funnels.
   - `ECOMMERCE_SHOPIFY`: Conversion rate optimization, speed remediation, and bundle upsells.
   - `SAAS_AI_REMEDIATION`: Vibe-coded app auditing, security rules hardening, and production refactoring.

2. **Interactive Quote & Scope Guardrail Engine**:
   - Real-time calculation of Setup Fees (`0–50,000`), Monthly Retainers (`0–5,000`), and Deposit Splits (`0%–100%`).
   - Built-in **Scope Guardrails** enforcing third-party offloading for payments (Stripe Payment Links), PII intake (Google Forms / Typeform), and authentication (Managed OAuth / Firebase Auth).

3. **6-Part Pitch & Proposal Script Generator**:
   - Automatically generates tailored Cold Openers, Website Gap Audits, Live Demo Walk-Throughs, Objection Handlers, Scope-Safe Proposals, and Referral Ask scripts across multiple `OfferArchetype` models.

4. **30-Day Guest Account to Permanent Business Account Lifecycle**:
   - **Days 1–19**: Frictionless on-device Guest Mode via `LocalGuestSessionManager` with zero sign-up barrier.
   - **Days 20–30**: Dismissible upgrade banner prompting users to bind a permanent Google Business Account.
   - **Day 31+**: Required upgrade gate that automatically migrates all local prospects, leads, playbook progress, and branding to Firebase Cloud Firestore (`migrateGuestDataToCloud`) upon Google Sign-In via Android Jetpack `CredentialManager`.

## Tech Stack

- **Language & UI**: Kotlin 2.x, Jetpack Compose (Material 3), Coroutines & StateFlow
- **Android SDK Matrix**: `compileSdk 36`, `targetSdk 36`, `minSdk 24`
- **Authentication**: AndroidX `CredentialManager` + Google ID Token Sign-In (`GoogleAuthHelper`)
- **Cloud Database**: Firebase Cloud Firestore with zero-trust owner-isolated `firestore.rules`
- **Local Persistence**: `SharedPreferences` JSON state engine (`LocalGuestSessionManager`)
- **Testing**: JUnit 4, Robolectric, Firebase Local Emulator Suite (`@firebase/rules-unit-testing`)

## Prerequisites & Configuration

1. **Android Studio**: Ladybug / Meerkat or newer with JDK 17 configured.
2. **Node.js**: v20+ and Firebase CLI (`npm install -g firebase-tools`) for running Firestore security rule tests.
3. **Firebase Project Configuration**:
   - Place your `google-services.json` inside `app/google-services.json`.
   - Enable **Google Sign-In** in the Firebase Authentication console and configure your Web Client ID in `strings.xml` or runtime environment configuration.

## Build & Test Commands

```bash
# 1. Run Tier 1 Firestore Security Rules Emulator Tests
node --test firestore.test.js

# 2. Run Tier 2 Kotlin Unit & Robolectric Tests
gradle :app:testDebugUnitTest

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
