package com.gitinclo.vibepitch.ui.features.livedemo

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gitinclo.vibepitch.data.model.NicheCatalog
import com.gitinclo.vibepitch.data.model.NicheKey
import com.gitinclo.vibepitch.data.model.RevenueStreamMode
import com.gitinclo.vibepitch.ui.ActiveDemoBrandState
import com.gitinclo.vibepitch.ui.QuoteCalculatorState
import com.gitinclo.vibepitch.ui.theme.parseBrandColor
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
            DemoSwitcherTopBar(
                activeBrand = activeBrand,
                brandAccent = brandAccent,
                isPresentationMode = isPresentationMode,
                onTogglePresentationMode = onTogglePresentationMode,
                onSwitchRevenueStream = onSwitchRevenueStream,
                onSwitchNichePreset = onSwitchNichePreset,
                onOpenBrandStudio = onOpenBrandStudio
            )

            DemoWhiteLabelHeroCard(
                activeBrand = activeBrand,
                brandAccent = brandAccent,
                estimatedTotal = estimatedTotal,
                depositAmount = depositAmount
            )

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
