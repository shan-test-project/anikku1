package mihon.feature.airingschedule

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

/**
 * Periodic WorkManager job that automatically refreshes the weekly airing schedule
 * from AniList and caches it locally. Frequency is configurable by the user
 * (1–7 days). Disabled by default.
 */
class ScheduleDataRefreshWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    private val scheduleJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val schedulePrefs = Injekt.get<SchedulePreferences>()

            if (!schedulePrefs.scheduleAutoRefreshEnabled().get()) {
                return@withContext Result.success()
            }

            val repository = AiringScheduleRepository()
            val includeAdult = schedulePrefs.showAdultContent().get()

            val zone = ZoneId.systemDefault()
            val now = ZonedDateTime.now(zone)
            val weekStart = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .toLocalDate().atStartOfDay(zone)
            val weekEnd = weekStart.plusDays(7).minusSeconds(1)

            val entries = repository.getWeeklySchedule(
                weekStart.toEpochSecond(),
                weekEnd.toEpochSecond(),
                includeAdult = includeAdult,
            )

            val cacheData = ScheduleCacheData(
                fetchedAt = System.currentTimeMillis(),
                weekStartEpoch = weekStart.toEpochSecond(),
                entries = entries.map { it.toCached() },
            )

            val cacheFile = context.cacheFile()
            cacheFile.parentFile?.mkdirs()
            cacheFile.writeText(scheduleJson.encodeToString(ScheduleCacheData.serializer(), cacheData))

            schedulePrefs.scheduleLastAutoRefresh().set(System.currentTimeMillis())
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "ScheduleDataRefreshWorker"
        private val cacheJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        private fun Context.cacheFile() = java.io.File(filesDir, "schedule_cache.json")

        fun schedule(context: Context, frequency: SchedulePreferences.AutoRefreshFrequency) {
            val days = frequency.toDays()
            val request = PeriodicWorkRequestBuilder<ScheduleDataRefreshWorker>(days, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        fun readCache(context: Context): ScheduleCacheData? {
            return try {
                val file = context.cacheFile()
                if (!file.exists()) return null
                cacheJson.decodeFromString(ScheduleCacheData.serializer(), file.readText())
            } catch (_: Exception) {
                null
            }
        }

        fun isCacheFresh(cacheData: ScheduleCacheData, frequency: SchedulePreferences.AutoRefreshFrequency): Boolean {
            val maxAge = frequency.toDays() * 24L * 60L * 60L * 1000L
            val age = System.currentTimeMillis() - cacheData.fetchedAt
            return age < maxAge
        }

        private fun SchedulePreferences.AutoRefreshFrequency.toDays(): Long = when (this) {
            SchedulePreferences.AutoRefreshFrequency.EVERY_1_DAY -> 1L
            SchedulePreferences.AutoRefreshFrequency.EVERY_2_DAYS -> 2L
            SchedulePreferences.AutoRefreshFrequency.EVERY_3_DAYS -> 3L
            SchedulePreferences.AutoRefreshFrequency.EVERY_4_DAYS -> 4L
            SchedulePreferences.AutoRefreshFrequency.EVERY_5_DAYS -> 5L
            SchedulePreferences.AutoRefreshFrequency.EVERY_6_DAYS -> 6L
            SchedulePreferences.AutoRefreshFrequency.EVERY_7_DAYS -> 7L
        }
    }
}

private fun AiringScheduleEntry.toCached() = CachedAiringEntry(
    scheduleId = scheduleId,
    airingAt = airingAt,
    episode = episode,
    mediaId = mediaId,
    titleUserPreferred = titleUserPreferred,
    titleEnglish = titleEnglish,
    titleRomaji = titleRomaji,
    titleNative = titleNative,
    coverImageUrl = coverImageUrl,
    totalEpisodes = totalEpisodes,
    averageScore = averageScore,
    format = format,
    status = status,
    isAdult = isAdult,
    genres = genres,
)

fun CachedAiringEntry.toEntry() = AiringScheduleEntry(
    scheduleId = scheduleId,
    airingAt = airingAt,
    episode = episode,
    mediaId = mediaId,
    titleUserPreferred = titleUserPreferred,
    titleEnglish = titleEnglish,
    titleRomaji = titleRomaji,
    titleNative = titleNative,
    coverImageUrl = coverImageUrl,
    totalEpisodes = totalEpisodes,
    averageScore = averageScore,
    format = format,
    status = status,
    isAdult = isAdult,
    genres = genres,
)

@Serializable
data class ScheduleCacheData(
    val fetchedAt: Long,
    val weekStartEpoch: Long,
    val entries: List<CachedAiringEntry>,
)

@Serializable
data class CachedAiringEntry(
    val scheduleId: Int,
    val airingAt: Long,
    val episode: Int,
    val mediaId: Int,
    val titleUserPreferred: String,
    val titleEnglish: String? = null,
    val titleRomaji: String? = null,
    val titleNative: String? = null,
    val coverImageUrl: String,
    val totalEpisodes: Int? = null,
    val averageScore: Int? = null,
    val format: String? = null,
    val status: String? = null,
    val isAdult: Boolean = false,
    val genres: List<String> = emptyList(),
)
