/**
 * Generator fixture'ów konformancji protokołu.
 *
 * Zod (packages/protocol) jest źródłem prawdy. Ten skrypt emituje przypadki
 * valid/invalid, które MUSZĄ być tak samo oceniane przez każdą implementację
 * protokołu (kotlinx.serialization na Androidzie, docelowo Rust/Tauri).
 * Każdy `valid` jest walidowany Zodem przed emisją; każdy `invalid` musi
 * zostać przez Zod odrzucony — inaczej generator pada.
 *
 * Uruchomienie: pnpm gen:fixtures
 */
import { mkdirSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import type { z } from "zod";
import {
  ClientMessage,
  PairingConfirm,
  PairingJoin,
  PairingQrPayload,
  ServerMessage,
} from "../src/index.js";

const here = dirname(fileURLToPath(import.meta.url));
const outPath = join(here, "..", "test-fixtures", "protocol-fixtures.json");

const PK = "ab".repeat(32);
const PK2 = "cd".repeat(32);
const NONCE = "01".repeat(24);
const SIG = "ef".repeat(64);
const UUID = "b7e14ab8-6f1e-4b0a-9b0a-3c9d2f9a1e42";
const UUID2 = "0d1c2b3a-4958-4677-8899-aabbccddeeff";
const ULID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
const ULID2 = "01BX5ZZKBKACTAV9WEVGEMMVRZ";

type Fixture = {
  schema: string;
  valid: unknown[];
  invalid: { reason: string; data: unknown }[];
};

const fixtures: Fixture[] = [
  {
    schema: "PairingQrPayload",
    valid: [
      {
        v: 1,
        sessionId: UUID,
        relayUrl: "https://relay.szuflada.app",
        pkD: PK,
      },
      {
        v: 1,
        sessionId: UUID2,
        relayUrl: "http://localhost:8080",
        pkD: PK2,
      },
    ],
    invalid: [
      {
        reason: "nieznane pole (strict)",
        data: { v: 1, sessionId: UUID, relayUrl: "https://r.example", pkD: PK, evil: "x" },
      },
      {
        reason: "zła wersja protokołu",
        data: { v: 2, sessionId: UUID, relayUrl: "https://r.example", pkD: PK },
      },
      {
        reason: "klucz za krótki (31 bajtów)",
        data: { v: 1, sessionId: UUID, relayUrl: "https://r.example", pkD: "ab".repeat(31) },
      },
      {
        reason: "hex wielkimi literami",
        data: { v: 1, sessionId: UUID, relayUrl: "https://r.example", pkD: PK.toUpperCase() },
      },
      {
        reason: "relayUrl nie jest URL-em",
        data: { v: 1, sessionId: UUID, relayUrl: "not-a-url", pkD: PK },
      },
      {
        reason: "sessionId nie jest UUID",
        data: { v: 1, sessionId: "not-a-uuid", relayUrl: "https://r.example", pkD: PK },
      },
      {
        reason: "brak pola pkD",
        data: { v: 1, sessionId: UUID, relayUrl: "https://r.example" },
      },
    ],
  },
  {
    schema: "PairingJoin",
    valid: [
      {
        v: 1,
        sessionId: UUID,
        pkP: PK,
        wrappedVaultKey: { nonce: NONCE, ciphertext: "deadbeef" },
        phoneSigningPk: PK2,
      },
    ],
    invalid: [
      {
        reason: "pusty ciphertext",
        data: {
          v: 1,
          sessionId: UUID,
          pkP: PK,
          wrappedVaultKey: { nonce: NONCE, ciphertext: "" },
          phoneSigningPk: PK2,
        },
      },
      {
        reason: "ciphertext o nieparzystej liczbie znaków hex",
        data: {
          v: 1,
          sessionId: UUID,
          pkP: PK,
          wrappedVaultKey: { nonce: NONCE, ciphertext: "abc" },
          phoneSigningPk: PK2,
        },
      },
      {
        reason: "nonce za krótki (23 bajty)",
        data: {
          v: 1,
          sessionId: UUID,
          pkP: PK,
          wrappedVaultKey: { nonce: "01".repeat(23), ciphertext: "deadbeef" },
          phoneSigningPk: PK2,
        },
      },
      {
        reason: "nieznane pole w kopercie (strict)",
        data: {
          v: 1,
          sessionId: UUID,
          pkP: PK,
          wrappedVaultKey: { nonce: NONCE, ciphertext: "deadbeef", plaintext: "oops" },
          phoneSigningPk: PK2,
        },
      },
    ],
  },
  {
    schema: "PairingConfirm",
    valid: [
      {
        v: 1,
        sessionId: UUID,
        signingPk: PK,
        deviceName: "Pixel Kacpra",
        signature: SIG,
      },
    ],
    invalid: [
      {
        reason: "pusta nazwa urządzenia",
        data: { v: 1, sessionId: UUID, signingPk: PK, deviceName: "", signature: SIG },
      },
      {
        reason: "nazwa urządzenia > 64 znaki",
        data: { v: 1, sessionId: UUID, signingPk: PK, deviceName: "x".repeat(65), signature: SIG },
      },
      {
        reason: "podpis za krótki (63 bajty)",
        data: { v: 1, sessionId: UUID, signingPk: PK, deviceName: "Laptop", signature: "ef".repeat(63) },
      },
    ],
  },
  {
    schema: "ClientMessage",
    valid: [
      {
        type: "push_ops",
        ops: [
          {
            opId: ULID,
            authorDeviceId: UUID,
            payload: { nonce: NONCE, ciphertext: "deadbeef" },
          },
          {
            opId: ULID2,
            authorDeviceId: UUID2,
            payload: { nonce: NONCE, ciphertext: "cafebabe" },
          },
        ],
      },
      { type: "pull_ops", sinceUlid: ULID },
      { type: "pull_ops", sinceUlid: null },
      { type: "presence", status: "online" },
      { type: "auth_response", deviceId: UUID, signingPk: PK, signature: SIG },
    ],
    invalid: [
      { reason: "nieznany typ wiadomości", data: { type: "drop_table" } },
      { reason: "push_ops z pustą listą", data: { type: "push_ops", ops: [] } },
      {
        reason: "zły ULID (znak I spoza Crockford base32)",
        data: { type: "pull_ops", sinceUlid: "01ARZ3NDEKTSV4RRFFQ69G5FAI" },
      },
      {
        reason: "ULID małymi literami",
        data: { type: "pull_ops", sinceUlid: ULID.toLowerCase() },
      },
      { reason: "presence z nieznanym statusem", data: { type: "presence", status: "away" } },
      {
        reason: "nieznane pole (strict)",
        data: { type: "presence", status: "online", extra: 1 },
      },
      { reason: "brak pola type", data: { sinceUlid: null } },
    ],
  },
  {
    schema: "ServerMessage",
    valid: [
      { type: "auth_challenge", challenge: "aa".repeat(32) },
      {
        type: "ops",
        ops: [
          {
            opId: ULID,
            authorDeviceId: UUID,
            payload: { nonce: NONCE, ciphertext: "deadbeef" },
          },
        ],
        hasMore: false,
      },
      { type: "ops", ops: [], hasMore: true },
      { type: "push_ack", accepted: [ULID, ULID2] },
      { type: "peer_presence", deviceId: UUID, status: "offline" },
      { type: "error", code: "quota_exceeded", message: "limit 500 MB przekroczony" },
    ],
    invalid: [
      { reason: "nieznany kod błędu", data: { type: "error", code: "teapot", message: "..." } },
      { reason: "ops bez pola hasMore", data: { type: "ops", ops: [] } },
      {
        reason: "push_ack ze złym ULID-em",
        data: { type: "push_ack", accepted: ["nie-ulid"] },
      },
    ],
  },
];

// ── Samokontrola: Zod musi zgadzać się z etykietami valid/invalid ──────────

const schemas: Record<string, z.ZodTypeAny> = {
  PairingQrPayload,
  PairingJoin,
  PairingConfirm,
  ClientMessage,
  ServerMessage,
};

for (const f of fixtures) {
  const schema = schemas[f.schema];
  if (!schema) throw new Error(`Brak schematu: ${f.schema}`);
  f.valid.forEach((v, i) => {
    const r = schema.safeParse(v);
    if (!r.success) {
      throw new Error(`${f.schema} valid[${i}] NIE przechodzi Zod: ${r.error.message}`);
    }
  });
  f.invalid.forEach((v, i) => {
    if (schema.safeParse(v.data).success) {
      throw new Error(`${f.schema} invalid[${i}] (${v.reason}) PRZECHODZI Zod, a nie powinien`);
    }
  });
}

const out = { version: 1, fixtures };
mkdirSync(dirname(outPath), { recursive: true });
writeFileSync(outPath, JSON.stringify(out, null, 2) + "\n");
const nValid = fixtures.reduce((a, f) => a + f.valid.length, 0);
const nInvalid = fixtures.reduce((a, f) => a + f.invalid.length, 0);
console.log(`✔ Zapisano ${outPath} (${nValid} valid, ${nInvalid} invalid)`);
