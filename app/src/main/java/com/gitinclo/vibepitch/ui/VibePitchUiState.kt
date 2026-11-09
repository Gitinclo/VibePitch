package com.gitinclo.vibepitch.ui

import androidx.compose.runtime.Immutable
import com.gitinclo.vibepitch.data.model.CapturedLead
import com.gitinclo.vibepitch.data.model.NicheCatalog
import com.gitinclo.vibepitch.data.model.NicheKey
import com.gitinclo.vibepitch.data.model.RevenueStreamMode

@Immutable
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    @Immutable
    data class Success<T>(val data: T) : UiState<T>
    @Immutable
    data class Error(val message: String) : UiState<Nothing>
}

enum class AppTab(val route: String) {
    LIVE_DEMO("live_demo"),
    BRAND_STUDIO("brand_studio"),
    CRM_LEADS("crm_leads"),
    PLAYBOOK("playbook")
}

@Immutable
data class ActiveDemoBrandState(
    val prospectId: String = "seed_prospect_hvac",
    val businessName: String = "Apex Climate & HVAC",
    val contactName: String = "Mike Rivera",
    val niche: NicheKey = NicheKey.HOME_TRADES,
    val tagline: String = NicheKey.HOME_TRADES.defaultTagline,
    val brandColorHex: String = NicheKey.HOME_TRADES.defaultColorHex,
    val websiteGapNote: String = "Has a static 'Request a Quote' form with no instant ballpark estimator.",
    val depositPercent: Int = 20,
    val setupFeeOffer: Int = 250,
    val monthlyRetainerOffer: Int = 35
)

@Immutable
data class QuoteCalculatorState(
    val selectedServiceId: String = NicheCatalog.getServicePackages(NicheKey.HOME_TRADES).first().id,
    val unitValue: Int = NicheKey.HOME_TRADES.defaultUnitValue,
    val selectedAddOnIds: Set<String> = setOf("trades_thermostat"),
    val customerName: String = "",
    val customerPhone: String = "",
    val customerEmail: String = "",
    val preferredDate: String = "Tomorrow Morning (8am – 12pm)",
    val notes: String = "",
    val isSubmitting: Boolean = false,
    val latestSubmittedLead: CapturedLead? = null,
    val statusMessage: String? = null
)

enum class OfferArchetype(
    val title: String,
    val shortLabel: String,
    val revenueStream: RevenueStreamMode,
    val outcomeHook: String,
    val deliverableBullet: String
) {
    BOOKING_QUOTE_FORM(
        title = "Booking & Instant Quote Form",
        shortLabel = "Booking / Quote",
        revenueStream = RevenueStreamMode.LOCAL_BUSINESS,
        outcomeHook = "you currently take bookings and price inquiries by DM or phone tag",
        deliverableBullet = "Interactive mobile booking & instant ballpark quote page that saves you phone calls"
    ),
    CONTACT_PAYMENT_SITE(
        title = "Contact & Payment Links Hub",
        shortLabel = "Payment Hub",
        revenueStream = RevenueStreamMode.LOCAL_BUSINESS,
        outcomeHook = "customers have to ask where to pay deposits or find your service menu",
        deliverableBullet = "Clean mobile page with your service menu, contact intake, and trusted Stripe payment links"
    ),
    SPREADSHEET_REPLACEMENT(
        title = "Spreadsheet Replacement Tool",
        shortLabel = "Spreadsheet Tool",
        revenueStream = RevenueStreamMode.LOCAL_BUSINESS,
        outcomeHook = "quoting and tracking jobs on spreadsheets or paper notes slows down your team",
        deliverableBullet = "Simple custom calculator and job-intake tool that replaces manual spreadsheet math"
    ),
    UPWORK_MICRO_TOOL(
        title = "Shopify Tweak / Chrome Extension / Automation",
        shortLabel = "Upwork Micro-Tool",
        revenueStream = RevenueStreamMode.FREELANCE_MICRO_TOOLS,
        outcomeHook = "repetitive manual workflow steps and high-friction storefront pages are costing you conversions",
        deliverableBullet = "Focused landing page, Shopify conversion tweak, email auto-sorter, or Manifest V3 Chrome extension"
    ),
    AI_PROTOTYPE_RESCUE(
        title = "Broken AI App Security & Production Rescue",
        shortLabel = "AI App Rescue",
        revenueStream = RevenueStreamMode.PROTOTYPE_REMEDIATION,
        outcomeHook = "your AI-built prototype has login bugs, exposed API keys, or security gaps blocking launch",
        deliverableBullet = "End-to-end production audit, managed auth/Stripe handoff, security rules hardening, and viewport bug fixes"
    )
}

@Immutable
data class ScopeGuardrailConfig(
    val paymentGuardrail: String = "Stripe Payment Links / Native Checkout",
    val dataPrivacyGuardrail: String = "Google Forms / Zero-Trust Cloud Rules",
    val authGuardrail: String = "Managed Google OAuth (No Custom Passwords)",
    val supportWindowDays: Int = 14,
    val useExternalPaymentLinks: Boolean = true,
    val useExternalLeadForms: Boolean = true,
    val useManagedOAuth: Boolean = true
)

@Immutable
data class GeneratedPitchScripts(
    val igFacebookDmScript: String,
    val marketplaceProposalScript: String,
    val coldOutreachMessage: String,
    val loomTeleprompterScript: String,
    val deRiskedOfferSheet: String,
    val testimonialAndReferralScript: String
) {
    fun toList(): List<PitchScriptItem> = listOf(
        PitchScriptItem("IG / FB DM", igFacebookDmScript),
        PitchScriptItem("Upwork / Rescue", marketplaceProposalScript),
        PitchScriptItem("Email Pitch", coldOutreachMessage),
        PitchScriptItem("90s Loom", loomTeleprompterScript),
        PitchScriptItem("Scope & Security", deRiskedOfferSheet),
        PitchScriptItem("Testimonial & Referrals", testimonialAndReferralScript)
    )
}

@Immutable
data class PitchScriptItem(
    val title: String,
    val body: String
)
