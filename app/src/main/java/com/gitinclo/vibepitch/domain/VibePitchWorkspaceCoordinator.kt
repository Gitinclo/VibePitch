package com.gitinclo.vibepitch.domain

import com.gitinclo.vibepitch.data.local.LocalGuestSessionManager
import com.gitinclo.vibepitch.data.model.BusinessAccountProfile
import com.gitinclo.vibepitch.data.model.CapturedLead
import com.gitinclo.vibepitch.data.model.NicheCatalog
import com.gitinclo.vibepitch.data.model.NicheKey
import com.gitinclo.vibepitch.data.model.PipelineStage
import com.gitinclo.vibepitch.data.model.PlaybookTask
import com.gitinclo.vibepitch.data.model.ProspectProfile
import com.gitinclo.vibepitch.data.repository.VibePitchRepository
import com.gitinclo.vibepitch.ui.ActiveDemoBrandState
import com.gitinclo.vibepitch.ui.QuoteCalculatorState

/**
 * Domain coordinator encapsulating Guest-to-Cloud migration, offline-first persistence routing,
 * and schema-validated prospect/lead creation so VibePitchViewModel remains a focused UI state holder.
 */
class VibePitchWorkspaceCoordinator(
    private val repository: VibePitchRepository?,
    private val guestSessionManager: LocalGuestSessionManager,
    private val currentUserId: String,
    private val isGuestMode: Boolean
) {

    fun isOfflineOrGuest(isDeviceOnline: Boolean): Boolean =
        isGuestMode || repository == null || !isDeviceOnline

    suspend fun syncGuestUpgradeOrSeedStarterData(): String? {
        if (isGuestMode || repository == null) return null
        val pendingProfile = guestSessionManager.consumePendingBusinessUpgradeDraft(currentUserId)
        return if (guestSessionManager.hasGuestDataToMigrate() || pendingProfile != null) {
            val result = repository.migrateGuestDataToCloud(
                userId = currentUserId,
                profile = pendingProfile,
                guestProspects = guestSessionManager.guestProspects.value,
                guestLeads = guestSessionManager.guestLeads.value,
                guestPlaybook = guestSessionManager.guestPlaybook.value
            )
            if (result.isSuccess) {
                guestSessionManager.markGuestMigratedAndDeactivate()
                "Permanent Business Account activated & guest data synced!"
            } else {
                repository.seedStarterDataIfEmpty(currentUserId)
                null
            }
        } else {
            repository.seedStarterDataIfEmpty(currentUserId)
            null
        }
    }

    fun buildProspectFromBrand(
        brand: ActiveDemoBrandState,
        stage: PipelineStage = PipelineStage.DEMO_READY
    ): ProspectProfile {
        val normalized = InputBoundsValidator.normalizeProspectInput(
            businessName = brand.businessName,
            contactName = brand.contactName.ifBlank { "Owner" },
            websiteGapNote = brand.websiteGapNote.ifBlank { brand.niche.microProblemSummary },
            tagline = brand.tagline.ifBlank { brand.niche.defaultTagline },
            depositPercent = brand.depositPercent,
            setupFeeOffer = brand.setupFeeOffer,
            monthlyRetainerOffer = brand.monthlyRetainerOffer
        )
        return ProspectProfile(
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
            stage = stage.key
        )
    }

    fun buildNewProspect(
        businessName: String,
        contactName: String,
        niche: NicheKey,
        websiteGapNote: String,
        brandColorHex: String
    ): ProspectProfile {
        val normalized = InputBoundsValidator.normalizeProspectInput(
            businessName = businessName,
            contactName = contactName.ifBlank { "Owner" },
            websiteGapNote = websiteGapNote.ifBlank { niche.microProblemSummary },
            tagline = niche.defaultTagline,
            depositPercent = 20,
            setupFeeOffer = 250,
            monthlyRetainerOffer = 35
        )
        return ProspectProfile(
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
    }

    fun buildCapturedLead(
        brand: ActiveDemoBrandState,
        calc: QuoteCalculatorState,
        estimatedTotal: Int,
        depositAmount: Int
    ): CapturedLead {
        val packages = NicheCatalog.getServicePackages(brand.niche)
        val selectedPkg = packages.firstOrNull { it.id == calc.selectedServiceId } ?: packages.first()
        val selectedAddOnNames = NicheCatalog.getAddOns(brand.niche)
            .filter { calc.selectedAddOnIds.contains(it.id) }
            .joinToString(", ") { it.name }

        return CapturedLead(
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
    }

    suspend fun saveBusinessProfile(
        profile: BusinessAccountProfile,
        isDeviceOnline: Boolean
    ): Result<String> {
        if (isOfflineOrGuest(isDeviceOnline) || repository == null) {
            guestSessionManager.savePendingBusinessUpgradeDraft(profile)
            return Result.success("Offline • Business profile saved to local storage")
        }
        return repository.saveBusinessProfile(profile.copy(userId = currentUserId))
            .map { "Business Account Profile saved to Firestore!" }
    }

    suspend fun persistProspect(
        prospect: ProspectProfile,
        isDeviceOnline: Boolean
    ): Result<String> {
        if (isOfflineOrGuest(isDeviceOnline) || repository == null) {
            val localId = guestSessionManager.addOrUpdateGuestProspect(prospect)
            return Result.success(localId)
        }
        return repository.createProspect(prospect)
    }

    suspend fun advanceProspectStage(
        prospect: ProspectProfile,
        isDeviceOnline: Boolean
    ): Result<PipelineStage> {
        val next = PipelineStage.fromKey(prospect.stage).nextStage()
        val updated = prospect.copy(stage = next.key)
        if (isOfflineOrGuest(isDeviceOnline) || repository == null) {
            guestSessionManager.addOrUpdateGuestProspect(updated)
            return Result.success(next)
        }
        return repository.updateProspect(updated).map { next }
    }

    suspend fun deleteProspect(
        prospectId: String,
        isDeviceOnline: Boolean
    ): Result<Unit> {
        if (isOfflineOrGuest(isDeviceOnline) || repository == null) {
            guestSessionManager.deleteGuestProspect(prospectId)
            return Result.success(Unit)
        }
        return repository.deleteProspect(prospectId)
    }

    suspend fun deleteLead(
        leadId: String,
        isDeviceOnline: Boolean
    ): Result<Unit> {
        if (isOfflineOrGuest(isDeviceOnline) || repository == null) {
            guestSessionManager.deleteGuestLead(leadId)
            return Result.success(Unit)
        }
        return repository.deleteLead(leadId)
    }

    suspend fun togglePlaybookTask(
        task: PlaybookTask,
        isDeviceOnline: Boolean
    ): Result<Unit> {
        if (isOfflineOrGuest(isDeviceOnline) || repository == null) {
            guestSessionManager.toggleGuestPlaybookTask(task.id)
            return Result.success(Unit)
        }
        return repository.togglePlaybookTask(task)
    }
}
