package app.szuflada.data.db

import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/** Dostarcza 32-bajtowy klucz bazy SQLCipher. */
interface DbKeyProvider {
    fun databaseKey(): ByteArray
}

/**
 * SCAFFOLD (Faza 1, tymczasowe): klucz losowy per proces — baza NIE
 * przetrwa restartu aplikacji.
 *
 * Docelowo (następny checkbox Fazy 1): losowy DEK opakowany kluczem
 * z Android Keystore (AES-GCM, StrongBox jeśli dostępny), zapisany
 * w EncryptedFile/prefs. Wymaga testów instrumentacyjnych — zasada
 * „krypto: najpierw testy”, dlatego nie robimy tego na ślepo bez SDK.
 */
@Singleton
class EphemeralDbKeyProvider @Inject constructor() : DbKeyProvider {
    private val key: ByteArray by lazy {
        ByteArray(32).also { SecureRandom().nextBytes(it) }
    }

    override fun databaseKey(): ByteArray = key.copyOf()
}
