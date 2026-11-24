package me.animeheaven.android

import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
import me.animeheaven.android.ui.components.AhImageLoadTracker
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

        // Wait for actual Home feed content & decoded Hero/Top 10 cover images before snapping
        waitForText("Popular Today", timeoutMs = 20_000L)
        waitForText("Subbed Anime Schedule", timeoutMs = 20_000L)
        waitForImages(minCount = 4, timeoutMs = 15_000L)
        settle(800L)
        saveScreenshot("01-phone-home")

        // New tab
        val beforeNew = AhImageLoadTracker.loadedCount.get()
        composeRule.onAllNodesWithText("New").onFirst().performClick()
        waitForText("Latest Releases", timeoutMs = 15_000L)
        waitForImages(minCount = beforeNew + 2, timeoutMs = 10_000L)
        settle(800L)
        saveScreenshot("02-phone-new")

        // Popular tab
        val beforePopular = AhImageLoadTracker.loadedCount.get()
        composeRule.onAllNodesWithText("Popular").onFirst().performClick()
        waitForText("Ranked by Views", timeoutMs = 15_000L)
        waitForImages(minCount = beforePopular + 2, timeoutMs = 10_000L)
        settle(800L)
        saveScreenshot("03-phone-popular")

        // Bookmarks tab
        composeRule.onAllNodesWithText("Bookmarks").onFirst().performClick()
        waitForText("My Bookmarks", timeoutMs = 10_000L)
        settle(1_000L)
        saveScreenshot("04-phone-bookmarks")

        // Back to Schedule then open Search
        composeRule.onAllNodesWithText("Schedule").onFirst().performClick()
        settle(600L)
        composeRule.onAllNodesWithText("Search").onFirst().performClick()
        composeRule.waitUntil(timeoutMillis = 10_000L) {
            composeRule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        val beforeSearch = AhImageLoadTracker.loadedCount.get()
        composeRule.onNode(hasSetTextAction()).performTextInput("frieren")
        runCatching {
            composeRule.waitUntil(timeoutMillis = 15_000L) {
                composeRule.onAllNodesWithText("Frieren", substring = true, ignoreCase = true)
                    .fetchSemanticsNodes()
                    .size >= 2
            }
        }
        waitForImages(minCount = beforeSearch + 1, timeoutMs = 8_000L)
        dismissKeyboard()
        saveScreenshot("05-phone-search")

        // Return from Search and open Settings
        composeRule.onNodeWithContentDescription("Back").performClick()
        waitForText("Schedule", timeoutMs = 10_000L)
        composeRule.onNodeWithContentDescription("Settings & Themes").performClick()
        waitForText("Settings & Themes", timeoutMs = 10_000L)
        composeRule.onNodeWithText("Settings & Themes").assertIsDisplayed()
        settle(1_000L)
        saveScreenshot("06-phone-settings-dark")

        // Switch theme to Midnight Indigo
        composeRule.onNodeWithText("Midnight Indigo").performClick()
        settle(2_200L)
        saveScreenshot("07-phone-settings-indigo")

        // Return to Home in Midnight Indigo
        composeRule.onNodeWithContentDescription("Back").performClick()
        waitForText("Schedule", timeoutMs = 10_000L)
        waitForText("Popular Today", timeoutMs = 10_000L)
        settle(1_200L)
        saveScreenshot("08-phone-home-indigo")

        // Open Settings and switch to Classic Light for light-theme contrast verification
        composeRule.onNodeWithContentDescription("Settings & Themes").performClick()
        waitForText("Classic Light", timeoutMs = 10_000L)
        composeRule.onNodeWithText("Classic Light").performClick()
        settle(2_200L)
        saveScreenshot("09-phone-settings-light")

        // Return to Home and open Hero Detail screen
        composeRule.onNodeWithContentDescription("Back").performClick()
        waitForText("Schedule", timeoutMs = 10_000L)
        waitForText("Popular Today", timeoutMs = 10_000L)
        settle(600L)
        val beforeDetail = AhImageLoadTracker.loadedCount.get()
        if (composeRule.onAllNodesWithTag("hero_details_btn").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onAllNodesWithTag("hero_details_btn").onFirst().performClick()
        } else {
            composeRule.onAllNodesWithText("Details").onFirst().performClick()
        }
        composeRule.waitUntil(timeoutMillis = 20_000L) {
            composeRule.onAllNodesWithText("Episodes", substring = true).fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText("Add to My List").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText("In My List").fetchSemanticsNodes().isNotEmpty()
        }
        waitForImages(minCount = beforeDetail + 1, timeoutMs = 10_000L)
        settle(800L)
        saveScreenshot("10-phone-detail")
    }

    private fun waitForText(text: String, timeoutMs: Long) {
        composeRule.waitUntil(timeoutMillis = timeoutMs) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForImages(minCount: Int, timeoutMs: Long) {
        runCatching {
            composeRule.waitUntil(timeoutMillis = timeoutMs) {
                AhImageLoadTracker.loadedCount.get() >= minCount
            }
        }
    }

    private fun dismissKeyboard() {
        composeRule.activityRule.scenario.onActivity { activity ->
            val imm = activity.getSystemService(InputMethodManager::class.java)
            val token = activity.currentFocus?.windowToken ?: activity.window.decorView.windowToken
            activity.currentFocus?.clearFocus()
            imm?.hideSoftInputFromWindow(token, 0)
        }
        settle(700L)
    }

    private fun settle(ms: Long) {
        composeRule.waitForIdle()
        Thread.sleep(ms)
        composeRule.waitForIdle()
    }

    private fun runShell(cmd: String): String {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        return ParcelFileDescriptor.AutoCloseInputStream(
            uiAutomation.executeShellCommand(cmd)
        ).use { it.readBytes().decodeToString() }
    }

    private fun saveScreenshot(name: String) {
        val dir = "/data/local/tmp/ci-screenshots"
        val path = "$dir/$name.png"
        runShell("mkdir -p $dir")
        val capOut = runShell("screencap -p $path")
        var lsOut = runShell("ls -l $path")
        if (!lsOut.contains("$name.png")) {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val bitmap = instrumentation.uiAutomation.takeScreenshot()
            if (bitmap != null) {
                val baseDir = instrumentation.targetContext.externalCacheDir
                    ?: instrumentation.targetContext.cacheDir
                val tmp = File(baseDir, "$name.png")
                FileOutputStream(tmp).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                bitmap.recycle()
                runShell("cp ${tmp.absolutePath} $path")
                lsOut = runShell("ls -l $path")
            }
        }
        assertTrue("Screenshot failed for $name (capOut='$capOut', lsOut='$lsOut')", lsOut.contains("$name.png"))
    }
}
