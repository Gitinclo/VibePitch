package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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

            ScopeGuardrailBuilderCard(
                scopeGuardrails = scopeGuardrails,
                onUpdateScopeGuardrails = onUpdateScopeGuardrails
            )

            VibeGuardrailsExplorerCard(
                brandAccent = brandAccent,
                onShowMessage = onShowMessage
            )

            ViewportQaAuditorCard(
                qaRemediationItems = qaRemediationItems,
                qaRemediationChecks = qaRemediationChecks
            )

            PlaybookProgressHeaderCard(
                brandAccent = brandAccent,
                completedCount = completedCount,
                totalTasks = tasks.size,
                progress = progress
            )

            tasks.forEach { task ->
                PlaybookTaskCard(
                    task = task,
                    onToggle = { onToggleTask(task) }
                )
            }

            ComplianceSuiteEntryCard(
                onOpenComplianceDocs = onOpenComplianceDocs
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
