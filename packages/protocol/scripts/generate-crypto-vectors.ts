/**
 * Generator kanonicznych wektorów testowych krypto (Faza 0, spike ryzyka #1).
 *
 * Autorytet: libsodium (przez libsodium-wrappers / wasm). Zanim cokolwiek
 * zostanie wyemitowane, generator ASSERTUJE zgodność libsodium z wektorami
 * z RFC 7748 §6.1 (X25519) i RFC 8032 §7.1 (Ed25519) — jeśli się nie zgadza,
 * plik NIE powstaje.
 *
 * Wyjście: test-vectors/crypto-vectors.json — konsumowane przez testy
 * TS (vitest), JVM (lazysodium) i docelowo Rust (Tauri).
 *
 * Uruchomienie: pnpm gen:vectors
 */
import _sodium from "./sodium.js";
import { mkdirSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { CryptoVectorsFile } from "../src/crypto-vectors.js";

const here = dirname(fileURLToPath(import.meta.url));
const outPath = join(here, "..", "test-vectors", "crypto-vectors.json");

// ── Kotwice RFC ────────────────────────────────────────────────────────────

/** RFC 7748 §6.1 — Diffie-Hellman Alice/Bob. */
const RFC7748 = {
  aliceSk: "77076d0a7318a57d3c16c17251b26645df4c2f87ebc0992ab177fba51db92c2a",
  alicePk: "8520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a",
  bobSk: "5dab087e624a8a4b79e17f8b83800ee66f3bb1292618b6fd1c2f8b27ff88e0eb",
  bobPk: "de9edb7d7b7dc1b4d35b61c2ece435373f8343c85b78674dadfc7e146f882b4f",
  shared: "4a5d9d5ba4ce2de1728e3bf480350f25e07e21c947d19e3376f09b3c1e161742",
} as const;

/** RFC 8032 §7.1 — TEST 1 (pusta wiadomość). */
const RFC8032_TEST1 = {
  seed: "9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60",
  pk: "d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a",
  message: "",
  signature:
    "e5564300c360ac729086e2cc806e828a84877f1eb8e5d974d873e065224901555fb8821590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b",
} as const;

/** RFC 8032 §7.1 — TEST 2 (jednobajtowa wiadomość 0x72). */
const RFC8032_TEST2 = {
  seed: "4ccd089b28ff96da9db6c346ec114e0f5b8a319f35aba624da8cf6ed4fb8a6fb",
  pk: "3d4017c3e843895a92b70aa74d1b7ebc9c982ccf2ec4968cc0cd55f12af4660c",
  message: "72",
  signature:
    "92a009a9f0d4cab8720e820b5f642540a2b27b5416503f8fb3762223ebdb69da085ac1e43e15996e458f3613d0f11d8c387b2eaeb4302aeeb00d291612bb0c00",
} as const;

function assertEq(actual: string, expected: string, what: string): void {
  if (actual !== expected) {
    throw new Error(
      `KOTWICA RFC NIE PRZESZŁA: ${what}\n  expected: ${expected}\n  actual:   ${actual}`,
    );
  }
}

await _sodium.ready;
const sodium = _sodium;

const toHex = (b: Uint8Array): string => sodium.to_hex(b);
const fromHex = (h: string): Uint8Array => sodium.from_hex(h);

// ── 1. Assert kotwic RFC ───────────────────────────────────────────────────

{
  const alicePk = sodium.crypto_scalarmult_base(fromHex(RFC7748.aliceSk));
  assertEq(toHex(alicePk), RFC7748.alicePk, "RFC 7748 alice pk");
  const bobPk = sodium.crypto_scalarmult_base(fromHex(RFC7748.bobSk));
  assertEq(toHex(bobPk), RFC7748.bobPk, "RFC 7748 bob pk");
  const sharedA = sodium.crypto_scalarmult(
    fromHex(RFC7748.aliceSk),
    fromHex(RFC7748.bobPk),
  );
  assertEq(toHex(sharedA), RFC7748.shared, "RFC 7748 shared (alice side)");
  const sharedB = sodium.crypto_scalarmult(
    fromHex(RFC7748.bobSk),
    fromHex(RFC7748.alicePk),
  );
  assertEq(toHex(sharedB), RFC7748.shared, "RFC 7748 shared (bob side)");
}

for (const t of [RFC8032_TEST1, RFC8032_TEST2]) {
  const kp = sodium.crypto_sign_seed_keypair(fromHex(t.seed));
  assertEq(toHex(kp.publicKey), t.pk, `RFC 8032 pk (seed ${t.seed.slice(0, 8)}…)`);
  const sig = sodium.crypto_sign_detached(fromHex(t.message), kp.privateKey);
  assertEq(toHex(sig), t.signature, `RFC 8032 signature (seed ${t.seed.slice(0, 8)}…)`);
}

console.log("✔ Kotwice RFC 7748 i RFC 8032 zgodne z libsodium");

// ── 2. Wektory deterministyczne (stałe seedy → pełna reprodukowalność) ─────

/** Deterministyczny "seed" — czytelny wzorzec, nie losowość. */
const seedByte = (label: number) => (i: number) => (label * 16 + i) % 256;
const seed32 = (label: number): Uint8Array =>
  Uint8Array.from({ length: 32 }, (_, i) => seedByte(label)(i));
const seed24 = (label: number): Uint8Array =>
  Uint8Array.from({ length: 24 }, (_, i) => seedByte(label)(i));

function x25519Vector(name: string, skA: Uint8Array, skB: Uint8Array) {
  const pkA = sodium.crypto_scalarmult_base(skA);
  const pkB = sodium.crypto_scalarmult_base(skB);
  const shared = sodium.crypto_scalarmult(skA, pkB);
  const sharedCheck = sodium.crypto_scalarmult(skB, pkA);
  assertEq(toHex(sharedCheck), toHex(shared), `DH symetria: ${name}`);
  return {
    name,
    skA: toHex(skA),
    pkA: toHex(pkA),
    skB: toHex(skB),
    pkB: toHex(pkB),
    shared: toHex(shared),
  };
}

function aeadVector(
  name: string,
  key: Uint8Array,
  nonce: Uint8Array,
  plaintextUtf8: string,
  adHex: string,
) {
  const ad = adHex === "" ? null : fromHex(adHex);
  const ct = sodium.crypto_aead_xchacha20poly1305_ietf_encrypt(
    sodium.from_string(plaintextUtf8),
    ad,
    null,
    nonce,
    key,
  );
  // round-trip sanity
  const pt = sodium.crypto_aead_xchacha20poly1305_ietf_decrypt(
    null,
    ct,
    ad,
    nonce,
    key,
  );
  assertEq(sodium.to_string(pt), plaintextUtf8, `AEAD round-trip: ${name}`);
  return { name, key: toHex(key), nonce: toHex(nonce), plaintextUtf8, adHex, ciphertext: toHex(ct) };
}

function ed25519Vector(name: string, seed: Uint8Array, messageHex: string) {
  const kp = sodium.crypto_sign_seed_keypair(seed);
  const msg = messageHex === "" ? new Uint8Array(0) : fromHex(messageHex);
  const signature = sodium.crypto_sign_detached(msg, kp.privateKey);
  if (!sodium.crypto_sign_verify_detached(signature, msg, kp.publicKey)) {
    throw new Error(`Podpis nie weryfikuje się: ${name}`);
  }
  return { name, seed: toHex(seed), pk: toHex(kp.publicKey), messageHex, signature: toHex(signature) };
}

const pairingPlaintext = JSON.stringify({
  note: "wrapped vault key — przykład koperty parowania",
  vaultKey: toHex(seed32(9)),
});

const vectors = {
  version: 1 as const,
  generator: `libsodium-wrappers ${sodium.SODIUM_VERSION_STRING} (wasm)`,
  x25519: [
    {
      name: "rfc7748-6.1-alice-bob",
      skA: RFC7748.aliceSk,
      pkA: RFC7748.alicePk,
      skB: RFC7748.bobSk,
      pkB: RFC7748.bobPk,
      shared: RFC7748.shared,
    },
    x25519Vector("deterministic-1", seed32(1), seed32(2)),
    x25519Vector("deterministic-2", seed32(3), seed32(4)),
  ],
  xchacha20poly1305ietf: [
    aeadVector("empty-ad", seed32(5), seed24(6), "Szuflada: paragon za pralkę 2499,00 zł", ""),
    aeadVector(
      "with-ad",
      seed32(7),
      seed24(8),
      pairingPlaintext,
      toHex(sodium.from_string("szuflada/pairing/v1")),
    ),
    aeadVector("empty-plaintext", seed32(10), seed24(11), "", ""),
  ],
  ed25519: [
    {
      name: "rfc8032-7.1-test1",
      seed: RFC8032_TEST1.seed,
      pk: RFC8032_TEST1.pk,
      messageHex: RFC8032_TEST1.message,
      signature: RFC8032_TEST1.signature,
    },
    {
      name: "rfc8032-7.1-test2",
      seed: RFC8032_TEST2.seed,
      pk: RFC8032_TEST2.pk,
      messageHex: RFC8032_TEST2.message,
      signature: RFC8032_TEST2.signature,
    },
    ed25519Vector(
      "deterministic-1",
      seed32(12),
      toHex(sodium.from_string("potwierdzenie parowania: sessionId‖pk_d‖pk_p")),
    ),
  ],
};

// Walidacja Zod na granicy I/O — plik musi przejść własny schemat.
const parsed = CryptoVectorsFile.parse(vectors);

mkdirSync(dirname(outPath), { recursive: true });
writeFileSync(outPath, JSON.stringify(parsed, null, 2) + "\n");
console.log(`✔ Zapisano ${outPath}`);
console.log(
  `  x25519: ${parsed.x25519.length}, aead: ${parsed.xchacha20poly1305ietf.length}, ed25519: ${parsed.ed25519.length}`,
);
