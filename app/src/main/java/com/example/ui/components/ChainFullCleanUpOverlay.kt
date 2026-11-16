package com.example.ui.components

import android.graphics.Paint
import android.graphics.RectF
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.assets.AssetLoader
import com.example.game.belt.BeltPath
import com.example.game.constants.GameConstants
import com.example.game.model.BeltBall
import com.example.game.model.FunnelBall
import com.example.game.model.GridCell
import com.example.game.rules.GameRules
import com.example.game.sound.SoundManager
import com.example.ui.theme.FredokaFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

const val CLEAN_UP_COIN_COST = 900

@Composable
fun ChainFullCleanUpOverlay(
    coins: Int,
    beltCapacity: Int,
    highlightedBeltBalls: List<BeltBall>,
    viewScale: Float,
    viewOffsetX: Float,
    viewArenaOffsetY: Float,
    grid: Array<Array<GridCell>>? = null,
    funnelBalls: List<FunnelBall> = emptyList(),
    beltBalls: List<BeltBall> = emptyList(),
    beltScrollDistance: Float = 0f,
    onWatchAdCleanUp: () -> Unit,
    onCoinsCleanUp: () -> Unit,
    onQuit: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val quitBmp = remember { AssetLoader.loadBitmap(context, "cleanup/btn_quit.png") }
    val watchAdBmp = remember { AssetLoader.loadBitmap(context, "cleanup/btn_cleanup_watch_ad.png") }
    val coinsBtnBmp = remember { AssetLoader.loadBitmap(context, "cleanup/btn_cleanup_coins.png") }

    var showOutOfSpaceModal by remember { mutableStateOf(false) }
    var isWatchingAd by remember { mutableStateOf(false) }
    var insufficientCoinsTick by remember { mutableIntStateOf(0) }
    var showNotEnoughCoinsBanner by remember { mutableStateOf(false) }
    val coinsShakeOffset = remember { Animatable(0f) }

    BackHandler(enabled = showOutOfSpaceModal && !isWatchingAd) {
        SoundManager.playTap()
        showOutOfSpaceModal = false
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

    val watchAdIntroScale = remember { Animatable(0.28f) }
    val coinsIntroScale = remember { Animatable(0.28f) }
    val buttonsIntroAlpha = remember { Animatable(0f) }
    val watchAdNudgeY = remember { Animatable(0f) }
    val watchAdNudgeScale = remember { Animatable(1f) }
    val coinsNudgeY = remember { Animatable(0f) }
    val coinsNudgeScale = remember { Animatable(1f) }
    val quitRevealProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Gentle one-time entrance: Clean Up buttons pop smoothly from small to normal size, Quit glides in
        launch {
            buttonsIntroAlpha.animateTo(1f, tween(durationMillis = 180))
        }
        launch {
            quitRevealProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
            )
        }
        launch {
            watchAdIntroScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.62f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        launch {
            delay(70)
            coinsIntroScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.62f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }

        // Periodic subtle upward "tap me" prompt every ~2.5 seconds
        while (true) {
            delay(2500)
            if (!isWatchingAd && !showOutOfSpaceModal) {
                launch {
                    watchAdNudgeScale.animateTo(1.045f, tween(170, easing = FastOutSlowInEasing))
                    watchAdNudgeScale.animateTo(1f, tween(240, easing = FastOutSlowInEasing))
                }
                launch {
                    watchAdNudgeY.animateTo(-7f, tween(170, easing = FastOutSlowInEasing))
                    watchAdNudgeY.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = 0.56f,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
                delay(110)
                launch {
                    coinsNudgeScale.animateTo(1.045f, tween(170, easing = FastOutSlowInEasing))
                    coinsNudgeScale.animateTo(1f, tween(240, easing = FastOutSlowInEasing))
                }
                launch {
                    coinsNudgeY.animateTo(-7f, tween(170, easing = FastOutSlowInEasing))
                    coinsNudgeY.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = 0.56f,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
            }
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "cleanup_highlight_pulse")
    val highlightPulse by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ball_highlight"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .testTag("chain_full_cleanup_overlay")
            .background(Color.Black.copy(alpha = 0.48f))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    }
                }
            }
    ) {
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val navBarBottomPx = WindowInsets.navigationBars.getBottom(density).toFloat()

        if (!showOutOfSpaceModal) {
            // 1. Highlight the 8 selected chain balls cleanly above the translucent black scrim
            val beltBallScale = if (beltCapacity > 24) 0.85f else 1f
            if (viewScale > 0f && highlightedBeltBalls.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvas = drawContext.canvas.nativeCanvas
                canvas.save()
                canvas.translate(viewOffsetX, viewArenaOffsetY)
                canvas.scale(viewScale, viewScale)

                val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.parseColor("#FFE875")
                    alpha = (115 + (highlightPulse * 70f)).toInt().coerceIn(0, 255)
                }
                val outerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.WHITE
                    alpha = (210 + (highlightPulse * 45f)).toInt().coerceIn(0, 255)
                    style = Paint.Style.STROKE
                    strokeWidth = 1.6f + highlightPulse * 0.6f
                }

                for (ball in highlightedBeltBalls) {
                    val coords = BeltPath.getBeltCoordinates(ball.s)
                    val r = GameConstants.BALL_RADIUS_BELT * 1.35f * beltBallScale *
                        (1.04f + 0.05f * highlightPulse)

                    // Bright halo + crisp white outline around each highlighted chain ball
                    canvas.drawCircle(coords.x, coords.y, r + 2.4f + highlightPulse * 1.0f, glowPaint)
                    canvas.drawCircle(coords.x, coords.y, r + 0.8f, outerRingPaint)

                    val fluffyBmp = AssetLoader.getFluffy(context, ball.color, "surprised")
                        ?: AssetLoader.getFluffy(context, ball.color, ball.mood)
                    if (fluffyBmp != null) {
                        canvas.drawBitmap(
                            fluffyBmp,
                            null,
                            RectF(coords.x - r, coords.y - r, coords.x + r, coords.y + r),
                            null
                        )
                    }
                }
                canvas.restore()
            }
        }

        // Compute responsive positions aligned with the belt geometry so the 8 highlighted balls remain clearly visible
        val rowHorizontalMarginPx = with(density) { 12.dp.toPx() }
        val maxRowWidthPx = with(density) { 420.dp.toPx() }
        val rowWidthPx = (maxWidthPx - rowHorizontalMarginPx * 2f).coerceAtMost(maxRowWidthPx)
        val gapPx = with(density) { 10.dp.toPx() }
        val singleCleanBtnWidthPx = (rowWidthPx - gapPx) / 2f
        val singleCleanBtnHeightPx = singleCleanBtnWidthPx / (2534f / 1315f)

        val defaultCleanUpTopPx = maxHeightPx - navBarBottomPx - singleCleanBtnHeightPx - with(density) { 26.dp.toPx() }
        val cleanUpTopPx = if (viewScale > 0f) {
            val beltBottomScreenY = viewArenaOffsetY + (GameConstants.BELT_BOTTOM_Y + 14f) * viewScale
            beltBottomScreenY.coerceIn(
                with(density) { 120.dp.toPx() },
                (maxHeightPx - navBarBottomPx - singleCleanBtnHeightPx - with(density) { 14.dp.toPx() })
                    .coerceAtLeast(with(density) { 120.dp.toPx() })
            )
        } else {
            defaultCleanUpTopPx
        }

        val quitWidthPx = if (viewScale > 0f) {
            (84f * viewScale).coerceIn(with(density) { 82.dp.toPx() }, with(density) { 114.dp.toPx() })
        } else {
            with(density) { 90.dp.toPx() }
        }
        val quitHeightPx = quitWidthPx / (2174f / 1231f)
        val quitLeftBlendPx = -(quitWidthPx * 0.16f)
        val quitSlideExtraPx = -(quitWidthPx * 0.45f) * (1f - quitRevealProgress.value)
        val quitTopPx = if (viewScale > 0f) {
            val beltTopScreenY = viewArenaOffsetY + (GameConstants.BELT_TOP_Y + 1f) * viewScale
            (beltTopScreenY - quitHeightPx).coerceAtMost(cleanUpTopPx - quitHeightPx - with(density) { 8.dp.toPx() })
        } else {
            cleanUpTopPx - quitHeightPx - with(density) { 12.dp.toPx() }
        }

        // 2. Red Quit Button (blended gently off the left screen edge just above the belt)
        val quitPress = rememberBouncyPressState(pressedScale = 0.88f)
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (quitLeftBlendPx + quitSlideExtraPx).roundToInt(),
                        y = quitTopPx.roundToInt().coerceAtLeast(0)
                    )
                }
                .width(with(density) { quitWidthPx.toDp() })
                .aspectRatio(2174f / 1231f)
                .minimumInteractiveComponentSize()
                .testTag("cleanup_quit_button")
                .bouncyClickTarget(quitPress, role = Role.Button) {
                    SoundManager.playTap()
                    showOutOfSpaceModal = true
                }
                .graphicsLayer {
                    alpha = quitRevealProgress.value
                }
                .bouncyScale(quitPress),
            contentAlignment = Alignment.Center
        ) {
            if (quitBmp != null) {
                Image(
                    bitmap = quitBmp.asImageBitmap(),
                    contentDescription = "Quit",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
            } else {
                Text(
                    text = "Quit",
                    fontFamily = FredokaFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = Color.White
                )
            }
        }

        // 3. Side-by-side Clean Up! Buttons (Watch Ad & 900 Coins)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset { IntOffset(x = 0, y = cleanUpTopPx.roundToInt().coerceAtLeast(0)) }
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Clean Up! Watch Ad (Yellow Button)
            val watchAdPress = rememberBouncyPressState(pressedScale = 0.90f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(2529f / 1315f)
                    .minimumInteractiveComponentSize()
                    .testTag("cleanup_watch_ad_button")
                    .bouncyClickTarget(watchAdPress, role = Role.Button) {
                        SoundManager.playTap()
                        isWatchingAd = true
                    }
                    .graphicsLayer {
                        val s = watchAdIntroScale.value * watchAdNudgeScale.value
                        scaleX = s
                        scaleY = s
                        alpha = buttonsIntroAlpha.value
                        translationY = with(density) { watchAdNudgeY.value.dp.toPx() }
                    }
                    .bouncyScale(watchAdPress),
                contentAlignment = Alignment.Center
            ) {
                if (watchAdBmp != null) {
                    Image(
                        bitmap = watchAdBmp.asImageBitmap(),
                        contentDescription = "Clean Up Watch Ad",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                } else {
                    Text(
                        text = "Clean Up! Watch Ad",
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Right: Clean Up! 900 Coins (Green Button)
            val coinsPress = rememberBouncyPressState(pressedScale = 0.90f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .offset { IntOffset(x = coinsShakeOffset.value.roundToInt(), y = 0) }
                    .aspectRatio(2534f / 1315f)
                    .minimumInteractiveComponentSize()
                    .testTag("cleanup_coins_button")
                    .bouncyClickTarget(coinsPress, role = Role.Button) {
                        if (coins >= CLEAN_UP_COIN_COST) {
                            SoundManager.playCoinCollect(0f)
                            onCoinsCleanUp()
                        } else {
                            SoundManager.playBlocked()
                            insufficientCoinsTick++
                        }
                    }
                    .graphicsLayer {
                        val s = coinsIntroScale.value * coinsNudgeScale.value
                        scaleX = s
                        scaleY = s
                        alpha = buttonsIntroAlpha.value
                        translationY = with(density) { coinsNudgeY.value.dp.toPx() }
                    }
                    .bouncyScale(coinsPress),
                contentAlignment = Alignment.Center
            ) {
                if (coinsBtnBmp != null) {
                    Image(
                        bitmap = coinsBtnBmp.asImageBitmap(),
                        contentDescription = "Clean Up 900 Coins",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                } else {
                    Text(
                        text = "Clean Up! 900",
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Insufficient Coins Toast Pill
        if (showNotEnoughCoinsBanner) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset {
                        IntOffset(
                            x = 0,
                            y = (cleanUpTopPx - with(density) { 44.dp.toPx() }).roundToInt().coerceAtLeast(0)
                        )
                    }
                    .shadow(8.dp, RoundedCornerShape(20.dp))
                    .background(Color(0xFFE93D5A), RoundedCornerShape(20.dp))
                    .border(2.dp, Color.White, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Need 900 coins! Watch Ad to Clean Up",
                    fontFamily = FredokaFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White
                )
            }
            }
        } else {
            val previewSnapshot = remember(showOutOfSpaceModal) {
                if (grid != null) {
                    GameRules.buildCleanUpPreviewSnapshot(
                        grid = grid,
                        funnelBalls = funnelBalls,
                        beltBalls = beltBalls.ifEmpty { highlightedBeltBalls },
                        highlightedBeltIds = highlightedBeltBalls.map { it.id }.toSet(),
                        beltCapacity = beltCapacity,
                        beltScrollDistance = beltScrollDistance
                    )
                } else {
                    null
                }
            }
            OutOfSpaceModal(
                coins = coins,
                previewSnapshot = previewSnapshot,
                onWatchAdPlayOn = { isWatchingAd = true },
                onCoinsPlayOn = onCoinsCleanUp,
                onCloseToHome = onQuit
            )
        }

        // Rewarded Ad Modal Flow
        if (isWatchingAd) {
            WatchingRewardedAdDialog(
                onAdFinished = {
                    isWatchingAd = false
                    onWatchAdCleanUp()
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
private fun WatchingRewardedAdDialog(
    onAdFinished: () -> Unit,
    onCancel: () -> Unit
) {
    var progress by remember { mutableFloatStateOf(0f) }
    var remainingSeconds by remember { mutableIntStateOf(3) }

    LaunchedEffect(Unit) {
        val totalMs = 2400L
        val stepMs = 40L
        var elapsed = 0L
        while (elapsed < totalMs) {
            delay(stepMs)
            elapsed += stepMs
            progress = (elapsed.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
            val secsLeft = (((totalMs - elapsed) + 799L) / 800L).toInt().coerceIn(0, 3)
            remainingSeconds = secsLeft
        }
        progress = 1f
        remainingSeconds = 0
        onAdFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 310.dp)
                .fillMaxWidth(0.84f)
                .shadow(16.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0F3B82), Color(0xFF071E4A))
                    )
                )
                .border(3.dp, Color(0xFFFFD700), RoundedCornerShape(24.dp))
                .padding(horizontal = 22.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "REWARDED AD",
                fontFamily = FredokaFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                color = Color(0xFFFFE600),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (remainingSeconds > 0) {
                    "Clean Up reward ready in ${remainingSeconds}s..."
                } else {
                    "Reward unlocked! Cleaning up..."
                },
                fontFamily = FredokaFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(50)),
                color = Color(0xFF22C55E),
                trackColor = Color(0xFF1E3A8A)
            )

            Spacer(modifier = Modifier.height(18.dp))

            val cancelPress = rememberBouncyPressState(pressedScale = 0.90f)
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF334155))
                    .border(1.5.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .testTag("rewarded_ad_cancel_button")
                    .bouncyClickTarget(cancelPress, role = Role.Button, onClick = onCancel)
                    .bouncyScale(cancelPress)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
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
