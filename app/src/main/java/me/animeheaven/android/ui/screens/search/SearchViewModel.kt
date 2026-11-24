package me.animeheaven.android.ui.screens.search

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.animeheaven.android.data.cache.AnimePreloader
import me.animeheaven.android.data.model.AnimeCard
import me.animeheaven.android.data.model.FastSearchItem
import me.animeheaven.android.data.repository.AnimeHeavenRepository
import me.animeheaven.android.data.runCatchingCancellable

interface SearchGateway {
    fun observeRecentSearches(): Flow<List<String>>
    suspend fun fastSearch(query: String): List<FastSearchItem>
    suspend fun fullSearch(query: String): Pair<String, List<AnimeCard>>
    suspend fun recordSearch(query: String)
}

class RepositorySearchGateway(
    private val repository: AnimeHeavenRepository
) : SearchGateway {
    override fun observeRecentSearches(): Flow<List<String>> = repository.observeRecentSearches()
    override suspend fun fastSearch(query: String) = repository.fastSearch(query)
    override suspend fun fullSearch(query: String) = repository.fullSearch(query)
    override suspend fun recordSearch(query: String) = repository.recordSearch(query)
}

@Immutable
data class SearchUiState(
    val query: String = "",
    val fastResults: List<FastSearchItem> = emptyList(),
    val fullResults: List<AnimeCard> = emptyList(),
    val fullResultsTitle: String = "",
    val isFullSearchMode: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val recentSearches: List<String> = emptyList()
)

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val gateway: SearchGateway,
    debounceMillis: Long = 160L,
    private val preloader: AnimePreloader? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var activeSearchJob: Job? = null
    private var queryRevision = 0L
    private val typedQueryEvents = MutableSharedFlow<Pair<Long, String>>(
        replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        viewModelScope.launch {
            gateway.observeRecentSearches().collect { recent ->
                _uiState.update { it.copy(recentSearches = recent) }
                preloader?.preloadSearchEcosystem(recent)
            }
        }

        viewModelScope.launch {
            typedQueryEvents
                .debounce(debounceMillis)
                .collect { (revision, trimmed) ->
                    if (revision != queryRevision) return@collect
                    if (trimmed.length >= 2) {
                        runFastSearch(trimmed)
                    } else {
                        _uiState.update {
                            it.copy(
                                fastResults = emptyList(),
                                fullResults = emptyList(),
                                isFullSearchMode = false,
                                isLoading = false,
                                errorMessage = null
                            )
                        }
                    }
                }
        }
    }

    fun updateQuery(newQuery: String) {
        val capped = newQuery.take(100)
        if (_uiState.value.query == capped) return
        val revision = ++queryRevision
        activeSearchJob?.cancel()
        _uiState.update {
            it.copy(query = capped, fastResults = emptyList(), isLoading = capped.trim().length >= 2,
                isFullSearchMode = false, errorMessage = null)
        }
        typedQueryEvents.tryEmit(revision to capped.trim())
    }

    fun clearQuery() {
        queryRevision++
        activeSearchJob?.cancel()
        _uiState.update {
            it.copy(
                query = "",
                fastResults = emptyList(),
                fullResults = emptyList(),
                fullResultsTitle = "",
                isFullSearchMode = false,
                isLoading = false,
                errorMessage = null
            )
        }
    }

    fun applyRecentQuery(recent: String) {
        _uiState.update { it.copy(query = recent) }
        submitFullSearch()
    }

    fun showLiveSuggestions() {
        _uiState.update { it.copy(isFullSearchMode = false) }
    }

    fun submitFullSearch() {
        val trimmed = _uiState.value.query.trim()
        if (trimmed.isEmpty()) return
        queryRevision++

        activeSearchJob?.cancel()
        activeSearchJob = viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, isFullSearchMode = true)
            }
            runCatchingCancellable {
                gateway.recordSearch(trimmed)
                gateway.fullSearch(trimmed)
            }.onSuccess { (title, results) ->
                coroutineContext.ensureActive()
                _uiState.update {
                    it.copy(
                        fullResultsTitle = title.ifBlank { trimmed },
                        fullResults = results,
                        isFullSearchMode = true,
                        isLoading = false,
                        errorMessage = null
                    )
                }
                preloader?.preloadCatalogPage(results)
            }.onFailure { err ->
                coroutineContext.ensureActive()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isFullSearchMode = true,
                        errorMessage = err.message ?: "Search failed"
                    )
                }
            }
        }
    }

    private fun runFastSearch(trimmedQuery: String) {
        activeSearchJob?.cancel()
        activeSearchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatchingCancellable {
                gateway.fastSearch(trimmedQuery)
            }.onSuccess { items ->
                coroutineContext.ensureActive()
                _uiState.update {
                    it.copy(
                        fastResults = items,
                        isFullSearchMode = false,
                        isLoading = false,
                        errorMessage = null
                    )
                }
                preloader?.preloadSearchMatches(items)
            }.onFailure { err ->
                coroutineContext.ensureActive()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = err.message ?: "Live search failed"
                    )
                }
            }
        }
    }
}
