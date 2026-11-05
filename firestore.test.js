const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("expenses").get());
  await assertFails(unauthDb.collection("friends").get());
  await assertFails(unauthDb.collection("groups").get());
  await assertFails(unauthDb.collection("payments").get());
});

test("User profile: owner can create and read own profile, non-owner denied (PII isolation)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Rahul Shah",
      email: "rahul.shah@gmail.com",
      preferredLanguage: "en",
      defaultCurrency: "USD",
      theme: "Pastel Sky",
      themeMode: "Light",
      uiScaleMode: "Compact",
      notificationsEnabled: true,
      locationSharingEnabled: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      theme: "Pastel Lavender",
      uiScaleMode: "Standard",
      updatedAt: now,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("Expense: owner can create, query with userId filter, and update; shadow fields & cross-user fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();

  const validExpense = {
    userId: ALICE_UID,
    title: "Pizza Hut Dinner",
    category: "Food & Drink",
    place: "Pizza Hut, Thamel",
    expenseDate: "Apr 28, 2025 • 8:00 PM",
    monthKey: "2025-04",
    currency: "USD",
    subtotalMinor: 7250,
    taxPercent: 13,
    taxMinor: 943,
    serviceChargeMinor: 0,
    tipMinor: 0,
    extraAmountMinor: 0,
    discountMinor: 500,
    totalMinor: 7693,
    splitMethod: "EQUAL",
    status: "PAID",
    paidMinor: 7693,
    owedToYouMinor: 5770,
    youOweMinor: 0,
    participantCount: 4,
    participantNames: ["Rahul Shah", "Aarav", "Priya", "Sita"],
    participantSharesSummary: "Rahul Shah:1924:PAID|Aarav:1923:PAID|Priya:1923:PAID|Sita:1923:PAID",
    createdAt: now,
    updatedAt: now,
  };

  await assertSucceeds(aliceDb.collection("expenses").doc("exp_1").set(validExpense));

  // Shadow update with ghost field must fail
  await assertFails(
    aliceDb.collection("expenses").doc("exp_1").update({
      isVerified: true,
      updatedAt: now,
    })
  );

  // Query alignment test
  await assertSucceeds(
    aliceDb.collection("expenses").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(aliceDb.collection("expenses").get());
  await assertFails(bobDb.collection("expenses").doc("exp_1").get());
});

test("Payments ledger is append-only: create succeeds, update and delete fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("payments").doc("pay_1").set({
      userId: ALICE_UID,
      expenseId: "exp_1",
      expenseTitle: "Pizza Hut Dinner",
      payerName: "Aarav",
      payeeName: "Rahul Shah",
      amountMinor: 1923,
      currency: "USD",
      method: "UPI",
      entryType: "PAYMENT",
      createdAt: now,
    })
  );

  await assertFails(
    aliceDb.collection("payments").doc("pay_1").update({
      amountMinor: 500,
    })
  );
  await assertFails(aliceDb.collection("payments").doc("pay_1").delete());
});
