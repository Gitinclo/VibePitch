package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.LocalGuestSessionManager
import com.example.data.model.BusinessAccountProfile
import com.example.data.model.CapturedLead
import com.example.data.model.GuestLifecycleStatus
import com.example.data.model.NicheCatalog
import com.example.data.model.NicheKey
import com.example.data.model.PlaybookTask
import com.example.data.model.ProspectProfile
import com.example.data.model.RevenueStreamMode
import com.example.data.repository.VibePitchRepository
import com.example.domain.InputBoundsValidator
import com.example.domain.VibePitchWorkspaceCoordinator
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VibePitchViewModel(
    private val repository: VibePitchRepository?,
    private val guestSessionManager: LocalGuestSessionManager,
    private val currentUserId: String,
    val isGuestMode: Boolean
) : ViewModel() {

    private val coordinator = VibePitchWorkspaceCoordinator(
        repository = repository,
        guestSessionManager = guestSessionManager,
        currentUserId = currentUserId,
        isGuestMode = isGuestMode
    )

    private val _selectedTab = MutableStateFlow(AppTab.LIVE_DEMO)
    val selectedTab: StateFlow<AppTab> = _selectedTab.asStateFlow()

    private val _isPresentationMode = MutableStateFlow(false)
    val isPresentationMode: StateFlow<Boolean> = _isPresentationMode.asStateFlow()

    private val _activeBrand = MutableStateFlow(ActiveDemoBrandState())
    val activeBrand: StateFlow<ActiveDemoBrandState> = _activeBrand.asStateFlow()

    private val _selectedOfferArchetype = MutableStateFlow(OfferArchetype.BOOKING_QUOTE_FORM)
    val selectedOfferArchetype: StateFlow<OfferArchetype> = _selectedOfferArchetype.asStateFlow()

    private val _scopeGuardrailConfig = MutableStateFlow(ScopeGuardrailConfig())
    val scopeGuardrailConfig: StateFlow<ScopeGuardrailConfig> = _scopeGuardrailConfig.asStateFlow()

    private val _quoteState = MutableStateFlow(QuoteCalculatorState())
    val quoteState: StateFlow<QuoteCalculatorState> = _quoteState.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _showBusinessUpgradeModal = MutableStateFlow(false)
    val showBusinessUpgradeModal: StateFlow<Boolean> = _showBusinessUpgradeModal.asStateFlow()

    private val _showComplianceDocsModal = MutableStateFlow(false)
    val showComplianceDocsModal: StateFlow<Boolean> = _showComplianceDocsModal.asStateFlow()

    private val _isDeviceOnline = MutableStateFlow(true)
    val isDeviceOnline: StateFlow<Boolean> = _isDeviceOnline.asStateFlow()

    val guestStatus: StateFlow<GuestLifecycleStatus> = guestSessionManager.guestStatus

    val businessProfileState: StateFlow<BusinessAccountProfile?> =
        if (!isGuestMode && repository != null) {
            repository.observeBusinessProfile(currentUserId)
                .catch { error ->
                    val firestoreError = error.unwrapFirestoreException()
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w(TAG, "Error observing business profile", firestoreError ?: error)
                    emit(null)
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = null
                )
        } else {
            flowOf(null).stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = null
            )
        }

    val prospectsState: StateFlow<UiState<List<ProspectProfile>>> =
        observeCollectionAsUiState(
            cloudFlow = { repository?.observeProspects(currentUserId) },
            guestFlow = guestSessionManager.guestProspects,
            fallbackError = "Failed to load prospects"
        )

    val leadsState: StateFlow<UiState<List<CapturedLead>>> =
        observeCollectionAsUiState(
            cloudFlow = { repository?.observeLeads(currentUserId) },
            guestFlow = guestSessionManager.guestLeads,
            fallbackError = "Failed to load leads"
        )

    val playbookState: StateFlow<UiState<List<PlaybookTask>>> =
        observeCollectionAsUiState(
            cloudFlow = { repository?.observePlaybookTasks(currentUserId) },
            guestFlow = guestSessionManager.guestPlaybook,
            fallbackError = "Failed to load 14-day plan"
        )

    init {
        if (isGuestMode) {
            guestSessionManager.refreshStatus()
        } else if (repository != null) {
            viewModelScope.launch {
                coordinator.syncGuestUpgradeOrSeedStarterData()?.let { msg ->
                    _snackbarMessage.value = msg
                }
            }
        }
    }

    fun selectTab(tab: AppTab) {
        _selectedTab.value = tab
    }

    fun selectOfferArchetype(archetype: OfferArchetype) {
        _selectedOfferArchetype.value = archetype
    }

    fun switchRevenueStream(mode: RevenueStreamMode) {
        val targetNiche = when (mode) {
            RevenueStreamMode.LOCAL_BUSINESS -> NicheKey.SALON_AND_CLEANERS
            RevenueStreamMode.FREELANCE_MICRO_TOOLS -> NicheKey.ECOMMERCE_SHOPIFY
            RevenueStreamMode.PROTOTYPE_REMEDIATION -> NicheKey.SAAS_AI_REMEDIATION
        }
        val targetArchetype = when (mode) {
            RevenueStreamMode.LOCAL_BUSINESS -> OfferArchetype.BOOKING_QUOTE_FORM
            RevenueStreamMode.FREELANCE_MICRO_TOOLS -> OfferArchetype.UPWORK_MICRO_TOOL
            RevenueStreamMode.PROTOTYPE_REMEDIATION -> OfferArchetype.AI_PROTOTYPE_RESCUE
        }
        _selectedOfferArchetype.value = targetArchetype
        switchNichePreset(targetNiche)
    }

    fun updateScopeGuardrails(
        paymentGuardrail: String = _scopeGuardrailConfig.value.paymentGuardrail,
        dataPrivacyGuardrail: String = _scopeGuardrailConfig.value.dataPrivacyGuardrail,
        authGuardrail: String = _scopeGuardrailConfig.value.authGuardrail,
        supportWindowDays: Int = _scopeGuardrailConfig.value.supportWindowDays
    ) {
        _scopeGuardrailConfig.value = ScopeGuardrailConfig(
            paymentGuardrail = paymentGuardrail,
            dataPrivacyGuardrail = dataPrivacyGuardrail,
            authGuardrail = authGuardrail,
            supportWindowDays = supportWindowDays
        )
    }

    fun setFixedStarterPrice(price: Int) {
        _activeBrand.update { it.copy(setupFeeOffer = price.coerceIn(50, 10000)) }
    }

    fun setPresentationMode(enabled: Boolean) {
        _isPresentationMode.value = enabled
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun openBusinessUpgradeModal() {
        _showBusinessUpgradeModal.value = true
    }

    fun closeBusinessUpgradeModal() {
        _showBusinessUpgradeModal.value = false
    }

    fun openComplianceDocsModal() {
        _showComplianceDocsModal.value = true
    }

    fun closeComplianceDocsModal() {
        _showComplianceDocsModal.value = false
    }

    fun setDeviceOnlineStatus(isOnline: Boolean) {
        _isDeviceOnline.value = isOnline
    }

    fun dismissDay20Prompt() {
        guestSessionManager.dismissDay20PromptForToday()
    }

    fun stageBusinessProfileForGuestUpgrade(profile: BusinessAccountProfile) {
        guestSessionManager.savePendingBusinessUpgradeDraft(profile)
    }

    fun savePermanentBusinessProfileDirectly(profile: BusinessAccountProfile) {
        viewModelScope.launch {
            coordinator.saveBusinessProfile(profile, _isDeviceOnline.value)
                .onSuccess { message ->
                    _showBusinessUpgradeModal.value = false
                    _snackbarMessage.value = message
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Failed to save business profile"
                }
        }
    }

    fun switchNichePreset(niche: NicheKey) {
        val packages = NicheCatalog.getServicePackages(niche)
        val firstPackage = packages.firstOrNull { it.popularBadge } ?: packages.first()
        val firstAddOn = NicheCatalog.getAddOns(niche).firstOrNull()?.id

        _selectedOfferArchetype.value = when (niche.revenueStream) {
            RevenueStreamMode.LOCAL_BUSINESS -> OfferArchetype.BOOKING_QUOTE_FORM
            RevenueStreamMode.FREELANCE_MICRO_TOOLS -> OfferArchetype.UPWORK_MICRO_TOOL
            RevenueStreamMode.PROTOTYPE_REMEDIATION -> OfferArchetype.AI_PROTOTYPE_RESCUE
        }

        _activeBrand.update {
            it.copy(
                niche = niche,
                businessName = niche.defaultBusinessName,
                contactName = niche.defaultContactName,
                tagline = niche.defaultTagline,
                brandColorHex = niche.defaultColorHex,
                websiteGapNote = niche.microProblemSummary
            )
        }
        _quoteState.update {
            it.copy(
                selectedServiceId = firstPackage.id,
                unitValue = niche.defaultUnitValue,
                selectedAddOnIds = setOfNotNull(firstAddOn)
            )
        }
    }

    fun updateActiveBrandDraft(
        businessName: String = _activeBrand.value.businessName,
        contactName: String = _activeBrand.value.contactName,
        niche: NicheKey = _activeBrand.value.niche,
        tagline: String = _activeBrand.value.tagline,
        brandColorHex: String = _activeBrand.value.brandColorHex,
        websiteGapNote: String = _activeBrand.value.websiteGapNote,
        depositPercent: Int = _activeBrand.value.depositPercent,
        setupFeeOffer: Int = _activeBrand.value.setupFeeOffer,
        monthlyRetainerOffer: Int = _activeBrand.value.monthlyRetainerOffer
    ) {
        val nicheChanged = niche != _activeBrand.value.niche
        _activeBrand.value = _activeBrand.value.copy(
            businessName = InputBoundsValidator.sanitizeBusinessName(businessName),
            contactName = InputBoundsValidator.sanitizeContactName(contactName),
            niche = niche,
            tagline = InputBoundsValidator.sanitizeTagline(tagline),
            brandColorHex = brandColorHex,
            websiteGapNote = InputBoundsValidator.sanitizeWebsiteGapNote(websiteGapNote),
            depositPercent = InputBoundsValidator.clampDepositPercent(depositPercent),
            setupFeeOffer = InputBoundsValidator.clampSetupFeeOffer(setupFeeOffer),
            monthlyRetainerOffer = InputBoundsValidator.clampMonthlyRetainerOffer(monthlyRetainerOffer)
        )
        if (nicheChanged) {
            val packages = NicheCatalog.getServicePackages(niche)
            val defaultPkg = packages.firstOrNull { it.popularBadge } ?: packages.first()
            _quoteState.update {
                it.copy(
                    selectedServiceId = defaultPkg.id,
                    unitValue = niche.defaultUnitValue,
                    selectedAddOnIds = emptySet()
                )
            }
        }
    }

    fun loadProspectIntoDemo(prospect: ProspectProfile, navigateToDemo: Boolean = true) {
        val niche = NicheKey.fromKey(prospect.nicheKey)
        val packages = NicheCatalog.getServicePackages(niche)
        val defaultPkg = packages.firstOrNull { it.popularBadge } ?: packages.first()

        _activeBrand.value = ActiveDemoBrandState(
            prospectId = prospect.id,
            businessName = InputBoundsValidator.sanitizeBusinessName(prospect.businessName),
            contactName = InputBoundsValidator.sanitizeContactName(prospect.contactName),
            niche = niche,
            tagline = InputBoundsValidator.sanitizeTagline(prospect.tagline.ifBlank { niche.defaultTagline }),
            brandColorHex = prospect.brandColorHex.ifBlank { niche.defaultColorHex },
            websiteGapNote = InputBoundsValidator.sanitizeWebsiteGapNote(prospect.websiteGapNote.ifBlank { niche.microProblemSummary }),
            depositPercent = InputBoundsValidator.clampDepositPercent(prospect.depositPercent.toInt()),
            setupFeeOffer = InputBoundsValidator.clampSetupFeeOffer(prospect.setupFeeOffer.toInt()),
            monthlyRetainerOffer = InputBoundsValidator.clampMonthlyRetainerOffer(prospect.monthlyRetainerOffer.toInt())
        )
        _quoteState.update {
            it.copy(
                selectedServiceId = defaultPkg.id,
                unitValue = niche.defaultUnitValue,
                selectedAddOnIds = emptySet()
            )
        }
        if (navigateToDemo) {
            _selectedTab.value = AppTab.LIVE_DEMO
        }
        _snackbarMessage.value = "Loaded ${prospect.businessName} into Live Demo"
    }

    fun saveActiveBrandAsProspectAndLaunch() {
        val brand = _activeBrand.value
        if (brand.businessName.isBlank()) {
            _snackbarMessage.value = "Enter a business name first"
            return
        }
        val prospect = coordinator.buildProspectFromBrand(brand)
        val isOnline = _isDeviceOnline.value
        viewModelScope.launch {
            coordinator.persistProspect(prospect, isOnline)
                .onSuccess { newId ->
                    _activeBrand.update { it.copy(prospectId = newId) }
                    _selectedTab.value = AppTab.LIVE_DEMO
                    _snackbarMessage.value = when {
                        !isOnline -> "Offline • Saved ${prospect.businessName} to local storage & updated Demo"
                        coordinator.isOfflineOrGuest(isOnline) -> "Saved ${prospect.businessName} to Guest CRM (local storage) & updated Demo"
                        else -> "Saved ${prospect.businessName} to CRM & updated Live Demo"
                    }
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Failed to save prospect"
                }
        }
    }

    fun createNewProspect(
        businessName: String,
        contactName: String,
        niche: NicheKey,
        websiteGapNote: String,
        brandColorHex: String
    ) {
        if (businessName.isBlank()) {
            _snackbarMessage.value = "Business name is required"
            return
        }
        val prospect = coordinator.buildNewProspect(businessName, contactName, niche, websiteGapNote, brandColorHex)
        val isOnline = _isDeviceOnline.value
        viewModelScope.launch {
            coordinator.persistProspect(prospect, isOnline)
                .onSuccess {
                    _snackbarMessage.value = if (!isOnline) {
                        "Offline • Added ${prospect.businessName} to local storage"
                    } else {
                        "Added ${prospect.businessName} to pipeline"
                    }
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Could not create prospect"
                }
        }
    }

    fun advanceProspectStage(prospect: ProspectProfile) {
        val isOnline = _isDeviceOnline.value
        viewModelScope.launch {
            coordinator.advanceProspectStage(prospect, isOnline)
                .onSuccess { next ->
                    _snackbarMessage.value = if (!isOnline) {
                        "Offline • ${prospect.businessName} moved to ${next.label} (saved locally)"
                    } else {
                        "${prospect.businessName} moved to ${next.label}"
                    }
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Failed to update stage"
                }
        }
    }

    fun deleteProspect(prospectId: String) {
        val isOnline = _isDeviceOnline.value
        viewModelScope.launch {
            coordinator.deleteProspect(prospectId, isOnline)
                .onSuccess {
                    _snackbarMessage.value = if (!isOnline) {
                        "Offline • Prospect removed from local storage"
                    } else {
                        "Prospect removed"
                    }
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Delete failed"
                }
        }
    }

    fun selectServicePackage(serviceId: String) {
        _quoteState.update { it.copy(selectedServiceId = serviceId) }
    }

    fun updateUnitQuantity(units: Int) {
        _quoteState.update { it.copy(unitValue = units) }
    }

    fun toggleAddOn(addOnId: String) {
        _quoteState.update { state ->
            val updated = state.selectedAddOnIds.toMutableSet()
            if (!updated.add(addOnId)) updated.remove(addOnId)
            state.copy(selectedAddOnIds = updated)
        }
    }

    fun updateBookingForm(
        customerName: String = _quoteState.value.customerName,
        customerPhone: String = _quoteState.value.customerPhone,
        customerEmail: String = _quoteState.value.customerEmail,
        preferredDate: String = _quoteState.value.preferredDate,
        notes: String = _quoteState.value.notes
    ) {
        _quoteState.update {
            it.copy(
                customerName = customerName,
                customerPhone = customerPhone,
                customerEmail = customerEmail,
                preferredDate = preferredDate,
                notes = notes
            )
        }
    }

    fun dismissLeadReceiptDialog() {
        _quoteState.update { it.copy(latestSubmittedLead = null) }
    }

    fun calculateCurrentQuote(): Pair<Int, Int> =
        QuotePricingCalculator.calculateEstimateAndDeposit(_activeBrand.value, _quoteState.value)

    fun submitBookingIntake() {
        val brand = _activeBrand.value
        val calc = _quoteState.value
        if (calc.customerName.isBlank() || calc.customerPhone.isBlank()) {
            _snackbarMessage.value = "Please enter customer name and phone number to lock in the quote"
            return
        }

        val (estimatedTotal, depositAmount) = calculateCurrentQuote()
        val lead = coordinator.buildCapturedLead(brand, calc, estimatedTotal, depositAmount)
        val isOnline = _isDeviceOnline.value

        if (coordinator.isOfflineOrGuest(isOnline)) {
            guestSessionManager.addGuestLead(lead)
            _quoteState.update {
                it.copy(
                    isSubmitting = false,
                    customerName = "",
                    customerPhone = "",
                    customerEmail = "",
                    notes = "",
                    latestSubmittedLead = lead
                )
            }
            _snackbarMessage.value = if (!isOnline) {
                "Offline • Lead captured & saved to local storage!"
            } else {
                "Lead captured & saved to Guest CRM!"
            }
        } else {
            _quoteState.update { it.copy(isSubmitting = true) }
            viewModelScope.launch {
                repository?.submitLead(lead)
                    ?.onSuccess {
                        _quoteState.update {
                            it.copy(
                                isSubmitting = false,
                                customerName = "",
                                customerPhone = "",
                                customerEmail = "",
                                notes = "",
                                latestSubmittedLead = lead
                            )
                        }
                        _snackbarMessage.value = "Lead captured & saved to CRM!"
                    }
                    ?.onFailure { err ->
                        _quoteState.update { it.copy(isSubmitting = false) }
                        _snackbarMessage.value = err.localizedMessage ?: "Failed to submit booking"
                    }
            }
        }
    }

    fun deleteLead(leadId: String) {
        val isOnline = _isDeviceOnline.value
        viewModelScope.launch {
            coordinator.deleteLead(leadId, isOnline)
                .onSuccess {
                    _snackbarMessage.value = if (!isOnline) {
                        "Offline • Lead deleted from local storage"
                    } else {
                        "Lead deleted"
                    }
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Failed to delete lead"
                }
        }
    }

    fun togglePlaybookTask(task: PlaybookTask) {
        val isOnline = _isDeviceOnline.value
        viewModelScope.launch {
            coordinator.togglePlaybookTask(task, isOnline)
                .onSuccess {
                    if (!isOnline) {
                        _snackbarMessage.value = "Offline • Checklist progress saved to local storage"
                    }
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Could not update checklist"
                }
        }
    }

    fun buildPitchScripts(
        archetype: OfferArchetype = _selectedOfferArchetype.value,
        guardrails: ScopeGuardrailConfig = _scopeGuardrailConfig.value
    ): GeneratedPitchScripts {
        val brand = _activeBrand.value
        val (sampleTotal, sampleDeposit) = calculateCurrentQuote()
        return PitchScriptGenerator.buildPitchScripts(
            nicheKey = brand.niche,
            archetype = archetype,
            businessName = brand.businessName,
            contactName = brand.contactName,
            setupFee = brand.setupFeeOffer,
            monthlyRetainer = brand.monthlyRetainerOffer,
            depositPercent = brand.depositPercent,
            guardrailConfig = guardrails,
            websiteGapNote = brand.websiteGapNote,
            sampleTotal = sampleTotal,
            sampleDeposit = sampleDeposit
        )
    }

    private fun <T> observeCollectionAsUiState(
        cloudFlow: () -> Flow<List<T>>?,
        guestFlow: StateFlow<List<T>>,
        fallbackError: String
    ): StateFlow<UiState<List<T>>> {
        val activeCloudFlow = if (!isGuestMode) cloudFlow() else null
        return if (activeCloudFlow != null) {
            activeCloudFlow
                .map<List<T>, UiState<List<T>>> { UiState.Success(it) }
                .catch { error ->
                    val firestoreError = error.unwrapFirestoreException()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    Log.w(TAG, fallbackError, rootError)
                    emit(UiState.Error(rootError.message ?: fallbackError))
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = UiState.Loading
                )
        } else {
            guestFlow
                .map<List<T>, UiState<List<T>>> { UiState.Success(it) }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = UiState.Success(guestFlow.value)
                )
        }
    }

    private fun Throwable.unwrapFirestoreException(): FirebaseFirestoreException? =
        generateSequence(this) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    private companion object {
        const val TAG = "VibePitchVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
