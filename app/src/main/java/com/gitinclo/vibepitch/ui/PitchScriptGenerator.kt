package com.gitinclo.vibepitch.ui

import com.gitinclo.vibepitch.data.model.AddOnOption
import com.gitinclo.vibepitch.data.model.NicheCatalog
import com.gitinclo.vibepitch.data.model.NicheKey
import com.gitinclo.vibepitch.data.model.ServicePackageOption
import com.gitinclo.vibepitch.domain.InputBoundsValidator
import kotlin.math.roundToInt

object QuotePricingCalculator {
    fun calculateEstimateAndDeposit(
        brand: ActiveDemoBrandState,
        calc: QuoteCalculatorState
    ): Pair<Int, Int> {
        val packages = NicheCatalog.getServicePackages(brand.niche)
        val selectedPkg: ServicePackageOption = packages.firstOrNull { it.id == calc.selectedServiceId }
            ?: packages.first()
        val addOns: List<AddOnOption> = NicheCatalog.getAddOns(brand.niche)
            .filter { calc.selectedAddOnIds.contains(it.id) }

        val unitSubtotal = selectedPkg.perUnitRate * calc.unitValue
        val addOnTotal = addOns.sumOf { it.price }
        val estimatedTotal = selectedPkg.basePrice + unitSubtotal + addOnTotal
        val depositAmount = ((estimatedTotal * brand.depositPercent) / 100.0).roundToInt()
        return estimatedTotal to depositAmount
    }
}

object PitchScriptGenerator {

    fun buildPitchScripts(
        nicheKey: NicheKey,
        archetype: OfferArchetype,
        businessName: String,
        contactName: String,
        setupFee: Int,
        monthlyRetainer: Int,
        depositPercent: Int,
        guardrailConfig: ScopeGuardrailConfig = ScopeGuardrailConfig(),
        websiteGapNote: String = nicheKey.microProblemSummary,
        sampleTotal: Int = 2000,
        sampleDeposit: Int = ((sampleTotal * depositPercent) / 100.0).roundToInt()
    ): GeneratedPitchScripts {
        val contactGreeting = contactName.ifBlank { "there" }
        val clampedSetup = InputBoundsValidator.clampSetupFeeOffer(setupFee)
        val clampedRetainer = InputBoundsValidator.clampMonthlyRetainerOffer(monthlyRetainer)
        val clampedDeposit = InputBoundsValidator.clampDepositPercent(depositPercent)

        val igFacebookDm = """
Hey $contactGreeting! Saw $businessName on here—your recent ${nicheKey.shortBadge} work looks great.

I noticed ${archetype.outcomeHook}. I went ahead and built a working ${archetype.title} sample with $businessName's name and colors on it so customers can see an instant ~$$sampleTotal ballpark quote and request a time slot in 60 seconds (saves you back-and-forth DMs and phone calls).

I can set it up for $businessName for a flat $$clampedSetup—and you only pay after you've tested it on your phone and it's live.

Mind if I send the link or a 60-second screen video so you can tap around on it?
        """.trimIndent()

        val marketplaceProposal = """
UPWORK / FIVERR / RESCUE PROPOSAL — ${businessName.uppercase()}
────────────────────────────────────────────────────────
Hi $contactGreeting,

I read your brief carefully—you don't just need raw code, you need a reliable ${archetype.title} that works cleanly on both mobile and desktop without security headaches.

I already built a working interactive prototype tailored for $businessName (${nicheKey.displayName}):
1. Solves the exact bottleneck: $websiteGapNote
2. Built-in Security Guardrails: Uses ${guardrailConfig.paymentGuardrail} and ${guardrailConfig.dataPrivacyGuardrail} so you never have to worry about exposed customer data or broken auth.
3. Fixed-Price Milestone: $$clampedSetup flat, delivered in 48 hours, including ${guardrailConfig.supportWindowDays} days of post-delivery bug fixes.

I recorded a 60-second walkthrough of your working sample—happy to share the link right now so you can test it on your phone before hiring!
        """.trimIndent()

        val loomScript = """
[0:00–0:20 • The Specific Observation]
"Hey $contactGreeting — I saw your work at $businessName and your reviews look awesome. I noticed that ${archetype.outcomeHook}."

[0:20–0:55 • The Outcome Demo (Not The Code)]
"Clients pay for outcomes, not complicated software. So I mocked up a working ${archetype.title} branded for $businessName. Watch how a user picks their ${nicheKey.shortBadge} option, slides their scope, and immediately sees a ~$$sampleTotal estimate with a $$sampleDeposit ($clampedDeposit%) deposit link via ${guardrailConfig.paymentGuardrail}."

[0:55–1:30 • The Fixed $$clampedSetup Starter Offer]
"If you want this live this week, I’ll connect it to your phone/email for a flat $$clampedSetup one-time fee—and include ${guardrailConfig.supportWindowDays} days of free post-delivery adjustments. Zero risk: you only pay after you test every button on your phone!"
        """.trimIndent()

        val outreachMsg = """
Subject: Simple ${archetype.title.lowercase()} for $businessName

Hey $contactGreeting,

Saw $businessName—great reputation. I noticed ${archetype.outcomeHook}, which usually means answering the same price and availability questions all evening.

I built a working ${archetype.title} sample branded for $businessName:
• ${archetype.deliverableBullet}
• Instant ~$$sampleTotal estimate + $$sampleDeposit ($clampedDeposit%) deposit option via ${guardrailConfig.paymentGuardrail}
• Routes leads via ${guardrailConfig.dataPrivacyGuardrail} straight to your phone/email

I charge a small fixed price ($$clampedSetup flat) to customize and launch it for you, payable only after it's live and tested on phone and desktop.

Want me to send over the sample link so you can try it?
        """.trimIndent()

        val offerSheet = """
PLAIN-ENGLISH SCOPE & SECURITY AGREEMENT — ${businessName.uppercase()}
────────────────────────────────────────────────────────
• Revenue Stream: ${nicheKey.revenueStream.title}
• Selected Deliverable: ${archetype.title}
• Operational Pain Point Solved: $websiteGapNote
• Fixed Project Price: $$clampedSetup flat ($0 upfront — payable ONLY after live phone & desktop testing)
• Optional Care Plan: $$clampedRetainer/mo (Hosting, price tweaks & ongoing updates)

WHAT THIS TOOL DOES (IN-SCOPE):
✓ ${archetype.deliverableBullet}
✓ Tested end-to-end across mobile phone and desktop viewports
✓ Offloads payments to: ${guardrailConfig.paymentGuardrail}
✓ Offloads data & intake to: ${guardrailConfig.dataPrivacyGuardrail}
✓ Authentication boundary: ${guardrailConfig.authGuardrail}

WHAT THIS TOOL DOES NOT DO (SAFETY & SCOPE BOUNDARIES):
✗ Never processes or stores raw credit card numbers, plain-text passwords, or unencrypted PII on custom code
✗ Does not replace full ERP, tax, or payroll accounting software

POST-DELIVERY MAINTENANCE AGREEMENT:
✓ Includes ${guardrailConfig.supportWindowDays} calendar days of free bug fixes and copy/price tweaks after delivery
✓ Any new features outside this scope are quoted as separate fixed-price mini-projects ($100–$300)
        """.trimIndent()

        val testimonialAndReferral = """
POST-DELIVERY TESTIMONIAL & REFERRAL REQUEST — ${businessName.uppercase()}
────────────────────────────────────────────────────────
Hey $contactGreeting! Awesome seeing your new ${archetype.title} live and tested for $businessName.

As a reminder, you have ${guardrailConfig.supportWindowDays} days of free tweaks and bug support included—just text me if you want any prices or wording adjusted.

Quick favor while everything is fresh:
1. Testimonial: Could you reply with 1–2 sentences on how this saves you time (e.g., fewer back-and-forth DMs/phone calls) that I can quote in my portfolio?
2. Local Referral: Do you know 1 or 2 other local business owners (cleaners, salons, gyms, or trades) who are still taking bookings by DM or spreadsheet? If you introduce us, I'll take $50 off their setup or give you 2 free months of maintenance!
        """.trimIndent()

        return GeneratedPitchScripts(
            igFacebookDmScript = igFacebookDm,
            marketplaceProposalScript = marketplaceProposal,
            coldOutreachMessage = outreachMsg,
            loomTeleprompterScript = loomScript,
            deRiskedOfferSheet = offerSheet,
            testimonialAndReferralScript = testimonialAndReferral
        )
    }

    fun generateAllSixScripts(
        nicheKey: NicheKey,
        archetype: OfferArchetype,
        businessName: String,
        contactName: String,
        setupFee: Int,
        monthlyRetainer: Int,
        depositPercent: Int,
        guardrailConfig: ScopeGuardrailConfig = ScopeGuardrailConfig(),
        websiteGapNote: String = nicheKey.microProblemSummary,
        sampleTotal: Int = 2000,
        sampleDeposit: Int = (sampleTotal * depositPercent) / 100
    ): List<PitchScriptItem> = buildPitchScripts(
        nicheKey = nicheKey,
        archetype = archetype,
        businessName = businessName,
        contactName = contactName,
        setupFee = setupFee,
        monthlyRetainer = monthlyRetainer,
        depositPercent = depositPercent,
        guardrailConfig = guardrailConfig,
        websiteGapNote = websiteGapNote,
        sampleTotal = sampleTotal,
        sampleDeposit = sampleDeposit
    ).toList()
}
