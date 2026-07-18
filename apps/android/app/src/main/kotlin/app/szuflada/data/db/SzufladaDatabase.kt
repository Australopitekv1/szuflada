package app.szuflada.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        VaultEntity::class,
        ItemEntity::class,
        AttachmentEntity::class,
        DeviceEntity::class,
        SyncLogEntity::class,
    ],
    version = 1,
    exportSchema = false, // TODO: włączyć export schematów przed pierwszą migracją
)
abstract class SzufladaDatabase : RoomDatabase() {
    abstract fun vaultDao(): VaultDao
    abstract fun itemDao(): ItemDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun deviceDao(): DeviceDao
    abstract fun syncLogDao(): SyncLogDao
}
