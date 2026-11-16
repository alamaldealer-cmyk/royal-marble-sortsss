package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.example.game.belt.BeltPath
import com.example.game.constants.GameConstants
import com.example.game.lives.LifeSnapshot
import com.example.game.lives.LivesManager
import com.example.game.model.BeltBall
import com.example.game.model.CellKind
import com.example.game.model.FunnelBall
import com.example.game.model.GridCell
import com.example.game.model.MarbleColor
import com.example.game.rules.GameRules
import com.example.ui.components.ChainFullCleanUpOverlay
import com.example.ui.screens.GameBoardScreen
import com.example.ui.screens.HomeScreen
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

    composeTestRule.runOnUiThread {
      activeLevel = 2
    }
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.mainClock.advanceTimeByFrame()

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

    composeTestRule.onNodeWithTag("pause_button").performClick()
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.onNodeWithContentDescription("Resume").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Resume").performClick()
    composeTestRule.mainClock.advanceTimeByFrame()
    assertEquals(false, returnedHome)
  }

  @Test
  fun `splashScreen renders animated logo and smooth loading bar`() {
    var completed = false

    composeTestRule.mainClock.autoAdvance = false
    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.screens.SplashScreen(
          onComplete = { completed = true }
        )
      }
    }

    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.onNodeWithTag("splash_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("splash_animated_logo").assertIsDisplayed()
    composeTestRule.onNodeWithTag("splash_loading_bar").assertIsDisplayed()
    assertEquals(false, completed)
  }

  @Test
  fun `storyCutsceneScreen final beat renders green play pill matching home screen`() {
    var started = false
    val episode = com.example.game.story.StoryCatalog.chapters.first()

    composeTestRule.mainClock.autoAdvance = false
    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.screens.StoryCutsceneScreen(
          episode = episode,
          levelToStart = 1,
          onComplete = { started = true }
        )
      }
    }

    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.mainClock.advanceTimeByFrame()

    repeat(episode.beats.size - 1) {
      composeTestRule.onNodeWithTag("story_next_button").assertIsDisplayed()
      composeTestRule.onNodeWithTag("story_next_button").performClick()
      composeTestRule.mainClock.advanceTimeByFrame()
      composeTestRule.mainClock.advanceTimeByFrame()
    }

    composeTestRule.onNodeWithTag("story_start_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("story_start_button").performClick()
    composeTestRule.mainClock.advanceTimeByFrame()
    assertEquals(true, started)
  }

  @Test
  fun `bucketColorTestScreen renders dark color bucket and interactive bouncy controls`() {
    var backedOut = false

    composeTestRule.mainClock.autoAdvance = false
    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.screens.BucketColorTestScreen(
          onBack = { backedOut = true }
        )
      }
    }

    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.onNodeWithTag("test_step_drop").assertIsDisplayed()
    composeTestRule.onNodeWithTag("test_step_drop").performClick()
    composeTestRule.mainClock.advanceTimeByFrame()

    composeTestRule.onNodeWithTag("color_next_button").performClick()
    composeTestRule.mainClock.advanceTimeByFrame()

    composeTestRule.onNodeWithTag("color_test_back").performClick()
    composeTestRule.mainClock.advanceTimeByFrame()
    assertEquals(true, backedOut)
  }

  @Test
  fun `chainFullCleanUpOverlay renders 3 buttons and handles coins and quit actions`() {
    var watchAdTriggered = false
    var coinsTriggered = false
    var quitTriggered = false
    var currentCoins by mutableIntStateOf(500)

    val belt = (0 until 24).map { idx ->
      BeltBall(
        id = idx + 1,
        color = MarbleColor.ORANGE,
        slotIndex = idx,
        s = idx * (BeltPath.TOTAL_PATH_LEN / 24f)
      )
    }
    val highlighted = GameRules.selectCleanUpBeltBalls(belt, 8)
    val grid = Array(GameConstants.GRID_ROWS) { r ->
      Array(GameConstants.GRID_COLS) { c ->
        if (r == 0 && c == 0) {
          GridCell(kind = CellKind.BLOCK, color = MarbleColor.CYAN, ballCount = 9)
        } else {
          GridCell(kind = CellKind.EMPTY)
        }
      }
    }
    val funnelBalls = listOf(
      FunnelBall(101, MarbleColor.BLUE, 130f, 412f, 0f, 0f),
      FunnelBall(102, MarbleColor.ORANGE, 155f, 420f, 0f, 0f)
    )

    composeTestRule.mainClock.autoAdvance = false
    composeTestRule.setContent {
      MyApplicationTheme {
        ChainFullCleanUpOverlay(
          coins = currentCoins,
          beltCapacity = 24,
          highlightedBeltBalls = highlighted,
          viewScale = 1.2f,
          viewOffsetX = 0f,
          viewArenaOffsetY = -42f,
          grid = grid,
          funnelBalls = funnelBalls,
          beltBalls = belt,
          onWatchAdCleanUp = { watchAdTriggered = true },
          onCoinsCleanUp = { coinsTriggered = true },
          onQuit = { quitTriggered = true }
        )
      }
    }

    composeTestRule.mainClock.advanceTimeBy(450L)
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.onNodeWithTag("chain_full_cleanup_overlay").assertIsDisplayed()
    composeTestRule.onNodeWithTag("cleanup_quit_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("cleanup_watch_ad_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("cleanup_coins_button").assertIsDisplayed()

    // Insufficient coins (< 900) should NOT trigger coins clean up
    composeTestRule.onNodeWithTag("cleanup_coins_button").performClick()
    composeTestRule.mainClock.advanceTimeByFrame()
    assertEquals(false, coinsTriggered)

    // With >= 900 coins, tapping Clean Up 900 triggers onCoinsCleanUp
    composeTestRule.runOnUiThread { currentCoins = 1000 }
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.onNodeWithTag("cleanup_coins_button").performClick()
    composeTestRule.mainClock.advanceTimeByFrame()
    assertEquals(true, coinsTriggered)

    // Tapping Quit opens Out of Space! popup instead of immediately quitting
    composeTestRule.onNodeWithTag("cleanup_quit_button").performClick()
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.mainClock.advanceTimeBy(350L)
    composeTestRule.mainClock.advanceTimeByFrame()
    assertEquals(false, quitTriggered)
    composeTestRule.onNodeWithTag("out_of_space_modal").assertIsDisplayed()
    composeTestRule.onNodeWithTag("out_of_space_preview").assertIsDisplayed()
    composeTestRule.onNodeWithTag("out_of_space_watch_ad_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("out_of_space_coins_button").assertIsDisplayed()

    // Tapping X on Out of Space! popup triggers onQuit (back to Home screen)
    composeTestRule.onNodeWithTag("out_of_space_close_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("out_of_space_close_button").performClick()
    composeTestRule.mainClock.advanceTimeByFrame()
    assertEquals(true, quitTriggered)
    assertEquals(false, watchAdTriggered)
  }

  @Test
  fun `homeScreen hides player name near logo and displays new lives and coins HUD`() {
    var currentLives by mutableIntStateOf(5)
    var remainingMs by androidx.compose.runtime.mutableLongStateOf(0L)
    var gameStarted = false

    composeTestRule.mainClock.autoAdvance = false
    composeTestRule.setContent {
      MyApplicationTheme {
        HomeScreen(
          levelNumber = 1,
          coins = 1820,
          lifeSnapshot = LifeSnapshot(
            lives = currentLives,
            nextRefillAtMs = if (currentLives < 5) 100_000L else 0L,
            remainingMs = remainingMs
          ),
          playerName = "Player 1",
          soundEnabled = true,
          musicEnabled = false,
          vibrationEnabled = true,
          onStartGame = { gameStarted = true },
          onAddCoins = {},
          onSetLevel = {},
          onToggleSound = {},
          onToggleMusic = {},
          onToggleVibration = {}
        )
      }
    }

    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.mainClock.advanceTimeByFrame()

    // Profile avatar icon is present on top-left, but text name pill is removed
    composeTestRule.onNodeWithTag("profile_button").assertIsDisplayed()
    composeTestRule.onNodeWithText("Player 1").assertDoesNotExist()

    // New Lives (5 + Full) and Coins (1820 + green plus) HUD must appear
    composeTestRule.onNodeWithTag("lives_hud_pill").assertIsDisplayed()
    composeTestRule.onNodeWithText("5").assertIsDisplayed()
    composeTestRule.onNodeWithText("Full").assertIsDisplayed()
    composeTestRule.onNodeWithTag("coins_hud_pill").assertIsDisplayed()
    composeTestRule.onNodeWithText("1820").assertIsDisplayed()
    composeTestRule.onNodeWithTag("coins_hud_plus_button", useUnmergedTree = true).assertIsDisplayed()

    // Dropping life 5 -> 4 swaps "5" for "4" and "Full" for timer ("19:59")
    composeTestRule.runOnUiThread {
      currentLives = 4
      remainingMs = 19L * 60_000L + 59_000L
    }
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.onNodeWithText("Full").assertDoesNotExist()
    composeTestRule.onNodeWithText("4").assertIsDisplayed()
    composeTestRule.onNodeWithText("19:59").assertIsDisplayed()

    // Dropping lives to 0 gates play_button behind Out of Lives modal
    composeTestRule.runOnUiThread {
      currentLives = 0
      remainingMs = LivesManager.REFILL_DURATION_MS
    }
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.onNodeWithTag("play_button").performClick()
    composeTestRule.mainClock.advanceTimeBy(300L)
    composeTestRule.mainClock.advanceTimeByFrame()
    assertEquals(false, gameStarted)
    composeTestRule.onNodeWithTag("out_of_lives_modal").assertIsDisplayed()
  }

  @Test
  fun `gameBoardScreen deducts a life when returning home via pause menu`() {
    var returnedHome = false
    var lifeLost = false

    composeTestRule.mainClock.autoAdvance = false
    composeTestRule.setContent {
      MyApplicationTheme {
        GameBoardScreen(
          levelNumber = 1,
          coins = 1820,
          lifeSnapshot = LifeSnapshot(LivesManager.MAX_LIVES, 0L, 0L),
          onReturnHome = { returnedHome = true },
          onLoseLife = { lifeLost = true },
          onAddCoins = {},
          onLevelAdvanced = {}
        )
      }
    }

    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.onNodeWithTag("pause_button").performClick()
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.onNodeWithContentDescription("Home").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Home").performClick()
    composeTestRule.mainClock.advanceTimeByFrame()

    assertEquals(true, lifeLost)
    assertEquals(true, returnedHome)
  }
}
