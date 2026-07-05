package mihon.feature.airingschedule

import android.app.Application
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.source.service.SourcePreferences
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mihon.feature.airingschedule.components.BellNotifyState
import mihon.feature.airingschedule.notification.ScheduleNotifications
import tachiyomi.domain.anime.interactor.GetLibraryAnime
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

class AiringScheduleScreenModel : StateScreenModel<AiringScheduleScreenModel.State>(State()) {

    private val repository = AiringScheduleRepository()
    private val schedulePrefs: SchedulePreferences = Injekt.get()
    private val sourcePreferences: SourcePreferences = Injekt.get()
    private val uploadDelayTracker: UploadDelayTracker = Injekt.get()
    private val application: Application = Injekt.get()
    private val getLibraryAnime: GetLibraryAnime = Injekt.get()

    private var allEntries: List<AiringScheduleEntry> = emptyList()
    private var hasLoaded = false

    init {
        loadSchedule()
        observePreferences()
        observeLibrary()
    }

    private fun observeLibrary() {
        screenModelScope.launch {
            getLibraryAnime.subscribe().collectLatest { libraryAnime ->
                val titles = libraryAnime.map { lib ->
                    lib.anime.title.trim().lowercase()
                }.toSet()
                mutableState.update { it.copy(libraryAnimeTitles = titles) }
            }
        }
    }

    private fun observePreferences() {
        screenModelScope.launch {
            combine(
                schedulePrefs.showOnlyFavoriteSources().changes(),
                schedulePrefs.filterBySourceAvailability().changes(),
                schedulePrefs.favoriteSourceIds().changes(),
                schedulePrefs.showAdultContent().changes(),
                schedulePrefs.titleLanguage().changes(),
                schedulePrefs.autoAddFromPinnedSources().changes(),
            ) { _ -> Unit }.collectLatest {
                if (hasLoaded) applyFilters()
            }
        }
    }

    fun loadSchedule() {
        screenModelScope.launch {
            mutableState.update { it.copy(isLoading = true, error = null) }
            try {
                val zone = ZoneId.systemDefault()
                val now = ZonedDateTime.now(zone)
                val weekStart = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .toLocalDate().atStartOfDay(zone)
                val weekEnd = weekStart.plusDays(7).minusSeconds(1)

                val autoRefreshEnabled = schedulePrefs.scheduleAutoRefreshEnabled().get()
                val entries: List<AiringScheduleEntry> = if (autoRefreshEnabled) {
                    val frequency = schedulePrefs.scheduleAutoRefreshFrequency().get()
                    val cache = ScheduleDataRefreshWorker.readCache(application)
                    val currentWeekStart = weekStart.toEpochSecond()
                    if (cache != null &&
                        cache.weekStartEpoch == currentWeekStart &&
                        ScheduleDataRefreshWorker.isCacheFresh(cache, frequency)
                    ) {
                        cache.entries.map { it.toEntry() }
                    } else {
                        val includeAdult = schedulePrefs.showAdultContent().get()
                        repository.getWeeklySchedule(
                            weekStart.toEpochSecond(),
                            weekEnd.toEpochSecond(),
                            includeAdult = includeAdult,
                        )
                    }
                } else {
                    val includeAdult = schedulePrefs.showAdultContent().get()
                    repository.getWeeklySchedule(
                        weekStart.toEpochSecond(),
                        weekEnd.toEpochSecond(),
                        includeAdult = includeAdult,
                    )
                }

                allEntries = entries
                hasLoaded = true

                val delays = if (schedulePrefs.uploadDelayEnabled().get()) {
                    uploadDelayTracker.getDelays()
                } else {
                    emptyMap()
                }

                rescheduleSeriesAlarms()

                applyFilters(
                    entries = allEntries,
                    delays = delays,
                    weekStart = weekStart.toLocalDate(),
                    weekEnd = weekEnd.toLocalDate(),
                )
            } catch (e: Exception) {
                mutableState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun applyFilters(
        entries: List<AiringScheduleEntry> = allEntries,
        delays: Map<String, Long> = if (schedulePrefs.uploadDelayEnabled().get()) uploadDelayTracker.getDelays() else emptyMap(),
        weekStart: LocalDate? = mutableState.value.weekStartDate,
        weekEnd: LocalDate? = mutableState.value.weekEndDate,
    ) {
        val showOnlyFavorites = schedulePrefs.showOnlyFavoriteSources().get()
        val favoriteIds = schedulePrefs.favoriteSourceIds().get()
        val titleLang = schedulePrefs.titleLanguage().get()
        val autoAdd = schedulePrefs.autoAddFromPinnedSources().get()
        val pinnedSources = sourcePreferences.pinnedSources().get()
        val zone = ZoneId.systemDefault()

        val filtered = entries.filter { _ ->
            if (showOnlyFavorites && favoriteIds.isEmpty()) return@filter true
            true
        }

        val grouped = filtered.groupBy { entry ->
            val priorityDelay = getPriorityDelay(delays, pinnedSources, favoriteIds)
            val airTime = if (priorityDelay != null) {
                entry.airingAt + (priorityDelay * 60)
            } else {
                entry.airingAt
            }
            ZonedDateTime.ofInstant(
                Instant.ofEpochSecond(airTime),
                zone,
            ).dayOfWeek
        }

        mutableState.update {
            it.copy(
                isLoading = false,
                scheduleByDay = grouped,
                weekStartDate = weekStart,
                weekEndDate = weekEnd,
                titleLanguage = titleLang,
                sourceDelays = delays,
                favoriteSourceIds = favoriteIds,
                pinnedSourceIds = pinnedSources,
                autoAddFromPinnedSources = autoAdd,
                notifyOnceMediaIds = schedulePrefs.notifyOnceMediaIds().get(),
                notifySeriesMediaIds = schedulePrefs.notifySeriesMediaIds().get(),
            )
        }
    }

    /** Determines the current bell state for a given schedule entry. */
    fun notifyStateFor(mediaId: Int): BellNotifyState {
        val state = mutableState.value
        return when {
            mediaId.toString() in state.notifySeriesMediaIds -> BellNotifyState.SERIES
            mediaId.toString() in state.notifyOnceMediaIds -> BellNotifyState.ONCE
            else -> BellNotifyState.NONE
        }
    }

    /** Toggles a single alert for the next upcoming episode of this anime. */
    fun toggleNotifyOnce(entry: AiringScheduleEntry) {
        val key = entry.mediaId.toString()
        val current = schedulePrefs.notifyOnceMediaIds().get()
        val seriesCurrent = schedulePrefs.notifySeriesMediaIds().get()
        if (key in current) {
            schedulePrefs.notifyOnceMediaIds().set(current - key)
            ScheduleNotifications.cancel(application, entry)
        } else {
            schedulePrefs.notifyOnceMediaIds().set(current + key)
            schedulePrefs.notifySeriesMediaIds().set(seriesCurrent - key)
            ScheduleNotifications.ensureScheduled(application, entry)
        }
        applyFilters()
    }

    /** Toggles recurring alerts for every future episode of this anime until it finishes airing. */
    fun toggleNotifySeries(entry: AiringScheduleEntry) {
        val key = entry.mediaId.toString()
        val seriesCurrent = schedulePrefs.notifySeriesMediaIds().get()
        val onceCurrent = schedulePrefs.notifyOnceMediaIds().get()
        if (key in seriesCurrent) {
            schedulePrefs.notifySeriesMediaIds().set(seriesCurrent - key)
            ScheduleNotifications.cancelAllForMedia(application, entry.mediaId, allEntries)
        } else {
            schedulePrefs.notifySeriesMediaIds().set(seriesCurrent + key)
            schedulePrefs.notifyOnceMediaIds().set(onceCurrent - key)
            rescheduleSeriesAlarms()
        }
        applyFilters()
    }

    private fun rescheduleSeriesAlarms() {
        val seriesIds = schedulePrefs.notifySeriesMediaIds().get()
        if (seriesIds.isEmpty()) return
        allEntries
            .filter { it.mediaId.toString() in seriesIds && !it.hasAired() }
            .forEach { ScheduleNotifications.ensureScheduled(application, it) }
    }

    /**
     * Returns the delay (minutes) of the highest-priority pinned source that has a known delay.
     * Priority: first iterate pinned sources (from Browse), then favorite sources.
     * Falls back to null if none have a known delay.
     */
    private fun getPriorityDelay(
        delays: Map<String, Long>,
        pinnedSources: Set<String>,
        favoriteSources: Set<String>,
    ): Long? {
        if (delays.isEmpty()) return null
        for (sourceId in pinnedSources) {
            delays[sourceId]?.let { return it }
        }
        for (sourceId in favoriteSources) {
            delays[sourceId]?.let { return it }
        }
        return null
    }

    fun selectDay(day: DayOfWeek) {
        mutableState.update { it.copy(selectedDay = day) }
    }

    fun clearLearnedDelays() {
        uploadDelayTracker.clearAllDelays()
        applyFilters(delays = emptyMap())
    }

    data class State(
        val isLoading: Boolean = true,
        val scheduleByDay: Map<DayOfWeek, List<AiringScheduleEntry>> = emptyMap(),
        val selectedDay: DayOfWeek = ZonedDateTime.now().dayOfWeek,
        val weekStartDate: LocalDate? = null,
        val weekEndDate: LocalDate? = null,
        val error: String? = null,
        val titleLanguage: SchedulePreferences.TitleLanguage = SchedulePreferences.TitleLanguage.USER_PREFERRED,
        val sourceDelays: Map<String, Long> = emptyMap(),
        val favoriteSourceIds: Set<String> = emptySet(),
        val pinnedSourceIds: Set<String> = emptySet(),
        val autoAddFromPinnedSources: Boolean = false,
        val notifyOnceMediaIds: Set<String> = emptySet(),
        val notifySeriesMediaIds: Set<String> = emptySet(),
        val libraryAnimeTitles: Set<String> = emptySet(),
    )
}
