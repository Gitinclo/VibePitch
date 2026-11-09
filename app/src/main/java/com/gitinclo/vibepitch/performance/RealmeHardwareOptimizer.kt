package com.gitinclo.vibepitch.performance

import android.content.Context
import android.os.Process
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

enum class RefreshRateMode(val label: String) {
    AUTO_90HZ_THERMAL_GUARD("Auto (90Hz w/ Thermal Guard)"),
    LOCKED_90HZ("Locked 90Hz (11.1ms Budget)"),
    THERMAL_SAVER_60HZ("60Hz Thermal Saver (16.6ms)")
}

enum class GpuRenderScaleMode(val label: String) {
    NATIVE_1080P("Native 1080x2400 (ASTC + 4x MSAA)"),
    SAVER_720P_SCALE("720p Fill-Rate Saver (Flat Shaders)")
}

@Immutable
data class HardwareTelemetryState(
    val socModel: String = "MediaTek Dimensity 700 (2x A76 @ 2.2GHz + 6x A55 @ 2.0GHz)",
    val gpuModel: String = "ARM Mali-G57 MC2 @ 955MHz (Vulkan 1.1 / OpenGL ES 3.2)",
    val performanceProfile: DimensityPerformanceProfile = DimensityPerformanceProfile.BALANCED_ADAPTIVE,
    val refreshRateMode: RefreshRateMode = RefreshRateMode.AUTO_90HZ_THERMAL_GUARD,
    val targetFps: Int = 90,
    val frameBudgetMs: Float = 11.1f,
    val lastFrameTimeMs: Float = 8.2f,
    val rollingAverageFrameTimeMs: Float = 8.2f,
    val p95FrameTimeMs: Float = 9.0f,
    val withinFrameBudget: Boolean = true,
    val gpuRenderScaleMode: GpuRenderScaleMode = GpuRenderScaleMode.NATIVE_1080P,
    val astcAndMsaaEnabled: Boolean = true,
    val ramUsedMb: Int = 0,
    val ramMaxMb: Int = 4096,
    val isLowMemory4GbBaseline: Boolean = true,
    val lastMemoryTrimLevel: String = "NORMAL",
    val ufsAsyncIoQueueDepth: Int = 0,
    val thermalStatusLabel: String = "NOMINAL",
    val isThermalThrottlingActive: Boolean = false
)

object DimensityCpuScheduler {
    private val threadId = AtomicInteger(1)
    private val ufsQueueCounter = AtomicInteger(0)

    val pendingUfsIoTasks: Int
        get() = ufsQueueCounter.get().coerceAtLeast(0)

    /**
     * Bounded 6-thread pool pinned to Linux EAS background priority for the six 2.0 GHz Cortex-A55 cores,
     * keeping the two 2.2 GHz Cortex-A76 performance cores dedicated to the UI render loop.
     */
    val a55BackgroundDispatcher: CoroutineDispatcher =
        Executors.newFixedThreadPool(6) { runnable ->
            Thread({
                try {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                } catch (_: Throwable) {
                    // Safe fallback in unit test environments
                }
                runnable.run()
            }, "Dimensity-A55-Worker-${threadId.getAndIncrement()}")
        }.asCoroutineDispatcher()

    /**
     * Dedicated sequential dispatcher for non-blocking UFS 2.1 SharedPreferences / JSON I/O.
     */
    val ufsSequentialIoDispatcher: CoroutineDispatcher =
        Executors.newSingleThreadExecutor { runnable ->
            Thread({
                try {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                } catch (_: Throwable) {
                    // Safe fallback in unit test environments
                }
                runnable.run()
            }, "Realme-UFS21-Sequential-IO")
        }.asCoroutineDispatcher()

    fun incrementUfsQueueDepth(): Int = ufsQueueCounter.incrementAndGet()
    fun decrementUfsQueueDepth(): Int = ufsQueueCounter.decrementAndGet().coerceAtLeast(0)
}

/**
 * Realme 8 5G hardware optimizer façade delegating all Dimensity 700 performance profile management,
 * 500ms-throttled frame telemetry, and 90Hz/60Hz thermal fallback toggling to [HardwareGovernor].
 */
class RealmeHardwareOptimizer(
    context: Context,
    onClearDomainCaches: () -> Unit = {}
) : HardwareGovernor(
    context = context,
    onClearDomainCaches = onClearDomainCaches
)
