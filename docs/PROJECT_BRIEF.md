# Szuflada — Project Brief

> Ten dokument jest źródłem prawdy o wizji i architekturze projektu.
> Zmiany w **sekcji 1 (zasady nadrzędne)** i **sekcji 4 (protokół parowania)**
> wymagają świadomej decyzji człowieka — nie modyfikujemy ich mimochodem przy
> okazji innych zadań.

---

## 1. Wizja i zasady nadrzędne

**Problem:** Każdy ma szufladę z paragonami, gwarancjami i „ważnymi papierami”.
Nikt nie wie, gdzie jest paragon za pralkę, kiedy kończy się gwarancja
telewizora i gdzie leży polisa OC. Firmy (JDG) mają dodatkowo chaos fakturowy,
który od 2026 pogłębia obowiązkowy KSeF.

**Rozwiązanie:** Aplikacja mobilna (Android-first) jako źródło prawdy. Dane
trzymane lokalnie w zaszyfrowanej bazie. Desktop i drugie urządzenie
(partner/partnerka) dołączają przez skan kodu QR. Serwer relay przekazuje
wyłącznie zaszyfrowane bloby.

### Zasady, których NIE łamiemy (architectural constraints)

- **Zero-knowledge relay** — serwer nigdy nie posiada kluczy ani plaintext.
  Żadnych wyjątków „na chwilę, do debugowania”.
- **Local-first** — aplikacja w 100% funkcjonalna offline. Sync to dodatek, nie
  wymóg.
- **On-device processing** — OCR paragonów przez ML Kit lokalnie. Żadne zdjęcie
  nie opuszcza urządzenia niezaszyfrowane.
- **Open source core** — apka + self-hosted relay publiczne (AGPL-3.0).
  Monetyzacja przez hosted convenience i funkcje firmowe.
- **RODO by design** — eksport wszystkich danych jednym przyciskiem, pełne
  usunięcie konta = usunięcie blobów z relaya.

---

## 2. Stack technologiczny

| Warstwa | Technologia | Uzasadnienie |
| --- | --- | --- |
| Mobile (Android) | Kotlin + Jetpack Compose | Doświadczenie zespołu (wcześniejsza apka Kick chat), natywny ML Kit |
| Baza lokalna | SQLite + SQLCipher (Room) | Szyfrowanie at-rest, dojrzały ekosystem |
| OCR | Google ML Kit Text Recognition v2 (on-device) | Zero kosztów, zero chmury, argument prywatnościowy |
| Desktop | Tauri 2 (Rust shell + frontend TS) | Lekki, natywny; odczyt kamery niepotrzebny (desktop WYŚWIETLA QR, nie skanuje) |
| Frontend desktop | TypeScript + React + Zod | Spójne z konwencjami zespołu (full TS, walidacja Zod na granicach) |
| Relay server | Go (albo Node/TS — decyzja w Fazie 0) | Prosty, jeden binarny plik do self-hostingu, Docker-friendly |
| Krypto | libsodium (X25519, XChaCha20-Poly1305, Ed25519) | Sprawdzone prymitywy, bindingi na wszystkie platformy |
| Sync transport | WebSocket + fallback HTTP polling | Relay = skrzynka pocztowa na zaszyfrowane koperty |
| Hosted infra (później) | VPS + Docker Compose / Hetzner EU | Dane w UE, niskie koszty, znajomy stack |

### Konwencje kodu (obowiązują we wszystkich promptach Claude Code)

- **Full TypeScript** w części webowej, `strict: true`, **Zod** na każdej
  granicy I/O.
- **Kotlin:** coroutines + Flow, brak LiveData, DI przez Hilt.
- **Brak N+1** — zapytania agregujące na poziomie SQL (views), nie w pętli w
  kodzie.
- **Testy jednostkowe** dla warstwy krypto i sync — OBOWIĄZKOWE przed merge.
- **Commity:** conventional commits (`feat:`, `fix:`, `chore:`).

---

## 3. Model danych (lokalny, SQLCipher)

```
Vault (jeden na użytkownika)
├── vault_id (UUID), master_key_wrapped, created_at
│
Item (dokument w szufladzie)
├── item_id (UUID), vault_id
├── type: RECEIPT | INVOICE | WARRANTY | POLICY | CONTRACT | NOTE | SERIAL_NUMBER
├── title, merchant, amount, currency, purchase_date
├── warranty_until (nullable), return_until (nullable)
├── tags (JSON array), ocr_text (pełny tekst z ML Kit, do wyszukiwania FTS5)
├── created_at, updated_at, deleted_at (soft delete — potrzebny do sync)
│
Attachment
├── attachment_id, item_id
├── file_blob (zaszyfrowany JPEG/PDF), thumbnail_blob, mime_type, size
│
Device (sparowane urządzenia)
├── device_id, public_key, name ("Pixel Kacpra", "Laptop"), role: OWNER | MEMBER
├── paired_at, last_seen_at, revoked_at (nullable)
│
SyncLog (oplog do synchronizacji)
├── op_id (ULID — sortowalne), item_id, operation: PUT | DELETE
├── payload_encrypted, author_device_id, lamport_clock
```

**Strategia sync:** oplog + Last-Writer-Wins per pole z zegarem Lamporta.
Świadomie NIE robimy pełnego CRDT w MVP — konflikt dwóch osób edytujących ten
sam paragon w tej samej sekundzie jest skrajnie rzadki; LWW + historia wersji
(premium) wystarcza. Decyzja do rewizji w Fazie 4.

---

## 4. Protokół parowania QR (serce projektu)

> ⚠️ Zmiany w tej sekcji wymagają świadomej decyzji człowieka.

Model WhatsApp Web — telefon jest urządzeniem nadrzędnym (posiada master key),
desktop/drugi telefon dołącza.

```
1. Desktop → Relay:  "utwórz sesję parowania"
   Relay → Desktop:  session_id + relay_url (TTL 60 s)

2. Desktop generuje efemeryczną parę kluczy X25519 (pk_d, sk_d)
   Desktop wyświetla QR = { session_id, relay_url, pk_d, wersja_protokołu }

3. Telefon skanuje QR. Pokazuje użytkownikowi fingerprint (emoji-hash pk_d)
   → użytkownik potwierdza "Tak, paruj" (świadoma zgoda, ochrona przed QR-phishingiem)

4. Telefon generuje własną parę (pk_p, sk_p), liczy shared secret (X25519 DH),
   wysyła przez relay: pk_p + wrapped_vault_key (master key zaszyfrowany shared
   secretem, XChaCha20-Poly1305)

5. Desktop odszyfrowuje vault key, zapisuje w OS keychain (Windows Credential
   Manager / macOS Keychain / Secret Service)

6. Obie strony podpisują potwierdzenie (Ed25519), telefon zapisuje desktop
   w tabeli Device. Sesja parowania na relayu jest usuwana.

7. Od teraz: sync przez relay — koperty szyfrowane per-device
   (vault key + nonce), relay widzi tylko: nadawca, odbiorca, blob, timestamp.
```

### Wymogi bezpieczeństwa

- Sesja parowania jednorazowa, TTL 60 sekund, unieważniana po użyciu.
- Telefon może w każdej chwili odwołać urządzenie (revoke) → rotacja vault key
  i re-encrypt oplogu dla pozostałych urządzeń.
- Grupa FREE = max 2 urządzenia-osoby (OWNER + 1 MEMBER) + nielimitowane własne
  desktopy OWNERA. Limity egzekwowane na relayu hostowanym (self-hosted: bez
  limitów — to feature, nie bug).

---

## 5. Relay server (zero-knowledge)

Minimalny zakres — relay to głupia skrzynka:

- `POST /pairing/session` — utworzenie sesji parowania
- `WS /sync` — kanał urządzenia; wiadomości: `push_ops`, `pull_ops(since_ulid)`,
  `presence`
- `POST /blobs` / `GET /blobs/:id` — zaszyfrowane załączniki (limit rozmiaru per
  plan)
- `DELETE /account` — RODO wipe
- **Auth urządzenia:** challenge-response na Ed25519 (żadnych haseł na relayu)
- **Retencja:** bloby trzymane do potwierdzenia odbioru przez wszystkie
  urządzenia + 30 dni bufora

**Dystrybucja:** jeden obraz Docker + `docker-compose.yml` w repo. README
z instrukcją self-host w 5 minut (target: społeczność r/selfhosted).

---

## 6. Fazy realizacji

### Faza 0 — Fundamenty (1–2 tyg. pracy z Claude Code)

- [x] Monorepo: `apps/android`, `apps/desktop`, `apps/relay`,
  `packages/protocol` (wspólne schematy wiadomości: Zod + kotlinx.serialization
  generowane z jednego źródła) — *schematy Zod gotowe; generacja
  kotlinx.serialization przy scaffoldingu `apps/android`*
- [x] Decyzja Go vs Node dla relaya (kryterium: łatwość self-host + WebSocket
  przy 10k połączeń) — *Go, uzasadnienie w
  [ADR 0001](adr/0001-relay-in-go.md); szkielet relaya z sesjami parowania
  w `apps/relay`*
- [x] Spike krypto: libsodium na Androidzie (lazysodium) i w Tauri — test
  wektorów między platformami. **To jest ryzyko #1 projektu — zrobić najpierw.**
  — *wektory RFC-anchored w `packages/protocol/test-vectors/`, zweryfikowane
  TS (libsodium-wrappers) i JVM (lazysodium, `spikes/crypto-jvm`); potwierdzenie
  na fizycznym Androidzie i w Tauri przy scaffoldingu tych apek*
- [x] CI: GitHub Actions (build + testy krypto na każdy PR)

### Faza 1 — Samotna szuflada (MVP offline, tylko Android)

- [ ] Room + SQLCipher, model danych z sekcji 3 — *encje + DAO + SupportFactory
  gotowe; pozostaje klucz bazy z Android Keystore (teraz efemeryczny scaffold)*
- [ ] Dodawanie paragonu: zdjęcie → ML Kit OCR → auto-parsowanie (kwota, data,
  NIP sprzedawcy — regexy na polskie formaty paragonów) — *parser z testami
  gotowy (`:core:parsing`); pozostaje flow aparat → ML Kit*
- [ ] Lista + wyszukiwarka FTS5 po `ocr_text`
- [ ] Pola gwarancji/zwrotu + lokalne notyfikacje („gwarancja kończy się za 30
  dni”)
- [ ] Eksport ZIP (RODO) od pierwszego dnia

**Kryterium ukończenia:** apka użyteczna bez internetu i bez konta.

### Faza 2 — Parowanie + desktop

- [ ] Relay: sesje parowania + kanał sync
- [ ] Implementacja protokołu z sekcji 4 (Android skanuje, Tauri wyświetla QR)
- [ ] Desktop read-only w pierwszej iteracji (przegląd + wyszukiwanie), potem
  edycja
- [ ] Zarządzanie urządzeniami + revoke z rotacją klucza

### Faza 3 — Grupa 2-osobowa (free tier)

- [ ] Drugi telefon jako MEMBER (ten sam protokół QR)
- [ ] Atrybucja „dodane przez” na itemach
- [ ] Presence („Ania właśnie dodała paragon — Media Expert, 2499 zł”)

### Faza 4 — Monetyzacja

- [ ] Hosted relay + konta + płatności (Stripe / Przelewy24)
- [ ] Szuflada Firma: integracja API KSeF — auto-pobieranie faktur zakupowych,
  kategoryzacja, miesięczna paczka dla księgowej (ZIP + CSV). **Uwaga:** przed
  implementacją zweryfikować aktualny stan API KSeF 2.0 i terminy obowiązkowości
  — zmieniały się wielokrotnie.
- [ ] Premium konsumenckie: grupa 4+, historia wersji, sejf awaryjny (dostęp
  zapasowy dla bliskiej osoby), większy limit załączników

### Faza 5 — Rozszerzenia (backlog, nie planować szczegółowo)

- iOS (Kotlin Multiplatform — decyzja dopiero po walidacji Androida)
- Import e-paragonów / forward maili z fakturami na dedykowany alias
- Web clipper potwierdzeń zakupów online

---

## 7. Model biznesowy (podsumowanie)

| Plan | Cena | Zawartość |
| --- | --- | --- |
| Free | 0 zł | Pełna apka offline, 2 osoby, hosted sync z limitem załączników (np. 500 MB), self-host bez limitów |
| Premium | ~9 zł/mies | Grupa 4+, historia wersji, sejf awaryjny, 10 GB |
| Firma | ~19 zł/mies | KSeF, paczki księgowe, wiele „szuflad” (osobista + firmowa) |

**Kanał marketingowy:** budowa w public na kanale YouTube (seria „robię
polskiego Bitwardena na paragony”) + r/selfhosted po publikacji self-hosted
relaya.

---

## 8. Ryzyka i otwarte decyzje

- **Krypto cross-platform (Faza 0 spike)** — największe ryzyko techniczne. Jeśli
  lazysodium sprawia problemy, alternatywa: Tink albo własny cienki wrapper na
  JNI.
- **KSeF API** — terminy i spec zmienne; nie budować niczego na sztywno przed
  Fazą 4, wtedy research od zera.
- **Backup master key** — telefon zgubiony = dane stracone? MVP: eksport
  recovery phrase (BIP39-style, 12 słów) wymuszony przy onboardingu. Bez tego
  projekt jest bombą UX.
- **Google Play a szyfrowanie** — deklaracja eksportowa krypto w konsoli Play
  (formalność, nie zapomnieć).
- **LWW vs CRDT** — akceptujemy LWW w MVP, rewizja jeśli użytkownicy zgłoszą
  utracone edycje.

---

## 9. Jak pracować z tym briefem w Claude Code

- Każda sesja zaczyna się od wskazania **fazy** i **konkretnego checkboxa**
  z sekcji 6.
- Zmiany w sekcjach 1 (zasady) i 4 (protokół) wymagają świadomej decyzji
  człowieka — Claude Code nie modyfikuje ich samodzielnie przy okazji innych
  zadań.
- Warstwa krypto i sync: **najpierw testy** (wektory testowe między
  platformami), potem implementacja.
- Ten plik żyje w repo jako `docs/PROJECT_BRIEF.md`; skrócone konwencje kodu
  trafiają do `CLAUDE.md` w rootcie.
