package com.gitinclo.vibepitch.data.repository

import com.gitinclo.vibepitch.base.FirestoreEmulatorTestBase
import com.gitinclo.vibepitch.data.local.LocalGuestSessionManager
import com.gitinclo.vibepitch.data.model.BusinessAccountProfile
import com.gitinclo.vibepitch.data.model.CapturedLead
import com.gitinclo.vibepitch.data.model.NicheKey
import com.gitinclo.vibepitch.data.model.PipelineStage
import com.gitinclo.vibepitch.data.model.ProspectProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class VibePitchRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun calculateGuestLifecycle_day1_day20_and_day31_behavesAsExpected() {
        val startMillis = 1_700_000_000_000L

        // Day 1 (0 days elapsed)
        val day1Status = LocalGuestSessionManager.calculateGuestLifecycle(
            isGuestActive = true,
            startedAtMillis = startMillis,
            nowMillis = startMillis + (2 * 3600_000L)
        )
        assertEquals(1, day1Status.currentDayNumber)
        assertEquals(30, day1Status.daysRemaining)
        assertFalse(day1Status.shouldShowDay20Prompt)
        assertFalse(day1Status.isGuestExpired)

        // Day 20 (19 full days elapsed -> entering the 20th day)
        val day20Millis = startMillis + (19L * LocalGuestSessionManager.MILLIS_PER_DAY) + 60_000L
        val day20Status = LocalGuestSessionManager.calculateGuestLifecycle(
            isGuestActive = true,
            startedAtMillis = startMillis,
            nowMillis = day20Millis
        )
        assertEquals(20, day20Status.currentDayNumber)
        assertEquals(11, day20Status.daysRemaining)
        assertTrue(day20Status.shouldShowDay20Prompt)
        assertFalse(day20Status.isGuestExpired)

        // Day 20 dismissed for today
        val day20Dismissed = LocalGuestSessionManager.calculateGuestLifecycle(
            isGuestActive = true,
            startedAtMillis = startMillis,
            nowMillis = day20Millis,
            dismissedDay = 20
        )
        assertFalse(day20Dismissed.shouldShowDay20Prompt)

        // Day 31 (30 full days elapsed -> guest expired)
        val day31Millis = startMillis + (30L * LocalGuestSessionManager.MILLIS_PER_DAY) + 60_000L
        val day31Status = LocalGuestSessionManager.calculateGuestLifecycle(
            isGuestActive = true,
            startedAtMillis = startMillis,
            nowMillis = day31Millis
        )
        assertEquals(31, day31Status.currentDayNumber)
        assertEquals(0, day31Status.daysRemaining)
        assertFalse(day31Status.shouldShowDay20Prompt)
        assertTrue(day31Status.isGuestExpired)
    }

    @Test
    fun saveAndObserveBusinessProfile_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = VibePitchRepository(firestore)

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveBusinessProfile(
                BusinessAccountProfile(
                    id = "profile",
                    userId = uid,
                    agencyName = "Apex Growth Studio",
                    ownerName = "Alice Vance",
                    businessEmail = "alice@apexgrowth.io",
                    businessPhone = "555-0199",
                    primaryNicheKey = NicheKey.HOME_TRADES.key,
                    websiteUrl = "https://apexgrowth.io",
                    upgradedFromGuest = true
                )
            )
        }
        assertTrue(saveResult.isSuccess)

        val observedProfile = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeBusinessProfile(uid).first { it != null }
        }
        assertNotNull(observedProfile)
        assertEquals("Apex Growth Studio", observedProfile?.agencyName)
        assertTrue(observedProfile?.upgradedFromGuest == true)
    }

    @Test
    fun createAndObserveProspect_allSevenNicheKeys_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = VibePitchRepository(firestore)

        for (niche in NicheKey.entries) {
            val uniqueId = "prospect_${niche.name}_${UUID.randomUUID().toString().take(8)}"
            val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
                repository.createProspect(
                    ProspectProfile(
                        id = uniqueId,
                        userId = uid,
                        businessName = "X".repeat(120), // Sanitized to 100 by InputBoundsValidator
                        contactName = "Carlos Gomez",
                        nicheKey = niche.key,
                        websiteGapNote = "W".repeat(340), // Sanitized to 300 by InputBoundsValidator
                        brandColorHex = niche.defaultColorHex,
                        tagline = "T".repeat(180), // Sanitized to 150 by InputBoundsValidator
                        depositPercent = 125L, // Clamped to 100 by InputBoundsValidator
                        setupFeeOffer = 75_000L, // Clamped to 50,000 by InputBoundsValidator
                        monthlyRetainerOffer = 9_000L, // Clamped to 5,000 by InputBoundsValidator
                        stage = PipelineStage.DEMO_READY.key
                    )
                )
            }
            assertTrue("Expected createProspect to succeed for $niche", createResult.isSuccess)
        }

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeProspects(uid).first { list -> list.size >= NicheKey.entries.size }
        }
        assertEquals(NicheKey.entries.size, observed.size)
        assertTrue(observed.all { it.businessName.length == 100 && it.setupFeeOffer == 50_000L })
    }

    @Test
    fun submitLead_authenticatedOwner_createsAndEmitsLead() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = VibePitchRepository(firestore)

        val leadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.submitLead(
                CapturedLead(
                    userId = uid,
                    prospectId = "prospect_hvac_1",
                    prospectBusinessName = "Summit HVAC Pros",
                    nicheKey = NicheKey.SALON_AND_CLEANERS.key,
                    customerName = "Jordan Taylor",
                    customerPhone = "555-0188",
                    customerEmail = "jordan@example.com",
                    preferredDate = "Thursday 9:00 AM",
                    selectedServiceName = "Signature Color, Gloss & Blowout Package",
                    selectedAddOns = "Deep Bond Repair / Eco Green Supplies",
                    unitQuantity = 3L,
                    estimatedTotal = 275L,
                    depositAmount = 55L,
                    notes = "N".repeat(600) // Sanitized to 500 chars by InputBoundsValidator
                )
            )
        }
        assertTrue(leadResult.isSuccess)
        val leadId = leadResult.getOrThrow()

        val leads = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeLeads(uid).first { list -> list.any { it.id == leadId } }
        }
        val createdLead = leads.first { it.id == leadId }
        assertEquals("Jordan Taylor", createdLead.customerName)
        assertEquals(500, createdLead.notes.length)
    }

    @Test
    fun getProspectById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = VibePitchRepository(firestore)
        val prospectId = "prospect_${UUID.randomUUID()}"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createProspect(
                ProspectProfile(
                    id = prospectId,
                    userId = aliceUid,
                    businessName = "Alice Private Prospect",
                    contactName = "Alice",
                    nicheKey = NicheKey.AUTO_DETAILING.key,
                    websiteGapNote = "No booking form",
                    brandColorHex = "#3B82F6",
                    tagline = "Mobile Detailing",
                    depositPercent = 25L,
                    setupFeeOffer = 250L,
                    monthlyRetainerOffer = 35L,
                    stage = PipelineStage.AUDITED.key
                )
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = VibePitchRepository(firestore)
        val crossResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getProspectById(aliceUid, prospectId)
        }
        assertTrue(crossResult.isFailure)
        val firestoreEx = crossResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun observeProspects_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = VibePitchRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeProspects(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED exception for unauthenticated user")
        } catch (e: Exception) {
            val firestoreException = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@vibepitch.test"
        const val BOB_EMAIL = "bob@vibepitch.test"
        const val DEFAULT_TIMEOUT_MS = 10000L
        const val FLOW_TIMEOUT_MS = 10000L
    }
}
