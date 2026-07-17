# packages/protocol

Wspólne schematy wiadomości protokołu (parowanie QR + sync).

## Cel

Jedno źródło prawdy dla schematów wiadomości, z którego generowane są:

- **Zod** — dla części webowej/desktop (TypeScript, walidacja na granicach I/O)
- **kotlinx.serialization** — dla Androida (Kotlin)

Dzięki temu telefon, desktop i relay mówią dokładnie tym samym protokołem,
a niezgodności wychodzą na etapie kompilacji, nie w produkcji.

## Zakres

Schematy z sekcji 4 (parowanie) i sekcji 5 (sync/relay) briefu:
wiadomości `push_ops`, `pull_ops`, `presence`, koperty parowania, wrapped keys.

> Skeleton — implementacja nie została jeszcze rozpoczęta.
