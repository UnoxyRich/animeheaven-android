package me.animeheaven.android

import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MobileUiSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun fullAppWalkthroughCapturesScreenshots() {
        waitForText("Schedule", timeoutMs = 15_000L)
        composeRule.onAllNodesWithText("Schedule").onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("New").onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("Popular").onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("Bookmarks").onFirst().assertIsDisplayed()
        settle(3_000L)
        saveScreenshot("01-phone-home")

        // New tab
        composeRule.onAllNodesWithText("New").onFirst().performClick()
        settle(2_500L)
        saveScreenshot("02-phone-new")

        // Popular tab
        composeRule.onAllNodesWithText("Popular").onFirst().performClick()
        settle(2_500L)
        saveScreenshot("03-phone-popular")

        // Bookmarks tab
        composeRule.onAllNodesWithText("Bookmarks").onFirst().performClick()
        settle(1_500L)
        saveScreenshot("04-phone-bookmarks")

        // Back to Schedule then open Search
        composeRule.onAllNodesWithText("Schedule").onFirst().performClick()
        settle(800L)
        composeRule.onAllNodesWithText("Search").onFirst().performClick()
        composeRule.waitUntil(timeoutMillis = 10_000L) {
            composeRule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNode(hasSetTextAction()).performTextInput("frieren")
        settle(2_500L)
        saveScreenshot("05-phone-search")

        // Return from Search and open Settings
        composeRule.onNodeWithContentDescription("Back").performClick()
        waitForText("Schedule", timeoutMs = 10_000L)
        composeRule.onNodeWithContentDescription("Settings & Themes").performClick()
        waitForText("Settings & Themes", timeoutMs = 10_000L)
        composeRule.onNodeWithText("Settings & Themes").assertIsDisplayed()
        settle(1_200L)
        saveScreenshot("06-phone-settings-dark")

        // Switch theme to Midnight Indigo
        composeRule.onNodeWithText("Midnight Indigo").performClick()
        settle(1_200L)
        saveScreenshot("07-phone-settings-indigo")

        // Return to Home in Midnight Indigo
        composeRule.onNodeWithContentDescription("Back").performClick()
        waitForText("Schedule", timeoutMs = 10_000L)
        settle(1_500L)
        saveScreenshot("08-phone-home-indigo")
    }

    private fun waitForText(text: String, timeoutMs: Long) {
        composeRule.waitUntil(timeoutMillis = timeoutMs) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun settle(ms: Long) {
        composeRule.waitForIdle()
        Thread.sleep(ms)
        composeRule.waitForIdle()
    }

    private fun saveScreenshot(name: String) {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val path = "/data/local/tmp/ci-screenshots/$name.png"
        val pfd = uiAutomation.executeShellCommand(
            "sh -c 'mkdir -p /data/local/tmp/ci-screenshots && screencap -p $path && ls -l $path'"
        )
        val output = ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes().decodeToString() }
        assertTrue("Screenshot failed for $name: $output", output.contains("$name.png"))
    }
}
