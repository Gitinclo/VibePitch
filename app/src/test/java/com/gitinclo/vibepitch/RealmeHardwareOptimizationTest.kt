package com.gitinclo.vibepitch

import android.content.ComponentCallbacks2
import android.content.Context
import android.os.PowerManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.gitinclo.vibepitch.performance.DimensityCpuScheduler
import com.gitinclo.vibepitch.performance.DimensityPerformanceProfile
import com.gitinclo.vibepitch.performance.GpuRenderScaleMode
import com.gitinclo.vibepitch.performance.HardwareGovernor
import com.gitinclo.vibepitch.performance.HardwareTelemetryState
import com.gitinclo.vibepitch.performance.RealmeHardwareOptimizer
import com.gitinclo.vibepitch.performance.RefreshRateMode
import com.gitinclo.vibepitch.ui.features.performance.HardwareTelemetryTopBarBadge
import com.gitinclo.vibepitch.ui.features.performance.Realme8HardwareHudDialog
import com.gitinclo.vibepitch.ui.theme.VibePitchTheme
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RealmeHardwareOptimizationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun dimensityCpuScheduler_routesWorkToNamedA55BackgroundAndUfsSequentialThreads() = runBlocking {
        val a55ThreadName = withContext(DimensityCpuScheduler.a55BackgroundDispatcher) {
            Thread.currentThread().name
        }
        assertTrue(
            "Expected Dimensity-A55-Worker thread, got $a55ThreadName",
            a55ThreadName.startsWith("Dimensity-A55-Worker-")
        )

        val ufsThreadName = withContext(DimensityCpuScheduler.ufsSequentialIoDispatcher) {
            Thread.currentThread().name
        }
        assertTrue(
            "Expected Realme-UFS21-Sequential-IO thread, got $ufsThreadName",
            ufsThreadName.startsWith("Realme-UFS21-Sequential-IO")
        )
    }

    @Test
    fun hardwareGovernor_managesDimensity700ProfilesAndTogglesBetween90HzAnd60HzThermalFallback() {
        val governor = HardwareGovernor(context)
        assertEquals(DimensityPerformanceProfile.BALANCED_ADAPTIVE, governor.telemetry.value.performanceProfile)
        assertEquals(90, governor.telemetry.value.targetFps)
        assertEquals(11.1f, governor.telemetry.value.frameBudgetMs, 0.01f)

        // Toggle thermal fallback ON -> 60Hz (16.6ms budget) + 720p fill-rate saver
        val fallbackActive = governor.toggleThermalFallback()
        assertTrue(fallbackActive)
        assertEquals(DimensityPerformanceProfile.THERMAL_SAVER_60HZ, governor.telemetry.value.performanceProfile)
        assertEquals(RefreshRateMode.THERMAL_SAVER_60HZ, governor.telemetry.value.refreshRateMode)
        assertEquals(60, governor.telemetry.value.targetFps)
        assertEquals(16.6f, governor.telemetry.value.frameBudgetMs, 0.01f)
        assertEquals(GpuRenderScaleMode.SAVER_720P_SCALE, governor.telemetry.value.gpuRenderScaleMode)
        assertFalse(governor.telemetry.value.astcAndMsaaEnabled)

        // Toggle thermal fallback OFF -> restores 90Hz Balanced Adaptive (11.1ms budget) + 1080p native
        val fallbackAfterRestore = governor.toggleThermalFallback()
        assertFalse(fallbackAfterRestore)
        assertEquals(DimensityPerformanceProfile.BALANCED_ADAPTIVE, governor.telemetry.value.performanceProfile)
        assertEquals(RefreshRateMode.AUTO_90HZ_THERMAL_GUARD, governor.telemetry.value.refreshRateMode)
        assertEquals(90, governor.telemetry.value.targetFps)
        assertEquals(11.1f, governor.telemetry.value.frameBudgetMs, 0.01f)
        assertEquals(GpuRenderScaleMode.NATIVE_1080P, governor.telemetry.value.gpuRenderScaleMode)
        assertTrue(governor.telemetry.value.astcAndMsaaEnabled)

        // Switch explicitly to HIGH_THROUGHPUT_90HZ profile
        governor.setPerformanceProfile(DimensityPerformanceProfile.HIGH_THROUGHPUT_90HZ)
        assertEquals(DimensityPerformanceProfile.HIGH_THROUGHPUT_90HZ, governor.telemetry.value.performanceProfile)
        assertEquals(RefreshRateMode.LOCKED_90HZ, governor.telemetry.value.refreshRateMode)
        assertEquals(90, governor.telemetry.value.targetFps)
    }

    @Test
    fun thermalGovernor_switchesTo60HzAnd720pSaverAtModerateThermalStatus() {
        val optimizer = RealmeHardwareOptimizer(context)
        assertEquals(90, optimizer.telemetry.value.targetFps)
        assertEquals(11.1f, optimizer.telemetry.value.frameBudgetMs, 0.01f)
        assertEquals(GpuRenderScaleMode.NATIVE_1080P, optimizer.telemetry.value.gpuRenderScaleMode)
        assertTrue(optimizer.telemetry.value.astcAndMsaaEnabled)

        // Simulate THERMAL_STATUS_MODERATE on Realme 8 5G
        optimizer.handleThermalStatusChanged(PowerManager.THERMAL_STATUS_MODERATE)
        val throttled = optimizer.telemetry.value
        assertTrue(throttled.isThermalThrottlingActive)
        assertEquals(60, throttled.targetFps)
        assertEquals(16.6f, throttled.frameBudgetMs, 0.01f)
        assertEquals(GpuRenderScaleMode.SAVER_720P_SCALE, throttled.gpuRenderScaleMode)
        assertFalse(throttled.astcAndMsaaEnabled)

        // Return to THERMAL_STATUS_NONE -> restores 90 FPS under AUTO_90HZ_THERMAL_GUARD
        optimizer.handleThermalStatusChanged(PowerManager.THERMAL_STATUS_NONE)
        val recovered = optimizer.telemetry.value
        assertFalse(recovered.isThermalThrottlingActive)
        assertEquals(90, recovered.targetFps)
        assertEquals(11.1f, recovered.frameBudgetMs, 0.01f)
        assertEquals(GpuRenderScaleMode.NATIVE_1080P, recovered.gpuRenderScaleMode)
        assertTrue(recovered.astcAndMsaaEnabled)
    }

    @Test
    fun frameTelemetrySampling_throttlesStateEmissionsTo500msIntervalsAndComputesRingBufferStats() {
        val governor = HardwareGovernor(context)

        // Initial frame anchor at t = 1,000ms (1_000_000_000 ns)
        governor.recordFrameSample(1_000_000_000L)

        // Next frame at t = 1,491ms (within 500ms throttle window -> does not emit UI state yet)
        governor.recordFrameSample(1_491_000_000L)
        assertEquals(8.2f, governor.telemetry.value.lastFrameTimeMs, 0.01f)

        // Frame at t = 1,500.5ms (500ms elapsed since 1,000ms anchor; 9.5ms delta from 1,491ms frame)
        governor.recordFrameSample(1_500_500_000L)
        assertEquals(9.5f, governor.telemetry.value.lastFrameTimeMs, 0.1f)
        assertTrue(governor.telemetry.value.rollingAverageFrameTimeMs > 0f)
        assertTrue(governor.telemetry.value.p95FrameTimeMs >= 9.5f)
        assertTrue(governor.telemetry.value.withinFrameBudget)
    }

    @Test
    @Suppress("DEPRECATION")
    fun memoryTrimEngine_evictsDomainCachesWithoutBlockingOnLowMemoryAndManualPurge() {
        var cacheEvictionCount = 0
        val governor = HardwareGovernor(
            context = context,
            onClearDomainCaches = { cacheEvictionCount++ }
        )

        governor.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW)
        assertEquals(1, cacheEvictionCount)
        assertEquals(
            "SYSTEM_TRIM_${ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW}",
            governor.telemetry.value.lastMemoryTrimLevel
        )

        governor.performMemoryTrim()
        assertEquals(2, cacheEvictionCount)
        assertEquals("MANUAL_4GB_PURGE", governor.telemetry.value.lastMemoryTrimLevel)
    }

    @Test
    fun realme8HardwareHudDialog_andTopBarBadge_renderAndTriggerControlsInCompose() {
        var telemetryState by mutableStateOf(HardwareTelemetryState())
        var hudOpened by mutableStateOf(false)
        var memoryTrimTriggered = false

        composeTestRule.setContent {
            VibePitchTheme {
                HardwareTelemetryTopBarBadge(
                    telemetry = telemetryState,
                    onClick = { hudOpened = true }
                )
                if (hudOpened) {
                    Realme8HardwareHudDialog(
                        telemetry = telemetryState,
                        onSelectRefreshRateMode = { mode ->
                            telemetryState = telemetryState.copy(
                                refreshRateMode = mode,
                                targetFps = if (mode == RefreshRateMode.THERMAL_SAVER_60HZ) 60 else 90,
                                frameBudgetMs = if (mode == RefreshRateMode.THERMAL_SAVER_60HZ) 16.6f else 11.1f
                            )
                        },
                        onSelectGpuRenderScaleMode = { scaleMode ->
                            telemetryState = telemetryState.copy(
                                gpuRenderScaleMode = scaleMode,
                                astcAndMsaaEnabled = scaleMode == GpuRenderScaleMode.NATIVE_1080P
                            )
                        },
                        onPerformMemoryTrim = {
                            memoryTrimTriggered = true
                            telemetryState = telemetryState.copy(lastMemoryTrimLevel = "MANUAL_4GB_PURGE")
                        },
                        onDismiss = { hudOpened = false }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("hardware_telemetry_chip").assertIsDisplayed()
        composeTestRule.onNodeWithTag("hardware_telemetry_chip").performClick()
        composeTestRule.waitForIdle()
        assertTrue(hudOpened)

        composeTestRule.onNodeWithTag("realme_hardware_hud_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("refresh_mode_60hz").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(RefreshRateMode.THERMAL_SAVER_60HZ, telemetryState.refreshRateMode)
        assertEquals(60, telemetryState.targetFps)

        composeTestRule.onNodeWithTag("gpu_scale_saver_720p").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(GpuRenderScaleMode.SAVER_720P_SCALE, telemetryState.gpuRenderScaleMode)
        assertFalse(telemetryState.astcAndMsaaEnabled)

        composeTestRule.onNodeWithTag("trim_memory_action_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertTrue(memoryTrimTriggered)
        assertEquals("MANUAL_4GB_PURGE", telemetryState.lastMemoryTrimLevel)
    }
}
