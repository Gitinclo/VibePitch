# Realme 8 5G (Dimensity 700 / 90Hz / 4GB RAM) Hardware & Performance Optimization Plan

Deliver a dedicated hardware-level optimization layer and interactive **Realme 8 5G Performance & Thermal HUD** tailored to the **MediaTek Dimensity 700 (MT6833)** SoC, **90Hz 1080x2400 (20:9) IPS LCD**, **ARM Mali-G57 MC2 GPU**, **4GB LPDDR4X RAM baseline**, and **UFS 2.1 storage** so VibePitch Studio sustains sub-11.1ms frame times at 90 FPS without secondary recomposition jank, OEM window manager conflicts, memory pressure, or thermal throttling.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following hardware optimization and Realme UI 4.0 engineering refinements govern this execution plan:

- **Confirmed Decision 1 — Both Background Optimizations & Live HUD Panel**: Implement under-the-hood SoC, RAM, display, and UFS 2.1 I/O optimizations alongside an interactive **Realme 8 5G Hardware Telemetry & Optimizer HUD** accessible from the top app bar.
- **Confirmed Decision 2 — Auto 90Hz with 60Hz Thermal Fallback + Manual Toggle**: Automatically negotiate the 90Hz display mode (11.1ms frame budget) during normal operation, dynamically drop to a 60Hz (16.6ms) thermal-saver cap when Android `PowerManager` reports `THERMAL_STATUS_MODERATE` or higher, and allow manual user override (`Auto 90Hz/60Hz`, `Lock 90Hz`, `Lock 60Hz Thermal Saver`).
- **Confirmed Decision 3 — Four Realme UI 4.0 & Compose Engineering Refinements**:
  1. **Telemetry State Throttling (500ms Cadence)**: Sample `Choreographer` frame times without emitting UI state on every frame (which would force 90 recompositions/sec); throttle `HardwareTelemetryState` emissions to once every **500ms**.
  2. **Realme UI 4.0 Refresh Rate Locking**: Pair `preferredDisplayModeId` (matching `1080x2400`) with `preferredRefreshRate` (`90f` or `60f`) on `WindowManager.LayoutParams` so Realme UI 4.0 does not override the target refresh mode.
  3. **Non-Destructive 720p Scaling**: Never mutate global Compose density when `SAVER_720P_SCALE` is active (which would distort `1080x2400` layouts); instead, drop shader complexity, toggle `astcAndMsaaEnabled = (mode == GpuRenderScaleMode.NATIVE_1080P)`, and downsample only heavy background canvas layers.
  4. **Non-Blocking Heap Trim**: Never invoke explicit stop-the-world `System.gc()` inside `performMemoryTrim()`; instead, evict pitch script and guardrail caches asynchronously via `onClearDomainCaches()` and record updated heap metrics without blocking the UI thread.

---

## 1. Overview & Core Concept

- **What It Does**:
  - **Cortex-A76 / Cortex-A55 Heterogeneous Thread Pinning (`DimensityCpuScheduler`)**: Keeps UI/Compose frame traversal strictly on the main thread (scheduled on the two 2.2 GHz Cortex-A76 performance cores) while routing JSON serialization, pitch script generation, and 100-guardrail catalog filtering to a bounded 6-thread pool (`a55BackgroundDispatcher`, `THREAD_PRIORITY_BACKGROUND`) for the six 2.0 GHz Cortex-A55 cores, plus a single-thread sequential executor (`ufsSequentialIoDispatcher`, `THREAD_PRIORITY_BACKGROUND`) for non-blocking UFS 2.1 `SharedPreferences`/JSON I/O.
  - **90Hz Display & Thermal Governor (`RealmeHardwareOptimizer`)**: Queries `Display.supportedModes` on Android 11–13 (Realme UI 4.0) to match `1080x2400` and sets both `preferredDisplayModeId` and `preferredRefreshRate`, binds `PowerManager.OnThermalStatusChangedListener` (`THERMAL_STATUS_MODERATE` triggers automatic 60Hz cap and `SAVER_720P_SCALE`), and samples `Choreographer` frame durations with 500ms UI state throttling.
  - **4GB LPDDR4X RAM & Compose Stability**: Marks UI state models `@Immutable`, adds explicit `key` and `contentType` parameters to all `LazyColumn` items, caches generated pitch scripts and filtered guardrails with instant eviction on `ComponentCallbacks2.onTrimMemory` (`TRIM_MEMORY_RUNNING_LOW`, `TRIM_MEMORY_UI_HIDDEN`), and provides a non-blocking 1-tap **Purge Cache & Trim 4GB Heap** action.
  - **Mali-G57 MC2 GPU Fill-Rate Protection**: Uses single-pass flat vector canvas gauges in `Realme8HardwareHudDialog` and the top-bar badge, and dynamically drops background shader complexity and MSAA when `SAVER_720P_SCALE` is active.
- **Target Audience / Persona**: Agency founders and field sales reps running live client demos on the Realme 8 5G (4GB/64GB and 4GB/128GB models) who require locked 90 FPS responsiveness and zero low-memory crashes.
- **Key Value**: Sustains sub-11.1ms frame times at 90 FPS while preventing secondary recomposition overhead, Realme UI 4.0 refresh-rate resets, and thermal throttling.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Automatic Boot & Realme UI 4.0 Refresh Negotiation**: On launch, `MainActivity` binds `RealmeHardwareOptimizer`, applies both `preferredDisplayModeId` and `preferredRefreshRate` for `1080x2400 @ 90Hz`, starts the 500ms-throttled `Choreographer` frame monitor, and routes `LocalGuestSessionManager` JSON writes through `DimensityCpuScheduler.ufsSequentialIoDispatcher`.
  2. **Top-Bar Hardware Telemetry Pill (`90Hz • 8.2ms`)**: A compact, single-pass status badge in the top app bar displays the active refresh target (`90Hz` or `60Hz Cap`), throttled frame duration, and thermal status. Tapping it opens `Realme8HardwareHudDialog`.
  3. **Interactive Realme 8 5G Optimizer HUD (`Realme8HardwareHudDialog`)**:
     - **SoC & Thread Affinity Card**: Visualizes **MediaTek Dimensity 700 (2x A76 @ 2.2GHz + 6x A55 @ 2.0GHz)**, the 6-worker Cortex-A55 background pool, and the sequential UFS 2.1 I/O queue depth.
     - **Display & Frame-Time Budget Card**: Displays live 500ms-sampled frame duration against the **11.1ms (90 FPS)** and **16.6ms (60 FPS)** budgets using a single-pass flat vector gauge, plus interactive chips for **Auto (90Hz w/ Thermal Guard)**, **Locked 90Hz**, and **60Hz Thermal Saver**.
     - **Mali-G57 MC2 GPU & Non-Destructive Render Scale Card**: Shows **ARM Mali-G57 MC2 @ 955MHz (Vulkan 1.1 / OpenGL ES 3.2)** status, `ASTC / 4x MSAA` state, and a toggle between **Native 1080p (`NATIVE_1080P`)** and **720p Fill-Rate Saver (`SAVER_720P_SCALE`)** without distorting Compose layout density.
     - **4GB LPDDR4X RAM & Non-Blocking Cache Trim Card**: Shows live heap usage (`ramUsedMb / ramMaxMb`), last trim level (`NORMAL`, `SYSTEM_TRIM_*`, `MANUAL_4GB_PURGE`), and a **Purge Domain Caches (4GB Trim)** button.
- **Visual Identity & Theme**:
  - *Aesthetic Direction*: High-contrast hardware telemetry integrated with VibePitch Studio's Obsidian Slate (`#0B0F19`), Electric Indigo (`#6366F1`), and Emerald Conversion (`#10B981`) theme.
  - *GPU-Friendly Styling*: Single-pass flat vector progress bars and crisp 1.dp borders that preserve Mali-G57 MC2 fill rate.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: 500ms Throttled Choreographer Telemetry**
  - *Chosen Approach*: Track every frame's `frameTimeNanos` delta in `Choreographer.FrameCallback`, but only update `_telemetry` when `nowMs - lastUiEmitTimestampMs >= 500L`.
  - *Why*: Emitting new `StateFlow` values at 90Hz would trigger 90 Compose recompositions per second, defeating the 11.1ms frame budget.
- **Decision 2: Dual `preferredDisplayModeId` + `preferredRefreshRate` Window Locking**
  - *Chosen Approach*: Filter `display.supportedModes` for `physicalWidth == 1080 && physicalHeight == 2400`, pick the mode closest to `targetHz`, set `params.preferredDisplayModeId = matchedMode.modeId`, and also set `params.preferredRefreshRate = targetHz`.
  - *Why*: Realme UI 4.0 frequently ignores `preferredDisplayModeId` unless `preferredRefreshRate` is explicitly set on the window attributes.
- **Decision 3: Non-Destructive `SAVER_720P_SCALE` & Non-Blocking Cache Trim**
  - *Chosen Approach*: Toggle `astcAndMsaaEnabled = (mode == GpuRenderScaleMode.NATIVE_1080P)` and simplify background canvas/gradient shaders without altering `LocalDensity`, and evict cached pitch scripts/guardrails via `onClearDomainCaches()` without calling `System.gc()`.
  - *Why*: Preserves crisp `1080x2400` UI geometry and avoids stop-the-world GC pauses on the 4GB RAM model.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```
┌────────────────────────────────────────────────────────────────────────────┐
│            MainActivity (Realme UI 4.0 Window & Trim Callbacks)            │
│  • preferredDisplayModeId + preferredRefreshRate (90f <-> 60f)             │
│  • ComponentCallbacks2.onTrimMemory -> optimizer.onTrimMemory(level)       │
└─────────────────────────────────────┬──────────────────────────────────────┘
                                      │
                                      ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                 RealmeHardwareOptimizer & DimensityCpuScheduler            │
├────────────────────────┬─────────────────────────┬─────────────────────────┤
│ DimensityCpuScheduler  │ 90Hz & Thermal Governor │ 4GB RAM & GPU Guardrail │
│ • 6x A55 Worker Pool   │ • 500ms Frame Sampling  │ • Non-Blocking Trim     │
│   (PRIORITY_BACKGROUND)│ • 11.1ms / 16.6ms Budget│ • Non-Destructive 720p  │
│ • 1x UFS 2.1 Seq I/O   │ • Thermal >= MODERATE   │ • ASTC & 4x MSAA Toggle │
└──────────┬─────────────┴────────────┬────────────┴────────────┬────────────┘
           │                          │                         │
           ▼                          ▼                         ▼
┌────────────────────────────────────────────────────────────────────────────┐
│             VibePitchViewModel & LocalGuestSessionManager                  │
│  • Routes UFS 2.1 JSON writes via ufsSequentialIoDispatcher                │
│  • Caches generated PitchScripts; flushes on onClearDomainCaches()         │
│  • Exposes HardwareTelemetryState to TopAppBar & Realme8HardwareHudDialog  │
└─────────────────────────────────────┬──────────────────────────────────────┘
                                      │
                                      ▼
┌────────────────────────────────────────────────────────────────────────────┐
│             Jetpack Compose 90Hz UI & Realme8HardwareHudDialog             │
│  • @Immutable UI models + LazyColumn keys/contentTypes                     │
│  • Single-pass flat vector telemetry gauges (Mali-G57 MC2 friendly)        │
└────────────────────────────────────────────────────────────────────────────┘
```

### Phase-by-Phase Execution Roadmap
1. **Phase 1 — SoC & UFS 2.1 I/O Setup**: Implement `DimensityCpuScheduler` (6-thread `a55BackgroundDispatcher` + single-thread `ufsSequentialIoDispatcher` at `Process.THREAD_PRIORITY_BACKGROUND`) and route `LocalGuestSessionManager` JSON persistence through the sequential UFS 2.1 dispatcher with queue depth tracking.
2. **Phase 2 — 90Hz Display & Thermal Governor**: Implement `RealmeHardwareOptimizer` with 500ms-throttled `Choreographer` frame telemetry, dual `preferredDisplayModeId` + `preferredRefreshRate` window negotiation, and `PowerManager.OnThermalStatusChangedListener` triggering 60Hz + `SAVER_720P_SCALE` fallback at `THERMAL_STATUS_MODERATE`.
3. **Phase 3 — 4GB RAM & Compose Stability**: Annotate UI state classes with `@Immutable`, add `key` and `contentType` to all `LazyColumn` items across CRM and Playbook screens, memoize pitch script generation in `VibePitchViewModel`, and wire `ComponentCallbacks2.onTrimMemory` in `MainActivity` to flush domain caches without blocking UI.
4. **Phase 4 — HUD & Top-Bar Telemetry UI**: Build the top-bar `HardwareTelemetryBadge` (`hardware_telemetry_chip`) and `Realme8HardwareHudDialog` using single-pass flat vector gauges and interactive controls for `RefreshRateMode`, `GpuRenderScaleMode`, and `performMemoryTrim()`.
5. **Phase 5 — Automated Verification**: Implement `RealmeHardwareOptimizationTest` to verify A55 background thread priority (`Process.THREAD_PRIORITY_BACKGROUND`), thermal status transitions (`THERMAL_STATUS_MODERATE` -> 60 FPS / 16.6ms budget / `SAVER_720P_SCALE`), non-blocking memory trim handling (`onTrimMemory` & `performMemoryTrim`), and HUD Compose controls.
