package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.LocalGuestSessionManager
import com.example.data.model.AddOnOption
import com.example.data.model.BusinessAccountProfile
import com.example.data.model.CapturedLead
import com.example.data.model.GuestLifecycleStatus
import com.example.data.model.NicheCatalog
import com.example.data.model.NicheKey
import com.example.data.model.PipelineStage
import com.example.data.model.PlaybookTask
import com.example.data.model.ProspectProfile
import com.example.data.model.RevenueStreamMode
import com.example.data.model.ServicePackageOption
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
import kotlin.math.roundToInt

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class AppTab(val route: String) {
    LIVE_DEMO("live_demo"),
    BRAND_STUDIO("brand_studio"),
    CRM_LEADS("crm_leads"),
    PLAYBOOK("playbook")
}

data class ActiveDemoBrandState(
    val prospectId: String = "seed_prospect_hvac",
    val businessName: String = "Apex Climate & HVAC",
    val contactName: String = "Mike Rivera",
    val niche: NicheKey = NicheKey.HOME_TRADES,
    val tagline: String = NicheKey.HOME_TRADES.defaultTagline,
    val brandColorHex: String = NicheKey.HOME_TRADES.defaultColorHex,
    val websiteGapNote: String = "Has a static 'Request a Quote' form with no instant ballpark estimator.",
    val depositPercent: Int = 20,
    val setupFeeOffer: Int = 250,
    val monthlyRetainerOffer: Int = 35
)

data class QuoteCalculatorState(
    val selectedServiceId: String = NicheCatalog.getServicePackages(NicheKey.HOME_TRADES).first().id,
    val unitValue: Int = NicheKey.HOME_TRADES.defaultUnitValue,
    val selectedAddOnIds: Set<String> = setOf("trades_thermostat"),
    val customerName: String = "",
    val customerPhone: String = "",
    val customerEmail: String = "",
    val preferredDate: String = "Tomorrow Morning (8am – 12pm)",
    val notes: String = "",
    val isSubmitting: Boolean = false,
    val latestSubmittedLead: CapturedLead? = null,
    val statusMessage: String? = null
)

enum class OfferArchetype(
    val title: String,
    val shortLabel: String,
    val revenueStream: RevenueStreamMode,
    val outcomeHook: String,
    val deliverableBullet: String
) {
    BOOKING_QUOTE_FORM(
        title = "Booking & Instant Quote Form",
        shortLabel = "Booking / Quote",
        revenueStream = RevenueStreamMode.LOCAL_BUSINESS,
        outcomeHook = "you currently take bookings and price inquiries by DM or phone tag",
        deliverableBullet = "Interactive mobile booking & instant ballpark quote page that saves you phone calls"
    ),
    CONTACT_PAYMENT_SITE(
        title = "Contact & Payment Links Hub",
        shortLabel = "Payment Hub",
        revenueStream = RevenueStreamMode.LOCAL_BUSINESS,
        outcomeHook = "customers have to ask where to pay deposits or find your service menu",
        deliverableBullet = "Clean mobile page with your service menu, contact intake, and trusted Stripe payment links"
    ),
    SPREADSHEET_REPLACEMENT(
        title = "Spreadsheet Replacement Tool",
        shortLabel = "Spreadsheet Tool",
        revenueStream = RevenueStreamMode.LOCAL_BUSINESS,
        outcomeHook = "quoting and tracking jobs on spreadsheets or paper notes slows down your team",
        deliverableBullet = "Simple custom calculator and job-intake tool that replaces manual spreadsheet math"
    ),
    UPWORK_MICRO_TOOL(
        title = "Shopify Tweak / Chrome Extension / Automation",
        shortLabel = "Upwork Micro-Tool",
        revenueStream = RevenueStreamMode.FREELANCE_MICRO_TOOLS,
        outcomeHook = "repetitive manual workflow steps and high-friction storefront pages are costing you conversions",
        deliverableBullet = "Focused landing page, Shopify conversion tweak, email auto-sorter, or Manifest V3 Chrome extension"
    ),
    AI_PROTOTYPE_RESCUE(
        title = "Broken AI App Security & Production Rescue",
        shortLabel = "AI App Rescue",
        revenueStream = RevenueStreamMode.PROTOTYPE_REMEDIATION,
        outcomeHook = "your AI-built prototype has login bugs, exposed API keys, or security gaps blocking launch",
        deliverableBullet = "End-to-end production audit, managed auth/Stripe handoff, security rules hardening, and viewport bug fixes"
    )
}

data class ScopeGuardrailConfig(
    val paymentGuardrail: String = "Stripe Payment Links / Native Checkout",
    val dataPrivacyGuardrail: String = "Google Forms / Zero-Trust Cloud Rules",
    val authGuardrail: String = "Managed Google OAuth (No Custom Passwords)",
    val supportWindowDays: Int = 14,
    val useExternalPaymentLinks: Boolean = true,
    val useExternalLeadForms: Boolean = true,
    val useManagedOAuth: Boolean = true
)

data class GeneratedPitchScripts(
    val igFacebookDmScript: String,
    val marketplaceProposalScript: String,
    val coldOutreachMessage: String,
    val loomTeleprompterScript: String,
    val deRiskedOfferSheet: String,
    val testimonialAndReferralScript: String
) {
    fun toList(): List<PitchScriptItem> = listOf(
        PitchScriptItem("IG / FB DM", igFacebookDmScript),
        PitchScriptItem("Upwork / Rescue", marketplaceProposalScript),
        PitchScriptItem("Email Pitch", coldOutreachMessage),
        PitchScriptItem("90s Loom", loomTeleprompterScript),
        PitchScriptItem("Scope & Security", deRiskedOfferSheet),
        PitchScriptItem("Testimonial & Referrals", testimonialAndReferralScript)
    )
}

data class PitchScriptItem(
    val title: String,
    val body: String
)

object PitchScriptGenerator {
    fun generateAllSixScripts(
        nicheKey: NicheKey,
        archetype: OfferArchetype,
        businessName: String,
        contactName: String,
        setupFee: Int,
        monthlyRetainer: Int,
        depositPercent: Int,
        guardrailConfig: ScopeGuardrailConfig = ScopeGuardrailConfig(),
        websiteGapNote: String = nicheKey.microProblemSummary,
        sampleTotal: Int = 2000,
        sampleDeposit: Int = (sampleTotal * depositPercent) / 100
    ): List<PitchScriptItem> {
        val contactGreeting = contactName.ifBlank { "there" }
        val clampedSetup = InputBoundsValidator.clampSetupFeeOffer(setupFee)
        val clampedRetainer = InputBoundsValidator.clampMonthlyRetainerOffer(monthlyRetainer)
        val clampedDeposit = InputBoundsValidator.clampDepositPercent(depositPercent)

        val igFacebookDm = """
Hey $contactGreeting! Saw $businessName on here—your recent ${nicheKey.shortBadge} work looks great.

I noticed ${archetype.outcomeHook}. I went ahead and built a working ${archetype.title} sample with $businessName's name and colors on it so customers can see an instant ~$$sampleTotal ballpark quote and request a time slot in 60 seconds (saves you back-and-forth DMs and phone calls).

I can set it up for $businessName for a flat $$clampedSetup—and you only pay after you've tested it on your phone and it's live.

Mind if I send the link or a 60-second screen video so you can tap around on it?
        """.trimIndent()

        val marketplaceProposal = """
UPWORK / FIVERR / RESCUE PROPOSAL — ${businessName.uppercase()}
────────────────────────────────────────────────────────
Hi $contactGreeting,

I read your brief carefully—you don't just need raw code, you need a reliable ${archetype.title} that works cleanly on both mobile and desktop without security headaches.

I already built a working interactive prototype tailored for $businessName (${nicheKey.displayName}):
1. Solves the exact bottleneck: $websiteGapNote
2. Built-in Security Guardrails: Uses ${guardrailConfig.paymentGuardrail} and ${guardrailConfig.dataPrivacyGuardrail} so you never have to worry about exposed customer data or broken auth.
3. Fixed-Price Milestone: $$clampedSetup flat, delivered in 48 hours, including ${guardrailConfig.supportWindowDays} days of post-delivery bug fixes.

I recorded a 60-second walkthrough of your working sample—happy to share the link right now so you can test it on your phone before hiring!
        """.trimIndent()

        val loomScript = """
[0:00–0:20 • The Specific Observation]
"Hey $contactGreeting — I saw your work at $businessName and your reviews look awesome. I noticed that ${archetype.outcomeHook}."

[0:20–0:55 • The Outcome Demo (Not The Code)]
"Clients pay for outcomes, not complicated software. So I mocked up a working ${archetype.title} branded for $businessName. Watch how a user picks their ${nicheKey.shortBadge} option, slides their scope, and immediately sees a ~$$sampleTotal estimate with a $$sampleDeposit ($clampedDeposit%) deposit link via ${guardrailConfig.paymentGuardrail}."

[0:55–1:30 • The Fixed $$clampedSetup Starter Offer]
"If you want this live this week, I’ll connect it to your phone/email for a flat $$clampedSetup one-time fee—and include ${guardrailConfig.supportWindowDays} days of free post-delivery adjustments. Zero risk: you only pay after you test every button on your phone!"
        """.trimIndent()

        val outreachMsg = """
Subject: Simple ${archetype.title.lowercase()} for $businessName

Hey $contactGreeting,

Saw $businessName—great reputation. I noticed ${archetype.outcomeHook}, which usually means answering the same price and availability questions all evening.

I built a working ${archetype.title} sample branded for $businessName:
• ${archetype.deliverableBullet}
• Instant ~$$sampleTotal estimate + $$sampleDeposit ($clampedDeposit%) deposit option via ${guardrailConfig.paymentGuardrail}
• Routes leads via ${guardrailConfig.dataPrivacyGuardrail} straight to your phone/email

I charge a small fixed price ($$clampedSetup flat) to customize and launch it for you, payable only after it's live and tested on phone and desktop.

Want me to send over the sample link so you can try it?
        """.trimIndent()

        val offerSheet = """
PLAIN-ENGLISH SCOPE & SECURITY AGREEMENT — ${businessName.uppercase()}
────────────────────────────────────────────────────────
• Revenue Stream: ${nicheKey.revenueStream.title}
• Selected Deliverable: ${archetype.title}
• Operational Pain Point Solved: $websiteGapNote
• Fixed Project Price: $$clampedSetup flat ($0 upfront — payable ONLY after live phone & desktop testing)
• Optional Care Plan: $$clampedRetainer/mo (Hosting, price tweaks & ongoing updates)

WHAT THIS TOOL DOES (IN-SCOPE):
✓ ${archetype.deliverableBullet}
✓ Tested end-to-end across mobile phone and desktop viewports
✓ Offloads payments to: ${guardrailConfig.paymentGuardrail}
✓ Offloads data & intake to: ${guardrailConfig.dataPrivacyGuardrail}
✓ Authentication boundary: ${guardrailConfig.authGuardrail}

WHAT THIS TOOL DOES NOT DO (SAFETY & SCOPE BOUNDARIES):
✗ Never processes or stores raw credit card numbers, plain-text passwords, or unencrypted PII on custom code
✗ Does not replace full ERP, tax, or payroll accounting software

POST-DELIVERY MAINTENANCE AGREEMENT:
✓ Includes ${guardrailConfig.supportWindowDays} calendar days of free bug fixes and copy/price tweaks after delivery
✓ Any new features outside this scope are quoted as separate fixed-price mini-projects ($100–$300)
        """.trimIndent()

        val testimonialAndReferral = """
POST-DELIVERY TESTIMONIAL & REFERRAL REQUEST — ${businessName.uppercase()}
────────────────────────────────────────────────────────
Hey $contactGreeting! Awesome seeing your new ${archetype.title} live and tested for $businessName.

As a reminder, you have ${guardrailConfig.supportWindowDays} days of free tweaks and bug support included—just text me if you want any prices or wording adjusted.

Quick favor while everything is fresh:
1. Testimonial: Could you reply with 1–2 sentences on how this saves you time (e.g., fewer back-and-forth DMs/phone calls) that I can quote in my portfolio?
2. Local Referral: Do you know 1 or 2 other local business owners (cleaners, salons, gyms, or trades) who are still taking bookings by DM or spreadsheet? If you introduce us, I'll take $50 off their setup or give you 2 free months of maintenance!
        """.trimIndent()

        return GeneratedPitchScripts(
            igFacebookDmScript = igFacebookDm,
            marketplaceProposalScript = marketplaceProposal,
            coldOutreachMessage = outreachMsg,
            loomTeleprompterScript = loomScript,
            deRiskedOfferSheet = offerSheet,
            testimonialAndReferralScript = testimonialAndReferral
        ).toList()
    }
}

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

    fun dismissDay20Prompt() {
        guestSessionManager.dismissDay20PromptForToday()
    }

    fun stageBusinessProfileForGuestUpgrade(profile: BusinessAccountProfile) {
        guestSessionManager.savePendingBusinessUpgradeDraft(profile)
    }

    fun savePermanentBusinessProfileDirectly(profile: BusinessAccountProfile) {
        val repo = repository ?: return
        viewModelScope.launch {
            repo.saveBusinessProfile(profile.copy(userId = currentUserId))
                .onSuccess {
                    _showBusinessUpgradeModal.value = false
                    _snackbarMessage.value = "Business Account Profile saved!"
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
        if (isGuestMode || repository == null) {
            val newId = guestSessionManager.addOrUpdateGuestProspect(prospect)
            _activeBrand.update { it.copy(prospectId = newId) }
            _selectedTab.value = AppTab.LIVE_DEMO
            _snackbarMessage.value = "Saved ${normalized.businessName} to Guest CRM & updated Demo"
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
        if (isGuestMode || repository == null) {
            guestSessionManager.addOrUpdateGuestProspect(prospect)
            _snackbarMessage.value = "Added ${normalized.businessName} to pipeline"
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
        if (isGuestMode || repository == null) {
            guestSessionManager.addOrUpdateGuestProspect(updated)
            _snackbarMessage.value = "${prospect.businessName} moved to ${next.label}"
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
        if (isGuestMode || repository == null) {
            guestSessionManager.deleteGuestProspect(prospectId)
            _snackbarMessage.value = "Prospect removed"
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

    fun calculateCurrentQuote(): Pair<Int, Int> {
        val brand = _activeBrand.value
        val calc = _quoteState.value
        val packages = NicheCatalog.getServicePackages(brand.niche)
        val selectedPkg: ServicePackageOption = packages.firstOrNull { it.id == calc.selectedServiceId }
            ?: packages.first()
        val addOns: List<AddOnOption> = NicheCatalog.getAddOns(brand.niche)
            .filter { calc.selectedAddOnIds.contains(it.id) }

        val unitSubtotal = selectedPkg.perUnitRate * calc.unitValue
        val addOnTotal = addOns.sumOf { it.price }
        val estimatedTotal = selectedPkg.basePrice + unitSubtotal + addOnTotal
        val depositAmount = ((estimatedTotal * brand.depositPercent) / 100.0).roundToInt()
        return estimatedTotal to depositAmount
    }

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

        if (isGuestMode || repository == null) {
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
            _snackbarMessage.value = "Lead captured & saved to Guest CRM!"
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
        if (isGuestMode || repository == null) {
            guestSessionManager.deleteGuestLead(leadId)
            _snackbarMessage.value = "Lead deleted"
        } else {
            viewModelScope.launch {
                repository.deleteLead(leadId)
                    .onSuccess { _snackbarMessage.value = "Lead deleted" }
                    .onFailure { err -> _snackbarMessage.value = err.localizedMessage ?: "Failed to delete lead" }
            }
        }
    }

    fun togglePlaybookTask(task: PlaybookTask) {
        if (isGuestMode || repository == null) {
            guestSessionManager.toggleGuestPlaybookTask(task.id)
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
        val contactGreeting = brand.contactName.ifBlank { "there" }
        val (sampleTotal, sampleDeposit) = calculateCurrentQuote()

        val igFacebookDm = """
Hey $contactGreeting! Saw ${brand.businessName} on here—your recent ${brand.niche.shortBadge} work looks great.

I noticed ${archetype.outcomeHook}. I went ahead and built a working ${archetype.title} sample with ${brand.businessName}'s name and colors on it so customers can see an instant ~$${sampleTotal} ballpark quote and request a time slot in 60 seconds (saves you back-and-forth DMs and phone calls).

I can set it up for ${brand.businessName} for a flat $${brand.setupFeeOffer}—and you only pay after you've tested it on your phone and it's live.

Mind if I send the link or a 60-second screen video so you can tap around on it?
        """.trimIndent()

        val marketplaceProposal = """
UPWORK / FIVERR / RESCUE PROPOSAL — ${brand.businessName.uppercase()}
────────────────────────────────────────────────────────
Hi $contactGreeting,

I read your brief carefully—you don't just need raw code, you need a reliable ${archetype.title} that works cleanly on both mobile and desktop without security headaches.

I already built a working interactive prototype tailored for ${brand.businessName} (${brand.niche.displayName}):
1. Solves the exact bottleneck: ${brand.websiteGapNote}
2. Built-in Security Guardrails: Uses ${guardrails.paymentGuardrail} and ${guardrails.dataPrivacyGuardrail} so you never have to worry about exposed customer data or broken auth.
3. Fixed-Price Milestone: $${brand.setupFeeOffer} flat, delivered in 48 hours, including ${guardrails.supportWindowDays} days of post-delivery bug fixes.

I recorded a 60-second walkthrough of your working sample—happy to share the link right now so you can test it on your phone before hiring!
        """.trimIndent()

        val loomScript = """
[0:00–0:20 • The Specific Observation]
"Hey $contactGreeting — I saw your work at ${brand.businessName} and your reviews look awesome. I noticed that ${archetype.outcomeHook}."

[0:20–0:55 • The Outcome Demo (Not The Code)]
"Clients pay for outcomes, not complicated software. So I mocked up a working ${archetype.title} branded for ${brand.businessName}. Watch how a user picks their ${brand.niche.shortBadge} option, slides their scope, and immediately sees a ~$${sampleTotal} estimate with a $${sampleDeposit} (${brand.depositPercent}%) deposit link via ${guardrails.paymentGuardrail}."

[0:55–1:30 • The Fixed $${brand.setupFeeOffer} Starter Offer]
"If you want this live this week, I’ll connect it to your phone/email for a flat $${brand.setupFeeOffer} one-time fee—and include ${guardrails.supportWindowDays} days of free post-delivery adjustments. Zero risk: you only pay after you test every button on your phone!"
        """.trimIndent()

        val outreachMsg = """
Subject: Simple ${archetype.title.lowercase()} for ${brand.businessName}

Hey $contactGreeting,

Saw ${brand.businessName}—great reputation. I noticed ${archetype.outcomeHook}, which usually means answering the same price and availability questions all evening.

I built a working ${archetype.title} sample branded for ${brand.businessName}:
• ${archetype.deliverableBullet}
• Instant ~$${sampleTotal} estimate + $${sampleDeposit} (${brand.depositPercent}%) deposit option via ${guardrails.paymentGuardrail}
• Routes leads via ${guardrails.dataPrivacyGuardrail} straight to your phone/email

I charge a small fixed price ($${brand.setupFeeOffer} flat) to customize and launch it for you, payable only after it's live and tested on phone and desktop.

Want me to send over the sample link so you can try it?
        """.trimIndent()

        val offerSheet = """
PLAIN-ENGLISH SCOPE & SECURITY AGREEMENT — ${brand.businessName.uppercase()}
────────────────────────────────────────────────────────
• Revenue Stream: ${brand.niche.revenueStream.title}
• Selected Deliverable: ${archetype.title}
• Operational Pain Point Solved: ${brand.websiteGapNote}
• Fixed Project Price: $${brand.setupFeeOffer} flat ($0 upfront — payable ONLY after live phone & desktop testing)
• Optional Care Plan: $${brand.monthlyRetainerOffer}/mo (Hosting, price tweaks & ongoing updates)

WHAT THIS TOOL DOES (IN-SCOPE):
✓ ${archetype.deliverableBullet}
✓ Tested end-to-end across mobile phone and desktop viewports
✓ Offloads payments to: ${guardrails.paymentGuardrail}
✓ Offloads data & intake to: ${guardrails.dataPrivacyGuardrail}
✓ Authentication boundary: ${guardrails.authGuardrail}

WHAT THIS TOOL DOES NOT DO (SAFETY & SCOPE BOUNDARIES):
✗ Never processes or stores raw credit card numbers, plain-text passwords, or unencrypted PII on custom code
✗ Does not replace full ERP, tax, or payroll accounting software

POST-DELIVERY MAINTENANCE AGREEMENT:
✓ Includes ${guardrails.supportWindowDays} calendar days of free bug fixes and copy/price tweaks after delivery
✓ Any new features outside this scope are quoted as separate fixed-price mini-projects ($100–$300)
        """.trimIndent()

        val testimonialAndReferral = """
POST-DELIVERY TESTIMONIAL & REFERRAL REQUEST — ${brand.businessName.uppercase()}
────────────────────────────────────────────────────────
Hey $contactGreeting! Awesome seeing your new ${archetype.title} live and tested for ${brand.businessName}.

As a reminder, you have ${guardrails.supportWindowDays} days of free tweaks and bug support included—just text me if you want any prices or wording adjusted.

Quick favor while everything is fresh:
1. Testimonial: Could you reply with 1–2 sentences on how this saves you time (e.g., fewer back-and-forth DMs/phone calls) that I can quote in my portfolio?
2. Local Referral: Do you know 1 or 2 other local business owners (cleaners, salons, gyms, or trades) who are still taking bookings by DM or spreadsheet? If you introduce us, I'll take $50 off their setup or give you 2 free months of maintenance!
        """.trimIndent()

        return GeneratedPitchScripts(
            igFacebookDmScript = igFacebookDm,
            marketplaceProposalScript = marketplaceProposal,
            coldOutreachMessage = outreachMsg,
            loomTeleprompterScript = loomScript,
            deRiskedOfferSheet = offerSheet,
            testimonialAndReferralScript = testimonialAndReferral
        )
    }

    private companion object {
        const val TAG = "VibePitchVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
