# apps/relay

Zero-knowledge relay server — **głupia skrzynka na zaszyfrowane koperty**.

> Wybór języka (Go vs Node/TS) to otwarta decyzja **Fazy 0**. Kryterium:
> łatwość self-hostingu + WebSocket przy 10k połączeń.

## Zasada

Relay nigdy nie posiada kluczy ani plaintextu. Widzi wyłącznie: nadawcę,
odbiorcę, zaszyfrowany blob i timestamp.

## API (minimalny zakres)

- `POST /pairing/session` — utworzenie sesji parowania (TTL 60 s, jednorazowa)
- `WS /sync` — kanał urządzenia: `push_ops`, `pull_ops(since_ulid)`, `presence`
- `POST /blobs` / `GET /blobs/:id` — zaszyfrowane załączniki (limit per plan)
- `DELETE /account` — RODO wipe
- Auth urządzenia: challenge-response na Ed25519 (żadnych haseł na relayu)

## Retencja

Bloby trzymane do potwierdzenia odbioru przez wszystkie urządzenia + 30 dni
bufora.

## Self-host

Cel: jeden obraz Docker + `docker-compose.yml`, instrukcja self-host w 5 minut
(target: r/selfhosted). Self-hosted = bez limitów urządzeń/planów.

> Skeleton — implementacja nie została jeszcze rozpoczęta.
