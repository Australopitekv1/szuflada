# apps/desktop

Klient desktop Szuflady — **Tauri 2** (Rust shell + TypeScript/React frontend).

## Stack

- Tauri 2 (lekki, natywny shell)
- TypeScript + React + Zod (`strict: true`, walidacja na granicach)
- OS keychain do przechowywania vault key (Windows Credential Manager /
  macOS Keychain / Secret Service)

## Rola w protokole parowania

Desktop **wyświetla** kod QR (nie skanuje) — telefon skanuje i dołącza desktop
jako urządzenie. Szczegóły: sekcja 4 briefu.

## Zakres (Faza 2)

- Read-only w pierwszej iteracji (przegląd + wyszukiwanie), potem edycja
- Parowanie przez QR + zarządzanie urządzeniami

> Skeleton — implementacja nie została jeszcze rozpoczęta.
