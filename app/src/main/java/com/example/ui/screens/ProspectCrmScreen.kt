package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.CapturedLead
import com.example.data.model.NicheKey
import com.example.data.model.PipelineStage
import com.example.data.model.ProspectProfile
import com.example.ui.UiState
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.EmeraldConversion

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
