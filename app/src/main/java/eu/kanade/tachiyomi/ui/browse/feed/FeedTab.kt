package eu.kanade.tachiyomi.ui.browse.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabContent
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreen
import eu.kanade.tachiyomi.ui.feed.FeedItem
import eu.kanade.tachiyomi.ui.feed.FeedScreenModel
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen

@Composable
fun Screen.feedTab(): TabContent {
    val navigator = LocalNavigator.currentOrThrow
    val screenModel = rememberScreenModel { FeedScreenModel() }
    val state by screenModel.state.collectAsState()

    return TabContent(
        titleRes = MR.strings.label_feed,
        actions = persistentListOf(
            AppBar.Action(
                title = stringResource(MR.strings.action_webview_refresh),
                icon = Icons.Outlined.Refresh,
                onClick = { screenModel.loadFeeds() },
            ),
        ),
        content = { contentPadding, _ ->
            when {
                state.isLoading -> LoadingScreen(modifier = Modifier.padding(contentPadding))
                state.feedItems.isEmpty() -> {
                    EmptyScreen(
                        modifier = Modifier.padding(contentPadding),
                        stringRes = MR.strings.information_no_entries_found,
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .padding(contentPadding)
                            .fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        items(items = state.feedItems, key = { it.savedSearch.id }) { item ->
                            FeedItemRow(
                                item = item,
                                onClick = {
                                    navigator.push(
                                        BrowseSourceScreen(item.savedSearch.source, item.savedSearch.query),
                                    )
                                },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun FeedItemRow(item: FeedItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.savedSearch.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.sourceName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
