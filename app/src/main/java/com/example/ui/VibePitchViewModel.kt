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
import com.example.data.model.PipelineStage
import com.example.data.model.PlaybookTask
import com.example.data.model.ProspectProfile
import com.example.data.model.RevenueStreamMode
import com.example.data.repository.VibePitchRepository
import com.example.domain.InputBoundsValidator
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
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
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
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
        if (!isGuestMode && repository != null) {
            repository.observeProspects(currentUserId)
                .map<List<ProspectProfile>, UiState<List<ProspectProfile>>> { UiState.Success(it) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    Log.w(TAG, "Error observing prospects", rootError)
                    emit(UiState.Error(rootError.message ?: "Failed to load prospects"))
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = UiState.Loading
                )
        } else {
            guestSessionManager.guestProspects
                .map<List<ProspectProfile>, UiState<List<ProspectProfile>>> { UiState.Success(it) }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = UiState.Success(guestSessionManager.guestProspects.value)
                )
        }

    val leadsState: StateFlow<UiState<List<CapturedLead>>> =
        if (!isGuestMode && repository != null) {
            repository.observeLeads(currentUserId)
                .map<List<CapturedLead>, UiState<List<CapturedLead>>> { UiState.Success(it) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    Log.w(TAG, "Error observing leads", rootError)
                    emit(UiState.Error(rootError.message ?: "Failed to load leads"))
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = UiState.Loading
                )
        } else {
            guestSessionManager.guestLeads
                .map<List<CapturedLead>, UiState<List<CapturedLead>>> { UiState.Success(it) }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = UiState.Success(guestSessionManager.guestLeads.value)
                )
        }

    val playbookState: StateFlow<UiState<List<PlaybookTask>>> =
        if (!isGuestMode && repository != null) {
            repository.observePlaybookTasks(currentUserId)
                .map<List<PlaybookTask>, UiState<List<PlaybookTask>>> { UiState.Success(it) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    Log.w(TAG, "Error observing playbook", rootError)
                    emit(UiState.Error(rootError.message ?: "Failed to load 14-day plan"))
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = UiState.Loading
                )
        } else {
            guestSessionManager.guestPlaybook
                .map<List<PlaybookTask>, UiState<List<PlaybookTask>>> { UiState.Success(it) }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = UiState.Success(guestSessionManager.guestPlaybook.value)
                )
        }

    init {
        if (isGuestMode) {
            guestSessionManager.refreshStatus()
        } else if (repository != null) {
            viewModelScope.launch {
                val pendingProfile = guestSessionManager.consumePendingBusinessUpgradeDraft(currentUserId)
                if (guestSessionManager.hasGuestDataToMigrate() || pendingProfile != null) {
                    repository.migrateGuestDataToCloud(
                        userId = currentUserId,
                        profile = pendingProfile,
                        guestProspects = guestSessionManager.guestProspects.value,
                        guestLeads = guestSessionManager.guestLeads.value,
                        guestPlaybook = guestSessionManager.guestPlaybook.value
                    ).onSuccess {
                        guestSessionManager.markGuestMigratedAndDeactivate()
                        _snackbarMessage.value = "Permanent Business Account activated & guest data synced!"
                    }.onFailure {
                        repository.seedStarterDataIfEmpty(currentUserId)
                    }
                } else {
                    repository.seedStarterDataIfEmpty(currentUserId)
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
        if (!_isDeviceOnline.value || repository == null) {
            guestSessionManager.savePendingBusinessUpgradeDraft(profile)
            _showBusinessUpgradeModal.value = false
            _snackbarMessage.value = "Offline • Business profile saved to local storage"
            return
        }
        viewModelScope.launch {
            repository.saveBusinessProfile(profile.copy(userId = currentUserId))
                .onSuccess {
                    _showBusinessUpgradeModal.value = false
                    _snackbarMessage.value = "Business Account Profile saved to Firestore!"
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

        val matchingArchetype = when (niche.revenueStream) {
            RevenueStreamMode.LOCAL_BUSINESS -> OfferArchetype.BOOKING_QUOTE_FORM
            RevenueStreamMode.FREELANCE_MICRO_TOOLS -> OfferArchetype.UPWORK_MICRO_TOOL
            RevenueStreamMode.PROTOTYPE_REMEDIATION -> OfferArchetype.AI_PROTOTYPE_RESCUE
        }
        _selectedOfferArchetype.value = matchingArchetype

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
        val normalized = InputBoundsValidator.normalizeProspectInput(
            businessName = brand.businessName,
            contactName = brand.contactName.ifBlank { "Owner" },
            websiteGapNote = brand.websiteGapNote.ifBlank { brand.niche.microProblemSummary },
            tagline = brand.tagline.ifBlank { brand.niche.defaultTagline },
            depositPercent = brand.depositPercent,
            setupFeeOffer = brand.setupFeeOffer,
            monthlyRetainerOffer = brand.monthlyRetainerOffer
        )
        val prospect = ProspectProfile(
            id = "",
            userId = currentUserId,
            businessName = normalized.businessName,
            contactName = normalized.contactName,
            nicheKey = brand.niche.key,
            websiteGapNote = normalized.websiteGapNote,
            brandColorHex = brand.brandColorHex,
            tagline = normalized.tagline,
            depositPercent = normalized.depositPercent.toLong(),
            setupFeeOffer = normalized.setupFeeOffer.toLong(),
            monthlyRetainerOffer = normalized.monthlyRetainerOffer.toLong(),
            stage = PipelineStage.DEMO_READY.key
        )
        val offlineOrGuest = isGuestMode || repository == null || !_isDeviceOnline.value
        if (offlineOrGuest) {
            val newId = guestSessionManager.addOrUpdateGuestProspect(prospect)
            _activeBrand.update { it.copy(prospectId = newId) }
            _selectedTab.value = AppTab.LIVE_DEMO
            _snackbarMessage.value = if (!_isDeviceOnline.value) {
                "Offline • Saved ${normalized.businessName} to local storage & updated Demo"
            } else {
                "Saved ${normalized.businessName} to Guest CRM (local storage) & updated Demo"
            }
        } else {
            viewModelScope.launch {
                repository.createProspect(prospect)
                    .onSuccess { newId ->
                        _activeBrand.update { it.copy(prospectId = newId) }
                        _selectedTab.value = AppTab.LIVE_DEMO
                        _snackbarMessage.value = "Saved ${normalized.businessName} to CRM & updated Live Demo"
                    }
                    .onFailure { err ->
                        _snackbarMessage.value = err.localizedMessage ?: "Failed to save prospect"
                    }
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
        val normalized = InputBoundsValidator.normalizeProspectInput(
            businessName = businessName,
            contactName = contactName.ifBlank { "Owner" },
            websiteGapNote = websiteGapNote.ifBlank { niche.microProblemSummary },
            tagline = niche.defaultTagline,
            depositPercent = 20,
            setupFeeOffer = 250,
            monthlyRetainerOffer = 35
        )
        val prospect = ProspectProfile(
            userId = currentUserId,
            businessName = normalized.businessName,
            contactName = normalized.contactName,
            nicheKey = niche.key,
            websiteGapNote = normalized.websiteGapNote,
            brandColorHex = brandColorHex,
            tagline = normalized.tagline,
            depositPercent = normalized.depositPercent.toLong(),
            setupFeeOffer = normalized.setupFeeOffer.toLong(),
            monthlyRetainerOffer = normalized.monthlyRetainerOffer.toLong(),
            stage = PipelineStage.AUDITED.key
        )
        val offlineOrGuest = isGuestMode || repository == null || !_isDeviceOnline.value
        if (offlineOrGuest) {
            guestSessionManager.addOrUpdateGuestProspect(prospect)
            _snackbarMessage.value = if (!_isDeviceOnline.value) {
                "Offline • Added ${normalized.businessName} to local storage"
            } else {
                "Added ${normalized.businessName} to pipeline"
            }
        } else {
            viewModelScope.launch {
                repository.createProspect(prospect)
                    .onSuccess {
                        _snackbarMessage.value = "Added ${normalized.businessName} to pipeline"
                    }
                    .onFailure { err ->
                        _snackbarMessage.value = err.localizedMessage ?: "Could not create prospect"
                    }
            }
        }
    }

    fun advanceProspectStage(prospect: ProspectProfile) {
        val current = PipelineStage.fromKey(prospect.stage)
        val next = current.nextStage()
        val updated = prospect.copy(stage = next.key)
        val offlineOrGuest = isGuestMode || repository == null || !_isDeviceOnline.value
        if (offlineOrGuest) {
            guestSessionManager.addOrUpdateGuestProspect(updated)
            _snackbarMessage.value = if (!_isDeviceOnline.value) {
                "Offline • ${prospect.businessName} moved to ${next.label} (saved locally)"
            } else {
                "${prospect.businessName} moved to ${next.label}"
            }
        } else {
            viewModelScope.launch {
                repository.updateProspect(updated)
                    .onSuccess {
                        _snackbarMessage.value = "${prospect.businessName} moved to ${next.label}"
                    }
                    .onFailure { err ->
                        _snackbarMessage.value = err.localizedMessage ?: "Failed to update stage"
                    }
            }
        }
    }

    fun deleteProspect(prospectId: String) {
        val offlineOrGuest = isGuestMode || repository == null || !_isDeviceOnline.value
        if (offlineOrGuest) {
            guestSessionManager.deleteGuestProspect(prospectId)
            _snackbarMessage.value = if (!_isDeviceOnline.value) {
                "Offline • Prospect removed from local storage"
            } else {
                "Prospect removed"
            }
        } else {
            viewModelScope.launch {
                repository.deleteProspect(prospectId)
                    .onSuccess { _snackbarMessage.value = "Prospect removed" }
                    .onFailure { err -> _snackbarMessage.value = err.localizedMessage ?: "Delete failed" }
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
            if (!updated.add(addOnId)) {
                updated.remove(addOnId)
            }
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

        val packages = NicheCatalog.getServicePackages(brand.niche)
        val selectedPkg = packages.firstOrNull { it.id == calc.selectedServiceId } ?: packages.first()
        val selectedAddOnNames = NicheCatalog.getAddOns(brand.niche)
            .filter { calc.selectedAddOnIds.contains(it.id) }
            .joinToString(", ") { it.name }
        val (estimatedTotal, depositAmount) = calculateCurrentQuote()

        val lead = CapturedLead(
            userId = currentUserId,
            prospectId = brand.prospectId,
            prospectBusinessName = brand.businessName,
            nicheKey = brand.niche.key,
            customerName = calc.customerName.trim(),
            customerPhone = calc.customerPhone.trim(),
            customerEmail = calc.customerEmail.trim(),
            preferredDate = calc.preferredDate.trim().ifBlank { "Next Available Slot" },
            selectedServiceName = selectedPkg.title,
            selectedAddOns = selectedAddOnNames,
            unitQuantity = calc.unitValue.toLong(),
            estimatedTotal = estimatedTotal.toLong(),
            depositAmount = depositAmount.toLong(),
            notes = calc.notes.trim()
        )

        val offlineOrGuest = isGuestMode || repository == null || !_isDeviceOnline.value
        if (offlineOrGuest) {
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
            _snackbarMessage.value = if (!_isDeviceOnline.value) {
                "Offline • Lead captured & saved to local storage!"
            } else {
                "Lead captured & saved to Guest CRM!"
            }
        } else {
            _quoteState.update { it.copy(isSubmitting = true) }
            viewModelScope.launch {
                repository.submitLead(lead)
                    .onSuccess {
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
                    .onFailure { err ->
                        _quoteState.update { it.copy(isSubmitting = false) }
                        _snackbarMessage.value = err.localizedMessage ?: "Failed to submit booking"
                    }
            }
        }
    }

    fun deleteLead(leadId: String) {
        val offlineOrGuest = isGuestMode || repository == null || !_isDeviceOnline.value
        if (offlineOrGuest) {
            guestSessionManager.deleteGuestLead(leadId)
            _snackbarMessage.value = if (!_isDeviceOnline.value) {
                "Offline • Lead deleted from local storage"
            } else {
                "Lead deleted"
            }
        } else {
            viewModelScope.launch {
                repository.deleteLead(leadId)
                    .onSuccess { _snackbarMessage.value = "Lead deleted" }
                    .onFailure { err -> _snackbarMessage.value = err.localizedMessage ?: "Failed to delete lead" }
            }
        }
    }

    fun togglePlaybookTask(task: PlaybookTask) {
        val offlineOrGuest = isGuestMode || repository == null || !_isDeviceOnline.value
        if (offlineOrGuest) {
            guestSessionManager.toggleGuestPlaybookTask(task.id)
            if (!_isDeviceOnline.value) {
                _snackbarMessage.value = "Offline • Checklist progress saved to local storage"
            }
        } else {
            viewModelScope.launch {
                repository.togglePlaybookTask(task)
                    .onFailure { err ->
                        _snackbarMessage.value = err.localizedMessage ?: "Could not update checklist"
                    }
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

    private companion object {
        const val TAG = "VibePitchVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
