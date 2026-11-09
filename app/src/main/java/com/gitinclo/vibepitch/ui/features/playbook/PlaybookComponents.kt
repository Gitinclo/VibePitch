package com.gitinclo.vibepitch.ui.features.playbook

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gitinclo.vibepitch.data.model.PlaybookTask
import com.gitinclo.vibepitch.ui.ScopeGuardrailConfig
import com.gitinclo.vibepitch.ui.theme.EmeraldConversion

@Composable
fun ScopeGuardrailBuilderCard(
    scopeGuardrails: ScopeGuardrailConfig,
    onUpdateScopeGuardrails: (String, String, String, Int) -> Unit
) {
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
}

@Composable
fun ViewportQaAuditorCard(
    qaRemediationItems: List<String>,
    qaRemediationChecks: SnapshotStateList<Boolean>
) {
    val qaPassedCount = qaRemediationChecks.count { it }
    val qaReadinessProgress = qaPassedCount.toFloat() / qaRemediationItems.size.toFloat()

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
}

@Composable
fun PlaybookProgressHeaderCard(
    brandAccent: Color,
    completedCount: Int,
    totalTasks: Int,
    progress: Float
) {
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
                            text = "$completedCount of $totalTasks milestones completed",
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
}

@Composable
fun PlaybookTaskCard(
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

@Composable
fun ComplianceSuiteEntryCard(
    onOpenComplianceDocs: () -> Unit
) {
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
}
