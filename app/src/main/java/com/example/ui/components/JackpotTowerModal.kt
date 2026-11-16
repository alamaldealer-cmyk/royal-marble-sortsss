package com.example.ui.components

import android.graphics.RectF
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.example.game.assets.AssetLoader
import com.example.game.jackpot.JackpotEventManager
import com.example.game.sound.SoundManager
import com.example.ui.theme.FredokaFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

val JACKPOT_TIER_REWARDS = listOf(50, 100, 200, 400, 800)

@Composable
fun JackpotTowerModal(
    coins: Int,
    claimedSteps: Int,
    remainingMs: Long = JackpotEventManager.CYCLE_DURATION_MS,
    onClaimTier: (rewardCoins: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val watchAdBtnBmp = remember { AssetLoader.getJackpotWatchAdButton(context) }
    val closeXBmp = remember { AssetLoader.loadBitmap(context, "cleanup/btn_close_x.png") }
    val coinRackBitmap = remember { AssetLoader.loadBitmap(context, "coin_rack.png") }
    val hudCoinBmp = remember { AssetLoader.loadBitmap(context, "hud/hud_coin.png") }
    val coinFrames = remember {
        (1..8).mapNotNull { i -> AssetLoader.loadBitmap(context, "coins/coin_0$i.png") }
    }

    val safeClaimedSteps = claimedSteps.coerceIn(0, JACKPOT_TIER_REWARDS.size)
    val allClaimed = safeClaimedSteps >= JACKPOT_TIER_REWARDS.size
    val activeTierIndex = safeClaimedSteps.coerceIn(0, JACKPOT_TIER_REWARDS.lastIndex)
    val currentReward = JACKPOT_TIER_REWARDS[activeTierIndex]
    val isFreeStep = safeClaimedSteps == 0

    // Entry pop-in
    val modalScale = remember { Animatable(0.86f) }
    val modalAlpha = remember { Animatable(0.15f) }
    LaunchedEffect(Unit) {
        launch { modalAlpha.animateTo(1f, tween(170)) }
        launch {
            modalScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.72f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    var isCelebratingBurst by remember { mutableStateOf(false) }
    LaunchedEffect(isCelebratingBurst) {
        if (isCelebratingBurst) {
            delay(850L)
            isCelebratingBurst = false
        }
    }

    // Coin rack & 60fps flying coins state (matching LevelCompleteModal)
    var displayedCoins by remember { mutableIntStateOf(coins) }
    var rackPulseTrigger by remember { mutableIntStateOf(0) }
    val rackScale = remember { Animatable(1f) }
    var modalOriginInRoot by remember { mutableStateOf(Offset.Zero) }
    var rackTargetInRoot by remember { mutableStateOf(Offset.Zero) }
    var claimButtonCenterInRoot by remember { mutableStateOf(Offset.Zero) }

    val flyingCoins = remember { mutableStateListOf<FlyingCoinState>() }
    var isCollecting by remember { mutableStateOf(false) }
    var pendingReward by remember { mutableIntStateOf(0) }
    var rewardCommitted by remember { mutableStateOf(false) }
    var coinAnimTick by remember { mutableLongStateOf(0L) }
    var isWatchingAd by remember { mutableStateOf(false) }

    LaunchedEffect(coins) {
        if (!isCollecting) {
            displayedCoins = coins
        }
    }

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

    fun commitPendingRewardIfNeeded() {
        if (pendingReward > 0 && !rewardCommitted) {
            rewardCommitted = true
            onClaimTier(pendingReward)
            pendingReward = 0
        }
    }

    fun handleClose() {
        if (isWatchingAd) return
        commitPendingRewardIfNeeded()
        SoundManager.playTap()
        onDismiss()
    }

    BackHandler(enabled = !isWatchingAd) {
        handleClose()
    }

    fun launchCoinFlight(rewardAmount: Int, originInRoot: Offset) {
        if (isCollecting || rewardAmount <= 0) return
        pendingReward = rewardAmount
        rewardCommitted = false
        isCelebratingBurst = true
        SoundManager.playBrickClear()

        val localStart = when {
            originInRoot != Offset.Zero -> originInRoot - modalOriginInRoot
            claimButtonCenterInRoot != Offset.Zero -> claimButtonCenterInRoot - modalOriginInRoot
            else -> Offset(500f, 1300f)
        }

        val count = 15
        val burstRadiusMinPx = with(density) { 26.dp.toPx() }
        val burstRadiusSpanPx = with(density) { 36.dp.toPx() }

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
                    startDelayMs = i * 38L,
                    currentX = localStart.x,
                    currentY = localStart.y
                )
            )
        }
        isCollecting = true
    }

    // 60FPS Coin Flight Animation Loop -> Bezier arc into top-left Coin Rack
    LaunchedEffect(isCollecting) {
        if (!isCollecting || flyingCoins.isEmpty()) return@LaunchedEffect

        var startNanos = -1L
        val t1 = 240f
        val t2 = 600f
        val totalCount = flyingCoins.size
        val reward = pendingReward.coerceAtLeast(1)
        val perCoin = reward / totalCount
        val remainder = reward - (perCoin * totalCount)
        var arrivedCount = 0

        while (arrivedCount < totalCount) {
            withFrameNanos { nanos ->
                if (startNanos < 0L) startNanos = nanos
                val elapsedOverall = (nanos - startNanos) / 1_000_000L
                val targetLocal = if (rackTargetInRoot != Offset.Zero) {
                    rackTargetInRoot - modalOriginInRoot
                } else {
                    Offset(120f, 90f)
                }

                for (coin in flyingCoins) {
                    if (coin.arrived) continue

                    val coinElapsed = elapsedOverall - coin.startDelayMs
                    if (coinElapsed < 0) {
                        coin.alpha = 0f
                        continue
                    }

                    coin.alpha = 1f
                    coin.frameIdx = (((elapsedOverall / 65L) + coin.id) % 8).toInt()

                    if (coinElapsed < t1) {
                        val p = (coinElapsed / t1).coerceIn(0f, 1f)
                        val easeOut = 1f - (1f - p) * (1f - p) * (1f - p)
                        coin.currentX = coin.startX + (coin.burstX - coin.startX) * easeOut
                        coin.currentY = coin.startY + (coin.burstY - coin.startY) * easeOut
                        coin.scale = 0.65f + 0.50f * easeOut
                    } else {
                        val p = ((coinElapsed - t1) / t2).coerceIn(0f, 1f)
                        val e = p * p * (3f - 2f * p)
                        val arcDipPx = with(density) { 44.dp.toPx() }
                        val ctrlX = (coin.burstX + targetLocal.x) * 0.5f + arcDipPx * 0.45f
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
                                commitPendingRewardIfNeeded()
                            }
                        }
                    }
                }
                coinAnimTick = nanos
            }
        }

        delay(120L)
        isCollecting = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("jackpot_tower_modal")
            .onGloballyPositioned { coords ->
                modalOriginInRoot = coords.positionInRoot()
            }
            .background(Color.Black.copy(alpha = 0.84f))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    }
                }
            }
    ) {
        // 1. Main Jackpot Machine (Large, Crisp & Rock-Steady) + Bottom Claim Button
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 64.dp, bottom = 22.dp, start = 16.dp, end = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            val maxMachineW = min(maxWidth * 0.86f, 326.dp)
            val maxMachineFromH = (maxHeight - 108.dp).coerceAtLeast(220.dp) * 0.67f
            val machineWidth = min(maxMachineW, maxMachineFromH)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = modalScale.value
                        scaleY = modalScale.value
                        alpha = modalAlpha.value
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                JackpotTowerMachine(
                    activeTierIndex = activeTierIndex,
                    allClaimed = allClaimed,
                    isCelebratingBurst = isCelebratingBurst,
                    hudCoinBmp = hudCoinBmp,
                    coinFrames = coinFrames,
                    modifier = Modifier.width(machineWidth)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom Action Button:
                // - All claimed: Green Pill Button showing live 24h countdown timer matching Home button
                // - Step 0: Green Pill Button ("Free") for 50 coins
                // - Steps 1..4: Pink Watch Ad ("Free Coins") button for 100..800 coins
                if (allClaimed) {
                    val cooldownText = remember(remainingMs) {
                        JackpotEventManager.formatCountdownTimer(remainingMs)
                    }
                    GreenPlayPillButton(
                        text = cooldownText,
                        contentDescription = "Next Jackpot in $cooldownText",
                        testTag = "jackpot_cooldown_button",
                        modifier = Modifier
                            .fillMaxWidth(0.68f)
                            .widthIn(max = 250.dp),
                        onClick = {
                            SoundManager.playBlocked()
                        }
                    )
                } else if (isFreeStep) {
                    Box(
                        modifier = Modifier
                            .onGloballyPositioned { coords ->
                                val pos = coords.positionInRoot()
                                claimButtonCenterInRoot = Offset(
                                    pos.x + coords.size.width / 2f,
                                    pos.y + coords.size.height / 2f
                                )
                            }
                    ) {
                        GreenPlayPillButton(
                            text = "Free",
                            contentDescription = "Claim 50 Free Coins",
                            testTag = "jackpot_claim_free_button",
                            modifier = Modifier
                                .fillMaxWidth(0.68f)
                                .widthIn(max = 250.dp),
                            onClick = {
                                if (!isCollecting && !isWatchingAd) {
                                    launchCoinFlight(currentReward, claimButtonCenterInRoot)
                                }
                            }
                        )
                    }
                } else {
                    val adPress = rememberBouncyPressState(pressedScale = 0.89f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.68f)
                            .widthIn(max = 250.dp)
                            .aspectRatio(688f / 259f)
                            .minimumInteractiveComponentSize()
                            .onGloballyPositioned { coords ->
                                val pos = coords.positionInRoot()
                                claimButtonCenterInRoot = Offset(
                                    pos.x + coords.size.width / 2f,
                                    pos.y + coords.size.height / 2f
                                )
                            }
                            .testTag("jackpot_watch_ad_button")
                            .bouncyClickTarget(
                                state = adPress,
                                enabled = !isCollecting && !isWatchingAd,
                                role = Role.Button
                            ) {
                                SoundManager.playTap()
                                isWatchingAd = true
                            }
                            .bouncyScale(adPress),
                        contentAlignment = Alignment.Center
                    ) {
                        if (watchAdBtnBmp != null) {
                            Image(
                                bitmap = watchAdBtnBmp.asImageBitmap(),
                                contentDescription = "Watch Ad for $currentReward Free Coins",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit,
                                filterQuality = FilterQuality.High
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(32.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color(0xFFFF4FD8), Color(0xFFC014A5))
                                        )
                                    )
                                    .border(3.dp, Color(0xFFFFD700), RoundedCornerShape(32.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Free Coins",
                                    fontFamily = FredokaFontFamily,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Top Header Bar: Coins Rack (Top-Left) & Cross Button (Top-Right), placed above body in z-order
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            UniversalCoinRack(
                bitmap = coinRackBitmap,
                coins = displayedCoins,
                compact = false,
                showPlusButton = false,
                modifier = Modifier
                    .onGloballyPositioned { coords ->
                        val pos = coords.positionInRoot()
                        rackTargetInRoot = Offset(
                            pos.x + coords.size.width * 0.17f,
                            pos.y + coords.size.height * 0.54f
                        )
                    }
                    .scale(rackScale.value)
                    .testTag("jackpot_coin_rack")
            )

            val closePress = rememberBouncyPressState(pressedScale = 0.86f)
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("jackpot_close_button")
                    .bouncyClickTarget(
                        state = closePress,
                        enabled = !isWatchingAd,
                        role = Role.Button
                    ) {
                        handleClose()
                    }
                    .bouncyScale(closePress),
                contentAlignment = Alignment.Center
            ) {
                if (closeXBmp != null) {
                    Image(
                        bitmap = closeXBmp.asImageBitmap(),
                        contentDescription = "Close Jackpot Tower",
                        modifier = Modifier.size(42.dp),
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

        // 3. 60FPS CANVAS OVERLAY FOR 15 SPINNING COINS FLYING INTO TOP-LEFT COIN RACK
        if (isCollecting && coinFrames.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
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

        // 4. Rewarded Ad Progress Overlay for Steps 1..4 (100, 200, 400, 800 coins)
        if (isWatchingAd) {
            var adProgress by remember { mutableIntStateOf(0) }
            LaunchedEffect(Unit) {
                val totalMs = 1500L
                val stepMs = 50L
                var elapsed = 0L
                while (elapsed < totalMs) {
                    delay(stepMs)
                    elapsed += stepMs
                    adProgress = ((elapsed * 100L) / totalMs).toInt().coerceIn(0, 100)
                }
                isWatchingAd = false
                launchCoinFlight(currentReward, claimButtonCenterInRoot)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.72f))
                    .testTag("jackpot_watching_ad_overlay"),
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
                        text = "Claiming $currentReward Free Coins...",
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { adProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(50)),
                        color = Color(0xFFE935C8),
                        trackColor = Color(0xFF1E3A8A)
                    )
                }
            }
        }
    }
}
