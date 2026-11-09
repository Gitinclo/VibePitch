package com.gitinclo.vibepitch.ui.features.studio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gitinclo.vibepitch.data.model.NicheKey
import com.gitinclo.vibepitch.ui.ActiveDemoBrandState
import com.gitinclo.vibepitch.ui.theme.parseBrandColor
import kotlin.math.roundToInt

private val BrandSwatches = listOf(
    "#F97316" to "HVAC Flame",
    "#EC4899" to "Salon Magenta",
    "#06B6D4" to "Dental Cyan",
    "#3B82F6" to "Detailing Cobalt",
    "#10B981" to "Fitness Emerald",
    "#8B5CF6" to "Shopify Violet",
    "#EAB308" to "Rescue Gold"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BrandSwatchPricingCard(
    activeBrand: ActiveDemoBrandState,
    accentColor: Color,
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
    ) -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Brand Theme Swatch & Deposit Terms",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BrandSwatches.forEach { (hex, label) ->
                    val swatchColor = parseBrandColor(hex)
                    val isSelected = activeBrand.brandColorHex.equals(hex, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) {
                            swatchColor.copy(alpha = 0.18f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) swatchColor else Color.Transparent
                        ),
                        modifier = Modifier
                            .clickable {
                                onUpdateBrand(
                                    activeBrand.businessName,
                                    activeBrand.contactName,
                                    activeBrand.niche,
                                    activeBrand.tagline,
                                    hex,
                                    activeBrand.websiteGapNote,
                                    activeBrand.depositPercent,
                                    activeBrand.setupFeeOffer,
                                    activeBrand.monthlyRetainerOffer
                                )
                            }
                            .testTag("swatch_${hex.removePrefix("#")}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(swatchColor)
                                    .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Booking Deposit Required",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = "${activeBrand.depositPercent}%",
                        style = MaterialTheme.typography.titleMedium,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = activeBrand.depositPercent.toFloat(),
                    onValueChange = { raw ->
                        onUpdateBrand(
                            activeBrand.businessName,
                            activeBrand.contactName,
                            activeBrand.niche,
                            activeBrand.tagline,
                            activeBrand.brandColorHex,
                            activeBrand.websiteGapNote,
                            raw.roundToInt(),
                            activeBrand.setupFeeOffer,
                            activeBrand.monthlyRetainerOffer
                        )
                    },
                    valueRange = 10f..50f,
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = activeBrand.setupFeeOffer.toString(),
                    onValueChange = { text ->
                        val parsed = text.filter { it.isDigit() }.toIntOrNull() ?: 250
                        onUpdateBrand(
                            activeBrand.businessName,
                            activeBrand.contactName,
                            activeBrand.niche,
                            activeBrand.tagline,
                            activeBrand.brandColorHex,
                            activeBrand.websiteGapNote,
                            activeBrand.depositPercent,
                            parsed,
                            activeBrand.monthlyRetainerOffer
                        )
                    },
                    label = { Text("Setup Offer ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = activeBrand.monthlyRetainerOffer.toString(),
                    onValueChange = { text ->
                        val parsed = text.filter { it.isDigit() }.toIntOrNull() ?: 35
                        onUpdateBrand(
                            activeBrand.businessName,
                            activeBrand.contactName,
                            activeBrand.niche,
                            activeBrand.tagline,
                            activeBrand.brandColorHex,
                            activeBrand.websiteGapNote,
                            activeBrand.depositPercent,
                            activeBrand.setupFeeOffer,
                            parsed
                        )
                    },
                    label = { Text("Monthly Retainer ($/mo)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
