package com.gitinclo.vibepitch.ui.features.performance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gitinclo.vibepitch.performance.GpuRenderScaleMode
import com.gitinclo.vibepitch.performance.HardwareTelemetryState
import com.gitinclo.vibepitch.performance.RefreshRateMode
import com.gitinclo.vibepitch.ui.theme.AmberHighlight
import com.gitinclo.vibepitch.ui.theme.ElectricIndigo
import com.gitinclo.vibepitch.ui.theme.EmeraldConversion
import java.util.Locale

@Composable
fun HardwareTelemetryTopBarBadge(
    telemetry: HardwareTelemetryState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dotColor = when {
        telemetry.isThermalThrottlingActive -> AmberHighlight
        !telemetry.withinFrameBudget -> Color(0xFFF43F5E)
        telemetry.targetFps == 60 -> AmberHighlight
        else -> EmeraldConversion
    }
    val labelText = String.format(
        Locale.US,
        "%dHz • %.1fms",
        telemetry.targetFps,
        telemetry.lastFrameTimeMs
    )
    val accessibilityDescription =
        "Realme 8 5G Hardware Governor: ${telemetry.targetFps}Hz target, ${telemetry.lastFrameTimeMs}ms frame time, ${telemetry.thermalStatusLabel}"

    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, dotColor.copy(alpha = 0.55f)),
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .semantics { contentDescription = accessibilityDescription }
            .testTag("hardware_telemetry_chip")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
                    .testTag("hardware_telemetry_dot")
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = null,
                tint = dotColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = labelText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Realme8HardwareHudDialog(
    telemetry: HardwareTelemetryState,
    onSelectRefreshRateMode: (RefreshRateMode) -> Unit,
    onSelectGpuRenderScaleMode: (GpuRenderScaleMode) -> Unit,
    onPerformMemoryTrim: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("realme_hardware_hud_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = null,
                    tint = EmeraldConversion,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Realme 8 5G Hardware Governor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Dimensity 700 • 90Hz 1080x2400 • 4GB LPDDR4X Baseline",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. SoC & Cortex-A76 / Cortex-A55 Thread Affinity Card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "CPU Topology & EAS Thread Pinning",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = ElectricIndigo
                        )
                        Text(
                            text = telemetry.socModel,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "• UI Render Loop: 2x Cortex-A76 @ 2.2 GHz (Display Priority)\n" +
                                "• Background Pool: 6x Cortex-A55 @ 2.0 GHz (THREAD_PRIORITY_BACKGROUND)\n" +
                                "• UFS 2.1 Sequential I/O Queue: ${telemetry.ufsAsyncIoQueueDepth} pending",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 2. 90Hz Display & Frame-Time Budget Card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, EmeraldConversion.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "90Hz Display & Thermal Governor",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldConversion
                            )
                            Text(
                                text = "Thermal: ${telemetry.thermalStatusLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (telemetry.isThermalThrottlingActive) AmberHighlight else EmeraldConversion
                            )
                        }

                        Text(
                            text = String.format(
                                Locale.US,
                                "Target: %d FPS (Budget %.1fms) • Sampled Frame: %.1fms (500ms cadence)",
                                telemetry.targetFps,
                                telemetry.frameBudgetMs,
                                telemetry.lastFrameTimeMs
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )

                        // Single-pass flat vector gauge protecting Mali-G57 MC2 fill rate
                        val budgetFraction = (telemetry.lastFrameTimeMs / telemetry.frameBudgetMs).coerceIn(0.05f, 1f)
                        val barColor = if (telemetry.withinFrameBudget) EmeraldConversion else Color(0xFFF43F5E)
                        val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .testTag("hud_frame_budget_gauge")
                        ) {
                            drawRoundRect(
                                color = trackColor,
                                size = size,
                                cornerRadius = CornerRadius(8f, 8f)
                            )
                            drawRoundRect(
                                color = barColor,
                                size = Size(width = size.width * budgetFraction, height = size.height),
                                cornerRadius = CornerRadius(8f, 8f)
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilterChip(
                                selected = telemetry.refreshRateMode == RefreshRateMode.AUTO_90HZ_THERMAL_GUARD,
                                onClick = { onSelectRefreshRateMode(RefreshRateMode.AUTO_90HZ_THERMAL_GUARD) },
                                label = { Text("Auto 90Hz/60Hz", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("refresh_mode_auto")
                            )
                            FilterChip(
                                selected = telemetry.refreshRateMode == RefreshRateMode.LOCKED_90HZ,
                                onClick = { onSelectRefreshRateMode(RefreshRateMode.LOCKED_90HZ) },
                                label = { Text("Lock 90Hz", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("refresh_mode_90hz")
                            )
                            FilterChip(
                                selected = telemetry.refreshRateMode == RefreshRateMode.THERMAL_SAVER_60HZ,
                                onClick = { onSelectRefreshRateMode(RefreshRateMode.THERMAL_SAVER_60HZ) },
                                label = { Text("60Hz Thermal Cap", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("refresh_mode_60hz")
                            )
                        }
                    }
                }

                // 3. ARM Mali-G57 MC2 GPU & Non-Destructive Render Scale Card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Mali-G57 MC2 GPU & Non-Destructive Scaling",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = telemetry.gpuModel,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = if (telemetry.astcAndMsaaEnabled) {
                                "ASTC Compression + 4x MSAA Active • Full 1080x2400 Shaders"
                            } else {
                                "720p Fill-Rate Saver Active • MSAA Bypassed (Preserves 1080x2400 Layout)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilterChip(
                                selected = telemetry.gpuRenderScaleMode == GpuRenderScaleMode.NATIVE_1080P,
                                onClick = { onSelectGpuRenderScaleMode(GpuRenderScaleMode.NATIVE_1080P) },
                                label = { Text("1080p Native (4x MSAA)", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("gpu_scale_native_1080p")
                            )
                            FilterChip(
                                selected = telemetry.gpuRenderScaleMode == GpuRenderScaleMode.SAVER_720P_SCALE,
                                onClick = { onSelectGpuRenderScaleMode(GpuRenderScaleMode.SAVER_720P_SCALE) },
                                label = { Text("720p Fill-Rate Saver", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("gpu_scale_saver_720p")
                            )
                        }
                    }
                }

                // 4. 4GB LPDDR4X RAM & Non-Blocking Cache Trim Card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, AmberHighlight.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "4GB LPDDR4X RAM & Non-Blocking Cache Guard",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = AmberHighlight
                        )
                        Text(
                            text = "JVM Heap: ${telemetry.ramUsedMb} MB / ${telemetry.ramMaxMb} MB • Last Trim: ${telemetry.lastMemoryTrimLevel}",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Button(
                            onClick = onPerformMemoryTrim,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("trim_memory_action_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CleaningServices,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Purge Domain Caches (Non-Blocking 4GB Trim)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_hardware_hud_button")
            ) {
                Text("Close HUD", fontWeight = FontWeight.Bold)
            }
        }
    )
}
