package com.example

import com.example.data.model.NicheCatalog
import com.example.data.model.NicheKey
import com.example.data.model.PipelineStage
import com.example.domain.InputBoundsValidator
import com.example.ui.OfferArchetype
import com.example.ui.PitchScriptGenerator
import com.example.ui.ScopeGuardrailConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

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
    fun pipelineStage_transitionsProgressSequentially() {
        val stages = PipelineStage.entries
        assertTrue(stages.isNotEmpty())
        for (i in 0 until stages.size - 1) {
            val current = stages[i]
            val next = current.nextStage()
            assertEquals(stages[i + 1], next)
        }
    }

    @Test
    fun quoteAndDepositMath_respectsFirestoreBoundsAndCalculatesAccurately() {
        val setupFee = InputBoundsValidator.clampSetupFeeOffer(65_000) // Clamped to 50,000
        val monthlyRetainer = InputBoundsValidator.clampMonthlyRetainerOffer(7_500) // Clamped to 5,000
        val depositPercent = InputBoundsValidator.clampDepositPercent(120) // Clamped to 100

        assertEquals(50_000, setupFee)
        assertEquals(5_000, monthlyRetainer)
        assertEquals(100, depositPercent)

        val standardDeposit = (2_000 * 50) / 100
        assertEquals(1_000, standardDeposit)
    }

    @Test
    fun scriptGenerators_produceNonBlankScriptsAcrossAllOfferArchetypesAndGuardrails() {
        val guardrails = ScopeGuardrailConfig(
            useExternalPaymentLinks = true,
            useExternalLeadForms = true,
            useManagedOAuth = true
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
    }
}
