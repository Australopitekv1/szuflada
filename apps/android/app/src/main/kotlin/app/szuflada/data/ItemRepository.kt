package app.szuflada.data

import app.szuflada.data.db.ItemDao
import app.szuflada.data.db.ItemEntity
import app.szuflada.data.db.ItemType
import app.szuflada.data.db.VaultDao
import app.szuflada.data.db.VaultEntity
import kotlinx.coroutines.flow.Flow
import java.security.SecureRandom
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRepository @Inject constructor(
    private val itemDao: ItemDao,
    private val vaultDao: VaultDao,
) {

    fun observeAll(): Flow<List<ItemEntity>> = itemDao.observeAll()

    /** Wyszukiwarka: prefiksowe dopasowanie każdego słowa (FTS4). */
    fun search(query: String): Flow<List<ItemEntity>> {
        val match = query
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ") { term -> term.replace("\"", "") + "*" }
        return itemDao.search(match)
    }

    suspend fun addItem(
        type: ItemType,
        title: String,
        merchant: String?,
        amountGrosze: Long?,
        currency: String?,
        purchaseDate: LocalDate?,
        warrantyUntil: LocalDate?,
        ocrText: String?,
    ): ItemEntity {
        val vault = ensureVault()
        val now = System.currentTimeMillis()
        val item = ItemEntity(
            itemId = UUID.randomUUID().toString(),
            vaultId = vault.vaultId,
            type = type,
            title = title,
            merchant = merchant?.takeIf { it.isNotBlank() },
            amountGrosze = amountGrosze,
            currency = currency,
            purchaseDate = purchaseDate?.toEpochDay(),
            warrantyUntil = warrantyUntil?.toEpochDay(),
            returnUntil = null,
            ocrText = ocrText?.takeIf { it.isNotBlank() },
            createdAt = now,
            updatedAt = now,
        )
        itemDao.upsert(item)
        return item
    }

    suspend fun softDelete(itemId: String) {
        itemDao.softDelete(itemId, System.currentTimeMillis())
    }

    /**
     * Jeden vault na użytkownika (sekcja 3 briefu). Placeholder master key —
     * właściwe generowanie + recovery phrase (BIP39) to checkbox onboardingu.
     */
    private suspend fun ensureVault(): VaultEntity {
        vaultDao.getVault()?.let { return it }
        val vault = VaultEntity(
            vaultId = UUID.randomUUID().toString(),
            masterKeyWrapped = ByteArray(32).also { SecureRandom().nextBytes(it) },
            createdAt = System.currentTimeMillis(),
        )
        vaultDao.insert(vault)
        return vault
    }
}
