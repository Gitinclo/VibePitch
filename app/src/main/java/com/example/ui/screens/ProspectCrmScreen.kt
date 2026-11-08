package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CapturedLead
import com.example.data.model.NicheKey
import com.example.data.model.PipelineStage
import com.example.data.model.ProspectProfile
import com.example.ui.UiState
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.EmeraldConversion
import com.example.ui.theme.parseBrandColor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProspectCrmScreen(
    prospectsState: UiState<List<ProspectProfile>>,
    leadsState: UiState<List<CapturedLead>>,
    onLoadProspectIntoDemo: (ProspectProfile) -> Unit,
    onOpenLoomScriptForProspect: (ProspectProfile) -> Unit,
    onAdvanceProspectStage: (ProspectProfile) -> Unit,
    onDeleteProspect: (String) -> Unit,
    onCreateProspect: (String, String, NicheKey, String, String) -> Unit,
    onDeleteLead: (String) -> Unit
) {
    var selectedSubTab by remember { mutableStateOf(0) } // 0 = Prospects, 1 = Captured Leads
    var showAddDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val prospects = (prospectsState as? UiState.Success)?.data ?: emptyList()
    val leads = (leadsState as? UiState.Success)?.data ?: emptyList()
    val closedCount = prospects.count { it.stage == PipelineStage.CLOSED_WON.key }
    val potentialSetupRevenue = prospects.sumOf { it.setupFeeOffer }
    val potentialMonthlyRetainer = prospects.sumOf { it.monthlyRetainerOffer }

    Scaffold(
        floatingActionButton = {
            if (selectedSubTab == 0) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = ElectricIndigo,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_prospect_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add local business prospect"
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    // Pipeline Metric Strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricPillCard(
                            label = "Target Prospects",
                            value = "${prospects.size}/15",
                            subtext = "$closedCount closed",
                            accent = ElectricIndigo,
                            modifier = Modifier.weight(1f)
                        )
                        MetricPillCard(
                            label = "Pipeline Value",
                            value = "$$potentialSetupRevenue",
                            subtext = "+$$potentialMonthlyRetainer/mo",
                            accent = EmeraldConversion,
                            modifier = Modifier.weight(1f)
                        )
                        MetricPillCard(
                            label = "Demo Leads",
                            value = "${leads.size}",
                            subtext = "Captured",
                            accent = Color(0xFFF59E0B),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    // Sub-Tab Switcher
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = selectedSubTab == 0,
                            onClick = { selectedSubTab = 0 },
                            label = { Text("Prospect Pipeline (${prospects.size})") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.PeopleAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("crm_subtab_prospects")
                        )
                        FilterChip(
                            selected = selectedSubTab == 1,
                            onClick = { selectedSubTab = 1 },
                            label = { Text("Captured Leads Inbox (${leads.size})") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Inbox,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("crm_subtab_leads")
                        )
                    }
                }

                if (selectedSubTab == 0) {
                    when (prospectsState) {
                        is UiState.Loading -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }

                        is UiState.Error -> {
                            item {
                                Text(
                                    text = prospectsState.message,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }

                        is UiState.Success -> {
                            if (prospects.isEmpty()) {
                                item {
                                    EmptyStateCard(
                                        title = "No Local Prospects Logged Yet",
                                        subtitle = "Audit 10–15 local HVAC, detailing, or fitness businesses with static contact forms and tap + to add them."
                                    )
                                }
                            } else {
                                items(prospects, key = { it.id }) { prospect ->
                                    ProspectPipelineCard(
                                        prospect = prospect,
                                        onLoadDemo = { onLoadProspectIntoDemo(prospect) },
                                        onOpenScript = { onOpenLoomScriptForProspect(prospect) },
                                        onAdvanceStage = { onAdvanceProspectStage(prospect) },
                                        onDelete = { onDeleteProspect(prospect.id) }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    when (leadsState) {
                        is UiState.Loading -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }

                        is UiState.Error -> {
                            item {
                                Text(
                                    text = leadsState.message,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }

                        is UiState.Success -> {
                            if (leads.isEmpty()) {
                                item {
                                    EmptyStateCard(
                                        title = "No Demo Bookings Captured Yet",
                                        subtitle = "Submit an estimate on the Live Demo tab to show prospects how customer leads & deposits arrive in real time."
                                    )
                                }
                            } else {
                                items(leads, key = { it.id }) { lead ->
                                    CapturedLeadCard(
                                        lead = lead,
                                        onShareLead = {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    "New Instant Quote Lead for ${lead.prospectBusinessName}:\n" +
                                                        "Customer: ${lead.customerName} (${lead.customerPhone})\n" +
                                                        "Package: ${lead.selectedServiceName}\n" +
                                                        "Estimated Total: $${lead.estimatedTotal} (Deposit: $${lead.depositAmount})\n" +
                                                        "Preferred Slot: ${lead.preferredDate}"
                                                )
                                            }
                                            context.startActivity(
                                                Intent.createChooser(shareIntent, "Forward Lead Notification")
                                            )
                                        },
                                        onDelete = { onDeleteLead(lead.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    if (showAddDialog) {
        AddProspectDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { bizName, contact, niche, gap, colorHex ->
                onCreateProspect(bizName, contact, niche, gap, colorHex)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun MetricPillCard(
    label: String,
    value: String,
    subtext: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProspectPipelineCard(
    prospect: ProspectProfile,
    onLoadDemo: () -> Unit,
    onOpenScript: () -> Unit,
    onAdvanceStage: () -> Unit,
    onDelete: () -> Unit
) {
    val brandAccent = parseBrandColor(prospect.brandColorHex)
    val niche = NicheKey.fromKey(prospect.nicheKey)
    val stage = PipelineStage.fromKey(prospect.stage)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, brandAccent.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("prospect_card_${prospect.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
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
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(brandAccent)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = prospect.businessName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${niche.shortBadge} • Contact: ${prospect.contactName.ifBlank { "Owner" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = brandAccent.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = stage.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = brandAccent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (prospect.websiteGapNote.isNotBlank()) {
                Text(
                    text = "Audit Gap: ${prospect.websiteGapNote}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Offer: $${prospect.setupFeeOffer} after live + $${prospect.monthlyRetainerOffer}/mo",
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldConversion,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete prospect",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onLoadDemo,
                    colors = ButtonDefaults.buttonColors(containerColor = brandAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("load_demo_button_${prospect.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Load Demo", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = onOpenScript,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("loom_script_button_${prospect.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Loom Script", style = MaterialTheme.typography.labelMedium)
                }

                TextButton(
                    onClick = onAdvanceStage,
                    modifier = Modifier.testTag("advance_stage_button_${prospect.id}")
                ) {
                    Text("Next", style = MaterialTheme.typography.labelMedium)
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Advance pipeline stage",
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CapturedLeadCard(
    lead: CapturedLead,
    onShareLead: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${lead.customerName} • ${lead.customerPhone}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Demo Business: ${lead.prospectBusinessName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = EmeraldConversion.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "$${lead.estimatedTotal} (Dep: $${lead.depositAmount})",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldConversion,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "Service: ${lead.selectedServiceName} (${lead.unitQuantity} units)",
                style = MaterialTheme.typography.bodySmall
            )
            if (lead.selectedAddOns.isNotBlank()) {
                Text(
                    text = "Add-Ons: ${lead.selectedAddOns}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Slot: ${lead.preferredDate}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onShareLead) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share lead details"
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete lead"
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    title: String,
    subtitle: String
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddProspectDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, NicheKey, String, String) -> Unit
) {
    var businessName by remember { mutableStateOf("") }
    var contactName by remember { mutableStateOf("") }
    var selectedNiche by remember { mutableStateOf(NicheKey.HOME_TRADES) }
    var websiteGapNote by remember {
        mutableStateOf("Static 'Contact Us' form with no instant ballpark quote calculator.")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Local Business Prospect") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = businessName,
                    onValueChange = { businessName = it },
                    label = { Text("Business Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_prospect_name_input")
                )
                OutlinedTextField(
                    value = contactName,
                    onValueChange = { contactName = it },
                    label = { Text("Owner / Contact Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Select Niche Template", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NicheKey.entries.forEach { niche ->
                        FilterChip(
                            selected = selectedNiche == niche,
                            onClick = { selectedNiche = niche },
                            label = { Text(niche.shortBadge) }
                        )
                    }
                }
                OutlinedTextField(
                    value = websiteGapNote,
                    onValueChange = { websiteGapNote = it },
                    label = { Text("Website Audit Note") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        businessName,
                        contactName,
                        selectedNiche,
                        websiteGapNote,
                        selectedNiche.defaultColorHex
                    )
                },
                modifier = Modifier.testTag("confirm_add_prospect_button")
            ) {
                Text("Add Prospect")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
