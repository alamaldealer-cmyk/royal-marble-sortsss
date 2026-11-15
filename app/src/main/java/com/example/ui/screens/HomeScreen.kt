package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.assets.AssetLoader
import com.example.game.sound.SoundManager
import com.example.ui.components.AnimatedGameLogo
import com.example.ui.components.BottomNav
import com.example.ui.components.NavTab
import com.example.ui.components.UniversalCoinRack
import com.example.ui.theme.FredokaFontFamily

data class AvatarOption(val id: String, val name: String, val assetPath: String)

val AVATAR_OPTIONS = listOf(
    AvatarOption("dragon", "Dragon", "profile/avatar_dragon.png"),
    AvatarOption("lion", "Lion", "profile/avatar_lion.png"),
    AvatarOption("cat", "Cat", "profile/avatar_cat.png"),
    AvatarOption("panda", "Panda", "profile/avatar_panda.png"),
    AvatarOption("fox", "Fox", "profile/avatar_fox.png"),
    AvatarOption("penguin", "Penguin", "profile/avatar_penguin.png"),
    AvatarOption("husky", "Husky", "profile/avatar_husky.png"),
    AvatarOption("unicorn", "Unicorn", "profile/avatar_unicorn.png")
)

data class ShopItem(val displayAmount: String, val price: String, val numCoins: Int)

val SHOP_ITEMS = listOf(
    ShopItem("1.000", "PKR 550.00", 1000),
    ShopItem("5.000", "PKR 2200.00", 5000),
    ShopItem("10.000", "PKR 4150.00", 10000),
    ShopItem("25.000", "PKR 8400.00", 25000),
    ShopItem("50.000", "PKR 15300.00", 50000),
    ShopItem("100.000", "PKR 27800.00", 100000)
)

@Composable
fun HomeScreen(
    levelNumber: Int,
    coins: Int,
    playerName: String,
    selectedAvatarId: String,
    soundEnabled: Boolean,
    musicEnabled: Boolean,
    vibrationEnabled: Boolean,
    onStartGame: () -> Unit,
    onOpenColorTest: () -> Unit = {},
    onAddCoins: (Int) -> Unit,
    onUpdateProfile: (String, String) -> Unit,
    onSetLevel: (Int) -> Unit,
    onToggleSound: () -> Unit,
    onToggleMusic: () -> Unit,
    onToggleVibration: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(NavTab.HOME) }
    var isProfileModalOpen by remember { mutableStateOf(false) }
    var activeInfoModal by remember { mutableStateOf<String?>(null) }

    // Bitmaps
    val tropicalBg = remember { AssetLoader.loadBitmap(context, "tropical_beach_resort_entrance.png") }
    val coinRackBitmap = remember { AssetLoader.loadBitmap(context, "coin_rack.png") }
    val btnLevelGreen = remember { AssetLoader.loadBitmap(context, "profile/btn_level_green.png") }
    val goldCoinBitmap = remember { AssetLoader.loadBitmap(context, "coins/coin_01.png") }

    val currentAvatar = AVATAR_OPTIONS.firstOrNull { it.id == selectedAvatarId } ?: AVATAR_OPTIONS[0]
    val currentAvatarBitmap = remember(selectedAvatarId) {
        AssetLoader.loadBitmap(context, currentAvatar.assetPath)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Background
        if (activeTab == NavTab.HOME && tropicalBg != null) {
            Image(
                bitmap = tropicalBg.asImageBitmap(),
                contentDescription = "Tropical Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Dark royal blue background with radial pattern for Shop and Settings
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF0055B8),
                                Color(0xFF002D78),
                                Color(0xFF001A4D)
                            )
                        )
                    )
            )
        }

        // 2. Main layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 110.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Header Bar (matches original game: pt-3 / top 8px, px-4 / right 10px)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (activeTab == NavTab.HOME) {
                    // Profile button: Avatar + Name Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .testTag("profile_button")
                            .clickable {
                                SoundManager.playTap()
                                isProfileModalOpen = true
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .shadow(8.dp, RoundedCornerShape(14.dp))
                                .border(3.dp, Color(0xFFFFD700), RoundedCornerShape(14.dp))
                                .clip(RoundedCornerShape(14.dp))
                        ) {
                            if (currentAvatarBitmap != null) {
                                Image(
                                    bitmap = currentAvatarBitmap.asImageBitmap(),
                                    contentDescription = playerName,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .background(Color(0xD9002766), RoundedCornerShape(22.dp))
                                .border(2.5.dp, Color(0xFFFFD700), RoundedCornerShape(22.dp))
                                .padding(horizontal = 16.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = playerName,
                                fontFamily = FredokaFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    // Left spacer on shop/settings tabs to keep title centered
                    Spacer(modifier = Modifier.width(95.dp))
                }

                // Center Title
                if (activeTab == NavTab.SHOP) {
                    Text(
                        text = "SHOP",
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 30.sp,
                        color = Color(0xFFFFE600),
                        letterSpacing = 2.sp
                    )
                } else if (activeTab == NavTab.SETTINGS) {
                    Text(
                        text = "SETTINGS",
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 30.sp,
                        color = Color(0xFFFFE600),
                        letterSpacing = 2.sp
                    )
                }

                // Top-Right Coin rack on ALL tabs (Home, Shop, Settings)
                UniversalCoinRack(coinRackBitmap, coins)
            }

            // Divider for Shop / Settings
            if (activeTab != NavTab.HOME) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFFFB800), Color(0xFFFFF5A5), Color(0xFFFFB800))
                            )
                        )
                )
            }

            // Tab Content
            when (activeTab) {
                NavTab.HOME -> {
                    // Home Content: Animated Floofies Logo + Green Play Button
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp)
                            .padding(top = 6.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            AnimatedGameLogo(
                                modifier = Modifier
                                    .fillMaxWidth(0.95f)
                                    .widthIn(max = 360.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .widthIn(max = 340.dp)
                                .height(82.dp)
                                .testTag("play_button")
                                .clickable {
                                    SoundManager.playTap()
                                    onStartGame()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (btnLevelGreen != null) {
                                Image(
                                    bitmap = btnLevelGreen.asImageBitmap(),
                                    contentDescription = "Play Level $levelNumber",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "LEVEL $levelNumber",
                                    fontFamily = FredokaFontFamily,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 28.sp,
                                    color = Color.White,
                                    letterSpacing = 2.sp
                                )
                                Text(
                                    text = "TAP TO PLAY",
                                    fontFamily = FredokaFontFamily,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = Color(0xFFFFE27D),
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.72f)
                                .widthIn(max = 300.dp)
                                .height(48.dp)
                                .shadow(6.dp, RoundedCornerShape(24.dp))
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF38BDF8),
                                            Color(0xFF0284C7),
                                            Color(0xFF0369A1)
                                        )
                                    )
                                )
                                .border(2.5.dp, Color(0xFFFFD700), RoundedCornerShape(24.dp))
                                .clickable {
                                    SoundManager.playTap()
                                    onOpenColorTest()
                                }
                                .testTag("color_test_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "TEST 16 COLORS & BUCKETS",
                                fontFamily = FredokaFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = Color.White,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }

                NavTab.SHOP -> {
                    // Shop Items List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(SHOP_ITEMS) { item ->
                            ShopItemCard(
                                item = item,
                                coinBitmap = goldCoinBitmap,
                                onBuy = {
                                    SoundManager.playPowerup()
                                    onAddCoins(item.numCoins)
                                }
                            )
                        }
                    }
                }

                NavTab.SETTINGS -> {
                    // Settings Content
                    SettingsTabContent(
                        context = context,
                        levelNumber = levelNumber,
                        musicEnabled = musicEnabled,
                        soundEnabled = soundEnabled,
                        vibrationEnabled = vibrationEnabled,
                        onSetLevel = onSetLevel,
                        onToggleMusic = onToggleMusic,
                        onToggleSound = onToggleSound,
                        onToggleVibration = onToggleVibration,
                        onOpenModal = { activeInfoModal = it }
                    )
                }
            }
        }

        // Bottom Navigation Bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            BottomNav(
                activeTab = activeTab,
                onTabChange = { activeTab = it }
            )
        }

        // Profile Dialog Modal
        if (isProfileModalOpen) {
            ProfileModal(
                context = context,
                currentName = playerName,
                currentAvatarId = selectedAvatarId,
                currentLevel = levelNumber,
                onDismiss = { isProfileModalOpen = false },
                onSave = { newName, newAvatarId, newLevel ->
                    onUpdateProfile(newName, newAvatarId)
                    onSetLevel(newLevel)
                    isProfileModalOpen = false
                }
            )
        }

        // Support / Terms / Privacy Info Dialogs
        activeInfoModal?.let { modalType ->
            InfoModal(
                type = modalType,
                onDismiss = { activeInfoModal = null }
            )
        }
    }
}

@Composable
fun CoinRackWidget(
    bitmap: Bitmap?,
    coins: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 98.dp, height = 44.dp),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Coin Rack",
                modifier = Modifier.fillMaxSize()
            )
        }
        Text(
            text = coins.toString(),
            fontFamily = FredokaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = Color.White,
            modifier = Modifier.offset(x = 14.dp, y = 2.dp)
        )
    }
}

@Composable
fun ShopItemCard(item: ShopItem, coinBitmap: Bitmap?, onBuy: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFFFF6E5))
            .border(2.5.dp, Color(0xFFDDAF66), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (coinBitmap != null) {
                Image(
                    bitmap = coinBitmap.asImageBitmap(),
                    contentDescription = "Coins",
                    modifier = Modifier.size(36.dp)
                )
            } else {
                Text(text = "🪙", fontSize = 28.sp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = item.displayAmount,
                fontFamily = FredokaFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                color = Color(0xFF5E3A10)
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF4ADE80), Color(0xFF22C55E), Color(0xFF15803D))
                    )
                )
                .border(2.dp, Color(0xFFA8F028), RoundedCornerShape(12.dp))
                .clickable { onBuy() }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = item.price,
                fontFamily = FredokaFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                color = Color.White
            )
        }
    }
}

@Composable
fun SettingsTabContent(
    context: Context,
    levelNumber: Int,
    musicEnabled: Boolean,
    soundEnabled: Boolean,
    vibrationEnabled: Boolean,
    onSetLevel: (Int) -> Unit,
    onToggleMusic: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleVibration: () -> Unit,
    onOpenModal: (String) -> Unit
) {
    val musicSquareOn = remember { AssetLoader.loadBitmap(context, "settings_buttons/btn_music_square.png") }
    val musicSquareOff = remember { AssetLoader.loadBitmap(context, "settings_buttons/btn_music_square_off.png") }
    val soundSquareOn = remember { AssetLoader.loadBitmap(context, "settings_buttons/btn_sound_square.png") }
    val soundSquareOff = remember { AssetLoader.loadBitmap(context, "settings_buttons/btn_sound_square_off.png") }
    val vibSquareOn = remember { AssetLoader.loadBitmap(context, "settings_buttons/btn_vibration_square.png") }
    val vibSquareOff = remember { AssetLoader.loadBitmap(context, "settings_buttons/btn_vibration_square_off.png") }
    val switchOn = remember { AssetLoader.loadBitmap(context, "settings_buttons/switch_on_norm.png") }
    val switchOff = remember { AssetLoader.loadBitmap(context, "settings_buttons/switch_off_norm.png") }

    val btnSupport = remember { AssetLoader.loadBitmap(context, "settings_buttons/btn_support.png") }
    val btnTerms = remember { AssetLoader.loadBitmap(context, "settings_buttons/btn_terms.png") }
    val btnPrivacy = remember { AssetLoader.loadBitmap(context, "settings_buttons/btn_privacy.png") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Level Selection Control in Settings
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xE60B5FA7))
                .border(2.5.dp, Color(0xFF00C4FF), RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "STARTING LEVEL",
                    fontFamily = FredokaFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = Color(0xFFFFE600),
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Decrease Level Button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE93D5A))
                            .border(1.5.dp, Color.White, CircleShape)
                            .clickable {
                                if (levelNumber > 1) {
                                    SoundManager.playTap()
                                    onSetLevel(levelNumber - 1)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "-",
                            fontFamily = FredokaFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Current Level Label
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF001D4A), RoundedCornerShape(12.dp))
                            .border(2.dp, Color(0xFFFFD700), RoundedCornerShape(12.dp))
                            .padding(horizontal = 18.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "LEVEL $levelNumber",
                            fontFamily = FredokaFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Increase Level Button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2EB55A))
                            .border(1.5.dp, Color.White, CircleShape)
                            .clickable {
                                if (levelNumber < 100) {
                                    SoundManager.playTap()
                                    onSetLevel(levelNumber + 1)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            fontFamily = FredokaFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Reset to Level 1 Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF004A80))
                        .border(1.dp, Color(0xFF00C4FF), RoundedCornerShape(10.dp))
                        .clickable {
                            SoundManager.playTap()
                            onSetLevel(1)
                        }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "↺ RESET TO LEVEL 1",
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Blue container for 3 audio/vibration toggles
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xE60B5FA7))
                .border(2.5.dp, Color(0xFF00C4FF), RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Music
                SettingToggleColumn(
                    iconBitmap = if (musicEnabled) musicSquareOn else musicSquareOff,
                    switchBitmap = if (musicEnabled) switchOn else switchOff,
                    onClick = onToggleMusic
                )
                // Sound
                SettingToggleColumn(
                    iconBitmap = if (soundEnabled) soundSquareOn else soundSquareOff,
                    switchBitmap = if (soundEnabled) switchOn else switchOff,
                    onClick = onToggleSound
                )
                // Vibration
                SettingToggleColumn(
                    iconBitmap = if (vibrationEnabled) vibSquareOn else vibSquareOff,
                    switchBitmap = if (vibrationEnabled) switchOn else switchOff,
                    onClick = onToggleVibration
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Support Button
        if (btnSupport != null) {
            Image(
                bitmap = btnSupport.asImageBitmap(),
                contentDescription = "Support",
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .clickable {
                        SoundManager.playTap()
                        onOpenModal("support")
                    }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Terms and Privacy Buttons
        Row(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (btnTerms != null) {
                Image(
                    bitmap = btnTerms.asImageBitmap(),
                    contentDescription = "Terms",
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            SoundManager.playTap()
                            onOpenModal("terms")
                        }
                )
            }
            if (btnPrivacy != null) {
                Image(
                    bitmap = btnPrivacy.asImageBitmap(),
                    contentDescription = "Privacy",
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            SoundManager.playTap()
                            onOpenModal("privacy")
                        }
                )
            }
        }
    }
}

@Composable
fun SettingToggleColumn(
    iconBitmap: Bitmap?,
    switchBitmap: Bitmap?,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable {
            SoundManager.playTap()
            onClick()
        }
    ) {
        if (iconBitmap != null) {
            Image(
                bitmap = iconBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(64.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        if (switchBitmap != null) {
            Image(
                bitmap = switchBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(width = 52.dp, height = 28.dp)
            )
        }
    }
}

@Composable
fun ProfileModal(
    context: Context,
    currentName: String,
    currentAvatarId: String,
    currentLevel: Int,
    onDismiss: () -> Unit,
    onSave: (String, String, Int) -> Unit
) {
    var draftName by remember { mutableStateOf(currentName) }
    var draftAvatarId by remember { mutableStateOf(currentAvatarId) }
    var draftLevel by remember { mutableIntStateOf(currentLevel) }

    val modalPanel = remember { AssetLoader.loadBitmap(context, "profile/modal_panel.png") }
    val nameInputBar = remember { AssetLoader.loadBitmap(context, "profile/name_input_bar_perfect.png") }
    val avatarTray = remember { AssetLoader.loadBitmap(context, "profile/avatar_8_slots_tray.png") }
    val btnLevelGreen = remember { AssetLoader.loadBitmap(context, "profile/btn_level_green.png") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(280.dp)
        ) {
            // Modal Panel Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(537f / 456f)
            ) {
                if (modalPanel != null) {
                    Image(
                        bitmap = modalPanel.asImageBitmap(),
                        contentDescription = "Profile Panel",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Close X Hit Area in top right
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(48.dp)
                        .clickable {
                            SoundManager.playTap()
                            onDismiss()
                        }
                )

                // Title
                Text(
                    text = "PLAYER PROFILE",
                    fontFamily = FredokaFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = Color(0xFFFFE600),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp)
                )

                // Content Column inside panel
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 36.dp, bottom = 12.dp, start = 16.dp, end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Name Input Bar
                    Box(
                        modifier = Modifier
                            .width(180.dp)
                            .height(34.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (nameInputBar != null) {
                            Image(
                                bitmap = nameInputBar.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        BasicTextField(
                            value = draftName,
                            onValueChange = { if (it.length <= 14) draftName = it },
                            textStyle = TextStyle(
                                fontFamily = FredokaFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                        )
                    }

                    // Level Selector Row in Profile
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE93D5A))
                                .border(1.dp, Color.White, CircleShape)
                                .clickable {
                                    if (draftLevel > 1) {
                                        SoundManager.playTap()
                                        draftLevel--
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "-", fontFamily = FredokaFontFamily, fontWeight = FontWeight.Black, color = Color.White, fontSize = 16.sp)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Box(
                            modifier = Modifier
                                .background(Color(0xFF001D4A), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "LEVEL $draftLevel",
                                fontFamily = FredokaFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2EB55A))
                                .border(1.dp, Color.White, CircleShape)
                                .clickable {
                                    if (draftLevel < 100) {
                                        SoundManager.playTap()
                                        draftLevel++
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "+", fontFamily = FredokaFontFamily, fontWeight = FontWeight.Black, color = Color.White, fontSize = 16.sp)
                        }
                    }

                    // 8 Avatar Slots Tray
                    Box(
                        modifier = Modifier
                            .width(200.dp)
                            .height(84.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarTray != null) {
                            Image(
                                bitmap = avatarTray.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // 2 rows of 4 avatars
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                for (i in 0..3) {
                                    val opt = AVATAR_OPTIONS[i]
                                    AvatarSlotItem(
                                        context = context,
                                        avatar = opt,
                                        isSelected = draftAvatarId == opt.id,
                                        onSelect = { draftAvatarId = opt.id }
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                for (i in 4..7) {
                                    val opt = AVATAR_OPTIONS[i]
                                    AvatarSlotItem(
                                        context = context,
                                        avatar = opt,
                                        isSelected = draftAvatarId == opt.id,
                                        onSelect = { draftAvatarId = opt.id }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button Outside / Below Panel
            Box(
                modifier = Modifier
                    .width(176.dp)
                    .height(48.dp)
                    .clickable {
                        SoundManager.playPowerup()
                        onSave(draftName.trim().ifEmpty { "Player 1" }, draftAvatarId, draftLevel)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (btnLevelGreen != null) {
                    Image(
                        bitmap = btnLevelGreen.asImageBitmap(),
                        contentDescription = "Save",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Text(
                    text = "✔ SAVE",
                    fontFamily = FredokaFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun AvatarSlotItem(
    context: Context,
    avatar: AvatarOption,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val bitmap = remember(avatar.id) {
        AssetLoader.loadBitmap(context, avatar.assetPath)
    }

    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(6.dp))
            .border(
                width = if (isSelected) 2.5.dp else 0.dp,
                color = if (isSelected) Color(0xFFFFE600) else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable {
                SoundManager.playTap()
                onSelect()
            }
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = avatar.name,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun InfoModal(type: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF002766))
                .border(3.dp, Color(0xFF00C4FF), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when (type) {
                    "support" -> {
                        Text(
                            text = "PLAYER SUPPORT",
                            fontFamily = FredokaFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = Color(0xFFFFE600)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Need help or have suggestions? Contact our support team directly.",
                            fontFamily = FredokaFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF001D4A), RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "support@royalmarblesort.com",
                                color = Color(0xFF8CE4FD),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    "terms" -> {
                        Text(
                            text = "TERMS OF SERVICE",
                            fontFamily = FredokaFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = Color(0xFFFFE600)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "By playing Floofies Marble Sort, you agree to enjoy the puzzle mechanics responsibly. All coins and boosters are in-game virtual items for entertainment purposes.",
                            fontFamily = FredokaFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center
                        )
                    }

                    "privacy" -> {
                        Text(
                            text = "PRIVACY POLICY",
                            fontFamily = FredokaFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = Color(0xFFFFE600)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "We respect your privacy. Game progress, level settings, and preferences are stored locally on your device. No personal data is shared with third parties.",
                            fontFamily = FredokaFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2EB55A))
                        .border(1.5.dp, Color.White, RoundedCornerShape(12.dp))
                        .clickable { onDismiss() }
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Close",
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}
