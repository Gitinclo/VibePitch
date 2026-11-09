package com.gitinclo.vibepitch.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitinclo.vibepitch.data.local.LocalGuestSessionManager
import com.gitinclo.vibepitch.data.model.BusinessAccountProfile
import com.gitinclo.vibepitch.data.model.CapturedLead
import com.gitinclo.vibepitch.data.model.GuestLifecycleStatus
import com.gitinclo.vibepitch.data.model.NicheKey
import com.gitinclo.vibepitch.data.model.PlaybookTask
import com.gitinclo.vibepitch.data.model.ProspectProfile
import com.gitinclo.vibepitch.data.model.RevenueStreamMode
import com.gitinclo.vibepitch.data.repository.VibePitchRepository
import com.gitinclo.vibepitch.domain.VibePitchWorkspaceCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        VibePitchStateTransitions.observeBusinessProfileState(
            scope = viewModelScope,
            repository = repository,
            currentUserId = currentUserId,
            isGuestMode = isGuestMode
        )

    val prospectsState: StateFlow<UiState<List<ProspectProfile>>> =
        VibePitchStateTransitions.observeCollectionState(
            scope = viewModelScope,
            isGuestMode = isGuestMode,
            cloudFlow = { repository?.observeProspects(currentUserId) },
            guestFlow = guestSessionManager.guestProspects,
            fallbackError = "Failed to load prospects"
        )

    val leadsState: StateFlow<UiState<List<CapturedLead>>> =
        VibePitchStateTransitions.observeCollectionState(
            scope = viewModelScope,
            isGuestMode = isGuestMode,
            cloudFlow = { repository?.observeLeads(currentUserId) },
            guestFlow = guestSessionManager.guestLeads,
            fallbackError = "Failed to load leads"
        )

    val playbookState: StateFlow<UiState<List<PlaybookTask>>> =
        VibePitchStateTransitions.observeCollectionState(
            scope = viewModelScope,
            isGuestMode = isGuestMode,
            cloudFlow = { repository?.observePlaybookTasks(currentUserId) },
            guestFlow = guestSessionManager.guestPlaybook,
            fallbackError = "Failed to load 14-day plan"
        )

    init {
        if (isGuestMode) {
            guestSessionManager.refreshStatus()
        } else if (repository != null) {
            viewModelScope.launch {
                coordinator.syncGuestUpgradeOrSeedStarterData()?.let { msg -> _snackbarMessage.value = msg }
            }
        }
    }

    fun selectTab(tab: AppTab) { _selectedTab.value = tab }
    fun selectOfferArchetype(archetype: OfferArchetype) { _selectedOfferArchetype.value = archetype }
    fun setFixedStarterPrice(price: Int) { _activeBrand.update { it.copy(setupFeeOffer = price.coerceIn(50, 10000)) } }
    fun setPresentationMode(enabled: Boolean) { _isPresentationMode.value = enabled }
    fun clearSnackbar() { _snackbarMessage.value = null }
    fun showMessage(msg: String) { _snackbarMessage.value = msg }
    fun openBusinessUpgradeModal() { _showBusinessUpgradeModal.value = true }
    fun closeBusinessUpgradeModal() { _showBusinessUpgradeModal.value = false }
    fun openComplianceDocsModal() { _showComplianceDocsModal.value = true }
    fun closeComplianceDocsModal() { _showComplianceDocsModal.value = false }
    fun setDeviceOnlineStatus(isOnline: Boolean) { _isDeviceOnline.value = isOnline }
    fun dismissDay20Prompt() { guestSessionManager.dismissDay20PromptForToday() }
    fun stageBusinessProfileForGuestUpgrade(profile: BusinessAccountProfile) { guestSessionManager.savePendingBusinessUpgradeDraft(profile) }
    fun selectServicePackage(serviceId: String) { _quoteState.update { it.copy(selectedServiceId = serviceId) } }
    fun updateUnitQuantity(units: Int) { _quoteState.update { it.copy(unitValue = units) } }
    fun dismissLeadReceiptDialog() { _quoteState.update { it.copy(latestSubmittedLead = null) } }

    fun switchRevenueStream(mode: RevenueStreamMode) {
        val targetNiche = when (mode) {
            RevenueStreamMode.LOCAL_BUSINESS -> NicheKey.SALON_AND_CLEANERS
            RevenueStreamMode.FREELANCE_MICRO_TOOLS -> NicheKey.ECOMMERCE_SHOPIFY
            RevenueStreamMode.PROTOTYPE_REMEDIATION -> NicheKey.SAAS_AI_REMEDIATION
        }
        switchNichePreset(targetNiche)
    }

    fun updateScopeGuardrails(
        paymentGuardrail: String = _scopeGuardrailConfig.value.paymentGuardrail,
        dataPrivacyGuardrail: String = _scopeGuardrailConfig.value.dataPrivacyGuardrail,
        authGuardrail: String = _scopeGuardrailConfig.value.authGuardrail,
        supportWindowDays: Int = _scopeGuardrailConfig.value.supportWindowDays
    ) {
        _scopeGuardrailConfig.value = ScopeGuardrailConfig(paymentGuardrail, dataPrivacyGuardrail, authGuardrail, supportWindowDays)
    }

    fun savePermanentBusinessProfileDirectly(profile: BusinessAccountProfile) {
        viewModelScope.launch {
            coordinator.saveBusinessProfile(profile, _isDeviceOnline.value)
                .onSuccess { message ->
                    _showBusinessUpgradeModal.value = false
                    _snackbarMessage.value = message
                }
                .onFailure { err -> _snackbarMessage.value = err.localizedMessage ?: "Failed to save business profile" }
        }
    }

    fun switchNichePreset(niche: NicheKey) {
        val (archetype, updatedBrand, updatedQuote) = VibePitchStateTransitions.buildBrandAndQuoteForNichePreset(
            currentBrand = _activeBrand.value,
            currentQuote = _quoteState.value,
            niche = niche
        )
        _selectedOfferArchetype.value = archetype
        _activeBrand.value = updatedBrand
        _quoteState.value = updatedQuote
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
        val (updatedBrand, updatedQuote) = VibePitchStateTransitions.applyActiveBrandDraft(
            currentBrand = _activeBrand.value,
            currentQuote = _quoteState.value,
            businessName = businessName,
            contactName = contactName,
            niche = niche,
            tagline = tagline,
            brandColorHex = brandColorHex,
            websiteGapNote = websiteGapNote,
            depositPercent = depositPercent,
            setupFeeOffer = setupFeeOffer,
            monthlyRetainerOffer = monthlyRetainerOffer
        )
        _activeBrand.value = updatedBrand
        _quoteState.value = updatedQuote
    }

    fun loadProspectIntoDemo(prospect: ProspectProfile, navigateToDemo: Boolean = true) {
        val (brand, quote) = VibePitchStateTransitions.buildBrandAndQuoteFromProspect(_quoteState.value, prospect)
        _activeBrand.value = brand
        _quoteState.value = quote
        if (navigateToDemo) _selectedTab.value = AppTab.LIVE_DEMO
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
                .onFailure { err -> _snackbarMessage.value = err.localizedMessage ?: "Failed to save prospect" }
        }
    }

    fun createNewProspect(businessName: String, contactName: String, niche: NicheKey, websiteGapNote: String, brandColorHex: String) {
        if (businessName.isBlank()) {
            _snackbarMessage.value = "Business name is required"
            return
        }
        val prospect = coordinator.buildNewProspect(businessName, contactName, niche, websiteGapNote, brandColorHex)
        val isOnline = _isDeviceOnline.value
        viewModelScope.launch {
            coordinator.persistProspect(prospect, isOnline)
                .onSuccess {
                    _snackbarMessage.value = if (!isOnline) "Offline • Added ${prospect.businessName} to local storage" else "Added ${prospect.businessName} to pipeline"
                }
                .onFailure { err -> _snackbarMessage.value = err.localizedMessage ?: "Could not create prospect" }
        }
    }

    fun advanceProspectStage(prospect: ProspectProfile) {
        val isOnline = _isDeviceOnline.value
        viewModelScope.launch {
            coordinator.advanceProspectStage(prospect, isOnline)
                .onSuccess { next ->
                    _snackbarMessage.value = if (!isOnline) "Offline • ${prospect.businessName} moved to ${next.label} (saved locally)" else "${prospect.businessName} moved to ${next.label}"
                }
                .onFailure { err -> _snackbarMessage.value = err.localizedMessage ?: "Failed to update stage" }
        }
    }

    fun deleteProspect(prospectId: String) {
        val isOnline = _isDeviceOnline.value
        viewModelScope.launch {
            coordinator.deleteProspect(prospectId, isOnline)
                .onSuccess { _snackbarMessage.value = if (!isOnline) "Offline • Prospect removed from local storage" else "Prospect removed" }
                .onFailure { err -> _snackbarMessage.value = err.localizedMessage ?: "Delete failed" }
        }
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
        _quoteState.update { it.copy(customerName = customerName, customerPhone = customerPhone, customerEmail = customerEmail, preferredDate = preferredDate, notes = notes) }
    }

    fun calculateCurrentQuote(): Pair<Int, Int> = QuotePricingCalculator.calculateEstimateAndDeposit(_activeBrand.value, _quoteState.value)

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
            _quoteState.update { it.copy(isSubmitting = false, customerName = "", customerPhone = "", customerEmail = "", notes = "", latestSubmittedLead = lead) }
            _snackbarMessage.value = if (!isOnline) "Offline • Lead captured & saved to local storage!" else "Lead captured & saved to Guest CRM!"
        } else {
            _quoteState.update { it.copy(isSubmitting = true) }
            viewModelScope.launch {
                repository?.submitLead(lead)
                    ?.onSuccess {
                        _quoteState.update { it.copy(isSubmitting = false, customerName = "", customerPhone = "", customerEmail = "", notes = "", latestSubmittedLead = lead) }
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
                .onSuccess { _snackbarMessage.value = if (!isOnline) "Offline • Lead deleted from local storage" else "Lead deleted" }
                .onFailure { err -> _snackbarMessage.value = err.localizedMessage ?: "Failed to delete lead" }
        }
    }

    fun togglePlaybookTask(task: PlaybookTask) {
        val isOnline = _isDeviceOnline.value
        viewModelScope.launch {
            coordinator.togglePlaybookTask(task, isOnline)
                .onSuccess { if (!isOnline) _snackbarMessage.value = "Offline • Checklist progress saved to local storage" }
                .onFailure { err -> _snackbarMessage.value = err.localizedMessage ?: "Could not update checklist" }
        }
    }

    fun buildPitchScripts(
        archetype: OfferArchetype = _selectedOfferArchetype.value,
        guardrails: ScopeGuardrailConfig = _scopeGuardrailConfig.value
    ): GeneratedPitchScripts {
        val brand = _activeBrand.value
        val (sampleTotal, sampleDeposit) = calculateCurrentQuote()
        return PitchScriptGenerator.buildPitchScripts(
            nicheKey = brand.niche, archetype = archetype, businessName = brand.businessName, contactName = brand.contactName,
            setupFee = brand.setupFeeOffer, monthlyRetainer = brand.monthlyRetainerOffer, depositPercent = brand.depositPercent,
            guardrailConfig = guardrails, websiteGapNote = brand.websiteGapNote, sampleTotal = sampleTotal, sampleDeposit = sampleDeposit
        )
    }
}
