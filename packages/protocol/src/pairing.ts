import { z } from "zod";
import {
  EncryptedEnvelope,
  PublicKeyHex,
  SignatureHex,
  Uuid,
} from "./common.js";

/**
 * Protokół parowania QR — sekcja 4 briefu (docs/PROJECT_BRIEF.md).
 * Model WhatsApp Web: telefon posiada master key, desktop dołącza.
 *
 * Schematy wiadomości są źródłem prawdy dla TS (Zod) i Kotlina
 * (kotlinx.serialization — generacja w kolejnym kroku Fazy 0).
 */

/** Krok 2: treść kodu QR wyświetlanego przez desktop. */
export const PairingQrPayload = z
  .object({
    v: z.literal(1),
    sessionId: Uuid,
    relayUrl: z.string().url(),
    /** Efemeryczny klucz publiczny X25519 desktopu (pk_d). */
    pkD: PublicKeyHex,
  })
  .strict();
export type PairingQrPayload = z.infer<typeof PairingQrPayload>;

/**
 * Krok 4: telefon → desktop (przez relay).
 * wrappedVaultKey = master key zaszyfrowany shared secretem
 * (X25519 DH → XChaCha20-Poly1305).
 */
export const PairingJoin = z
  .object({
    v: z.literal(1),
    sessionId: Uuid,
    /** Klucz publiczny X25519 telefonu (pk_p). */
    pkP: PublicKeyHex,
    wrappedVaultKey: EncryptedEnvelope,
    /** Klucz publiczny Ed25519 telefonu — tożsamość urządzenia do auth na relayu. */
    phoneSigningPk: PublicKeyHex,
  })
  .strict();
export type PairingJoin = z.infer<typeof PairingJoin>;

/**
 * Krok 6: obustronne podpisane potwierdzenie (Ed25519).
 * Podpis liczony nad transkryptem sesji: sessionId ‖ pk_d ‖ pk_p.
 */
export const PairingConfirm = z
  .object({
    v: z.literal(1),
    sessionId: Uuid,
    /** Klucz publiczny Ed25519 strony potwierdzającej. */
    signingPk: PublicKeyHex,
    /** Nazwa urządzenia, np. "Laptop", "Pixel Kacpra". */
    deviceName: z.string().min(1).max(64),
    signature: SignatureHex,
  })
  .strict();
export type PairingConfirm = z.infer<typeof PairingConfirm>;
