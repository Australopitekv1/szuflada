package app.szuflada.core.export

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

class ZipExporterTest {

    private val item = ExportItem(
        itemId = "item-1",
        type = "RECEIPT",
        title = "Pralka",
        merchant = "Media Expert",
        amountGrosze = 249_900,
        currency = "PLN",
        purchaseDate = "2026-07-15",
        warrantyUntil = "2028-07-15",
        tags = listOf("agd", "gwarancja"),
        ocrText = "MEDIA EXPERT\nSUMA PLN 2499,00",
        createdAt = 1_700_000_000_000,
        updatedAt = 1_700_000_000_000,
        attachments = listOf(
            ExportAttachment(
                attachmentId = "att-1",
                mimeType = "image/jpeg",
                sizeBytes = 4,
                path = ZipExporter.attachmentPath("item-1", "att-1", "image/jpeg"),
            ),
        ),
    )

    private fun exportToBytes(): ByteArray {
        val out = ByteArrayOutputStream()
        ZipExporter.export(
            manifest = ExportManifest(
                exportedAt = "2026-07-18T12:00:00Z",
                itemCount = 1,
                items = listOf(item),
            ),
            attachments = listOf(
                AttachmentContent(item.attachments[0].path) { byteArrayOf(1, 2, 3, 4) },
            ),
            out = out,
        )
        return out.toByteArray()
    }

    private fun readEntries(zipBytes: ByteArray): Map<String, ByteArray> {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zip ->
            var e = zip.nextEntry
            while (e != null) {
                entries[e.name] = zip.readBytes()
                e = zip.nextEntry
            }
        }
        return entries
    }

    @Test
    fun `archiwum zawiera manifest i zalaczniki pod zadeklarowanymi sciezkami`() {
        val entries = readEntries(exportToBytes())

        assertEquals(
            setOf(
                "szuflada-export/items.json",
                "szuflada-export/attachments/item-1/att-1.jpg",
            ),
            entries.keys,
        )
        assertArrayEquals(
            byteArrayOf(1, 2, 3, 4),
            entries["szuflada-export/attachments/item-1/att-1.jpg"],
        )
    }

    @Test
    fun `manifest round-tripuje przez kotlinx serialization`() {
        val entries = readEntries(exportToBytes())
        val decoded = Json.decodeFromString<ExportManifest>(
            entries.getValue("szuflada-export/items.json").decodeToString(),
        )

        assertEquals(1, decoded.formatVersion)
        assertEquals(1, decoded.itemCount)
        assertEquals(listOf(item), decoded.items)
    }

    @Test
    fun `sciezki zalacznikow po mime type`() {
        assertTrue(ZipExporter.attachmentPath("i", "a", "application/pdf").endsWith("a.pdf"))
        assertTrue(ZipExporter.attachmentPath("i", "a", "image/png").endsWith("a.png"))
        assertTrue(ZipExporter.attachmentPath("i", "a", "application/x-cokolwiek").endsWith("a.bin"))
    }
}
