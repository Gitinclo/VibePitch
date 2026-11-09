package com.gitinclo.vibepitch

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.gitinclo.vibepitch.auth.AuthGuardState
import com.gitinclo.vibepitch.auth.DataResult
import com.gitinclo.vibepitch.auth.UnauthenticatedSessionException
import com.gitinclo.vibepitch.data.local.LocalGuestSessionManager
import com.gitinclo.vibepitch.data.model.BusinessAccountProfile
import com.gitinclo.vibepitch.data.model.CapturedLead
import com.gitinclo.vibepitch.data.model.PlaybookTask
import com.gitinclo.vibepitch.data.model.ProspectProfile
import com.gitinclo.vibepitch.data.repository.VibePitchRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VibePitchRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: VibePitchRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = VibePitchRepository(context)
    }

    @Test
    fun evaluateAuthGuard_distinguishesAuthenticatedGuestAndUnauthenticatedStates() {
        val unauthenticated = repository.evaluateAuthGuard(explicitUserId = "")
        assertTrue(unauthenticated is AuthGuardState.Unauthenticated)

        val guestMode = repository.evaluateAuthGuard(explicitUserId = LocalGuestSessionManager.GUEST_USER_ID)
        assertTrue(guestMode is AuthGuardState.GuestMode)
        assertEquals(LocalGuestSessionManager.GUEST_USER_ID, (guestMode as AuthGuardState.GuestMode).guestUserId)

        val authenticated = repository.evaluateAuthGuard(explicitUserId = "  user_abc_123  ")
        assertTrue(authenticated is AuthGuardState.Authenticated)
        assertEquals("user_abc_123", (authenticated as AuthGuardState.Authenticated).userId)
    }

    @Test
    fun saveBusinessProfileSafe_returnsAuthRequiredDataResultWithoutCrashingWhenUserIdBlank() = runBlocking {
        // Use a repository with null Firestore or blank userId to verify safe DataResult return
        val unconfiguredRepo = VibePitchRepository(db = null)
        val result = unconfiguredRepo.saveBusinessProfileSafe(BusinessAccountProfile(userId = ""))
        assertTrue(result is DataResult.AuthRequired)
        assertTrue(result.isFailure)
        assertNull(result.getOrNull())
    }

    @Test
    fun writeOperations_withUnconfiguredOrUnauthenticatedSession_returnExplicitFailureWithUnauthenticatedSessionException() = runBlocking {
        val unconfiguredRepo = VibePitchRepository(db = null)

        val profileResult = unconfiguredRepo.saveBusinessProfile(BusinessAccountProfile(userId = "   "))
        assertTrue(profileResult.isFailure)
        assertTrue(profileResult.exceptionOrNull() is UnauthenticatedSessionException)

        val prospectResult = unconfiguredRepo.createProspect(ProspectProfile(userId = ""))
        assertTrue(prospectResult.isFailure)
        assertTrue(prospectResult.exceptionOrNull() is UnauthenticatedSessionException)

        val updateProspectResult = unconfiguredRepo.updateProspect(ProspectProfile(id = "p1", userId = ""))
        assertTrue(updateProspectResult.isFailure)
        assertTrue(updateProspectResult.exceptionOrNull() is UnauthenticatedSessionException)

        val deleteProspectResult = unconfiguredRepo.deleteProspect(prospectId = "p1", userId = "")
        assertTrue(deleteProspectResult.isFailure)
        assertTrue(deleteProspectResult.exceptionOrNull() is UnauthenticatedSessionException)

        val leadResult = unconfiguredRepo.submitLead(CapturedLead(userId = ""))
        assertTrue(leadResult.isFailure)
        assertTrue(leadResult.exceptionOrNull() is UnauthenticatedSessionException)

        val deleteLeadResult = unconfiguredRepo.deleteLead(leadId = "l1", userId = "")
        assertTrue(deleteLeadResult.isFailure)
        assertTrue(deleteLeadResult.exceptionOrNull() is UnauthenticatedSessionException)

        val toggleTaskResult = unconfiguredRepo.togglePlaybookTask(PlaybookTask(id = "t1", userId = ""))
        assertTrue(toggleTaskResult.isFailure)
        assertTrue(toggleTaskResult.exceptionOrNull() is UnauthenticatedSessionException)

        val seedResult = unconfiguredRepo.seedStarterDataIfEmpty(userId = "")
        assertTrue(seedResult.isFailure)
        assertTrue(seedResult.exceptionOrNull() is UnauthenticatedSessionException)

        val migrateResult = unconfiguredRepo.migrateGuestDataToCloud(
            userId = "",
            profile = null,
            guestProspects = emptyList(),
            guestLeads = emptyList(),
            guestPlaybook = emptyList()
        )
        assertTrue(migrateResult.isFailure)
        assertTrue(migrateResult.exceptionOrNull() is UnauthenticatedSessionException)
    }

    @Test
    fun observeFlows_withBlankUserId_emitSafeEmptyDefaultsWithoutThrowing() = runBlocking {
        assertNull(repository.observeBusinessProfile("   ").first())
        assertTrue(repository.observeProspects("").first().isEmpty())
        assertTrue(repository.observeLeads("").first().isEmpty())
        assertTrue(repository.observePlaybookTasks("").first().isEmpty())
    }

    @Test
    fun getProspectById_withBlankUserIdOrProspectId_returnsFailureResult() = runBlocking {
        val blankUserResult = repository.getProspectById(targetUserId = "", prospectId = "p1")
        assertTrue(blankUserResult.isFailure)
        assertNotNull(blankUserResult.exceptionOrNull())

        val blankProspectResult = repository.getProspectById(targetUserId = "user_1", prospectId = "   ")
        assertTrue(blankProspectResult.isFailure)
        assertNotNull(blankProspectResult.exceptionOrNull())
        assertFalse(blankProspectResult.isSuccess)
    }
}
