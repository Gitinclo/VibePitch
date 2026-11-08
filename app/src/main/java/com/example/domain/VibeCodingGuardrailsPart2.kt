package com.example.domain

internal object VibeCodingGuardrailsPart2 {

    val rulesPart2: List<VibeGuardrailRule> = listOf(
        // VI. Frontend, UI/UX & State Management (51–60)
        VibeGuardrailRule(51, GuardrailCategory.FRONTEND_UI_STATE, "Mandatory Loading, Empty & Error States", "Every data-driven component must explicitly render a loading skeleton/spinner, an empty-state view, and a retryable error state."),
        VibeGuardrailRule(52, GuardrailCategory.FRONTEND_UI_STATE, "Prevent Infinite Effect Loops", "Never put unstable object/array/function literals into effect keys without memoization, and never unconditionally set state inside an effect that depends on that state."),
        VibeGuardrailRule(53, GuardrailCategory.FRONTEND_UI_STATE, "Cleanup Side Effects", "Every network callback, event listener, timer, or WebSocket must be cleaned up on component unmount / awaitClose."),
        VibeGuardrailRule(54, GuardrailCategory.FRONTEND_UI_STATE, "Optimistic UI Rollbacks & Offline Fallback", "If a network write fails or the device is offline, route gracefully to local storage or restore previous state with a clear user notice."),
        VibeGuardrailRule(55, GuardrailCategory.FRONTEND_UI_STATE, "Disable Submit During In-Flight Requests", "Every form button or action trigger must disable itself and show a pending state while its async request is running to prevent double-submissions."),
        VibeGuardrailRule(56, GuardrailCategory.FRONTEND_UI_STATE, "Responsive by Default", "Never use fixed pixel widths without responsive breakpoints or widthIn(max = 640.dp) constraints for mobile and tablet viewports."),
        VibeGuardrailRule(57, GuardrailCategory.FRONTEND_UI_STATE, "Design Token Consistency", "Use the project's defined MaterialTheme colorScheme, spacing scale, and typography classes rather than arbitrary inline styles."),
        VibeGuardrailRule(58, GuardrailCategory.FRONTEND_UI_STATE, "Stable List Keys", "Never use array index or Math.random() as a list key if list items can be reordered, filtered, or deleted; use stable item IDs."),
        VibeGuardrailRule(59, GuardrailCategory.FRONTEND_UI_STATE, "Form Validation Before Network", "Validate user inputs client-side with clear field-level or snackbar error messages before firing a backend request."),
        VibeGuardrailRule(60, GuardrailCategory.FRONTEND_UI_STATE, "Accessible Interactive Elements", "Ensure 48dp minimum touch targets, semantic Button/Chip roles, contentDescriptions, and testTags on interactive controls."),

        // VII. Backend, API & Database Safety (61–70)
        VibeGuardrailRule(61, GuardrailCategory.BACKEND_DB_SAFETY, "Idempotent Mutations", "Design critical write operations (guest-to-cloud migration, order creation, state transitions) to be idempotent using deterministic keys."),
        VibeGuardrailRule(62, GuardrailCategory.BACKEND_DB_SAFETY, "Database Transactions for Multi-Step Writes", "Wrap multi-step writes inside atomic transactions or verified sequential handoffs so partial failures never corrupt state."),
        VibeGuardrailRule(63, GuardrailCategory.BACKEND_DB_SAFETY, "Enforce Pagination & Query Limits", "Never write an unbounded query on user-facing endpoints; always enforce a limit and field bounds."),
        VibeGuardrailRule(64, GuardrailCategory.BACKEND_DB_SAFETY, "Eliminate N+1 Queries", "Reuse shared StateFlow snapshot listeners and batch queries rather than firing individual queries inside UI loops."),
        VibeGuardrailRule(65, GuardrailCategory.BACKEND_DB_SAFETY, "Destructive Migration Guardrail", "Never auto-run destructive wipes (DROP TABLE, TRUNCATE, full account wipe) without explicit human confirmation."),
        VibeGuardrailRule(66, GuardrailCategory.BACKEND_DB_SAFETY, "Explicit Timeouts on External Calls", "Every outbound network call, database request, or test task must include a timeout config so hung services don't lock up execution."),
        VibeGuardrailRule(67, GuardrailCategory.BACKEND_DB_SAFETY, "Standardized Error Envelope", "Return consistent Result<T> and structured JSON error shapes ({ path, operation, message })—never expose raw HTML stack traces."),
        VibeGuardrailRule(68, GuardrailCategory.BACKEND_DB_SAFETY, "Connection Pool Singleton", "Reuse singleton database/Firestore instances to prevent exhausting client connection limits."),
        VibeGuardrailRule(69, GuardrailCategory.BACKEND_DB_SAFETY, "Background Heavy Tasks", "Never run heavy I/O or batch migrations synchronously on the main UI thread; offload to coroutines or background workers."),
        VibeGuardrailRule(70, GuardrailCategory.BACKEND_DB_SAFETY, "Graceful Shutdown & Cancellation Handling", "Always rethrow CancellationException in coroutine catch blocks and close active listeners cleanly."),

        // VIII. Error Handling, Logging & Observability (71–80)
        VibeGuardrailRule(71, GuardrailCategory.ERROR_OBSERVABILITY, "Ban Empty Catch Blocks", "Never write catch (e) {} that silently swallows an error and lets execution continue with corrupted state."),
        VibeGuardrailRule(72, GuardrailCategory.ERROR_OBSERVABILITY, "Contextual Error Wrapping", "When catching and handling an error, attach context (operation type, document path, and auth state)."),
        VibeGuardrailRule(73, GuardrailCategory.ERROR_OBSERVABILITY, "Zero PII in Logs", "Never log plaintext passwords, credit card numbers, full session tokens, or private user messages to stdout/Logcat."),
        VibeGuardrailRule(74, GuardrailCategory.ERROR_OBSERVABILITY, "Distinguish Operational vs. Programmer Errors", "Return friendly, actionable messages for invalid user input, and log unexpected exceptions without leaking internal details."),
        VibeGuardrailRule(75, GuardrailCategory.ERROR_OBSERVABILITY, "Global Unhandled Rejection / Flow Catch", "Ensure every reactive stream and screen boundary has error handling so a single failure never crashes the app."),
        VibeGuardrailRule(76, GuardrailCategory.ERROR_OBSERVABILITY, "Structured Logging", "Prefer structured JSON/tagged logs (handleFirestoreError) over cryptic console.log('here 123') debugging statements."),
        VibeGuardrailRule(77, GuardrailCategory.ERROR_OBSERVABILITY, "Remove Debug Artifacts Before Completion", "Strip temporary debug logs, debugger statements, and test mock overrides before marking a task complete."),
        VibeGuardrailRule(78, GuardrailCategory.ERROR_OBSERVABILITY, "Fail Loudly / Clearly in Development", "Surface missing configuration or schema mismatches immediately with actionable remediation steps (FirebaseEnvValidator)."),
        VibeGuardrailRule(79, GuardrailCategory.ERROR_OBSERVABILITY, "External API Rate-Limit Backoff & Offline Routing", "Detect offline states via NetworkConnectivityObserver and route mutations cleanly to local storage."),
        VibeGuardrailRule(80, GuardrailCategory.ERROR_OBSERVABILITY, "Health & Storage Status Indicator", "Maintain a live connectivity/storage indicator (StorageSyncStatusDotChip) showing Offline Local vs. Cloud Firestore state."),

        // IX. Terminal, Git & Autonomous Execution Limits (81–90)
        VibeGuardrailRule(81, GuardrailCategory.TERMINAL_EXECUTION, "Ban Destructive Shell Commands", "Never execute rm -rf /, rm -rf *, git reset --hard, git push --force, or git clean -fd without explicit user approval."),
        VibeGuardrailRule(82, GuardrailCategory.TERMINAL_EXECUTION, "Scoped Directory Access", "Never read, modify, or execute files outside the current project workspace root directory."),
        VibeGuardrailRule(83, GuardrailCategory.TERMINAL_EXECUTION, "Non-Interactive CLI Flags Only", "Always pass non-interactive flags (-y, --yes, --no-input, CI=true) to terminal commands so the agent never hangs waiting for stdin."),
        VibeGuardrailRule(84, GuardrailCategory.TERMINAL_EXECUTION, "Port & Emulator Conflict Awareness", "Check active ports or use isolated firebase emulators:exec runners rather than spawning duplicate zombie processes."),
        VibeGuardrailRule(85, GuardrailCategory.TERMINAL_EXECUTION, "Atomic Checkpoints", "Complete cohesive multi-file changes cleanly before running build/test verification so rollback is straightforward."),
        VibeGuardrailRule(86, GuardrailCategory.TERMINAL_EXECUTION, "Never Commit Build Artifacts or Secrets", "Verify .gitignore excludes node_modules/, build/, .gradle/, .env*, .DS_Store, and local keystores."),
        VibeGuardrailRule(87, GuardrailCategory.TERMINAL_EXECUTION, "Avoid Blind Staging", "Target specific modified files to avoid accidentally committing temporary scratch scripts or secrets."),
        VibeGuardrailRule(88, GuardrailCategory.TERMINAL_EXECUTION, "Kill Hung Subprocesses", "Enforce a hard execution timeout on any test runner or build command executed by the agent."),
        VibeGuardrailRule(89, GuardrailCategory.TERMINAL_EXECUTION, "No Global System Mutations", "Never run sudo, modify /etc/hosts, alter global git configs, or install global packages without asking."),
        VibeGuardrailRule(90, GuardrailCategory.TERMINAL_EXECUTION, "Dry-Run & Emulator Validation First", "Validate security rules and migrations against local emulators before deploying to production."),

        // X. Self-Verification, Reasoning & Loop Prevention (91–100)
        VibeGuardrailRule(91, GuardrailCategory.SELF_VERIFICATION, "The 3-Strike Loop Breaker", "If the agent attempts to fix the same bug or compiler error 3 times and fails, it must stop, summarize what failed, and ask the user for guidance."),
        VibeGuardrailRule(92, GuardrailCategory.SELF_VERIFICATION, "Mandatory Typecheck/Build Gate", "Never declare a coding task 'done' until compile_applet and unit/Robolectric tests pass with zero new errors."),
        VibeGuardrailRule(93, GuardrailCategory.SELF_VERIFICATION, "Fix Root Causes, Not Symptoms", "Do not patch a downstream crash with a dummy fallback if the bug is caused by an upstream function returning malformed data."),
        VibeGuardrailRule(94, GuardrailCategory.SELF_VERIFICATION, "Plan Before Multi-File Execution", "For any feature touching more than 3 files, map out the files to create/edit and data flow before writing code."),
        VibeGuardrailRule(95, GuardrailCategory.SELF_VERIFICATION, "Clarify Ambiguous Requirements First", "When Planning Mode is enabled and a prompt has critical ambiguity, ask 1–3 concise clarifying questions before generating code."),
        VibeGuardrailRule(96, GuardrailCategory.SELF_VERIFICATION, "No Fake Passing Tests", "Never modify a failing unit test to assert broken output just to make the test suite turn green."),
        VibeGuardrailRule(97, GuardrailCategory.SELF_VERIFICATION, "Verify Imports & Exports Match", "After creating or editing a module, verify that package declarations, imports, and function signatures match every caller."),
        VibeGuardrailRule(98, GuardrailCategory.SELF_VERIFICATION, "Runtime & UI Smoke Check", "Verify that the application compiles and UI components render without runtime errors via Robolectric Compose tests."),
        VibeGuardrailRule(99, GuardrailCategory.SELF_VERIFICATION, "Self-Diff Review", "Before finishing a turn, review modified files to ensure no unintended deletions, stray debug code, or regressions were introduced."),
        VibeGuardrailRule(100, GuardrailCategory.SELF_VERIFICATION, "State Assumptions Explicitly", "Whenever making a minor architectural choice not specified by the user, state it briefly in the final summary so the user stays in control.")
    )
}
