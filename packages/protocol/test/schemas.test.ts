import { describe, expect, it } from "vitest";
import {
  ClientMessage,
  PairingConfirm,
  PairingJoin,
  PairingQrPayload,
  ServerMessage,
  Ulid,
} from "../src/index.js";

const hex = (byte: string, bytes: number) => byte.repeat(bytes);
const PK = hex("ab", 32);
const NONCE = hex("01", 24);
const SIG = hex("cd", 64);
const UUID = "b7e14ab8-6f1e-4b0a-9b0a-3c9d2f9a1e42";
const ULID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";

describe("PairingQrPayload", () => {
  const valid = {
    v: 1,
    sessionId: UUID,
    relayUrl: "https://relay.szuflada.app",
    pkD: PK,
  };

  it("round-trip", () => {
    expect(PairingQrPayload.parse(valid)).toEqual(valid);
  });

  it("odrzuca nieznane pola (strict — ochrona przed QR-phishingiem przez przemyt danych)", () => {
    expect(() =>
      PairingQrPayload.parse({ ...valid, evil: "payload" }),
    ).toThrow();
  });

  it("odrzuca zły rozmiar klucza", () => {
    expect(() =>
      PairingQrPayload.parse({ ...valid, pkD: hex("ab", 31) }),
    ).toThrow();
  });

  it("odrzuca hex wielkimi literami (kanoniczna forma: lowercase)", () => {
    expect(() =>
      PairingQrPayload.parse({ ...valid, pkD: PK.toUpperCase() }),
    ).toThrow();
  });

  it("odrzuca nieznaną wersję protokołu", () => {
    expect(() => PairingQrPayload.parse({ ...valid, v: 2 })).toThrow();
  });
});

describe("PairingJoin / PairingConfirm", () => {
  it("PairingJoin round-trip", () => {
    const msg = {
      v: 1,
      sessionId: UUID,
      pkP: PK,
      wrappedVaultKey: { nonce: NONCE, ciphertext: "deadbeef" },
      phoneSigningPk: PK,
    };
    expect(PairingJoin.parse(msg)).toEqual(msg);
  });

  it("PairingConfirm wymaga podpisu o pełnej długości", () => {
    const msg = {
      v: 1,
      sessionId: UUID,
      signingPk: PK,
      deviceName: "Laptop",
      signature: hex("cd", 63),
    };
    expect(() => PairingConfirm.parse(msg)).toThrow();
  });
});

describe("Ulid", () => {
  it("akceptuje poprawny ULID", () => {
    expect(Ulid.parse(ULID)).toBe(ULID);
  });

  it("odrzuca znaki spoza Crockford base32 (I, L, O, U)", () => {
    expect(() => Ulid.parse("01ARZ3NDEKTSV4RRFFQ69G5FAI")).toThrow();
  });
});

describe("ClientMessage / ServerMessage (discriminated unions)", () => {
  it("push_ops", () => {
    const msg = {
      type: "push_ops",
      ops: [
        {
          opId: ULID,
          authorDeviceId: UUID,
          payload: { nonce: NONCE, ciphertext: "deadbeef" },
        },
      ],
    };
    expect(ClientMessage.parse(msg)).toEqual(msg);
  });

  it("pull_ops z sinceUlid=null (pełny pull)", () => {
    const msg = { type: "pull_ops", sinceUlid: null };
    expect(ClientMessage.parse(msg)).toEqual(msg);
  });

  it("push_ops z pustą listą opów jest błędem", () => {
    expect(() => ClientMessage.parse({ type: "push_ops", ops: [] })).toThrow();
  });

  it("auth challenge-response", () => {
    const challenge = { type: "auth_challenge", challenge: hex("aa", 32) };
    expect(ServerMessage.parse(challenge)).toEqual(challenge);

    const response = {
      type: "auth_response",
      deviceId: UUID,
      signingPk: PK,
      signature: SIG,
    };
    expect(ClientMessage.parse(response)).toEqual(response);
  });

  it("odrzuca nieznany typ wiadomości", () => {
    expect(() => ClientMessage.parse({ type: "drop_table" })).toThrow();
  });
});
