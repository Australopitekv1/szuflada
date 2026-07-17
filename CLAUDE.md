# CLAUDE.md — Szuflada

Prywatna, zero-knowledge szuflada na paragony, gwarancje i ważne papiery.
Android-first, local-first, sync przez zaszyfrowany relay.

Pełna wizja i architektura: [`docs/PROJECT_BRIEF.md`](docs/PROJECT_BRIEF.md).
Ten plik to skrócone konwencje — brief jest źródłem prawdy.

## Zasady nadrzędne (nie łamiemy)

- **Zero-knowledge relay** — serwer nigdy nie widzi kluczy ani plaintextu.
  Żadnych wyjątków „na chwilę, do debugowania”.
- **Local-first** — apka w 100% działa offline. Sync to dodatek, nie wymóg.
- **On-device processing** — OCR (ML Kit) lokalnie. Żadne zdjęcie nie opuszcza
  urządzenia niezaszyfrowane.
- **Open source core** — apka + self-hosted relay na AGPL-3.0.
- **RODO by design** — eksport wszystkich danych i pełne usunięcie konta jednym
  przyciskiem.

## Struktura monorepo

- `apps/android` — Kotlin + Jetpack Compose (główna apka)
- `apps/desktop` — Tauri 2 (Rust shell + TS/React frontend)
- `apps/relay` — zero-knowledge relay server (Go vs Node — decyzja w Fazie 0)
- `packages/protocol` — wspólne schematy wiadomości (Zod + kotlinx.serialization
  z jednego źródła)

## Konwencje kodu

- **TypeScript (web/desktop/protocol):** `strict: true`, **Zod na każdej
  granicy I/O**.
- **Kotlin (Android):** coroutines + Flow, **brak LiveData**, DI przez Hilt.
- **Brak N+1:** agregacje na poziomie SQL (views), nie w pętli w kodzie.
- **Krypto i sync: najpierw testy** (wektory testowe cross-platform), potem
  implementacja. Testy jednostkowe krypto/sync są OBOWIĄZKOWE przed merge.
- **Commity:** conventional commits (`feat:`, `fix:`, `chore:`, `docs:`,
  `test:`, `refactor:`).

## Krypto (prymitywy)

libsodium: X25519 (DH), XChaCha20-Poly1305 (AEAD), Ed25519 (podpisy).
Nie wymyślamy własnych schematów — używamy tych prymitywów.

## Zasady współpracy

- Każda sesja zaczyna się od wskazania **fazy** i **konkretnego checkboxa**
  z sekcji 6 briefu.
- **Sekcja 1 (zasady) i sekcja 4 (protokół parowania QR)** w briefie są
  chronione — nie modyfikujemy ich mimochodem przy okazji innych zadań; zmiana
  wymaga świadomej decyzji człowieka.
