package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.game.assets.AssetLoader
import com.example.game.sound.SoundManager
import com.example.ui.screens.GameBoardScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme

import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
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
    var currentView by remember { mutableStateOf("home") } // "home" or "playing"

    var coins by remember {
        mutableIntStateOf(prefs.getInt("coins", 19330))
    }

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
                Lifecycle.Event.ON_RESUME -> SoundManager.onAppResumed(context)
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

    fun saveCoins(newCoins: Int) {
        coins = newCoins
        prefs.edit().putInt("coins", newCoins).apply()
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF002766))
    ) {
        if (currentView == "playing") {
            BackHandler {
                currentView = "home"
            }

            key(currentLevel) {
                GameBoardScreen(
                    levelNumber = currentLevel,
                    coins = coins,
                    onReturnHome = { currentView = "home" },
                    onAddCoins = { amount -> saveCoins(coins + amount) },
                    onLevelAdvanced = { nextLvl -> saveLevel(nextLvl) }
                )
            }
        } else {
            HomeScreen(
                levelNumber = currentLevel,
                coins = coins,
                playerName = playerName,
                selectedAvatarId = selectedAvatarId,
                soundEnabled = soundEnabled,
                musicEnabled = musicEnabled,
                vibrationEnabled = vibrationEnabled,
                onStartGame = { currentView = "playing" },
                onAddCoins = { amount -> saveCoins(coins + amount) },
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

        if (showSplash) {
            SplashScreen(
                onComplete = { showSplash = false }
            )
        }
    }
}
