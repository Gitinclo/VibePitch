package com.gitinclo.vibepitch.data.local

import com.gitinclo.vibepitch.data.model.BusinessAccountProfile
import com.gitinclo.vibepitch.data.model.CapturedLead
import com.gitinclo.vibepitch.data.model.NicheCatalog
import com.gitinclo.vibepitch.data.model.NicheKey
import com.gitinclo.vibepitch.data.model.PipelineStage
import com.gitinclo.vibepitch.data.model.PlaybookTask
import com.gitinclo.vibepitch.data.model.ProspectProfile
import com.gitinclo.vibepitch.domain.InputBoundsValidator
import org.json.JSONArray
import org.json.JSONObject

/**
 * Pure JSON codec and schema-bound sanitizer for LocalGuestSessionManager.
 * Enforces identical field bounds to firestore.rules so every locally stored
 * guest record is guaranteed to pass Cloud Firestore validation upon upgrade.
 */
internal object GuestSessionJsonCodec {

    fun encodeBusinessProfile(profile: BusinessAccountProfile): String {
        val sanitizedAgency = InputBoundsValidator.sanitizeBusinessName(profile.agencyName)
        val sanitizedOwner = InputBoundsValidator.sanitizeContactName(profile.ownerName)
        val sanitizedTagline = InputBoundsValidator.sanitizeTagline(profile.tagline)
        return JSONObject().apply {
            put("agencyName", sanitizedAgency)
            put("ownerName", sanitizedOwner)
            put("businessEmail", profile.businessEmail.trim().take(120))
            put("businessPhone", profile.businessPhone.trim().take(40))
            put("primaryNicheKey", profile.primaryNicheKey)
            put("websiteUrl", profile.websiteUrl.trim().take(200))
            put("tagline", sanitizedTagline)
        }.toString()
    }

    fun decodeBusinessProfile(raw: String?, userId: String): BusinessAccountProfile? {
        if (raw.isNullOrBlank()) return null
        return try {
            val obj = JSONObject(raw)
            BusinessAccountProfile(
                id = "profile",
                userId = userId,
                agencyName = InputBoundsValidator.sanitizeBusinessName(obj.optString("agencyName", "")),
                ownerName = InputBoundsValidator.sanitizeContactName(obj.optString("ownerName", "")),
                businessEmail = obj.optString("businessEmail", "").take(120),
                businessPhone = obj.optString("businessPhone", "").take(40),
                primaryNicheKey = obj.optString("primaryNicheKey", NicheKey.HOME_TRADES.key),
                websiteUrl = obj.optString("websiteUrl", "").take(200),
                tagline = InputBoundsValidator.sanitizeTagline(obj.optString("tagline", "")),
                upgradedFromGuest = true
            )
        } catch (_: Exception) {
            null
        }
    }

    fun decodeProspects(raw: String?, guestUserId: String): List<ProspectProfile> {
        if (raw.isNullOrBlank()) {
            return NicheCatalog.buildDefaultProspects(guestUserId)
        }
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        ProspectProfile(
                            id = obj.optString("id", "guest_prospect_$i"),
                            userId = guestUserId,
                            businessName = InputBoundsValidator.sanitizeBusinessName(obj.optString("businessName", "")),
                            contactName = InputBoundsValidator.sanitizeContactName(obj.optString("contactName", "")),
                            nicheKey = obj.optString("nicheKey", NicheKey.HOME_TRADES.key),
                            websiteGapNote = InputBoundsValidator.sanitizeWebsiteGapNote(obj.optString("websiteGapNote", "")),
                            brandColorHex = obj.optString("brandColorHex", "#F97316"),
                            tagline = InputBoundsValidator.sanitizeTagline(obj.optString("tagline", "")),
                            depositPercent = InputBoundsValidator.clampDepositPercentLong(obj.optLong("depositPercent", 20L)),
                            setupFeeOffer = InputBoundsValidator.clampSetupFeeOfferLong(obj.optLong("setupFeeOffer", 250L)),
                            monthlyRetainerOffer = InputBoundsValidator.clampMonthlyRetainerOfferLong(obj.optLong("monthlyRetainerOffer", 35L)),
                            stage = obj.optString("stage", PipelineStage.DEMO_READY.key)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            NicheCatalog.buildDefaultProspects(guestUserId)
        }
    }

    fun encodeProspects(list: List<ProspectProfile>): String {
        val arr = JSONArray()
        list.forEach { p ->
            arr.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("businessName", InputBoundsValidator.sanitizeBusinessName(p.businessName))
                    put("contactName", InputBoundsValidator.sanitizeContactName(p.contactName))
                    put("nicheKey", p.nicheKey)
                    put("websiteGapNote", InputBoundsValidator.sanitizeWebsiteGapNote(p.websiteGapNote))
                    put("brandColorHex", p.brandColorHex)
                    put("tagline", InputBoundsValidator.sanitizeTagline(p.tagline))
                    put("depositPercent", InputBoundsValidator.clampDepositPercentLong(p.depositPercent))
                    put("setupFeeOffer", InputBoundsValidator.clampSetupFeeOfferLong(p.setupFeeOffer))
                    put("monthlyRetainerOffer", InputBoundsValidator.clampMonthlyRetainerOfferLong(p.monthlyRetainerOffer))
                    put("stage", p.stage)
                }
            )
        }
        return arr.toString()
    }

    fun decodeLeads(raw: String?, guestUserId: String): List<CapturedLead> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        CapturedLead(
                            id = obj.optString("id", "guest_lead_$i"),
                            userId = guestUserId,
                            prospectId = obj.optString("prospectId", ""),
                            prospectBusinessName = InputBoundsValidator.sanitizeBusinessName(obj.optString("prospectBusinessName", "")),
                            nicheKey = obj.optString("nicheKey", NicheKey.HOME_TRADES.key),
                            customerName = InputBoundsValidator.sanitizeContactName(obj.optString("customerName", "")),
                            customerPhone = obj.optString("customerPhone", ""),
                            customerEmail = obj.optString("customerEmail", ""),
                            preferredDate = obj.optString("preferredDate", ""),
                            selectedServiceName = obj.optString("selectedServiceName", ""),
                            selectedAddOns = obj.optString("selectedAddOns", ""),
                            unitQuantity = obj.optLong("unitQuantity", 1L),
                            estimatedTotal = obj.optLong("estimatedTotal", 0L),
                            depositAmount = obj.optLong("depositAmount", 0L),
                            notes = InputBoundsValidator.sanitizeNotes(obj.optString("notes", ""))
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun encodeLeads(list: List<CapturedLead>): String {
        val arr = JSONArray()
        list.forEach { lead ->
            arr.put(
                JSONObject().apply {
                    put("id", lead.id)
                    put("prospectId", lead.prospectId)
                    put("prospectBusinessName", InputBoundsValidator.sanitizeBusinessName(lead.prospectBusinessName))
                    put("nicheKey", lead.nicheKey)
                    put("customerName", InputBoundsValidator.sanitizeContactName(lead.customerName))
                    put("customerPhone", lead.customerPhone)
                    put("customerEmail", lead.customerEmail)
                    put("preferredDate", lead.preferredDate)
                    put("selectedServiceName", lead.selectedServiceName)
                    put("selectedAddOns", lead.selectedAddOns)
                    put("unitQuantity", lead.unitQuantity)
                    put("estimatedTotal", lead.estimatedTotal)
                    put("depositAmount", lead.depositAmount)
                    put("notes", InputBoundsValidator.sanitizeNotes(lead.notes))
                }
            )
        }
        return arr.toString()
    }

    fun decodePlaybookTasks(raw: String?, guestUserId: String): List<PlaybookTask> {
        if (raw.isNullOrBlank()) {
            return NicheCatalog.buildDefaultPlaybookTasks(guestUserId)
        }
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        PlaybookTask(
                            id = obj.optString("id", "task_$i"),
                            userId = guestUserId,
                            dayRange = obj.optString("dayRange", "Days 1–2"),
                            stepNumber = obj.optLong("stepNumber", 1L),
                            title = obj.optString("title", ""),
                            description = obj.optString("description", ""),
                            proTip = obj.optString("proTip", ""),
                            isCompleted = obj.optBoolean("isCompleted", false)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            NicheCatalog.buildDefaultPlaybookTasks(guestUserId)
        }
    }

    fun encodePlaybookTasks(list: List<PlaybookTask>): String {
        val arr = JSONArray()
        list.forEach { t ->
            arr.put(
                JSONObject().apply {
                    put("id", t.id)
                    put("dayRange", t.dayRange)
                    put("stepNumber", t.stepNumber)
                    put("title", t.title)
                    put("description", t.description)
                    put("proTip", t.proTip)
                    put("isCompleted", t.isCompleted)
                }
            )
        }
        return arr.toString()
    }
}
