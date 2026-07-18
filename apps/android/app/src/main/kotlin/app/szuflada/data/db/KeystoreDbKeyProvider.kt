package app.szuflada.data.db

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Trwały klucz bazy SQLCipher (DEK, 32 B) opakowany kluczem sprzętowym
 * (KEK, AES-256-GCM w Android Keystore — klucz nie opuszcza TEE).
 *
 * Format pliku db.key (noBackupFilesDir): [1 B długość IV][IV][ciphertext+tag].
 * allowBackup=false + noBackupFilesDir → wrapped DEK nie trafia do backupów.
 *
 * Weryfikacja na urządzeniu: test instrumentacyjny (androidTest) — zaplanowany
 * przy podłączeniu emulatora do CI; logika formatu pliku jest trywialna,
 * a prymitywów (Keystore, AES-GCM) nie da się testować na czystym JVM.
 */
@Singleton
class KeystoreDbKeyProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : DbKeyProvider {

    private companion object {
        const val KEK_ALIAS = "szuflada_db_kek"
        const val KEY_FILE = "db.key"
        const val GCM_TAG_BITS = 128
        const val DEK_BYTES = 32
    }

    @Volatile
    private var cached: ByteArray? = null

    override fun databaseKey(): ByteArray {
        cached?.let { return it.copyOf() }
        synchronized(this) {
            cached?.let { return it.copyOf() }
            val dek = loadOrCreateDek()
            cached = dek
            return dek.copyOf()
        }
    }

    private fun loadOrCreateDek(): ByteArray {
        val file = File(context.noBackupFilesDir, KEY_FILE)
        if (file.exists()) {
            return unwrap(file.readBytes())
        }
        val dek = ByteArray(DEK_BYTES).also { SecureRandom().nextBytes(it) }
        val wrapped = wrap(dek)
        val tmp = File(context.noBackupFilesDir, "$KEY_FILE.tmp")
        tmp.writeBytes(wrapped)
        if (!tmp.renameTo(file)) {
            tmp.delete()
            error("Nie udało się zapisać wrapped DEK")
        }
        return dek
    }

    private fun wrap(dek: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, kek())
        val ct = cipher.doFinal(dek)
        val iv = cipher.iv
        check(iv.size in 1..255)
        return byteArrayOf(iv.size.toByte()) + iv + ct
    }

    private fun unwrap(blob: ByteArray): ByteArray {
        require(blob.size > 1) { "Uszkodzony plik klucza" }
        val ivLen = blob[0].toInt() and 0xFF
        require(blob.size > 1 + ivLen) { "Uszkodzony plik klucza" }
        val iv = blob.copyOfRange(1, 1 + ivLen)
        val ct = blob.copyOfRange(1 + ivLen, blob.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, kek(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ct)
    }

    private fun kek(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEK_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore",
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                KEK_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }
}
