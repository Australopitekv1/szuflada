import { z } from "zod";

/**
 * Wersja protokołu parowania/sync. Podbijana świadomie — zmiana formatu
 * wiadomości bez podbicia wersji jest błędem.
 */
export const PROTOCOL_VERSION = 1 as const;

/** Hex (lowercase) o dokładnej długości `bytes` bajtów. */
const hexOfBytes = (bytes: number) =>
  z
    .string()
    .regex(/^[0-9a-f]+$/, "expected lowercase hex")
    .length(bytes * 2, `expected ${bytes} bytes of hex`);

/** Hex (lowercase) o dowolnej, parzystej długości — np. ciphertext. */
export const HexString = z
  .string()
  .regex(/^(?:[0-9a-f]{2})+$/, "expected lowercase hex of whole bytes");

/** Klucz publiczny X25519 / Ed25519 — 32 bajty. */
export const PublicKeyHex = hexOfBytes(32);

/** Nonce XChaCha20-Poly1305 — 24 bajty. */
export const NonceHex = hexOfBytes(24);

/** Podpis Ed25519 — 64 bajty. */
export const SignatureHex = hexOfBytes(64);

export const Uuid = z.string().uuid();

/** ULID — 26 znaków Crockford base32, sortowalny (oplog). */
export const Ulid = z.string().regex(/^[0-7][0-9A-HJKMNP-TV-Z]{25}$/);

export const DeviceRole = z.enum(["OWNER", "MEMBER"]);
export type DeviceRole = z.infer<typeof DeviceRole>;

/**
 * Zaszyfrowana koperta — jedyne, co relay widzi poza metadanymi routingu.
 * Klucz: vault key (XChaCha20-Poly1305). Zero-knowledge: brak plaintextu.
 */
export const EncryptedEnvelope = z
  .object({
    nonce: NonceHex,
    ciphertext: HexString,
  })
  .strict();
export type EncryptedEnvelope = z.infer<typeof EncryptedEnvelope>;
