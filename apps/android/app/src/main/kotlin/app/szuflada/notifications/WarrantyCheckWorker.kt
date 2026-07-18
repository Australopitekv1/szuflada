package app.szuflada.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.szuflada.data.db.ItemDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration
import java.time.LocalDate

/**
 * Codzienny check gwarancji: itemy z gwarancją kończącą się w ciągu 30 dni
 * → lokalna notyfikacja. Agregacja w SQL (warrantiesExpiringBetween),
 * nie w pętli — konwencja briefu.
 */
@HiltWorker
class WarrantyCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val itemDao: ItemDao,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val today = LocalDate.now().toEpochDay()
        val expiring = itemDao.warrantiesExpiringBetween(today, today + 30)
        WarrantyNotifications.postExpiringWarranties(applicationContext, expiring)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "warranty-check"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WarrantyCheckWorker>(Duration.ofDays(1))
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
