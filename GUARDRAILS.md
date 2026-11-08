# 100 Vibe Coding Agent Guardrails (`GUARDRAILS.md`)

This specification defines the complete spectrum of **100 Vibe Coding Agent Guardrails** enforced across VibePitch Studio and ready to drop directly into `CLAUDE.md`, `.cursorrules`, `.windsurfrules`, or custom autonomous coding agent system prompts.

---

## I. Code Preservation & Anti-Regression (1–10)
*The "Do Not Break Working Code" Rules*

1. **No Unrequested Refactoring**: Never refactor, rename, or "clean up" working code outside the explicit scope of the current prompt.
2. **Ban Lazy Placeholders**: Never output `// ... rest of code remains the same`, `// TODO: implement`, or truncated blocks when writing files unless explicitly instructed to provide a diff.
3. **Preserve Existing Comments & Docs**: Do not strip out KDoc/JSDoc blocks, architectural notes, or `IMPORTANT:` comments left by previous sessions.
4. **Surgical Edits Only**: Modify the minimum number of lines required to achieve the requested feature or bug fix.
5. **No Silent Feature Deletion**: If a change conflicts with an existing feature, pause and ask before deleting or disabling the existing logic.
6. **Lock Core Configs**: Never modify `build.gradle.kts`, `settings.gradle.kts`, `AndroidManifest.xml`, `tsconfig.json`, `vite.config.ts`, or bundler configs unless specifically tasked with a build/config issue.
7. **Respect Existing Patterns**: Match the repository's established naming conventions, folder structures, and state patterns instead of inventing a new paradigm mid-project.
8. **Backward-Compatible Signatures**: When updating a shared utility function, use optional parameters with defaults rather than breaking existing call sites.
9. **Verify Imports Before Removal**: Never delete an import statement without verifying it is completely unused across the entire file.
10. **Read Before Write**: Always read the target file and its immediate imports in full before proposing or applying edits.

---

## II. Architecture, Modularity & File Hygiene (11–20)
*Preventing the 3,000-Line Monolith Trap*

11. **Hard File Line Limit**: No single source file may exceed 300–400 lines of code. Split into sub-modules, composables/hooks, or services before hitting the ceiling.
12. **Single Responsibility Principle**: Separate UI rendering, business logic, data fetching, and type definitions into distinct files.
13. **Ban Circular Dependencies**: Never allow Module A to import from Module B if Module B (directly or indirectly) imports from Module A.
14. **Strict Layer Boundaries**: UI components must never directly query the database or import server-only SDKs; always route through a ViewModel, repository, or controller layer.
15. **No Orphaned Files**: When replacing or renaming a component/module, delete the old file and update all references in the same turn.
16. **Centralized Constants**: Never scatter magic numbers, hardcoded URLs, or timeout durations across components; place them in a dedicated constants/validator object.
17. **Barrel File Discipline**: Avoid deep chains of barrel exports that break tree-shaking or cause circularity.
18. **Colocate Feature Assets**: Keep feature-specific subcomponents, state holders, and tests inside their feature package rather than dumping everything into a global `utils/` folder.
19. **Pure Utility Functions**: Functions inside `domain/` or `utils/` helpers must be stateless, pure, and free of side effects.
20. **Explicit Public APIs**: Only export/expose functions, classes, and types from a module if they are actually consumed outside that file.

---

## III. Dependency, Package & Environment Management (21–30)
*Stopping Package Hallucinations & Version Hell*

21. **Zero Hallucinated Packages**: Never import a library that isn't already listed in `libs.versions.toml` / `build.gradle.kts` / `package.json` without installing and verifying it first.
22. **Check Native APIs First**: Prefer native platform/runtime features (e.g., Android `ConnectivityManager`, `JSONObject`, `UUID.randomUUID()`, native `fetch`) over installing heavy third-party packages.
23. **Lockfile & Catalog Integrity**: Never corrupt lockfiles or version catalogs; keep versions aligned with tested SDK matrices.
24. **Version Pinning Awareness**: Check the installed major version of frameworks (e.g., Compose Material 3, Kotlin 2.x, AGP, React 19 vs 18) before writing version-specific syntax.
25. **No Duplicate Purpose Libraries**: Do not install a second HTTP client, JSON parser, or icon library when one is already configured in the project.
26. **Peer Dependency Check**: Verify dependency compatibility before adding UI primitives or plugin packages.
27. **Separate Dev/Test vs. Prod Dependencies**: Always scope test runners and mocking libraries to `testImplementation` / `devDependencies`.
28. **Verify Environment Variables at Boot**: Validate required environment/Firebase configuration at startup (`FirebaseEnvValidator`) and fall back or fail clearly if missing.
29. **Maintain `.env.example`**: Every time a new environment variable or secret key is introduced, immediately append a placeholder key to `.env.example`.
30. **Runtime Engine Alignment**: Do not use runtime APIs unsupported by the project's `minSdk` / JVM target or Node/Python engine version.

---

## IV. Type Safety & Data Contracts (31–40)
*Eliminating Runtime Type Crashes*

31. **Strict `Any` / `any` Ban**: Never use untyped `Any` casts or `: any` to silence the compiler. Use sealed interfaces, generics, or explicit type narrowing.
32. **No Compiler Error Suppression**: Fix the underlying type mismatch instead of suppressing compiler errors (`@Suppress("UNCHECKED_CAST")`, `@ts-ignore`).
33. **Shared Client-Server Schemas**: Synchronize data contracts across client models (`VibePitchModels.kt`), input validators (`InputBoundsValidator.kt`), and backend security rules (`firestore.rules`).
34. **Explicit Return Types**: Always declare explicit return types on public repository methods, domain services, and state flows.
35. **Avoid Non-Null Assertions (`!!` / `!`)**: Never use `user!!` or `user!.id` unless preceded by a guaranteed check; use safe calls (`?.`) and Elvis fallbacks (`?:`).
36. **Discriminated Unions for State**: Model async states as `sealed interface UiState<out T> { Loading, Success, Error }` rather than separate boolean flags that can contradict each other.
37. **Immutable Data Structures**: Use immutable `val` properties and `List`/`Set` collections in state payloads so state is never mutated in place.
38. **Runtime Boundary Validation**: Never trust external JSON (from APIs, `SharedPreferences`, `localStorage`, or webhooks) blindly; sanitize and clamp bounds at runtime (`InputBoundsValidator`).
39. **Exhaustive `when` / `switch` Checks**: Ensure `when` expressions over `enum class` and `sealed interface` types are exhaustive without catch-all branches that hide new cases.
40. **Strict Null & Bounds Compliance**: Always use `firstOrNull()`, `getOrNull()`, and `.coerceIn()` when indexing lists or clamping numeric inputs.

---

## V. Security, Secrets & Auth Boundaries (41–50)
*Protecting Keys, Users & Infrastructure*

41. **Zero Hardcoded Secrets**: Never write API keys, JWT secrets, database URIs, or private tokens directly into source files—even "temporarily for testing."
42. **Never Read or Print `.env`**: The agent must not dump `.env` contents to logs, UI text, or external network requests.
43. **Server-Side Auth Enforcement**: Never rely solely on client UI hiding for security; enforce authentication and ownership checks in `firestore.rules` / backend endpoints.
44. **Parameterized Queries Only**: Never concatenate raw user strings into queries; strictly use typed SDK filters (`whereEqualTo`) or parameterized statements.
45. **Sanitize Rendered Content**: Sanitize and bound all user-authored strings before rendering or exporting into templates.
46. **Strict Cross-Origin & Intent Scoping**: Scope external intents, deep links, and CORS configurations to trusted origins and explicit chooser intents.
47. **Rate Limiting & Quota Protection**: Bound query sizes (`limit`) and debounce rapid user mutations on sensitive routes.
48. **Trusted Payment & Webhook Offloading**: Never process raw credit card numbers in custom code; offload payment capture to verified Stripe Payment Links or native checkout.
49. **IDOR Prevention (Ownership Check)**: Always scope database reads, updates, and deletes by the authenticated `userId` alongside the resource `id` (`/users/{userId}/...`).
50. **Safe File & Asset Handling**: Validate MIME types, enforce strict size limits, and use zero-permission system pickers (`PickVisualMedia`) when handling media.

---

## VI. Frontend, UI/UX & State Management (51–60)
*Preventing Janky, Unresponsive & Crashing UIs*

51. **Mandatory Loading, Empty & Error States**: Every data-driven screen must explicitly render a loading state, an empty-state callout, and a retryable/clear error state.
52. **Prevent Infinite Effect Loops**: Key `LaunchedEffect` / `useEffect` blocks on stable primitives and never unconditionally mutate the same state that triggers the effect.
53. **Cleanup Side Effects**: Always unregister network callbacks (`awaitClose { connectivityManager.unregisterNetworkCallback(callback) }`), listeners, and timers on disposal.
54. **Optimistic UI & Offline Rollbacks**: When offline or when a network write fails, route gracefully to local storage (`LocalGuestSessionManager`) or restore previous state with a clear user notice.
55. **Disable Submit During In-Flight Requests**: Every form submit button must disable itself (`enabled = !quoteState.isSubmitting`) while its async request is running to prevent double-submissions.
56. **Responsive by Default**: Never use fixed pixel widths; use `BoxWithConstraints`, `fillMaxWidth()`, `widthIn(max = 640.dp)`, and adaptive `NavigationBar` / `NavigationRail` layouts.
57. **Design Token Consistency**: Use centralized Material 3 `MaterialTheme.colorScheme` and `Typography` tokens rather than scattered inline styling.
58. **Stable List Keys**: Always pass stable item IDs (`key = { it.id }`) in `LazyColumn` / dynamic lists so items can be reordered, filtered, or deleted cleanly.
59. **Form Validation Before Network**: Validate user inputs client-side with clear snackbar/field feedback before firing a repository write.
60. **Accessible Interactive Elements**: Ensure a minimum 48dp touch target, meaningful `contentDescription` semantics, and `Modifier.testTag(...)` on all interactive controls.

---

## VII. Backend, API & Database Safety (61–70)
*Preventing Data Corruption & Lockups*

61. **Idempotent Mutations**: Design critical writes (guest-to-cloud migration, profile saves, task toggles) to be idempotent using deterministic document IDs.
62. **Atomic Multi-Step Writes**: Ensure multi-step state transitions (such as marking guest data migrated only after cloud writes succeed) preserve local data on partial failure.
63. **Enforce Query & Collection Limits**: Always bound queries and enforce field size limits in both client validators and `firestore.rules`.
64. **Eliminate Redundant Listener Spawns**: Share cold-to-hot `StateFlow` streams via `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000))` instead of spawning duplicate listeners per composable.
65. **Destructive Action Guardrail**: Never execute bulk data wipes or account resets without explicit user confirmation.
66. **Explicit Timeouts on Async Calls**: Configure test and network timeouts so hung connections never block the main thread or test runner indefinitely.
67. **Standardized Error Context**: Emit structured `FirestoreErrorContext` JSON payloads (`path`, `operation`, `authState`) in logs rather than opaque stack traces.
68. **Singleton Client Reuse**: Reuse singleton `FirebaseFirestore` and `LocalGuestSessionManager` instances rather than re-instantiating database clients per screen.
69. **Background Dispatcher Offloading**: Keep disk I/O and network operations off the main UI thread using `viewModelScope` and coroutines.
70. **Graceful Lifecycle Shutdown**: Respect `CancellationException` in all coroutine `catch` blocks (`if (e is CancellationException) throw e`) so structured concurrency cancels cleanly.

---

## VIII. Error Handling, Logging & Observability (71–80)
*Banning Silent Failures*

71. **Ban Empty Catch Blocks**: Never swallow exceptions silently without logging context or emitting a safe fallback state.
72. **Contextual Error Wrapping**: Attach the operation type (`GET`, `LIST`, `CREATE`, `UPDATE`, `DELETE`) and document path via `handleFirestoreError`.
73. **Zero PII in Logs**: Never log plaintext passwords, credit card numbers, raw tokens, or private customer notes to `Logcat` / `stdout`.
74. **Distinguish Operational vs. Programmer Errors**: Surface friendly, actionable messages in the UI snackbar while logging technical diagnostics to `Log.w` / `Log.e`.
75. **Top-Level Flow Error Boundaries**: Guard every `StateFlow` pipeline with `.catch { ... }` so a single permission or network error never crashes the app process.
76. **Structured Diagnostic Logging**: Use consistent log tags (`VibePitchVM`, `FirestoreError`, `FirebaseEnvValidator`) with structured metadata.
77. **Remove Debug Artifacts Before Completion**: Strip temporary debug prints, mock overrides, and dead imports before completing any turn.
78. **Fail Gracefully in Unconfigured Environments**: Use `FirebaseEnvValidator` to detect missing Firebase configs in clean environments and fall back to Guest Mode without crashing.
79. **Offline & Retry Resilience**: Automatically detect network loss via `NetworkConnectivityObserver`, display the visual offline status dot, and route saves to local storage.
80. **Runtime Health & Storage Status Indicator**: Display live storage sync status (`Offline · Local`, `Local Storage`, `Cloud Synced`) in the top app bar at all times.

---

## IX. Terminal, Git & Autonomous Execution Limits (81–90)
*Keeping Autonomous Agents from Wrecking the Machine*

81. **Ban Destructive Shell Commands**: Never execute `rm -rf /`, `rm -rf *`, `git reset --hard`, `git push --force`, or `git clean -fd` without explicit user approval.
82. **Scoped Directory Access**: Never read, modify, or execute files outside the project workspace root directory.
83. **Non-Interactive CLI Flags Only**: Always pass non-interactive flags to CLI tools so commands never hang waiting for stdin prompts.
84. **Emulator & Port Awareness**: Use isolated emulator runners (`firebase emulators:exec`) so background processes are cleaned up automatically after tests finish.
85. **Atomic Multi-File Cohesion**: Ensure all related model, repository, ViewModel, UI, and test edits are completed cohesively before running the build check.
86. **Never Commit Build Artifacts or Secrets**: Ensure `.gitignore` excludes `build/`, `.gradle/`, `.env`, and local keystores.
87. **Explicit File Targeting**: Modify and stage only the specific files required for the task; delete replaced template files (`ExampleUnitTest.kt`) in the same turn.
88. **Enforce Test Runner Timeouts**: Configure `timeout.set(Duration.ofMinutes(2))` and `failFast = true` on Gradle `Test` tasks so hung tests terminate promptly.
89. **No Global System Mutations**: Never run `sudo`, modify `/etc/hosts`, or alter global system configurations.
90. **Validate Security Rules Before Deployment**: Always test `firestore.rules` against `firestore.test.js` and `VibePitchRepositoryRuleTest` when rules change.

---

## X. Self-Verification, Reasoning & Loop Prevention (91–100)
*Preventing Hallucination Spiral & "Death Loops"*

91. **The 3-Strike Loop Breaker**: If fixing the same compiler or test error fails 3 times, stop, analyze the root cause, and avoid repeating the same failing edit.
92. **Mandatory Build & Test Gate**: Never declare a task complete until `compile_applet` and the relevant unit/Robolectric tests pass with zero errors.
93. **Fix Root Causes, Not Symptoms**: Fix upstream data normalization or state wiring rather than masking bugs with brittle downstream workarounds.
94. **Plan Before Multi-File Execution**: Map out the exact files to create/update and their layer boundaries before executing multi-file changes.
95. **Clarify Ambiguous Requirements First**: When Planning Mode is active and requirements have critical ambiguity, ask 1–3 concise multiple-choice questions first.
96. **No Fake Passing Tests**: Never weaken or delete a valid assertion just to make a failing test turn green; fix the underlying implementation.
97. **Verify Imports & Package Signatures Match**: After creating or moving a module, verify that package declarations and imports match across every consuming file.
98. **Runtime & UI Smoke Verification**: Verify Compose UI components with Robolectric `createComposeRule()` assertions (`assertIsDisplayed()`, `performClick()`).
99. **Self-Diff & Line-Count Audit**: Verify that no unintended deletions, duplicate functions, or oversized monolith files were introduced.
100. **State Architectural Choices Clearly**: Summarize key architectural decisions, storage routing behavior, and verification results concisely at the end of the turn.
