package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.PlaybookTask
import com.example.data.model.RevenueStreamMode
import com.example.ui.ActiveDemoBrandState
import com.example.ui.GeneratedPitchScripts
import com.example.ui.OfferArchetype
import com.example.ui.ScopeGuardrailConfig
import com.example.ui.UiState
import com.example.ui.theme.EmeraldConversion
import com.example.ui.theme.parseBrandColor

@Composable
fun PlaybookAndPitchScreen(
    activeBrand: ActiveDemoBrandState,
    selectedOfferArchetype: OfferArchetype,
    scopeGuardrails: ScopeGuardrailConfig,
    playbookState: UiState<List<PlaybookTask>>,
    pitchScripts: GeneratedPitchScripts,
    onSwitchRevenueStream: (RevenueStreamMode) -> Unit,
    onSelectOfferArchetype: (OfferArchetype) -> Unit,
    onSelectStarterPrice: (Int) -> Unit,
    onUpdateScopeGuardrails: (String, String, String, Int) -> Unit,
    onToggleTask: (PlaybookTask) -> Unit,
    onShowMessage: (String) -> Unit,
    onOpenComplianceDocs: () -> Unit = {}
) {
    val context = LocalContext.current
    val brandAccent = parseBrandColor(activeBrand.brandColorHex)
    // 0 = IG/FB DM, 1 = Upwork/Rescue, 2 = Email, 3 = 90s Loom, 4 = Scope & Guardrails, 5 = Testimonial & Referrals
    var selectedScriptTab by remember { mutableIntStateOf(0) }

    val qaRemediationChecks = remember {
        mutableStateListOf(true, true, true, false, false, false)
    }
    val qaRemediationItems = remember {
        listOf(
            "Viewport QA: Tested every user flow, button, and form on both mobile phone and desktop viewports",
            "Payment & PII Guardrail: Zero custom credit-card or raw PII storage—offloaded to Stripe Links / Google Forms",
            "Auth & Database Guardrail: Uses managed Google Sign-In & strict per-user security rules (no broken AI auth loops)",
            "AI Prototype Remediation: Scrubbed hardcoded API keys into server secrets & verified graceful error handling",
            "Plain-English Scope Locked: Client agreed on what the tool does, what it doesn't do, and post-delivery support window",
            "Commercial Loop Closed: Delivered fast & sent the 2-sentence Testimonial + Local Referral request"
        )
    }
    val qaPassedCount = qaRemediationChecks.count { it }
    val qaReadinessProgress = qaPassedCount.toFloat() / qaRemediationItems.size.toFloat()

    val tasks = (playbookState as? UiState.Success)?.data ?: emptyList()
    val completedCount = tasks.count { it.isCompleted }
    val totalCount = tasks.size.coerceAtLeast(1)
    val progress = completedCount.toFloat() / totalCount.toFloat()

    val currentScriptText = when (selectedScriptTab) {
        0 -> pitchScripts.igFacebookDmScript
        1 -> pitchScripts.marketplaceProposalScript
        2 -> pitchScripts.coldOutreachMessage
        3 -> pitchScripts.loomTeleprompterScript
        4 -> pitchScripts.deRiskedOfferSheet
        else -> pitchScripts.testimonialAndReferralScript
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Multi-Stream Outreach, Proposal & Testimonial Drafter
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, brandAccent.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = brandAccent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Multi-Stream Outreach & Proposal Engine",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "${activeBrand.businessName} • ${activeBrand.niche.displayName} (${activeBrand.niche.revenueStream.shortBadge})",
                                style = MaterialTheme.typography.bodySmall,
                                color = brandAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // 1. Revenue Stream Selector
                    Text(
                        text = "1. Target Revenue Stream:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RevenueStreamMode.entries.forEach { stream ->
                            FilterChip(
                                selected = activeBrand.niche.revenueStream == stream,
                                onClick = { onSwitchRevenueStream(stream) },
                                label = { Text(stream.shortBadge) },
                                modifier = Modifier.testTag("playbook_stream_${stream.key.lowercase()}")
                            )
                        }
                    }

                    // 2. Offer Archetype Selector
                    Text(
                        text = "2. Deliverable Outcome (Clients pay for utility, not code):",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OfferArchetype.entries.forEach { archetype ->
                            FilterChip(
                                selected = selectedOfferArchetype == archetype,
                                onClick = { onSelectOfferArchetype(archetype) },
                                label = { Text(archetype.shortLabel) },
                                modifier = Modifier.testTag("offer_archetype_${archetype.name.lowercase()}")
                            )
                        }
                    }

                    // 3. Fixed Starter Price ($100 to $500)
                    Text(
                        text = "3. Fixed Starter Rate ($100–$500):",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(150, 250, 350, 500).forEach { price ->
                            FilterChip(
                                selected = activeBrand.setupFeeOffer == price,
                                onClick = { onSelectStarterPrice(price) },
                                label = { Text("$$price Flat") },
                                modifier = Modifier.testTag("starter_price_$price")
                            )
                        }
                    }

                    HorizontalDivider()

                    // 6 Script Channel Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedScriptTab == 0,
                            onClick = { selectedScriptTab = 0 },
                            label = { Text("IG / FB DM") },
                            modifier = Modifier.testTag("script_tab_dm")
                        )
                        FilterChip(
                            selected = selectedScriptTab == 1,
                            onClick = { selectedScriptTab = 1 },
                            label = { Text("Upwork / Rescue") },
                            modifier = Modifier.testTag("script_tab_marketplace")
                        )
                        FilterChip(
                            selected = selectedScriptTab == 2,
                            onClick = { selectedScriptTab = 2 },
                            label = { Text("Email Pitch") },
                            modifier = Modifier.testTag("script_tab_email")
                        )
                        FilterChip(
                            selected = selectedScriptTab == 3,
                            onClick = { selectedScriptTab = 3 },
                            label = { Text("90s Loom") },
                            modifier = Modifier.testTag("script_tab_loom")
                        )
                        FilterChip(
                            selected = selectedScriptTab == 4,
                            onClick = { selectedScriptTab = 4 },
                            label = { Text("Scope & $${activeBrand.setupFeeOffer}") },
                            modifier = Modifier.testTag("script_tab_offer")
                        )
                        FilterChip(
                            selected = selectedScriptTab == 5,
                            onClick = { selectedScriptTab = 5 },
                            label = { Text("Testimonial & Referrals") },
                            modifier = Modifier.testTag("script_tab_referral")
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = currentScriptText,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .padding(16.dp)
                                .testTag("generated_pitch_script_text")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(
                                    ClipData.newPlainText("VibePitch Script", currentScriptText)
                                )
                                onShowMessage("Copied script for ${activeBrand.businessName}!")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = brandAccent),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("copy_pitch_script_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Script")
                        }

                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "${selectedOfferArchetype.title} for ${activeBrand.businessName}")
                                    putExtra(Intent.EXTRA_TEXT, currentScriptText)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Script"))
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("share_pitch_script_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share Pitch")
                        }
                    }
                }
            }

            // Section 2: Plain-English Scope & Security Guardrail Builder
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = EmeraldConversion
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Plain-English Scope & Security Guardrails",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Configure trusted providers & maintenance terms (updates Scope & Proposal tabs live):",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Payment Handling (Never write custom card processing):",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Stripe Payment Links / Native Checkout",
                            "Calendly / Square Paid Booking Link",
                            "Shopify Native Checkout"
                        ).forEach { option ->
                            FilterChip(
                                selected = scopeGuardrails.paymentGuardrail == option,
                                onClick = {
                                    onUpdateScopeGuardrails(
                                        option,
                                        scopeGuardrails.dataPrivacyGuardrail,
                                        scopeGuardrails.authGuardrail,
                                        scopeGuardrails.supportWindowDays
                                    )
                                },
                                label = { Text(option) }
                            )
                        }
                    }

                    Text(
                        text = "Personal Data & Intake Boundary:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Google Forms / Zero-Trust Cloud Rules",
                            "Direct SMS / Email Forward (Zero DB PII)",
                            "Airtable / Google Sheets Webhook"
                        ).forEach { option ->
                            FilterChip(
                                selected = scopeGuardrails.dataPrivacyGuardrail == option,
                                onClick = {
                                    onUpdateScopeGuardrails(
                                        scopeGuardrails.paymentGuardrail,
                                        option,
                                        scopeGuardrails.authGuardrail,
                                        scopeGuardrails.supportWindowDays
                                    )
                                },
                                label = { Text(option) }
                            )
                        }
                    }

                    Text(
                        text = "Post-Delivery Bug Fix & Handover Window:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(7, 14, 30).forEach { days ->
                            FilterChip(
                                selected = scopeGuardrails.supportWindowDays == days,
                                onClick = {
                                    onUpdateScopeGuardrails(
                                        scopeGuardrails.paymentGuardrail,
                                        scopeGuardrails.dataPrivacyGuardrail,
                                        scopeGuardrails.authGuardrail,
                                        days
                                    )
                                },
                                label = { Text("$days Days Free Fixes") }
                            )
                        }
                    }
                }
            }

            // Section 3: Build, Viewport QA & AI Prototype Remediation Auditor
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = EmeraldConversion
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Viewport QA & AI Prototype Auditor",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$qaPassedCount of ${qaRemediationItems.size} production & security gates verified",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            color = EmeraldConversion.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "${(qaReadinessProgress * 100).toInt()}% Ready",
                                style = MaterialTheme.typography.labelLarge,
                                color = EmeraldConversion,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { qaReadinessProgress },
                        color = EmeraldConversion,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                    )

                    qaRemediationItems.forEachIndexed { index, itemText ->
                        val checked = qaRemediationChecks[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { qaRemediationChecks[index] = !checked }
                                .padding(vertical = 6.dp, horizontal = 4.dp)
                                .testTag("preship_check_$index"),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = if (checked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = if (checked) "Verified" else "Mark verified",
                                tint = if (checked) EmeraldConversion else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = itemText,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Section 4: 14-Day Execution Progress Card
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                tint = brandAccent
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "14-Day Client Acquisition Protocol",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = "$completedCount of ${tasks.size} milestones completed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            color = EmeraldConversion.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                color = EmeraldConversion,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        color = EmeraldConversion,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(50))
                    )
                }
            }

            // Checklist Items
            tasks.forEach { task ->
                PlaybookTaskCard(
                    task = task,
                    onToggle = { onToggleTask(task) }
                )
            }

            // Section 5: Global Compliance, Security & Governance Hub
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = EmeraldConversion
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Docs & Global Compliance Suite",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Browse README, SECURITY, PRIVACY (150+ countries), TERMS, ACCOUNT, CONTRIBUTING, CODE OF CONDUCT & ROADMAP",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenComplianceDocs,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("playbook_open_compliance_docs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Docs & Compliance Hub")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PlaybookTaskCard(
    task: PlaybookTask,
    onToggle: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .testTag("playbook_task_${task.id}")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = if (task.isCompleted) {
                    Icons.Default.CheckCircle
                } else {
                    Icons.Default.RadioButtonUnchecked
                },
                contentDescription = if (task.isCompleted) "Completed" else "Mark completed",
                tint = if (task.isCompleted) EmeraldConversion else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "STEP ${task.stepNumber} • ${task.dayRange.uppercase()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (task.proTip.isNotBlank()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = EmeraldConversion,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = task.proTip,
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldConversion
                        )
                    }
                }
            }
        }
    }
}
