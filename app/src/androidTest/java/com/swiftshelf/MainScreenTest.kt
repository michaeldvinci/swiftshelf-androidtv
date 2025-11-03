package com.swiftshelf

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.swiftshelf.data.model.LibraryItem
import com.swiftshelf.data.model.MediaType
import com.swiftshelf.ui.theme.SwiftShelfTheme
import org.junit.Rule
import org.junit.Test

class MainScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mainScreen_displaysAppName() {
        composeTestRule.setContent {
            SwiftShelfTheme {
                MainScreen()
            }
        }

        composeTestRule
            .onNodeWithText("SwiftShelf")
            .assertExists()
    }

    @Test
    fun libraryItemCard_displaysCorrectInformation() {
        val testItem = LibraryItem(
            id = "1",
            title = "Test Title",
            artist = "Test Artist",
            albumArt = null,
            duration = 100L,
            mediaUrl = "test://url",
            mediaType = MediaType.AUDIO
        )

        composeTestRule.setContent {
            SwiftShelfTheme {
                LibraryItemCard(
                    item = testItem,
                    onItemClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Test Title").assertExists()
        composeTestRule.onNodeWithText("Test Artist").assertExists()
    }
}