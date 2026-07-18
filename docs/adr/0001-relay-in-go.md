# ADR 0001 — Relay server w Go

**Status:** zaakceptowane (Faza 0) · **Data:** 2026-07-18

## Kontekst

Brief (sekcja 6, Faza 0) wymaga decyzji Go vs Node/TS dla relaya.
Kryterium: **łatwość self-hostingu + WebSocket przy 10k połączeń**.

## Decyzja

Relay piszemy w **Go**.

## Uzasadnienie

- **Self-host:** jeden statycznie linkowany binarek; obraz Docker
  (distroless/scratch) rzędu 10–20 MB, zero zależności runtime. To jest
  dokładnie historia, którą kupuje r/selfhosted.
- **10k WebSocketów:** goroutines + netpoller ogarniają to bez tuningu;
  w Node realnie potrzeba uWebSockets.js i strojenia.
- **Zero-knowledge minimalizuje koszt braku Zod:** relay prawie nie parsuje
  wiadomości — routuje zaszyfrowane koperty. Powierzchnia schematów po
  stronie relaya jest mała (auth, push/pull, presence), a zgodność
  protokołu między platformami i tak pilnują wspólne wektory/fixtury JSON
  z `packages/protocol` (ten sam mechanizm co w spike'u krypto).

## Konsekwencje

- Schematy wiadomości utrzymujemy w `packages/protocol` (Zod = źródło
  prawdy); relay dostaje testy zgodności na fixturach JSON zamiast reuse
  typów.
- Kod relaya trzymamy minimalny („głupia skrzynka"), std-lib first;
  zależności zewnętrzne tylko tam, gdzie std-lib nie wystarcza (WebSocket).

## Rewizja

Decyzja odwracalna do końca Fazy 2 małym kosztem (relay ma minimalny
zakres). Gdyby zespół chciał pełnego reuse TS — wrócić do tego ADR.
