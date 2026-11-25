package me.animeheaven.android.ui.screens.search

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
import me.animeheaven.android.data.model.MultilingualFastSearchResult
import me.animeheaven.android.data.model.MultilingualFullSearchResult
import me.animeheaven.android.data.network.MultilingualQueryNormalizer
import me.animeheaven.android.data.network.QueryScript
import me.animeheaven.android.data.repository.AnimeHeavenRepository
import me.animeheaven.android.data.runCatchingCancellable

interface SearchGateway {
    fun observeRecentSearches(): Flow<List<String>>
    suspend fun fastSearch(query: String): List<FastSearchItem>
    suspend fun fullSearch(query: String): Pair<String, List<AnimeCard>>
    suspend fun recordSearch(query: String)
    suspend fun fastSearchMultilingual(
        query: String,
        forcedCandidate: String? = null
    ): MultilingualFastSearchResult = MultilingualFastSearchResult(items = fastSearch(forcedCandidate ?: query))

    suspend fun fullSearchMultilingual(
        query: String,
        forcedCandidate: String? = null
    ): MultilingualFullSearchResult {
        val (title, items) = fullSearch(forcedCandidate ?: query)
        return MultilingualFullSearchResult(title = title, items = items)
    }
}

class RepositorySearchGateway(
    private val repository: AnimeHeavenRepository
) : SearchGateway {
    override fun observeRecentSearches(): Flow<List<String>> = repository.observeRecentSearches()
    override suspend fun fastSearch(query: String) = repository.fastSearch(query)
    override suspend fun fullSearch(query: String) = repository.fullSearch(query)
    override suspend fun recordSearch(query: String) = repository.recordSearch(query)
    override suspend fun fastSearchMultilingual(query: String, forcedCandidate: String?) =
        repository.fastSearchMultilingual(query, forcedCandidate)
    override suspend fun fullSearchMultilingual(query: String, forcedCandidate: String?) =
        repository.fullSearchMultilingual(query, forcedCandidate)
}

@Immutable
data class SearchUiState(
    val query: String = "",
    val fastResults: List<FastSearchItem> = emptyList(),
    val fullResults: List<AnimeCard> = emptyList(),
    val fullResultsTitle: String = "",
    val matchedQuery: String? = null,
    val candidateQueries: List<String> = emptyList(),
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

    private val baseDebounceMillis = debounceMillis
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
                        val script = MultilingualQueryNormalizer.analyze(trimmed).script
                        if (script != QueryScript.LATIN && baseDebounceMillis > 0L) {
                            delay(baseDebounceMillis)
                            if (revision != queryRevision) return@collect
                        }
                        runFastSearch(trimmed)
                    } else {
                        _uiState.update {
                            it.copy(
                                fastResults = emptyList(),
                                fullResults = emptyList(),
                                matchedQuery = null,
                                candidateQueries = emptyList(),
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
            it.copy(
                query = capped,
                fastResults = emptyList(),
                matchedQuery = null,
                candidateQueries = emptyList(),
                isLoading = capped.trim().length >= 2,
                isFullSearchMode = false,
                errorMessage = null
            )
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
                matchedQuery = null,
                candidateQueries = emptyList(),
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

    fun selectCandidateQuery(candidate: String) {
        val trimmed = _uiState.value.query.trim()
        if (trimmed.isEmpty() || candidate.isBlank()) return
        queryRevision++
        val fullMode = _uiState.value.isFullSearchMode
        val existingCandidates = _uiState.value.candidateQueries

        activeSearchJob?.cancel()
        activeSearchJob = viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, matchedQuery = candidate)
            }
            if (fullMode) {
                runCatchingCancellable {
                    gateway.fullSearchMultilingual(trimmed, forcedCandidate = candidate)
                }.onSuccess { result ->
                    coroutineContext.ensureActive()
                    val mergedCandidates = if (existingCandidates.isNotEmpty()) existingCandidates else result.candidateQueries
                    _uiState.update {
                        it.copy(
                            fullResultsTitle = result.title.ifBlank { candidate },
                            fullResults = result.items,
                            matchedQuery = candidate,
                            candidateQueries = mergedCandidates,
                            isFullSearchMode = true,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                    preloader?.preloadCatalogPage(result.items)
                }.onFailure { err ->
                    coroutineContext.ensureActive()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = err.message ?: "Search failed"
                        )
                    }
                }
            } else {
                runCatchingCancellable {
                    gateway.fastSearchMultilingual(trimmed, forcedCandidate = candidate)
                }.onSuccess { result ->
                    coroutineContext.ensureActive()
                    val mergedCandidates = if (existingCandidates.isNotEmpty()) existingCandidates else result.candidateQueries
                    _uiState.update {
                        it.copy(
                            fastResults = result.items,
                            matchedQuery = candidate,
                            candidateQueries = mergedCandidates,
                            isFullSearchMode = false,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                    preloader?.preloadSearchMatches(result.items)
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
                gateway.fullSearchMultilingual(trimmed)
            }.onSuccess { result ->
                coroutineContext.ensureActive()
                _uiState.update {
                    it.copy(
                        fullResultsTitle = result.title.ifBlank { trimmed },
                        fullResults = result.items,
                        matchedQuery = result.matchedQuery,
                        candidateQueries = result.candidateQueries,
                        isFullSearchMode = true,
                        isLoading = false,
                        errorMessage = null
                    )
                }
                preloader?.preloadCatalogPage(result.items)
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
                gateway.fastSearchMultilingual(trimmedQuery)
            }.onSuccess { result ->
                coroutineContext.ensureActive()
                _uiState.update {
                    it.copy(
                        fastResults = result.items,
                        matchedQuery = result.matchedQuery,
                        candidateQueries = result.candidateQueries,
                        isFullSearchMode = false,
                        isLoading = false,
                        errorMessage = null
                    )
                }
                preloader?.preloadSearchMatches(result.items)
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
