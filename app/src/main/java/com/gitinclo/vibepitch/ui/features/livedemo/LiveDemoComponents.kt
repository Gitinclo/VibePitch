package com.gitinclo.vibepitch.ui.features.livedemo

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gitinclo.vibepitch.data.model.CapturedLead
import com.gitinclo.vibepitch.data.model.NicheKey
import com.gitinclo.vibepitch.data.model.RevenueStreamMode
import com.gitinclo.vibepitch.data.model.ServicePackageOption
import com.gitinclo.vibepitch.ui.ActiveDemoBrandState
import com.gitinclo.vibepitch.ui.theme.EmeraldConversion
import com.gitinclo.vibepitch.ui.theme.ObsidianSlate

@Composable
fun DemoSwitcherTopBar(
    activeBrand: ActiveDemoBrandState,
    brandAccent: Color,
    isPresentationMode: Boolean,
    onTogglePresentationMode: () -> Unit,
    onSwitchRevenueStream: (RevenueStreamMode) -> Unit,
    onSwitchNichePreset: (NicheKey) -> Unit,
    onOpenBrandStudio: () -> Unit
) {
    if (!isPresentationMode) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = brandAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "REVENUE STREAM & NICHE DEMO BAR",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = onOpenBrandStudio,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("customize_brand_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit brand",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rebrand", style = MaterialTheme.typography.labelMedium)
                        }
                        Button(
                            onClick = onTogglePresentationMode,
                            colors = ButtonDefaults.buttonColors(containerColor = brandAccent),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("toggle_presentation_mode_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Enter Loom presentation view",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Loom View", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RevenueStreamMode.entries.forEach { stream ->
                        val selected = activeBrand.niche.revenueStream == stream
                        FilterChip(
                            selected = selected,
                            onClick = { onSwitchRevenueStream(stream) },
                            label = { Text(stream.shortBadge) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldConversion.copy(alpha = 0.2f),
                                selectedLabelColor = EmeraldConversion
                            ),
                            modifier = Modifier.testTag("stream_chip_${stream.key.lowercase()}")
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NicheKey.entries.forEach { niche ->
                        val selected = activeBrand.niche == niche
                        FilterChip(
                            selected = selected,
                            onClick = { onSwitchNichePreset(niche) },
                            label = { Text(niche.shortBadge) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = brandAccent.copy(alpha = 0.2f),
                                selectedLabelColor = brandAccent
                            ),
                            modifier = Modifier.testTag("niche_chip_${niche.key.lowercase()}")
                        )
                    }
                }
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = EmeraldConversion.copy(alpha = 0.16f),
                shape = RoundedCornerShape(50)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(EmeraldConversion)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CLIENT PREVIEW • LOOM RECORDING MODE",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldConversion,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            TextButton(
                onClick = onTogglePresentationMode,
                modifier = Modifier.testTag("exit_presentation_mode_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FullscreenExit,
                    contentDescription = "Exit Loom view",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Exit")
            }
        }
    }
}

@Composable
fun DemoWhiteLabelHeroCard(
    activeBrand: ActiveDemoBrandState,
    brandAccent: Color,
    estimatedTotal: Int,
    depositAmount: Int
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSlate),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            brandAccent.copy(alpha = 0.88f),
                            ObsidianSlate
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${activeBrand.niche.displayName.uppercase()} • INSTANT QUOTE",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text(
                    text = activeBrand.businessName,
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White
                )
                Text(
                    text = activeBrand.tagline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "ESTIMATED TOTAL",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                        AnimatedContent(
                            targetState = estimatedTotal,
                            label = "quote_total_anim"
                        ) { targetTotal ->
                            Text(
                                text = "$$targetTotal",
                                style = MaterialTheme.typography.displayMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("live_quote_total_text")
                            )
                        }
                    }
                    Surface(
                        color = EmeraldConversion,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "${activeBrand.depositPercent}% BOOKING DEPOSIT",
                                style = MaterialTheme.typography.labelMedium,
                                color = ObsidianSlate,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$$depositAmount due today",
                                style = MaterialTheme.typography.titleMedium,
                                color = ObsidianSlate,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ServicePackageCard(
    pkg: ServicePackageOption,
    isSelected: Boolean,
    brandAccent: Color,
    onSelect: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                brandAccent.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) brandAccent else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .testTag("service_card_${pkg.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = pkg.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (pkg.popularBadge) {
                        Surface(
                            color = brandAccent,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "MOST POPULAR",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = pkg.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${pkg.durationEstimate} • +$${pkg.perUnitRate}/unit",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "From",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$${pkg.basePrice}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (isSelected) brandAccent else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun LeadReceiptDialog(
    lead: CapturedLead,
    brandAccent: Color,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = brandAccent)
            ) {
                Text("Done")
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldConversion
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Instant Quote Locked In!")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "This lead has been saved to your CRM Leads Inbox and shows your prospect how instant intake works:",
                    style = MaterialTheme.typography.bodySmall
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = lead.prospectBusinessName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Customer: ${lead.customerName} (${lead.customerPhone})")
                        Text("Package: ${lead.selectedServiceName}")
                        if (lead.selectedAddOns.isNotBlank()) {
                            Text("Add-Ons: ${lead.selectedAddOns}")
                        }
                        Text("Slot: ${lead.preferredDate}")
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = "Estimated Total: $${lead.estimatedTotal} • Deposit Due: $${lead.depositAmount}",
                            fontWeight = FontWeight.Bold,
                            color = brandAccent
                        )
                    }
                }
            }
        }
    )
}
