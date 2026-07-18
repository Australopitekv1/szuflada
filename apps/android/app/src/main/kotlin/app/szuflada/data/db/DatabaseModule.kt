package app.szuflada.data.db

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DbKeyModule {
    @Binds
    abstract fun bindDbKeyProvider(impl: EphemeralDbKeyProvider): DbKeyProvider
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        keyProvider: DbKeyProvider,
    ): SzufladaDatabase {
        System.loadLibrary("sqlcipher")
        return Room.databaseBuilder(context, SzufladaDatabase::class.java, "szuflada.db")
            .openHelperFactory(SupportOpenHelperFactory(keyProvider.databaseKey()))
            .build()
    }

    @Provides fun vaultDao(db: SzufladaDatabase): VaultDao = db.vaultDao()
    @Provides fun itemDao(db: SzufladaDatabase): ItemDao = db.itemDao()
    @Provides fun attachmentDao(db: SzufladaDatabase): AttachmentDao = db.attachmentDao()
    @Provides fun deviceDao(db: SzufladaDatabase): DeviceDao = db.deviceDao()
    @Provides fun syncLogDao(db: SzufladaDatabase): SyncLogDao = db.syncLogDao()
}
