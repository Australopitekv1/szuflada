package app.szuflada.core.protocol

import app.szuflada.core.protocol.Validation.NONCE_BYTES
import app.szuflada.core.protocol.Validation.PUBLIC_KEY_BYTES
import app.szuflada.core.protocol.Validation.SIGNATURE_BYTES
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Wiadomości protokołu Szuflady — implementacja Kotlin schematów
 * z packages/protocol (Zod = źródło prawdy, sekcje 4 i 5 briefu).
 *
 * Każda klasa waliduje niezmienniki w init — dokładnie te same reguły co
 * Zod. Zgodność wymusza test na wspólnych fixture'ach.
 */

/** Wspólna konfiguracja JSON: strict (nieznane pola odrzucane), dyskryminator "type". */
val ProtocolJson: Json = Json {
    classDiscriminator = "type"
}

const val PROTOCOL_VERSION = 1

// ── Typy wspólne ───────────────────────────────────────────────────────────

@Serializable
data class EncryptedEnvelope(val nonce: String, val ciphertext: String) {
    init {
        Validation.requireHex(nonce, NONCE_BYTES, "nonce")
        Validation.requireHex(ciphertext, "ciphertext")
    }
}

@Serializable
data class SyncOp(
    val opId: String,
    val authorDeviceId: String,
    val payload: EncryptedEnvelope,
) {
    init {
        Validation.requireUlid(opId, "opId")
        Validation.requireUuid(authorDeviceId, "authorDeviceId")
    }
}

@Serializable
enum class PresenceStatus {
    @SerialName("online") ONLINE,
    @SerialName("offline") OFFLINE,
}

// ── Parowanie (sekcja 4) ───────────────────────────────────────────────────

@Serializable
data class PairingQrPayload(
    val v: Int,
    val sessionId: String,
    val relayUrl: String,
    val pkD: String,
) {
    init {
        require(v == PROTOCOL_VERSION) { "v: nieznana wersja protokołu" }
        Validation.requireUuid(sessionId, "sessionId")
        Validation.requireUrl(relayUrl, "relayUrl")
        Validation.requireHex(pkD, PUBLIC_KEY_BYTES, "pkD")
    }
}

@Serializable
data class PairingJoin(
    val v: Int,
    val sessionId: String,
    val pkP: String,
    val wrappedVaultKey: EncryptedEnvelope,
    val phoneSigningPk: String,
) {
    init {
        require(v == PROTOCOL_VERSION) { "v: nieznana wersja protokołu" }
        Validation.requireUuid(sessionId, "sessionId")
        Validation.requireHex(pkP, PUBLIC_KEY_BYTES, "pkP")
        Validation.requireHex(phoneSigningPk, PUBLIC_KEY_BYTES, "phoneSigningPk")
    }
}

@Serializable
data class PairingConfirm(
    val v: Int,
    val sessionId: String,
    val signingPk: String,
    val deviceName: String,
    val signature: String,
) {
    init {
        require(v == PROTOCOL_VERSION) { "v: nieznana wersja protokołu" }
        Validation.requireUuid(sessionId, "sessionId")
        Validation.requireHex(signingPk, PUBLIC_KEY_BYTES, "signingPk")
        require(deviceName.length in 1..64) { "deviceName: 1..64 znaki" }
        Validation.requireHex(signature, SIGNATURE_BYTES, "signature")
    }
}

// ── Sync: klient → relay (sekcja 5) ────────────────────────────────────────

@Serializable
sealed interface ClientMessage

@Serializable
@SerialName("auth_response")
data class AuthResponse(
    val deviceId: String,
    val signingPk: String,
    val signature: String,
) : ClientMessage {
    init {
        Validation.requireUuid(deviceId, "deviceId")
        Validation.requireHex(signingPk, PUBLIC_KEY_BYTES, "signingPk")
        Validation.requireHex(signature, SIGNATURE_BYTES, "signature")
    }
}

@Serializable
@SerialName("push_ops")
data class PushOps(val ops: List<SyncOp>) : ClientMessage {
    init {
        require(ops.isNotEmpty()) { "ops: lista nie może być pusta" }
    }
}

@Serializable
@SerialName("pull_ops")
data class PullOps(val sinceUlid: String?) : ClientMessage {
    init {
        sinceUlid?.let { Validation.requireUlid(it, "sinceUlid") }
    }
}

@Serializable
@SerialName("presence")
data class PresenceUpdate(val status: PresenceStatus) : ClientMessage

// ── Sync: relay → klient ───────────────────────────────────────────────────

@Serializable
sealed interface ServerMessage

@Serializable
@SerialName("auth_challenge")
data class AuthChallenge(val challenge: String) : ServerMessage {
    init {
        Validation.requireHex(challenge, "challenge")
    }
}

@Serializable
@SerialName("ops")
data class OpsBatch(val ops: List<SyncOp>, val hasMore: Boolean) : ServerMessage

@Serializable
@SerialName("push_ack")
data class PushAck(val accepted: List<String>) : ServerMessage {
    init {
        accepted.forEach { Validation.requireUlid(it, "accepted[]") }
    }
}

@Serializable
@SerialName("peer_presence")
data class PeerPresence(val deviceId: String, val status: PresenceStatus) : ServerMessage {
    init {
        Validation.requireUuid(deviceId, "deviceId")
    }
}

@Serializable
enum class RelayErrorCode {
    @SerialName("auth_failed") AUTH_FAILED,
    @SerialName("device_revoked") DEVICE_REVOKED,
    @SerialName("quota_exceeded") QUOTA_EXCEEDED,
    @SerialName("malformed") MALFORMED,
}

@Serializable
@SerialName("error")
data class RelayError(val code: RelayErrorCode, val message: String) : ServerMessage
