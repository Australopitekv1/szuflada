package app.szuflada.data

import android.content.Context
import android.net.Uri
import app.szuflada.core.export.AttachmentContent
import app.szuflada.core.export.ExportAttachment
import app.szuflada.core.export.ExportItem
import app.szuflada.core.export.ExportManifest
import app.szuflada.core.export.ZipExporter
import app.szuflada.data.db.AttachmentDao
import app.szuflada.data.db.AttachmentEntity
import app.szuflada.data.db.ItemDao
import app.szuflada.data.db.ItemEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Eksport ZIP (RODO) — mapuje encje na format :core:export i zapisuje
 * archiwum pod URI wybranym przez użytkownika (SAF).
 */
@Singleton
class ExportService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val itemDao: ItemDao,
    private val attachmentDao: AttachmentDao,
) {

    suspend fun exportTo(uri: Uri) = withContext(Dispatchers.IO) {
        val items = itemDao.getAllActive()
        val attachments = attachmentDao.getAll().groupBy(AttachmentEntity::itemId)

        val exportItems = items.map { it.toExportItem(attachments[it.itemId].orEmpty()) }
        val contents = attachments.flatMap { (itemId, list) ->
            list.map { a ->
                AttachmentContent(
                    path = ZipExporter.attachmentPath(itemId, a.attachmentId, a.mimeType),
                    bytes = { a.fileBlob },
                )
            }
        }

        val out = context.contentResolver.openOutputStream(uri)
            ?: error("Nie można otworzyć strumienia do zapisu: $uri")
        out.use {
            ZipExporter.export(
                manifest = ExportManifest(
                    exportedAt = Instant.now().toString(),
                    itemCount = exportItems.size,
                    items = exportItems,
                ),
                attachments = contents,
                out = it,
            )
        }
    }

    private fun ItemEntity.toExportItem(attachments: List<AttachmentEntity>) = ExportItem(
        itemId = itemId,
        type = type.name,
        title = title,
        merchant = merchant,
        amountGrosze = amountGrosze,
        currency = currency,
        purchaseDate = purchaseDate?.let { LocalDate.ofEpochDay(it).toString() },
        warrantyUntil = warrantyUntil?.let { LocalDate.ofEpochDay(it).toString() },
        returnUntil = returnUntil?.let { LocalDate.ofEpochDay(it).toString() },
        tags = runCatching { Json.decodeFromString<List<String>>(tagsJson) }.getOrDefault(emptyList()),
        ocrText = ocrText,
        createdAt = createdAt,
        updatedAt = updatedAt,
        attachments = attachments.map {
            ExportAttachment(
                attachmentId = it.attachmentId,
                mimeType = it.mimeType,
                sizeBytes = it.sizeBytes,
                path = ZipExporter.attachmentPath(itemId, it.attachmentId, it.mimeType),
            )
        },
    )
}
