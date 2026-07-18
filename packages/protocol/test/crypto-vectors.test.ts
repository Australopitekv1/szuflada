/**
 * Weryfikacja kanonicznych wektorów krypto po stronie TS (libsodium-wrappers).
 * Ten sam plik JSON konsumują testy JVM (lazysodium) — spikes/crypto-jvm.
 */
import _sodium from "../scripts/sodium.js";
import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { beforeAll, describe, expect, it } from "vitest";
import { CryptoVectorsFile } from "../src/crypto-vectors.js";

const here = dirname(fileURLToPath(import.meta.url));
const vectorsPath = join(here, "..", "test-vectors", "crypto-vectors.json");

const vectors = CryptoVectorsFile.parse(
  JSON.parse(readFileSync(vectorsPath, "utf8")),
);

let sodium: typeof _sodium;
beforeAll(async () => {
  await _sodium.ready;
  sodium = _sodium;
});

describe("X25519 (crypto_scalarmult)", () => {
  for (const v of vectors.x25519) {
    it(v.name, () => {
      const skA = sodium.from_hex(v.skA);
      const skB = sodium.from_hex(v.skB);
      expect(sodium.to_hex(sodium.crypto_scalarmult_base(skA))).toBe(v.pkA);
      expect(sodium.to_hex(sodium.crypto_scalarmult_base(skB))).toBe(v.pkB);
      expect(
        sodium.to_hex(sodium.crypto_scalarmult(skA, sodium.from_hex(v.pkB))),
      ).toBe(v.shared);
      expect(
        sodium.to_hex(sodium.crypto_scalarmult(skB, sodium.from_hex(v.pkA))),
      ).toBe(v.shared);
    });
  }
});

describe("XChaCha20-Poly1305-IETF (AEAD)", () => {
  for (const v of vectors.xchacha20poly1305ietf) {
    it(`${v.name}: encrypt`, () => {
      const ad = v.adHex === "" ? null : sodium.from_hex(v.adHex);
      const ct = sodium.crypto_aead_xchacha20poly1305_ietf_encrypt(
        sodium.from_string(v.plaintextUtf8),
        ad,
        null,
        sodium.from_hex(v.nonce),
        sodium.from_hex(v.key),
      );
      expect(sodium.to_hex(ct)).toBe(v.ciphertext);
    });

    it(`${v.name}: decrypt`, () => {
      const ad = v.adHex === "" ? null : sodium.from_hex(v.adHex);
      const pt = sodium.crypto_aead_xchacha20poly1305_ietf_decrypt(
        null,
        sodium.from_hex(v.ciphertext),
        ad,
        sodium.from_hex(v.nonce),
        sodium.from_hex(v.key),
      );
      expect(sodium.to_string(pt)).toBe(v.plaintextUtf8);
    });

    it(`${v.name}: tampered ciphertext MUST fail`, () => {
      const ad = v.adHex === "" ? null : sodium.from_hex(v.adHex);
      const tampered = sodium.from_hex(v.ciphertext);
      tampered[0] = (tampered[0]! ^ 0x01) & 0xff;
      expect(() =>
        sodium.crypto_aead_xchacha20poly1305_ietf_decrypt(
          null,
          tampered,
          ad,
          sodium.from_hex(v.nonce),
          sodium.from_hex(v.key),
        ),
      ).toThrow();
    });
  }
});

describe("Ed25519 (crypto_sign_detached)", () => {
  for (const v of vectors.ed25519) {
    it(v.name, () => {
      const kp = sodium.crypto_sign_seed_keypair(sodium.from_hex(v.seed));
      expect(sodium.to_hex(kp.publicKey)).toBe(v.pk);
      const msg =
        v.messageHex === "" ? new Uint8Array(0) : sodium.from_hex(v.messageHex);
      const sig = sodium.crypto_sign_detached(msg, kp.privateKey);
      expect(sodium.to_hex(sig)).toBe(v.signature);
      expect(
        sodium.crypto_sign_verify_detached(sig, msg, kp.publicKey),
      ).toBe(true);
    });

    it(`${v.name}: tampered signature MUST fail`, () => {
      const kp = sodium.crypto_sign_seed_keypair(sodium.from_hex(v.seed));
      const msg =
        v.messageHex === "" ? new Uint8Array(0) : sodium.from_hex(v.messageHex);
      const badSig = sodium.from_hex(v.signature);
      badSig[0] = (badSig[0]! ^ 0x01) & 0xff;
      expect(
        sodium.crypto_sign_verify_detached(badSig, msg, kp.publicKey),
      ).toBe(false);
    });
  }
});
