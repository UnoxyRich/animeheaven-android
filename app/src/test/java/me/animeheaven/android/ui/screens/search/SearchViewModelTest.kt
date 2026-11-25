package me.animeheaven.android.ui.screens.search

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.animeheaven.android.data.model.AnimeCard
import me.animeheaven.android.data.model.FastSearchItem
import me.animeheaven.android.data.model.MultilingualFastSearchResult
import me.animeheaven.android.data.model.MultilingualFullSearchResult
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeSearchGateway : SearchGateway {
        val recentFlow = MutableStateFlow(listOf("dandadan", "clover"))
        val fastQueries = mutableListOf<String>()
        val fullQueries = mutableListOf<String>()
        val recorded = mutableListOf<String>()

        override fun observeRecentSearches(): Flow<List<String>> = recentFlow

        override suspend fun fastSearch(query: String): List<FastSearchItem> {
            fastQueries.add(query)
            return listOf(
                FastSearchItem("0pqfl", "Black Clover 2nd Season", "https://animeheaven.me/image.php?82gob")
            )
        }

        override suspend fun fullSearch(query: String): Pair<String, List<AnimeCard>> {
            fullQueries.add(query)
            return query to listOf(
                AnimeCard(id = "l1cen", title = "Black Clover", coverUrl = "https://animeheaven.me/image.php?mg1oq")
            )
        }

        override suspend fun recordSearch(query: String) {
            recorded.add(query)
        }

        override suspend fun fastSearchMultilingual(
            query: String,
            forcedCandidate: String?
        ): MultilingualFastSearchResult {
            val effective = forcedCandidate ?: query
            return if (query == "葬送的芙莉莲" && forcedCandidate == null) {
                fastQueries.add(effective)
                MultilingualFastSearchResult(
                    items = listOf(
                        FastSearchItem("ak2gr", "Sousou no Frieren", "https://animeheaven.me/image.php?z4mfi")
                    ),
                    matchedQuery = "Sousou no Frieren",
                    candidateQueries = listOf("Sousou no Frieren", "Frieren: Beyond Journey's End")
                )
            } else {
                MultilingualFastSearchResult(
                    items = fastSearch(effective),
                    matchedQuery = forcedCandidate,
                    candidateQueries = listOfNotNull(forcedCandidate)
                )
            }
        }

        override suspend fun fullSearchMultilingual(
            query: String,
            forcedCandidate: String?
        ): MultilingualFullSearchResult {
            val effective = forcedCandidate ?: query
            return if (query == "葬送的芙莉莲") {
                fullQueries.add(effective)
                MultilingualFullSearchResult(
                    title = effective,
                    items = listOf(
                        AnimeCard(id = "ak2gr", title = "Match for $effective", coverUrl = "https://animeheaven.me/image.php?z4mfi")
                    ),
                    matchedQuery = forcedCandidate ?: "Sousou no Frieren",
                    candidateQueries = listOf("Sousou no Frieren", "Frieren: Beyond Journey's End")
                )
            } else {
                val (title, items) = fullSearch(effective)
                MultilingualFullSearchResult(title = title, items = items, matchedQuery = forcedCandidate)
            }
        }
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `handles native IME query updates, debounced fastsearch, full submit, and clear`() = runTest {
        val gateway = FakeSearchGateway()
        val vm = SearchViewModel(gateway = gateway, debounceMillis = 250L)

        vm.updateQuery("bla")
        vm.updateQuery("bla c")
        assertEquals("bla c", vm.uiState.value.query)

        advanceTimeBy(300L)
        advanceUntilIdle()

        assertEquals(listOf("bla c"), gateway.fastQueries)
        assertEquals(1, vm.uiState.value.fastResults.size)
        assertFalse(vm.uiState.value.isFullSearchMode)

        vm.submitFullSearch()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isFullSearchMode)
        assertEquals(listOf("bla c"), gateway.fullQueries)
        assertEquals(listOf("bla c"), gateway.recorded)
        assertEquals(1, vm.uiState.value.fullResults.size)

        vm.clearQuery()
        advanceUntilIdle()
        assertEquals("", vm.uiState.value.query)
        assertTrue(vm.uiState.value.fastResults.isEmpty())
        assertTrue(vm.uiState.value.fullResults.isEmpty())
    }

    @Test
    fun `full submission and recent selection invalidate pending typing`() = runTest {
        val gateway = FakeSearchGateway()
        val vm = SearchViewModel(gateway = gateway, debounceMillis = 250L)
        vm.updateQuery("clover")
        advanceTimeBy(125)
        vm.submitFullSearch()
        advanceUntilIdle()
        vm.applyRecentQuery("dandadan")
        advanceUntilIdle()
        assertEquals(emptyList<String>(), gateway.fastQueries)
        assertEquals(listOf("clover", "dandadan"), gateway.fullQueries)
        assertTrue(vm.uiState.value.isFullSearchMode)
    }

    @Test
    fun `multilingual query preserves original query in history and supports candidate switching`() = runTest {
        val gateway = FakeSearchGateway()
        val vm = SearchViewModel(gateway = gateway, debounceMillis = 100L)

        vm.updateQuery("葬送的芙莉莲")
        advanceTimeBy(250L)
        advanceUntilIdle()

        assertEquals("Sousou no Frieren", vm.uiState.value.matchedQuery)
        assertEquals(
            listOf("Sousou no Frieren", "Frieren: Beyond Journey's End"),
            vm.uiState.value.candidateQueries
        )

        vm.submitFullSearch()
        advanceUntilIdle()
        assertEquals(listOf("葬送的芙莉莲"), gateway.recorded)

        vm.selectCandidateQuery("Frieren: Beyond Journey's End")
        advanceUntilIdle()
        assertEquals("葬送的芙莉莲", vm.uiState.value.query)
        assertEquals("Frieren: Beyond Journey's End", vm.uiState.value.matchedQuery)
        assertEquals("Match for Frieren: Beyond Journey's End", vm.uiState.value.fullResults.first().title)
        assertEquals(listOf("葬送的芙莉莲"), gateway.recorded)
    }
}
