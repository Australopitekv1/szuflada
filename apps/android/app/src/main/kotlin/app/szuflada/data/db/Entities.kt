package app.szuflada.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Model danych z sekcji 3 briefu (docs/PROJECT_BRIEF.md).
 * Baza szyfrowana at-rest (SQLCipher). Daty jako epoch day (Long),
 * kwoty w groszach (Long) — bez Double dla pieniędzy.
 */

enum class ItemType { RECEIPT, INVOICE, WARRANTY, POLICY, CONTRACT, NOTE, SERIAL_NUMBER }

enum class DeviceRole { OWNER, MEMBER }

enum class SyncOperation { PUT, DELETE }

@Entity(tableName = "vaults")
data class VaultEntity(
    @PrimaryKey val vaultId: String,
    /** Master key opakowany kluczem urządzenia — nigdy plaintext. */
    val masterKeyWrapped: ByteArray,
    val createdAt: Long,
)

@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = VaultEntity::class,
            parentColumns = ["vaultId"],
            childColumns = ["vaultId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("vaultId"), Index("warrantyUntil"), Index("deletedAt")],
)
data class ItemEntity(
    @PrimaryKey val itemId: String,
    val vaultId: String,
    val type: ItemType,
    val title: String,
    val merchant: String?,
    /** Kwota w groszach. */
    val amountGrosze: Long?,
    val currency: String?,
    /** Epoch day (LocalDate.toEpochDay). */
    val purchaseDate: Long?,
    val warrantyUntil: Long?,
    val returnUntil: Long?,
    /** JSON array stringów — proste tagowanie w MVP. */
    val tagsJson: String = "[]",
    /** Pełny tekst z ML Kit — źródło dla wyszukiwarki FTS. */
    val ocrText: String?,
    val createdAt: Long,
    val updatedAt: Long,
    /** Soft delete — wymagany przez sync (oplog). */
    val deletedAt: Long? = null,
)

/**
 * Indeks pełnotekstowy nad items — wyszukiwarka po tytule, sklepie
 * i tekście OCR. Room utrzymuje synchronizację triggerami (contentEntity).
 * Uwaga: Room wspiera FTS4 (nie FTS5) — świadome odstępstwo od briefu,
 * funkcjonalnie równoważne dla MVP.
 */
@Fts4(contentEntity = ItemEntity::class)
@Entity(tableName = "items_fts")
data class ItemFtsEntity(
    val title: String,
    val merchant: String?,
    val ocrText: String?,
)

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["itemId"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("itemId")],
)
data class AttachmentEntity(
    @PrimaryKey val attachmentId: String,
    val itemId: String,
    /** Zaszyfrowany JPEG/PDF — cała baza jest pod SQLCipher. */
    val fileBlob: ByteArray,
    val thumbnailBlob: ByteArray?,
    val mimeType: String,
    val sizeBytes: Long,
)

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val deviceId: String,
    /** Klucz publiczny Ed25519 (hex) — tożsamość urządzenia. */
    val publicKey: String,
    /** Nazwa, np. "Pixel Kacpra", "Laptop". */
    val name: String,
    val role: DeviceRole,
    val pairedAt: Long,
    val lastSeenAt: Long?,
    /** Revoke → rotacja vault key (sekcja 4 briefu). */
    val revokedAt: Long? = null,
)

@Entity(
    tableName = "sync_log",
    indices = [Index("itemId"), Index("lamportClock")],
)
data class SyncLogEntity(
    /** ULID — sortowalny leksykograficznie porządek oplogu. */
    @PrimaryKey val opId: String,
    val itemId: String,
    val operation: SyncOperation,
    val payloadEncrypted: ByteArray,
    val authorDeviceId: String,
    val lamportClock: Long,
)
