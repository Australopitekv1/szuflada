# spikes/crypto-jvm

**Spike Fazy 0 — ryzyko #1 projektu:** zgodność libsodium między platformami.

## Pytanie

Czy `lazysodium` (JVM/Android, JNI na natywny libsodium) produkuje **bajt w
bajt** te same wyniki co `libsodium-wrappers` (TS/wasm, desktop + generator
wektorów)?

## Odpowiedź: TAK ✅

Testy konsumują kanoniczne wektory
[`packages/protocol/test-vectors/crypto-vectors.json`](../../packages/protocol/test-vectors/crypto-vectors.json)
(generowane libsodium-wrappers, zakotwiczone o RFC 7748 §6.1 i RFC 8032 §7.1)
i weryfikują przez lazysodium:

- **X25519** (`crypto_scalarmult`) — klucze publiczne + DH z obu stron
- **XChaCha20-Poly1305-IETF** — encrypt/decrypt + odrzucenie zmanipulowanego
  ciphertextu
- **Ed25519** (`crypto_sign_detached`) — keypair z seeda, podpis, weryfikacja,
  odrzucenie zmanipulowanego podpisu

Zweryfikowano na JVM 21 (lazysodium-java 5.1.4, lazysodium bundluje natywny
libsodium). Na fizycznym urządzeniu Android te same wektory przejdą przez
`lazysodium-android` — do potwierdzenia przy scaffoldingu `apps/android`
(Faza 1), ale bindingi są identyczne.

## Uruchomienie

```bash
cd spikes/crypto-jvm
gradle test
```

## Wnioski dla implementacji

- lazysodium API `Sign.Native.cryptoSignDetached` nie przyjmuje wskaźnika
  długości podpisu (w libsodium C to `siglen_p`) — podpis zawsze 64 B.
- AEAD w warstwie Native wymaga ręcznej alokacji buforów (`plaintext + 16`).
  W apce opakujemy to w cienki, testowany wrapper.
