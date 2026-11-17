package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.game.assets.AssetLoader
import com.example.game.jackpot.JackpotEventManager
import com.example.game.jackpot.JackpotEventSnapshot
import com.example.game.lives.LifeSnapshot
import com.example.game.lives.LivesManager
import com.example.game.sound.SoundManager
import com.example.game.story.StoryCatalog
import com.example.game.story.StoryEpisode
import com.example.ui.screens.BucketColorTestScreen
import com.example.ui.screens.GameBoardScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.StoryCutsceneScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            MyApplicationTheme {
                RoyalMarblesApp()
            }
        }
    }
}

@Composable
fun RoyalMarblesApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("royal_marbles_prefs", Context.MODE_PRIVATE) }

    var showSplash by remember { mutableStateOf(true) }
    var currentView by remember { mutableStateOf("home") } // "home", "story", or "playing"
    var activeStory by remember { mutableStateOf<StoryEpisode?>(null) }
    var storyReturnToHome by remember { mutableStateOf(false) }

    var coins by remember {
        mutableIntStateOf(prefs.getInt("coins", 19330))
    }

    var jackpotSnapshot by remember {
        mutableStateOf(JackpotEventManager.load(prefs))
    }

    var lifeSnapshot by remember {
        mutableStateOf(LivesManager.load(prefs))
    }
    var activeLevelWon by remember { mutableStateOf(false) }

    // Start at level 1 on refresh / launch as requested
    var currentLevel by remember {
        mutableIntStateOf(1)
    }

    var playerName by remember {
        mutableStateOf(prefs.getString("player_name", "Player 1") ?: "Player 1")
    }

    var selectedAvatarId by remember {
        mutableStateOf(prefs.getString("avatar_id", "dragon") ?: "dragon")
    }

    var soundEnabled by remember {
        mutableStateOf(prefs.getBoolean("sound_enabled", true))
    }

    var musicEnabled by remember {
        mutableStateOf(prefs.getBoolean("music_enabled", true))
    }

    var vibrationEnabled by remember {
        mutableStateOf(prefs.getBoolean("vibration_enabled", true))
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    fun refreshLives(nowMs: Long = System.currentTimeMillis()): LifeSnapshot {
        val updated = LivesManager.reconcileLives(
            lives = lifeSnapshot.lives,
            nextRefillAtMs = lifeSnapshot.nextRefillAtMs,
            nowMs = nowMs
        )
        if (updated != lifeSnapshot) {
            lifeSnapshot = updated
            LivesManager.save(prefs, updated)
        }
        return updated
    }

    fun loseOneLife() {
        val updated = LivesManager.consumeOneLife(
            lives = lifeSnapshot.lives,
            nextRefillAtMs = lifeSnapshot.nextRefillAtMs,
            nowMs = System.currentTimeMillis()
        )
        lifeSnapshot = updated
        LivesManager.save(prefs, updated)
    }

    fun gainOneAdLife() {
        val updated = LivesManager.addOneLife(
            lives = lifeSnapshot.lives,
            nextRefillAtMs = lifeSnapshot.nextRefillAtMs,
            nowMs = System.currentTimeMillis()
        )
        lifeSnapshot = updated
        LivesManager.save(prefs, updated)
    }

    fun gainAdLives() {
        val updated = LivesManager.addLives(
            lives = lifeSnapshot.lives,
            nextRefillAtMs = lifeSnapshot.nextRefillAtMs,
            nowMs = System.currentTimeMillis(),
            count = LivesManager.AD_REWARD_LIVES
        )
        lifeSnapshot = updated
        LivesManager.save(prefs, updated)
    }

    fun refillAllLives() {
        val updated = LivesManager.refillAllLives()
        lifeSnapshot = updated
        LivesManager.save(prefs, updated)
    }

    fun saveCoins(newCoins: Int) {
        coins = newCoins
        prefs.edit().putInt("coins", newCoins).apply()
    }

    fun refreshJackpot(nowMs: Long = System.currentTimeMillis()): JackpotEventSnapshot {
        val updated = JackpotEventManager.reconcile(
            claimedSteps = jackpotSnapshot.claimedSteps,
            cycleEndAtMs = jackpotSnapshot.cycleEndAtMs,
            nowMs = nowMs
        )
        if (updated.claimedSteps != jackpotSnapshot.claimedSteps ||
            updated.cycleEndAtMs != jackpotSnapshot.cycleEndAtMs
        ) {
            JackpotEventManager.save(prefs, updated)
        }
        if (updated != jackpotSnapshot) {
            jackpotSnapshot = updated
        }
        return updated
    }

    fun claimNextJackpotTier(rewardCoins: Int) {
        val nowMs = System.currentTimeMillis()
        val updated = JackpotEventManager.claimNextStep(
            claimedSteps = jackpotSnapshot.claimedSteps,
            cycleEndAtMs = jackpotSnapshot.cycleEndAtMs,
            nowMs = nowMs
        )
        jackpotSnapshot = updated
        JackpotEventManager.save(prefs, updated)
        saveCoins(coins + rewardCoins)
    }

    // 1-second live 24-hour Jackpot timer ticker & automatic reset after 24 hours
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            refreshJackpot()
        }
    }

    // 1-second live countdown ticker while lives < 5
    LaunchedEffect(lifeSnapshot.isFull, lifeSnapshot.nextRefillAtMs) {
        if (!lifeSnapshot.isFull) {
            while (true) {
                delay(1000L)
                val updated = LivesManager.reconcileLives(
                    lives = lifeSnapshot.lives,
                    nextRefillAtMs = lifeSnapshot.nextRefillAtMs,
                    nowMs = System.currentTimeMillis()
                )
                lifeSnapshot = updated
                if (updated.isFull) {
                    LivesManager.save(prefs, updated)
                    break
                }
            }
        }
    }

    // Sync sound & music settings
    LaunchedEffect(soundEnabled) {
        SoundManager.init(context)
        SoundManager.enabled = soundEnabled
    }

    LaunchedEffect(musicEnabled) {
        SoundManager.setMusicEnabled(context, musicEnabled)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> SoundManager.onAppPaused()
                Lifecycle.Event.ON_RESUME -> {
                    SoundManager.onAppResumed(context)
                    refreshLives()
                    refreshJackpot()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Preload critical game assets
    LaunchedEffect(Unit) {
        SoundManager.init(context)
        AssetLoader.preloadBitmaps(context)
    }

    fun saveLevel(newLevel: Int) {
        currentLevel = newLevel
        prefs.edit().putInt("current_level", newLevel).apply()
    }

    fun saveProfile(newName: String, newAvatarId: String) {
        playerName = newName
        selectedAvatarId = newAvatarId
        prefs.edit()
            .putString("player_name", newName)
            .putString("avatar_id", newAvatarId)
            .apply()
    }

    fun startPlayingWithOptionalStory(targetLevel: Int) {
        val latestLives = refreshLives()
        if (latestLives.lives <= 0) return
        activeLevelWon = false
        val story = StoryCatalog.storyForStartingLevel(targetLevel)
        if (story != null) {
            activeStory = story
            storyReturnToHome = false
            currentView = "story"
        } else {
            currentView = "playing"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF002766))
    ) {
        AnimatedContent(
            targetState = currentView,
            transitionSpec = {
                fadeIn(animationSpec = tween(durationMillis = 170, delayMillis = 45)) togetherWith
                    fadeOut(animationSpec = tween(durationMillis = 160))
            },
            label = "root_screen_transition"
        ) { view ->
            when (view) {
                "story" -> {
                    val episode = activeStory ?: StoryCatalog.chapters.first()
                    StoryCutsceneScreen(
                        episode = episode,
                        levelToStart = currentLevel,
                        onComplete = {
                            if (!storyReturnToHome) {
                                activeLevelWon = false
                            }
                            currentView = if (storyReturnToHome) "home" else "playing"
                        }
                    )
                }

                "playing" -> {
                    key(currentLevel) {
                        GameBoardScreen(
                            levelNumber = currentLevel,
                            coins = coins,
                            lifeSnapshot = lifeSnapshot,
                            onReturnHome = { currentView = "home" },
                            onLoseLife = { loseOneLife() },
                            onGainOneLife = { gainOneAdLife() },
                            onWatchAdForLife = { gainAdLives() },
                            onRefillLivesWithCoins = {
                                if (coins >= LivesManager.REFILL_COIN_COST) {
                                    saveCoins(coins - LivesManager.REFILL_COIN_COST)
                                    refillAllLives()
                                }
                            },
                            onLevelWon = { activeLevelWon = true },
                            onAddCoins = { amount -> saveCoins(coins + amount) },
                            onLevelAdvanced = { nextLvl ->
                                activeLevelWon = false
                                if (nextLvl > 100) {
                                    saveLevel(1)
                                    activeStory = StoryCatalog.grandFinale
                                    storyReturnToHome = true
                                    currentView = "story"
                                } else {
                                    saveLevel(nextLvl)
                                    val milestoneStory = StoryCatalog.storyForStartingLevel(nextLvl)
                                    if (milestoneStory != null) {
                                        activeStory = milestoneStory
                                        storyReturnToHome = false
                                        currentView = "story"
                                    }
                                }
                            }
                        )
                    }
                }

                "color_test" -> {
                    BucketColorTestScreen(
                        onBack = { currentView = "home" }
                    )
                }

                else -> {
                    HomeScreen(
                        levelNumber = currentLevel,
                        coins = coins,
                        lifeSnapshot = lifeSnapshot,
                        playerName = playerName,
                        selectedAvatarId = selectedAvatarId,
                        soundEnabled = soundEnabled,
                        musicEnabled = musicEnabled,
                        vibrationEnabled = vibrationEnabled,
                        onStartGame = { startPlayingWithOptionalStory(currentLevel) },
                        onOpenColorTest = { currentView = "color_test" },
                        jackpotSnapshot = jackpotSnapshot,
                        jackpotClaimedSteps = jackpotSnapshot.claimedSteps,
                        onClaimJackpotReward = { rewardCoins ->
                            claimNextJackpotTier(rewardCoins)
                        },
                        onAddCoins = { amount -> saveCoins(coins + amount) },
                        onWatchAdForLife = { gainAdLives() },
                        onRefillLivesWithCoins = {
                            if (coins >= LivesManager.REFILL_COIN_COST) {
                                saveCoins(coins - LivesManager.REFILL_COIN_COST)
                                refillAllLives()
                            }
                        },
                        onUpdateProfile = { newName, newAvatarId -> saveProfile(newName, newAvatarId) },
                        onSetLevel = { newLevel -> saveLevel(newLevel) },
                        onToggleSound = {
                            val next = !soundEnabled
                            soundEnabled = next
                            prefs.edit().putBoolean("sound_enabled", next).apply()
                        },
                        onToggleMusic = {
                            val next = !musicEnabled
                            musicEnabled = next
                            prefs.edit().putBoolean("music_enabled", next).apply()
                        },
                        onToggleVibration = {
                            val next = !vibrationEnabled
                            vibrationEnabled = next
                            prefs.edit().putBoolean("vibration_enabled", next).apply()
                        }
                    )
                }
            }
        }

        if (showSplash) {
            SplashScreen(
                onComplete = { showSplash = false }
            )
        }
    }
}
