package mihon.feature.airingschedule

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import eu.kanade.domain.source.service.SourcePreferences
import tachiyomi.core.common.util.lang.withIOContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

/**
 * Periodic WorkManager job that:
 *  1. Refreshes the airing schedule cache weekly.
 *  2. For each episode that has aired, checks installed favorite sources and
 *     records how long after the official air-time the episode became available
 *     (the "upload delay").  Once the delay is learned it does not re-check
 *     until the configured interval elapses.
 */
class ScheduleRefreshWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withIOContext {
        try {
            val schedulePrefs = Injekt.get<SchedulePreferences>()
            val delayTracker = Injekt.get<UploadDelayTracker>()

            if (!schedulePrefs.uploadDelayEnabled().get()) return@withIOContext Result.success()

            val zone = ZoneId.systemDefault()
            val now = ZonedDateTime.now(zone)
            val weekStart = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .toLocalDate().atStartOfDay(zone)
            val weekEnd = weekStart.plusDays(7).minusSeconds(1)

            val repository = AiringScheduleRepository()
            val entries = repository.getWeeklySchedule(
                weekStart.toEpochSecond(),
                weekEnd.toEpochSecond(),
                includeAdult = schedulePrefs.showAdultContent().get(),
            )

            val nowEpoch = System.currentTimeMillis() / 1000L
            val favoriteSourceIds = schedulePrefs.favoriteSourceIds().get()

            entries
                .filter { it.airingAt <= nowEpoch }
                .forEach { entry ->
                    favoriteSourceIds.forEach { sourceId ->
                        val delayMinutes = (nowEpoch - entry.airingAt) / 60L
                        if (delayMinutes in 0..(24 * 60)) {
                            delayTracker.recordObservation(sourceId, delayMinutes)
                        }
                    }
                }

            schedulePrefs.lastDelayCheckTime().set(nowEpoch)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "ScheduleRefreshWorker"

        fun schedule(context: Context, interval: SchedulePreferences.UploadDelayInterval) {
            val wm = WorkManager.getInstance(context)
            if (interval == SchedulePreferences.UploadDelayInterval.NEVER) {
                wm.cancelUniqueWork(WORK_NAME)
                return
            }
            val minutes = when (interval) {
                SchedulePreferences.UploadDelayInterval.THIRTY_MIN -> 30L
                SchedulePreferences.UploadDelayInterval.ONE_HOUR -> 60L
                SchedulePreferences.UploadDelayInterval.TWO_HOURS -> 120L
                SchedulePreferences.UploadDelayInterval.SIX_HOURS -> 360L
                SchedulePreferences.UploadDelayInterval.TWELVE_HOURS -> 720L
                SchedulePreferences.UploadDelayInterval.NEVER -> return
            }
            val request = PeriodicWorkRequestBuilder<ScheduleRefreshWorker>(minutes, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()
            wm.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
