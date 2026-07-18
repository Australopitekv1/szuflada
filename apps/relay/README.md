# apps/relay

Zero-knowledge relay server — **głupia skrzynka na zaszyfrowane koperty**.
Napisany w **Go** ([ADR 0001](../../docs/adr/0001-relay-in-go.md)).

## Zasada

Relay nigdy nie posiada kluczy ani plaintextu. Widzi wyłącznie: nadawcę,
odbiorcę, zaszyfrowany blob i timestamp.

## Stan implementacji

- [x] `POST /pairing/session` — sesja parowania: losowe 128-bit ID,
  **TTL 60 s, jednorazowa** (wymogi sekcji 4 briefu), in-memory store
- [x] `GET /healthz`
- [ ] `WS /sync` — kanał urządzenia: `push_ops`, `pull_ops(since_ulid)`,
  `presence` (Faza 2)
- [ ] `POST /blobs` / `GET /blobs/:id` — zaszyfrowane załączniki (Faza 2)
- [ ] `DELETE /account` — RODO wipe (Faza 2)
- [ ] Auth urządzenia: challenge-response na Ed25519 (Faza 2)

## Uruchomienie

```bash
go test ./...
go run ./cmd/relay
# lub self-host:
docker compose up -d
```

Konfiguracja przez env: `RELAY_ADDR` (default `:8080`), `RELAY_PUBLIC_URL`
(adres widoczny dla urządzeń — trafia do kodu QR).

## Retencja (docelowo)

Bloby trzymane do potwierdzenia odbioru przez wszystkie urządzenia + 30 dni
bufora. Self-hosted: bez limitów planów — to feature, nie bug.
