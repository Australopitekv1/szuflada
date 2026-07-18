package app.szuflada.spike

import com.google.gson.Gson
import com.goterl.lazysodium.LazySodiumJava
import com.goterl.lazysodium.SodiumJava
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.nio.file.Files
import java.nio.file.Paths
import java.util.HexFormat

/**
 * Spike ryzyka #1: weryfikacja kanonicznych wektorów krypto
 * (packages/protocol/test-vectors/crypto-vectors.json) przez lazysodium.
 *
 * Te same wektory przechodzą w TS (libsodium-wrappers) — jeśli przechodzą
 * i tutaj, mamy potwierdzoną zgodność bajt-w-bajt między platformami.
 */
class CryptoVectorsTest {

    // ── Model pliku wektorów ──────────────────────────────────────────────

    data class X25519Vector(
        val name: String, val skA: String, val pkA: String,
        val skB: String, val pkB: String, val shared: String,
    )

    data class AeadVector(
        val name: String, val key: String, val nonce: String,
        val plaintextUtf8: String, val adHex: String, val ciphertext: String,
    )

    data class Ed25519Vector(
        val name: String, val seed: String, val pk: String,
        val messageHex: String, val signature: String,
    )

    data class VectorsFile(
        val version: Int, val generator: String,
        val x25519: List<X25519Vector>,
        val xchacha20poly1305ietf: List<AeadVector>,
        val ed25519: List<Ed25519Vector>,
    )

    private val hexF = HexFormat.of()
    private fun hex(s: String): ByteArray =
        if (s.isEmpty()) ByteArray(0) else hexF.parseHex(s)

    private fun ByteArray.toHexLower(): String = hexF.formatHex(this)

    private val sodium = LazySodiumJava(SodiumJava())

    private val vectors: VectorsFile = run {
        val path = Paths.get(
            System.getProperty("user.dir"), "..", "..",
            "packages", "protocol", "test-vectors", "crypto-vectors.json",
        ).normalize()
        Gson().fromJson(Files.readString(path), VectorsFile::class.java)
    }

    // ── X25519 (crypto_scalarmult) ────────────────────────────────────────

    @TestFactory
    fun x25519(): List<DynamicTest> = vectors.x25519.map { v ->
        DynamicTest.dynamicTest("x25519 ${v.name}") {
            val skA = hex(v.skA)
            val skB = hex(v.skB)

            val pkA = ByteArray(32)
            assertTrue(sodium.cryptoScalarMultBase(pkA, skA))
            assertArrayEquals(hex(v.pkA), pkA, "pkA")

            val pkB = ByteArray(32)
            assertTrue(sodium.cryptoScalarMultBase(pkB, skB))
            assertArrayEquals(hex(v.pkB), pkB, "pkB")

            val sharedA = ByteArray(32)
            assertTrue(sodium.cryptoScalarMult(sharedA, skA, hex(v.pkB)))
            assertArrayEquals(hex(v.shared), sharedA, "shared (strona A)")

            val sharedB = ByteArray(32)
            assertTrue(sodium.cryptoScalarMult(sharedB, skB, hex(v.pkA)))
            assertArrayEquals(hex(v.shared), sharedB, "shared (strona B)")
        }
    }

    // ── XChaCha20-Poly1305-IETF (AEAD) ────────────────────────────────────

    @TestFactory
    fun aead(): List<DynamicTest> = vectors.xchacha20poly1305ietf.flatMap { v ->
        val key = hex(v.key)
        val nonce = hex(v.nonce)
        val ad = hex(v.adHex)
        val plaintext = v.plaintextUtf8.toByteArray(Charsets.UTF_8)
        val expectedCt = hex(v.ciphertext)

        listOf(
            DynamicTest.dynamicTest("aead ${v.name}: encrypt") {
                val ct = ByteArray(plaintext.size + 16)
                val ctLen = LongArray(1)
                assertTrue(
                    sodium.cryptoAeadXChaCha20Poly1305IetfEncrypt(
                        ct, ctLen, plaintext, plaintext.size.toLong(),
                        ad, ad.size.toLong(), null, nonce, key,
                    ),
                )
                assertArrayEquals(expectedCt, ct.copyOf(ctLen[0].toInt()), "ciphertext")
            },
            DynamicTest.dynamicTest("aead ${v.name}: decrypt") {
                val pt = ByteArray(expectedCt.size - 16)
                val ptLen = LongArray(1)
                assertTrue(
                    sodium.cryptoAeadXChaCha20Poly1305IetfDecrypt(
                        pt, ptLen, null, expectedCt, expectedCt.size.toLong(),
                        ad, ad.size.toLong(), nonce, key,
                    ),
                )
                assertArrayEquals(plaintext, pt.copyOf(ptLen[0].toInt()), "plaintext")
            },
            DynamicTest.dynamicTest("aead ${v.name}: tampered MUST fail") {
                val tampered = expectedCt.copyOf()
                tampered[0] = (tampered[0].toInt() xor 0x01).toByte()
                val pt = ByteArray(maxOf(tampered.size - 16, 0))
                val ptLen = LongArray(1)
                assertFalse(
                    sodium.cryptoAeadXChaCha20Poly1305IetfDecrypt(
                        pt, ptLen, null, tampered, tampered.size.toLong(),
                        ad, ad.size.toLong(), nonce, key,
                    ),
                )
            },
        )
    }

    // ── Ed25519 (crypto_sign_detached) ────────────────────────────────────

    @TestFactory
    fun ed25519(): List<DynamicTest> = vectors.ed25519.flatMap { v ->
        val seed = hex(v.seed)
        val msg = hex(v.messageHex)

        listOf(
            DynamicTest.dynamicTest("ed25519 ${v.name}: keypair + sign") {
                val pk = ByteArray(32)
                val sk = ByteArray(64)
                assertTrue(sodium.cryptoSignSeedKeypair(pk, sk, seed))
                assertArrayEquals(hex(v.pk), pk, "public key")

                val sig = ByteArray(64)
                assertTrue(
                    sodium.cryptoSignDetached(sig, msg, msg.size.toLong(), sk),
                )
                assertArrayEquals(hex(v.signature), sig, "signature")
                assertTrue(
                    sodium.cryptoSignVerifyDetached(sig, msg, msg.size, pk),
                    "verify",
                )
            },
            DynamicTest.dynamicTest("ed25519 ${v.name}: tampered signature MUST fail") {
                val badSig = hex(v.signature)
                badSig[0] = (badSig[0].toInt() xor 0x01).toByte()
                assertFalse(
                    sodium.cryptoSignVerifyDetached(badSig, msg, msg.size, hex(v.pk)),
                )
            },
        )
    }
}
