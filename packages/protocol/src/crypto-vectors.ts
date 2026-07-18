import { z } from "zod";
import { HexString, NonceHex, PublicKeyHex, SignatureHex } from "./common.js";

/**
 * Schemat pliku wektorów testowych krypto
 * (test-vectors/crypto-vectors.json).
 *
 * Jedno źródło prawdy dla wszystkich platform: TS (libsodium-wrappers),
 * Android/JVM (lazysodium), Rust/Tauri. Każda implementacja MUSI przejść
 * te wektory przed merge — zasada „krypto: najpierw testy”.
 */

const Hex32 = PublicKeyHex; // 32 bajty
const KeyHex = PublicKeyHex; // klucz symetryczny — też 32 bajty

export const X25519Vector = z
  .object({
    name: z.string(),
    skA: Hex32,
    pkA: Hex32,
    skB: Hex32,
    pkB: Hex32,
    /** crypto_scalarmult(skA, pkB) == crypto_scalarmult(skB, pkA) */
    shared: Hex32,
  })
  .strict();
export type X25519Vector = z.infer<typeof X25519Vector>;

export const AeadVector = z
  .object({
    name: z.string(),
    key: KeyHex,
    nonce: NonceHex,
    plaintextUtf8: z.string(),
    /** Additional data (hex); pusty string = brak AD. */
    adHex: z.union([HexString, z.literal("")]),
    /** crypto_aead_xchacha20poly1305_ietf_encrypt — ciphertext ‖ tag. */
    ciphertext: HexString,
  })
  .strict();
export type AeadVector = z.infer<typeof AeadVector>;

export const Ed25519Vector = z
  .object({
    name: z.string(),
    seed: Hex32,
    pk: Hex32,
    messageHex: z.union([HexString, z.literal("")]),
    /** crypto_sign_detached */
    signature: SignatureHex,
  })
  .strict();
export type Ed25519Vector = z.infer<typeof Ed25519Vector>;

export const CryptoVectorsFile = z
  .object({
    /** Wersja formatu pliku wektorów. */
    version: z.literal(1),
    generator: z.string(),
    x25519: z.array(X25519Vector).min(1),
    xchacha20poly1305ietf: z.array(AeadVector).min(1),
    ed25519: z.array(Ed25519Vector).min(1),
  })
  .strict();
export type CryptoVectorsFile = z.infer<typeof CryptoVectorsFile>;
