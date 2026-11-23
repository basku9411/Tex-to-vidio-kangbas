import {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} from "@firebase/rules-unit-testing";
import { readFileSync } from "node:fs";
import test, { before, after, beforeEach } from "node:test";
import {
  doc,
  getDoc,
  setDoc,
  updateDoc,
  deleteDoc,
  collection,
  getDocs,
  query,
  orderBy,
  serverTimestamp,
  Timestamp,
} from "firebase/firestore";

const PROJECT_ID = "gen-lang-client-0849925636";
const DATABASE_ID =
  "ai-studio-android-remixapk-e58f190d-bc71-404a-99cc-c1a3dbe227d9";

let testEnv;

function dbFor(uid) {
  if (!uid) return testEnv.unauthenticatedContext().firestore(DATABASE_ID);
  return testEnv
    .authenticatedContext(uid, { email: `${uid}@gmail.com` })
    .firestore(DATABASE_ID);
}

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: readFileSync("firestore.rules", "utf8"),
      host: "127.0.0.1",
      port: 8085,
    },
  });
});

beforeEach(async () => {
  await testEnv.clearFirestore();
});

after(async () => {
  await testEnv.cleanup();
});

test("unauthenticated users cannot read or write profiles or video history", async () => {
  const unauth = dbFor(null);
  await assertFails(getDoc(doc(unauth, "users/alice")));
  await assertFails(
    setDoc(doc(unauth, "users/alice"), {
      uid: "alice",
      email: "alice@gmail.com",
      displayName: "Alice",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    getDocs(collection(unauth, "users/alice/videoHistory"))
  );
});

test("owner can create and update their user profile and videoHistory record", async () => {
  const aliceDb = dbFor("alice");

  await assertSucceeds(
    setDoc(doc(aliceDb, "users/alice"), {
      uid: "alice",
      email: "alice@gmail.com",
      displayName: "Alice",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  const histRef = doc(aliceDb, "users/alice/videoHistory/local-1001");
  await assertSucceeds(
    setDoc(histRef, {
      userId: "alice",
      title: "Pantai saat senja",
      modelLabel: "Seedance V1 Lite · Text",
      mode: "text",
      status: "queued",
      progressText: "MENYIAPKAN VIDEO",
      createdAtMs: 1700000001000,
      estimatedCredits: 20,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    updateDoc(histRef, {
      status: "completed",
      progressText: "SELESAI",
      videoUrl: "https://cdn.example.com/v1.mp4",
      completedAtMs: 1700000025000,
      updatedAt: serverTimestamp(),
    })
  );

  const listSnap = await assertSucceeds(
    getDocs(
      query(
        collection(aliceDb, "users/alice/videoHistory"),
        orderBy("createdAtMs", "desc")
      )
    )
  );
  if (listSnap.size !== 1) {
    throw new Error(`Expected 1 history item, got ${listSnap.size}`);
  }
});

test("cross-user isolation blocks bob from reading, modifying, or deleting alice history", async () => {
  const aliceDb = dbFor("alice");
  const bobDb = dbFor("bob");

  await assertSucceeds(
    setDoc(doc(aliceDb, "users/alice/videoHistory/anim-2002"), {
      userId: "alice",
      title: "Animasi Merpati",
      modelLabel: "Animasi Foto",
      mode: "image",
      status: "completed",
      progressText: "SELESAI · 0 KREDIT",
      createdAtMs: 1700000002000,
      estimatedCredits: 0,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertFails(getDoc(doc(bobDb, "users/alice/videoHistory/anim-2002")));
  await assertFails(getDocs(collection(bobDb, "users/alice/videoHistory")));
  await assertFails(
    updateDoc(doc(bobDb, "users/alice/videoHistory/anim-2002"), {
      title: "Hacked",
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    deleteDoc(doc(bobDb, "users/alice/videoHistory/anim-2002"))
  );
});

test("rejects ghost fields, invalid document IDs, future timestamps, and immutable field tampering", async () => {
  const aliceDb = dbFor("alice");

  await assertFails(
    setDoc(doc(aliceDb, "users/alice/videoHistory/bad.id"), {
      userId: "alice",
      title: "Bad ID",
      modelLabel: "Model",
      mode: "text",
      status: "queued",
      progressText: "MENYIAPKAN",
      createdAtMs: 1700000003000,
      estimatedCredits: 10,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertFails(
    setDoc(doc(aliceDb, "users/alice/videoHistory/local-1003"), {
      userId: "alice",
      title: "Ghost",
      modelLabel: "Model",
      mode: "text",
      status: "queued",
      progressText: "MENYIAPKAN",
      createdAtMs: 1700000003000,
      estimatedCredits: 10,
      id: "local-1003",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  const validRef = doc(aliceDb, "users/alice/videoHistory/local-1004");
  await assertSucceeds(
    setDoc(validRef, {
      userId: "alice",
      title: "Valid",
      modelLabel: "Model",
      mode: "text",
      status: "queued",
      progressText: "MENYIAPKAN",
      createdAtMs: 1700000004000,
      estimatedCredits: 10,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Immutable userId / createdAtMs / future updatedAt rejection
  await assertFails(
    updateDoc(validRef, {
      userId: "bob",
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    updateDoc(validRef, {
      createdAtMs: 999,
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    updateDoc(validRef, {
      title: "Future",
      updatedAt: Timestamp.fromMillis(Date.now() + 600_000),
    })
  );
});
