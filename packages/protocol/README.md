# @szuflada/protocol

Wspólne schematy wiadomości protokołu (parowanie QR + sync) oraz kanoniczne
wektory testowe krypto.

## Zawartość

- `src/pairing.ts` — wiadomości protokołu parowania QR (sekcja 4 briefu):
  `PairingQrPayload`, `PairingJoin`, `PairingConfirm`
- `src/sync.ts` — kanał sync na relayu (sekcja 5): auth challenge-response
  (Ed25519), `push_ops` / `pull_ops` / `presence`, koperty `SyncOp`
- `src/common.ts` — typy bazowe: hex, klucze, nonce, ULID, `EncryptedEnvelope`
- `src/crypto-vectors.ts` — schemat pliku wektorów testowych
- `test-vectors/crypto-vectors.json` — **kanoniczne wektory krypto** (X25519,
  XChaCha20-Poly1305-IETF, Ed25519), zakotwiczone o RFC 7748 i RFC 8032;
  jedno źródło prawdy dla TS, JVM (lazysodium) i Rust

Wszystkie schematy: Zod, `strict()` — nieznane pola są odrzucane na każdej
granicy I/O.

## Komendy

```bash
pnpm test          # testy schematów + weryfikacja wektorów (libsodium-wrappers)
pnpm typecheck
pnpm gen:vectors   # regeneracja wektorów (asserty RFC muszą przejść)
```

Wektory w repo muszą być bajt w bajt tym, co produkuje generator — pilnuje
tego CI.

## Kotlin (kotlinx.serialization)

Generacja schematów Kotlin z tego źródła — otwarty krok Fazy 0 (zrobimy przy
scaffoldingu `apps/android`, żeby generować od razu do właściwego modułu).
