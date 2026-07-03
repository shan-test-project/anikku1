package mihon.feature.airingschedule

import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum

class SchedulePreferences(
    private val preferenceStore: PreferenceStore,
) {
    enum class TitleLanguage { USER_PREFERRED, ENGLISH, ROMAJI, NATIVE }
    enum class UploadDelayInterval { THIRTY_MIN, ONE_HOUR, TWO_HOURS, SIX_HOURS, TWELVE_HOURS, NEVER }
    enum class AutoRefreshFrequency { EVERY_1_DAY, EVERY_2_DAYS, EVERY_3_DAYS, EVERY_4_DAYS, EVERY_5_DAYS, EVERY_6_DAYS, EVERY_7_DAYS }

    fun favoriteSourceIds() = preferenceStore.getStringSet(
        "schedule_favorite_source_ids",
        emptySet(),
    )

    fun showOnlyFavoriteSources() = preferenceStore.getBoolean(
        "schedule_show_only_favorite_sources",
        false,
    )

    fun filterBySourceAvailability() = preferenceStore.getBoolean(
        "schedule_filter_by_source_availability",
        false,
    )

    fun titleLanguage() = preferenceStore.getEnum(
        "schedule_title_language",
        TitleLanguage.USER_PREFERRED,
    )

    fun showAdultContent() = preferenceStore.getBoolean(
        "schedule_show_adult_content",
        false,
    )

    fun uploadDelayEnabled() = preferenceStore.getBoolean(
        "schedule_upload_delay_enabled",
        false,
    )

    fun uploadDelayRefreshInterval() = preferenceStore.getEnum(
        "schedule_upload_delay_interval",
        UploadDelayInterval.ONE_HOUR,
    )

    fun sourceUploadDelays() = preferenceStore.getString(
        "schedule_source_upload_delays",
        "{}",
    )

    fun lastDelayCheckTime() = preferenceStore.getLong(
        "schedule_last_delay_check_time",
        0L,
    )

    fun autoAddFromPinnedSources() = preferenceStore.getBoolean(
        "schedule_auto_add_from_pinned_sources",
        false,
    )

    fun scheduleAutoRefreshEnabled() = preferenceStore.getBoolean(
        "schedule_auto_refresh_enabled",
        false,
    )

    fun scheduleAutoRefreshFrequency() = preferenceStore.getEnum(
        "schedule_auto_refresh_frequency",
        AutoRefreshFrequency.EVERY_7_DAYS,
    )

    fun scheduleLastAutoRefresh() = preferenceStore.getLong(
        "schedule_last_auto_refresh",
        0L,
    )

    /** Media ids for which the user wants a single alert for the next upcoming episode. */
    fun notifyOnceMediaIds() = preferenceStore.getStringSet(
        "schedule_notify_once_media_ids",
        emptySet(),
    )

    /** Media ids for which the user wants alerts for every episode until the series finishes. */
    fun notifySeriesMediaIds() = preferenceStore.getStringSet(
        "schedule_notify_series_media_ids",
        emptySet(),
    )

    /** Composite "mediaId:episode" keys for which an alarm has already been scheduled. */
    fun scheduledAlarmKeys() = preferenceStore.getStringSet(
        "schedule_scheduled_alarm_keys",
        emptySet(),
    )
}
