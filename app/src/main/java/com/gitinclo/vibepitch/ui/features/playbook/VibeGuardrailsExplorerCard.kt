package com.gitinclo.vibepitch.ui.features.playbook

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gitinclo.vibepitch.domain.GuardrailCategory
import com.gitinclo.vibepitch.domain.GuardrailExportFormat
import com.gitinclo.vibepitch.domain.VibeCodingGuardrailsCatalog
import com.gitinclo.vibepitch.ui.theme.EmeraldConversion

@Composable
fun VibeGuardrailsExplorerCard(
    brandAccent: Color,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCategoryName by rememberSaveable { mutableStateOf<String?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedExportFormat by rememberSaveable { mutableStateOf(GuardrailExportFormat.CLAUDE_MD) }

    val selectedCategory = remember(selectedCategoryName) {
        GuardrailCategory.entries.firstOrNull { it.name == selectedCategoryName }
    }

    val filteredRules = remember(selectedCategory, searchQuery) {
        VibeCodingGuardrailsCatalog.filterRules(selectedCategory, searchQuery)
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, EmeraldConversion.copy(alpha = 0.45f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("vibe_guardrails_explorer_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = EmeraldConversion
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "100 Vibe-Coding Agent Guardrails",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "10 Categories (Rules 1–100) • Export to CLAUDE.md, .cursorrules, or .windsurfrules",
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
                        text = "${filteredRules.size}/100",
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldConversion,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("guardrails_count_badge")
                    )
                }
            }

            // Search bar across all 100 guardrails
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search 100 guardrails (e.g. #11, monolith, secrets, IDOR, 3-strike)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("guardrails_search_input")
            )

            // Category Filter Chips (All 100 + I through X)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategoryName = null },
                    label = { Text("All 100 (I–X)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldConversion.copy(alpha = 0.2f),
                        selectedLabelColor = EmeraldConversion
                    ),
                    modifier = Modifier.testTag("guardrail_cat_all")
                )
                GuardrailCategory.entries.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedCategoryName = if (isSelected) null else cat.name
                        },
                        label = { Text("${cat.shortLabel} (${cat.rangeLabel})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = brandAccent.copy(alpha = 0.18f),
                            selectedLabelColor = brandAccent
                        ),
                        modifier = Modifier.testTag("guardrail_cat_${cat.romanNumeral.lowercase()}")
                    )
                }
            }

            if (selectedCategory != null) {
                Surface(
                    color = brandAccent.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            text = selectedCategory.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = brandAccent,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = selectedCategory.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Scrollable Guardrails List
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 320.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filteredRules.isEmpty()) {
                        Text(
                            text = "No guardrails match \"$searchQuery\". Try clearing the search filter.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        filteredRules.forEach { rule ->
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("guardrail_rule_item_${rule.number}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "#${rule.number}. ${rule.title}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = rule.category.romanNumeral,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldConversion,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = rule.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // Ready-to-Use File Exporter (CLAUDE.md / .cursorrules / .windsurfrules / Agent Prompt)
            Text(
                text = "Package & Export Ready-to-Use Guardrail File:",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GuardrailExportFormat.entries.forEach { format ->
                    FilterChip(
                        selected = selectedExportFormat == format,
                        onClick = { selectedExportFormat = format },
                        label = { Text(format.label) },
                        modifier = Modifier.testTag("guardrail_export_format_${format.name.lowercase()}")
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val exportedText = VibeCodingGuardrailsCatalog.exportGuardrailsBlock(
                            format = selectedExportFormat,
                            categoryFilter = selectedCategory
                        )
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText(selectedExportFormat.fileName, exportedText)
                        )
                        onShowMessage(
                            "Copied ${filteredRules.size} guardrails formatted for ${selectedExportFormat.fileName}!"
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldConversion),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("copy_guardrails_export_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy ${selectedExportFormat.label}")
                }

                OutlinedButton(
                    onClick = {
                        val exportedText = VibeCodingGuardrailsCatalog.exportGuardrailsBlock(
                            format = selectedExportFormat,
                            categoryFilter = selectedCategory
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "${selectedExportFormat.fileName} — 100 Vibe Coding Guardrails")
                            putExtra(Intent.EXTRA_TEXT, exportedText)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share ${selectedExportFormat.fileName}"))
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("share_guardrails_export_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share File")
                }
            }
        }
    }
}
