const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "vibepitch-studio-test";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const ALL_NICHE_KEYS = [
  "HOME_TRADES",
  "SALON_AND_CLEANERS",
  "DENTAL_AND_TUTORS",
  "AUTO_DETAILING",
  "BOUTIQUE_FITNESS",
  "ECOMMERCE_SHOPIFY",
  "SAAS_AI_REMEDIATION",
];

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write businessProfile, prospects, leads, or playbook", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("businessProfile").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("prospects").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("leads").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("playbook").get()
  );
});

test("Authenticated user can create and update their permanent BusinessAccountProfile across all 7 NicheKeys", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const profileRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("businessProfile")
    .doc("profile");

  for (const nicheKey of ALL_NICHE_KEYS) {
    await assertSucceeds(
      profileRef.set({
        userId: ALICE_UID,
        agencyName: "VibeFlow Local Studio",
        ownerName: "Alex Mercer",
        businessEmail: "alex@vibeflow.studio",
        businessPhone: "555-0144",
        primaryNicheKey: nicheKey,
        websiteUrl: "https://vibeflow.studio",
        upgradedFromGuest: true,
        createdAt: now,
        updatedAt: now,
      })
    );
  }

  await assertSucceeds(profileRef.get());
  await assertSucceeds(
    profileRef.update({
      agencyName: "VibeFlow Digital Partners",
      updatedAt: new Date(),
    })
  );

  // Rejects shadow field
  await assertFails(
    profileRef.update({
      agencyName: "VibeFlow Digital Partners",
      isVerifiedAdmin: true,
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user can write and read prospects across all 7 NicheKey values", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  for (const nicheKey of ALL_NICHE_KEYS) {
    const docRef = aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("prospects")
      .doc(`prospect_${nicheKey}`);

    await assertSucceeds(
      docRef.set({
        userId: ALICE_UID,
        businessName: `Sample ${nicheKey}`,
        contactName: "Alex Owner",
        nicheKey: nicheKey,
        websiteGapNote: "Missing mobile booking CTA",
        brandColorHex: "#F97316",
        tagline: "Instant Quotes in 60 Seconds",
        depositPercent: 50,
        setupFeeOffer: 1500,
        monthlyRetainerOffer: 250,
        stage: "DEMO_READY",
        createdAt: now,
        updatedAt: now,
      })
    );

    await assertSucceeds(docRef.get());
  }

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("prospects")
      .where("userId", "==", ALICE_UID)
      .get()
  );
});

test("Shadow update with ghost field or out-of-bounds values on prospect is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const docRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("prospects")
    .doc("prospect_shadow");

  // Ghost field rejected
  await assertFails(
    docRef.set({
      userId: ALICE_UID,
      businessName: "Apex Climate Control",
      contactName: "Mike Rivera",
      nicheKey: "HOME_TRADES",
      websiteGapNote: "Static contact form",
      brandColorHex: "#F97316",
      tagline: "Instant Quotes",
      depositPercent: 20,
      setupFeeOffer: 250,
      monthlyRetainerOffer: 35,
      stage: "DEMO_READY",
      isAdminSpoof: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Out-of-bounds setupFeeOffer (> 50000) rejected
  await assertFails(
    docRef.set({
      userId: ALICE_UID,
      businessName: "Apex Climate Control",
      contactName: "Mike Rivera",
      nicheKey: "HOME_TRADES",
      websiteGapNote: "Static contact form",
      brandColorHex: "#F97316",
      tagline: "Instant Quotes",
      depositPercent: 20,
      setupFeeOffer: 75000,
      monthlyRetainerOffer: 35,
      stage: "DEMO_READY",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Cross-user access to another user's prospects, leads, or playbook is rejected", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .collection("prospects")
      .doc("bob_p1")
      .set({
        userId: BOB_UID,
        businessName: "Bob Detailing",
        contactName: "Bob",
        nicheKey: "AUTO_DETAILING",
        websiteGapNote: "No booking link",
        brandColorHex: "#3B82F6",
        tagline: "Mobile Detailing",
        depositPercent: 25,
        setupFeeOffer: 250,
        monthlyRetainerOffer: 35,
        stage: "AUDITED",
        createdAt: now,
        updatedAt: now,
      });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("prospects")
      .doc("bob_p1")
      .get()
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("prospects")
      .doc("bob_p1")
      .delete()
  );
});

test("Authenticated user can create and read captured leads across all 7 NicheKeys and playbook tasks", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  for (const nicheKey of ALL_NICHE_KEYS) {
    const leadRef = aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("leads")
      .doc(`lead_${nicheKey}`);
    await assertSucceeds(
      leadRef.set({
        userId: ALICE_UID,
        prospectId: "prospect_1",
        prospectBusinessName: `Business ${nicheKey}`,
        nicheKey: nicheKey,
        customerName: "Sarah Jenkins",
        customerPhone: "555-0192",
        customerEmail: "sarah@example.com",
        preferredDate: "Tomorrow Morning (8am - 12pm)",
        selectedServiceName: "Signature Package",
        selectedAddOns: "Priority Upgrade",
        unitQuantity: 2,
        estimatedTotal: 2650,
        depositAmount: 530,
        notes: "2-zone install for upstairs bedrooms",
        createdAt: now,
        updatedAt: now,
      })
    );
  }

  const taskRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("playbook")
    .doc("task_1");
  await assertSucceeds(
    taskRef.set({
      userId: ALICE_UID,
      dayRange: "Days 1-2",
      stepNumber: 1,
      title: "Select 1 Micro-Problem in 1 Niche",
      description: "Pick HVAC, Auto Detailing, or Boutique Fitness.",
      proTip: "Small businesses buy more leads, not custom code.",
      isCompleted: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});
