# Global Privacy Policy (`PRIVACY.md`)

**Effective Date:** October 10, 2026  
**Compliance Scope:** 150+ International Jurisdictions & ISO/IEC 27701:2025 Privacy Information Management System (PIMS)

## 1. Regulatory Alignment & Global Scope

This Privacy Policy governs the collection, processing, storage, and transfer of personal data by **VibePitch Studio** in accordance with international cybersecurity and data protection laws in effect as of **October 10, 2026**, including but not limited to:
- **European Economic Area (EEA) & UK**: General Data Protection Regulation (EU GDPR) and UK Data Protection Act / UK GDPR.
- **United States**: California Consumer Privacy Act as amended by the California Privacy Rights Act (CCPA/CPRA), Virginia CDPA, Colorado CPA, Connecticut CTDPA, Texas TDPSA, and applicable state privacy statutes.
- **India**: Digital Personal Data Protection Act, 2023 (DPDP Act) and enforcing rules.
- **Brazil**: Lei Geral de Proteção de Dados Pessoais (LGPD — Law No. 13.709/2018).
- **People's Republic of China**: Personal Information Protection Law (PIPL), Cybersecurity Law (CSL), and Data Security Law (DSL).
- **Canada**: Personal Information Protection and Electronic Documents Act (PIPEDA) and Quebec Law 25.
- **Asia-Pacific, Middle East & Africa**: Australia Privacy Act 1988 (as amended), Japan APPI, Singapore PDPA, South Korea PIPA, UAE Federal Decree-Law No. 45 of 2021, Saudi Arabia PDPL, South Africa POPIA, Nigeria NDPA, and over 130 additional national data protection frameworks aligned with the Berne/OECD/Council of Europe Convention 108+ principles.

## 2. Categories of Data Processed

### A. On-Device Guest Account Mode (Days 1–30)
When operating in Guest Mode prior to Google Sign-In:
- **Data Collected**: Local prospect business names, prospect contact names, website gap notes, quote configurations, playbook checklist toggles, and installation timestamp (`installEpochMillis`).
- **Storage Location**: Stored strictly locally on your Android device via `LocalGuestSessionManager`. **Zero personal data is transmitted to cloud servers during Guest Mode.**

### B. Permanent Business Account Mode (Authenticated via Google Sign-In)
When you link your Google Account via Android `CredentialManager`:
- **Authentication Identifiers**: Firebase User UID (`uid`), email address, and display name provided via Google OAuth consent.
- **Cloud-Synced Business Records**: Agency branding profile (`BusinessAccountProfile`), B2B prospect pipeline entries, captured lead notes, and playbook progress stored under `/users/{uid}` in Firebase Cloud Firestore.

### C. Prohibited Sensitive Personal Data
VibePitch Studio does not request, collect, or process Sensitive Personal Data / Special Category Data (such as biometric identifiers, health records, government ID numbers, payment card credentials, or precise background geolocation). Users are prohibited from entering sensitive personal data into free-text notes fields.

## 3. Lawful Basis for Processing

Depending on your jurisdiction, we process personal data under the following legal bases:
1. **Performance of a Contract**: Providing local and cloud-synced sales pipeline, quoting, and pitch script functionality requested by the user.
2. **Explicit Consent**: Obtained when you initiate Google Sign-In or trigger Guest-to-Cloud migration (`migrateGuestDataToCloud`).
3. **Legitimate Interests**: Maintaining application security, enforcing `firestore.rules` tenant isolation, and preventing abuse.

## 4. Data Retention & Deletion

- **Guest Mode Data**: Retained solely on-device until cleared by the user, uninstalled, or migrated to a Permanent Business Account.
- **Permanent Business Account Data**: Retained in Firebase Cloud Firestore only while your account remains active.
- **Immediate Erasure**: Users may delete individual prospects/leads at any time or execute a full account and cloud data deletion (`ACCOUNT.md`), which permanently purges `/users/{uid}` and all nested subcollections within **72 hours** (and from encrypted backups within **30 days**).

## 5. International Data Transfers

Cloud data synced via Firebase Cloud Firestore is processed in Google Cloud data centers certified under **ISO/IEC 27001**, **ISO/IEC 27018**, and **ISO/IEC 27701**. Cross-border transfers rely on:
- **EU-U.S. Data Privacy Framework (DPF)**, UK Extension, and Swiss-U.S. DPF.
- **European Commission Standard Contractual Clauses (SCCs)** and UK International Data Transfer Addendum (IDTA).
- Standard contractual transfer mechanisms recognized under India DPDP Act, Brazil LGPD, and ASEAN Model Contractual Clauses.

## 6. Your Global Privacy Rights

Regardless of your country of residence, VibePitch Studio extends the highest tier of statutory privacy rights to all users:
- **Right of Access & Portability**: Export your complete profile, prospects, and leads in structured JSON format.
- **Right to Rectification**: Edit any prospect, lead, or agency profile field directly in the app.
- **Right to Erasure ("Right to be Forgotten")**: Clear local guest storage or permanently delete your cloud account and all associated records.
- **Right to Withdraw Consent & Restrict Processing**: Sign out or revoke Google OAuth permissions at any time via Google Account Security settings.
- **Right to Non-Discrimination & Automated Decision Opt-Out**: VibePitch Studio does not sell personal data, share data for cross-context behavioral advertising, or perform automated profiling with legal effects.

## 7. Data Protection Officer (DPO) & Grievance Officer Contact

For privacy inquiries, Data Subject Access Requests (DSARs), or India DPDP Act Grievance Redressal:
- **Email**: `privacy@vibepitch.studio`
- **Response SLA**: Acknowledgment within **48 hours**; full resolution within **15 calendar days** (or shorter where mandated by local statute).
