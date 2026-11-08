package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class RevenueStreamMode(
    val key: String,
    val title: String,
    val shortBadge: String,
    val subtitle: String,
    val priceGuidance: String
) {
    LOCAL_BUSINESS(
        key = "LOCAL_BUSINESS",
        title = "Local Business Solutions",
        shortBadge = "Local Business",
        subtitle = "Booking pages, instant quote calculators, payment portals & spreadsheet replacements",
        priceGuidance = "$150–$500 flat setup + $35/mo optional care"
    ),
    FREELANCE_MICRO_TOOLS(
        key = "FREELANCE_MICRO_TOOLS",
        title = "Freelance & Micro-Tools (Upwork / Fiverr)",
        shortBadge = "Upwork / Micro-Tools",
        subtitle = "Landing pages, Shopify conversion tweaks, email automations & Chrome extensions",
        priceGuidance = "$100–$450 fixed-price marketplace milestones"
    ),
    PROTOTYPE_REMEDIATION(
        key = "PROTOTYPE_REMEDIATION",
        title = "Broken AI Prototype Remediation",
        shortBadge = "AI App Rescue",
        subtitle = "Audit, debug, harden security & finish broken AI-built prototypes for production",
        priceGuidance = "$250–$500 production rescue & security hardening"
    )
}

enum class NicheKey(
    val key: String,
    val displayName: String,
    val shortBadge: String,
    val revenueStream: RevenueStreamMode,
    val defaultBusinessName: String,
    val defaultContactName: String,
    val defaultTagline: String,
    val defaultColorHex: String,
    val unitLabel: String,
    val unitMin: Int,
    val unitMax: Int,
    val unitStep: Int,
    val defaultUnitValue: Int,
    val microProblemSummary: String
) {
    HOME_TRADES(
        key = "HOME_TRADES",
        displayName = "HVAC & Home Trades",
        shortBadge = "Trades / HVAC",
        revenueStream = RevenueStreamMode.LOCAL_BUSINESS,
        defaultBusinessName = "Apex Climate & HVAC",
        defaultContactName = "Mike Rivera",
        defaultTagline = "Instant System & Service Estimate in 60 Seconds",
        defaultColorHex = "#F97316",
        unitLabel = "Home Size / Coverage (x100 sq ft)",
        unitMin = 8,
        unitMax = 45,
        unitStep = 1,
        defaultUnitValue = 18,
        microProblemSummary = "Visitors currently wait 24–48 hours on a static 'Contact Us' form just to get a ballpark HVAC or landscaping price."
    ),
    SALON_AND_CLEANERS(
        key = "SALON_AND_CLEANERS",
        displayName = "Salons, Spas & Home Cleaners",
        shortBadge = "Salons & Cleaners",
        revenueStream = RevenueStreamMode.LOCAL_BUSINESS,
        defaultBusinessName = "Lumina Salon & EcoClean",
        defaultContactName = "Chloe Bennett",
        defaultTagline = "Skip the DM Tag — See Your Price & Book Your Slot Online",
        defaultColorHex = "#EC4899",
        unitLabel = "Service Duration / Home Bedrooms Tier (1–6)",
        unitMin = 1,
        unitMax = 6,
        unitStep = 1,
        defaultUnitValue = 3,
        microProblemSummary = "Owner spends hours every evening answering 'How much and when are you free?' in Instagram & Facebook DMs."
    ),
    DENTAL_AND_TUTORS(
        key = "DENTAL_AND_TUTORS",
        displayName = "Dentists, MedSpas & Tutors",
        shortBadge = "Dentists & Tutors",
        revenueStream = RevenueStreamMode.LOCAL_BUSINESS,
        defaultBusinessName = "BrightSmile & Prep Academy",
        defaultContactName = "Dr. Aris Thorne",
        defaultTagline = "Match Your Care or Tutoring Plan & Reserve Your Consultation",
        defaultColorHex = "#06B6D4",
        unitLabel = "Sessions / Treatment Visits Needed",
        unitMin = 1,
        unitMax = 12,
        unitStep = 1,
        defaultUnitValue = 4,
        microProblemSummary = "Patients and parents drop off because scheduling an assessment or tutoring package requires phone tag."
    ),
    AUTO_DETAILING(
        key = "AUTO_DETAILING",
        displayName = "Mobile Auto Detailing",
        shortBadge = "Auto Detailing",
        revenueStream = RevenueStreamMode.LOCAL_BUSINESS,
        defaultBusinessName = "Obsidian Mobile Detailing",
        defaultContactName = "Devon Brooks",
        defaultTagline = "Custom Ceramic & Detailing Quote + Mobile Booking",
        defaultColorHex = "#3B82F6",
        unitLabel = "Vehicle Size & Condition Tier (1=Compact, 4=Full SUV/Truck)",
        unitMin = 1,
        unitMax = 4,
        unitStep = 1,
        defaultUnitValue = 2,
        microProblemSummary = "Car owners play back-and-forth SMS tag asking 'How much for an SUV interior detail?' before paying a deposit."
    ),
    BOUTIQUE_FITNESS(
        key = "BOUTIQUE_FITNESS",
        displayName = "Boutique Fitness & PT",
        shortBadge = "Fitness / Gym",
        revenueStream = RevenueStreamMode.LOCAL_BUSINESS,
        defaultBusinessName = "Pulse Foundry Athletics",
        defaultContactName = "Elena Vance",
        defaultTagline = "Find Your Training Program & Lock In Your Intro Session",
        defaultColorHex = "#10B981",
        unitLabel = "Coaching Sessions Per Month",
        unitMin = 4,
        unitMax = 16,
        unitStep = 2,
        defaultUnitValue = 8,
        microProblemSummary = "Prospective members leave the website because pricing and program matching require a manual phone consultation."
    ),
    ECOMMERCE_SHOPIFY(
        key = "ECOMMERCE_SHOPIFY",
        displayName = "Shopify, Automations & Micro-Tools",
        shortBadge = "Shopify / Upwork",
        revenueStream = RevenueStreamMode.FREELANCE_MICRO_TOOLS,
        defaultBusinessName = "Veloce D2C & Chrome Tools",
        defaultContactName = "Jordan Wu",
        defaultTagline = "Fixed-Price Shopify Tweaks, Automations & Chrome Extensions",
        defaultColorHex = "#8B5CF6",
        unitLabel = "Workflow Steps / Storefront Components (1–10)",
        unitMin = 1,
        unitMax = 10,
        unitStep = 1,
        defaultUnitValue = 3,
        microProblemSummary = "Team wastes hours on repetitive email sorting, manual spreadsheet exports, or high-friction Shopify product pages."
    ),
    SAAS_AI_REMEDIATION(
        key = "SAAS_AI_REMEDIATION",
        displayName = "Broken AI App Remediation",
        shortBadge = "AI App Fix",
        revenueStream = RevenueStreamMode.PROTOTYPE_REMEDIATION,
        defaultBusinessName = "LaunchPad AI Prototype",
        defaultContactName = "Samir Patel",
        defaultTagline = "Turn Your Broken AI Prototype Into a Secure Production App",
        defaultColorHex = "#EAB308",
        unitLabel = "Broken Flows / Modules to Harden (1–8)",
        unitMin = 1,
        unitMax = 8,
        unitStep = 1,
        defaultUnitValue = 3,
        microProblemSummary = "Prototype built in Cursor/Lovable/Bolt breaks on login, exposes API keys, or fails on mobile viewports."
    );

    companion object {
        fun fromKey(key: String): NicheKey =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: HOME_TRADES
    }
}

enum class PipelineStage(
    val key: String,
    val label: String,
    val stepIndex: Int
) {
    AUDITED("AUDITED", "1. Site Audited", 0),
    DEMO_READY("DEMO_READY", "2. Demo Branded", 1),
    LOOM_SENT("LOOM_SENT", "3. Pitch / DM Sent", 2),
    TESTING_LIVE("TESTING_LIVE", "4. Live QA Testing", 3),
    CLOSED_WON("CLOSED_WON", "5. Closed ($150–$500)", 4);

    fun nextStage(): PipelineStage {
        val all = entries
        return all[(stepIndex + 1) % all.size]
    }

    companion object {
        fun fromKey(key: String): PipelineStage =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: AUDITED
    }
}

data class ServicePackageOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val basePrice: Int,
    val perUnitRate: Int,
    val durationEstimate: String,
    val popularBadge: Boolean = false
)

data class AddOnOption(
    val id: String,
    val name: String,
    val price: Int,
    val description: String
)

data class NichePreset(
    val nicheKey: NicheKey,
    val packages: List<ServicePackageOption>,
    val addOns: List<AddOnOption>,
    val revenueStreamMode: RevenueStreamMode
)

@Keep
@IgnoreExtraProperties
data class BusinessAccountProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val agencyName: String = "",
    val ownerName: String = "",
    val businessEmail: String = "",
    val businessPhone: String = "",
    val primaryNicheKey: String = NicheKey.HOME_TRADES.key,
    val websiteUrl: String = "",
    val tagline: String = "",
    val upgradedFromGuest: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class GuestLifecycleStatus(
    val isGuestActive: Boolean = false,
    val startedAtMillis: Long = 0L,
    val currentDayNumber: Int = 1,
    val daysRemaining: Int = 30,
    val shouldShowDay20Prompt: Boolean = false,
    val isGuestExpired: Boolean = false
) {
    val dayCount: Int
        get() = currentDayNumber

    val shouldShowSoftPrompt: Boolean
        get() = shouldShowDay20Prompt

    val isHardGated: Boolean
        get() = isGuestExpired
}

@Keep
@IgnoreExtraProperties
data class ProspectProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val businessName: String = "",
    val contactName: String = "",
    val nicheKey: String = NicheKey.HOME_TRADES.key,
    val websiteGapNote: String = "",
    val brandColorHex: String = "#F97316",
    val tagline: String = "",
    val depositPercent: Long = 20L,
    val setupFeeOffer: Long = 250L,
    val monthlyRetainerOffer: Long = 35L,
    val stage: String = PipelineStage.DEMO_READY.key,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CapturedLead(
    @DocumentId val id: String = "",
    val userId: String = "",
    val prospectId: String = "",
    val prospectBusinessName: String = "",
    val nicheKey: String = NicheKey.HOME_TRADES.key,
    val customerName: String = "",
    val customerPhone: String = "",
    val customerEmail: String = "",
    val preferredDate: String = "",
    val selectedServiceName: String = "",
    val selectedAddOns: String = "",
    val unitQuantity: Long = 1L,
    val estimatedTotal: Long = 0L,
    val depositAmount: Long = 0L,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val businessName: String
        get() = prospectBusinessName
}

@Keep
@IgnoreExtraProperties
data class PlaybookTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dayRange: String = "Days 1-2",
    val stepNumber: Long = 1L,
    val title: String = "",
    val description: String = "",
    val proTip: String = "",
    val isCompleted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
