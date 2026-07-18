# apps/android

Główna aplikacja Szuflady — **Kotlin + Jetpack Compose**, Android-first.

## Moduły

| Moduł | Opis | Testowalny bez SDK |
| --- | --- | --- |
| `:app` | Aplikacja: Compose, Hilt, Room + SQLCipher (encje sekcji 3 briefu), FTS, lista + dodawanie | nie (CI buduje APK) |
| `:core:parsing` | Parser OCR polskich paragonów: kwota (grosze), data, NIP z sumą kontrolną, sklep | tak |
| `:core:protocol` | Wiadomości protokołu (kotlinx.serialization) + walidacja lustrzana do Zod | tak |
| `:core:export` | Eksport ZIP (RODO): manifest `items.json` + załączniki — wspólny format z desktopem | tak |

## Rozwój bez Android SDK

W środowiskach bez SDK / dostępu do dl.google.com (sandbox):

```bash
SZUFLADA_SKIP_ANDROID=1 gradle test
```

buduje i testuje wyłącznie moduły JVM. Pełny build robi CI (job `android`).

## Zgodność protokołu

`:core:protocol` NIE jest ręczną kopią schematów: test konformancji konsumuje
`packages/protocol/test-fixtures/protocol-fixtures.json` (generowane z Zod —
źródła prawdy) i wymusza identyczną ocenę każdego przypadku valid/invalid.

## Stan Fazy 1

- [x] Encje + DAO modelu danych (sekcja 3), SQLCipher przez `SupportOpenHelperFactory`
- [x] Auto-parsowanie paragonu (kwota, data, NIP) — `:core:parsing`
- [x] Wyszukiwarka FTS (tytuł, sklep, `ocr_text`) + lista i ekran dodawania
  (wklejenie tekstu paragonu → auto-fill z parsera)
- [x] Logika eksportu ZIP (RODO) — `:core:export` (UI z SAF: kolejny krok)
- [ ] Klucz bazy z Android Keystore (teraz: efemeryczny scaffold — patrz `DbKeyProvider`)
- [ ] Zdjęcie → ML Kit OCR (wymaga urządzenia)
- [ ] Notyfikacje gwarancji ("kończy się za 30 dni")
