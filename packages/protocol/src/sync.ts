import { z } from "zod";
import {
  EncryptedEnvelope,
  HexString,
  PublicKeyHex,
  SignatureHex,
  Ulid,
  Uuid,
} from "./common.js";

/**
 * Kanał sync na relayu — sekcja 5 briefu (docs/PROJECT_BRIEF.md).
 * Relay to głupia skrzynka: widzi nadawcę, odbiorcę, blob i timestamp.
 */

/** Pojedynczy op z oplogu, zaszyfrowany per-device. */
export const SyncOp = z
  .object({
    /** ULID — sortowalny identyfikator opa (porządek oplogu). */
    opId: Ulid,
    authorDeviceId: Uuid,
    payload: EncryptedEnvelope,
  })
  .strict();
export type SyncOp = z.infer<typeof SyncOp>;

// ── Auth: challenge-response na Ed25519 (żadnych haseł na relayu) ──────────

export const AuthChallenge = z
  .object({
    type: z.literal("auth_challenge"),
    /** Losowe 32 bajty od relaya. */
    challenge: HexString,
  })
  .strict();
export type AuthChallenge = z.infer<typeof AuthChallenge>;

export const AuthResponse = z
  .object({
    type: z.literal("auth_response"),
    deviceId: Uuid,
    signingPk: PublicKeyHex,
    /** Podpis Ed25519 nad challenge. */
    signature: SignatureHex,
  })
  .strict();
export type AuthResponse = z.infer<typeof AuthResponse>;

// ── Wiadomości klient → relay ──────────────────────────────────────────────

export const PushOps = z
  .object({
    type: z.literal("push_ops"),
    ops: z.array(SyncOp).min(1),
  })
  .strict();
export type PushOps = z.infer<typeof PushOps>;

export const PullOps = z
  .object({
    type: z.literal("pull_ops"),
    /** Ostatni znany ULID — relay zwraca opy nowsze niż ten. Null = od zera. */
    sinceUlid: Ulid.nullable(),
  })
  .strict();
export type PullOps = z.infer<typeof PullOps>;

export const PresenceUpdate = z
  .object({
    type: z.literal("presence"),
    status: z.enum(["online", "offline"]),
  })
  .strict();
export type PresenceUpdate = z.infer<typeof PresenceUpdate>;

export const ClientMessage = z.discriminatedUnion("type", [
  AuthResponse,
  PushOps,
  PullOps,
  PresenceUpdate,
]);
export type ClientMessage = z.infer<typeof ClientMessage>;

// ── Wiadomości relay → klient ──────────────────────────────────────────────

export const OpsBatch = z
  .object({
    type: z.literal("ops"),
    ops: z.array(SyncOp),
    /** Czy są jeszcze starsze opy do dociągnięcia. */
    hasMore: z.boolean(),
  })
  .strict();
export type OpsBatch = z.infer<typeof OpsBatch>;

export const PushAck = z
  .object({
    type: z.literal("push_ack"),
    /** ULID-y przyjętych opów. */
    accepted: z.array(Ulid),
  })
  .strict();
export type PushAck = z.infer<typeof PushAck>;

export const PeerPresence = z
  .object({
    type: z.literal("peer_presence"),
    deviceId: Uuid,
    status: z.enum(["online", "offline"]),
  })
  .strict();
export type PeerPresence = z.infer<typeof PeerPresence>;

export const RelayError = z
  .object({
    type: z.literal("error"),
    code: z.enum([
      "auth_failed",
      "device_revoked",
      "quota_exceeded",
      "malformed",
    ]),
    message: z.string(),
  })
  .strict();
export type RelayError = z.infer<typeof RelayError>;

export const ServerMessage = z.discriminatedUnion("type", [
  AuthChallenge,
  OpsBatch,
  PushAck,
  PeerPresence,
  RelayError,
]);
export type ServerMessage = z.infer<typeof ServerMessage>;
