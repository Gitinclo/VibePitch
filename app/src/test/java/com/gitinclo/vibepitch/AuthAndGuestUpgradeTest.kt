package com.gitinclo.vibepitch

import android.content.Context
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.gitinclo.vibepitch.data.local.LocalGuestSessionManager
import com.gitinclo.vibepitch.data.model.BusinessAccountProfile
import com.gitinclo.vibepitch.data.model.CapturedLead
import com.gitinclo.vibepitch.data.model.NicheKey
import com.gitinclo.vibepitch.data.model.ProspectProfile
import com.gitinclo.vibepitch.data.model.RevenueStreamMode
import com.gitinclo.vibepitch.data.repository.VibePitchRepository
import com.gitinclo.vibepitch.ui.AppTab
import com.gitinclo.vibepitch.ui.VibePitchViewModel
import com.gitinclo.vibepitch.ui.features.crm.OfflineStorageNoticeBanner
import com.gitinclo.vibepitch.ui.features.crm.StorageSyncStatusDotChip
import com.gitinclo.vibepitch.ui.features.playbook.VibeGuardrailsExplorerCard
import com.gitinclo.vibepitch.ui.theme.VibePitchTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthAndGuestUpgradeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

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

        val day1Status = guestManager.evaluateLifecycleAt(
            installEpochMillis = now,
            currentEpochMillis = now
        )
        assertEquals(1, day1Status.dayCount)
        assertFalse(day1Status.shouldShowSoftPrompt)
        assertFalse(day1Status.isHardGated)

        val day20Status = guestManager.evaluateLifecycleAt(
            installEpochMillis = now - (19 * dayInMillis),
            currentEpochMillis = now
        )
        assertEquals(20, day20Status.dayCount)
        assertTrue(day20Status.shouldShowSoftPrompt)
        assertFalse(day20Status.isHardGated)

        val day31Status = guestManager.evaluateLifecycleAt(
            installEpochMillis = now - (30 * dayInMillis),
            currentEpochMillis = now
        )
        assertEquals(31, day31Status.dayCount)
        assertTrue(day31Status.isHardGated)
    }

    @Test
    fun guestToBusinessAccountMigration_stagesConsumesAndDeactivatesGuestSessionCleanly() {
        guestManager.startOrResumeGuestSession()
        assertTrue(guestManager.guestStatus.value.isGuestActive)

        val createdProspect = guestManager.addProspect(
            businessName = "Lumina Salon",
            contactName = "Chloe",
            nicheKey = NicheKey.SALON_AND_CLEANERS,
            websiteGapNote = "Takes bookings via Instagram DM",
            setupFeeOffer = 300,
            monthlyRetainerOffer = 45,
            depositPercent = 25
        )
        val createdLead = guestManager.captureLead(
            businessName = "Lumina Salon",
            contactName = "Emma Client",
            nicheKey = NicheKey.SALON_AND_CLEANERS,
            notes = "Saturday morning balayage"
        )
        assertTrue(guestManager.hasGuestDataToMigrate())
        assertTrue(guestManager.getProspects().any { it.id == createdProspect.id })
        assertTrue(guestManager.guestLeads.value.any { it.id == createdLead.id })

        guestManager.savePendingBusinessUpgradeDraft(
            BusinessAccountProfile(
                agencyName = "Argon Outcome Studio",
                ownerName = "Alex Founder",
                businessEmail = "alex@argonstudio.io",
                businessPhone = "555-0199",
                primaryNicheKey = NicheKey.SALON_AND_CLEANERS.key,
                websiteUrl = "https://argonstudio.io"
            )
        )

        val consumed = guestManager.consumePendingBusinessUpgradeDraft("auth_uid_999")
        assertNotNull(consumed)
        assertEquals("auth_uid_999", consumed!!.userId)
        assertEquals("Argon Outcome Studio", consumed.agencyName)
        assertTrue(consumed.upgradedFromGuest)

        guestManager.markGuestMigratedAndDeactivate()
        assertFalse(guestManager.guestStatus.value.isGuestActive)
        assertFalse(guestManager.hasGuestDataToMigrate())
        assertNull(guestManager.consumePendingBusinessUpgradeDraft("auth_uid_999"))
    }

    @Test
    fun vibePitchViewModel_offlineFallbackAndMultiStreamSwitching_workEndToEnd() {
        guestManager.startOrResumeGuestSession()
        val viewModel = VibePitchViewModel(
            repository = null,
            guestSessionManager = guestManager,
            currentUserId = "auth_user_offline_test",
            isGuestMode = false
        )

        // Simulate device going offline while authenticated
        viewModel.setDeviceOnlineStatus(false)
        assertFalse(viewModel.isDeviceOnline.value)

        // Switch across all 3 revenue streams
        viewModel.switchRevenueStream(RevenueStreamMode.FREELANCE_MICRO_TOOLS)
        assertEquals(NicheKey.ECOMMERCE_SHOPIFY, viewModel.activeBrand.value.niche)

        viewModel.switchRevenueStream(RevenueStreamMode.PROTOTYPE_REMEDIATION)
        assertEquals(NicheKey.SAAS_AI_REMEDIATION, viewModel.activeBrand.value.niche)

        // Create prospect while offline -> must route to local storage
        viewModel.createNewProspect(
            businessName = "Offline AI SaaS Rescue",
            contactName = "Samir",
            niche = NicheKey.SAAS_AI_REMEDIATION,
            websiteGapNote = "Broken OAuth redirect loop",
            brandColorHex = "#EAB308"
        )
        assertTrue(viewModel.snackbarMessage.value?.contains("Offline •") == true)
        assertTrue(guestManager.getProspects().any { it.businessName == "Offline AI SaaS Rescue" })

        // Submit booking intake while offline -> must route to local storage
        viewModel.updateBookingForm(
            customerName = "Taylor Client",
            customerPhone = "555-0144",
            customerEmail = "taylor@example.com",
            preferredDate = "Tomorrow Morning (8am – 12pm)",
            notes = "Need Stripe link wired up"
        )
        viewModel.submitBookingIntake()
        assertTrue(viewModel.snackbarMessage.value?.contains("Offline •") == true)
        assertTrue(guestManager.guestLeads.value.any { it.customerName == "Taylor Client" })
        assertNotNull(viewModel.quoteState.value.latestSubmittedLead)
        viewModel.dismissLeadReceiptDialog()
        assertNull(viewModel.quoteState.value.latestSubmittedLead)

        // Save business profile while offline -> stages locally
        viewModel.savePermanentBusinessProfileDirectly(
            BusinessAccountProfile(
                agencyName = "Offline Saved Agency",
                ownerName = "Jordan"
            )
        )
        assertTrue(viewModel.snackbarMessage.value?.contains("Offline •") == true)
        assertEquals("Offline Saved Agency", guestManager.getPendingBusinessProfile()?.agencyName)

        // Tab navigation & Presentation mode state transitions
        viewModel.selectTab(AppTab.PLAYBOOK)
        assertEquals(AppTab.PLAYBOOK, viewModel.selectedTab.value)
        viewModel.setPresentationMode(true)
        assertTrue(viewModel.isPresentationMode.value)
        viewModel.setPresentationMode(false)
        assertFalse(viewModel.isPresentationMode.value)
    }

    @Test
    fun repository_unauthenticatedEdgeCases_returnGracefulEmptyFlowsAndFailureResults() = runBlocking {
        // Verify that an unauthenticated/unconfigured repository never crashes Flow collectors or suspend calls
        val repo = VibePitchRepository(context)

        assertNull(repo.observeBusinessProfile("").first())
        assertTrue(repo.observeProspects("").first().isEmpty())
        assertTrue(repo.observeLeads("").first().isEmpty())
        assertTrue(repo.observePlaybookTasks("").first().isEmpty())

        val saveProfileResult = repo.saveBusinessProfile(BusinessAccountProfile(userId = ""))
        assertTrue(saveProfileResult.isFailure)

        val createProspectResult = repo.createProspect(ProspectProfile(userId = ""))
        assertTrue(createProspectResult.isFailure)

        val submitLeadResult = repo.submitLead(CapturedLead(userId = ""))
        assertTrue(submitLeadResult.isFailure)
    }

    @Test
    fun networkStorageStatusComponents_renderOfflineDotAndBannerCorrectlyInCompose() {
        var chipClicked = false
        composeTestRule.setContent {
            VibePitchTheme {
                androidx.compose.foundation.layout.Column {
                    StorageSyncStatusDotChip(
                        isDeviceOnline = false,
                        isGuestMode = false,
                        onClick = { chipClicked = true }
                    )
                    OfflineStorageNoticeBanner(
                        isDeviceOnline = false,
                        isGuestMode = false
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("storage_sync_status_chip").assertIsDisplayed()
        composeTestRule.onNodeWithTag("storage_sync_status_dot", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("offline_local_storage_banner").assertIsDisplayed()
        composeTestRule.onNodeWithTag("offline_banner_status_dot", useUnmergedTree = true).assertIsDisplayed()

        composeTestRule.onNodeWithTag("storage_sync_status_chip").performClick()
        composeTestRule.waitForIdle()
        assertTrue(chipClicked)
    }

    @Test
    fun vibeGuardrailsExplorerCard_renders100RulesAndSupportsCategoryFilteringAndExport() {
        var lastCopiedMessage: String? = null
        composeTestRule.setContent {
            VibePitchTheme {
                androidx.compose.foundation.layout.Column(
                    modifier = androidx.compose.ui.Modifier.verticalScroll(
                        androidx.compose.foundation.rememberScrollState()
                    )
                ) {
                    VibeGuardrailsExplorerCard(
                        brandAccent = androidx.compose.ui.graphics.Color(0xFF10B981),
                        onShowMessage = { lastCopiedMessage = it }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("vibe_guardrails_explorer_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("guardrail_cat_all").assertIsDisplayed()
        composeTestRule.onNodeWithTag("guardrail_cat_i").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("copy_guardrails_export_button")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        assertTrue(lastCopiedMessage?.contains("10 guardrails") == true)
    }
}
