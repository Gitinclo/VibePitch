# Security Policy & Architecture (`SECURITY.md`)

**Effective Date:** October 10, 2026  
**Status:** Active Enforcement

## 1. Supported Versions

Security patches, dependency updates, and Firestore rule updates are actively maintained for the following release branches:

| Version | Android API Level | Support Status |
| :--- | :--- | :--- |
| `1.1.x` (Current) | API 24 – API 36 | Actively Supported |
| `1.0.x` | API 24 – API 35 | Critical Security Fixes Only |
| `< 1.0.0` | Legacy | Unsupported |

## 2. Zero-Trust Firestore Security Architecture

VibePitch Studio enforces strict document-level isolation and schema validation in `firestore.rules`:

1. **Default-Deny Posture**: All paths not explicitly matched in `firestore.rules` are rejected unconditionally.
2. **Strict Owner Isolation**: Every user document and subcollection (`prospects`, `leads`, `playbook`, `businessProfile`) resides under `/users/{userId}` and requires `request.auth != null && request.auth.uid == userId`.
3. **Server-Side Schema & Bounds Validation**:
   - `nicheKey` is strictly validated against the 7 allowed vertical constants (`HOME_TRADES`, `SALON_AND_CLEANERS`, `DENTAL_AND_TUTORS`, `AUTO_DETAILING`, `BOUTIQUE_FITNESS`, `ECOMMERCE_SHOPIFY`, `SAAS_AI_REMEDIATION`).
   - String lengths are capped at the rule layer (`businessName <= 100`, `contactName <= 100`, `websiteGapNote <= 300`, `tagline <= 150`, `notes <= 500`) to prevent payload bloat and storage exhaustion attacks.
   - Financial fields are bounded (`depositPercent` in `0..100`, `setupFeeOffer` in `0..50000`, `monthlyRetainerOffer` in `0..5000`).

## 3. Payment & PII Offloading Guardrails

By architectural design, VibePitch Studio **never** processes, transmits, or stores raw payment card numbers (PAN), CVV codes, bank routing numbers, or sensitive end-customer health/financial records:
- **Payments**: Offloaded exclusively to PCI-DSS Level 1 compliant external links (e.g., Stripe Payment Links).
- **Customer Intake**: Offloaded to dedicated external form providers (e.g., Google Forms, Typeform, Calendly).
- **Authentication**: Handled via AndroidX `CredentialManager` and Firebase Authentication using cryptographic OAuth 2.0 / OpenID Connect ID tokens.

## 4. Encryption Standards

- **Data in Transit**: All communication with Firebase Authentication and Cloud Firestore is encrypted via TLS 1.3.
- **Data at Rest (Cloud)**: All Firestore documents are encrypted at rest using AES-256 managed by Google Cloud Platform.
- **Data at Rest (On-Device Guest Mode)**: Guest session data resides within the private Android application sandbox (`/data/data/com.aistudio.vibepitch.kxpqmr/shared_prefs/`), protected by Android OS file-based encryption (FBE) and UID sandboxing.

## 5. Vulnerability Disclosure & Incident Response SLAs

If you discover a security vulnerability in VibePitch Studio or its `firestore.rules`, do **not** open a public GitHub issue. Report it privately via email to `security@vibepitch.studio` with:
- A clear description of the vulnerability and affected component.
- Steps to reproduce or a proof-of-concept test case (`firestore.test.js` or Robolectric test).

### Response Timeline SLAs
- **Initial Acknowledgment**: Within **24 hours** of receipt.
- **Triage & Severity Assessment (CVSS v4.0)**: Within **72 hours**.
- **Critical/High Patch Deployment**: Within **7 days**.
- **Medium/Low Patch Deployment**: Within **30 days**.
