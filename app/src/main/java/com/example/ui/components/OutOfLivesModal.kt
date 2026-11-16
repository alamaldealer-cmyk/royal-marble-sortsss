package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
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

@Composable
fun OutOfLivesModal(
    lifeSnapshot: LifeSnapshot,
    coins: Int,
    onWatchAdForLife: () -> Unit,
    onCoinsRefillAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val panelBmp = remember { AssetLoader.loadBitmap(context, "cleanup/panel_out_of_space.png") }
    val closeXBmp = remember { AssetLoader.loadBitmap(context, "cleanup/btn_close_x.png") }
    val heartBmp = remember { AssetLoader.loadBitmap(context, "hud/hud_heart.png") }
    val coinBmp = remember { AssetLoader.loadBitmap(context, "hud/hud_coin.png") }
    val coinsBtnBmp = remember { AssetLoader.loadBitmap(context, "hud/btn_lives_900_coins.png") }
    val watchAdBtnBmp = remember { AssetLoader.loadBitmap(context, "hud/btn_lives_watch_ad.png") }

    val modalScale = remember { Animatable(0.90f) }
    val modalAlpha = remember { Animatable(0.15f) }
    var isWatchingAd by remember { mutableStateOf(false) }
    var insufficientCoinsTick by remember { mutableIntStateOf(0) }
    var showNotEnoughCoinsBanner by remember { mutableStateOf(false) }
    val coinsShakeOffset = remember { Animatable(0f) }

    BackHandler(enabled = !isWatchingAd) {
        SoundManager.playTap()
        onDismiss()
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
                    animationSpec = tween(45)
                )
            }
            coinsShakeOffset.animateTo(0f, tween(50))
            delay(1600)
            showNotEnoughCoinsBanner = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("out_of_lives_modal")
            .background(Color.Black.copy(alpha = 0.58f))
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
                .widthIn(max = 340.dp)
                .fillMaxWidth(0.86f)
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

                // Header Row
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.155f)
                        .align(Alignment.TopCenter)
                ) {
                    val title = if (lifeSnapshot.lives == 0) "Out of Lives!" else "Get Lives!"
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 46.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            style = TextStyle(
                                fontFamily = FredokaFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                color = Color(0xFF0C2E66),
                                drawStyle = Stroke(width = 7f, join = StrokeJoin.Round),
                                shadow = Shadow(Color(0x88081F44), Offset(0f, 3f), 3f)
                            )
                        )
                        Text(
                            text = title,
                            style = TextStyle(
                                fontFamily = FredokaFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                color = Color.White
                            )
                        )
                    }

                    val closePress = rememberBouncyPressState(pressedScale = 0.86f)
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp)
                            .size(52.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("out_of_lives_close_button")
                            .bouncyClickTarget(closePress, role = Role.Button) {
                                SoundManager.playTap()
                                onDismiss()
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
                                fontSize = 20.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                // Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.845f)
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Heart + Timer Showcase
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(width = 86.dp, height = 78.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (heartBmp != null) {
                                Image(
                                    bitmap = heartBmp.asImageBitmap(),
                                    contentDescription = "Lives ${lifeSnapshot.lives}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            val numStr = lifeSnapshot.lives.toString()
                            Box(modifier = Modifier.offset(y = (-2).dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = numStr,
                                    style = TextStyle(
                                        fontFamily = FredokaFontFamily,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 34.sp,
                                        color = Color(0xFF8C0915),
                                        drawStyle = Stroke(width = 7f, join = StrokeJoin.Round)
                                    )
                                )
                                Text(
                                    text = numStr,
                                    style = TextStyle(
                                        fontFamily = FredokaFontFamily,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 34.sp,
                                        color = Color.White
                                    )
                                )
                            }
                        }

                        // Next Life Timer Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.White.copy(alpha = 0.94f))
                                .border(2.dp, Color(0xFF8FB4DB), RoundedCornerShape(18.dp))
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                        ) {
                            val label = if (lifeSnapshot.isFull) {
                                "Lives Full (5/5)"
                            } else {
                                "Next life in ${LivesManager.formatCountdownTimer(lifeSnapshot.remainingMs)}"
                            }
                            Text(
                                text = label,
                                fontFamily = FredokaFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = Color(0xFF47535D)
                            )
                        }
                    }

                    Text(
                        text = if (lifeSnapshot.isFull) {
                            "You have all 5 lives ready to play!"
                        } else {
                            "1 life refills every 20 minutes.\nRefill now to keep playing!"
                        },
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        lineHeight = 18.sp,
                        color = Color(0xFF0E3369),
                        textAlign = TextAlign.Center
                    )

                    if (!lifeSnapshot.isFull) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Refill Full (900 Coins -> +5 Lives)
                            val coinsPress = rememberBouncyPressState(pressedScale = 0.90f)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .offset { IntOffset(x = coinsShakeOffset.value.roundToInt(), y = 0) }
                                    .aspectRatio(253f / 118f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("out_of_lives_coins_button")
                                    .bouncyClickTarget(coinsPress, role = Role.Button) {
                                        if (coins >= LivesManager.REFILL_COIN_COST) {
                                            SoundManager.playCoinCollect(0f)
                                            onCoinsRefillAll()
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
                                        contentDescription = "Refill Full Lives for ${LivesManager.REFILL_COIN_COST} Coins",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .shadow(4.dp, RoundedCornerShape(16.dp))
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color(0xFF4ADE80), Color(0xFF16A34A))
                                                )
                                            )
                                            .border(2.5.dp, Color(0xFF064E3B), RoundedCornerShape(16.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (coinBmp != null) {
                                                Image(
                                                    bitmap = coinBmp.asImageBitmap(),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Text(
                                                text = "${LivesManager.REFILL_COIN_COST} (+${LivesManager.MAX_LIVES})",
                                                fontFamily = FredokaFontFamily,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 14.sp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            // Watch Ad (Free -> +3 Lives)
                            val adPress = rememberBouncyPressState(pressedScale = 0.90f)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(253f / 118f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("out_of_lives_watch_ad_button")
                                    .bouncyClickTarget(adPress, role = Role.Button) {
                                        SoundManager.playTap()
                                        isWatchingAd = true
                                    }
                                    .bouncyScale(adPress),
                                contentAlignment = Alignment.Center
                            ) {
                                if (watchAdBtnBmp != null) {
                                    Image(
                                        bitmap = watchAdBtnBmp.asImageBitmap(),
                                        contentDescription = "Watch Ad for +${LivesManager.AD_REWARD_LIVES} Lives",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .shadow(4.dp, RoundedCornerShape(16.dp))
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color(0xFFE846FF), Color(0xFFB312DC))
                                                )
                                            )
                                            .border(2.5.dp, Color(0xFFFFD54F), RoundedCornerShape(16.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Free (+${LivesManager.AD_REWARD_LIVES})",
                                            fontFamily = FredokaFontFamily,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
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
                        text = "Need 900 coins! Watch Ad for +${LivesManager.AD_REWARD_LIVES} Lives",
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }
        }

        if (isWatchingAd) {
            var adProgress by remember { mutableFloatStateOf(0f) }
            LaunchedEffect(Unit) {
                val totalMs = 2000L
                val stepMs = 40L
                var elapsed = 0L
                while (elapsed < totalMs) {
                    delay(stepMs)
                    elapsed += stepMs
                    adProgress = (elapsed.toFloat() / totalMs).coerceIn(0f, 1f)
                }
                isWatchingAd = false
                onWatchAdForLife()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 290.dp)
                        .fillMaxWidth(0.8f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF0A2A66))
                        .border(3.dp, Color(0xFFFFD700), RoundedCornerShape(22.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "REWARDED AD",
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = Color(0xFFFFE600)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Getting +${LivesManager.AD_REWARD_LIVES} Lives...",
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { adProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(50)),
                        color = Color(0xFF22C55E),
                        trackColor = Color(0xFF1E3A8A)
                    )
                }
            }
        }
    }
}
