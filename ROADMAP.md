# Product & Technical Roadmap (`ROADMAP.md`)

**Last Updated:** Q4 2026

## 1. Short-Term Milestones (Q4 2026 — Completed & Active)

- [x] **7-Vertical Niche Catalog (`NicheCatalog`)**: Complete interactive demo presets, packages, and add-ons for `HOME_TRADES`, `SALON_AND_CLEANERS`, `DENTAL_AND_TUTORS`, `AUTO_DETAILING`, `BOUTIQUE_FITNESS`, `ECOMMERCE_SHOPIFY`, and `SAAS_AI_REMEDIATION`.
- [x] **6-Part Pitch & Scope Script Generator**: Dynamic generation of cold openers, website gap audits, demo scripts, objection handlers, scope-safe proposals, and referral scripts.
- [x] **30-Day Guest-to-Business Account Lifecycle**: Local `LocalGuestSessionManager` with Day 20–30 soft upgrade prompts, Day 31+ hard gate, Jetpack `CredentialManager` Google Sign-In, and automatic cloud migration (`migrateGuestDataToCloud`).
- [x] **Zero-Trust Firestore Rules & Input Hardening**: Strict owner isolation (`request.auth.uid == userId`), enum validation for all 7 `NicheKey` values, and client/server bounds alignment (`InputBoundsValidator`).
- [x] **Global Compliance & In-App Docs Hub**: Full suite of 9 repository governance/compliance/guardrail documents and interactive `ComplianceDocsDialog.kt` viewer inside the app.
- [x] **Production Modularization & Release Hardening (`com.gitinclo.vibepitch`)**: Decomposed feature-scoped UI packages, explicit `AuthGuardState` / `DataResult<T>` repository contracts, R8 minification & resource shrinking, and 100 Vibe Coding Agent Guardrails Explorer.
- [x] **Realme 8 5G (Dimensity 700 / 90Hz / 4GB RAM) Hardware Governor & HUD**: Cortex-A76/A55 thread scheduling (`DimensityCpuScheduler`), UFS 2.1 sequential async I/O, 500ms-throttled 90Hz/60Hz thermal display governor, non-destructive Mali-G57 MC2 scaling, and non-blocking 4GB RAM cache trim.

## 2. Mid-Term Milestones (2027)

- [ ] **PDF & Branded HTML Proposal Export (Q1 2027)**: Generate one-tap shareable PDF proposals containing the prospect's custom quote breakdown, selected add-ons, Scope Guardrail disclaimers, and embedded Stripe Payment Link QR codes.
- [ ] **Multi-Currency & Regional Tax Presets (Q1 2027)**: Support instant currency formatting and tax/GST/VAT toggles across USD (`$`), EUR (`€`), GBP (`£`), INR (`₹`), BRL (`R$`), CAD (`CA$`), and AUD (`A$`).
- [ ] **Offline-First WorkManager Cloud Sync Queue (Q2 2027)**: Enhance Permanent Business Accounts with background conflict resolution and retry telemetry when pitching in low-connectivity field environments.
- [ ] **Custom Vertical Preset Builder (Q3 2027)**: Allow authenticated Business Account users to clone any of the 7 core verticals and save custom package tiers and industry terminology in `/users/{uid}/customPresets`.
- [ ] **Playbook Analytics & Conversion Funnel Insights (Q4 2027)**: On-device and owner-isolated pipeline velocity metrics (Lead -> Pitched -> Proposal Sent -> Deposit Paid conversion rates by vertical).

## 3. Long-Term Vision (2028 & Beyond)

- [ ] **Multi-Seat Agency Team Workspaces**: Role-based team collaboration (`OWNER`, `CLOSER`, `FULFILLMENT`) with dedicated Firestore security rules for shared agency pipelines.
- [ ] **Client-Facing Interactive Micro-Preview Links**: Publish read-only, time-expiring web preview snapshots of a configured vertical pitch so prospects can review their custom package asynchronously on desktop or mobile.
- [ ] **Automated Website Gap Scanner Integration**: Optional lightweight URL metadata inspection (SSL status, mobile viewport check, booking CTA detection) to pre-populate the `websiteGapNote` field automatically.
