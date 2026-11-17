package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.assets.AssetLoader
import com.example.game.lives.LifeSnapshot
import com.example.game.lives.LivesManager
import com.example.game.sound.SoundManager
import com.example.ui.theme.FredokaFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class LoseLifeWarningMode {
    OUT_OF_SPACE,
    LEAVE_LEVEL
}

@Composable
fun LoseLifeWarningModal(
    mode: LoseLifeWarningMode,
    coins: Int,
    onWatchAdPlayOn: () -> Unit = {},
    onCoinsPlayOn: () -> Unit = {},
    onKeepPlaying: () -> Unit = {},
    onConfirmLoseLife: () -> Unit
) {
    val context = LocalContext.current
    val panelBmp = remember { AssetLoader.loadBitmap(context, "cleanup/panel_out_of_space.png") }
    val closeXBmp = remember { AssetLoader.loadBitmap(context, "cleanup/btn_close_x.png") }
    val heartBmp = remember { AssetLoader.loadBitmap(context, "hud/hud_heart.png") }
    val watchAdBmp = remember { AssetLoader.loadBitmap(context, "cleanup/btn_play_on_watch_ad.png") }
    val coinsBtnBmp = remember { AssetLoader.loadBitmap(context, "cleanup/btn_play_on_coins.png") }
    val quitBtnBmp = remember { AssetLoader.loadBitmap(context, "cleanup/btn_quit.png") }

    val modalScale = remember { Animatable(0.90f) }
    val modalAlpha = remember { Animatable(0.15f) }
    var isWatchingAd by remember { mutableStateOf(false) }
    var insufficientCoinsTick by remember { mutableIntStateOf(0) }
    var showNotEnoughCoinsBanner by remember { mutableStateOf(false) }
    val coinsShakeOffset = remember { Animatable(0f) }

    BackHandler(enabled = !isWatchingAd) {
        SoundManager.playTap()
        if (mode == LoseLifeWarningMode.LEAVE_LEVEL) {
            onKeepPlaying()
        } else {
            onConfirmLoseLife()
        }
    }

    LaunchedEffect(Unit) {
        launch { modalAlpha.animateTo(1f, tween(140)) }
        launch {
            modalScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.72f,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    LaunchedEffect(insufficientCoinsTick) {
        if (insufficientCoinsTick > 0) {
            showNotEnoughCoinsBanner = true
            for (i in 0 until 6) {
                coinsShakeOffset.animateTo(
                    targetValue = if (i % 2 == 0) 8f else -8f,
                    animationSpec = tween(durationMillis = 45)
                )
            }
            coinsShakeOffset.animateTo(0f, tween(durationMillis = 50))
            delay(1600)
            showNotEnoughCoinsBanner = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("lose_life_warning_modal")
            .background(Color.Black.copy(alpha = 0.60f))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 312.dp)
                .fillMaxWidth(0.82f)
                .graphicsLayer {
                    scaleX = modalScale.value
                    scaleY = modalScale.value
                    alpha = modalAlpha.value
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.86f)
            ) {
                // Game's own Popup Panel Background
                if (panelBmp != null) {
                    Image(
                        bitmap = panelBmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF7BB5F5))
                            .border(3.dp, Color(0xFF1D4E89), RoundedCornerShape(24.dp))
                    )
                }

                // Header Row (Top ~15.5% of panel_out_of_space.png)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.155f)
                        .align(Alignment.TopCenter)
                ) {
                    ModalOutlinedText(
                        text = if (mode == LoseLifeWarningMode.OUT_OF_SPACE) "Out of Space!" else "Leave Level?",
                        fontSize = 23.sp,
                        strokeColor = Color(0xFF0C2E66),
                        strokeWidth = 7f,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 46.dp)
                    )

                    val closePress = rememberBouncyPressState(pressedScale = 0.86f)
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp)
                            .size(52.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("lose_life_warning_close_button")
                            .bouncyClickTarget(closePress, role = Role.Button) {
                                SoundManager.playTap()
                                if (mode == LoseLifeWarningMode.LEAVE_LEVEL) {
                                    onKeepPlaying()
                                } else {
                                    onConfirmLoseLife()
                                }
                            }
                            .bouncyScale(closePress),
                        contentAlignment = Alignment.Center
                    ) {
                        if (closeXBmp != null) {
                            Image(
                                bitmap = closeXBmp.asImageBitmap(),
                                contentDescription = "Close",
                                modifier = Modifier.size(38.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text(
                                text = "X",
                                fontFamily = FredokaFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                // Main Content Body (Bottom ~84.5% of panel)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.845f)
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Inner Royal Blue Card with game's Glossy Heart & "-1"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF1F579C), Color(0xFF153D73))
                                )
                            )
                            .border(2.dp, Color(0xFF4783C4), RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier.size(width = 96.dp, height = 88.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (heartBmp != null) {
                                Image(
                                    bitmap = heartBmp.asImageBitmap(),
                                    contentDescription = "Lose 1 Life",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(84.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE11D48))
                                )
                            }
                            ModalOutlinedText(
                                text = "-1",
                                fontSize = 31.sp,
                                strokeColor = Color(0xFF8C0915),
                                strokeWidth = 7.5f,
                                modifier = Modifier.offset(y = (-3).dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    ModalOutlinedText(
                        text = "You will lose a life!",
                        fontSize = 20.sp,
                        strokeColor = Color(0xFF0C2E66),
                        strokeWidth = 6f
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (mode == LoseLifeWarningMode.OUT_OF_SPACE) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val watchAdPress = rememberBouncyPressState(pressedScale = 0.90f)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(2584f / 1793f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("lose_life_watch_ad_button")
                                    .bouncyClickTarget(watchAdPress, role = Role.Button) {
                                        SoundManager.playTap()
                                        isWatchingAd = true
                                    }
                                    .bouncyScale(watchAdPress),
                                contentAlignment = Alignment.Center
                            ) {
                                if (watchAdBmp != null) {
                                    Image(
                                        bitmap = watchAdBmp.asImageBitmap(),
                                        contentDescription = "Play On Watch Ad",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.FillBounds
                                    )
                                } else {
                                    Text(
                                        text = "Play On! Watch Ad",
                                        fontFamily = FredokaFontFamily,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            val coinsPress = rememberBouncyPressState(pressedScale = 0.90f)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .offset { IntOffset(x = coinsShakeOffset.value.roundToInt(), y = 0) }
                                    .aspectRatio(2481f / 1738f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("lose_life_coins_button")
                                    .bouncyClickTarget(coinsPress, role = Role.Button) {
                                        if (coins >= CLEAN_UP_COIN_COST) {
                                            SoundManager.playCoinCollect(0f)
                                            onCoinsPlayOn()
                                        } else {
                                            SoundManager.playBlocked()
                                            insufficientCoinsTick++
                                        }
                                    }
                                    .bouncyScale(coinsPress),
                                contentAlignment = Alignment.Center
                            ) {
                                if (coinsBtnBmp != null) {
                                    Image(
                                        bitmap = coinsBtnBmp.asImageBitmap(),
                                        contentDescription = "Play On 900 Coins",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.FillBounds
                                    )
                                } else {
                                    Text(
                                        text = "Play On! 900",
                                        fontFamily = FredokaFontFamily,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        // Leave Level mid-game buttons using game's real green pill + red Quit button assets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GreenRingPillButtonContent(
                                text = "Keep Playing",
                                contentDescription = "Keep Playing",
                                testTag = "leave_level_keep_playing_button",
                                labelSize = 12.5.sp,
                                letterSpacing = 0.3.sp,
                                strokeWidth = 4.2f,
                                onClick = {
                                    SoundManager.playTap()
                                    onKeepPlaying()
                                },
                                modifier = Modifier
                                    .weight(1.15f)
                                    .height(54.dp)
                                    .minimumInteractiveComponentSize()
                            )

                            val quitPress = rememberBouncyPressState(pressedScale = 0.90f)
                            Box(
                                modifier = Modifier
                                    .weight(0.85f)
                                    .height(58.dp)
                                    .minimumInteractiveComponentSize()
                                    .testTag("leave_level_quit_button")
                                    .bouncyClickTarget(quitPress, role = Role.Button) {
                                        SoundManager.playTap()
                                        onConfirmLoseLife()
                                    }
                                    .bouncyScale(quitPress),
                                contentAlignment = Alignment.Center
                            ) {
                                if (quitBtnBmp != null) {
                                    Image(
                                        bitmap = quitBtnBmp.asImageBitmap(),
                                        contentDescription = "Quit",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0xFFE92D42))
                                            .border(2.5.dp, Color(0xFF5A0611), RoundedCornerShape(16.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        ModalOutlinedText(
                                            text = "Quit",
                                            fontSize = 18.sp,
                                            strokeColor = Color(0xFF4A040C),
                                            strokeWidth = 6f
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showNotEnoughCoinsBanner) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .shadow(8.dp, RoundedCornerShape(20.dp))
                        .background(Color(0xFFE93D5A), RoundedCornerShape(20.dp))
                        .border(2.dp, Color.White, RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Need 900 coins! Watch Ad to Play On",
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }
        }

        if (isWatchingAd) {
            SimpleRewardedAdOverlay(
                title = "REWARDED AD",
                subtitle = "Cleaning up board to Play On...",
                onFinished = {
                    isWatchingAd = false
                    onWatchAdPlayOn()
                },
                onCancel = {
                    SoundManager.playTap()
                    isWatchingAd = false
                }
            )
        }
    }
}

@Composable
fun LevelFailedModal(
    levelNumber: Int,
    beforeLives: Int,
    lifeSnapshot: LifeSnapshot,
    onWatchAdPlusOneLife: () -> Unit,
    onTryAgain: () -> Unit,
    onCloseToHome: () -> Unit
) {
    val context = LocalContext.current
    val panelBmp = remember { AssetLoader.loadBitmap(context, "cleanup/panel_out_of_space.png") }
    val closeXBmp = remember { AssetLoader.loadBitmap(context, "cleanup/btn_close_x.png") }
    val heartBmp = remember { AssetLoader.loadBitmap(context, "hud/hud_heart.png") }
    val lifeBarBmp = remember { AssetLoader.loadBitmap(context, "hud/hud_bar_life.png") }
    val plusOneAdBmp = remember { AssetLoader.loadBitmap(context, "hud/btn_plus_one_life_ad.png") }

    val modalScale = remember { Animatable(0.90f) }
    val modalAlpha = remember { Animatable(0.15f) }
    var isWatchingAd by remember { mutableStateOf(false) }

    // Animate the newly lost heart from filled -> dark empty right as this modal opens
    val lostHeartIndex = if (beforeLives > lifeSnapshot.lives) {
        (beforeLives - 1).coerceIn(0, LivesManager.MAX_LIVES - 1)
    } else {
        -1
    }
    var lostHeartVisualFilled by remember(beforeLives, lifeSnapshot.lives) {
        mutableStateOf(lostHeartIndex >= 0)
    }
    val lostHeartScale = remember { Animatable(1f) }

    BackHandler(enabled = !isWatchingAd) {
        SoundManager.playTap()
        onCloseToHome()
    }

    LaunchedEffect(Unit) {
        launch { modalAlpha.animateTo(1f, tween(140)) }
        launch {
            modalScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.72f,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    LaunchedEffect(lostHeartIndex) {
        if (lostHeartIndex >= 0) {
            lostHeartVisualFilled = true
            delay(280L)
            lostHeartScale.animateTo(1.22f, tween(140, easing = FastOutSlowInEasing))
            lostHeartVisualFilled = false
            lostHeartScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.58f,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("level_failed_modal")
            .background(Color.Black.copy(alpha = 0.62f))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 296.dp)
                .fillMaxWidth(0.78f)
                .graphicsLayer {
                    scaleX = modalScale.value
                    scaleY = modalScale.value
                    alpha = modalAlpha.value
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.84f)
            ) {
                // Game's own Popup Panel Background
                if (panelBmp != null) {
                    Image(
                        bitmap = panelBmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF7BB5F5))
                            .border(3.dp, Color(0xFF1D4E89), RoundedCornerShape(24.dp))
                    )
                }

                // Header Row ("Level X" + Red X)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.155f)
                        .align(Alignment.TopCenter)
                ) {
                    ModalOutlinedText(
                        text = "Level $levelNumber",
                        fontSize = 21.sp,
                        strokeColor = Color(0xFF0C2E66),
                        strokeWidth = 6.5f,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 44.dp)
                    )

                    val closePress = rememberBouncyPressState(pressedScale = 0.86f)
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp)
                            .size(48.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("level_failed_close_button")
                            .bouncyClickTarget(closePress, role = Role.Button) {
                                SoundManager.playTap()
                                onCloseToHome()
                            }
                            .bouncyScale(closePress),
                        contentAlignment = Alignment.Center
                    ) {
                        if (closeXBmp != null) {
                            Image(
                                bitmap = closeXBmp.asImageBitmap(),
                                contentDescription = "Close",
                                modifier = Modifier.size(34.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text(
                                text = "X",
                                fontFamily = FredokaFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                // Main Content Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.845f)
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Inner Royal Blue Card (5 Hearts using hud_heart.png + hud_bar_life.png pill + +1 Watch Ad icon)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF1F579C), Color(0xFF153D73))
                                )
                            )
                            .border(2.dp, Color(0xFF4783C4), RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Row of 5 Hearts (using hud_heart.png)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("level_failed_hearts_row"),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (idx in 0 until LivesManager.MAX_LIVES) {
                                val isFilled = when {
                                    idx == lostHeartIndex -> lostHeartVisualFilled
                                    else -> idx < lifeSnapshot.lives
                                }
                                val scaleVal = if (idx == lostHeartIndex) lostHeartScale.value else 1f
                                Box(
                                    modifier = Modifier
                                        .size(width = 39.dp, height = 35.dp)
                                        .graphicsLayer {
                                            scaleX = scaleVal
                                            scaleY = scaleVal
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (heartBmp != null) {
                                        if (isFilled) {
                                            Image(
                                                bitmap = heartBmp.asImageBitmap(),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Fit
                                            )
                                        } else {
                                            Image(
                                                bitmap = heartBmp.asImageBitmap(),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Fit,
                                                colorFilter = ColorFilter.tint(Color(0xFFDCE8FA))
                                            )
                                            Image(
                                                bitmap = heartBmp.asImageBitmap(),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(0.86f),
                                                contentScale = ContentScale.Fit,
                                                colorFilter = ColorFilter.tint(Color(0xFF1F2937))
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isFilled) Color(0xFFE11D48) else Color(0xFF1F2937)
                                                )
                                                .border(2.dp, Color(0xFFDCE8FA), CircleShape)
                                        )
                                    }
                                }
                            }
                        }

                        // Game's Life HUD Pill (hud_bar_life.png + hud_heart.png) + User's +1 Ad Icon
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val timerLabel = if (lifeSnapshot.isFull) {
                                "Full"
                            } else {
                                formatMinutesSecondsWords(lifeSnapshot.remainingMs)
                            }
                            val barStartOffset = 16.dp
                            val barWidth = 98.dp
                            val barHeight = 33.dp
                            val heartWidth = 40.dp
                            val heartHeight = 36.dp

                            Box(
                                modifier = Modifier.size(
                                    width = barStartOffset + barWidth,
                                    height = 40.dp
                                ),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Box(
                                    modifier = Modifier
                                        .offset(x = barStartOffset)
                                        .size(width = barWidth, height = barHeight)
                                        .align(Alignment.CenterStart),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (lifeBarBmp != null) {
                                        Image(
                                            bitmap = lifeBarBmp.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.FillBounds
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(50))
                                                .background(Color(0xFFF4F7FA))
                                                .border(1.5.dp, Color(0xFFBFC8D2), RoundedCornerShape(50))
                                        )
                                    }

                                    Text(
                                        text = timerLabel,
                                        fontFamily = FredokaFontFamily,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = Color(0xFF47535D),
                                        maxLines = 1,
                                        softWrap = false,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(
                                            start = heartWidth - barStartOffset - 2.dp,
                                            end = 6.dp
                                        )
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(width = heartWidth, height = heartHeight)
                                        .align(Alignment.CenterStart),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (heartBmp != null) {
                                        Image(
                                            bitmap = heartBmp.asImageBitmap(),
                                            contentDescription = "Lives ${lifeSnapshot.lives}",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(heartHeight)
                                                .clip(CircleShape)
                                                .background(Color(0xFFE11D48))
                                        )
                                    }
                                    ModalOutlinedText(
                                        text = lifeSnapshot.lives.toString(),
                                        fontSize = 16.sp,
                                        strokeColor = Color(0xFF8C0915),
                                        strokeWidth = 4.5f,
                                        modifier = Modifier.offset(y = (-1.5).dp)
                                    )
                                }
                            }

                            if (!lifeSnapshot.isFull) {
                                val adPress = rememberBouncyPressState(pressedScale = 0.88f)
                                Box(
                                    modifier = Modifier
                                        .size(49.dp)
                                        .minimumInteractiveComponentSize()
                                        .testTag("level_failed_watch_ad_button")
                                        .semantics { contentDescription = "Watch Ad for +1 Life" }
                                        .bouncyClickTarget(adPress, role = Role.Button) {
                                            SoundManager.playTap()
                                            isWatchingAd = true
                                        }
                                        .bouncyScale(adPress),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (plusOneAdBmp != null) {
                                        Image(
                                            bitmap = plusOneAdBmp.asImageBitmap(),
                                            contentDescription = "Watch Ad for +1 Life",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFFFFCA00))
                                                .border(2.dp, Color(0xFF7A4B00), RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            ModalOutlinedText(
                                                text = "+1",
                                                fontSize = 15.sp,
                                                strokeColor = Color(0xFF3A030B),
                                                strokeWidth = 4.5f
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    ModalOutlinedText(
                        text = "Level Failed!",
                        fontSize = 18.5.sp,
                        strokeColor = Color(0xFF0C2E66),
                        strokeWidth = 5.5f
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Home screen GreenPlayPillButton labeled "Try Again"
                    GreenPlayPillButton(
                        text = "Try Again",
                        contentDescription = "Try Again",
                        testTag = "level_failed_try_again_button",
                        fontSize = 17.sp,
                        letterSpacing = 0.9.sp,
                        strokeWidth = 5.2f,
                        widthFraction = 0.68f,
                        onClick = {
                            SoundManager.playTap()
                            onTryAgain()
                        }
                    )
                }
            }
        }

        if (isWatchingAd) {
            SimpleRewardedAdOverlay(
                title = "REWARDED AD",
                subtitle = "Getting +1 Life...",
                onFinished = {
                    isWatchingAd = false
                    onWatchAdPlusOneLife()
                },
                onCancel = {
                    SoundManager.playTap()
                    isWatchingAd = false
                }
            )
        }
    }
}

private fun formatMinutesSecondsWords(remainingMs: Long): String {
    val clampedMs = remainingMs.coerceAtLeast(0L)
    val totalSeconds = (clampedMs + 999L) / 1000L
    val mins = totalSeconds / 60L
    val secs = totalSeconds % 60L
    return "${mins}m ${secs.toString().padStart(2, '0')}s"
}

@Composable
private fun ModalOutlinedText(
    text: String,
    fontSize: TextUnit,
    strokeColor: Color,
    strokeWidth: Float,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = text,
            modifier = Modifier.clearAndSetSemantics {},
            textAlign = textAlign,
            maxLines = 1,
            style = TextStyle(
                fontFamily = FredokaFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = fontSize,
                color = strokeColor,
                drawStyle = Stroke(width = strokeWidth, join = StrokeJoin.Round),
                shadow = Shadow(
                    color = Color(0x88081F44),
                    offset = Offset(0f, 3f),
                    blurRadius = 3f
                )
            )
        )
        Text(
            text = text,
            textAlign = textAlign,
            maxLines = 1,
            style = TextStyle(
                fontFamily = FredokaFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = fontSize,
                color = Color.White
            )
        )
    }
}

@Composable
private fun SimpleRewardedAdOverlay(
    title: String,
    subtitle: String,
    onFinished: () -> Unit,
    onCancel: () -> Unit
) {
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val totalMs = 2000L
        val stepMs = 40L
        var elapsed = 0L
        while (elapsed < totalMs) {
            delay(stepMs)
            elapsed += stepMs
            progress = (elapsed.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
        }
        progress = 1f
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.74f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .fillMaxWidth(0.80f)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF0A2A66))
                .border(3.dp, Color(0xFFFFD700), RoundedCornerShape(22.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontFamily = FredokaFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = Color(0xFFFFE600)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                fontFamily = FredokaFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50)),
                color = Color(0xFF22C55E),
                trackColor = Color(0xFF1E3A8A)
            )
            Spacer(modifier = Modifier.height(14.dp))
            val cancelPress = rememberBouncyPressState(pressedScale = 0.90f)
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF334155))
                    .border(1.5.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                    .bouncyClickTarget(cancelPress, role = Role.Button, onClick = onCancel)
                    .bouncyScale(cancelPress)
                    .padding(horizontal = 18.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Cancel",
                    fontFamily = FredokaFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White
                )
            }
        }
    }
}
