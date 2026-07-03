package eu.kanade.tachiyomi.ui.feed

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.domain.source.interactor.GetSavedSearchGlobalFeed
import tachiyomi.domain.source.model.SavedSearch
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class FeedScreenModel : StateScreenModel<FeedScreenModel.State>(State()) {

    private val getSavedSearchGlobalFeed: GetSavedSearchGlobalFeed = Injekt.get()
    private val sourceManager: SourceManager = Injekt.get()

    init {
        loadFeeds()
    }

    fun loadFeeds() {
        screenModelScope.launch {
            mutableState.update { it.copy(isLoading = true, error = null) }
            try {
                val savedSearches = getSavedSearchGlobalFeed.await()
                val feedItems = savedSearches.map { ss ->
                    val source = try { sourceManager.get(ss.source) } catch (_: Exception) { null }
                    FeedItem(
                        savedSearch = ss,
                        sourceName = source?.name ?: "Source #${ss.source}",
                    )
                }
                mutableState.update { it.copy(isLoading = false, feedItems = feedItems) }
            } catch (e: Exception) {
                mutableState.update { it.copy(isLoading = false, error = e.message ?: "Unknown error") }
            }
        }
    }

    data class State(
        val isLoading: Boolean = true,
        val feedItems: List<FeedItem> = emptyList(),
        val error: String? = null,
    )
}

data class FeedItem(
    val savedSearch: SavedSearch,
    val sourceName: String,
)
