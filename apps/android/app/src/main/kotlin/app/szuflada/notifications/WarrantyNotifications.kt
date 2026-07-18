package app.szuflada.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.szuflada.MainActivity
import app.szuflada.data.db.ItemEntity
import java.time.LocalDate

/** Lokalne notyfikacje o kończących się gwarancjach (Faza 1 briefu). */
object WarrantyNotifications {

    private const val CHANNEL_ID = "warranty"
    private const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Gwarancje",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Przypomnienia o kończących się gwarancjach"
            },
        )
    }

    fun postExpiringWarranties(context: Context, expiring: List<ItemEntity>) {
        if (expiring.isEmpty()) return
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val first = expiring.first()
        val until = first.warrantyUntil?.let { LocalDate.ofEpochDay(it) }
        val text = buildString {
            append("„${first.title}” — gwarancja do $until")
            if (expiring.size > 1) append(" (i ${expiring.size - 1} innych)")
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Kończy się gwarancja")
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
