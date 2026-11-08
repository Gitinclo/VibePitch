# Account Lifecycle, Authentication & Access Control (`ACCOUNT.md`)

**Last Updated:** October 10, 2026

## 1. Dual-Mode Account Architecture

VibePitch Studio implements a progressive onboarding model designed to deliver immediate utility on Day 1 while ensuring durable cloud backup and multi-device continuity for long-term commercial users:

1. **Local Guest Account (`LocalGuestSessionManager`)**: Instant on-device workspace requiring zero registration.
2. **Permanent Business Account (`GoogleAuthHelper` + `VibePitchRepository`)**: Cloud-backed workspace authenticated via Google OAuth and isolated in Firebase Cloud Firestore.

## 2. The 30-Day Guest Account Lifecycle

The guest lifecycle is calculated deterministically from the initial installation timestamp (`installEpochMillis`):

| Lifecycle Window | Day Count | UI & Functional Behavior |
| :--- | :--- | :--- |
| **Active Guest Window** | Days 1 – 19 | Full local access to all 7 vertical demos, quote calculator, pitch scripts, prospect pipeline, and playbook checklist. |
| **Soft Upgrade Window** | Days 20 – 30 | Displays a dismissible upgrade banner reminding the user to link their Google Account and stage their `BusinessAccountProfile`. |
| **Required Upgrade Gate** | Day 31+ | Displays a required upgrade modal gating further local mutations until the user completes Google Sign-In, automatically migrating all guest data to the cloud. |

## 3. Authentication Flow (Jetpack `CredentialManager`)

1. The user taps **Upgrade to Business Account** or **Sign in with Google**.
2. `GoogleAuthHelper` invokes the AndroidX `CredentialManager` API with `GetSignInWithGoogleOption`.
3. Upon user consent, Google returns a signed OIDC ID Token.
4. `VibePitchRepository` exchanges the Google ID Token with `FirebaseAuth.getInstance().signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))`.
5. Once authenticated, `FirebaseAuth.currentUser.uid` becomes the immutable owner identifier (`userId`).

## 4. Automatic Guest-to-Cloud Data Migration (`migrateGuestDataToCloud`)

Immediately after a Guest user authenticates with Google for the first time, `VibePitchRepository.migrateGuestDataToCloud()` executes an atomic batch migration:
1. Reads all local prospects, captured leads, playbook task states, and any staged `BusinessAccountProfile` from `LocalGuestSessionManager`.
2. Sanitizes every string and numeric field through `InputBoundsValidator` to guarantee compliance with `firestore.rules`.
3. Writes all records into `/users/{uid}/businessProfile/profile`, `/users/{uid}/prospects/{prospectId}`, `/users/{uid}/leads/{leadId}`, and `/users/{uid}/playbook/{taskId}`.
4. Marks the local guest migration flag as completed and clears redundant staging buffers.

## 5. Role-Based Access Control (RBAC) & Tenant Isolation

VibePitch Studio enforces strict single-tenant owner isolation via Firebase Security Rules (`firestore.rules`):
- **Owner Role (`request.auth.uid == userId`)**: Full read, create, update, and delete privileges on documents inside `/users/{userId}/**`, subject to schema validation rules.
- **Cross-Tenant Users (`request.auth.uid != userId`)**: Zero read or write access. Every cross-user query or mutation is rejected at the Firestore edge.
- **Unauthenticated Requests (`request.auth == null`)**: Rejected across all cloud collections.

## 6. Account Deletion & Data Export Procedures

### Exporting Your Account Data
- Users can copy individual or complete pipeline summaries and pitch scripts directly to the Android clipboard or share sheet at any time.

### Deleting Local Guest Data
- In Guest Mode, open **Playbook / Settings -> Clear Local Guest Workspace** (or clear application storage in Android Settings) to immediately wipe all on-device `SharedPreferences` records.

### Deleting Your Permanent Business Account
1. While signed in, select **Account -> Delete Permanent Business Account** or email `privacy@vibepitch.studio` from your registered Google email address.
2. The application deletes all documents under `/users/{uid}` (`prospects`, `leads`, `playbook`, `businessProfile`), revokes the Firebase Authentication user record, and resets the local device workspace.
