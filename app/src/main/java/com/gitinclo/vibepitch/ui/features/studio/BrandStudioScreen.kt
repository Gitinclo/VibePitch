package com.gitinclo.vibepitch.ui.features.studio

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gitinclo.vibepitch.data.model.NicheKey
import com.gitinclo.vibepitch.ui.ActiveDemoBrandState
import com.gitinclo.vibepitch.ui.theme.parseBrandColor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BrandStudioScreen(
    activeBrand: ActiveDemoBrandState,
    onUpdateBrand: (
        businessName: String,
        contactName: String,
        niche: NicheKey,
        tagline: String,
        brandColorHex: String,
        websiteGapNote: String,
        depositPercent: Int,
        setupFeeOffer: Int,
        monthlyRetainerOffer: Int
    ) -> Unit,
    onSwitchNichePreset: (NicheKey) -> Unit,
    onPreviewLiveDemo: () -> Unit,
    onSaveProspectAndLaunch: () -> Unit
) {
    val accentColor = parseBrandColor(activeBrand.brandColorHex)

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
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = accentColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "15-Second White-Label Brand Studio",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Pick a niche template and customize a prospect's branding before recording your 90-second Loom.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Select Local Business Niche Preset",
                        style = MaterialTheme.typography.labelLarge
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NicheKey.entries.forEach { niche ->
                            val selected = activeBrand.niche == niche
                            FilterChip(
                                selected = selected,
                                onClick = { onSwitchNichePreset(niche) },
                                label = { Text(niche.displayName) },
                                leadingIcon = if (selected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accentColor.copy(alpha = 0.18f),
                                    selectedLabelColor = accentColor
                                ),
                                modifier = Modifier.testTag("studio_niche_${niche.key.lowercase()}")
                            )
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Prospect Identity & Website Audit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = activeBrand.businessName,
                        onValueChange = {
                            onUpdateBrand(
                                it,
                                activeBrand.contactName,
                                activeBrand.niche,
                                activeBrand.tagline,
                                activeBrand.brandColorHex,
                                activeBrand.websiteGapNote,
                                activeBrand.depositPercent,
                                activeBrand.setupFeeOffer,
                                activeBrand.monthlyRetainerOffer
                            )
                        },
                        label = { Text("Prospect Business Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("studio_business_name_input")
                    )

                    OutlinedTextField(
                        value = activeBrand.contactName,
                        onValueChange = {
                            onUpdateBrand(
                                activeBrand.businessName,
                                it,
                                activeBrand.niche,
                                activeBrand.tagline,
                                activeBrand.brandColorHex,
                                activeBrand.websiteGapNote,
                                activeBrand.depositPercent,
                                activeBrand.setupFeeOffer,
                                activeBrand.monthlyRetainerOffer
                            )
                        },
                        label = { Text("Owner / Decision Maker First Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("studio_contact_name_input")
                    )

                    OutlinedTextField(
                        value = activeBrand.tagline,
                        onValueChange = {
                            onUpdateBrand(
                                activeBrand.businessName,
                                activeBrand.contactName,
                                activeBrand.niche,
                                it,
                                activeBrand.brandColorHex,
                                activeBrand.websiteGapNote,
                                activeBrand.depositPercent,
                                activeBrand.setupFeeOffer,
                                activeBrand.monthlyRetainerOffer
                            )
                        },
                        label = { Text("Hero Value Tagline") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("studio_tagline_input")
                    )

                    OutlinedTextField(
                        value = activeBrand.websiteGapNote,
                        onValueChange = {
                            onUpdateBrand(
                                activeBrand.businessName,
                                activeBrand.contactName,
                                activeBrand.niche,
                                activeBrand.tagline,
                                activeBrand.brandColorHex,
                                it,
                                activeBrand.depositPercent,
                                activeBrand.setupFeeOffer,
                                activeBrand.monthlyRetainerOffer
                            )
                        },
                        label = { Text("Website Gap Audit Note (Used in Loom Script)") },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("studio_gap_note_input")
                    )
                }
            }

            BrandSwatchPricingCard(
                activeBrand = activeBrand,
                accentColor = accentColor,
                onUpdateBrand = onUpdateBrand
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onPreviewLiveDemo,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("preview_live_demo_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.RocketLaunch,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Preview Demo")
                }

                Button(
                    onClick = onSaveProspectAndLaunch,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(52.dp)
                        .testTag("save_prospect_and_launch_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Save to CRM & Launch",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
