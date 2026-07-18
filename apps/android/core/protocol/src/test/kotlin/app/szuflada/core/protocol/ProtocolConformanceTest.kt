package app.szuflada.core.protocol

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Test konformancji: kotlinx.serialization musi oceniać fixture'y
 * (packages/protocol/test-fixtures/protocol-fixtures.json, generowane
 * z Zod — źródła prawdy) dokładnie tak samo: valid parsuje się
 * i round-tripuje, invalid rzuca.
 */
class ProtocolConformanceTest {

    private fun findRepoRoot(): Path {
        var dir = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
        while (true) {
            if (Files.exists(dir.resolve("packages/protocol/test-fixtures/protocol-fixtures.json"))) {
                return dir
            }
            dir = dir.parent ?: error("Nie znaleziono repo root z fixture'ami protokołu")
        }
    }

    private val fixturesJson: JsonObject = run {
        val path = findRepoRoot().resolve("packages/protocol/test-fixtures/protocol-fixtures.json")
        Json.parseToJsonElement(Files.readString(path)).jsonObject
    }

    /** Dekoder per schemat; round-trip: decode → encode → decode → equals. */
    private val decoders: Map<String, (String) -> Any> = mapOf(
        "PairingQrPayload" to { s ->
            val v = ProtocolJson.decodeFromString<PairingQrPayload>(s)
            assertEquals(v, ProtocolJson.decodeFromString<PairingQrPayload>(ProtocolJson.encodeToString(v)))
            v
        },
        "PairingJoin" to { s ->
            val v = ProtocolJson.decodeFromString<PairingJoin>(s)
            assertEquals(v, ProtocolJson.decodeFromString<PairingJoin>(ProtocolJson.encodeToString(v)))
            v
        },
        "PairingConfirm" to { s ->
            val v = ProtocolJson.decodeFromString<PairingConfirm>(s)
            assertEquals(v, ProtocolJson.decodeFromString<PairingConfirm>(ProtocolJson.encodeToString(v)))
            v
        },
        "ClientMessage" to { s ->
            val v = ProtocolJson.decodeFromString<ClientMessage>(s)
            assertEquals(v, ProtocolJson.decodeFromString<ClientMessage>(ProtocolJson.encodeToString(v)))
            v
        },
        "ServerMessage" to { s ->
            val v = ProtocolJson.decodeFromString<ServerMessage>(s)
            assertEquals(v, ProtocolJson.decodeFromString<ServerMessage>(ProtocolJson.encodeToString(v)))
            v
        },
    )

    @TestFactory
    fun conformance(): List<DynamicTest> {
        val tests = mutableListOf<DynamicTest>()
        for (fixture in fixturesJson.getValue("fixtures").jsonArray) {
            val obj = fixture.jsonObject
            val schema = obj.getValue("schema").jsonPrimitive.content
            val decode = decoders.getValue(schema)

            obj.getValue("valid").jsonArray.forEachIndexed { i, data: JsonElement ->
                tests += DynamicTest.dynamicTest("$schema valid[$i] przechodzi + round-trip") {
                    decode(data.toString())
                }
            }

            obj.getValue("invalid").jsonArray.forEachIndexed { i, case: JsonElement ->
                val reason = case.jsonObject.getValue("reason").jsonPrimitive.content
                val data = case.jsonObject.getValue("data")
                tests += DynamicTest.dynamicTest("$schema invalid[$i] ($reason) odrzucony") {
                    val result = runCatching { decode(data.toString()) }
                    assertTrue(result.isFailure) {
                        "kotlinx zaakceptował dane, które Zod odrzuca: $data"
                    }
                }
            }
        }
        return tests
    }
}
