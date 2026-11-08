package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LocalGuestSessionManager
import com.example.data.model.NicheKey
import com.example.ui.VibePitchViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var guestManager: LocalGuestSessionManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        guestManager = LocalGuestSessionManager(context)
        guestManager.clearAllGuestData()
    }

    @Test
    fun guestSessionLifecycle_correctlyEvaluatesDay1_Day20_andDay31States() {
        val now = System.currentTimeMillis()
        val dayInMillis = 24L * 60L * 60L * 1000L

        // Day 1: Active Guest Session (Days 1-19)
        val day1Status = guestManager.evaluateLifecycleAt(
            installEpochMillis = now - (0 * dayInMillis),
            currentEpochMillis = now
        )
        assertEquals(1, day1Status.dayCount)
        assertFalse(day1Status.shouldShowSoftPrompt)
        assertFalse(day1Status.isHardGated)

        // Day 20: Dismissible Soft Upgrade Prompt (Days 20-30)
        val day20Status = guestManager.evaluateLifecycleAt(
            installEpochMillis = now - (19 * dayInMillis),
            currentEpochMillis = now
        )
        assertEquals(20, day20Status.dayCount)
        assertTrue(day20Status.shouldShowSoftPrompt)
        assertFalse(day20Status.isHardGated)

        // Day 31: Required Upgrade Gate (Day 31+)
        val day31Status = guestManager.evaluateLifecycleAt(
            installEpochMillis = now - (30 * dayInMillis),
            currentEpochMillis = now
        )
        assertEquals(31, day31Status.dayCount)
        assertTrue(day31Status.isHardGated)
    }

    @Test
    fun guestCrudOperations_prospectsLeadsPlaybookAndPendingBusinessProfile() {
        // 1. Create Prospect with over-length strings to verify truncation
        val created = guestManager.addProspect(
            businessName = "X".repeat(120),
            contactName = "Y".repeat(120),
            nicheKey = NicheKey.SAAS_AI_REMEDIATION,
            websiteGapNote = "Z".repeat(350),
            setupFeeOffer = 60_000,
            monthlyRetainerOffer = 6_000,
            depositPercent = 50
        )
        assertEquals(100, created.businessName.length)
        assertEquals(100, created.contactName.length)
        assertEquals(300, created.websiteGapNote.length)
        assertEquals(50_000L, created.setupFeeOffer)
        assertEquals(5_000L, created.monthlyRetainerOffer)

        // 2. Advance Stage & Delete
        val advanced = guestManager.advanceProspectStage(created.id)
        assertNotNull(advanced)
        guestManager.deleteProspect(created.id)
        assertTrue(guestManager.getProspects().none { it.id == created.id })

        // 3. Lead Capture & Playbook Task Toggle
        val lead = guestManager.captureLead(
            businessName = "Glow Salon",
            contactName = "Maya",
            nicheKey = NicheKey.SALON_AND_CLEANERS,
            notes = "Interested in booking link integration"
        )
        assertEquals("Glow Salon", lead.businessName)

        guestManager.togglePlaybookTask("day_1_audit", completed = true)
        assertTrue(guestManager.isPlaybookTaskCompleted("day_1_audit"))

        // 4. Stage Pending BusinessAccountProfile for Cloud Migration
        guestManager.savePendingBusinessProfile(
            agencyName = "Apex Digital Studio",
            tagline = "T".repeat(200),
            primaryNiche = NicheKey.ECOMMERCE_SHOPIFY
        )
        val pendingProfile = guestManager.getPendingBusinessProfile()
        assertNotNull(pendingProfile)
        assertEquals(150, pendingProfile!!.tagline.length)
    }

    @Test
    fun vibePitchViewModel_guestModeOperations_updatesStateAndRespectsBounds() {
        guestManager.startOrResumeGuestSession()
        val viewModel = VibePitchViewModel(
            repository = null,
            guestSessionManager = guestManager,
            currentUserId = LocalGuestSessionManager.GUEST_USER_ID,
            isGuestMode = true
        )

        viewModel.updateActiveBrandDraft(
            businessName = "N".repeat(140),
            contactName = "C".repeat(130),
            niche = NicheKey.DENTAL_AND_TUTORS,
            tagline = "T".repeat(190),
            websiteGapNote = "W".repeat(360),
            depositPercent = 140,
            setupFeeOffer = 70_000,
            monthlyRetainerOffer = 8_000
        )

        val brand = viewModel.activeBrand.value
        assertEquals(100, brand.businessName.length)
        assertEquals(100, brand.contactName.length)
        assertEquals(150, brand.tagline.length)
        assertEquals(300, brand.websiteGapNote.length)
        assertEquals(100, brand.depositPercent)
        assertEquals(50_000, brand.setupFeeOffer)
        assertEquals(5_000, brand.monthlyRetainerOffer)

        viewModel.openComplianceDocsModal()
        assertTrue(viewModel.showComplianceDocsModal.value)
        viewModel.closeComplianceDocsModal()
        assertFalse(viewModel.showComplianceDocsModal.value)
    }
}
