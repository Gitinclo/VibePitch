package com.gitinclo.vibepitch.data.model

object NicheCatalog {
    fun getPreset(niche: NicheKey): NichePreset = NichePreset(
        nicheKey = niche,
        packages = getServicePackages(niche),
        addOns = getAddOns(niche),
        revenueStreamMode = niche.revenueStream
    )

    fun getServicePackages(niche: NicheKey): List<ServicePackageOption> = when (niche) {
        NicheKey.HOME_TRADES -> listOf(
            ServicePackageOption(
                id = "hvac_tuneup",
                title = "Seasonal HVAC Tune-Up & Diagnostic",
                subtitle = "21-point inspection, coil clean & refrigerant check",
                basePrice = 129,
                perUnitRate = 4,
                durationEstimate = "60–90 mins"
            ),
            ServicePackageOption(
                id = "hvac_minisplit",
                title = "Ductless Mini-Split Zone Install",
                subtitle = "High-efficiency inverter heat pump + wall mount",
                basePrice = 1450,
                perUnitRate = 45,
                durationEstimate = "1 Day Install",
                popularBadge = true
            ),
            ServicePackageOption(
                id = "trades_landscape",
                title = "Full Yard Clean-Up & Mulch Refresh",
                subtitle = "Edging, weed barrier, seasonal pruning & haul-away",
                basePrice = 380,
                perUnitRate = 18,
                durationEstimate = "4–6 hours"
            )
        )

        NicheKey.SALON_AND_CLEANERS -> listOf(
            ServicePackageOption(
                id = "salon_balayage",
                title = "Signature Color, Gloss & Blowout Package",
                subtitle = "Custom consultation, bond builder & silk press/style",
                basePrice = 165,
                perUnitRate = 25,
                durationEstimate = "2.5 hours",
                popularBadge = true
            ),
            ServicePackageOption(
                id = "clean_deep",
                title = "Deep Home Reset & Eco-Sanitization",
                subtitle = "Baseboards, kitchen appliances, baths & HEPA vacuum",
                basePrice = 140,
                perUnitRate = 35,
                durationEstimate = "3–4 hours"
            ),
            ServicePackageOption(
                id = "clean_moveout",
                title = "Move-In / Move-Out Inspection Guarantee Clean",
                subtitle = "Inside cabinets, oven, fridge & window tracks",
                basePrice = 240,
                perUnitRate = 45,
                durationEstimate = "5 hours"
            )
        )

        NicheKey.DENTAL_AND_TUTORS -> listOf(
            ServicePackageOption(
                id = "dental_whitening",
                title = "New Patient Exam, 3D Scan & Laser Whitening",
                subtitle = "Digital X-rays, gentle polish & in-office shade boost",
                basePrice = 195,
                perUnitRate = 30,
                durationEstimate = "75 mins",
                popularBadge = true
            ),
            ServicePackageOption(
                id = "tutor_sat_prep",
                title = "1-on-1 Exam & STEM Mastery Coaching",
                subtitle = "Diagnostic assessment, custom study sheet & weekly drills",
                basePrice = 120,
                perUnitRate = 55,
                durationEstimate = "60 min / session"
            ),
            ServicePackageOption(
                id = "dental_aligner",
                title = "Clear Aligner & Smile Simulation Consult",
                subtitle = "3D orthodontic preview + flexible payment plan breakdown",
                basePrice = 150,
                perUnitRate = 40,
                durationEstimate = "45 mins"
            )
        )

        NicheKey.AUTO_DETAILING -> listOf(
            ServicePackageOption(
                id = "detail_express",
                title = "Signature Mobile Interior + Foam Wash",
                subtitle = "Steam sanitization, leather conditioning & hand wax",
                basePrice = 145,
                perUnitRate = 30,
                durationEstimate = "2.5 hours"
            ),
            ServicePackageOption(
                id = "detail_ceramic",
                title = "1-Step Paint Enhancement & 3-Yr Ceramic",
                subtitle = "Swirl removal, clay bar decon & hydrophobic shield",
                basePrice = 490,
                perUnitRate = 65,
                durationEstimate = "5–6 hours",
                popularBadge = true
            ),
            ServicePackageOption(
                id = "detail_restoration",
                title = "Showroom Full Restoration Package",
                subtitle = "Deep extraction, ozone odor treatment & 2-step polish",
                basePrice = 340,
                perUnitRate = 50,
                durationEstimate = "4–5 hours"
            )
        )

        NicheKey.BOUTIQUE_FITNESS -> listOf(
            ServicePackageOption(
                id = "fit_kickstart",
                title = "1-on-1 Personal Training Transformation",
                subtitle = "Dedicated strength coach, custom macros & weekly check-in",
                basePrice = 160,
                perUnitRate = 45,
                durationEstimate = "50 min / session",
                popularBadge = true
            ),
            ServicePackageOption(
                id = "fit_semi_private",
                title = "Small-Group Strength & Conditioning",
                subtitle = "Max 6 athletes per pod with heart-rate telemetry",
                basePrice = 95,
                perUnitRate = 22,
                durationEstimate = "45 min / class"
            ),
            ServicePackageOption(
                id = "fit_mobility",
                title = "Athletic Mobility & Metabolic Reset",
                subtitle = "Corrective movement screen + guided recovery sessions",
                basePrice = 120,
                perUnitRate = 30,
                durationEstimate = "45 min / session"
            )
        )

        NicheKey.ECOMMERCE_SHOPIFY -> listOf(
            ServicePackageOption(
                id = "shopify_bundle_tweak",
                title = "Shopify Product Bundle & Sticky Cart Upgrade",
                subtitle = "Slide-out upsell drawer + mobile speed optimization",
                basePrice = 180,
                perUnitRate = 35,
                durationEstimate = "48-Hour Delivery",
                popularBadge = true
            ),
            ServicePackageOption(
                id = "micro_chrome_ext",
                title = "Custom Chrome Extension / Lead Scraper Utility",
                subtitle = "Manifest V3 popup + one-click Google Sheets export",
                basePrice = 220,
                perUnitRate = 45,
                durationEstimate = "3 Days"
            ),
            ServicePackageOption(
                id = "auto_email_sorter",
                title = "Inbox Auto-Sorter & Instant Lead Webhook",
                subtitle = "Routes customer inquiries to Slack/SMS & Google Forms",
                basePrice = 150,
                perUnitRate = 30,
                durationEstimate = "24–48 Hours"
            )
        )

        NicheKey.SAAS_AI_REMEDIATION -> listOf(
            ServicePackageOption(
                id = "ai_rescue_sprint",
                title = "Broken AI Prototype Production Rescue Sprint",
                subtitle = "Fix auth loops, broken state & mobile layout bugs",
                basePrice = 250,
                perUnitRate = 55,
                durationEstimate = "48–72 Hours",
                popularBadge = true
            ),
            ServicePackageOption(
                id = "ai_security_hardening",
                title = "Security & Payment Guardrail Hardening",
                subtitle = "Remove exposed keys, wire Stripe Links & database rules",
                basePrice = 290,
                perUnitRate = 60,
                durationEstimate = "2–3 Days"
            ),
            ServicePackageOption(
                id = "ai_deploy_finish",
                title = "Last-20% Polish & Cloud Deployment Handoff",
                subtitle = "End-to-end mobile/desktop QA + clean hosting setup",
                basePrice = 195,
                perUnitRate = 45,
                durationEstimate = "48 Hours"
            )
        )
    }

    fun getAddOns(niche: NicheKey): List<AddOnOption> = when (niche) {
        NicheKey.HOME_TRADES -> listOf(
            AddOnOption("trades_priority", "Same-Week Priority Dispatch", 65, "Guaranteed morning arrival window"),
            AddOnOption("trades_thermostat", "Smart Wi-Fi Thermostat Install", 185, "Includes app setup & wiring"),
            AddOnOption("trades_filter", "HEPA Media Filter 1-Year Pack", 95, "MERV-13 allergen filtration"),
            AddOnOption("trades_warranty", "2-Year Labor Peace-of-Mind Plan", 140, "Zero service call fees")
        )

        NicheKey.SALON_AND_CLEANERS -> listOf(
            AddOnOption("salon_olaplex", "Deep Bond Repair / Eco Green Supplies", 35, "Premium treatment upgrade"),
            AddOnOption("clean_fridge_oven", "Inside Fridge & Oven Deep Detail", 55, "Degrease & steam sanitize"),
            AddOnOption("clean_windows", "Interior Window & Mirror Polish", 45, "Streak-free microfiber finish"),
            AddOnOption("salon_weekend", "Priority Weekend Morning Slot", 30, "Reserved Saturday 9am–12pm")
        )

        NicheKey.DENTAL_AND_TUTORS -> listOf(
            AddOnOption("dental_takehome", "Custom Take-Home Whitening Trays", 75, "Molded maintenance kit"),
            AddOnOption("tutor_mock_exam", "Full Graded Practice Exam + Parent Report", 50, "Detailed score breakdown"),
            AddOnOption("tutor_flashcards", "Custom Spaced-Repetition Study Deck", 35, "Digital & printable review"),
            AddOnOption("dental_evening", "After-School / Evening Priority Slot", 25, "4pm–7pm appointment window")
        )

        NicheKey.AUTO_DETAILING -> listOf(
            AddOnOption("detail_pethair", "Heavy Pet Hair & Sand Extraction", 55, "Multi-stage pneumatic brush removal"),
            AddOnOption("detail_engine", "Engine Bay Steam & Satin Dress", 65, "Safe degreasing & plastic protection"),
            AddOnOption("detail_headlight", "UV Headlight Lens Restoration", 75, "Wet sand, polish & ceramic seal"),
            AddOnOption("detail_windshield", "Rain-Repellent Glass Coating", 45, "All exterior windows & mirrors")
        )

        NicheKey.BOUTIQUE_FITNESS -> listOf(
            AddOnOption("fit_inbody", "3D InBody Composition & Vo2 Scan", 45, "Baseline muscle & metabolic report"),
            AddOnOption("fit_nutrition", "Custom 30-Day High-Protein Meal Blueprint", 75, "Grocery lists + restaurant swaps"),
            AddOnOption("fit_sauna", "Unlimited Infrared Sauna & Cold Plunge", 60, "Post-workout recovery lounge pass"),
            AddOnOption("fit_form_video", "Async Form-Check Video Coaching", 40, "24hr trainer feedback via mobile")
        )

        NicheKey.ECOMMERCE_SHOPIFY -> listOf(
            AddOnOption("ecom_stripe_link", "Stripe Payment Link & Webhook Setup", 65, "Zero custom card-storage risk"),
            AddOnOption("ecom_sheets_sync", "Live Google Sheets / Airtable Sync", 55, "Replaces manual CSV exports"),
            AddOnOption("ecom_mobile_qa", "Cross-Browser Mobile & Desktop QA Pass", 45, "iOS Safari + Android Chrome verified"),
            AddOnOption("ecom_loom_docs", "Loom Handover & 14-Day Bug Support", 40, "Plain-English owner training video")
        )

        NicheKey.SAAS_AI_REMEDIATION -> listOf(
            AddOnOption("ai_auth_guard", "Managed Google OAuth Replacement", 85, "Replaces broken custom auth code"),
            AddOnOption("ai_rules_lock", "Zero-Trust Database Security Rules", 75, "Prevents unauthorized data reads/writes"),
            AddOnOption("ai_secret_scrub", "API Key & Environment Secret Scrub", 50, "Moves hardcoded keys to server secrets"),
            AddOnOption("ai_stripe_handoff", "Trusted Stripe Checkout Integration", 70, "Offloads all PCI/payment compliance")
        )
    }

    fun buildDefaultProspects(userId: String): List<ProspectProfile> = listOf(
        ProspectProfile(
            id = "seed_prospect_salon",
            userId = userId,
            businessName = "Lumina Salon & EcoClean",
            contactName = "Chloe Bennett",
            nicheKey = NicheKey.SALON_AND_CLEANERS.key,
            websiteGapNote = "Takes all bookings by Instagram DM; spends 2 hours a night replying to price questions.",
            brandColorHex = "#EC4899",
            tagline = "Skip the DM Tag — See Your Price & Book Your Slot Online",
            depositPercent = 20L,
            setupFeeOffer = 250L,
            monthlyRetainerOffer = 35L,
            stage = PipelineStage.DEMO_READY.key
        ),
        ProspectProfile(
            id = "seed_prospect_hvac",
            userId = userId,
            businessName = "Apex Climate & HVAC",
            contactName = "Mike Rivera",
            nicheKey = NicheKey.HOME_TRADES.key,
            websiteGapNote = "Has a static 'Request a Quote' form with no instant ballpark estimator.",
            brandColorHex = "#F97316",
            tagline = "Instant AC & Mini-Split Estimate in 60 Seconds",
            depositPercent = 20L,
            setupFeeOffer = 250L,
            monthlyRetainerOffer = 35L,
            stage = PipelineStage.DEMO_READY.key
        ),
        ProspectProfile(
            id = "seed_prospect_shopify",
            userId = userId,
            businessName = "Veloce D2C & Chrome Tools",
            contactName = "Jordan Wu",
            nicheKey = NicheKey.ECOMMERCE_SHOPIFY.key,
            websiteGapNote = "Upwork lead needing a spreadsheet-replacement calculator & Shopify bundle drawer.",
            brandColorHex = "#8B5CF6",
            tagline = "Fixed-Price Shopify Tweaks, Automations & Chrome Extensions",
            depositPercent = 50L,
            setupFeeOffer = 350L,
            monthlyRetainerOffer = 45L,
            stage = PipelineStage.LOOM_SENT.key
        ),
        ProspectProfile(
            id = "seed_prospect_airescue",
            userId = userId,
            businessName = "LaunchPad AI Prototype",
            contactName = "Samir Patel",
            nicheKey = NicheKey.SAAS_AI_REMEDIATION.key,
            websiteGapNote = "Built an AI prototype in Lovable/Cursor that breaks on sign-in and exposes API keys.",
            brandColorHex = "#EAB308",
            tagline = "Turn Your Broken AI Prototype Into a Secure Production App",
            depositPercent = 50L,
            setupFeeOffer = 450L,
            monthlyRetainerOffer = 50L,
            stage = PipelineStage.AUDITED.key
        )
    )

    fun buildDefaultPlaybookTasks(userId: String): List<PlaybookTask> = listOf(
        PlaybookTask(
            id = "step1_niche",
            userId = userId,
            dayRange = "Days 1–2",
            stepNumber = 1L,
            title = "Pick 1 Niche & Build 1 Working Sample Project",
            description = "Choose Salons, Cleaners, Dentists, Tutors, Gyms, Trades, or AI Prototype Rescue. Build a single sample that solves one specific pain point.",
            proTip = "Clients pay for the outcome (fewer phone calls & DMs), not raw code.",
            isCompleted = true
        ),
        PlaybookTask(
            id = "step1_audit",
            userId = userId,
            dayRange = "Days 1–2",
            stepNumber = 1L,
            title = "Find 10–20 Prospects on Instagram, Facebook, or Upwork",
            description = "Look for local businesses taking bookings by DM or spreadsheet, or founders asking for help fixing broken AI-built prototypes.",
            proTip = "Log each prospect in the CRM tab with the exact operational bottleneck you spotted.",
            isCompleted = true
        ),
        PlaybookTask(
            id = "step2_scope",
            userId = userId,
            dayRange = "Days 3–5",
            stepNumber = 2L,
            title = "Define Plain-English Scope & Security Guardrails",
            description = "Isolate one pain point, state clearly what the tool does and doesn't do, and offload payments/PII to Stripe or Google Forms.",
            proTip = "Never write custom logic to store raw credit cards or passwords—use trusted providers.",
            isCompleted = true
        ),
        PlaybookTask(
            id = "step3_outreach",
            userId = userId,
            dayRange = "Days 6–10",
            stepNumber = 3L,
            title = "Send 10–20 Targeted DM / Loom Outreach Messages",
            description = "Message prospects something specific: 'I noticed you take bookings by DM. I built a simple booking page sample for you.'",
            proTip = "Use a small fixed starter price ($100–$500) with $0 upfront until they test it live.",
            isCompleted = false
        ),
        PlaybookTask(
            id = "step3_verify",
            userId = userId,
            dayRange = "Days 6–10",
            stepNumber = 3L,
            title = "Verify Every Flow on Phone & Desktop Viewports",
            description = "Run through the Pre-Ship QA & AI Remediation Checklist on both mobile and desktop before handing the link to the client.",
            proTip = "You are responsible for what you ship—test every button, slider, and form edge case yourself.",
            isCompleted = false
        ),
        PlaybookTask(
            id = "step4_close",
            userId = userId,
            dayRange = "Days 11–14",
            stepNumber = 4L,
            title = "Deliver Fast & Lock In Post-Delivery Support Terms",
            description = "Hand over the live tool with a plain-English agreement on who fixes bugs after delivery and optional $35/mo maintenance.",
            proTip = "Fast delivery builds immediate trust and turns a $250 starter gig into recurring work.",
            isCompleted = false
        ),
        PlaybookTask(
            id = "step4_referrals",
            userId = userId,
            dayRange = "Days 11–14",
            stepNumber = 4L,
            title = "Capture Testimonial & Ask for 2 Local Business Referrals",
            description = "Use the Testimonial & Referral tab to ask your happy client for a 2-sentence review and 1–2 introductions to other business owners.",
            proTip = "One happy salon, cleaner, or trade owner knows five other local business owners.",
            isCompleted = false
        )
    )
}
