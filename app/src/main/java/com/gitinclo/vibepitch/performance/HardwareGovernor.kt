package com.gitinclo.vibepitch.performance

import android.app.Activity
import android.content.ComponentCallbacks2
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.view.Choreographer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs

/**
 * Hardware performance profiles tailored for the Realme 8 5G (MediaTek Dimensity 700:
 * 2x 2.2 GHz Cortex-A76 + 6x 2.0 GHz Cortex-A55, ARM Mali-G57 MC2 @ 955 MHz, 90Hz 1080x2400 IPS LCD).
 */
enum class DimensityPerformanceProfile(
    val label: String,
    val defaultRefreshMode: RefreshRateMode,
    val defaultScaleMode: GpuRenderScaleMode
) {
    HIGH_THROUGHPUT_90HZ(
        label = "High Throughput 90Hz (A76 Boost)",
        defaultRefreshMode = RefreshRateMode.LOCKED_90HZ,
        defaultScaleMode = GpuRenderScaleMode.NATIVE_1080P
    ),
    BALANCED_ADAPTIVE(
        label = "Balanced Adaptive (Auto 90Hz/60Hz)",
        defaultRefreshMode = RefreshRateMode.AUTO_90HZ_THERMAL_GUARD,
        defaultScaleMode = GpuRenderScaleMode.NATIVE_1080P
    ),
    THERMAL_SAVER_60HZ(
        label = "60Hz Thermal & Battery Saver",
        defaultRefreshMode = RefreshRateMode.THERMAL_SAVER_60HZ,
        defaultScaleMode = GpuRenderScaleMode.SAVER_720P_SCALE
    )
}

/**
 * Central hardware governor managing MediaTek Dimensity 700 performance profiles,
 * 500ms-throttled Choreographer frame telemetry with a 64-sample ring buffer,
 * and automatic/manual toggling between 90Hz (11.1ms budget) and 60Hz (16.6ms budget) thermal fallback.
 */
open class HardwareGovernor(
    private val context: Context,
    private val onClearDomainCaches: () -> Unit = {}
) {
    companion object {
        const val TELEMETRY_THROTTLE_INTERVAL_MS: Long = 500L
        const val FRAME_BUDGET_90HZ_MS: Float = 11.1f
        const val FRAME_BUDGET_60HZ_MS: Float = 16.6f
        private const val RING_BUFFER_CAPACITY: Int = 64
    }

    private val _telemetry = MutableStateFlow(
        HardwareTelemetryState(
            ramUsedMb = currentHeapUsedMb(),
            ramMaxMb = currentHeapMaxMb()
        )
    )
    val telemetry: StateFlow<HardwareTelemetryState> = _telemetry.asStateFlow()

    private val frameDurationRingBuffer = FloatArray(RING_BUFFER_CAPACITY)
    private var ringBufferWriteIndex: Int = 0
    private var ringBufferSampleCount: Int = 0

    private var lastFrameTimestampNs: Long = 0L
    private var lastUiEmitTimestampMs: Long = 0L
    private var frameTelemetryStarted: Boolean = false

    /**
     * Registers a system thermal listener on Android 10+ (Realme UI 2.0–4.0) to automatically
     * engage 60Hz thermal fallback and 720p fill-rate protection when chassis temperature rises.
     */
    fun bindThermalObserver() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
            try {
                powerManager.addThermalStatusListener(context.mainExecutor) { status ->
                    handleThermalStatusChanged(status)
                }
            } catch (_: Throwable) {
                // Safe no-op if thermal service is unavailable in unit test / headless environment
            }
        }
    }

    /**
     * Applies one of the Dimensity 700 performance profiles, coordinating refresh rate mode,
     * frame-time budget (11.1ms vs 16.6ms), and Mali-G57 MC2 shader scaling.
     */
    fun setPerformanceProfile(profile: DimensityPerformanceProfile, activity: Activity? = null) {
        _telemetry.update { current ->
            current.copy(
                performanceProfile = profile,
                gpuRenderScaleMode = profile.defaultScaleMode,
                astcAndMsaaEnabled = profile.defaultScaleMode == GpuRenderScaleMode.NATIVE_1080P
            )
        }
        setRefreshRateMode(profile.defaultRefreshMode, activity)
    }

    /**
     * Toggles between 90Hz high-refresh operation (11.1ms budget) and 60Hz thermal fallback
     * (16.6ms budget + 720p Mali-G57 fill-rate saver).
     *
     * @return true if 60Hz thermal fallback is now active, false if 90Hz operation is active.
     */
    fun toggleThermalFallback(activity: Activity? = null): Boolean {
        val enableFallback = _telemetry.value.targetFps > 60
        setThermalFallbackActive(enabled = enableFallback, activity = activity)
        return enableFallback
    }

    /**
     * Explicitly enables or disables 60Hz thermal fallback mode.
     */
    fun setThermalFallbackActive(enabled: Boolean, activity: Activity? = null) {
        if (enabled) {
            setPerformanceProfile(DimensityPerformanceProfile.THERMAL_SAVER_60HZ, activity)
        } else {
            setPerformanceProfile(DimensityPerformanceProfile.BALANCED_ADAPTIVE, activity)
        }
    }

    /**
     * Evaluates system thermal status and automatically shifts between 90Hz (11.1ms) and
     * 60Hz thermal fallback (16.6ms) when in [RefreshRateMode.AUTO_90HZ_THERMAL_GUARD].
     */
    fun handleThermalStatusChanged(status: Int) {
        val throttling = status >= PowerManager.THERMAL_STATUS_MODERATE
        val statusLabel = when (status) {
            PowerManager.THERMAL_STATUS_NONE -> "NOMINAL"
            PowerManager.THERMAL_STATUS_LIGHT -> "LIGHT"
            PowerManager.THERMAL_STATUS_MODERATE -> "MODERATE (60Hz Cap)"
            else -> "CRITICAL THROTTLE"
        }
        _telemetry.update { current ->
            val active60Hz = current.refreshRateMode == RefreshRateMode.THERMAL_SAVER_60HZ ||
                (current.refreshRateMode == RefreshRateMode.AUTO_90HZ_THERMAL_GUARD && throttling)
            val newScaleMode = when {
                throttling -> GpuRenderScaleMode.SAVER_720P_SCALE
                current.performanceProfile == DimensityPerformanceProfile.THERMAL_SAVER_60HZ ->
                    GpuRenderScaleMode.SAVER_720P_SCALE
                else -> GpuRenderScaleMode.NATIVE_1080P
            }
            val newBudget = if (active60Hz) FRAME_BUDGET_60HZ_MS else FRAME_BUDGET_90HZ_MS
            current.copy(
                thermalStatusLabel = statusLabel,
                isThermalThrottlingActive = throttling,
                targetFps = if (active60Hz) 60 else 90,
                frameBudgetMs = newBudget,
                withinFrameBudget = current.lastFrameTimeMs <= newBudget,
                gpuRenderScaleMode = newScaleMode,
                astcAndMsaaEnabled = newScaleMode == GpuRenderScaleMode.NATIVE_1080P
            )
        }
    }

    fun setRefreshRateMode(mode: RefreshRateMode, activity: Activity? = null) {
        if (activity != null) {
            applyWindowRefreshRate(activity, mode)
        } else {
            val throttling = _telemetry.value.isThermalThrottlingActive
            val targetHz = when (mode) {
                RefreshRateMode.LOCKED_90HZ -> 90f
                RefreshRateMode.THERMAL_SAVER_60HZ -> 60f
                RefreshRateMode.AUTO_90HZ_THERMAL_GUARD -> if (throttling) 60f else 90f
            }
            val is60Hz = targetHz <= 60f
            val newBudget = if (is60Hz) FRAME_BUDGET_60HZ_MS else FRAME_BUDGET_90HZ_MS
            val syncedProfile = when (mode) {
                RefreshRateMode.LOCKED_90HZ -> DimensityPerformanceProfile.HIGH_THROUGHPUT_90HZ
                RefreshRateMode.AUTO_90HZ_THERMAL_GUARD -> DimensityPerformanceProfile.BALANCED_ADAPTIVE
                RefreshRateMode.THERMAL_SAVER_60HZ -> DimensityPerformanceProfile.THERMAL_SAVER_60HZ
            }
            _telemetry.update { current ->
                current.copy(
                    performanceProfile = syncedProfile,
                    refreshRateMode = mode,
                    targetFps = if (is60Hz) 60 else 90,
                    frameBudgetMs = newBudget,
                    withinFrameBudget = current.lastFrameTimeMs <= newBudget
                )
            }
        }
    }

    /**
     * Negotiates the 90Hz or 60Hz display mode on Realme UI 4.0 by pairing
     * `preferredDisplayModeId` (filtered to 1080x2400 native resolution) with `preferredRefreshRate`.
     */
    @Suppress("DEPRECATION")
    fun applyWindowRefreshRate(activity: Activity, mode: RefreshRateMode) {
        val throttling = _telemetry.value.isThermalThrottlingActive
        val targetHz = when (mode) {
            RefreshRateMode.LOCKED_90HZ -> 90f
            RefreshRateMode.THERMAL_SAVER_60HZ -> 60f
            RefreshRateMode.AUTO_90HZ_THERMAL_GUARD -> if (throttling) 60f else 90f
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val display = activity.windowManager.defaultDisplay
                val matchedMode = display?.supportedModes
                    ?.filter { it.physicalWidth == 1080 && it.physicalHeight == 2400 }
                    ?.minByOrNull { abs(it.refreshRate - targetHz) }
                    ?: display?.supportedModes?.minByOrNull { abs(it.refreshRate - targetHz) }

                val params = activity.window.attributes
                if (matchedMode != null) {
                    params.preferredDisplayModeId = matchedMode.modeId
                }
                params.preferredRefreshRate = targetHz
                activity.window.attributes = params
            } catch (_: Throwable) {
                // Safe fallback if window display modes are unavailable in headless tests
            }
        }

        val is60Hz = targetHz <= 60f
        val newBudget = if (is60Hz) FRAME_BUDGET_60HZ_MS else FRAME_BUDGET_90HZ_MS
        val syncedProfile = when (mode) {
            RefreshRateMode.LOCKED_90HZ -> DimensityPerformanceProfile.HIGH_THROUGHPUT_90HZ
            RefreshRateMode.AUTO_90HZ_THERMAL_GUARD -> DimensityPerformanceProfile.BALANCED_ADAPTIVE
            RefreshRateMode.THERMAL_SAVER_60HZ -> DimensityPerformanceProfile.THERMAL_SAVER_60HZ
        }
        _telemetry.update { current ->
            current.copy(
                performanceProfile = syncedProfile,
                refreshRateMode = mode,
                targetFps = if (is60Hz) 60 else 90,
                frameBudgetMs = newBudget,
                withinFrameBudget = current.lastFrameTimeMs <= newBudget
            )
        }
    }

    fun startFrameTelemetry() {
        if (frameTelemetryStarted) return
        frameTelemetryStarted = true
        try {
            Choreographer.getInstance().postFrameCallback(object : Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    recordFrameSample(frameTimeNanos)
                    Choreographer.getInstance().postFrameCallback(this)
                }
            })
        } catch (_: Throwable) {
            // Safe no-op if Choreographer looper is not prepared
        }
    }

    /**
     * Records each frame duration into a zero-allocation 64-sample ring buffer and throttles
     * Compose [StateFlow] emissions to once every [TELEMETRY_THROTTLE_INTERVAL_MS] (500ms)
     * so telemetry collection never causes 90Hz recomposition jitter on the Cortex-A76 UI thread.
     */
    fun recordFrameSample(frameTimeNanos: Long) {
        val nowMs = frameTimeNanos / 1_000_000L
        if (lastFrameTimestampNs == 0L) {
            lastFrameTimestampNs = frameTimeNanos
            lastUiEmitTimestampMs = nowMs
            return
        }
        val durationMs = ((frameTimeNanos - lastFrameTimestampNs) / 1_000_000f).coerceAtLeast(0.1f)
        lastFrameTimestampNs = frameTimeNanos

        frameDurationRingBuffer[ringBufferWriteIndex] = durationMs
        ringBufferWriteIndex = (ringBufferWriteIndex + 1) % RING_BUFFER_CAPACITY
        if (ringBufferSampleCount < RING_BUFFER_CAPACITY) {
            ringBufferSampleCount++
        }

        if (nowMs - lastUiEmitTimestampMs >= TELEMETRY_THROTTLE_INTERVAL_MS) {
            lastUiEmitTimestampMs = nowMs
            val (avgMs, p95Ms) = computeRingBufferStats(fallbackMs = durationMs)
            val usedMb = currentHeapUsedMb()
            val maxMb = currentHeapMaxMb()
            _telemetry.update { state ->
                state.copy(
                    lastFrameTimeMs = durationMs,
                    rollingAverageFrameTimeMs = avgMs,
                    p95FrameTimeMs = p95Ms,
                    withinFrameBudget = durationMs <= state.frameBudgetMs,
                    ramUsedMb = usedMb,
                    ramMaxMb = maxMb,
                    ufsAsyncIoQueueDepth = DimensityCpuScheduler.pendingUfsIoTasks
                )
            }
        }
    }

    private fun computeRingBufferStats(fallbackMs: Float): Pair<Float, Float> {
        val count = ringBufferSampleCount
        if (count <= 0) return fallbackMs to fallbackMs
        var sum = 0f
        val snapshot = FloatArray(count)
        for (i in 0 until count) {
            val sample = frameDurationRingBuffer[i]
            sum += sample
            snapshot[i] = sample
        }
        snapshot.sort()
        val p95Index = ((count - 1) * 0.95f).toInt().coerceIn(0, count - 1)
        return (sum / count) to snapshot[p95Index]
    }

    fun setGpuRenderScaleMode(mode: GpuRenderScaleMode) {
        _telemetry.update {
            it.copy(
                gpuRenderScaleMode = mode,
                astcAndMsaaEnabled = mode == GpuRenderScaleMode.NATIVE_1080P
            )
        }
    }

    fun updateUfsQueueDepth(depth: Int) {
        _telemetry.update { it.copy(ufsAsyncIoQueueDepth = depth.coerceAtLeast(0)) }
    }

    fun onTrimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ||
            level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
        ) {
            performMemoryTrim(trimLabel = "SYSTEM_TRIM_$level")
        }
    }

    /**
     * Evicts domain caches without invoking blocking System.gc() pauses that would exceed the 11.1ms frame budget.
     */
    fun performMemoryTrim(trimLabel: String = "MANUAL_4GB_PURGE") {
        onClearDomainCaches()
        val usedMb = currentHeapUsedMb()
        _telemetry.update {
            it.copy(
                ramUsedMb = usedMb,
                lastMemoryTrimLevel = trimLabel,
                ufsAsyncIoQueueDepth = DimensityCpuScheduler.pendingUfsIoTasks
            )
        }
    }

    private fun currentHeapUsedMb(): Int {
        val runtime = Runtime.getRuntime()
        return ((runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)).toInt().coerceAtLeast(1)
    }

    private fun currentHeapMaxMb(): Int {
        val runtime = Runtime.getRuntime()
        return (runtime.maxMemory() / (1024 * 1024)).toInt().coerceAtLeast(256)
    }
}
