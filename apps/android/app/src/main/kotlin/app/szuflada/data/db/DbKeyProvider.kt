package app.szuflada.data.db

/**
 * Dostarcza 32-bajtowy klucz bazy SQLCipher.
 * Implementacja produkcyjna: [KeystoreDbKeyProvider] (DEK opakowany kluczem
 * z Android Keystore).
 */
interface DbKeyProvider {
    fun databaseKey(): ByteArray
}
