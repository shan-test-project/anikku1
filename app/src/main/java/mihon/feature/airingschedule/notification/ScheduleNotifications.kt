package mihon.feature.airingschedule.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import mihon.feature.airingschedule.AiringScheduleEntry
import mihon.feature.airingschedule.SchedulePreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Schedules/cancels the local alarms that back the per-anime "notify me" bell on the
 * Schedule tab. Notifications are delivered by [ScheduleAlarmReceiver] at episode air time.
 */
object ScheduleNotifications {

    private val schedulePreferences: SchedulePreferences by lazy { Injekt.get() }

    fun alarmKey(mediaId: Int, episode: Int): String = "$mediaId:$episode"

    fun requestCode(mediaId: Int, episode: Int): Int = alarmKey(mediaId, episode).hashCode()

    /** Ensures an alarm is scheduled for [entry] if it hasn't aired yet and isn't already pending. */
    fun ensureScheduled(context: Context, entry: AiringScheduleEntry) {
        if (entry.hasAired()) return
        val key = alarmKey(entry.mediaId, entry.episode)
        val scheduled = schedulePreferences.scheduledAlarmKeys().get()
        if (key in scheduled) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
            putExtra(ScheduleAlarmReceiver.EXTRA_MEDIA_ID, entry.mediaId)
            putExtra(ScheduleAlarmReceiver.EXTRA_EPISODE, entry.episode)
            putExtra(ScheduleAlarmReceiver.EXTRA_TITLE, entry.titleUserPreferred)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode(entry.mediaId, entry.episode),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val triggerAtMillis = entry.airingAt * 1000L
        runCatching {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }

        schedulePreferences.scheduledAlarmKeys().set(scheduled + key)
    }

    /** Cancels a single previously-scheduled alarm for [entry], if any. */
    fun cancel(context: Context, entry: AiringScheduleEntry) {
        val key = alarmKey(entry.mediaId, entry.episode)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ScheduleAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode(entry.mediaId, entry.episode),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pendingIntent)
        schedulePreferences.scheduledAlarmKeys().set(schedulePreferences.scheduledAlarmKeys().get() - key)
    }

    /** Cancels every alarm currently scheduled for episodes belonging to [mediaId]. */
    fun cancelAllForMedia(context: Context, mediaId: Int, entries: List<AiringScheduleEntry>) {
        entries.filter { it.mediaId == mediaId }.forEach { cancel(context, it) }
    }
}
