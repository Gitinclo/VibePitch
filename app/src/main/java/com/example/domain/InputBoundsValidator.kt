package com.example.domain

data class NormalizedProspectPayload(
    val businessName: String,
    val contactName: String,
    val websiteGapNote: String,
    val tagline: String,
    val notes: String,
    val depositPercent: Int,
    val setupFeeOffer: Int,
    val monthlyRetainerOffer: Int
)

object InputBoundsValidator {
    const val MAX_BUSINESS_NAME_LEN = 100
    const val MAX_CONTACT_NAME_LEN = 100
    const val MAX_WEBSITE_GAP_NOTE_LEN = 300
    const val MAX_TAGLINE_LEN = 150
    const val MAX_NOTES_LEN = 500

    const val MIN_DEPOSIT_PERCENT = 0
    const val MAX_DEPOSIT_PERCENT = 100

    const val MIN_SETUP_FEE = 0
    const val MAX_SETUP_FEE = 50_000

    const val MIN_MONTHLY_RETAINER = 0
    const val MAX_MONTHLY_RETAINER = 5_000

    fun sanitizeBusinessName(input: String): String =
        input.trim().take(MAX_BUSINESS_NAME_LEN)

    fun sanitizeContactName(input: String): String =
        input.trim().take(MAX_CONTACT_NAME_LEN)

    fun sanitizeWebsiteGapNote(input: String): String =
        input.trim().take(MAX_WEBSITE_GAP_NOTE_LEN)

    fun sanitizeTagline(input: String): String =
        input.trim().take(MAX_TAGLINE_LEN)

    fun sanitizeNotes(input: String): String =
        input.trim().take(MAX_NOTES_LEN)

    fun clampDepositPercent(value: Int): Int =
        value.coerceIn(MIN_DEPOSIT_PERCENT, MAX_DEPOSIT_PERCENT)

    fun clampSetupFeeOffer(value: Int): Int =
        value.coerceIn(MIN_SETUP_FEE, MAX_SETUP_FEE)

    fun clampMonthlyRetainerOffer(value: Int): Int =
        value.coerceIn(MIN_MONTHLY_RETAINER, MAX_MONTHLY_RETAINER)

    fun clampDepositPercentLong(value: Long): Long =
        value.coerceIn(MIN_DEPOSIT_PERCENT.toLong(), MAX_DEPOSIT_PERCENT.toLong())

    fun clampSetupFeeOfferLong(value: Long): Long =
        value.coerceIn(MIN_SETUP_FEE.toLong(), MAX_SETUP_FEE.toLong())

    fun clampMonthlyRetainerOfferLong(value: Long): Long =
        value.coerceIn(MIN_MONTHLY_RETAINER.toLong(), MAX_MONTHLY_RETAINER.toLong())

    fun normalizeProspectInput(
        businessName: String,
        contactName: String,
        websiteGapNote: String,
        tagline: String = "",
        notes: String = "",
        depositPercent: Int,
        setupFeeOffer: Int,
        monthlyRetainerOffer: Int
    ): NormalizedProspectPayload {
        return NormalizedProspectPayload(
            businessName = sanitizeBusinessName(businessName).ifEmpty { "Untitled Business" },
            contactName = sanitizeContactName(contactName),
            websiteGapNote = sanitizeWebsiteGapNote(websiteGapNote),
            tagline = sanitizeTagline(tagline),
            notes = sanitizeNotes(notes),
            depositPercent = clampDepositPercent(depositPercent),
            setupFeeOffer = clampSetupFeeOffer(setupFeeOffer),
            monthlyRetainerOffer = clampMonthlyRetainerOffer(monthlyRetainerOffer)
        )
    }

    fun normalizeBusinessProfileTagline(tagline: String): String =
        sanitizeTagline(tagline)
}
