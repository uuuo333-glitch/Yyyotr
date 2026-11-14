const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_owner_123";
const BOB_UID = "bob_other_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function nowTs() {
  return new Date();
}

function validStoreSettings(uid) {
  const ts = nowTs();
  return {
    userId: uid,
    storeName: "عوادم التيربو",
    verifiedBadge: true,
    tagline: "أنظمة العادم • دبات التلوث • تطبيق خاص بنا",
    themeColorKey: "emerald",
    instagramHandle: "@turbo_exhaust_sa",
    tiktokHandle: "@turbo.exhaust",
    appDownloadUrl: "https://turboexhaust.app/download",
    defaultShippingCompany: "أرامكس",
    defaultEstimatedDays: 3,
    activeWallpaperIndex: 0,
    selectedAlertSound: "turbo",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validProduct(uid) {
  const ts = nowTs();
  return {
    userId: uid,
    name: "دبة تلوث رياضية فولاذ 304",
    description: "دبة تلوث عالية التدفق تقلل الانبعاثات وترفع العزم",
    category: "دبات التلوث",
    priceSar: 850,
    imageKey: "catalytic_converter",
    seoKeywords: ["دبة تلوث", "كتاليزر", "شكمان رياضي"],
    inStock: true,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validChat(uid) {
  const ts = nowTs();
  return {
    userId: uid,
    customerName: "خالد العتيبي",
    customerPhone: "+966500000001",
    lastMessage: "السلام عليكم، هل متوفرة دبة تلوث كامري؟",
    isInternetBlocked: false,
    alertSound: "turbo",
    wallpaperIndex: 0,
    shippingCustomerName: "",
    shippingAddress: "",
    shippingRegion: "",
    shippingCompany: "أرامكس",
    shippingDaysRemaining: 3,
    waybillNumber: "WB-1001",
    waybillStatus: "pending",
    createdAt: ts,
    updatedAt: ts,
  };
}

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

test("Unauthenticated user cannot read or list store settings, products, or chats", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("store_settings").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("products").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("chats").get());
});

test("Owner can create and read store settings; ghost field is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.collection("users").doc(ALICE_UID).collection("store_settings").doc("default");
  await assertSucceeds(ref.set(validStoreSettings(ALICE_UID)));
  await assertSucceeds(ref.get());

  const ghostPayload = { ...validStoreSettings(ALICE_UID), isAdmin: true };
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("store_settings").doc("ghost").set(ghostPayload)
  );
});

test("Cross-user isolation prevents Bob from reading or writing Alice's products and chats", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc(ALICE_UID).collection("products").doc("prod_1").set(validProduct(ALICE_UID));
    await db.collection("users").doc(ALICE_UID).collection("chats").doc("chat_1").set(validChat(ALICE_UID));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("products").doc("prod_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("products").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("chats").doc("chat_1").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("chats").doc("chat_1").update({ isInternetBlocked: true, updatedAt: nowTs() })
  );
});

test("Owner can create product, chat, toggle internet block, update waybill, and append chat messages", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const prodRef = aliceDb.collection("users").doc(ALICE_UID).collection("products").doc("prod_1");
  await assertSucceeds(prodRef.set(validProduct(ALICE_UID)));

  const chatRef = aliceDb.collection("users").doc(ALICE_UID).collection("chats").doc("chat_1");
  await assertSucceeds(chatRef.set(validChat(ALICE_UID)));

  // Toggle internet block
  await assertSucceeds(chatRef.update({ isInternetBlocked: true, updatedAt: nowTs() }));

  // Update shipping waybill
  await assertSucceeds(
    chatRef.update({
      shippingCustomerName: "خالد العتيبي",
      shippingAddress: "حي الملقا، شارع أنس بن مالك",
      shippingRegion: "الرياض",
      shippingCompany: "سمسا (SMSA)",
      shippingDaysRemaining: 2,
      waybillNumber: "WB-7788",
      waybillStatus: "shipped",
      updatedAt: nowTs(),
    })
  );

  // Update custom app name, website URL, subscription, and chat theme
  const settingsRef = aliceDb.collection("users").doc(ALICE_UID).collection("store_settings").doc("default");
  await assertSucceeds(
    settingsRef.set({
      ...validStoreSettings(ALICE_UID),
      customAppNameUnderBadge: "عوادم التيربو بلس",
      websiteUrl: "https://turboexhaust.sa",
      subscriptionPlan: "1_month",
      subscriptionActive: true,
      subscriptionDaysLeft: 30,
      subscriptionSmsCode: "TB-482910",
      customerPhoneForSms: "+966501112233",
      chatThemeKey: "carbon_gold",
      selectedAlertSound: "pops_bangs",
    })
  );

  // Update chat theme and dramatic sound alert
  await assertSucceeds(
    chatRef.update({
      alertSound: "rocket",
      wallpaperIndex: 2,
      chatThemeKey: "neon_night",
      updatedAt: nowTs(),
    })
  );

  // Append message inside existing chat (sound alert + sms_code)
  const msgRef = chatRef.collection("messages").doc("msg_1");
  await assertSucceeds(
    msgRef.set({
      userId: ALICE_UID,
      chatId: "chat_1",
      senderRole: "owner",
      messageType: "sound_alert",
      text: "تم إرسال تنبيه صوت انفجار وفرقعة",
      soundType: "pops_bangs",
      createdAt: nowTs(),
    })
  );

  const smsMsgRef = chatRef.collection("messages").doc("msg_sms_1");
  await assertSucceeds(
    smsMsgRef.set({
      userId: ALICE_UID,
      chatId: "chat_1",
      senderRole: "system",
      messageType: "sms_code",
      text: "رسالة نصية SMS: كود تفعيل الاشتراك هو TB-482910",
      createdAt: nowTs(),
    })
  );

  // Messages are append-only (update denied)
  await assertFails(msgRef.update({ text: "edited" }));

  // Orphaned message without parent chat fails
  const orphanMsg = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("chats")
    .doc("missing_chat")
    .collection("messages")
    .doc("msg_orphan");
  await assertFails(
    orphanMsg.set({
      userId: ALICE_UID,
      chatId: "missing_chat",
      senderRole: "owner",
      messageType: "text",
      text: "orphan",
      createdAt: nowTs(),
    })
  );
});

test("Owner can manage customer_subscriptions and append sms_alert_logs; cross-user & unauth blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  const subRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("customer_subscriptions")
    .doc("sub_1");

  await assertSucceeds(
    subRef.set({
      userId: ALICE_UID,
      customerName: "سعد الدوسري",
      customerPhone: "+966551234567",
      chatId: "chat_1",
      planKey: "1_month",
      planLabelAr: "شهر واحد (25 ر.س)",
      priceSar: 25,
      remainingDays: 2,
      alertThresholdDays: 3,
      status: "expiring_soon",
      autoSmsAlertEnabled: true,
      lastSmsAlertText: "",
      smsAlertCount: 0,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Update remainingDays and mark alert sent
  await assertSucceeds(
    subRef.update({
      remainingDays: 2,
      status: "expiring_soon",
      lastSmsAlertText: "تنبيه: اشتراكك ينتهي خلال يومين",
      smsAlertCount: 1,
      lastAlertSentAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Ghost field fails
  await assertFails(subRef.update({ isSuperAdmin: true, updatedAt: nowTs() }));

  // Append-only SMS alert log
  const logRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("sms_alert_logs")
    .doc("log_1");
  await assertSucceeds(
    logRef.set({
      userId: ALICE_UID,
      subscriptionId: "sub_1",
      customerName: "سعد الدوسري",
      customerPhone: "+966551234567",
      messageBody: "مرحباً سعد، اشتراكك ينتهي خلال يومين.",
      triggerType: "auto_expiry",
      remainingDaysAtSend: 2,
      deliveryStatus: "queued_auto",
      createdAt: nowTs(),
    })
  );
  await assertFails(logRef.update({ messageBody: "tampered" }));

  // Bob and unauth denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("customer_subscriptions").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("sms_alert_logs").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("customer_subscriptions").get());
});
