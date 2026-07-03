package mihon.feature.airingschedule.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SourceFilter
import eu.kanade.tachiyomi.ui.main.MainActivity
import mihon.feature.airingschedule.SchedulePreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Fires the actual system notification once an alarm scheduled by [ScheduleNotifications]
 * reaches its trigger time.
 */
class ScheduleAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val mediaId = intent.getIntExtra(EXTRA_MEDIA_ID, -1)
        val episode = intent.getIntExtra(EXTRA_EPISODE, -1)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        if (mediaId == -1 || episode == -1) return

        val schedulePreferences: SchedulePreferences = Injekt.get()
        val key = ScheduleNotifications.alarmKey(mediaId, episode)
        schedulePreferences.scheduledAlarmKeys().set(schedulePreferences.scheduledAlarmKeys().get() - key)

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(MainActivity.INTENT_SEARCH_QUERY, title)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingContentIntent = android.app.PendingIntent.getActivity(
            context,
            ScheduleNotifications.requestCode(mediaId, episode),
            contentIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, Notifications.CHANNEL_AIRING_SCHEDULE)
            .setSmallIcon(R.drawable.ic_komikku)
            .setContentTitle(title)
            .setContentText("Episode $episode just aired")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingContentIntent)
            .build()

        val notificationId = Notifications.ID_AIRING_SCHEDULE_BASE - (ScheduleNotifications.requestCode(mediaId, episode) % 10000)
        NotificationManagerCompat.from(context).apply {
            runCatching { notify(notificationId, notification) }
        }
    }

    companion object {
        const val EXTRA_MEDIA_ID = "media_id"
        const val EXTRA_EPISODE = "episode"
        const val EXTRA_TITLE = "title"
    }
}
