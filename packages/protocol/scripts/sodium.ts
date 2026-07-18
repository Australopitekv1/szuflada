/**
 * Loader libsodium-wrappers przez CJS require — build ESM pakietu (0.7.x)
 * ma zepsute ścieżki modułów (brak dist/modules-esm/libsodium.mjs).
 */
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);

const sodium = require("libsodium-wrappers") as typeof import("libsodium-wrappers");

export default sodium;
