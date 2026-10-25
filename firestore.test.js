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

test("Unauthenticated user: cannot read or write cloud_accounts or users", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("cloud_accounts").doc("584920").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("cloud_transfers").doc("tx_1").get());
});

test("Authenticated user: can create and read their own private profile, but cannot read another user's PII", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      accountId: "639104",
      email: "bob@vibrapay.com",
      fullQuadName: "خالد محمد عبدالله القحطاني",
      emailOtpVerified: true,
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  // Cross-user PII read MUST fail
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());

  // Own profile create MUST succeed
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      accountId: "584920",
      email: "alice@vibrapay.com",
      fullQuadName: "أحمد علي صالح الشامي",
      emailOtpVerified: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Authenticated user: can look up recipient cloud_account by numeric ID and credit recipient balance, but cannot debit another user's balance", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("cloud_accounts").doc("639104").set({
      accountId: "639104",
      ownerUid: BOB_UID,
      firstName: "خالد",
      fatherName: "محمد",
      grandfatherName: "عبدالله",
      familyName: "القحطاني",
      fullQuadName: "خالد محمد عبدالله القحطاني",
      countryDialCode: "+966",
      countryNameAr: "المملكة العربية السعودية",
      countryFlag: "🇸🇦",
      phoneNumberLocal: "501234567",
      fullPhoneDigits: "966501234567",
      balanceYer: 300000,
      balanceSar: 12000,
      balanceUsd: 2500,
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Direct lookup by Account ID succeeds for recipient verification
  await assertSucceeds(aliceDb.collection("cloud_accounts").doc("639104").get());

  // Crediting recipient's balance succeeds
  await assertSucceeds(
    aliceDb.collection("cloud_accounts").doc("639104").update({
      balanceSar: 12500,
      updatedAt: new Date(),
    })
  );

  // Attempting to decrease another user's balance MUST fail
  await assertFails(
    aliceDb.collection("cloud_accounts").doc("639104").update({
      balanceSar: 1000,
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: can create atomic cloud_transfer and query transfers where they are a participant", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("cloud_transfers").doc("tx_99001").set({
      transferId: "tx_99001",
      ottId: "990011",
      senderUid: ALICE_UID,
      senderAccountId: "584920",
      senderQuadName: "أحمد علي صالح الشامي",
      recipientUid: BOB_UID,
      recipientAccountId: "639104",
      recipientQuadName: "خالد محمد عبدالله القحطاني",
      amount: 500,
      currency: "SAR",
      syncChannel: "ONLINE_CLOUD",
      status: "COMPLETED",
      participantUids: [ALICE_UID, BOB_UID],
      participantAccountIds: ["584920", "639104"],
      createdAt: now,
    })
  );

  // Querying with participantUids array-contains filter succeeds
  await assertSucceeds(
    aliceDb.collection("cloud_transfers").where("participantUids", "array-contains", ALICE_UID).get()
  );

  // Unfiltered list query MUST fail (Pillar 8 Query Enforcer)
  await assertFails(aliceDb.collection("cloud_transfers").get());

  // Mutating or deleting a completed transfer MUST fail (Immutable ledger)
  await assertFails(
    aliceDb.collection("cloud_transfers").doc("tx_99001").update({ amount: 999999 })
  );
  await assertFails(
    aliceDb.collection("cloud_transfers").doc("tx_99001").delete()
  );
});
