package me.animeheaven.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MobileUiSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunchesAndNavigatesBottomTabs() {
        composeRule.waitUntil(timeoutMillis = 15_000L) {
            composeRule.onAllNodesWithText("Schedule").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText("Schedule").onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("New").onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("Popular").onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("Bookmarks").onFirst().assertIsDisplayed()

        composeRule.onAllNodesWithText("New").onFirst().performClick()
        composeRule.onAllNodesWithText("Popular").onFirst().performClick()
        composeRule.onAllNodesWithText("Bookmarks").onFirst().performClick()
        composeRule.onAllNodesWithText("Schedule").onFirst().performClick()
    }

    @Test
    fun openSettingsSwitchThemeAndReturn() {
        composeRule.waitUntil(timeoutMillis = 15_000L) {
            composeRule.onAllNodesWithText("Schedule").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Settings & Themes").performClick()

        composeRule.waitUntil(timeoutMillis = 10_000L) {
            composeRule.onAllNodesWithText("Settings & Themes").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Settings & Themes").assertIsDisplayed()
        composeRule.onNodeWithText("Midnight Indigo").performClick()
        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.waitUntil(timeoutMillis = 10_000L) {
            composeRule.onAllNodesWithText("Schedule").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun openNativeSearchScreen() {
        composeRule.waitUntil(timeoutMillis = 15_000L) {
            composeRule.onAllNodesWithText("Search").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText("Search").onFirst().performClick()

        composeRule.waitUntil(timeoutMillis = 10_000L) {
            composeRule.onAllNodesWithText("Search anime...").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onNodeWithContentDescription("Back").let {
                    runCatching { it.assertIsDisplayed(); true }.getOrDefault(false)
                }
        }
        composeRule.onNodeWithContentDescription("Back").performClick()
    }
}
