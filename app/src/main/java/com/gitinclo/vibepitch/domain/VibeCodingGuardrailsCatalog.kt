package com.gitinclo.vibepitch.domain

enum class GuardrailCategory(
    val id: String,
    val romanNumeral: String,
    val shortLabel: String,
    val title: String,
    val subtitle: String,
    val rangeLabel: String
) {
    CODE_PRESERVATION(
        id = "CODE_PRESERVATION",
        romanNumeral = "I",
        shortLabel = "I. Anti-Regression",
        title = "I. Code Preservation & Anti-Regression (1–10)",
        subtitle = "The 'Do Not Break Working Code' Rules",
        rangeLabel = "1–10"
    ),
    ARCHITECTURE_MODULARITY(
        id = "ARCHITECTURE_MODULARITY",
        romanNumeral = "II",
        shortLabel = "II. Modularity",
        title = "II. Architecture, Modularity & File Hygiene (11–20)",
        subtitle = "Preventing the 3,000-Line Monolith Trap",
        rangeLabel = "11–20"
    ),
    DEPENDENCY_ENV(
        id = "DEPENDENCY_ENV",
        romanNumeral = "III",
        shortLabel = "III. Dependencies",
        title = "III. Dependency, Package & Environment Management (21–30)",
        subtitle = "Stopping Package Hallucinations & Version Hell",
        rangeLabel = "21–30"
    ),
    TYPE_SAFETY(
        id = "TYPE_SAFETY",
        romanNumeral = "IV",
        shortLabel = "IV. Type Safety",
        title = "IV. TypeScript / Kotlin Type Safety & Data Contracts (31–40)",
        subtitle = "Eliminating Runtime Type Crashes",
        rangeLabel = "31–40"
    ),
    SECURITY_AUTH(
        id = "SECURITY_AUTH",
        romanNumeral = "V",
        shortLabel = "V. Security & Auth",
        title = "V. Security, Secrets & Auth Boundaries (41–50)",
        subtitle = "Protecting Keys, Users & Infrastructure",
        rangeLabel = "41–50"
    ),
    FRONTEND_UI_STATE(
        id = "FRONTEND_UI_STATE",
        romanNumeral = "VI",
        shortLabel = "VI. UI & State",
        title = "VI. Frontend, UI/UX & State Management (51–60)",
        subtitle = "Preventing Janky, Unresponsive & Crashing UIs",
        rangeLabel = "51–60"
    ),
    BACKEND_DB_SAFETY(
        id = "BACKEND_DB_SAFETY",
        romanNumeral = "VII",
        shortLabel = "VII. DB & API",
        title = "VII. Backend, API & Database Safety (61–70)",
        subtitle = "Preventing Data Corruption & Server Lockups",
        rangeLabel = "61–70"
    ),
    ERROR_OBSERVABILITY(
        id = "ERROR_OBSERVABILITY",
        romanNumeral = "VIII",
        shortLabel = "VIII. Observability",
        title = "VIII. Error Handling, Logging & Observability (71–80)",
        subtitle = "Banning Silent Failures",
        rangeLabel = "71–80"
    ),
    TERMINAL_EXECUTION(
        id = "TERMINAL_EXECUTION",
        romanNumeral = "IX",
        shortLabel = "IX. Terminal & Git",
        title = "IX. Terminal, Git & Autonomous Execution Limits (81–90)",
        subtitle = "Keeping Autonomous Agents from Wrecking the Machine",
        rangeLabel = "81–90"
    ),
    SELF_VERIFICATION(
        id = "SELF_VERIFICATION",
        romanNumeral = "X",
        shortLabel = "X. Verification",
        title = "X. Self-Verification, Reasoning & Loop Prevention (91–100)",
        subtitle = "Preventing Hallucination Spiral & 'Death Loops'",
        rangeLabel = "91–100"
    )
}

enum class GuardrailExportFormat(
    val label: String,
    val fileName: String,
    val headerComment: String
) {
    CLAUDE_MD(
        label = "CLAUDE.md",
        fileName = "CLAUDE.md",
        headerComment = "# CLAUDE.md — 100 Vibe Coding Agent Guardrails\n# Drop into your repository root for Claude Code enforcement."
    ),
    CURSOR_RULES(
        label = ".cursorrules",
        fileName = ".cursorrules",
        headerComment = "# .cursorrules — 100 Vibe Coding Agent Guardrails\n# Drop into your repository root for Cursor AI enforcement."
    ),
    WINDSURF_RULES(
        label = ".windsurfrules",
        fileName = ".windsurfrules",
        headerComment = "# .windsurfrules — 100 Vibe Coding Agent Guardrails\n# Drop into your repository root for Windsurf enforcement."
    ),
    SYSTEM_PROMPT(
        label = "Agent Prompt",
        fileName = "agent_system_prompt.txt",
        headerComment = "You are an autonomous production software engineering agent. Enforce these 100 Vibe Coding Guardrails on every turn:"
    )
}

data class VibeGuardrailRule(
    val number: Int,
    val category: GuardrailCategory,
    val title: String,
    val description: String
)

object VibeCodingGuardrailsCatalog {

    val allRules: List<VibeGuardrailRule> by lazy {
        rulesPart1 + VibeCodingGuardrailsPart2.rulesPart2
    }

    fun getRulesByCategory(category: GuardrailCategory?): List<VibeGuardrailRule> {
        if (category == null) return allRules
        return allRules.filter { it.category == category }
    }

    fun filterRules(
        category: GuardrailCategory?,
        searchQuery: String
    ): List<VibeGuardrailRule> {
        val base = getRulesByCategory(category)
        val q = searchQuery.trim()
        if (q.isEmpty()) return base
        return base.filter { rule ->
            rule.number.toString() == q ||
                "#${rule.number}".equals(q, ignoreCase = true) ||
                rule.title.contains(q, ignoreCase = true) ||
                rule.description.contains(q, ignoreCase = true) ||
                rule.category.title.contains(q, ignoreCase = true)
        }
    }

    fun exportGuardrailsBlock(
        format: GuardrailExportFormat,
        categoryFilter: GuardrailCategory? = null
    ): String {
        val rulesToExport = getRulesByCategory(categoryFilter)
        val grouped = rulesToExport.groupBy { it.category }
        val sb = StringBuilder()
        sb.appendLine(format.headerComment)
        sb.appendLine()
        grouped.forEach { (category, rules) ->
            sb.appendLine("## ${category.title} — ${category.subtitle}")
            rules.forEach { rule ->
                sb.appendLine("${rule.number}. **${rule.title}**: ${rule.description}")
            }
            sb.appendLine()
        }
        return sb.toString().trimEnd()
    }

    internal val rulesPart1: List<VibeGuardrailRule> = listOf(
        // I. Code Preservation & Anti-Regression (1–10)
        VibeGuardrailRule(1, GuardrailCategory.CODE_PRESERVATION, "No Unrequested Refactoring", "Never refactor, rename, or 'clean up' working code outside the explicit scope of the current prompt."),
        VibeGuardrailRule(2, GuardrailCategory.CODE_PRESERVATION, "Ban Lazy Placeholders", "Never output // ... rest of code remains the same, // TODO: implement, or truncated blocks when writing files unless explicitly instructed to provide a diff."),
        VibeGuardrailRule(3, GuardrailCategory.CODE_PRESERVATION, "Preserve Existing Comments & Docs", "Do not strip out JSDoc/KDoc blocks, architectural notes, or IMPORTANT: comments left by previous sessions."),
        VibeGuardrailRule(4, GuardrailCategory.CODE_PRESERVATION, "Surgical Edits Only", "Modify the minimum number of lines required to achieve the requested feature or bug fix."),
        VibeGuardrailRule(5, GuardrailCategory.CODE_PRESERVATION, "No Silent Feature Deletion", "If a change conflicts with an existing feature, pause and ask before deleting or disabling the existing logic."),
        VibeGuardrailRule(6, GuardrailCategory.CODE_PRESERVATION, "Lock Core Configs", "Never modify tsconfig.json, vite.config.ts, next.config.js, build.gradle.kts, or bundler configs unless specifically tasked with a build/config issue."),
        VibeGuardrailRule(7, GuardrailCategory.CODE_PRESERVATION, "Respect Existing Patterns", "Match the repository's established naming conventions, folder structures, and state patterns instead of inventing a new paradigm mid-project."),
        VibeGuardrailRule(8, GuardrailCategory.CODE_PRESERVATION, "Backward-Compatible Signatures", "When updating a shared utility function, use optional parameters with defaults rather than breaking existing call sites."),
        VibeGuardrailRule(9, GuardrailCategory.CODE_PRESERVATION, "Verify Imports Before Removal", "Never delete an import statement without verifying it is completely unused across the entire file."),
        VibeGuardrailRule(10, GuardrailCategory.CODE_PRESERVATION, "Read Before Write", "Always read the target file and its immediate imports in full before proposing or applying edits."),

        // II. Architecture, Modularity & File Hygiene (11–20)
        VibeGuardrailRule(11, GuardrailCategory.ARCHITECTURE_MODULARITY, "Hard File Line Limit", "No single source file may exceed 300–400 lines of code. Split into sub-modules, hooks, or services before hitting the ceiling."),
        VibeGuardrailRule(12, GuardrailCategory.ARCHITECTURE_MODULARITY, "Single Responsibility Principle", "Separate UI rendering, business logic, data fetching, and type definitions into distinct files."),
        VibeGuardrailRule(13, GuardrailCategory.ARCHITECTURE_MODULARITY, "Ban Circular Dependencies", "Never allow Module A to import from Module B if Module B (directly or indirectly) imports from Module A."),
        VibeGuardrailRule(14, GuardrailCategory.ARCHITECTURE_MODULARITY, "Strict Layer Boundaries", "UI components must never directly query the database or import server-only SDKs; always route through an API, ViewModel, or repository layer."),
        VibeGuardrailRule(15, GuardrailCategory.ARCHITECTURE_MODULARITY, "No Orphaned Files", "When replacing or renaming a component/module, delete the old file and update all references in the same turn."),
        VibeGuardrailRule(16, GuardrailCategory.ARCHITECTURE_MODULARITY, "Centralized Constants", "Never scatter magic numbers, hardcoded URLs, or timeout durations across components; place them in a dedicated constants file."),
        VibeGuardrailRule(17, GuardrailCategory.ARCHITECTURE_MODULARITY, "Barrel File Discipline", "Avoid deep chains of index.ts barrel exports that break tree-shaking or cause bundler circularity."),
        VibeGuardrailRule(18, GuardrailCategory.ARCHITECTURE_MODULARITY, "Colocate Feature Assets", "Keep feature-specific subcomponents, hooks, and tests inside their feature directory rather than dumping everything into a global utils/ folder."),
        VibeGuardrailRule(19, GuardrailCategory.ARCHITECTURE_MODULARITY, "Pure Utility Functions", "Functions inside utils/ or helpers/ must be stateless, pure, and free of side effects."),
        VibeGuardrailRule(20, GuardrailCategory.ARCHITECTURE_MODULARITY, "Explicit Public APIs", "Only export functions, classes, and types from a module if they are actually consumed outside that file."),

        // III. Dependency, Package & Environment Management (21–30)
        VibeGuardrailRule(21, GuardrailCategory.DEPENDENCY_ENV, "Zero Hallucinated Packages", "Never import a library that isn't already listed in package.json / libs.versions.toml / requirements.txt without installing it first."),
        VibeGuardrailRule(22, GuardrailCategory.DEPENDENCY_ENV, "Check Native APIs First", "Prefer native runtime features (e.g., native fetch, crypto.randomUUID(), ConnectivityManager, JSONObject) over installing third-party packages."),
        VibeGuardrailRule(23, GuardrailCategory.DEPENDENCY_ENV, "Lockfile Integrity", "Never manually corrupt package-lock.json, pnpm-lock.yaml, yarn.lock, or Gradle lockfiles."),
        VibeGuardrailRule(24, GuardrailCategory.DEPENDENCY_ENV, "Version Pinning Awareness", "Check the installed major version of frameworks (e.g., Compose M3, Kotlin 2.x, React 19 vs 18, Tailwind v4 vs v3) before writing version-specific syntax."),
        VibeGuardrailRule(25, GuardrailCategory.DEPENDENCY_ENV, "No Duplicate Purpose Libraries", "Do not install axios if fetch is used, moment if date-fns is installed, or a second icon library."),
        VibeGuardrailRule(26, GuardrailCategory.DEPENDENCY_ENV, "Peer Dependency Check", "Verify peer dependency and Kotlin/AGP version compatibility before adding UI primitives or plugin packages."),
        VibeGuardrailRule(27, GuardrailCategory.DEPENDENCY_ENV, "Separate Dev vs. Prod Dependencies", "Always install linters, type definitions, and test runners as devDependencies / testImplementation."),
        VibeGuardrailRule(28, GuardrailCategory.DEPENDENCY_ENV, "Verify Environment Variables at Boot", "Validate required environment and Firebase configs at startup (FirebaseEnvValidator) and surface a clear status if any are missing."),
        VibeGuardrailRule(29, GuardrailCategory.DEPENDENCY_ENV, "Maintain .env.example", "Every time a new environment variable is introduced in code, immediately append a placeholder key to .env.example."),
        VibeGuardrailRule(30, GuardrailCategory.DEPENDENCY_ENV, "Runtime Engine Alignment", "Do not use runtime APIs unsupported by the version specified in minSdk, .nvmrc, or engines in package.json."),

        // IV. TypeScript / Kotlin Type Safety & Data Contracts (31–40)
        VibeGuardrailRule(31, GuardrailCategory.TYPE_SAFETY, "Strict Any / any Ban", "Never use : any, as any, or unchecked Any casts to silence the compiler. Use sealed types, unknown with type narrowing, or proper generics."),
        VibeGuardrailRule(32, GuardrailCategory.TYPE_SAFETY, "No @ts-ignore / Compiler Suppression", "Fix the underlying type mismatch instead of suppressing compiler errors."),
        VibeGuardrailRule(33, GuardrailCategory.TYPE_SAFETY, "Shared Client-Server Schemas", "Define request/response and entity contracts in synchronized schemas (VibePitchModels, InputBoundsValidator, firestore.rules)."),
        VibeGuardrailRule(34, GuardrailCategory.TYPE_SAFETY, "Explicit Return Types", "Always declare explicit return types on exported functions, repository methods, API handlers, and custom hooks."),
        VibeGuardrailRule(35, GuardrailCategory.TYPE_SAFETY, "Avoid Non-Null Assertions (! / !!)", "Never use user!.id or user!! unless preceded by a guaranteed check; use optional chaining (?.) and Elvis/nullish coalescing (?: / ??) with safe fallbacks."),
        VibeGuardrailRule(36, GuardrailCategory.TYPE_SAFETY, "Discriminated Unions for State", "Model async states as sealed UiState (Loading | Success | Error) unions rather than separate boolean flags that can contradict each other."),
        VibeGuardrailRule(37, GuardrailCategory.TYPE_SAFETY, "Readonly / Immutable Data Structures", "Use readonly val properties and immutable lists/sets for props and state payloads that should never be mutated in place."),
        VibeGuardrailRule(38, GuardrailCategory.TYPE_SAFETY, "Runtime Boundary Validation", "Never trust external JSON (from APIs, SharedPreferences, localStorage, or webhooks) blindly; parse and validate bounds at runtime."),
        VibeGuardrailRule(39, GuardrailCategory.TYPE_SAFETY, "Exhaustive when / switch Checks", "Ensure switch/when statements over union and enum types are exhaustive across all branches."),
        VibeGuardrailRule(40, GuardrailCategory.TYPE_SAFETY, "Strict Null Checks Compliance", "Always handle null/undefined explicitly when indexing arrays or collections (firstOrNull, getOrNull, coerceIn)."),

        // V. Security, Secrets & Auth Boundaries (41–50)
        VibeGuardrailRule(41, GuardrailCategory.SECURITY_AUTH, "Zero Hardcoded Secrets", "Never write API keys, JWT secrets, database URIs, or private tokens directly into source files—even 'temporarily for testing.'"),
        VibeGuardrailRule(42, GuardrailCategory.SECURITY_AUTH, "Never Read or Print .env", "The agent must not dump .env contents to the console, Logcat, or external network requests."),
        VibeGuardrailRule(43, GuardrailCategory.SECURITY_AUTH, "Server-Side Auth Enforcement", "Never rely on frontend route guards or hidden UI buttons for security; enforce authentication and ownership checks on every backend/Firestore rule."),
        VibeGuardrailRule(44, GuardrailCategory.SECURITY_AUTH, "Parameterized Queries Only", "Never concatenate or interpolate raw user strings into SQL or NoSQL queries; strictly use parameterized queries or typed SDK filters."),
        VibeGuardrailRule(45, GuardrailCategory.SECURITY_AUTH, "Sanitize Rendered Content", "Never render unsanitized HTML or unbounded user strings without sanitization first."),
        VibeGuardrailRule(46, GuardrailCategory.SECURITY_AUTH, "Strict CORS & Intent Configuration", "Do not leave wildcard Access-Control-Allow-Origin: * on authenticated endpoints in production configs."),
        VibeGuardrailRule(47, GuardrailCategory.SECURITY_AUTH, "Rate Limiting on Sensitive Routes", "Always attach rate limiters or quota bounds to login, registration, password reset, and paid AI/LLM generation endpoints."),
        VibeGuardrailRule(48, GuardrailCategory.SECURITY_AUTH, "Webhook Signature & Payment Verification", "Never process incoming payment webhooks without verifying cryptographic signatures; offload payment capture to Stripe Payment Links."),
        VibeGuardrailRule(49, GuardrailCategory.SECURITY_AUTH, "IDOR Prevention (Ownership Check)", "Always scope database reads, updates, and deletes by the authenticated userId alongside the resource id."),
        VibeGuardrailRule(50, GuardrailCategory.SECURITY_AUTH, "Safe File Uploads", "Validate file MIME types, enforce strict size limits, use zero-permission pickers, and rename uploaded files to random UUIDs.")
    )
}
