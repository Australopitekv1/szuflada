package app.szuflada.core.protocol

/**
 * Walidacja lustrzana do schematów Zod w packages/protocol (źródło prawdy).
 * Zmiana reguły tam == zmiana tutaj; pilnuje tego test konformancji na
 * wspólnych fixture'ach (protocol-fixtures.json).
 */
internal object Validation {

    private val hexRegex = Regex("^(?:[0-9a-f]{2})+$")
    private val uuidRegex = Regex(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
    )
    private val ulidRegex = Regex("^[0-7][0-9A-HJKMNP-TV-Z]{25}$")

    /** Hex lowercase o dokładnej długości [bytes] bajtów. */
    fun requireHex(value: String, bytes: Int, field: String) {
        require(hexRegex.matches(value) && value.length == bytes * 2) {
            "$field: oczekiwano $bytes bajtów lowercase hex"
        }
    }

    /** Hex lowercase o dowolnej długości ≥ 1 bajt (np. ciphertext). */
    fun requireHex(value: String, field: String) {
        require(hexRegex.matches(value)) { "$field: oczekiwano lowercase hex (≥1 bajt)" }
    }

    fun requireUuid(value: String, field: String) {
        require(uuidRegex.matches(value)) { "$field: oczekiwano UUID" }
    }

    fun requireUlid(value: String, field: String) {
        require(ulidRegex.matches(value)) { "$field: oczekiwano ULID (Crockford base32)" }
    }

    fun requireUrl(value: String, field: String) {
        val uri = runCatching { java.net.URI(value) }.getOrNull()
        require(uri != null && uri.isAbsolute && !uri.scheme.isNullOrEmpty()) {
            "$field: oczekiwano absolutnego URL-a"
        }
    }

    const val PUBLIC_KEY_BYTES = 32
    const val NONCE_BYTES = 24
    const val SIGNATURE_BYTES = 64
}
