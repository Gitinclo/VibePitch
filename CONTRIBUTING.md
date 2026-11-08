# Contributing to VibePitch Studio (`CONTRIBUTING.md`)

We welcome contributions that improve VibePitch Studio's vertical demo presets, pitch script generators, Jetpack Compose UI, and Firestore security rules.

## 1. Branching Strategy

- `main`: Protected production branch. All merges require passing Tier 1 (Firestore Emulator) and Tier 2 (Gradle Unit + Robolectric) test suites.
- `feat/<short-slug>`: New vertical features, script enhancements, or UI screens.
- `fix/<short-slug>`: Bug fixes, bounds hardening, or coroutine cancellation fixes.
- `docs/<short-slug>`: Documentation, compliance, and roadmap updates.

## 2. Conventional Commit Format

Follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:
- `feat(catalog): add seasonal upsell add-on to AUTO_DETAILING preset`
- `fix(viewmodel): clamp setupFeeOffer to 50000 before Firestore write`
- `test(rules): verify all 7 NicheKey values in firestore.test.js`
- `docs(privacy): align PRIVACY.md with October 2026 global frameworks`

## 3. Coding Standards & Architectural Guardrails

1. **Kotlin & Jetpack Compose**:
   - Target JDK 17 and maintain compatibility with `minSdk 24` through `compileSdk 36`.
   - Lift UI state to `VibePitchViewModel` via immutable `StateFlow` snapshots; keep Compose screen composables stateless wherever possible.
   - Never swallow `CancellationException` inside `try/catch` blocks within coroutines.
2. **Firestore Schema Synchronization**:
   - Any change to model fields in `VibePitchModels.kt` **must** be accompanied by matching bounds validation in `InputBoundsValidator.kt`, `firestore.rules`, and `firestore.test.js`.
3. **Zero Raw Payment/PII Handling**:
   - Do not introduce SDKs or UI fields that collect raw credit card numbers, SSNs/Aadhaar numbers, or passwords. Always use the Scope Guardrail pattern (external Stripe Payment Links, external intake forms, and managed Google OAuth).

## 4. Local Setup & Pre-PR Verification Checklist

Before opening a Pull Request, run the complete verification suite locally:

```bash
# Step 1: Verify Firestore Security Rules against the emulator
node --test firestore.test.js

# Step 2: Run Unit & Robolectric Tests
gradle :app:testDebugUnitTest

# Step 3: Verify Debug APK Compilation
gradle :app:assembleDebug
```

All Pull Requests must include:
- A concise summary of architectural changes.
- Updated unit or Robolectric tests in `ExampleUnitTest.kt` / `ExampleRobolectricTest.kt` covering new functions and edge cases.
- Screenshots or screen recordings for any Jetpack Compose UI modifications.
