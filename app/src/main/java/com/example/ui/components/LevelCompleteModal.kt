package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.assets.AssetLoader
import com.example.game.sound.SoundManager
import com.example.ui.theme.FredokaFontFamily
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class FlyingCoinState(
    val id: Int,
    val startX: Float,
    val startY: Float,
    val burstX: Float,
    val burstY: Float,
    val startDelayMs: Long,
    var currentX: Float = 0f,
    var currentY: Float = 0f,
    var alpha: Float = 0f,
    var scale: Float = 1f,
    var frameIdx: Int = 0,
    var arrived: Boolean = false
)

@Composable
fun LevelCompleteModal(
    levelNumber: Int,
    coins: Int,
    onPause: () -> Unit = {},
    onNextLevel: () -> Unit,
    onAddCoins: (Int) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // Victory Sprites
    val star1 = remember { AssetLoader.loadBitmap(context, "victory/star_1.png") }
    val star2 = remember { AssetLoader.loadBitmap(context, "victory/star_2.png") }
    val star3 = remember { AssetLoader.loadBitmap(context, "victory/star_3.png") }
    val charGreen = remember { AssetLoader.loadBitmap(context, "victory/char_green.png") }
    val charRed = remember { AssetLoader.loadBitmap(context, "victory/char_red.png") }
    val charPurple = remember { AssetLoader.loadBitmap(context, "victory/char_purple.png") }
    val multiplierBar = remember { AssetLoader.loadBitmap(context, "victory/multiplier_bar.png") }
    val arrowUp = remember { AssetLoader.loadBitmap(context, "victory/arrow_up.png") }
    val btnSmoothBlue = remember { AssetLoader.loadBitmap(context, "victory/btn_smooth_blue.png") }
    val btnSmoothGreen = remember { AssetLoader.loadBitmap(context, "victory/btn_smooth_green.png") }
    val victoryTitleClean = remember {
        AssetLoader.loadBitmap(context, "victory/victory_title_clean.png")
            ?: AssetLoader.loadBitmap(context, "victory/title_main.png")
    }
    val victoryTitleSheet = remember {
        AssetLoader.loadBitmap(context, "victory/victory_title_spritesheet.png")
    }

    // HUD Sprites
    val coinRackBitmap = remember { AssetLoader.loadBitmap(context, "coin_rack.png") }
    val pauseNormalBitmap = remember { AssetLoader.loadBitmap(context, "pause_normal.png") }

    // 8-Frame Spinning Coin Sprites
    val coinFrames = remember {
        (1..8).mapNotNull { i ->
            AssetLoader.loadBitmap(context, "coins/coin_0$i.png")
        }
    }

    var isStopped by remember { mutableStateOf(false) }
    var stoppedMultiplier by remember { mutableIntStateOf(5) }
    var stoppedArrowPos by remember { mutableFloatStateOf(0.5f) }
    var claimedReward by remember { mutableStateOf<Int?>(null) }
    var displayedCoins by remember { mutableIntStateOf(coins) }
    var rackPulseTrigger by remember { mutableIntStateOf(0) }
    val rackScale = remember { Animatable(1f) }

    // Screen element positions in root coordinates (converted to modal-local when drawing)
    var modalOriginInRoot by remember { mutableStateOf(Offset.Zero) }
    var rackTargetInRoot by remember { mutableStateOf(Offset.Zero) }
    var buttonCenterInRoot by remember { mutableStateOf(Offset.Zero) }

    val flyingCoins = remember { mutableStateListOf<FlyingCoinState>() }
    var isCollecting by remember { mutableStateOf(false) }
    var coinAnimTick by remember { mutableLongStateOf(0L) }

    val star1Scale = remember { Animatable(0f) }
    val star2Scale = remember { Animatable(0f) }
    val star3Scale = remember { Animatable(0f) }

    // Spring-bounce top-right coin rack whenever a flying coin arrives
    LaunchedEffect(rackPulseTrigger) {
        if (rackPulseTrigger > 0) {
            rackScale.snapTo(1.22f)
            rackScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    // Stars pop in on mount
    LaunchedEffect(Unit) {
        SoundManager.playWin()
        delay(180)
        SoundManager.playPopBurst()
        star1Scale.animateTo(1f, tween(300))
        delay(120)
        SoundManager.playPopBurst()
        star2Scale.animateTo(1.2f, tween(300))
        delay(120)
        SoundManager.playPopBurst()
        star3Scale.animateTo(1f, tween(300))
    }

    // 1. Victory Title Sprite Animation (pop_in: frames 0..13 @ 24fps, idle: frames 14..25 @ 12fps)
    var titleFrame by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        var lastFrameTime = startTime
        var state = "pop_in"
        var currentFrame = 0

        while (true) {
            withFrameNanos { _ ->
                val now = System.currentTimeMillis()
                val targetFps = if (state == "pop_in") 24 else 12
                val interval = 1000L / targetFps

                if (now - lastFrameTime >= interval) {
                    lastFrameTime = now
                    if (state == "pop_in") {
                        currentFrame++
                        if (currentFrame > 13) {
                            state = "idle"
                            currentFrame = 14
                        }
                    } else {
                        currentFrame++
                        if (currentFrame > 25) {
                            currentFrame = 14
                        }
                    }
                    titleFrame = currentFrame
                }
            }
        }
    }

    // 2. Character Animations
    val animTransition = rememberInfiniteTransition(label = "charAnim")
    val redBounceY by animTransition.animateFloat(
        initialValue = 0f,
        targetValue = -12f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "redBounce"
    )
    val redScaleX by animTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "redScale"
    )
    val greenPulse by animTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1250, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "greenPulse"
    )
    val purplePulse by animTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "purplePulse"
    )

    // 3. Oscillating arrow position (6% to 94%)
    val rawOscillation by animTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrowOsc"
    )

    val liveArrowPos = 0.06f + rawOscillation * 0.88f
    val liveMultiplier = when {
        liveArrowPos < 0.20f -> 2
        liveArrowPos < 0.40f -> 3
        liveArrowPos < 0.60f -> 5
        liveArrowPos < 0.80f -> 3
        else -> 2
    }
    val activeMultiplier = if (isStopped) stoppedMultiplier else liveMultiplier
    val currentArrowPos = if (isStopped) stoppedArrowPos else liveArrowPos

    // Trigger 15 flying coins collection: first scatter outward around button, then arc straight into top-right coin rack
    fun triggerCoinCollection(totalReward: Int, originInRoot: Offset = Offset.Zero) {
        if (isStopped) return
        stoppedArrowPos = liveArrowPos
        stoppedMultiplier = liveMultiplier
        isStopped = true
        claimedReward = totalReward

        SoundManager.playBrickClear()

        val localStart = when {
            originInRoot != Offset.Zero -> originInRoot - modalOriginInRoot
            buttonCenterInRoot != Offset.Zero -> buttonCenterInRoot - modalOriginInRoot
            else -> Offset(450f, 1100f)
        }

        val count = 15
        val burstRadiusMinPx = with(density) { 28.dp.toPx() }
        val burstRadiusSpanPx = with(density) { 38.dp.toPx() }

        flyingCoins.clear()
        for (i in 0 until count) {
            val angle = (i.toFloat() / count) * 2f * PI.toFloat() + (Random.nextFloat() - 0.5f) * 0.45f
            val burstDist = burstRadiusMinPx + Random.nextFloat() * burstRadiusSpanPx
            val bx = localStart.x + cos(angle) * burstDist
            val by = localStart.y + sin(angle) * burstDist

            flyingCoins.add(
                FlyingCoinState(
                    id = i,
                    startX = localStart.x,
                    startY = localStart.y,
                    burstX = bx,
                    burstY = by,
                    startDelayMs = i * 40L,
                    currentX = localStart.x,
                    currentY = localStart.y
                )
            )
        }
        isCollecting = true
    }

    // Coin Flight Animation Loop (60FPS Frame-based driven by coinAnimTick -> Canvas redraw)
    LaunchedEffect(isCollecting) {
        if (!isCollecting || flyingCoins.isEmpty()) return@LaunchedEffect

        val startTime = System.currentTimeMillis()
        val t1 = 260f // scatter burst phase around button
        val t2 = 620f // curved arc flight into top-right coin rack
        val totalCount = flyingCoins.size
        val reward = claimedReward ?: 50
        val perCoin = reward / totalCount
        val remainder = reward - (perCoin * totalCount)
        var arrivedCount = 0

        while (arrivedCount < totalCount) {
            withFrameNanos { nanos ->
                val now = System.currentTimeMillis()
                val elapsedOverall = now - startTime
                val targetLocal = if (rackTargetInRoot != Offset.Zero) {
                    rackTargetInRoot - modalOriginInRoot
                } else {
                    Offset(900f, 90f)
                }

                for (coin in flyingCoins) {
                    if (coin.arrived) continue

                    val coinElapsed = elapsedOverall - coin.startDelayMs
                    if (coinElapsed < 0) {
                        coin.alpha = 0f
                        continue
                    }

                    coin.alpha = 1f
                    coin.frameIdx = (((now / 65) + coin.id) % 8).toInt()

                    if (coinElapsed < t1) {
                        // Phase 1: Scatter outward from button with pop scale
                        val p = (coinElapsed / t1).coerceIn(0f, 1f)
                        val easeOut = 1f - (1f - p) * (1f - p) * (1f - p)
                        coin.currentX = coin.startX + (coin.burstX - coin.startX) * easeOut
                        coin.currentY = coin.startY + (coin.burstY - coin.startY) * easeOut
                        coin.scale = 0.65f + 0.50f * easeOut
                    } else {
                        // Phase 2: Quadratic Bezier arc directly into top-right Coin Rack emblem
                        val p = ((coinElapsed - t1) / t2).coerceIn(0f, 1f)
                        val e = p * p * (3f - 2f * p)
                        val arcDipPx = with(density) { 46.dp.toPx() }
                        // Control point curves gracefully left/up toward the corner rack
                        val ctrlX = (coin.burstX + targetLocal.x) * 0.5f - arcDipPx * 0.5f
                        val ctrlY = minOf(coin.burstY, targetLocal.y) - arcDipPx
                        val oneMinusE = 1f - e
                        coin.currentX = oneMinusE * oneMinusE * coin.burstX +
                            2f * oneMinusE * e * ctrlX +
                            e * e * targetLocal.x
                        coin.currentY = oneMinusE * oneMinusE * coin.burstY +
                            2f * oneMinusE * e * ctrlY +
                            e * e * targetLocal.y
                        coin.scale = 1.15f - 0.52f * e

                        if (p >= 1f) {
                            coin.arrived = true
                            coin.alpha = 0f
                            arrivedCount++

                            rackPulseTrigger++
                            SoundManager.playCoinCollect(coin.id * 12f)
                            displayedCoins += perCoin + if (arrivedCount == totalCount) remainder else 0

                            if (arrivedCount == totalCount) {
                                onAddCoins(reward)
                            }
                        }
                    }
                }
                coinAnimTick = nanos
            }
        }

        // Briefly pause so user sees the updated coin balance in top-right rack
        delay(380)
        onNextLevel()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                modalOriginInRoot = coords.positionInRoot()
            }
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {}
    ) {
        // 1. TOP HUD (Pause Button at top-left, Coin Rack at top-right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (pauseNormalBitmap != null) {
                val pausePress = rememberBouncyPressState(pressedScale = 0.86f)
                Image(
                    bitmap = pauseNormalBitmap.asImageBitmap(),
                    contentDescription = "Pause",
                    modifier = Modifier
                        .size(52.dp, 56.dp)
                        .testTag("victory_pause_button")
                        .bouncyClickTarget(pausePress) { onPause() }
                        .bouncyScale(pausePress)
                )
            } else {
                Spacer(modifier = Modifier.size(52.dp, 56.dp))
            }

            // Coin Rack (top-right) - flying coins target the left circular gold coin icon on this rack
            UniversalCoinRack(
                bitmap = coinRackBitmap,
                coins = displayedCoins,
                modifier = Modifier
                    .onGloballyPositioned { coords ->
                        val pos = coords.positionInRoot()
                        rackTargetInRoot = Offset(
                            pos.x + coords.size.width * 0.17f,
                            pos.y + coords.size.height * 0.54f
                        )
                    }
                    .scale(rackScale.value)
                    .testTag("victory_coin_rack")
            )
        }

        // 2. MAIN VICTORY MODAL DIALOG (Centered)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 24.dp)
                .offset(y = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. VICTORY Title Sprite Animation
            Box(
                modifier = Modifier
                    .width(260.dp)
                    .height(86.dp),
                contentAlignment = Alignment.Center
            ) {
                if (victoryTitleSheet != null) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val col = titleFrame % 4
                        val row = titleFrame / 4
                        val src = Rect(col * 720, row * 240, (col + 1) * 720, (row + 1) * 240)
                        val dst = RectF(0f, 0f, size.width, size.height)
                        drawContext.canvas.nativeCanvas.drawBitmap(victoryTitleSheet, src, dst, null)
                    }
                } else if (victoryTitleClean != null) {
                    Image(
                        bitmap = victoryTitleClean.asImageBitmap(),
                        contentDescription = "Victory",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = "VICTORY!",
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 38.sp,
                        color = Color(0xFFFFD700)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 2. 3 Stars Pop In
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (star1 != null) {
                    Image(
                        bitmap = star1.asImageBitmap(),
                        contentDescription = "Star 1",
                        modifier = Modifier
                            .size(46.dp)
                            .scale(star1Scale.value)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                if (star2 != null) {
                    Image(
                        bitmap = star2.asImageBitmap(),
                        contentDescription = "Star 2",
                        modifier = Modifier
                            .size(60.dp)
                            .offset(y = (-6).dp)
                            .scale(star2Scale.value)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                if (star3 != null) {
                    Image(
                        bitmap = star3.asImageBitmap(),
                        contentDescription = "Star 3",
                        modifier = Modifier
                            .size(46.dp)
                            .scale(star3Scale.value)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 3. 3 Cute Characters
            Row(
                modifier = Modifier.width(260.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                if (charGreen != null) {
                    Image(
                        bitmap = charGreen.asImageBitmap(),
                        contentDescription = "Character Green",
                        modifier = Modifier
                            .size(54.dp)
                            .scale(greenPulse)
                            .alpha(0.85f + 0.15f * greenPulse)
                    )
                }
                if (charRed != null) {
                    Image(
                        bitmap = charRed.asImageBitmap(),
                        contentDescription = "Character Red",
                        modifier = Modifier
                            .size(72.dp)
                            .offset(y = redBounceY.dp)
                            .scale(redScaleX, 2f - redScaleX)
                    )
                }
                if (charPurple != null) {
                    Image(
                        bitmap = charPurple.asImageBitmap(),
                        contentDescription = "Character Purple",
                        modifier = Modifier
                            .size(54.dp)
                            .scale(purplePulse)
                            .alpha(0.85f + 0.15f * purplePulse)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Multiplier Bar & 3D Yellow Pointer Arrow
            // Tightly matched to multiplier_bar.png natural 6:1 aspect ratio (318x53) so border hugs the bar with zero left/right gap
            BoxWithConstraints(
                modifier = Modifier
                    .width(246.dp)
                    .testTag("victory_multiplier_bar")
            ) {
                val barW = maxWidth

                Column(modifier = Modifier.fillMaxWidth()) {
                    if (multiplierBar != null) {
                        Image(
                            bitmap = multiplierBar.asImageBitmap(),
                            contentDescription = "Multiplier Bar",
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(318f / 53f)
                                .clip(RoundedCornerShape(50))
                                .border(1.5.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(50))
                        )
                    }

                    // Arrow indicator oscillating across exact track width
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                    ) {
                        val arrowX = barW * currentArrowPos
                        Box(
                            modifier = Modifier
                                .offset(x = arrowX - 10.dp, y = 1.dp)
                                .scale(if (isStopped) 1.25f else 1f)
                        ) {
                            if (arrowUp != null) {
                                Image(
                                    bitmap = arrowUp.asImageBitmap(),
                                    contentDescription = "Arrow",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            if (isStopped) {
                                Box(
                                    modifier = Modifier
                                        .offset(x = (-8).dp, y = (-22).dp)
                                        .background(Color(0xFFFFC107), RoundedCornerShape(10.dp))
                                        .border(1.dp, Color.White, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "x${activeMultiplier}!",
                                        fontFamily = FredokaFontFamily,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        color = Color(0xFF2E0854)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Claim Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coords ->
                        val pos = coords.positionInRoot()
                        buttonCenterInRoot = Offset(pos.x + coords.size.width / 2f, pos.y + coords.size.height / 2f)
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                var blueBtnPos by remember { mutableStateOf(Offset.Zero) }
                var greenBtnPos by remember { mutableStateOf(Offset.Zero) }

                // Top Button: Blue Glossy Multiplied Claim Button
                val bluePress = rememberBouncyPressState(pressedScale = 0.88f)
                Box(
                    modifier = Modifier
                        .width(170.dp)
                        .height(48.dp)
                        .testTag("claim_multiplied_button")
                        .onGloballyPositioned { coords ->
                            val pos = coords.positionInRoot()
                            blueBtnPos = Offset(pos.x + coords.size.width / 2f, pos.y + coords.size.height / 2f)
                        }
                        .bouncyClickTarget(bluePress, enabled = !isStopped) {
                            val origin = if (blueBtnPos != Offset.Zero) blueBtnPos else buttonCenterInRoot
                            triggerCoinCollection(50 * activeMultiplier, origin)
                        }
                        .bouncyScale(bluePress),
                    contentAlignment = Alignment.Center
                ) {
                    if (btnSmoothBlue != null) {
                        Image(
                            bitmap = btnSmoothBlue.asImageBitmap(),
                            contentDescription = "Claim Multiplied Coins",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Text(
                        text = "${50 * activeMultiplier}",
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color.White,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Button: Green Glossy Standard 50 Coins Button
                val greenPress = rememberBouncyPressState(pressedScale = 0.88f)
                Box(
                    modifier = Modifier
                        .width(170.dp)
                        .height(48.dp)
                        .testTag("claim_base_button")
                        .onGloballyPositioned { coords ->
                            val pos = coords.positionInRoot()
                            greenBtnPos = Offset(pos.x + coords.size.width / 2f, pos.y + coords.size.height / 2f)
                        }
                        .bouncyClickTarget(greenPress, enabled = !isStopped) {
                            val origin = if (greenBtnPos != Offset.Zero) greenBtnPos else buttonCenterInRoot
                            triggerCoinCollection(50, origin)
                        }
                        .bouncyScale(greenPress),
                    contentAlignment = Alignment.Center
                ) {
                    if (btnSmoothGreen != null) {
                        Image(
                            bitmap = btnSmoothGreen.asImageBitmap(),
                            contentDescription = "Claim 50 Coins",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Text(
                        text = "50",
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color.White,
                        modifier = Modifier.padding(start = 28.dp)
                    )
                }
            }

            // Claim Notification Banner
            if (claimedReward != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .background(Color(0xFFFFD700), RoundedCornerShape(20.dp))
                        .border(2.dp, Color.White, RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "+$claimedReward COINS CLAIMED!",
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF3B0066)
                    )
                }
            }
        }

        // 3. 60FPS CANVAS OVERLAY FOR 15 SPINNING COINS FLYING INTO TOP-RIGHT COIN RACK
        if (isCollecting && coinFrames.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Reading coinAnimTick ensures smooth redraw every animation frame
                @Suppress("UNUSED_VARIABLE")
                val tick = coinAnimTick
                val baseCoinSizePx = 34.dp.toPx()

                for (coin in flyingCoins) {
                    if (coin.alpha <= 0f || coin.arrived) continue
                    val bmp = coinFrames[coin.frameIdx.coerceIn(0, coinFrames.lastIndex)]
                    val drawSize = baseCoinSizePx * coin.scale
                    val half = drawSize * 0.5f
                    val dst = RectF(
                        coin.currentX - half,
                        coin.currentY - half,
                        coin.currentX + half,
                        coin.currentY + half
                    )
                    drawContext.canvas.nativeCanvas.drawBitmap(bmp, null, dst, null)
                }
            }
        }
    }
}
