package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.example.game.constants.GameConstants
import com.example.ui.screens.GameBoardScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.min

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Royal Marbles", appName)
  }

  @Test
  fun `keyed level transition from level 1 to level 2 renders fresh playable board`() {
    var activeLevel by mutableIntStateOf(1)
    var returnedHome = false

    composeTestRule.mainClock.autoAdvance = false
    composeTestRule.setContent {
      MyApplicationTheme {
        key(activeLevel) {
          GameBoardScreen(
            levelNumber = activeLevel,
            coins = 1000,
            onReturnHome = { returnedHome = true },
            onAddCoins = {},
            onLevelAdvanced = { nextLvl -> activeLevel = nextLvl }
          )
        }
      }
    }

    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.onNodeWithTag("pause_button").assertIsDisplayed()

    // Simulate advancing from Level 1 -> Level 2 via keyed state update
    composeTestRule.runOnUiThread {
      activeLevel = 2
    }
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.mainClock.advanceTimeByFrame()

    // Pause button must remain interactive and Level 2 board must respond to exposed block taps
    composeTestRule.onNodeWithTag("pause_button").assertIsDisplayed()

    val rootBounds = composeTestRule.onRoot().fetchSemanticsNode().boundsInRoot
    val scale = min(
      rootBounds.width / GameConstants.BASE_WIDTH,
      rootBounds.height / GameConstants.BASE_HEIGHT
    )
    val offsetX = (rootBounds.width - GameConstants.BASE_WIDTH * scale) / 2f
    val offsetY = (rootBounds.height - GameConstants.BASE_HEIGHT * scale) / 2f
    val tapX = offsetX + GameConstants.GRID_X_CENTERS[2] * scale
    val tapY = offsetY + (GameConstants.GRID_Y_CENTERS[5] + GameConstants.ARENA_SHIFT_Y) * scale

    composeTestRule.onRoot().performTouchInput {
      click(Offset(tapX, tapY))
    }
    composeTestRule.mainClock.advanceTimeByFrame()

    // Pause dialog still opens and resumes cleanly on Level 2
    composeTestRule.onNodeWithTag("pause_button").performClick()
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.onNodeWithContentDescription("Resume").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Resume").performClick()
    composeTestRule.mainClock.advanceTimeByFrame()
    assertEquals(false, returnedHome)
  }
}
