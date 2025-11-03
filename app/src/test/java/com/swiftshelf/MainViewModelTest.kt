package com.swiftshelf

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private lateinit var viewModel: MainViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = MainViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty`() = runTest {
        val initialState = viewModel.uiState.value
        assertTrue(initialState.libraryItems.isEmpty())
        assertNull(initialState.selectedItem)
        assertFalse(initialState.isLoading)
        assertNull(initialState.error)
    }

    @Test
    fun `selecting item updates selected state`() = runTest {
        val testItem = LibraryItem(
            id = "1",
            title = "Test Title",
            artist = "Test Artist",
            albumArt = null,
            duration = 100L,
            mediaUrl = "test://url",
            mediaType = MediaType.AUDIO
        )

        viewModel.onItemSelected(testItem)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(testItem, viewModel.uiState.value.selectedItem)
    }
}