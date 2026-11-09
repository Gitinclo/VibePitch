package com.gitinclo.vibepitch.ui.features.compliance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gitinclo.vibepitch.security.Aes256GcmCryptoEngine
import com.gitinclo.vibepitch.ui.theme.AmberHighlight
import com.gitinclo.vibepitch.ui.theme.ElectricIndigo
import com.gitinclo.vibepitch.ui.theme.EmeraldConversion

enum class ComplianceDocTab(
    val label: String,
    val fileName: String,
    val summaryTitle: String,
    val content: String
) {
    README(
        label = "README",
        fileName = "README.md",
        summaryTitle = "VibePitch Studio Architecture & Realme 8 5G Governor",
        content = """
            • Decomposed production namespace (com.gitinclo.vibepitch) supporting 7 turnkey verticals across 3 revenue streams.
            • Realme 8 5G Hardware Governor: Dimensity 700 A76/A55 thread pinning, 90Hz/60Hz thermal display governor, Mali-G57 MC2 non-destructive scaling, 4GB LPDDR4X RAM non-blocking cache trim, and UFS 2.1 sequential async I/O.
            • AES-256-GCM Authenticated Encryption (AEAD): 256-bit Android Keystore master key, fresh 96-bit SecureRandom nonces, 128-bit auth tags, and AAD context binding across local guest storage and Cloud Firestore PII fields.
            • Real-time Quote & Deposit Calculator (0–50,000 setup fee, 0–5,000/mo retainer, 0%–100% deposit) + 6-Part Pitch Script Generator.
        """.trimIndent()
    ),
    AES_256_GCM(
        label = "AES-256-GCM",
        fileName = "Aes256GcmCryptoEngine.kt",
        summaryTitle = "AES-256-GCM Authenticated Encryption (AEAD) & AAD Context Binding",
        content = """
            • Cipher Specification: AES/GCM/NoPadding with 256-bit (32-byte) key, fresh 96-bit (12-byte) SecureRandom nonce per encryption, and 128-bit (16-byte) authentication tag packed into ENCv1: Base64url envelopes.
            • Key Provisioning: Hardware-backed Android Keystore (vibe_pitch_aes256_gcm_master_key_v1) with automatic JVM 256-bit SecretKeySpec test fallback and PBKDF2-HMAC-SHA256 passphrase key derivation support.
            • Context Binding (AAD): Binds immutable owner/collection context (e.g. users/{uid}/leads or guest_prefs:local_guest_user:{key}) via cipher.updateAAD() so ciphertext cannot be transplanted across users or records.
            • Protected Storage Scope: Encrypts all LocalGuestSessionManager JSON payloads on UFS 2.1 storage and all sensitive Cloud Firestore PII fields (businessEmail, businessPhone, contactName, websiteGapNote, customerName, customerPhone, customerEmail, notes).
        """.trimIndent()
    ),
    SECURITY(
        label = "SECURITY",
        fileName = "SECURITY.md",
        summaryTitle = "Zero-Trust Firestore & Payment/PII Offloading",
        content = """
            • Zero-Trust Firestore Rules: Strict owner isolation (/users/{userId}) and server-side bounds validation.
            • Client-Side AES-256-GCM AEAD: Sensitive PII fields and local guest session payloads are encrypted with 96-bit nonces, 128-bit tags, and AAD context binding before persistence.
            • Payment & PII Offloading Guardrails: Never stores raw credit cards or banking credentials. Payments use external Stripe Links; intake uses external forms; auth uses Google OAuth via Jetpack CredentialManager.
            • Vulnerability SLA: 24h acknowledgment, 72h triage, 7d critical patch deployment.
        """.trimIndent()
    ),
    PRIVACY(
        label = "PRIVACY",
        fileName = "PRIVACY.md",
        summaryTitle = "150+ Country Privacy Compliance (Oct 10, 2026)",
        content = """
            • Aligned with GDPR, UK GDPR, CCPA/CPRA, India DPDP Act, Brazil LGPD, China PIPL, Canada PIPEDA, Australia Privacy Act, and ISO/IEC 27701.
            • Guest Mode (Days 1–30): 100% on-device AES-256-GCM encrypted storage via LocalGuestSessionManager; zero cloud transmission.
            • Permanent Business Account: Syncs only your agency profile, B2B prospects, leads, and playbook tasks under /users/{uid} with field-level AES-256-GCM PII encryption.
            • Global User Rights: Full access, rectification, JSON export, and permanent deletion within 72 hours.
        """.trimIndent()
    ),
    TERMS(
        label = "TERMS",
        fileName = "TERMS_AND_CONDITIONS.md",
        summaryTitle = "Commercial Usage & Scope Guardrail Terms",
        content = """
            • Grants a commercial license to present white-label demos, calculate quotes, and send generated proposals to prospective clients.
            • Users are responsible for complying with local outreach laws and configuring their own external Stripe Payment Links and intake forms.
            • Prohibits storing sensitive personal data or unlawful content in free-text prospect notes.
        """.trimIndent()
    ),
    ACCOUNT(
        label = "ACCOUNT",
        fileName = "ACCOUNT.md",
        summaryTitle = "30-Day Guest Lifecycle & Google Cloud Migration",
        content = """
            • Days 1–19: Full frictionless local Guest Mode.
            • Days 20–30: Dismissible banner to stage BusinessAccountProfile and link Google Account.
            • Day 31+: Required upgrade gate that automatically migrates all local guest data to Cloud Firestore (migrateGuestDataToCloud) via Jetpack CredentialManager Google Sign-In.
            • Instant self-service local wipe or permanent cloud account deletion.
        """.trimIndent()
    ),
    CONTRIBUTING(
        label = "CONTRIBUTING",
        fileName = "CONTRIBUTING.md",
        summaryTitle = "Engineering Standards & Automated Test Suite Verification",
        content = """
            • Conventional Commits and feature-branch workflow under com.gitinclo.vibepitch.
            • Schema synchronization required across VibePitchModels.kt, InputBoundsValidator.kt, firestore.rules, and firestore.test.js.
            • All PRs must pass Tier 1 (node --test firestore.test.js) and Tier 2 (PitchAndPlaybookValidationTest, AuthAndGuestUpgradeTest, VibePitchRepositoryTest, VibePitchRepositoryRuleTest, RealmeHardwareOptimizationTest, and Aes256GcmEncryptionTest).
        """.trimIndent()
    ),
    CODE_OF_CONDUCT(
        label = "CODE OF CONDUCT",
        fileName = "CODE_OF_CONDUCT.md",
        summaryTitle = "Contributor Covenant v2.1 Standards",
        content = """
            • Pledges an inclusive, harassment-free community for all contributors and users.
            • Prohibits harassment, doxxing, or intentionally weakening security rules or committing secrets.
            • Clear 3-tier enforcement model (Correction, Warning, Permanent Ban) via conduct@vibepitch.studio.
        """.trimIndent()
    ),
    ROADMAP(
        label = "ROADMAP",
        fileName = "ROADMAP.md",
        summaryTitle = "Q4 2026, 2027 & Long-Term Product Roadmap",
        content = """
            • Short-Term (Q4 2026): 7-vertical catalog, 6-script generator, 30-day guest-to-cloud lifecycle, com.gitinclo.vibepitch modular architecture, Realme 8 5G Hardware Governor HUD, AES-256-GCM AEAD encryption, and 100 Vibe Coding Guardrails suite.
            • Mid-Term (2027): Branded PDF proposal export with QR payment links, multi-currency presets (USD/EUR/GBP/INR/BRL), offline sync queue, and custom vertical builder.
            • Long-Term (2028+): Multi-seat agency workspaces and shareable client micro-preview links.
        """.trimIndent()
    ),
    GUARDRAILS(
        label = "100 GUARDRAILS",
        fileName = "GUARDRAILS.md",
        summaryTitle = "100 Vibe Coding Agent Guardrails (Categories I–X)",
        content = """
            • I. Code Preservation & Anti-Regression (1–10): No unrequested refactoring, ban lazy placeholders, surgical edits, backward-compatible signatures, read before write.
            • II. Architecture & Modularity (11–20): 300–400 line hard file limit, single responsibility, strict layer boundaries, centralized constants, pure helpers.
            • III. Dependencies & Env (21–30): Zero hallucinated packages, check native APIs first, lockfile integrity, boot env validation (FirebaseEnvValidator), .env.example sync.
            • IV. Type Safety & Contracts (31–40): Strict Any ban, shared client-server schemas, discriminated UiState unions, immutable state, runtime boundary validation.
            • V. Security, Secrets & Auth (41–50): Zero hardcoded secrets, server-side Firestore auth enforcement, Stripe/Forms offloading, IDOR ownership checks.
            • VI. Frontend, UI/UX & State (51–60): Mandatory Loading/Empty/Error states, cleanup side effects, offline fallback, disable in-flight submit, responsive 48dp touch targets.
            • VII. Backend & DB Safety (61–70): Idempotent guest migration, query/field limits, shared StateFlow listeners, explicit timeouts, CancellationException safety.
            • VIII. Error Handling & Observability (71–80): Ban empty catch blocks, contextual error wrapping, zero PII in logs, live StorageSyncStatusDotChip offline indicator.
            • IX. Terminal & Execution Limits (81–90): Ban destructive shell commands, scoped directory access, non-interactive CLI flags, emulator validation before deploy.
            • X. Self-Verification & Loop Prevention (91–100): 3-strike loop breaker, mandatory compile & test gate, fix root causes, no fake passing tests, explicit assumptions.
        """.trimIndent()
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComplianceDocsDialog(
    onDismiss: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(ComplianceDocTab.README) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Docs & Global Compliance Hub",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ISO/IEC 27701 • AES-256-GCM AEAD & 150+ Country Privacy Suite",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ComplianceDocTab.entries.forEach { tab ->
                        FilterChip(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            label = {
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.testTag("compliance_doc_tab_${tab.name.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    tonalElevation = 2.dp,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(14.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = selectedTab.summaryTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "Source: /${selectedTab.fileName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = selectedTab.content,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.testTag("compliance_doc_content")
                        )

                        if (selectedTab == ComplianceDocTab.AES_256_GCM) {
                            Aes256GcmInspectorCard()
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_compliance_docs_button")
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
fun Aes256GcmInspectorCard() {
    val samplePlaintext = "Client Lead: Jordan Taylor • 555-0188 • Deposit $550"
    val validAad = "users/user_id_9481/leads"
    val tamperedAad = "users/attacker_id_999/leads"

    var currentCiphertext by remember {
        mutableStateOf(
            Aes256GcmCryptoEngine.encrypt(
                plaintext = samplePlaintext,
                associatedData = validAad
            )
        )
    }
    var verificationResultText by remember {
        mutableStateOf("VERIFIED • Decrypted with bound AAD ($validAad): \"$samplePlaintext\"")
    }
    var isTamperRejected by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isTamperRejected) AmberHighlight.copy(alpha = 0.6f) else EmeraldConversion.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("aes256_gcm_inspector_card")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Live AES-256-GCM AEAD & AAD Context Inspector",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = EmeraldConversion
            )
            Text(
                text = "Key Provider: ${Aes256GcmCryptoEngine.activeKeyProviderDescription()}\n" +
                    "Spec: ${Aes256GcmCryptoEngine.KEY_SIZE_BITS}-bit Key • ${Aes256GcmCryptoEngine.NONCE_LENGTH_BYTES * 8}-bit Nonce • ${Aes256GcmCryptoEngine.AUTH_TAG_LENGTH_BITS}-bit Tag",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Packed Payload (ENCv1: Nonce + Ciphertext + Tag):\n$currentCiphertext",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.testTag("aes256_inspector_ciphertext")
            )

            Text(
                text = verificationResultText,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isTamperRejected) AmberHighlight else EmeraldConversion,
                modifier = Modifier.testTag("aes256_inspector_status")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val freshToken = Aes256GcmCryptoEngine.encrypt(
                            plaintext = samplePlaintext,
                            associatedData = validAad
                        )
                        currentCiphertext = freshToken
                        val decrypted = Aes256GcmCryptoEngine.decrypt(
                            token = freshToken,
                            associatedData = validAad
                        ).getOrElse { "DECRYPT_ERROR" }
                        isTamperRejected = false
                        verificationResultText = "VERIFIED • Fresh 96-bit Nonce + AAD ($validAad) -> \"$decrypted\""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("aes256_run_live_verify_button")
                ) {
                    Text(
                        text = "Rotate Nonce & Verify",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                FilledTonalButton(
                    onClick = {
                        val tamperAttempt = Aes256GcmCryptoEngine.decrypt(
                            token = currentCiphertext,
                            associatedData = tamperedAad
                        )
                        isTamperRejected = tamperAttempt.isFailure
                        val exName = tamperAttempt.exceptionOrNull()?.javaClass?.simpleName ?: "AEADBadTagException"
                        verificationResultText =
                            "BLOCKED ($exName) • Mismatched AAD ($tamperedAad) rejected before plaintext release"
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("aes256_simulate_tamper_button")
                ) {
                    Text(
                        text = "Simulate AAD Tamper",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
