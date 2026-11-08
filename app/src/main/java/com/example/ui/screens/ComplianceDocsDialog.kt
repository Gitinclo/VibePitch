package com.example.ui.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class ComplianceDocTab(
    val label: String,
    val fileName: String,
    val summaryTitle: String,
    val content: String
) {
    README(
        label = "README",
        fileName = "README.md",
        summaryTitle = "VibePitch Studio Architecture & Overview",
        content = """
            • Multi-Stream Sales & Pitch Studio supporting 7 turnkey verticals (HOME_TRADES, SALON_AND_CLEANERS, DENTAL_AND_TUTORS, AUTO_DETAILING, BOUTIQUE_FITNESS, ECOMMERCE_SHOPIFY, SAAS_AI_REMEDIATION).
            • Real-time Quote & Deposit Calculator (0–50,000 setup fee, 0–5,000/mo retainer, 0%–100% deposit).
            • 6-Part Pitch, Proposal, Scope Guardrail & Referral Script Generator.
            • 30-Day Local Guest Account to Permanent Cloud Business Account lifecycle.
        """.trimIndent()
    ),
    SECURITY(
        label = "SECURITY",
        fileName = "SECURITY.md",
        summaryTitle = "Zero-Trust Firestore & Payment/PII Offloading",
        content = """
            • Zero-Trust Firestore Rules: Strict owner isolation (/users/{userId}) and server-side bounds validation.
            • Payment & PII Offloading Guardrails: Never stores raw credit cards, banking credentials, or sensitive PII. Payments use external Stripe Links; intake uses external forms; auth uses Google OAuth via Jetpack CredentialManager.
            • Encryption: TLS 1.3 in transit, AES-256 at rest in Cloud Firestore, and Android OS sandbox encryption in Guest Mode.
            • Vulnerability SLA: 24h acknowledgment, 72h triage, 7d critical patch deployment.
        """.trimIndent()
    ),
    PRIVACY(
        label = "PRIVACY",
        fileName = "PRIVACY.md",
        summaryTitle = "150+ Country Privacy Compliance (Oct 10, 2026)",
        content = """
            • Aligned with GDPR, UK GDPR, CCPA/CPRA, India DPDP Act, Brazil LGPD, China PIPL, Canada PIPEDA, Australia Privacy Act, and ISO/IEC 27701.
            • Guest Mode (Days 1–30): 100% on-device storage via LocalGuestSessionManager; zero cloud transmission.
            • Permanent Business Account: Syncs only your agency profile, B2B prospects, leads, and playbook tasks under /users/{uid}.
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
        summaryTitle = "Engineering Standards & Emulator Verification",
        content = """
            • Conventional Commits and feature-branch workflow.
            • Schema synchronization required across VibePitchModels.kt, InputBoundsValidator.kt, firestore.rules, and firestore.test.js.
            • All PRs must pass Tier 1 (node --test firestore.test.js) and Tier 2 (gradle :app:testDebugUnitTest).
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
            • Short-Term (Q4 2026): 7-vertical catalog, 6-script generator, 30-day guest-to-cloud lifecycle, zero-trust Firestore rules, and 8-doc compliance suite.
            • Mid-Term (2027): Branded PDF proposal export with QR payment links, multi-currency presets (USD/EUR/GBP/INR/BRL), offline sync queue, and custom vertical builder.
            • Long-Term (2028+): Multi-seat agency workspaces and shareable client micro-preview links.
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
                    text = "ISO/IEC 27701 & 150+ Country Cybersecurity/Privacy Suite (Oct 10, 2026)",
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
                            .verticalScroll(rememberScrollState())
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
                            text = "Root File: /${selectedTab.fileName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = selectedTab.content,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.testTag("compliance_doc_content")
                        )
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
