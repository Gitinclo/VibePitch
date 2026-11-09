package com.gitinclo.vibepitch.data.local

import android.content.Context
import android.content.SharedPreferences
import com.gitinclo.vibepitch.data.model.BusinessAccountProfile
import com.gitinclo.vibepitch.data.model.CapturedLead
import com.gitinclo.vibepitch.data.model.GuestLifecycleStatus
import com.gitinclo.vibepitch.data.model.NicheCatalog
import com.gitinclo.vibepitch.data.model.NicheKey
import com.gitinclo.vibepitch.data.model.PipelineStage
import com.gitinclo.vibepitch.data.model.PlaybookTask
import com.gitinclo.vibepitch.data.model.ProspectProfile
import com.gitinclo.vibepitch.domain.InputBoundsValidator
import com.gitinclo.vibepitch.performance.DimensityCpuScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class LocalGuestSessionManager(context: Context) {

    private val ufsIoScope = CoroutineScope(SupervisorJob() + DimensityCpuScheduler.ufsSequentialIoDispatcher)

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _guestStatus = MutableStateFlow(computeStatus())
    val guestStatus: StateFlow<GuestLifecycleStatus> = _guestStatus.asStateFlow()

    private val _guestProspects = MutableStateFlow(loadProspects())
    val guestProspects: StateFlow<List<ProspectProfile>> = _guestProspects.asStateFlow()

    private val _guestLeads = MutableStateFlow(loadLeads())
    val guestLeads: StateFlow<List<CapturedLead>> = _guestLeads.asStateFlow()

    private val _guestPlaybook = MutableStateFlow(loadPlaybookTasks())
    val guestPlaybook: StateFlow<List<PlaybookTask>> = _guestPlaybook.asStateFlow()

    fun startOrResumeGuestSession(nowMillis: Long = System.currentTimeMillis()): GuestLifecycleStatus {
        val existingStart = prefs.getLong(KEY_GUEST_STARTED_AT, 0L)
        val startMillis = if (existingStart > 0L) existingStart else nowMillis
        prefs.edit()
            .putBoolean(KEY_GUEST_ACTIVE, true)
            .putLong(KEY_GUEST_STARTED_AT, startMillis)
            .apply()

        if (!prefs.contains(KEY_GUEST_PROSPECTS)) {
            saveProspects(NicheCatalog.buildDefaultProspects(GUEST_USER_ID))
        }
        if (!prefs.contains(KEY_GUEST_PLAYBOOK)) {
            savePlaybookTasks(NicheCatalog.buildDefaultPlaybookTasks(GUEST_USER_ID))
        }

        val updated = computeStatus(nowMillis)
        _guestStatus.value = updated
        return updated
    }

    fun clearAllGuestData() {
        prefs.edit().clear().apply()
        saveProspects(NicheCatalog.buildDefaultProspects(GUEST_USER_ID))
        saveLeads(emptyList())
        savePlaybookTasks(NicheCatalog.buildDefaultPlaybookTasks(GUEST_USER_ID))
        _guestStatus.value = computeStatus()
    }

    fun evaluateLifecycleAt(
        installEpochMillis: Long,
        currentEpochMillis: Long,
        dismissedDay: Int = -1
    ): GuestLifecycleStatus {
        return calculateGuestLifecycle(
            isGuestActive = true,
            startedAtMillis = installEpochMillis,
            nowMillis = currentEpochMillis,
            dismissedDay = dismissedDay
        )
    }

    fun exitGuestSession() {
        prefs.edit()
            .putBoolean(KEY_GUEST_ACTIVE, false)
            .apply()
        _guestStatus.value = computeStatus()
    }

    fun refreshStatus(nowMillis: Long = System.currentTimeMillis()): GuestLifecycleStatus {
        val status = computeStatus(nowMillis)
        _guestStatus.value = status
        return status
    }

    fun dismissDay20PromptForToday(nowMillis: Long = System.currentTimeMillis()) {
        val current = computeStatus(nowMillis)
        prefs.edit()
            .putInt(KEY_DAY20_DISMISSED_ON_DAY, current.currentDayNumber)
            .apply()
        _guestStatus.value = computeStatus(nowMillis)
    }

    fun savePendingBusinessUpgradeDraft(profile: BusinessAccountProfile) {
        prefs.edit()
            .putString(KEY_PENDING_BUSINESS_PROFILE, GuestSessionJsonCodec.encodeBusinessProfile(profile))
            .apply()
    }

    fun savePendingBusinessProfile(
        agencyName: String,
        tagline: String,
        primaryNiche: NicheKey,
        ownerName: String = "Owner",
        businessEmail: String = "",
        businessPhone: String = "",
        websiteUrl: String = ""
    ) {
        savePendingBusinessUpgradeDraft(
            BusinessAccountProfile(
                id = "profile",
                userId = GUEST_USER_ID,
                agencyName = agencyName,
                ownerName = ownerName,
                businessEmail = businessEmail,
                businessPhone = businessPhone,
                primaryNicheKey = primaryNiche.key,
                websiteUrl = websiteUrl,
                tagline = tagline,
                upgradedFromGuest = true
            )
        )
    }

    fun getPendingBusinessProfile(userId: String = GUEST_USER_ID): BusinessAccountProfile? {
        val raw = prefs.getString(KEY_PENDING_BUSINESS_PROFILE, null)
        return GuestSessionJsonCodec.decodeBusinessProfile(raw, userId)
    }

    fun consumePendingBusinessUpgradeDraft(userId: String): BusinessAccountProfile? {
        return getPendingBusinessProfile(userId)
    }

    fun hasGuestDataToMigrate(): Boolean {
        return prefs.getLong(KEY_GUEST_STARTED_AT, 0L) > 0L &&
            !prefs.getBoolean(KEY_GUEST_MIGRATED_TO_CLOUD, false)
    }

    fun markGuestMigratedAndDeactivate() {
        prefs.edit()
            .putBoolean(KEY_GUEST_ACTIVE, false)
            .putBoolean(KEY_GUEST_MIGRATED_TO_CLOUD, true)
            .remove(KEY_PENDING_BUSINESS_PROFILE)
            .apply()
        _guestStatus.value = computeStatus()
    }

    fun addOrUpdateGuestProspect(prospect: ProspectProfile): String {
        val current = _guestProspects.value.toMutableList()
        val resolvedId = prospect.id.ifBlank { "guest_prospect_${UUID.randomUUID()}" }
        val normalized = InputBoundsValidator.normalizeProspectInput(
            businessName = prospect.businessName,
            contactName = prospect.contactName,
            websiteGapNote = prospect.websiteGapNote,
            tagline = prospect.tagline,
            depositPercent = prospect.depositPercent.toInt(),
            setupFeeOffer = prospect.setupFeeOffer.toInt(),
            monthlyRetainerOffer = prospect.monthlyRetainerOffer.toInt()
        )
        val updatedProspect = prospect.copy(
            id = resolvedId,
            userId = GUEST_USER_ID,
            businessName = normalized.businessName,
            contactName = normalized.contactName,
            websiteGapNote = normalized.websiteGapNote,
            tagline = normalized.tagline,
            depositPercent = normalized.depositPercent.toLong(),
            setupFeeOffer = normalized.setupFeeOffer.toLong(),
            monthlyRetainerOffer = normalized.monthlyRetainerOffer.toLong()
        )
        val index = current.indexOfFirst { it.id == resolvedId }
        if (index >= 0) {
            current[index] = updatedProspect
        } else {
            current.add(0, updatedProspect)
        }
        saveProspects(current)
        return resolvedId
    }

    fun addProspect(
        businessName: String,
        contactName: String,
        nicheKey: NicheKey,
        websiteGapNote: String,
        setupFeeOffer: Int,
        monthlyRetainerOffer: Int,
        depositPercent: Int
    ): ProspectProfile {
        val id = addOrUpdateGuestProspect(
            ProspectProfile(
                userId = GUEST_USER_ID,
                businessName = businessName,
                contactName = contactName,
                nicheKey = nicheKey.key,
                websiteGapNote = websiteGapNote,
                brandColorHex = nicheKey.defaultColorHex,
                tagline = nicheKey.defaultTagline,
                depositPercent = depositPercent.toLong(),
                setupFeeOffer = setupFeeOffer.toLong(),
                monthlyRetainerOffer = monthlyRetainerOffer.toLong(),
                stage = PipelineStage.AUDITED.key
            )
        )
        return _guestProspects.value.first { it.id == id }
    }

    fun advanceProspectStage(prospectId: String): ProspectProfile? {
        val existing = _guestProspects.value.firstOrNull { it.id == prospectId } ?: return null
        val nextStage = PipelineStage.fromKey(existing.stage).nextStage()
        val updated = existing.copy(stage = nextStage.key)
        addOrUpdateGuestProspect(updated)
        return _guestProspects.value.firstOrNull { it.id == prospectId }
    }

    fun getProspects(): List<ProspectProfile> = _guestProspects.value

    fun deleteGuestProspect(prospectId: String) {
        saveProspects(_guestProspects.value.filterNot { it.id == prospectId })
    }

    fun deleteProspect(prospectId: String) = deleteGuestProspect(prospectId)

    fun addGuestLead(lead: CapturedLead): String {
        val current = _guestLeads.value.toMutableList()
        val resolvedId = lead.id.ifBlank { "guest_lead_${UUID.randomUUID()}" }
        val updatedLead = lead.copy(
            id = resolvedId,
            userId = GUEST_USER_ID,
            prospectBusinessName = InputBoundsValidator.sanitizeBusinessName(lead.prospectBusinessName)
                .ifEmpty { "Local Demo Business" },
            customerName = InputBoundsValidator.sanitizeContactName(lead.customerName),
            notes = InputBoundsValidator.sanitizeNotes(lead.notes)
        )
        current.add(0, updatedLead)
        saveLeads(current)
        return resolvedId
    }

    fun captureLead(
        businessName: String,
        contactName: String,
        nicheKey: NicheKey,
        notes: String
    ): CapturedLead {
        val id = addGuestLead(
            CapturedLead(
                userId = GUEST_USER_ID,
                prospectBusinessName = businessName,
                nicheKey = nicheKey.key,
                customerName = contactName,
                notes = notes
            )
        )
        return _guestLeads.value.first { it.id == id }
    }

    fun deleteGuestLead(leadId: String) {
        saveLeads(_guestLeads.value.filterNot { it.id == leadId })
    }

    fun toggleGuestPlaybookTask(taskId: String) {
        val current = _guestPlaybook.value.toMutableList()
        val existingIdx = current.indexOfFirst { it.id == taskId }
        if (existingIdx >= 0) {
            val task = current[existingIdx]
            current[existingIdx] = task.copy(isCompleted = !task.isCompleted)
        } else {
            current.add(PlaybookTask(id = taskId, userId = GUEST_USER_ID, title = taskId, isCompleted = true))
        }
        savePlaybookTasks(current)
    }

    fun togglePlaybookTask(taskId: String, completed: Boolean) {
        val current = _guestPlaybook.value.toMutableList()
        val existingIdx = current.indexOfFirst { it.id == taskId }
        if (existingIdx >= 0) {
            val task = current[existingIdx]
            current[existingIdx] = task.copy(isCompleted = completed)
        } else {
            current.add(PlaybookTask(id = taskId, userId = GUEST_USER_ID, title = taskId, isCompleted = completed))
        }
        savePlaybookTasks(current)
    }

    fun isPlaybookTaskCompleted(taskId: String): Boolean {
        return _guestPlaybook.value.firstOrNull { it.id == taskId }?.isCompleted == true
    }

    private fun computeStatus(nowMillis: Long = System.currentTimeMillis()): GuestLifecycleStatus {
        val isActive = prefs.getBoolean(KEY_GUEST_ACTIVE, false)
        val startedAt = prefs.getLong(KEY_GUEST_STARTED_AT, 0L)
        val dismissedDay = prefs.getInt(KEY_DAY20_DISMISSED_ON_DAY, -1)
        return calculateGuestLifecycle(
            isGuestActive = isActive,
            startedAtMillis = startedAt,
            nowMillis = nowMillis,
            dismissedDay = dismissedDay
        )
    }

    private fun loadProspects(): List<ProspectProfile> =
        GuestSessionJsonCodec.decodeProspects(prefs.getString(KEY_GUEST_PROSPECTS, null), GUEST_USER_ID)

    private fun saveProspects(list: List<ProspectProfile>) {
        _guestProspects.value = list
        enqueueUfsWrite {
            prefs.edit().putString(KEY_GUEST_PROSPECTS, GuestSessionJsonCodec.encodeProspects(list)).apply()
        }
    }

    private fun loadLeads(): List<CapturedLead> =
        GuestSessionJsonCodec.decodeLeads(prefs.getString(KEY_GUEST_LEADS, null), GUEST_USER_ID)

    private fun saveLeads(list: List<CapturedLead>) {
        _guestLeads.value = list
        enqueueUfsWrite {
            prefs.edit().putString(KEY_GUEST_LEADS, GuestSessionJsonCodec.encodeLeads(list)).apply()
        }
    }

    private fun loadPlaybookTasks(): List<PlaybookTask> =
        GuestSessionJsonCodec.decodePlaybookTasks(prefs.getString(KEY_GUEST_PLAYBOOK, null), GUEST_USER_ID)

    private fun savePlaybookTasks(list: List<PlaybookTask>) {
        _guestPlaybook.value = list
        enqueueUfsWrite {
            prefs.edit().putString(KEY_GUEST_PLAYBOOK, GuestSessionJsonCodec.encodePlaybookTasks(list)).apply()
        }
    }

    private fun enqueueUfsWrite(block: () -> Unit) {
        DimensityCpuScheduler.incrementUfsQueueDepth()
        ufsIoScope.launch {
            try {
                block()
            } finally {
                DimensityCpuScheduler.decrementUfsQueueDepth()
            }
        }
    }

    companion object {
        const val PREFS_NAME = "vibe_pitch_guest_prefs"
        const val KEY_GUEST_ACTIVE = "guest_active"
        const val KEY_GUEST_STARTED_AT = "guest_started_at_millis"
        const val KEY_DAY20_DISMISSED_ON_DAY = "day20_dismissed_on_day"
        const val KEY_GUEST_MIGRATED_TO_CLOUD = "guest_migrated_to_cloud"
        const val KEY_PENDING_BUSINESS_PROFILE = "pending_business_profile_json"
        const val KEY_GUEST_PROSPECTS = "guest_prospects_json"
        const val KEY_GUEST_LEADS = "guest_leads_json"
        const val KEY_GUEST_PLAYBOOK = "guest_playbook_json"
        const val GUEST_USER_ID = "local_guest_user"
        const val MILLIS_PER_DAY = 86_400_000L
        const val GUEST_MAX_DAYS = 30
        const val GUEST_UPGRADE_PROMPT_DAY = 20

        fun calculateGuestLifecycle(
            isGuestActive: Boolean,
            startedAtMillis: Long,
            nowMillis: Long,
            dismissedDay: Int = -1
        ): GuestLifecycleStatus {
            if (!isGuestActive || startedAtMillis <= 0L) {
                return GuestLifecycleStatus(
                    isGuestActive = false,
                    startedAtMillis = startedAtMillis,
                    currentDayNumber = 1,
                    daysRemaining = GUEST_MAX_DAYS,
                    shouldShowDay20Prompt = false,
                    isGuestExpired = false
                )
            }
            val elapsedMillis = (nowMillis - startedAtMillis).coerceAtLeast(0L)
            val currentDay = (elapsedMillis / MILLIS_PER_DAY).toInt() + 1
            val daysRemaining = (GUEST_MAX_DAYS - currentDay + 1).coerceAtLeast(0)
            val isExpired = currentDay > GUEST_MAX_DAYS
            val shouldPromptDay20 = !isExpired &&
                currentDay >= GUEST_UPGRADE_PROMPT_DAY &&
                dismissedDay != currentDay

            return GuestLifecycleStatus(
                isGuestActive = true,
                startedAtMillis = startedAtMillis,
                currentDayNumber = currentDay,
                daysRemaining = daysRemaining,
                shouldShowDay20Prompt = shouldPromptDay20,
                isGuestExpired = isExpired
            )
        }
    }
}
