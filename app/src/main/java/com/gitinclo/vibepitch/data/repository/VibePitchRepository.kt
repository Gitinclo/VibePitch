package com.gitinclo.vibepitch.data.repository

import android.content.Context
import com.gitinclo.vibepitch.auth.AuthGuardState
import com.gitinclo.vibepitch.auth.DataResult
import com.gitinclo.vibepitch.auth.UnauthenticatedSessionException
import com.gitinclo.vibepitch.auth.toDataResult
import com.gitinclo.vibepitch.data.local.LocalGuestSessionManager
import com.gitinclo.vibepitch.data.model.BusinessAccountProfile
import com.gitinclo.vibepitch.data.model.CapturedLead
import com.gitinclo.vibepitch.data.model.NicheCatalog
import com.gitinclo.vibepitch.data.model.PlaybookTask
import com.gitinclo.vibepitch.data.model.ProspectProfile
import com.gitinclo.vibepitch.data.remote.FirebaseEnvValidator
import com.gitinclo.vibepitch.data.remote.OperationType
import com.gitinclo.vibepitch.data.remote.handleFirestoreError
import com.gitinclo.vibepitch.domain.InputBoundsValidator
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Cloud Firestore repository for VibePitch Studio.
 * Uses explicit AuthGuardState and Result/DataResult return contracts rather than
 * throwing runtime assertions on unauthenticated or guest states.
 */
class VibePitchRepository(private val db: FirebaseFirestore? = null) {

    constructor(context: Context) : this(
        FirebaseEnvValidator.getFirestoreOrNull(context.applicationContext)
    )

    private fun currentAuthUidOrNull(): String? = try {
        Firebase.auth.currentUser?.uid?.takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }

    /**
     * Evaluates the current authentication state without throwing runtime exceptions.
     */
    fun evaluateAuthGuard(explicitUserId: String? = null): AuthGuardState {
        val activeUid = currentAuthUidOrNull() ?: explicitUserId?.trim()?.takeIf { it.isNotBlank() }
        return when {
            activeUid == null -> AuthGuardState.Unauthenticated("No signed-in user session")
            activeUid == LocalGuestSessionManager.GUEST_USER_ID -> AuthGuardState.GuestMode(activeUid)
            else -> AuthGuardState.Authenticated(activeUid)
        }
    }

    private fun resolveAuthenticatedUserId(explicitUserId: String? = null): Result<String> {
        return when (val guard = evaluateAuthGuard(explicitUserId)) {
            is AuthGuardState.Authenticated -> Result.success(guard.userId)
            is AuthGuardState.GuestMode -> Result.failure(
                UnauthenticatedSessionException("Guest mode uses local device storage until upgraded to a Permanent Business Account.")
            )
            is AuthGuardState.Unauthenticated -> Result.failure(
                UnauthenticatedSessionException(guard.reason)
            )
        }
    }

    private fun resolveFirestoreInstance(): Result<FirebaseFirestore> {
        val instance = db
        return if (instance != null) {
            Result.success(instance)
        } else {
            Result.failure(UnauthenticatedSessionException("Cloud Firestore is not initialized in this environment."))
        }
    }

    fun observeBusinessProfile(userId: String = ""): Flow<BusinessAccountProfile?> = flow {
        val firestore = db
        val guard = evaluateAuthGuard(userId)
        if (firestore == null || guard !is AuthGuardState.Authenticated) {
            emit(null)
            return@flow
        }
        val resolvedUid = guard.userId
        val path = "users/$resolvedUid/businessProfile/profile"
        emitAll(
            firestore.collection("users").document(resolvedUid)
                .collection("businessProfile").document("profile")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            BusinessAccountProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && currentAuthUidOrNull() == resolvedUid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveBusinessProfile(profile: BusinessAccountProfile): Result<Unit> {
        val firestore = resolveFirestoreInstance().getOrElse { return Result.failure(it) }
        val uid = resolveAuthenticatedUserId(profile.userId).getOrElse { return Result.failure(it) }
        return runCatching {
            val docRef = firestore.collection("users").document(uid)
                .collection("businessProfile").document("profile")
            val existingSnap = try {
                docRef.get().await()
            } catch (_: Exception) {
                null
            }
            val payload = mapOf(
                "userId" to uid,
                "agencyName" to InputBoundsValidator.sanitizeBusinessName(profile.agencyName).ifBlank { "My VibePitch Studio" },
                "ownerName" to InputBoundsValidator.sanitizeContactName(profile.ownerName).ifBlank { "Owner" },
                "businessEmail" to profile.businessEmail.trim().take(120),
                "businessPhone" to profile.businessPhone.trim().take(40),
                "primaryNicheKey" to profile.primaryNicheKey,
                "websiteUrl" to profile.websiteUrl.trim().take(200),
                "upgradedFromGuest" to profile.upgradedFromGuest,
                "createdAt" to (existingSnap?.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            try {
                docRef.set(payload).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, docRef.path)
                throw e
            }
        }
    }

    suspend fun saveBusinessProfileSafe(profile: BusinessAccountProfile): DataResult<Unit> =
        saveBusinessProfile(profile).toDataResult()

    fun observeProspects(userId: String = ""): Flow<List<ProspectProfile>> = flow {
        val firestore = db
        val guard = evaluateAuthGuard(userId)
        if (firestore == null || guard !is AuthGuardState.Authenticated) {
            emit(emptyList())
            return@flow
        }
        val resolvedUid = guard.userId
        val path = "users/$resolvedUid/prospects"
        emitAll(
            firestore.collection("users").document(resolvedUid).collection("prospects")
                .whereEqualTo("userId", resolvedUid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ProspectProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && currentAuthUidOrNull() == resolvedUid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeLeads(userId: String = ""): Flow<List<CapturedLead>> = flow {
        val firestore = db
        val guard = evaluateAuthGuard(userId)
        if (firestore == null || guard !is AuthGuardState.Authenticated) {
            emit(emptyList())
            return@flow
        }
        val resolvedUid = guard.userId
        val path = "users/$resolvedUid/leads"
        emitAll(
            firestore.collection("users").document(resolvedUid).collection("leads")
                .whereEqualTo("userId", resolvedUid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CapturedLead::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && currentAuthUidOrNull() == resolvedUid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observePlaybookTasks(userId: String = ""): Flow<List<PlaybookTask>> = flow {
        val firestore = db
        val guard = evaluateAuthGuard(userId)
        if (firestore == null || guard !is AuthGuardState.Authenticated) {
            emit(emptyList())
            return@flow
        }
        val resolvedUid = guard.userId
        val path = "users/$resolvedUid/playbook"
        emitAll(
            firestore.collection("users").document(resolvedUid).collection("playbook")
                .whereEqualTo("userId", resolvedUid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PlaybookTask::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(compareBy<PlaybookTask> { it.stepNumber }.thenBy { it.title })
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && currentAuthUidOrNull() == resolvedUid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createProspect(prospect: ProspectProfile): Result<String> {
        val firestore = resolveFirestoreInstance().getOrElse { return Result.failure(it) }
        val uid = resolveAuthenticatedUserId(prospect.userId).getOrElse { return Result.failure(it) }
        return runCatching {
            val sanitizedId = prospect.id.replace(Regex("[^a-zA-Z0-9_\\-]"), "")
            val docRef = if (sanitizedId.isNotBlank()) {
                firestore.collection("users").document(uid).collection("prospects").document(sanitizedId)
            } else {
                firestore.collection("users").document(uid).collection("prospects").document()
            }
            val payload = mapOf(
                "userId" to uid,
                "businessName" to InputBoundsValidator.sanitizeBusinessName(prospect.businessName).ifBlank { "Local Prospect" },
                "contactName" to InputBoundsValidator.sanitizeContactName(prospect.contactName),
                "nicheKey" to prospect.nicheKey,
                "websiteGapNote" to InputBoundsValidator.sanitizeWebsiteGapNote(prospect.websiteGapNote),
                "brandColorHex" to prospect.brandColorHex.trim().take(12),
                "tagline" to InputBoundsValidator.sanitizeTagline(prospect.tagline),
                "depositPercent" to InputBoundsValidator.clampDepositPercentLong(prospect.depositPercent),
                "setupFeeOffer" to InputBoundsValidator.clampSetupFeeOfferLong(prospect.setupFeeOffer),
                "monthlyRetainerOffer" to InputBoundsValidator.clampMonthlyRetainerOfferLong(prospect.monthlyRetainerOffer),
                "stage" to prospect.stage,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            try {
                docRef.set(payload).await()
                docRef.id
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, docRef.path)
                throw e
            }
        }
    }

    suspend fun getProspectById(targetUserId: String, prospectId: String): Result<ProspectProfile?> {
        val firestore = resolveFirestoreInstance().getOrElse { return Result.failure(it) }
        if (targetUserId.isBlank() || prospectId.isBlank()) {
            return Result.failure(IllegalArgumentException("targetUserId and prospectId must not be blank"))
        }
        return runCatching {
            val docRef = firestore.collection("users").document(targetUserId)
                .collection("prospects").document(prospectId)
            try {
                val snap = docRef.get().await()
                snap.toObject(ProspectProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.GET, docRef.path)
                throw e
            }
        }
    }

    suspend fun updateProspect(prospect: ProspectProfile): Result<Unit> {
        val firestore = resolveFirestoreInstance().getOrElse { return Result.failure(it) }
        val uid = resolveAuthenticatedUserId(prospect.userId).getOrElse { return Result.failure(it) }
        if (prospect.id.isBlank()) {
            return Result.failure(IllegalArgumentException("Prospect ID must not be blank for update"))
        }
        return runCatching {
            val docRef = firestore.collection("users").document(uid).collection("prospects").document(prospect.id)
            val updates = mapOf(
                "businessName" to InputBoundsValidator.sanitizeBusinessName(prospect.businessName),
                "contactName" to InputBoundsValidator.sanitizeContactName(prospect.contactName),
                "nicheKey" to prospect.nicheKey,
                "websiteGapNote" to InputBoundsValidator.sanitizeWebsiteGapNote(prospect.websiteGapNote),
                "brandColorHex" to prospect.brandColorHex.trim().take(12),
                "tagline" to InputBoundsValidator.sanitizeTagline(prospect.tagline),
                "depositPercent" to InputBoundsValidator.clampDepositPercentLong(prospect.depositPercent),
                "setupFeeOffer" to InputBoundsValidator.clampSetupFeeOfferLong(prospect.setupFeeOffer),
                "monthlyRetainerOffer" to InputBoundsValidator.clampMonthlyRetainerOfferLong(prospect.monthlyRetainerOffer),
                "stage" to prospect.stage,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            try {
                docRef.update(updates).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, docRef.path)
                throw e
            }
        }
    }

    suspend fun deleteProspect(prospectId: String, userId: String? = null): Result<Unit> {
        val firestore = resolveFirestoreInstance().getOrElse { return Result.failure(it) }
        val uid = resolveAuthenticatedUserId(userId).getOrElse { return Result.failure(it) }
        if (prospectId.isBlank()) {
            return Result.failure(IllegalArgumentException("Prospect ID must not be blank"))
        }
        return runCatching {
            val docRef = firestore.collection("users").document(uid).collection("prospects").document(prospectId)
            try {
                docRef.delete().await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.DELETE, docRef.path)
                throw e
            }
        }
    }

    suspend fun submitLead(lead: CapturedLead): Result<String> {
        val firestore = resolveFirestoreInstance().getOrElse { return Result.failure(it) }
        val uid = resolveAuthenticatedUserId(lead.userId).getOrElse { return Result.failure(it) }
        return runCatching {
            val docRef = firestore.collection("users").document(uid).collection("leads").document()
            val payload = mapOf(
                "userId" to uid,
                "prospectId" to lead.prospectId.take(128),
                "prospectBusinessName" to InputBoundsValidator.sanitizeBusinessName(lead.prospectBusinessName).ifBlank { "Local Demo Business" },
                "nicheKey" to lead.nicheKey,
                "customerName" to InputBoundsValidator.sanitizeContactName(lead.customerName),
                "customerPhone" to lead.customerPhone.trim().take(40),
                "customerEmail" to lead.customerEmail.trim().take(120),
                "preferredDate" to lead.preferredDate.trim().take(80),
                "selectedServiceName" to lead.selectedServiceName.trim().take(120),
                "selectedAddOns" to lead.selectedAddOns.trim().take(300),
                "unitQuantity" to lead.unitQuantity.coerceIn(1L, 100000L),
                "estimatedTotal" to lead.estimatedTotal.coerceIn(0L, 1000000L),
                "depositAmount" to lead.depositAmount.coerceIn(0L, 1000000L),
                "notes" to InputBoundsValidator.sanitizeNotes(lead.notes),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            try {
                docRef.set(payload).await()
                docRef.id
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, docRef.path)
                throw e
            }
        }
    }

    suspend fun deleteLead(leadId: String, userId: String? = null): Result<Unit> {
        val firestore = resolveFirestoreInstance().getOrElse { return Result.failure(it) }
        val uid = resolveAuthenticatedUserId(userId).getOrElse { return Result.failure(it) }
        if (leadId.isBlank()) {
            return Result.failure(IllegalArgumentException("Lead ID must not be blank"))
        }
        return runCatching {
            val docRef = firestore.collection("users").document(uid).collection("leads").document(leadId)
            try {
                docRef.delete().await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.DELETE, docRef.path)
                throw e
            }
        }
    }

    suspend fun togglePlaybookTask(task: PlaybookTask): Result<Unit> {
        val firestore = resolveFirestoreInstance().getOrElse { return Result.failure(it) }
        val uid = resolveAuthenticatedUserId(task.userId).getOrElse { return Result.failure(it) }
        if (task.id.isBlank()) {
            return Result.failure(IllegalArgumentException("Task ID must not be blank"))
        }
        return runCatching {
            val docRef = firestore.collection("users").document(uid).collection("playbook").document(task.id)
            val updates = mapOf(
                "isCompleted" to !task.isCompleted,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            try {
                docRef.update(updates).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, docRef.path)
                throw e
            }
        }
    }

    suspend fun migrateGuestDataToCloud(
        userId: String = "",
        profile: BusinessAccountProfile?,
        guestProspects: List<ProspectProfile>,
        guestLeads: List<CapturedLead>,
        guestPlaybook: List<PlaybookTask>
    ): Result<Unit> {
        val firestore = resolveFirestoreInstance().getOrElse { return Result.failure(it) }
        val resolvedUid = resolveAuthenticatedUserId(userId.ifBlank { profile?.userId })
            .getOrElse { return Result.failure(it) }
        return runCatching {
            if (profile != null) {
                saveBusinessProfile(profile.copy(userId = resolvedUid, upgradedFromGuest = true)).getOrThrow()
            }
            for (prospect in guestProspects) {
                createProspect(prospect.copy(userId = resolvedUid)).getOrThrow()
            }
            for (lead in guestLeads) {
                submitLead(lead.copy(userId = resolvedUid)).getOrThrow()
            }
            for (task in guestPlaybook) {
                val sanitizedId = task.id.replace(Regex("[^a-zA-Z0-9_\\-]"), "").ifBlank { "task_1" }
                val docRef = firestore.collection("users").document(resolvedUid).collection("playbook").document(sanitizedId)
                val payload = mapOf(
                    "userId" to resolvedUid,
                    "dayRange" to task.dayRange.take(40),
                    "stepNumber" to task.stepNumber.coerceIn(1L, 20L),
                    "title" to task.title.take(140),
                    "description" to task.description.take(400),
                    "proTip" to task.proTip.take(300),
                    "isCompleted" to task.isCompleted,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
        }
    }

    suspend fun seedStarterDataIfEmpty(userId: String = ""): Result<Unit> {
        val firestore = resolveFirestoreInstance().getOrElse { return Result.failure(it) }
        val resolvedUid = resolveAuthenticatedUserId(userId).getOrElse { return Result.failure(it) }
        return runCatching {
            val prospectsSnap = firestore.collection("users").document(resolvedUid).collection("prospects")
                .whereEqualTo("userId", resolvedUid)
                .limit(1)
                .get()
                .await()

            if (prospectsSnap.isEmpty) {
                val starterProspects = NicheCatalog.buildDefaultProspects(resolvedUid)
                for (p in starterProspects) {
                    createProspect(p).getOrThrow()
                }
            }

            val playbookSnap = firestore.collection("users").document(resolvedUid).collection("playbook")
                .whereEqualTo("userId", resolvedUid)
                .limit(1)
                .get()
                .await()

            if (playbookSnap.isEmpty) {
                val defaultTasks = NicheCatalog.buildDefaultPlaybookTasks(resolvedUid)
                for (task in defaultTasks) {
                    val docRef = firestore.collection("users").document(resolvedUid).collection("playbook").document(task.id)
                    val payload = mapOf(
                        "userId" to resolvedUid,
                        "dayRange" to task.dayRange,
                        "stepNumber" to task.stepNumber,
                        "title" to task.title,
                        "description" to task.description,
                        "proTip" to task.proTip,
                        "isCompleted" to task.isCompleted,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.set(payload).await()
                }
            }
        }
    }
}
