/**
 * Fixture'y konformancji muszą być tak samo oceniane przez Zod (tu)
 * i kotlinx.serialization (apps/android :core:protocol).
 */
import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import type { z } from "zod";
import {
  ClientMessage,
  PairingConfirm,
  PairingJoin,
  PairingQrPayload,
  ServerMessage,
} from "../src/index.js";

const here = dirname(fileURLToPath(import.meta.url));
const fixtures = JSON.parse(
  readFileSync(join(here, "..", "test-fixtures", "protocol-fixtures.json"), "utf8"),
) as {
  version: number;
  fixtures: {
    schema: string;
    valid: unknown[];
    invalid: { reason: string; data: unknown }[];
  }[];
};

const schemas: Record<string, z.ZodTypeAny> = {
  PairingQrPayload,
  PairingJoin,
  PairingConfirm,
  ClientMessage,
  ServerMessage,
};

describe("protocol fixtures (konformancja)", () => {
  for (const f of fixtures.fixtures) {
    const schema = schemas[f.schema];
    it(`${f.schema}: schemat istnieje`, () => {
      expect(schema).toBeDefined();
    });

    f.valid.forEach((v, i) => {
      it(`${f.schema} valid[${i}] przechodzi`, () => {
        expect(schema!.safeParse(v).success).toBe(true);
      });
    });

    f.invalid.forEach((v, i) => {
      it(`${f.schema} invalid[${i}] (${v.reason}) odrzucony`, () => {
        expect(schema!.safeParse(v.data).success).toBe(false);
      });
    });
  }
});
