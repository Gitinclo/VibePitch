package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.BusinessAccountProfile
import com.example.data.model.CapturedLead
import com.example.data.model.NicheCatalog
import com.example.data.model.PlaybookTask
import com.example.data.model.ProspectProfile
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.example.domain.InputBoundsValidator
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

class VibePitchRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeBusinessProfile(userId: String = requireUserId()): Flow<BusinessAccountProfile?> = flow {
        val path = "users/$userId/businessProfile/profile"
        emitAll(
            db.collection("users").document(userId)
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
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveBusinessProfile(profile: BusinessAccountProfile): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
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

    fun observeProspects(userId: String = requireUserId()): Flow<List<ProspectProfile>> = flow {
        val path = "users/$userId/prospects"
        emitAll(
            db.collection("users").document(userId).collection("prospects")
                .whereEqualTo("userId", userId)
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
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeLeads(userId: String = requireUserId()): Flow<List<CapturedLead>> = flow {
        val path = "users/$userId/leads"
        emitAll(
            db.collection("users").document(userId).collection("leads")
                .whereEqualTo("userId", userId)
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
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observePlaybookTasks(userId: String = requireUserId()): Flow<List<PlaybookTask>> = flow {
        val path = "users/$userId/playbook"
        emitAll(
            db.collection("users").document(userId).collection("playbook")
                .whereEqualTo("userId", userId)
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
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createProspect(prospect: ProspectProfile): Result<String> = runCatching {
        val uid = requireUserId()
        val sanitizedId = prospect.id.replace(Regex("[^a-zA-Z0-9_\\-]"), "")
        val docRef = if (sanitizedId.isNotBlank()) {
            db.collection("users").document(uid).collection("prospects").document(sanitizedId)
        } else {
            db.collection("users").document(uid).collection("prospects").document()
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

    suspend fun getProspectById(targetUserId: String, prospectId: String): Result<ProspectProfile?> = runCatching {
        val docRef = db.collection("users").document(targetUserId)
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

    suspend fun updateProspect(prospect: ProspectProfile): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(prospect.id.isNotBlank()) { "Prospect ID must not be blank for update" }
        val docRef = db.collection("users").document(uid).collection("prospects").document(prospect.id)
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

    suspend fun deleteProspect(prospectId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("prospects").document(prospectId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun submitLead(lead: CapturedLead): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("leads").document()
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

    suspend fun deleteLead(leadId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("leads").document(leadId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun togglePlaybookTask(task: PlaybookTask): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(task.id.isNotBlank()) { "Task ID must not be blank" }
        val docRef = db.collection("users").document(uid).collection("playbook").document(task.id)
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

    suspend fun migrateGuestDataToCloud(
        userId: String = requireUserId(),
        profile: BusinessAccountProfile?,
        guestProspects: List<ProspectProfile>,
        guestLeads: List<CapturedLead>,
        guestPlaybook: List<PlaybookTask>
    ): Result<Unit> = runCatching {
        if (profile != null) {
            saveBusinessProfile(profile.copy(userId = userId, upgradedFromGuest = true)).getOrThrow()
        }
        for (prospect in guestProspects) {
            createProspect(prospect.copy(userId = userId)).getOrThrow()
        }
        for (lead in guestLeads) {
            submitLead(lead.copy(userId = userId)).getOrThrow()
        }
        for (task in guestPlaybook) {
            val sanitizedId = task.id.replace(Regex("[^a-zA-Z0-9_\\-]"), "").ifBlank { "task_1" }
            val docRef = db.collection("users").document(userId).collection("playbook").document(sanitizedId)
            val payload = mapOf(
                "userId" to userId,
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

    suspend fun seedStarterDataIfEmpty(userId: String = requireUserId()): Result<Unit> = runCatching {
        val prospectsSnap = db.collection("users").document(userId).collection("prospects")
            .whereEqualTo("userId", userId)
            .limit(1)
            .get()
            .await()

        if (prospectsSnap.isEmpty) {
            val starterProspects = NicheCatalog.buildDefaultProspects(userId)
            for (p in starterProspects) {
                createProspect(p).getOrThrow()
            }
        }

        val playbookSnap = db.collection("users").document(userId).collection("playbook")
            .whereEqualTo("userId", userId)
            .limit(1)
            .get()
            .await()

        if (playbookSnap.isEmpty) {
            val defaultTasks = NicheCatalog.buildDefaultPlaybookTasks(userId)
            for (task in defaultTasks) {
                val docRef = db.collection("users").document(userId).collection("playbook").document(task.id)
                val payload = mapOf(
                    "userId" to userId,
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
