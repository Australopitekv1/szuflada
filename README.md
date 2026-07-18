# Szuflada 🗄️

Prywatna, zero-knowledge szuflada na paragony, gwarancje i ważne papiery.

Każdy ma szufladę z paragonami, gwarancjami i „ważnymi papierami”. Nikt nie wie,
gdzie jest paragon za pralkę, kiedy kończy się gwarancja telewizora i gdzie leży
polisa OC. Szuflada robi z tego jedno, prywatne źródło prawdy — trzymane lokalnie
w zaszyfrowanej bazie, synchronizowane między urządzeniami przez relay, który
nigdy nie widzi Twoich danych.

## Zasady

- **Zero-knowledge relay** — serwer przekazuje wyłącznie zaszyfrowane bloby.
- **Local-first** — apka w 100% funkcjonalna offline. Sync to dodatek.
- **On-device OCR** — zdjęcia paragonów nie opuszczają urządzenia niezaszyfrowane.
- **Open source core** — apka i self-hosted relay na licencji AGPL-3.0.
- **RODO by design** — eksport i usunięcie wszystkich danych jednym przyciskiem.

## Struktura

| Katalog | Opis |
| --- | --- |
| `apps/android` | Główna aplikacja — Kotlin + Jetpack Compose |
| `apps/desktop` | Klient desktop — Tauri 2 (Rust + TS/React) |
| `apps/relay` | Zero-knowledge relay server (self-hostowalny) |
| `packages/protocol` | Wspólne schematy wiadomości sync/parowania + wektory testowe krypto |
| `spikes/crypto-jvm` | Spike Fazy 0: zgodność libsodium TS ↔ JVM (lazysodium) ✅ |
| `docs/` | Dokumentacja projektu ([brief](docs/PROJECT_BRIEF.md)) |

## Rozwój

```bash
pnpm install
pnpm test        # schematy protokołu + wektory krypto (TS)
cd spikes/crypto-jvm && gradle test   # te same wektory przez lazysodium (JVM)
```

## Status

Projekt na etapie **Fazy 0 — Fundamenty**. Zobacz roadmapę w
[`docs/PROJECT_BRIEF.md`](docs/PROJECT_BRIEF.md) (sekcja 6).

## Licencja

[AGPL-3.0](LICENSE).
