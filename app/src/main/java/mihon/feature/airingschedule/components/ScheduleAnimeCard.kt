package mihon.feature.airingschedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import mihon.feature.airingschedule.AiringScheduleEntry
import mihon.feature.airingschedule.SchedulePreferences
import mihon.feature.airingschedule.UploadDelayTracker
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter12h = DateTimeFormatter.ofPattern("h:mm a")

@Composable
fun ScheduleAnimeCard(
    entry: AiringScheduleEntry,
    titleLanguage: SchedulePreferences.TitleLanguage,
    sourceDelays: Map<String, Long>,
    favoriteSourceIds: Set<String>,
    pinnedSourceIds: Set<String>,
    autoAddFromPinnedSources: Boolean,
    notifyState: BellNotifyState,
    onSearchClick: (String) -> Unit,
    onAddToLibraryClick: (String) -> Unit,
    onToggleNotifyOnce: () -> Unit,
    onToggleNotifySeries: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val displayTitle = entry.displayTitle(titleLanguage)

    val officialAirTime = Instant.ofEpochSecond(entry.airingAt)
        .atZone(zone)
        .format(timeFormatter12h)

    val hasAired = entry.hasAired()

    val priorityDelay: Long? = remember(sourceDelays, pinnedSourceIds, favoriteSourceIds) {
        if (sourceDelays.isEmpty()) return@remember null
        for (id in pinnedSourceIds) {
            sourceDelays[id]?.let { return@remember it }
        }
        for (id in favoriteSourceIds) {
            sourceDelays[id]?.let { return@remember it }
        }
        null
    }

    val expectedUploadTime: String? = priorityDelay?.let {
        val adjusted = UploadDelayTracker.adjustedAirTime(entry.airingAt, it)
        Instant.ofEpochSecond(adjusted).atZone(zone).format(timeFormatter12h)
    }

    var countdown by remember { mutableStateOf(formatCountdown(entry.airingAt)) }
    LaunchedEffect(entry.airingAt) {
        while (!entry.hasAired()) {
            countdown = formatCountdown(entry.airingAt)
            delay(60_000L)
        }
        countdown = null
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .clickable { onSearchClick(displayTitle) }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box {
                AsyncImage(
                    model = entry.coverImageUrl,
                    contentDescription = displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 56.dp, height = 80.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
                if (entry.isAdult) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .clip(RoundedCornerShape(bottomStart = 6.dp))
                            .background(MaterialTheme.colorScheme.error)
                            .padding(horizontal = 3.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text = "18+",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onError,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (hasAired) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                    ) {
                        Text(
                            text = if (hasAired) "Aired $officialAirTime" else officialAirTime,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (hasAired) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }

                    if (!hasAired && countdown != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                        ) {
                            Text(
                                text = "in $countdown",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }

                    if (expectedUploadTime != null && expectedUploadTime != officialAirTime) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(
                                text = "Src ~$expectedUploadTime",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }

                    val episodeText = if (entry.totalEpisodes != null) {
                        "Ep ${entry.episode} / ${entry.totalEpisodes}"
                    } else {
                        "Ep ${entry.episode}"
                    }
                    Text(
                        text = episodeText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    entry.format?.let { fmt ->
                        Text(
                            text = fmt.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    entry.averageScore?.let { score ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                text = "${score / 10.0}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(2.dp))

            Column(
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { },
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FilledTonalIconButton(
                    onClick = { onSearchClick(displayTitle) },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = if (hasAired) Icons.Outlined.PlayCircle else Icons.Outlined.Search,
                        contentDescription = if (hasAired) "Watch / Find episode" else "Find in sources",
                        modifier = Modifier.size(18.dp),
                    )
                }

                IconButton(
                    onClick = { onAddToLibraryClick(displayTitle) },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BookmarkBorder,
                        contentDescription = if (autoAddFromPinnedSources) "Add to library via pinned source" else "Add to library",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }

                EpisodeBell(
                    state = notifyState,
                    onTap = onToggleNotifyOnce,
                    onLongPress = onToggleNotifySeries,
                )
            }
        }
    }
}

private fun formatCountdown(airingAtEpochSeconds: Long): String? {
    val nowSeconds = System.currentTimeMillis() / 1000L
    val diff = airingAtEpochSeconds - nowSeconds
    if (diff <= 0) return null
    val hours = diff / 3600
    val minutes = (diff % 3600) / 60
    return when {
        hours >= 24 -> {
            val days = hours / 24
            val remHours = hours % 24
            if (remHours > 0) "${days}d ${remHours}h" else "${days}d"
        }
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}
