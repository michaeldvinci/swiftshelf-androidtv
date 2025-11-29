package com.swiftshelf

import android.app.Application
import com.swiftshelf.data.model.LibraryItem
import com.swiftshelf.data.model.Media
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SwiftShelfViewModelTest {

    @Mock
    private lateinit var mockApplication: Application

    private lateinit var viewModel: SwiftShelfViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = SwiftShelfViewModel(mockApplication)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial uiState is Loading`() = runTest {
        assertEquals(SwiftShelfViewModel.UiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `selecting item updates selectedItem state`() = runTest {
        val testItem = LibraryItem(
            id = "1",
            media = Media(
                duration = 100.0,
                coverPath = null,
                metadata = null,
                audioFiles = null,
                chapters = null,
                tracks = null
            ),
            userMediaProgress = null,
            libraryFiles = null,
            addedAt = 0.0,
            updatedAt = 0.0
        )

        viewModel.selectItem(testItem)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(testItem, viewModel.selectedItem.value)
    }
}
