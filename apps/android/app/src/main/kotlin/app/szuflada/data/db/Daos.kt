package app.szuflada.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Insert
    suspend fun insert(vault: VaultEntity)

    @Query("SELECT * FROM vaults LIMIT 1")
    suspend fun getVault(): VaultEntity?
}

@Dao
interface ItemDao {
    @Upsert
    suspend fun upsert(item: ItemEntity)

    @Query("SELECT * FROM items WHERE itemId = :id")
    suspend fun getById(id: String): ItemEntity?

    @Query("SELECT * FROM items WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ItemEntity>>

    /** Soft delete — deletedAt zostaje w bazie dla oplogu sync. */
    @Query("UPDATE items SET deletedAt = :at, updatedAt = :at WHERE itemId = :id")
    suspend fun softDelete(id: String, at: Long)

    /** Do notyfikacji „gwarancja kończy się za 30 dni” — agregacja w SQL, nie w pętli. */
    @Query(
        """
        SELECT * FROM items
        WHERE deletedAt IS NULL
          AND warrantyUntil IS NOT NULL
          AND warrantyUntil BETWEEN :fromEpochDay AND :untilEpochDay
        ORDER BY warrantyUntil
        """,
    )
    suspend fun warrantiesExpiringBetween(fromEpochDay: Long, untilEpochDay: Long): List<ItemEntity>
}

@Dao
interface AttachmentDao {
    @Insert
    suspend fun insert(attachment: AttachmentEntity)

    @Query("SELECT * FROM attachments WHERE itemId = :itemId")
    suspend fun forItem(itemId: String): List<AttachmentEntity>
}

@Dao
interface DeviceDao {
    @Upsert
    suspend fun upsert(device: DeviceEntity)

    @Query("SELECT * FROM devices WHERE revokedAt IS NULL")
    fun observeActive(): Flow<List<DeviceEntity>>
}

@Dao
interface SyncLogDao {
    @Insert
    suspend fun insert(op: SyncLogEntity)

    /** ULID-y są sortowalne leksykograficznie — porządek oplogu to ORDER BY opId. */
    @Query("SELECT * FROM sync_log WHERE opId > :sinceUlid ORDER BY opId")
    suspend fun opsSince(sinceUlid: String): List<SyncLogEntity>

    @Query("SELECT MAX(opId) FROM sync_log")
    suspend fun latestOpId(): String?
}
