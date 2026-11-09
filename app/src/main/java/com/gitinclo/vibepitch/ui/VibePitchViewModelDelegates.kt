package com.gitinclo.vibepitch.ui

import android.util.Log
import com.gitinclo.vibepitch.data.model.BusinessAccountProfile
import com.gitinclo.vibepitch.data.model.NicheCatalog
import com.gitinclo.vibepitch.data.model.NicheKey
import com.gitinclo.vibepitch.data.model.ProspectProfile
import com.gitinclo.vibepitch.data.model.RevenueStreamMode
import com.gitinclo.vibepitch.data.repository.VibePitchRepository
import com.gitinclo.vibepitch.domain.InputBoundsValidator
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal object VibePitchStateTransitions {

    private const val TAG = "VibePitchVM"
    private const val STOP_TIMEOUT_MILLIS = 5000L

    fun observeBusinessProfileState(
        scope: CoroutineScope,
        repository: VibePitchRepository?,
        currentUserId: String,
        isGuestMode: Boolean
    ): StateFlow<BusinessAccountProfile?> {
        return if (!isGuestMode && repository != null) {
            repository.observeBusinessProfile(currentUserId)
                .catch { error ->
                    val firestoreError = error.unwrapFirestoreException()
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w(TAG, "Error observing business profile", firestoreError ?: error)
                    emit(null)
                }
                .stateIn(
                    scope = scope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = null
                )
        } else {
            flowOf(null).stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = null
            )
        }
    }

    fun <T> observeCollectionState(
        scope: CoroutineScope,
        isGuestMode: Boolean,
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
                    scope = scope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = UiState.Loading
                )
        } else {
            guestFlow
                .map<List<T>, UiState<List<T>>> { UiState.Success(it) }
                .stateIn(
                    scope = scope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = UiState.Success(guestFlow.value)
                )
        }
    }

    fun buildBrandAndQuoteForNichePreset(
        currentBrand: ActiveDemoBrandState,
        currentQuote: QuoteCalculatorState,
        niche: NicheKey
    ): Triple<OfferArchetype, ActiveDemoBrandState, QuoteCalculatorState> {
        val packages = NicheCatalog.getServicePackages(niche)
        val firstPackage = packages.firstOrNull { it.popularBadge } ?: packages.first()
        val firstAddOn = NicheCatalog.getAddOns(niche).firstOrNull()?.id

        val archetype = when (niche.revenueStream) {
            RevenueStreamMode.LOCAL_BUSINESS -> OfferArchetype.BOOKING_QUOTE_FORM
            RevenueStreamMode.FREELANCE_MICRO_TOOLS -> OfferArchetype.UPWORK_MICRO_TOOL
            RevenueStreamMode.PROTOTYPE_REMEDIATION -> OfferArchetype.AI_PROTOTYPE_RESCUE
        }

        val updatedBrand = currentBrand.copy(
            niche = niche,
            businessName = niche.defaultBusinessName,
            contactName = niche.defaultContactName,
            tagline = niche.defaultTagline,
            brandColorHex = niche.defaultColorHex,
            websiteGapNote = niche.microProblemSummary
        )
        val updatedQuote = currentQuote.copy(
            selectedServiceId = firstPackage.id,
            unitValue = niche.defaultUnitValue,
            selectedAddOnIds = setOfNotNull(firstAddOn)
        )
        return Triple(archetype, updatedBrand, updatedQuote)
    }

    fun applyActiveBrandDraft(
        currentBrand: ActiveDemoBrandState,
        currentQuote: QuoteCalculatorState,
        businessName: String,
        contactName: String,
        niche: NicheKey,
        tagline: String,
        brandColorHex: String,
        websiteGapNote: String,
        depositPercent: Int,
        setupFeeOffer: Int,
        monthlyRetainerOffer: Int
    ): Pair<ActiveDemoBrandState, QuoteCalculatorState> {
        val nicheChanged = niche != currentBrand.niche
        val updatedBrand = currentBrand.copy(
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
        val updatedQuote = if (nicheChanged) {
            val packages = NicheCatalog.getServicePackages(niche)
            val defaultPkg = packages.firstOrNull { it.popularBadge } ?: packages.first()
            currentQuote.copy(
                selectedServiceId = defaultPkg.id,
                unitValue = niche.defaultUnitValue,
                selectedAddOnIds = emptySet()
            )
        } else {
            currentQuote
        }
        return updatedBrand to updatedQuote
    }

    fun buildBrandAndQuoteFromProspect(
        currentQuote: QuoteCalculatorState,
        prospect: ProspectProfile
    ): Pair<ActiveDemoBrandState, QuoteCalculatorState> {
        val niche = NicheKey.fromKey(prospect.nicheKey)
        val packages = NicheCatalog.getServicePackages(niche)
        val defaultPkg = packages.firstOrNull { it.popularBadge } ?: packages.first()

        val brand = ActiveDemoBrandState(
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
        val quote = currentQuote.copy(
            selectedServiceId = defaultPkg.id,
            unitValue = niche.defaultUnitValue,
            selectedAddOnIds = emptySet()
        )
        return brand to quote
    }

    @Volatile
    private var cachedScriptKey: Int = 0
    @Volatile
    private var cachedPitchScripts: GeneratedPitchScripts? = null

    fun buildOrGetPitchScripts(
        brand: ActiveDemoBrandState,
        archetype: OfferArchetype,
        guardrails: ScopeGuardrailConfig,
        sampleTotal: Int,
        sampleDeposit: Int
    ): GeneratedPitchScripts {
        val key = arrayOf(brand, archetype, guardrails, sampleTotal, sampleDeposit).contentHashCode()
        val existing = cachedPitchScripts
        if (existing != null && cachedScriptKey == key) {
            return existing
        }
        val generated = PitchScriptGenerator.buildPitchScripts(
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
        cachedScriptKey = key
        cachedPitchScripts = generated
        return generated
    }

    fun clearPitchScriptCache() {
        cachedScriptKey = 0
        cachedPitchScripts = null
    }

    private fun Throwable.unwrapFirestoreException(): FirebaseFirestoreException? =
        generateSequence(this) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
}
