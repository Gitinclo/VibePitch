package com.gitinclo.vibepitch.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gitinclo.vibepitch.R
import com.gitinclo.vibepitch.auth.signOutUser
import com.gitinclo.vibepitch.data.local.LocalGuestSessionManager
import com.gitinclo.vibepitch.data.remote.FirebaseEnvValidator
import com.gitinclo.vibepitch.data.remote.NetworkConnectivityObserver
import android.app.Activity
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import androidx.compose.runtime.DisposableEffect
import com.gitinclo.vibepitch.data.repository.VibePitchRepository
import com.gitinclo.vibepitch.performance.HardwareGovernor
import com.gitinclo.vibepitch.ui.features.auth.Day20MilestoneDialog
import com.gitinclo.vibepitch.ui.features.auth.Day30ExpiredLockScreen
import com.gitinclo.vibepitch.ui.features.auth.GuestLifecycleBanner
import com.gitinclo.vibepitch.ui.features.auth.PermanentBusinessAccountDialog
import com.gitinclo.vibepitch.ui.features.compliance.ComplianceDocsDialog
import com.gitinclo.vibepitch.ui.features.crm.OfflineStorageNoticeBanner
import com.gitinclo.vibepitch.ui.features.crm.ProspectCrmScreen
import com.gitinclo.vibepitch.ui.features.crm.StorageSyncStatusDotChip
import com.gitinclo.vibepitch.ui.features.livedemo.LiveDemoScreen
import com.gitinclo.vibepitch.ui.features.performance.HardwareTelemetryTopBarBadge
import com.gitinclo.vibepitch.ui.features.performance.Realme8HardwareHudDialog
import com.gitinclo.vibepitch.ui.features.playbook.PlaybookAndPitchScreen
import com.gitinclo.vibepitch.ui.features.studio.BrandStudioScreen
import com.gitinclo.vibepitch.ui.theme.EmeraldConversion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VibePitchApp(
    currentUserId: String,
    isGuestMode: Boolean,
    guestSessionManager: LocalGuestSessionManager
) {
    val viewModel: VibePitchViewModel = viewModel(
        key = "${currentUserId}_$isGuestMode",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val repository = if (!isGuestMode) {
                    FirebaseEnvValidator.getFirestoreOrNull(app)?.let { db ->
                        VibePitchRepository(db)
                    }
                } else {
                    null
                }
                VibePitchViewModel(
                    repository = repository,
                    guestSessionManager = guestSessionManager,
                    currentUserId = currentUserId,
                    isGuestMode = isGuestMode
                )
            }
        }
    )

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val isPresentationMode by viewModel.isPresentationMode.collectAsStateWithLifecycle()
    val activeBrand by viewModel.activeBrand.collectAsStateWithLifecycle()
    val selectedOfferArchetype by viewModel.selectedOfferArchetype.collectAsStateWithLifecycle()
    val scopeGuardrails by viewModel.scopeGuardrailConfig.collectAsStateWithLifecycle()
    val quoteState by viewModel.quoteState.collectAsStateWithLifecycle()
    val prospectsState by viewModel.prospectsState.collectAsStateWithLifecycle()
    val leadsState by viewModel.leadsState.collectAsStateWithLifecycle()
    val playbookState by viewModel.playbookState.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val guestStatus by viewModel.guestStatus.collectAsStateWithLifecycle()
    val businessProfile by viewModel.businessProfileState.collectAsStateWithLifecycle()
    val showBusinessUpgradeModal by viewModel.showBusinessUpgradeModal.collectAsStateWithLifecycle()
    val showComplianceDocsModal by viewModel.showComplianceDocsModal.collectAsStateWithLifecycle()
    val showHardwareHudModal by viewModel.showHardwareHudModal.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val hardwareGovernor = remember(context, viewModel) {
        HardwareGovernor(
            context = context.applicationContext,
            onClearDomainCaches = viewModel::clearDomainCaches
        )
    }
    val hardwareTelemetry by hardwareGovernor.telemetry.collectAsStateWithLifecycle()

    LaunchedEffect(hardwareGovernor) {
        hardwareGovernor.bindThermalObserver()
        hardwareGovernor.startFrameTelemetry()
    }

    LaunchedEffect(hardwareTelemetry.refreshRateMode, hardwareTelemetry.isThermalThrottlingActive, activity) {
        if (activity != null) {
            hardwareGovernor.applyWindowRefreshRate(activity, hardwareTelemetry.refreshRateMode)
        }
    }

    DisposableEffect(context, hardwareGovernor) {
        val appContext = context.applicationContext
        val callback = object : ComponentCallbacks2 {
            override fun onTrimMemory(level: Int) {
                hardwareGovernor.onTrimMemory(level)
            }
            override fun onConfigurationChanged(newConfig: Configuration) = Unit
            @Deprecated("Deprecated in Java")
            override fun onLowMemory() {
                hardwareGovernor.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL)
            }
        }
        appContext.registerComponentCallbacks(callback)
        onDispose {
            appContext.unregisterComponentCallbacks(callback)
        }
    }

    val connectivityFlow = remember(context) {
        NetworkConnectivityObserver.observeConnectivity(context)
    }
    val isDeviceOnline by connectivityFlow.collectAsStateWithLifecycle(
        initialValue = remember(context) { NetworkConnectivityObserver.isCurrentlyConnected(context) }
    )

    LaunchedEffect(isDeviceOnline) {
        viewModel.setDeviceOnlineStatus(isDeviceOnline)
    }

    val prospectsCount = (prospectsState as? UiState.Success)?.data?.size ?: 0
    val leadsCount = (leadsState as? UiState.Success)?.data?.size ?: 0

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    BackHandler(enabled = isPresentationMode || selectedTab != AppTab.LIVE_DEMO) {
        if (isPresentationMode) {
            viewModel.setPresentationMode(false)
        } else {
            viewModel.selectTab(AppTab.LIVE_DEMO)
        }
    }

    if (isGuestMode && guestStatus.isGuestExpired) {
        Day30ExpiredLockScreen(
            guestStatus = guestStatus,
            prospectCount = prospectsCount,
            leadCount = leadsCount,
            onUpgradeClicked = { viewModel.openBusinessUpgradeModal() },
            onExitGuestClicked = { guestSessionManager.exitGuestSession() }
        )

        if (showBusinessUpgradeModal) {
            PermanentBusinessAccountDialog(
                initialProfile = businessProfile,
                isGuestMode = true,
                onDismiss = { viewModel.closeBusinessUpgradeModal() },
                onStageProfileForGoogleUpgrade = viewModel::stageBusinessProfileForGuestUpgrade,
                onSaveProfileDirectly = viewModel::savePermanentBusinessProfileDirectly,
                onGoogleAuthSuccess = {}
            )
        }
        return
    }

    val (estimatedTotal, depositAmount) = viewModel.calculateCurrentQuote()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                if (!isPresentationMode) {
                    TopAppBar(
                        title = {
                            val titlePrefix = if (!isGuestMode && !businessProfile?.agencyName.isNullOrBlank()) {
                                businessProfile?.agencyName!!
                            } else {
                                "VibePitch Studio"
                            }
                            Text(
                                text = "$titlePrefix • ${activeBrand.businessName}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        },
                        actions = {
                            HardwareTelemetryTopBarBadge(
                                telemetry = hardwareTelemetry,
                                onClick = { viewModel.openHardwareHudModal() },
                                modifier = Modifier.padding(end = 4.dp)
                            )

                            StorageSyncStatusDotChip(
                                isDeviceOnline = isDeviceOnline,
                                isGuestMode = isGuestMode,
                                onClick = {
                                    val explanation = when {
                                        !isDeviceOnline ->
                                            "Offline Mode: Data is currently being saved to local device storage instead of Firestore."
                                        isGuestMode ->
                                            "Guest Mode: Data is saved to local device storage (upgrade anytime to sync to Firestore)."
                                        else ->
                                            "Online: Connected and syncing live with Cloud Firestore."
                                    }
                                    viewModel.showMessage(explanation)
                                },
                                modifier = Modifier.padding(end = 4.dp)
                            )

                            IconButton(
                                onClick = { viewModel.openComplianceDocsModal() },
                                modifier = Modifier.testTag("top_bar_compliance_docs_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Docs & Compliance Hub",
                                    tint = EmeraldConversion
                                )
                            }

                            FilledTonalButton(
                                onClick = { viewModel.openBusinessUpgradeModal() },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .testTag("top_bar_business_account_button")
                            ) {
                                Icon(
                                    imageVector = if (isGuestMode) {
                                        Icons.Default.BusinessCenter
                                    } else {
                                        Icons.Default.Verified
                                    },
                                    contentDescription = null,
                                    tint = if (isGuestMode) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        EmeraldConversion
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isGuestMode) {
                                        "Guest Day ${guestStatus.currentDayNumber}/30"
                                    } else {
                                        businessProfile?.agencyName?.take(14) ?: "Business Account"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (isGuestMode) {
                                        guestSessionManager.exitGuestSession()
                                    } else {
                                        signOutUser(
                                            credentialManager = credentialManager,
                                            scope = coroutineScope
                                        )
                                    }
                                },
                                modifier = Modifier.testTag("sign_out_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = if (isGuestMode) {
                                        stringResource(R.string.exit_guest_mode)
                                    } else {
                                        stringResource(R.string.sign_out)
                                    }
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            },
            bottomBar = {
                if (!isPresentationMode && !isWideScreen) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = selectedTab == AppTab.LIVE_DEMO,
                            onClick = { viewModel.selectTab(AppTab.LIVE_DEMO) },
                            icon = { Icon(Icons.Default.Calculate, contentDescription = null) },
                            label = { Text(stringResource(R.string.nav_live_demo)) },
                            modifier = Modifier.testTag("nav_tab_live_demo")
                        )
                        NavigationBarItem(
                            selected = selectedTab == AppTab.BRAND_STUDIO,
                            onClick = { viewModel.selectTab(AppTab.BRAND_STUDIO) },
                            icon = { Icon(Icons.Default.Palette, contentDescription = null) },
                            label = { Text(stringResource(R.string.nav_brand_studio)) },
                            modifier = Modifier.testTag("nav_tab_brand_studio")
                        )
                        NavigationBarItem(
                            selected = selectedTab == AppTab.CRM_LEADS,
                            onClick = { viewModel.selectTab(AppTab.CRM_LEADS) },
                            icon = { Icon(Icons.Default.PeopleAlt, contentDescription = null) },
                            label = { Text(stringResource(R.string.nav_crm)) },
                            modifier = Modifier.testTag("nav_tab_crm")
                        )
                        NavigationBarItem(
                            selected = selectedTab == AppTab.PLAYBOOK,
                            onClick = { viewModel.selectTab(AppTab.PLAYBOOK) },
                            icon = { Icon(Icons.Default.Checklist, contentDescription = null) },
                            label = { Text(stringResource(R.string.nav_playbook)) },
                            modifier = Modifier.testTag("nav_tab_playbook")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (!isPresentationMode && isWideScreen) {
                    NavigationRail {
                        NavigationRailItem(
                            selected = selectedTab == AppTab.LIVE_DEMO,
                            onClick = { viewModel.selectTab(AppTab.LIVE_DEMO) },
                            icon = { Icon(Icons.Default.Calculate, contentDescription = null) },
                            label = { Text(stringResource(R.string.nav_live_demo)) },
                            modifier = Modifier.testTag("rail_tab_live_demo")
                        )
                        NavigationRailItem(
                            selected = selectedTab == AppTab.BRAND_STUDIO,
                            onClick = { viewModel.selectTab(AppTab.BRAND_STUDIO) },
                            icon = { Icon(Icons.Default.Palette, contentDescription = null) },
                            label = { Text(stringResource(R.string.nav_brand_studio)) },
                            modifier = Modifier.testTag("rail_tab_brand_studio")
                        )
                        NavigationRailItem(
                            selected = selectedTab == AppTab.CRM_LEADS,
                            onClick = { viewModel.selectTab(AppTab.CRM_LEADS) },
                            icon = { Icon(Icons.Default.PeopleAlt, contentDescription = null) },
                            label = { Text(stringResource(R.string.nav_crm)) },
                            modifier = Modifier.testTag("rail_tab_crm")
                        )
                        NavigationRailItem(
                            selected = selectedTab == AppTab.PLAYBOOK,
                            onClick = { viewModel.selectTab(AppTab.PLAYBOOK) },
                            icon = { Icon(Icons.Default.Checklist, contentDescription = null) },
                            label = { Text(stringResource(R.string.nav_playbook)) },
                            modifier = Modifier.testTag("rail_tab_playbook")
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    if (!isPresentationMode) {
                        OfflineStorageNoticeBanner(
                            isDeviceOnline = isDeviceOnline,
                            isGuestMode = isGuestMode
                        )
                    }

                    if (isGuestMode && !isPresentationMode) {
                        GuestLifecycleBanner(
                            guestStatus = guestStatus,
                            onUpgradeClicked = { viewModel.openBusinessUpgradeModal() }
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedTab) {
                            AppTab.LIVE_DEMO -> {
                                LiveDemoScreen(
                                    activeBrand = activeBrand,
                                    quoteState = quoteState,
                                    estimatedTotal = estimatedTotal,
                                    depositAmount = depositAmount,
                                    isPresentationMode = isPresentationMode,
                                    onTogglePresentationMode = {
                                        viewModel.setPresentationMode(!isPresentationMode)
                                    },
                                    onSwitchRevenueStream = viewModel::switchRevenueStream,
                                    onSwitchNichePreset = viewModel::switchNichePreset,
                                    onOpenBrandStudio = { viewModel.selectTab(AppTab.BRAND_STUDIO) },
                                    onOpenPitchScript = { viewModel.selectTab(AppTab.PLAYBOOK) },
                                    onSelectService = viewModel::selectServicePackage,
                                    onUnitQuantityChange = viewModel::updateUnitQuantity,
                                    onToggleAddOn = viewModel::toggleAddOn,
                                    onUpdateBookingForm = viewModel::updateBookingForm,
                                    onSubmitBooking = viewModel::submitBookingIntake,
                                    onDismissLeadReceipt = viewModel::dismissLeadReceiptDialog
                                )
                            }

                            AppTab.BRAND_STUDIO -> {
                                BrandStudioScreen(
                                    activeBrand = activeBrand,
                                    onUpdateBrand = viewModel::updateActiveBrandDraft,
                                    onSwitchNichePreset = viewModel::switchNichePreset,
                                    onPreviewLiveDemo = { viewModel.selectTab(AppTab.LIVE_DEMO) },
                                    onSaveProspectAndLaunch = viewModel::saveActiveBrandAsProspectAndLaunch
                                )
                            }

                            AppTab.CRM_LEADS -> {
                                ProspectCrmScreen(
                                    prospectsState = prospectsState,
                                    leadsState = leadsState,
                                    onLoadProspectIntoDemo = { prospect ->
                                        viewModel.loadProspectIntoDemo(prospect, navigateToDemo = true)
                                    },
                                    onOpenLoomScriptForProspect = { prospect ->
                                        viewModel.loadProspectIntoDemo(prospect, navigateToDemo = false)
                                        viewModel.selectTab(AppTab.PLAYBOOK)
                                    },
                                    onAdvanceProspectStage = viewModel::advanceProspectStage,
                                    onDeleteProspect = viewModel::deleteProspect,
                                    onCreateProspect = viewModel::createNewProspect,
                                    onDeleteLead = viewModel::deleteLead
                                )
                            }

                            AppTab.PLAYBOOK -> {
                                PlaybookAndPitchScreen(
                                    activeBrand = activeBrand,
                                    selectedOfferArchetype = selectedOfferArchetype,
                                    scopeGuardrails = scopeGuardrails,
                                    playbookState = playbookState,
                                    pitchScripts = viewModel.buildPitchScripts(
                                        selectedOfferArchetype,
                                        scopeGuardrails
                                    ),
                                    onSwitchRevenueStream = viewModel::switchRevenueStream,
                                    onSelectOfferArchetype = viewModel::selectOfferArchetype,
                                    onSelectStarterPrice = viewModel::setFixedStarterPrice,
                                    onUpdateScopeGuardrails = viewModel::updateScopeGuardrails,
                                    onToggleTask = viewModel::togglePlaybookTask,
                                    onShowMessage = viewModel::showMessage,
                                    onOpenComplianceDocs = { viewModel.openComplianceDocsModal() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (isGuestMode && guestStatus.shouldShowDay20Prompt && !showBusinessUpgradeModal) {
        Day20MilestoneDialog(
            guestStatus = guestStatus,
            prospectCount = prospectsCount,
            leadCount = leadsCount,
            onUpgradeNow = {
                viewModel.dismissDay20Prompt()
                viewModel.openBusinessUpgradeModal()
            },
            onDismissForToday = {
                viewModel.dismissDay20Prompt()
            }
        )
    }

    if (showBusinessUpgradeModal) {
        PermanentBusinessAccountDialog(
            initialProfile = businessProfile,
            isGuestMode = isGuestMode,
            onDismiss = { viewModel.closeBusinessUpgradeModal() },
            onStageProfileForGoogleUpgrade = viewModel::stageBusinessProfileForGuestUpgrade,
            onSaveProfileDirectly = viewModel::savePermanentBusinessProfileDirectly,
            onGoogleAuthSuccess = {}
        )
    }

    if (showComplianceDocsModal) {
        ComplianceDocsDialog(
            onDismiss = { viewModel.closeComplianceDocsModal() }
        )
    }

    if (showHardwareHudModal) {
        Realme8HardwareHudDialog(
            telemetry = hardwareTelemetry,
            onSelectRefreshRateMode = { mode ->
                hardwareGovernor.setRefreshRateMode(mode, activity)
                viewModel.showMessage("Display governor set to ${mode.label}")
            },
            onSelectGpuRenderScaleMode = { scaleMode ->
                hardwareGovernor.setGpuRenderScaleMode(scaleMode)
                viewModel.showMessage("Mali-G57 GPU set to ${scaleMode.label}")
            },
            onPerformMemoryTrim = {
                hardwareGovernor.performMemoryTrim()
                viewModel.showMessage("4GB LPDDR4X cache purge complete • Zero UI stall")
            },
            onDismiss = { viewModel.closeHardwareHudModal() },
            onSelectPerformanceProfile = { profile ->
                hardwareGovernor.setPerformanceProfile(profile, activity)
                viewModel.showMessage("Dimensity 700 profile set to ${profile.label}")
            },
            onToggleThermalFallback = {
                val fallbackActive = hardwareGovernor.toggleThermalFallback(activity)
                val statusMsg = if (fallbackActive) {
                    "60Hz Thermal Fallback engaged (16.6ms budget + 720p fill-rate saver)"
                } else {
                    "90Hz Adaptive Mode restored (11.1ms budget + 1080p native shaders)"
                }
                viewModel.showMessage(statusMsg)
            }
        )
    }
}
