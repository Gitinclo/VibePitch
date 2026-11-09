package com.gitinclo.vibepitch

import com.gitinclo.vibepitch.data.model.NicheCatalog
import com.gitinclo.vibepitch.data.model.NicheKey
import com.gitinclo.vibepitch.data.model.PipelineStage
import com.gitinclo.vibepitch.data.model.RevenueStreamMode
import com.gitinclo.vibepitch.domain.GuardrailCategory
import com.gitinclo.vibepitch.domain.GuardrailExportFormat
import com.gitinclo.vibepitch.domain.InputBoundsValidator
import com.gitinclo.vibepitch.domain.VibeCodingGuardrailsCatalog
import com.gitinclo.vibepitch.ui.ActiveDemoBrandState
import com.gitinclo.vibepitch.ui.OfferArchetype
import com.gitinclo.vibepitch.ui.PitchScriptGenerator
import com.gitinclo.vibepitch.ui.QuoteCalculatorState
import com.gitinclo.vibepitch.ui.QuotePricingCalculator
import com.gitinclo.vibepitch.ui.ScopeGuardrailConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PitchAndPlaybookValidationTest {

    @Test
    fun allSevenNicheKeys_haveCompleteCatalogAndValidRevenueStreamMappings() {
        val expectedNiches = setOf(
            "HOME_TRADES",
            "SALON_AND_CLEANERS",
            "DENTAL_AND_TUTORS",
            "AUTO_DETAILING",
            "BOUTIQUE_FITNESS",
            "ECOMMERCE_SHOPIFY",
            "SAAS_AI_REMEDIATION"
        )
        val actualNames = NicheKey.entries.map { it.name }.toSet()
        assertEquals(expectedNiches, actualNames)

        NicheKey.entries.forEach { nicheKey ->
            val preset = NicheCatalog.getPreset(nicheKey)
            assertNotNull("Preset must exist for $nicheKey", preset)
            assertTrue("Packages must not be empty for $nicheKey", preset.packages.isNotEmpty())
            assertTrue("Add-ons must not be empty for $nicheKey", preset.addOns.isNotEmpty())
            assertNotNull("RevenueStreamMode must be mapped for $nicheKey", preset.revenueStreamMode)
        }
    }

    @Test
    fun defaultSeedBuilders_produceValidStarterProspectsAndPlaybookTasks() {
        val starterProspects = NicheCatalog.buildDefaultProspects("user_test_123")
        val starterTasks = NicheCatalog.buildDefaultPlaybookTasks("user_test_123")

        assertEquals(4, starterProspects.size)
        assertTrue(starterProspects.all { it.userId == "user_test_123" && it.businessName.isNotBlank() })

        assertEquals(7, starterTasks.size)
        assertTrue(starterTasks.all { it.userId == "user_test_123" && it.stepNumber in 1L..4L })
    }

    @Test
    fun pipelineStage_transitionsProgressSequentiallyAndLoopsCleanly() {
        val stages = PipelineStage.entries
        assertTrue(stages.isNotEmpty())
        for (i in 0 until stages.size - 1) {
            val current = stages[i]
            val next = current.nextStage()
            assertEquals(stages[i + 1], next)
        }
        assertEquals(PipelineStage.AUDITED, PipelineStage.CLOSED_WON.nextStage())
    }

    @Test
    fun quotePricingCalculator_computesBaseUnitAndAddOnTotalsAccurately() {
        val brand = ActiveDemoBrandState(
            niche = NicheKey.HOME_TRADES,
            depositPercent = 20
        )
        val packages = NicheCatalog.getServicePackages(NicheKey.HOME_TRADES)
        val firstPkg = packages.first() // basePrice = 129, perUnitRate = 4
        val calc = QuoteCalculatorState(
            selectedServiceId = firstPkg.id,
            unitValue = 10,
            selectedAddOnIds = setOf("trades_priority") // +65
        )

        val (estimatedTotal, depositAmount) = QuotePricingCalculator.calculateEstimateAndDeposit(brand, calc)
        // 129 + (4 * 10) + 65 = 234; 20% deposit = 46.8 -> 47
        assertEquals(234, estimatedTotal)
        assertEquals(47, depositAmount)
    }

    @Test
    fun quoteAndDepositMath_respectsFirestoreBoundsAndCalculatesAccurately() {
        val setupFee = InputBoundsValidator.clampSetupFeeOffer(65_000)
        val monthlyRetainer = InputBoundsValidator.clampMonthlyRetainerOffer(7_500)
        val depositPercent = InputBoundsValidator.clampDepositPercent(120)

        assertEquals(50_000, setupFee)
        assertEquals(5_000, monthlyRetainer)
        assertEquals(100, depositPercent)

        val standardDeposit = (2_000 * 50) / 100
        assertEquals(1_000, standardDeposit)
    }

    @Test
    fun scriptGenerators_produceNonBlankScriptsAcrossAllOfferArchetypesAndGuardrails() {
        val guardrails = ScopeGuardrailConfig(
            paymentGuardrail = "Stripe Payment Links / Native Checkout",
            dataPrivacyGuardrail = "Google Forms / Zero-Trust Cloud Rules",
            authGuardrail = "Managed Google OAuth (No Custom Passwords)",
            supportWindowDays = 14
        )
        NicheKey.entries.forEach { niche ->
            OfferArchetype.entries.forEach { archetype ->
                val scripts = PitchScriptGenerator.generateAllSixScripts(
                    nicheKey = niche,
                    archetype = archetype,
                    businessName = "Apex Service Co",
                    contactName = "Jordan",
                    setupFee = 1500,
                    monthlyRetainer = 299,
                    depositPercent = 50,
                    guardrailConfig = guardrails
                )
                assertEquals("Must generate all 6 scripts", 6, scripts.size)
                scripts.forEach { script ->
                    assertFalse("Script body should not be blank", script.body.isBlank())
                    assertTrue(
                        "Script must reference business name",
                        script.body.contains("Apex Service Co", ignoreCase = true)
                    )
                }
            }
        }
    }

    @Test
    fun inputBoundsValidator_enforcesExactFirestoreRuleLimits() {
        assertEquals(100, InputBoundsValidator.sanitizeBusinessName("A".repeat(150)).length)
        assertEquals(100, InputBoundsValidator.sanitizeContactName("B".repeat(140)).length)
        assertEquals(300, InputBoundsValidator.sanitizeWebsiteGapNote("C".repeat(400)).length)
        assertEquals(150, InputBoundsValidator.sanitizeTagline("D".repeat(200)).length)
        assertEquals(500, InputBoundsValidator.sanitizeNotes("E".repeat(650)).length)

        val normalized = InputBoundsValidator.normalizeProspectInput(
            businessName = "  " + "X".repeat(130),
            contactName = "  " + "Y".repeat(120),
            websiteGapNote = "Z".repeat(350),
            tagline = "T".repeat(180),
            depositPercent = -15,
            setupFeeOffer = 95_000,
            monthlyRetainerOffer = 9_000
        )
        assertEquals(100, normalized.businessName.length)
        assertEquals(100, normalized.contactName.length)
        assertEquals(300, normalized.websiteGapNote.length)
        assertEquals(150, normalized.tagline.length)
        assertEquals(0, normalized.depositPercent)
        assertEquals(50_000, normalized.setupFeeOffer)
        assertEquals(5_000, normalized.monthlyRetainerOffer)
    }

    @Test
    fun revenueStreamMode_mapsToExpectedArchetypesAndNiches() {
        assertEquals(3, RevenueStreamMode.entries.size)
        assertEquals(RevenueStreamMode.LOCAL_BUSINESS, NicheKey.SALON_AND_CLEANERS.revenueStream)
        assertEquals(RevenueStreamMode.FREELANCE_MICRO_TOOLS, NicheKey.ECOMMERCE_SHOPIFY.revenueStream)
        assertEquals(RevenueStreamMode.PROTOTYPE_REMEDIATION, NicheKey.SAAS_AI_REMEDIATION.revenueStream)
    }

    @Test
    fun vibeCodingGuardrailsCatalog_containsAll100RulesAcross10CategoriesAndExportsCleanly() {
        val allRules = VibeCodingGuardrailsCatalog.allRules
        assertEquals(100, allRules.size)
        assertEquals((1..100).toList(), allRules.map { it.number })

        GuardrailCategory.entries.forEach { category ->
            val catRules = VibeCodingGuardrailsCatalog.getRulesByCategory(category)
            assertEquals("Category $category must have 10 guardrails", 10, catRules.size)
            assertTrue(catRules.all { it.title.isNotBlank() && it.description.isNotBlank() })
        }

        val monolithMatches = VibeCodingGuardrailsCatalog.filterRules(null, "11")
        assertTrue(monolithMatches.any { it.number == 11 && it.title.contains("Hard File Line Limit") })

        val exportedClaude = VibeCodingGuardrailsCatalog.exportGuardrailsBlock(
            format = GuardrailExportFormat.CLAUDE_MD
        )
        assertTrue(exportedClaude.contains("CLAUDE.md"))
        assertTrue(exportedClaude.contains("1. **No Unrequested Refactoring**"))
        assertTrue(exportedClaude.contains("100. **State Assumptions Explicitly**"))
    }
}
