# apps/android

Główna aplikacja Szuflady — **Kotlin + Jetpack Compose**, Android-first.

## Stack

- Jetpack Compose (UI)
- Room + SQLCipher (lokalna, zaszyfrowana baza)
- Google ML Kit Text Recognition v2 (on-device OCR)
- coroutines + Flow, DI przez Hilt (brak LiveData)
- libsodium przez lazysodium (krypto)

## Zakres (Faza 1 — MVP offline)

- Model danych z sekcji 3 briefu (Room + SQLCipher)
- Dodawanie paragonu: zdjęcie → ML Kit OCR → auto-parsowanie (kwota, data, NIP)
- Lista + wyszukiwarka FTS5 po `ocr_text`
- Pola gwarancji/zwrotu + lokalne notyfikacje
- Eksport ZIP (RODO)

> Skeleton — implementacja nie została jeszcze rozpoczęta.
