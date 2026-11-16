package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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

    // Home animated coin-event button is displayed below logo and above play button
    composeTestRule.onNodeWithTag("home_coin_event_button").assertIsDisplayed()
    val logoBounds = composeTestRule.onNodeWithTag("home_animated_logo").fetchSemanticsNode().boundsInRoot
    val eventBounds = composeTestRule.onNodeWithTag("home_coin_event_button").fetchSemanticsNode().boundsInRoot
    val playBounds = composeTestRule.onNodeWithTag("play_button").fetchSemanticsNode().boundsInRoot
    org.junit.Assert.assertTrue(eventBounds.left > logoBounds.center.x)
    org.junit.Assert.assertTrue(eventBounds.top >= logoBounds.bottom - 1f)
    org.junit.Assert.assertTrue(eventBounds.bottom <= playBounds.top + 1f)
    composeTestRule.onNodeWithTag("home_coin_event_button").performClick()
    composeTestRule.mainClock.advanceTimeBy(300L)
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.onNodeWithTag("jackpot_tower_modal").assertIsDisplayed()
    composeTestRule.onNodeWithTag("jackpot_coin_rack").assertIsDisplayed()
    composeTestRule.onNodeWithTag("jackpot_claim_free_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("jackpot_close_button").performClick()
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.onNodeWithTag("jackpot_tower_modal").assertDoesNotExist()

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

  @Test
  fun `jackpotTowerModal claims 50 free coins with flight animation then switches to watch ad button`() {
    var totalCoins by mutableIntStateOf(1000)
    var claimedSteps by mutableIntStateOf(0)
    var dismissed = false

    composeTestRule.mainClock.autoAdvance = false
    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.components.JackpotTowerModal(
          coins = totalCoins,
          claimedSteps = claimedSteps,
          onClaimTier = { reward ->
            totalCoins += reward
            claimedSteps++
          },
          onDismiss = { dismissed = true }
        )
      }
    }

    composeTestRule.mainClock.advanceTimeBy(300L)
    composeTestRule.mainClock.advanceTimeByFrame()

    composeTestRule.onNodeWithTag("jackpot_tower_modal").assertIsDisplayed()
    composeTestRule.onNodeWithTag("jackpot_coin_rack").assertIsDisplayed()
    composeTestRule.onNodeWithTag("jackpot_tower_sprite").assertIsDisplayed()
    composeTestRule.onNodeWithTag("jackpot_arrow_left").assertIsDisplayed()
    composeTestRule.onNodeWithTag("jackpot_arrow_right").assertIsDisplayed()
    composeTestRule.onNodeWithTag("jackpot_claim_free_button").assertIsDisplayed()

    val towerBounds = composeTestRule.onNodeWithTag("jackpot_tower_sprite").fetchSemanticsNode().boundsInRoot
    val leftArrowBounds = composeTestRule.onNodeWithTag("jackpot_arrow_left").fetchSemanticsNode().boundsInRoot
    val rightArrowBounds = composeTestRule.onNodeWithTag("jackpot_arrow_right").fetchSemanticsNode().boundsInRoot
    org.junit.Assert.assertTrue(leftArrowBounds.left >= towerBounds.left)
    org.junit.Assert.assertTrue(rightArrowBounds.right <= towerBounds.right)
    org.junit.Assert.assertTrue(leftArrowBounds.right < rightArrowBounds.left)

    // Claim initial free 50 coins
    composeTestRule.onNodeWithTag("jackpot_claim_free_button").performClick()
    // Advance through coin burst & flight animation (~1.6s total)
    repeat(40) {
      composeTestRule.mainClock.advanceTimeBy(50L)
      composeTestRule.mainClock.advanceTimeByFrame()
    }

    assertEquals(1050, totalCoins)
    assertEquals(1, claimedSteps)

    // Next tier (100) must display the Watch Ad Free Coins button
    composeTestRule.onNodeWithTag("jackpot_watch_ad_button").assertIsDisplayed()

    // Tapping Watch Ad shows rewarded ad overlay then awards 100 coins
    composeTestRule.onNodeWithTag("jackpot_watch_ad_button").performClick()
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.onNodeWithTag("jackpot_watching_ad_overlay").assertIsDisplayed()

    repeat(75) {
      composeTestRule.mainClock.advanceTimeBy(50L)
      composeTestRule.mainClock.advanceTimeByFrame()
    }

    assertEquals(1150, totalCoins)
    assertEquals(2, claimedSteps)

    // Close button dismisses modal cleanly
    composeTestRule.onNodeWithTag("jackpot_close_button").performClick()
    composeTestRule.mainClock.advanceTimeByFrame()
    assertEquals(true, dismissed)
  }

  @Test
  fun `jackpotTowerModal shows green cooldown button with matching outer timer when all 5 rewards claimed`() {
    val sharedRemainingMs = (11L * 3600L + 22L * 60L + 33L) * 1000L // 11:22:33
    val snapshot = com.example.game.jackpot.JackpotEventSnapshot(
      claimedSteps = 5,
      cycleEndAtMs = 999_999_999L,
      remainingMs = sharedRemainingMs
    )

    composeTestRule.mainClock.autoAdvance = false
    composeTestRule.setContent {
      MyApplicationTheme {
        HomeScreen(
          levelNumber = 1,
          coins = 1820,
          jackpotSnapshot = snapshot,
          jackpotClaimedSteps = snapshot.claimedSteps,
          onClaimJackpotReward = {},
          soundEnabled = true,
          musicEnabled = false,
          vibrationEnabled = true,
          onStartGame = {},
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

    // Outer HomeCoinEventButton advertises exact same 11:22:33 timer
    composeTestRule.onNodeWithContentDescription(
      "Coin Event, 0 notifications, 0 of 5, 11:22:33"
    ).assertIsDisplayed()

    // Opening Jackpot Tower shows green cooldown button displaying 11:22:33
    composeTestRule.onNodeWithTag("home_coin_event_button").performClick()
    composeTestRule.mainClock.advanceTimeBy(300L)
    composeTestRule.mainClock.advanceTimeByFrame()

    composeTestRule.onNodeWithTag("jackpot_tower_modal").assertIsDisplayed()
    composeTestRule.onNodeWithTag("jackpot_cooldown_button").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Next Jackpot in 11:22:33").assertIsDisplayed()
  }

  @Test
  fun `outOfLivesModal renders cropped coin and ad buttons and awards +3 lives on ad`() {
    var coins by mutableIntStateOf(500)
    var watchAdCount = 0
    var refillAllCount = 0

    composeTestRule.mainClock.autoAdvance = false
    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.components.OutOfLivesModal(
          lifeSnapshot = LifeSnapshot(
            lives = 1,
            nextRefillAtMs = 600_000L,
            remainingMs = 600_000L
          ),
          coins = coins,
          onWatchAdForLife = { watchAdCount++ },
          onCoinsRefillAll = { refillAllCount++ },
          onDismiss = {}
        )
      }
    }

    composeTestRule.mainClock.advanceTimeBy(300L)
    composeTestRule.mainClock.advanceTimeByFrame()

    composeTestRule.onNodeWithTag("out_of_lives_coins_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("out_of_lives_watch_ad_button").assertIsDisplayed()

    // Insufficient coins (< 900) shows +3 Lives hint and does not refill
    composeTestRule.onNodeWithTag("out_of_lives_coins_button").performClick()
    composeTestRule.mainClock.advanceTimeBy(120L)
    composeTestRule.mainClock.advanceTimeByFrame()
    assertEquals(0, refillAllCount)
    composeTestRule.onNodeWithText("Need 900 coins! Watch Ad for +3 Lives").assertIsDisplayed()

    // Sufficient coins (>= 900) triggers full refill
    composeTestRule.runOnUiThread { coins = 1200 }
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.onNodeWithTag("out_of_lives_coins_button").performClick()
    composeTestRule.mainClock.advanceTimeByFrame()
    assertEquals(1, refillAllCount)

    // Watch Ad triggers +3 Lives rewarded ad overlay and callback
    composeTestRule.onNodeWithTag("out_of_lives_watch_ad_button").performClick()
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.onNodeWithText("Getting +3 Lives...").assertIsDisplayed()

    repeat(55) {
      composeTestRule.mainClock.advanceTimeBy(50L)
      composeTestRule.mainClock.advanceTimeByFrame()
    }
    assertEquals(1, watchAdCount)
  }

  @Test
  fun `close and watch ad buttons fire on first single tap even near outer edge during press shrink`() {
    var showOutOfSpace by mutableStateOf(false)
    var livesDismissCount = 0
    var outOfSpaceWatchAdCount = 0
    var outOfSpaceCloseCount = 0

    composeTestRule.mainClock.autoAdvance = false
    composeTestRule.setContent {
      MyApplicationTheme {
        if (!showOutOfSpace) {
          com.example.ui.components.OutOfLivesModal(
            lifeSnapshot = LifeSnapshot(lives = 0, nextRefillAtMs = 600_000L, remainingMs = 600_000L),
            coins = 100,
            onWatchAdForLife = {},
            onCoinsRefillAll = {},
            onDismiss = { livesDismissCount++ }
          )
        } else {
          com.example.ui.components.OutOfSpaceModal(
            coins = 100,
            onWatchAdPlayOn = { outOfSpaceWatchAdCount++ },
            onCoinsPlayOn = {},
            onCloseToHome = { outOfSpaceCloseCount++ }
          )
        }
      }
    }

    composeTestRule.mainClock.advanceTimeBy(300L)
    composeTestRule.mainClock.advanceTimeByFrame()

    // Press near outer 4% left edge of the close X button, advance clock through shrink animation, then release
    composeTestRule.onNodeWithTag("out_of_lives_close_button").performTouchInput {
      down(Offset(width * 0.04f, centerY))
    }
    composeTestRule.mainClock.advanceTimeBy(90L)
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.onNodeWithTag("out_of_lives_close_button").performTouchInput {
      up()
    }
    composeTestRule.mainClock.advanceTimeByFrame()
    assertEquals(1, livesDismissCount)

    // Verify OutOfSpaceModal Watch Ad and Close X buttons fire on first single edge-tap as well
    composeTestRule.runOnUiThread { showOutOfSpace = true }
    composeTestRule.mainClock.advanceTimeBy(300L)
    composeTestRule.mainClock.advanceTimeByFrame()

    composeTestRule.onNodeWithTag("out_of_space_watch_ad_button").performTouchInput {
      down(Offset(width * 0.03f, centerY))
    }
    composeTestRule.mainClock.advanceTimeBy(90L)
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.onNodeWithTag("out_of_space_watch_ad_button").performTouchInput {
      up()
    }
    composeTestRule.mainClock.advanceTimeByFrame()
    assertEquals(1, outOfSpaceWatchAdCount)

    composeTestRule.onNodeWithTag("out_of_space_close_button").performTouchInput {
      down(Offset(width * 0.96f, centerY))
    }
    composeTestRule.mainClock.advanceTimeBy(90L)
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.onNodeWithTag("out_of_space_close_button").performTouchInput {
      up()
    }
    composeTestRule.mainClock.advanceTimeByFrame()
    assertEquals(1, outOfSpaceCloseCount)
  }
}
