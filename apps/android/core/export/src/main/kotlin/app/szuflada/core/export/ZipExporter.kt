package app.szuflada.core.export

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Eksport ZIP (RODO) — "eksport wszystkich danych jednym przyciskiem",
 * obecny od pierwszego dnia (Faza 1 briefu).
 *
 * Format archiwum:
 *   szuflada-export/items.json          — wszystkie dokumenty (poniższy schemat)
 *   szuflada-export/attachments/<itemId>/<attachmentId>.<ext>
 *
 * Czysty JVM — bez zależności od Androida; ten sam format odczyta desktop.
 */

@Serializable
data class ExportAttachment(
    val attachmentId: String,
    val mimeType: String,
    val sizeBytes: Long,
    /** Ścieżka wewnątrz archiwum. */
    val path: String,
)

@Serializable
data class ExportItem(
    val itemId: String,
    val type: String,
    val title: String,
    val merchant: String? = null,
    val amountGrosze: Long? = null,
    val currency: String? = null,
    /** ISO-8601 (yyyy-MM-dd). */
    val purchaseDate: String? = null,
    val warrantyUntil: String? = null,
    val returnUntil: String? = null,
    val tags: List<String> = emptyList(),
    val ocrText: String? = null,
    /** Epoch millis. */
    val createdAt: Long,
    val updatedAt: Long,
    val attachments: List<ExportAttachment> = emptyList(),
)

@Serializable
data class ExportManifest(
    val formatVersion: Int = 1,
    val exportedAt: String,
    val itemCount: Int,
    val items: List<ExportItem>,
)

/** Bajty załącznika dostarczane leniwie — nie trzymamy wszystkich blobów w RAM. */
data class AttachmentContent(
    val path: String,
    val bytes: () -> ByteArray,
)

object ZipExporter {

    private val json = Json { prettyPrint = true }

    private const val ROOT = "szuflada-export"

    fun attachmentPath(itemId: String, attachmentId: String, mimeType: String): String {
        val ext = when (mimeType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "application/pdf" -> "pdf"
            else -> "bin"
        }
        return "$ROOT/attachments/$itemId/$attachmentId.$ext"
    }

    /**
     * Zapisuje archiwum do [out]. [attachments] muszą używać ścieżek
     * z [attachmentPath] — spójnych z manifestem.
     */
    fun export(
        manifest: ExportManifest,
        attachments: List<AttachmentContent>,
        out: OutputStream,
    ) {
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("$ROOT/items.json"))
            zip.write(json.encodeToString(manifest).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            for (attachment in attachments) {
                zip.putNextEntry(ZipEntry(attachment.path))
                zip.write(attachment.bytes())
                zip.closeEntry()
            }
        }
    }
}
