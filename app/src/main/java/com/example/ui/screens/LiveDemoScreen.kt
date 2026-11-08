package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.CapturedLead
import com.example.data.model.NicheCatalog
import com.example.data.model.NicheKey
import com.example.data.model.RevenueStreamMode
import com.example.data.model.ServicePackageOption
import com.example.ui.ActiveDemoBrandState
import com.example.ui.QuoteCalculatorState
import com.example.ui.theme.EmeraldConversion
import com.example.ui.theme.ObsidianSlate
import com.example.ui.theme.parseBrandColor
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveDemoScreen(
    activeBrand: ActiveDemoBrandState,
    quoteState: QuoteCalculatorState,
    estimatedTotal: Int,
    depositAmount: Int,
    isPresentationMode: Boolean,
    onTogglePresentationMode: () -> Unit,
    onSwitchRevenueStream: (RevenueStreamMode) -> Unit,
    onSwitchNichePreset: (NicheKey) -> Unit,
    onOpenBrandStudio: () -> Unit,
    onOpenPitchScript: () -> Unit,
    onSelectService: (String) -> Unit,
    onUnitQuantityChange: (Int) -> Unit,
    onToggleAddOn: (String) -> Unit,
    onUpdateBookingForm: (String, String, String, String, String) -> Unit,
    onSubmitBooking: () -> Unit,
    onDismissLeadReceipt: () -> Unit
) {
    val brandAccent = parseBrandColor(activeBrand.brandColorHex)
    val packages = NicheCatalog.getServicePackages(activeBrand.niche)
    val addOns = NicheCatalog.getAddOns(activeBrand.niche)
    val timeWindows = listOf(
        "Tomorrow Morning (8am – 12pm)",
        "Tomorrow Afternoon (1pm – 5pm)",
        "This Weekend Slot",
        "Next Week Flexible"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Freelancer Quick Switcher Bar (Hidden in Presentation / Loom View)
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

                        // Revenue Stream Mode Switcher (3 Streams)
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

                        // 7 Vertical Preset Switcher
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
                // Subtle Floating Bar to exit Presentation Mode after recording Loom
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

            // Prospect White-Label Hero Header
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

                        // Live Calculated Price & Deposit Banner
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

            // Step 1: Select Service Package
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "1. Select Service Package",
                    style = MaterialTheme.typography.titleLarge
                )
                packages.forEach { pkg ->
                    ServicePackageCard(
                        pkg = pkg,
                        isSelected = quoteState.selectedServiceId == pkg.id,
                        brandAccent = brandAccent,
                        onSelect = { onSelectService(pkg.id) }
                    )
                }
            }

            // Step 2: Interactive Unit / Size Slider
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. ${activeBrand.niche.unitLabel}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            color = brandAccent.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "${quoteState.unitValue} units",
                                style = MaterialTheme.typography.titleMedium,
                                color = brandAccent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }

                    val minVal = activeBrand.niche.unitMin.toFloat()
                    val maxVal = activeBrand.niche.unitMax.toFloat()
                    Slider(
                        value = quoteState.unitValue.toFloat().coerceIn(minVal, maxVal),
                        onValueChange = { raw ->
                            onUnitQuantityChange(raw.roundToInt())
                        },
                        valueRange = minVal..maxVal,
                        colors = SliderDefaults.colors(
                            thumbColor = brandAccent,
                            activeTrackColor = brandAccent
                        ),
                        modifier = Modifier.testTag("quote_unit_slider")
                    )
                }
            }

            // Step 3: Optional High-Margin Add-Ons
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "3. Customize with Priority Add-Ons",
                        style = MaterialTheme.typography.titleMedium
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        addOns.forEach { addOn ->
                            val isSelected = quoteState.selectedAddOnIds.contains(addOn.id)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onToggleAddOn(addOn.id) },
                                label = {
                                    Text("${addOn.name} (+$${addOn.price})")
                                },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = brandAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = brandAccent.copy(alpha = 0.16f),
                                    selectedLabelColor = brandAccent
                                ),
                                modifier = Modifier.testTag("addon_chip_${addOn.id}")
                            )
                        }
                    }
                }
            }

            // Step 4: Booking & Deposit Intake Form
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, brandAccent.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = brandAccent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "4. Lock In Estimate & Reserve Time Slot",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Instant SMS/email confirmation + managed deposit link",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedTextField(
                        value = quoteState.customerName,
                        onValueChange = {
                            onUpdateBookingForm(
                                it,
                                quoteState.customerPhone,
                                quoteState.customerEmail,
                                quoteState.preferredDate,
                                quoteState.notes
                            )
                        },
                        label = { Text("Your Full Name *") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("booking_name_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = quoteState.customerPhone,
                            onValueChange = {
                                onUpdateBookingForm(
                                    quoteState.customerName,
                                    it,
                                    quoteState.customerEmail,
                                    quoteState.preferredDate,
                                    quoteState.notes
                                )
                            },
                            label = { Text("Mobile Phone *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("booking_phone_input")
                        )

                        OutlinedTextField(
                            value = quoteState.customerEmail,
                            onValueChange = {
                                onUpdateBookingForm(
                                    quoteState.customerName,
                                    quoteState.customerPhone,
                                    it,
                                    quoteState.preferredDate,
                                    quoteState.notes
                                )
                            },
                            label = { Text("Email (Optional)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("booking_email_input")
                        )
                    }

                    Text(
                        text = "Preferred Arrival / Session Window",
                        style = MaterialTheme.typography.labelLarge
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        timeWindows.forEach { slot ->
                            val selected = quoteState.preferredDate == slot
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    onUpdateBookingForm(
                                        quoteState.customerName,
                                        quoteState.customerPhone,
                                        quoteState.customerEmail,
                                        slot,
                                        quoteState.notes
                                    )
                                },
                                label = { Text(slot) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = quoteState.notes,
                        onValueChange = {
                            onUpdateBookingForm(
                                quoteState.customerName,
                                quoteState.customerPhone,
                                quoteState.customerEmail,
                                quoteState.preferredDate,
                                it
                            )
                        },
                        label = { Text("Address, Vehicle, or Goal Notes") },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("booking_notes_input")
                    )

                    Button(
                        onClick = onSubmitBooking,
                        enabled = !quoteState.isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = brandAccent),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("submit_booking_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Lock In $$estimatedTotal Quote ($$depositAmount Deposit)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Managed Stripe Payment Link & instant SMS notification ready",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!isPresentationMode) {
                OutlinedButton(
                    onClick = onOpenPitchScript,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("generate_loom_script_from_demo_button")
                ) {
                    Text("Generate 90-Second Loom Script for ${activeBrand.businessName}")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    val submittedLead = quoteState.latestSubmittedLead
    if (submittedLead != null) {
        LeadReceiptDialog(
            lead = submittedLead,
            brandAccent = brandAccent,
            onDismiss = onDismissLeadReceipt
        )
    }
}

@Composable
private fun ServicePackageCard(
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
private fun LeadReceiptDialog(
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
