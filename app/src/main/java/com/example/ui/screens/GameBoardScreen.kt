package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import com.example.ui.components.BucketFluffyRenderer
import com.example.ui.components.LivesHudPill
import com.example.ui.components.UniversalCoinRack
import com.example.ui.components.bouncyClickTarget
import com.example.ui.components.bouncyScale
import com.example.ui.components.rememberBouncyPressState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.game.assets.AssetLoader
import com.example.game.belt.BeltPath
import com.example.game.constants.GameConstants
import com.example.game.levels.LevelGenerator
import com.example.game.lives.LifeSnapshot
import com.example.game.lives.LivesManager
import com.example.game.model.BeltBall
import com.example.game.model.Brick
import com.example.game.model.BrickColumn
import com.example.game.model.CannonCollectedBall
import com.example.game.model.DispenserDirection
import com.example.game.model.CannonProjectile
import com.example.game.model.CannonSmokePuff
import com.example.game.model.CellKind
import com.example.game.model.DroppingBall
import com.example.game.model.FluffyVanishEffect
import com.example.game.model.GridCell
import com.example.game.model.MagnetReturningBall
import com.example.game.model.MarbleColor
import com.example.game.model.Particle
import com.example.game.model.ParticleShape
import com.example.game.physics.FunnelPhysics
import com.example.game.rules.GameRules
import com.example.game.sound.SoundManager
import com.example.ui.components.CLEAN_UP_COIN_COST
import com.example.ui.components.ChainFullCleanUpOverlay
import com.example.ui.components.LevelCompleteModal
import com.example.ui.components.LevelFailedModal
import com.example.ui.components.LoseLifeWarningModal
import com.example.ui.components.LoseLifeWarningMode
import com.example.ui.components.OutOfLivesModal
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun GameBoardScreen(
    levelNumber: Int,
    coins: Int,
    lifeSnapshot: LifeSnapshot = LifeSnapshot(LivesManager.MAX_LIVES, 0L, 0L),
    onReturnHome: () -> Unit,
    onLoseLife: () -> Unit = {},
    onGainOneLife: () -> Unit = {},
    onWatchAdForLife: () -> Unit = {},
    onRefillLivesWithCoins: () -> Unit = {},
    onLevelWon: () -> Unit = {},
    onAddCoins: (Int) -> Unit,
    onLevelAdvanced: (Int) -> Unit
) {
    val context = LocalContext.current

    var currentLevel by remember { mutableIntStateOf(levelNumber) }
    LaunchedEffect(levelNumber) {
        if (currentLevel != levelNumber) {
            currentLevel = levelNumber
        }
    }
    var levelResetNonce by remember { mutableIntStateOf(0) }

    // Cached Bitmaps & Typeface across levels/restarts
    val bgBitmap = remember { AssetLoader.loadBitmap(context, "radiant_cyan_blue_gradient.png") }
    val cyberFrameBitmap = remember { AssetLoader.loadBitmap(context, "cyber_game_frame.png") }
    val coinRackBitmap = remember { AssetLoader.loadBitmap(context, "coin_rack.png") }
    val pauseNormalBitmap = remember { AssetLoader.loadBitmap(context, "pause_normal.png") }
    val handCursorBitmap = remember { AssetLoader.loadBitmap(context, "hand_cursor_trimmed.png") }
    val magnetBtnFrames = remember { (0..5).mapNotNull { AssetLoader.getMagnetBtnFrame(context, it) } }
    val shuffleBtnFrames = remember { (0..4).mapNotNull { AssetLoader.getShuffleBtnFrame(context, it) } }
    val cannonBtnFrames = remember { (0..4).mapNotNull { AssetLoader.getCannonBtnFrame(context, it) } }
    val magnetFxFrames = remember { (0..4).mapNotNull { AssetLoader.getMagnetFxFrame(context, it) } }
    val shuffleFxFrames = remember { (0..4).mapNotNull { AssetLoader.getShuffleFxFrame(context, it) } }
    val cannonIdleBitmap = remember { AssetLoader.getCannonIdleFrame(context) }
    val cannonChargeFrames = remember { (0..4).mapNotNull { AssetLoader.getCannonChargeFrame(context, it) } }
    val cannonFireFrames = remember { (0..1).mapNotNull { AssetLoader.getCannonFireFrame(context, it) } }
    val cannonSmokeFrames = remember { (0..3).mapNotNull { AssetLoader.getCannonSmokeFrame(context, it) } }
    val cannonFxSmoke = remember { AssetLoader.loadBitmap(context, "powerups/cannon_fx_smoke.png") }
    val cannonFxFlash = remember { AssetLoader.loadBitmap(context, "powerups/cannon_fx_flash.png") }
    val cannonFxRing = remember { AssetLoader.loadBitmap(context, "powerups/cannon_fx_ring.png") }
    val cannonFxSpark = remember { AssetLoader.loadBitmap(context, "powerups/cannon_fx_spark.png") }

    // Font Paint for high performance canvas rendering
    val fredokaTypeface = remember {
        try {
            ResourcesCompat.getFont(context, R.font.fredoka_one)
                ?: ResourcesCompat.getFont(context, R.font.fredoka_bold)
        } catch (_: Exception) {
            null
        }
    }

    val textPaint = remember(fredokaTypeface) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            fredokaTypeface?.let { typeface = it }
            color = android.graphics.Color.WHITE
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
    }

    key(currentLevel, levelResetNonce) {
        var score by remember { mutableIntStateOf(0) }
        var currentCoins by remember(coins) { mutableIntStateOf(coins) }
        var effectiveLifeSnapshot by remember(lifeSnapshot) { mutableStateOf(lifeSnapshot) }
        var gameState by remember { mutableStateOf("playing") }
        var isPaused by remember { mutableStateOf(false) }
        var loseLifeWarningMode by remember { mutableStateOf<LoseLifeWarningMode?>(null) }
        var showLevelFailedModal by remember { mutableStateOf(false) }
        var levelFailedBeforeLives by remember { mutableIntStateOf(effectiveLifeSnapshot.lives) }
        var showTryAgainOutOfLivesModal by remember { mutableStateOf(false) }
        var soundMuted by remember { mutableStateOf(!SoundManager.enabled) }

        var magnetCount by remember { mutableIntStateOf(3) }
        var shuffleCount by remember { mutableIntStateOf(3) }
        var cannonCount by remember { mutableIntStateOf(3) }
        var isMagnetActive by remember { mutableStateOf(false) }
        var isShuffling by remember { mutableStateOf(false) }
        var isCannonActive by remember { mutableStateOf(false) }
        var isTutorialDismissed by remember { mutableStateOf(false) }
        var isFunnelLocked by remember { mutableStateOf(false) }
        var funnelFullAlertTimer by remember { mutableFloatStateOf(0f) }
        val beltCapacity = GameConstants.beltCapacityForLevel(currentLevel)
        val maxWaitingBalls = GameConstants.maxWaitingBallsForLevel(currentLevel)
        val resumeWaitingBalls = GameConstants.resumeWaitingBallsForLevel(currentLevel)

        // Screen scale parameters in PIXELS
        var viewScale by remember { mutableFloatStateOf(1f) }
        var viewOffsetX by remember { mutableFloatStateOf(0f) }
        var viewOffsetY by remember { mutableFloatStateOf(0f) }
        var viewArenaOffsetY by remember { mutableFloatStateOf(0f) }
        val crateColNormX = remember { floatArrayOf(0.258f, 0.500f, 0.742f) }
        val crateRowNormY = remember { floatArrayOf(0.268f, 0.488f, 0.708f) }
        val crateSlotMoods = remember {
            arrayOf(
                "happy_open", "content", "wink",
                "curious", "happy_open", "eyes_closed",
                "wink", "surprised", "content"
            )
        }

        // Game state models - generated cleanly per keyed board session
        val levelData = remember {
            LevelGenerator.generateSolvableLevel(currentLevel)
        }

        val grid = remember {
            levelData.grid
        }

        val columns = remember {
            levelData.columns.mapIndexed { idx, colColors ->
                BrickColumn(
                    x = GameConstants.BRICK_COLUMNS_X[idx],
                    bricks = colColors.mapIndexed { bIdx, color ->
                        Brick(
                            color = color,
                            filled = 0,
                            inFlight = 0,
                            animState = if (bIdx == 0) "activate" else "idle"
                        )
                    }.toMutableList()
                )
            }.toMutableList()
        }

        val physics = remember { FunnelPhysics() }
        val beltBalls = remember { mutableStateListOf<BeltBall>() }
        val droppingBalls = remember { mutableStateListOf<DroppingBall>() }
        val particles = remember { mutableStateListOf<Particle>() }
        val fluffyEffects = remember { mutableStateListOf<FluffyVanishEffect>() }
        val magnetReturningBalls = remember { mutableStateListOf<MagnetReturningBall>() }
        val cannonCollectedBalls = remember { mutableStateListOf<CannonCollectedBall>() }
        val cannonProjectiles = remember { mutableStateListOf<CannonProjectile>() }
        val cannonSmokePuffs = remember { mutableStateListOf<CannonSmokePuff>() }
        var magnetPulseTimer by remember { mutableFloatStateOf(0f) }
        var magnetBtnAnimTimer by remember { mutableFloatStateOf(0f) }
        var shuffleBtnAnimTimer by remember { mutableFloatStateOf(0f) }
        var cannonBtnAnimTimer by remember { mutableFloatStateOf(0f) }
        var magnetBtnShakeTimer by remember { mutableFloatStateOf(0f) }
        var shuffleBtnShakeTimer by remember { mutableFloatStateOf(0f) }
        var cannonBtnShakeTimer by remember { mutableFloatStateOf(0f) }
        var cannonTimer by remember { mutableFloatStateOf(0f) }
        var cannonHasFired by remember { mutableStateOf(false) }
        var cannonAimAngleDeg by remember { mutableFloatStateOf(16f) }
        val cannonTotalDuration = 3.15f
        val cannonFireMoment = 1.72f
        val powerupBtnAnimDuration = 0.95f
        var activePowerupOverlayType by remember { mutableStateOf<String?>(null) } // "magnet" or "shuffle"
        var powerupOverlayTimer by remember { mutableFloatStateOf(0f) }
        val powerupOverlayDuration = 1.05f
        var nextDropId by remember { mutableIntStateOf(5000) }
        var beltScrollDistance by remember { mutableFloatStateOf(0f) }
        var currentBeltSpeed by remember { mutableFloatStateOf(GameConstants.BELT_SPEED) }
        var chainStuckTimer by remember { mutableFloatStateOf(0f) }
        var cleanUpHighlightedIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

        fun executeCleanUp() {
            val selectedBelt = beltBalls.filter { cleanUpHighlightedIds.contains(it.id) }.ifEmpty {
                GameRules.selectCleanUpBeltBalls(beltBalls.toList(), 8)
            }
            val cleanUpItems = buildList {
                for (fb in physics.balls) {
                    add(
                        GameRules.CleanUpReturnItem(
                            id = fb.id,
                            color = fb.color,
                            startX = fb.x,
                            startY = fb.y,
                            isFromFunnel = true
                        )
                    )
                }
                for (bb in selectedBelt) {
                    val coords = BeltPath.getBeltCoordinates(bb.s)
                    add(
                        GameRules.CleanUpReturnItem(
                            id = bb.id,
                            color = bb.color,
                            startX = coords.x,
                            startY = coords.y,
                            isFromFunnel = false
                        )
                    )
                }
            }

            val assignments = GameRules.planCleanUpReturn(grid, cleanUpItems)
            if (assignments.isNotEmpty()) {
                isMagnetActive = true
                magnetPulseTimer = 1.15f
                activePowerupOverlayType = "magnet"
                powerupOverlayTimer = powerupOverlayDuration
                isTutorialDismissed = true
                SoundManager.playPowerup()

                for (sp in 0 until 16) {
                    val angle = Random.nextFloat() * 2f * PI.toFloat()
                    val spd = 45f + Random.nextFloat() * 85f
                    particles.add(
                        Particle(
                            x = 148f + cos(angle) * 14f,
                            y = 342f + sin(angle) * 14f,
                            vx = cos(angle) * spd,
                            vy = sin(angle) * spd,
                            color = if (sp % 2 == 0) 0xFF38E8FF else 0xFFFFE15A,
                            size = 2.6f + Random.nextFloat() * 2.4f,
                            maxLife = 0.55f
                        )
                    )
                }

                val staggerStep = if (assignments.size > 1) {
                    minOf(0.026f, 0.50f / assignments.size)
                } else {
                    0f
                }
                val returnedFunnelIds = mutableSetOf<Int>()
                val returnedBeltIds = mutableSetOf<Int>()

                assignments.forEachIndexed { idx, assigned ->
                    if (assigned.item.isFromFunnel) {
                        returnedFunnelIds.add(assigned.item.id)
                    } else {
                        returnedBeltIds.add(assigned.item.id)
                    }

                    val cx = GameConstants.GRID_X_CENTERS[assigned.targetCol]
                    val cy = GameConstants.GRID_Y_CENTERS[assigned.targetRow]
                    val slotRow = assigned.targetSlotIndex / 3
                    val slotCol = assigned.targetSlotIndex % 3
                    val targetX = cx + (crateColNormX[slotCol] - 0.5f) * 34f
                    val targetY = cy + (crateRowNormY[slotRow] - 0.5f) * 33f

                    val startX = assigned.item.startX
                    val startY = assigned.item.startY
                    val midX = (startX + targetX) * 0.5f
                    val arcSide = if (startX < targetX) -22f else 22f
                    val controlX = (midX + arcSide).coerceIn(22f, 274f)
                    val controlY = (startY * 0.32f + targetY * 0.68f) - 20f

                    magnetReturningBalls.add(
                        MagnetReturningBall(
                            id = assigned.item.id,
                            color = assigned.item.color,
                            startX = startX,
                            startY = startY,
                            controlX = controlX,
                            controlY = controlY,
                            targetX = targetX,
                            targetY = targetY,
                            targetRow = assigned.targetRow,
                            targetCol = assigned.targetCol,
                            targetSlotIndex = assigned.targetSlotIndex,
                            elapsed = 0f,
                            delay = idx * staggerStep,
                            duration = 0.46f
                        )
                    )
                }

                if (returnedFunnelIds.isNotEmpty()) {
                    physics.balls.removeAll { returnedFunnelIds.contains(it.id) }
                }
                if (returnedBeltIds.isNotEmpty()) {
                    beltBalls.removeAll { returnedBeltIds.contains(it.id) }
                }
            }

            chainStuckTimer = 0f
            cleanUpHighlightedIds = emptySet()
            isFunnelLocked = GameRules.shouldBlockBlockTap(
                waitingBallsCount = physics.balls.size,
                currentlyLocked = false,
                maxWaitingBalls = maxWaitingBalls,
                resumeWaitingBalls = resumeWaitingBalls
            )
            funnelFullAlertTimer = 0f
            gameState = "playing"
        }

    // Reset on level change or restart
    LaunchedEffect(currentLevel, levelResetNonce) {
        isTutorialDismissed = false
        isFunnelLocked = false
        funnelFullAlertTimer = 0f
        isMagnetActive = false
        isShuffling = false
        isCannonActive = false
        magnetPulseTimer = 0f
        magnetBtnAnimTimer = 0f
        shuffleBtnAnimTimer = 0f
        cannonBtnAnimTimer = 0f
        magnetBtnShakeTimer = 0f
        shuffleBtnShakeTimer = 0f
        cannonBtnShakeTimer = 0f
        cannonTimer = 0f
        cannonHasFired = false
        cannonAimAngleDeg = 16f
        activePowerupOverlayType = null
        powerupOverlayTimer = 0f
        chainStuckTimer = 0f
        cleanUpHighlightedIds = emptySet()
        gameState = "playing"
        isPaused = false
        physics.balls.clear()
        beltBalls.clear()
        droppingBalls.clear()
        particles.clear()
        fluffyEffects.clear()
        magnetReturningBalls.clear()
        cannonCollectedBalls.clear()
        cannonProjectiles.clear()
        cannonSmokePuffs.clear()
        beltScrollDistance = 0f
        currentBeltSpeed = GameConstants.BELT_SPEED
    }

    // 60FPS Game Loop
    LaunchedEffect(isPaused, gameState, currentLevel, levelResetNonce) {
        var lastTime = System.nanoTime()

        while (true) {
            withFrameNanos { frameTimeNanos ->
                val dt = min((frameTimeNanos - lastTime) / 1_000_000_000f, 0.05f)
                lastTime = frameTimeNanos

                if (!isPaused && gameState == "playing") {
                    // 1. Advance conveyor belt (speeds up automatically once upper buckets are all cleared)
                    val desiredBeltSpeed = GameRules.targetBeltSpeed(
                        grid = grid,
                        hasReturningBalls = magnetReturningBalls.isNotEmpty()
                    )
                    val speedBlend = (dt * 6f).coerceIn(0f, 1f)
                    currentBeltSpeed += (desiredBeltSpeed - currentBeltSpeed) * speedBlend
                    if (kotlin.math.abs(desiredBeltSpeed - currentBeltSpeed) < 0.5f) {
                        currentBeltSpeed = desiredBeltSpeed
                    }
                    val isBeltBoosted = currentBeltSpeed > GameConstants.BELT_SPEED * 1.15f

                    beltScrollDistance = (beltScrollDistance + currentBeltSpeed * dt) % BeltPath.TOTAL_PATH_LEN
                    val slotSpacing = BeltPath.TOTAL_PATH_LEN / beltCapacity

                    // Check feed point eligibility
                    var feedSlotIndex = -1
                    var minFeedDist = Float.MAX_VALUE

                    for (k in 0 until beltCapacity) {
                        val slotS = (k * slotSpacing + beltScrollDistance) % BeltPath.TOTAL_PATH_LEN
                        val dist = BeltPath.circularDistance(slotS, BeltPath.FEED_POINT_S)
                        if (dist < minFeedDist) {
                            minFeedDist = dist
                            feedSlotIndex = k
                        }
                    }

                    val feedWindow = maxOf(
                        4.5f,
                        minOf(slotSpacing * 0.50f, currentBeltSpeed * dt * 0.65f + 2.0f)
                    )
                    val occupiedSlots = beltBalls.map { it.slotIndex }.toSet()
                    val canFeed = !isCannonActive &&
                            feedSlotIndex != -1 &&
                            minFeedDist <= feedWindow &&
                            !occupiedSlots.contains(feedSlotIndex) &&
                            beltBalls.size < beltCapacity

                    val fedBall = physics.update(dt, canFeed)
                    if (fedBall != null && feedSlotIndex != -1) {
                        SoundManager.playBallClink()
                        val targetS = (feedSlotIndex * slotSpacing + beltScrollDistance) % BeltPath.TOTAL_PATH_LEN
                        beltBalls.add(
                            BeltBall(
                                id = fedBall.id,
                                color = fedBall.color,
                                slotIndex = feedSlotIndex,
                                s = targetS,
                                scale = 1.15f,
                                mood = "content",
                                animTimer = 0f
                            )
                        )
                    }

                    // Keep funnel lock synced: 24+ on levels 1-15, 30+ after level 15
                    isFunnelLocked = GameRules.shouldBlockBlockTap(
                        waitingBallsCount = physics.balls.size,
                        currentlyLocked = isFunnelLocked,
                        maxWaitingBalls = maxWaitingBalls,
                        resumeWaitingBalls = resumeWaitingBalls
                    )
                    if (funnelFullAlertTimer > 0f) {
                        funnelFullAlertTimer = (funnelFullAlertTimer - dt).coerceAtLeast(0f)
                    }

                    // Advance each belt ball & update emotions (blinking, winking, content)
                    for (ball in beltBalls) {
                        ball.s = (ball.slotIndex * slotSpacing + beltScrollDistance) % BeltPath.TOTAL_PATH_LEN
                        if (ball.scale > 1f) {
                            ball.scale = (ball.scale - dt * 2.5f).coerceAtLeast(1f)
                        }

                        ball.animTimer += dt
                        // Blink or wink every 2-3 seconds
                        val cycle = (ball.animTimer + ball.id * 0.35f) % 3.0f
                        ball.mood = when {
                            cycle < 0.22f -> "eyes_closed"
                            cycle in 0.22f..0.38f && ball.id % 2 == 0 -> "wink"
                            cycle in 1.4f..1.6f -> "happy_open"
                            else -> "content"
                        }
                    }

                    // Update funnel balls mood (waiting -> angry, falling -> surprised/happy)
                    for (fb in physics.balls) {
                        fb.waitTime += dt
                        fb.animTimer += dt
                        val speedSq = fb.vx * fb.vx + fb.vy * fb.vy
                        if (speedSq > 800f) {
                            fb.mood = if (fb.vy > 0) "surprised" else "happy_open"
                        } else if (fb.waitTime > 3.5f) {
                            // Waiting in funnel for long -> gets angry!
                            val angryCycle = (fb.animTimer * 2f).toInt() % 2
                            fb.mood = if (angryCycle == 0) "angry_1" else "angry_2"
                        } else if (fb.waitTime > 1.8f) {
                            fb.mood = "curious"
                        } else {
                            val blinkCycle = (fb.animTimer + fb.id * 0.4f) % 2.5f
                            fb.mood = if (blinkCycle < 0.2f) "eyes_closed" else "content"
                        }
                    }

                    // Matching on bottom lane
                    val matchedBallIds = mutableSetOf<Int>()
                    val nowMs = System.currentTimeMillis()

                    val matchDropIntervalMs = if (isBeltBoosted) {
                        GameConstants.MIN_DROP_INTERVAL_FAST_MS
                    } else {
                        GameConstants.MIN_DROP_INTERVAL_MS
                    }
                    val dropFlightDuration = if (isBeltBoosted) 0.13f else 0.18f

                    for (ball in beltBalls) {
                        if (BeltPath.isSOnBottomLane(ball.s)) {
                            val coords = BeltPath.getBeltCoordinates(ball.s)
                            val match = GameRules.tryMatchBeltBall(
                                ballX = coords.x,
                                ballColor = ball.color,
                                columns = columns,
                                nowMs = nowMs,
                                minDropIntervalMs = matchDropIntervalMs
                            )
                            if (match != null) {
                                matchedBallIds.add(ball.id)
                                val col = columns[match.matchedColIndex]
                                col.lightPulseTimer = maxOf(col.lightPulseTimer, 0.45f)

                                droppingBalls.add(
                                    DroppingBall(
                                        id = nextDropId++,
                                        color = ball.color,
                                        startX = coords.x,
                                        startY = coords.y,
                                        targetX = col.x,
                                        targetY = GameConstants.BALTI_SOCKET_Y,
                                        progress = 0f,
                                        duration = dropFlightDuration,
                                        colIndex = match.matchedColIndex,
                                        holeIndex = match.targetHoleIndex,
                                        mood = "happy_open",
                                        targetBrick = col.bricks.firstOrNull()
                                    )
                                )
                            }
                        }
                    }

                    if (matchedBallIds.isNotEmpty()) {
                        beltBalls.removeAll { matchedBallIds.contains(it.id) }
                    }

                    // Update dropping balls
                    val dropsToRemove = mutableListOf<DroppingBall>()
                    for (drop in droppingBalls) {
                        drop.progress += dt / drop.duration
                        if (drop.progress >= 1f) {
                            dropsToRemove.add(drop)
                            val col = columns[drop.colIndex]
                            val targetBrick = drop.targetBrick?.takeIf { col.bricks.contains(it) }
                                ?: col.bricks.firstOrNull { it.color == drop.color && it.filled < GameConstants.BRICK_HOLES }
                                ?: col.bricks.firstOrNull()
                            if (targetBrick != null) {
                                targetBrick.inFlight = maxOf(0, targetBrick.inFlight - 1)
                                targetBrick.filled++
                                col.lightPulseTimer = 0.65f

                                when (targetBrick.filled) {
                                    1 -> {
                                        targetBrick.animState = "hit_1"
                                        targetBrick.animTimer = 0f
                                        SoundManager.playDropHole(0)
                                    }
                                    2 -> {
                                        targetBrick.animState = "hit_2"
                                        targetBrick.animTimer = 0f
                                        SoundManager.playDropHole(1)
                                    }
                                    else -> {
                                        targetBrick.animState = "hit_3"
                                        targetBrick.animTimer = 0f
                                        SoundManager.playDropHole(2)
                                    }
                                }
                            }
                        }
                    }
                    droppingBalls.removeAll(dropsToRemove)

                    // Brick animations and completion (supports both front buckets and behind-buckets hit by Cannon!)
                    for (col in columns) {
                        val bricksToRemove = mutableListOf<Brick>()
                        for (bIdx in col.bricks.indices) {
                            val brick = col.bricks[bIdx]
                            if (brick.filled >= GameConstants.BRICK_HOLES &&
                                brick.animState != "hit_3" &&
                                brick.animState != "hold_full" &&
                                brick.animState != "vanish"
                            ) {
                                brick.animState = "hit_3"
                                brick.animTimer = 0f
                            }
                            when (brick.animState) {
                                "activate", "hit_1", "hit_2" -> {
                                    brick.animTimer += dt
                                    if (brick.animTimer >= 0.16f) {
                                        brick.animState = "idle"
                                        brick.animTimer = 0f
                                    }
                                }
                                "hit_3" -> {
                                    brick.animTimer += dt
                                    if (brick.animTimer >= 0.18f) {
                                        brick.animState = "hold_full"
                                        brick.animTimer = 0f
                                    }
                                }
                                "hold_full" -> {
                                    brick.animTimer += dt
                                    if (brick.animTimer >= 0.16f) {
                                        brick.animState = "vanish"
                                        brick.animTimer = 0f
                                        SoundManager.playBrickClear()
                                        score += 300

                                        val visualY = GameConstants.BALTI_SOCKET_Y - minOf(bIdx, 2) * 8f

                                        // Particles explosion
                                        for (p in 0 until 14) {
                                            particles.add(
                                                Particle(
                                                    x = col.x + (Random.nextFloat() - 0.5f) * 36f,
                                                    y = visualY + (Random.nextFloat() - 0.5f) * 16f,
                                                    vx = (Random.nextFloat() - 0.5f) * 120f,
                                                    vy = -50f - Random.nextFloat() * 70f,
                                                    color = 0xFFFFE494,
                                                    size = 3f + Random.nextFloat() * 4f,
                                                    shape = if (Random.nextBoolean()) ParticleShape.STAR else ParticleShape.SPARKLE
                                                )
                                            )
                                        }

                                        // 3 Fluffies jump out of the full bucket joyfully into the air!
                                        val holeOffsets = floatArrayOf(-10f, 0f, 10f)
                                        val vxs = floatArrayOf(-35f, 0f, 35f)
                                        for (h in 0 until 3) {
                                            fluffyEffects.add(
                                                FluffyVanishEffect(
                                                    id = nextDropId++,
                                                    color = brick.color,
                                                    startX = col.x + holeOffsets[h],
                                                    startY = visualY - 4f,
                                                    vx = vxs[h],
                                                    vy = -160f - Random.nextFloat() * 40f,
                                                    x = col.x + holeOffsets[h],
                                                    y = visualY - 4f
                                                )
                                            )
                                        }
                                        bricksToRemove.add(brick)
                                    }
                                }
                            }
                        }
                        if (bricksToRemove.isNotEmpty()) {
                            val frontRemoved = col.bricks.firstOrNull() in bricksToRemove
                            col.bricks.removeAll(bricksToRemove)
                            if (frontRemoved && col.bricks.isNotEmpty()) {
                                val nextFront = col.bricks[0]
                                if (nextFront.filled < GameConstants.BRICK_HOLES && nextFront.animState == "idle") {
                                    nextFront.animState = "activate"
                                    nextFront.animTimer = 0f
                                }
                            }
                            if (GameRules.isLevelWon(columns)) {
                                gameState = "won"
                            }
                        }
                    }

                    if (gameState == "playing" &&
                        GameRules.isLevelWon(columns) &&
                        droppingBalls.isEmpty() &&
                        cannonProjectiles.isEmpty()
                    ) {
                        gameState = "won"
                    }

                    // Update flying fluffy celebration effects when bucket clears
                    val effectsToRemove = mutableListOf<FluffyVanishEffect>()
                    for (eff in fluffyEffects) {
                        eff.progress += dt / eff.duration
                        if (eff.progress >= 1f) {
                            effectsToRemove.add(eff)
                        } else {
                            val p = eff.progress
                            eff.x = eff.startX + eff.vx * p
                            // Arc trajectory upwards then gently down
                            eff.y = eff.startY + eff.vy * p + 180f * p * p
                            eff.scale = 1.0f + 0.35f * sin(p * PI.toFloat())
                            eff.alpha = (1f - p * 0.7f).coerceIn(0f, 1f)
                        }
                    }
                    fluffyEffects.removeAll(effectsToRemove)

                    // Update particles
                    val particlesToRemove = mutableListOf<Particle>()
                    for (p in particles) {
                        p.life += dt
                        if (p.life >= p.maxLife) {
                            particlesToRemove.add(p)
                        } else {
                            p.x += p.vx * dt
                            p.y += p.vy * dt
                            p.alpha = 1f - (p.life / p.maxLife)
                        }
                    }
                    particles.removeAll(particlesToRemove)

                    // Update Magnet returning balls (chain -> upper container pocket)
                    if (magnetPulseTimer > 0f) {
                        magnetPulseTimer = (magnetPulseTimer - dt).coerceAtLeast(0f)
                    }
                    if (magnetReturningBalls.isNotEmpty()) {
                        for (mb in magnetReturningBalls) {
                            mb.elapsed += dt
                            if (!mb.landed && mb.elapsed >= mb.delay + mb.duration) {
                                mb.landed = true
                                val targetCell = grid[mb.targetRow][mb.targetCol]
                                targetCell.ballCount = (targetCell.ballCount + 1).coerceAtMost(GameConstants.BALLS_PER_BLOCK)
                                targetCell.scaleX = 1.16f
                                targetCell.scaleY = 0.88f
                                SoundManager.playBallClink()
                                for (sp in 0 until 2) {
                                    particles.add(
                                        Particle(
                                            x = mb.targetX,
                                            y = mb.targetY,
                                            vx = (Random.nextFloat() - 0.5f) * 44f,
                                            vy = -18f - Random.nextFloat() * 28f,
                                            color = 0xFF7CE7FF,
                                            size = 2.4f,
                                            maxLife = 0.35f
                                        )
                                    )
                                }
                            }
                        }
                        magnetReturningBalls.removeAll { it.landed }
                    }
                    isMagnetActive = magnetReturningBalls.isNotEmpty()

                    if (magnetBtnAnimTimer > 0f) {
                        magnetBtnAnimTimer = (magnetBtnAnimTimer - dt).coerceAtLeast(0f)
                    }
                    if (shuffleBtnAnimTimer > 0f) {
                        shuffleBtnAnimTimer = (shuffleBtnAnimTimer - dt).coerceAtLeast(0f)
                    }
                    if (cannonBtnAnimTimer > 0f) {
                        cannonBtnAnimTimer = (cannonBtnAnimTimer - dt).coerceAtLeast(0f)
                    }
                    if (magnetBtnShakeTimer > 0f) {
                        magnetBtnShakeTimer = (magnetBtnShakeTimer - dt).coerceAtLeast(0f)
                    }
                    if (shuffleBtnShakeTimer > 0f) {
                        shuffleBtnShakeTimer = (shuffleBtnShakeTimer - dt).coerceAtLeast(0f)
                    }
                    if (cannonBtnShakeTimer > 0f) {
                        cannonBtnShakeTimer = (cannonBtnShakeTimer - dt).coerceAtLeast(0f)
                    }
                    if (powerupOverlayTimer > 0f) {
                        powerupOverlayTimer = (powerupOverlayTimer - dt).coerceAtLeast(0f)
                        if (powerupOverlayTimer <= 0f) {
                            activePowerupOverlayType = null
                        }
                    }

                    // Update Cannon Booster 6-stage sequence
                    if (isCannonActive) {
                        cannonTimer += dt

                        // Fire ALL collected balls in one simultaneous explosive blast at cannonFireMoment!
                        if (!cannonHasFired && cannonTimer >= cannonFireMoment) {
                            cannonHasFired = true
                            SoundManager.playBrickClear()
                            SoundManager.playPowerup()

                            // Exact muzzle position rotated with current cannonAimAngleDeg around (132f, 348f)
                            val cannonX = 132f
                            val cannonY = 348f
                            val drawSize = 176f
                            val localMuzzleX = (0.506f - 0.36f) * drawSize
                            val localMuzzleY = (0.577f - 0.62f) * drawSize
                            val rad = cannonAimAngleDeg * (PI.toFloat() / 180f)
                            val muzzleX = cannonX + localMuzzleX * cos(rad) - localMuzzleY * sin(rad)
                            val muzzleY = cannonY + localMuzzleX * sin(rad) + localMuzzleY * cos(rad)

                            cannonCollectedBalls.forEachIndexed { idx, cb ->
                                val col = columns.getOrNull(cb.targetCol)
                                val brick = cb.targetBrick
                                val brickIdx = if (col != null && brick != null) col.bricks.indexOf(brick) else -1
                                val hasValidBucket = col != null && brick != null && brickIdx >= 0

                                val destX = if (hasValidBucket) {
                                    col!!.x + (((idx % 3) - 1) * 8f)
                                } else {
                                    70f + (idx % 5) * 38f
                                }
                                val destY = if (hasValidBucket) {
                                    GameConstants.BALTI_SOCKET_Y - minOf(brickIdx, 2) * 8f
                                } else {
                                    470f + (idx % 3) * 18f
                                }

                                val ctrlX = (muzzleX + destX) * 0.5f + (if (destX >= muzzleX) 28f else -24f)
                                val ctrlY = minOf(muzzleY, destY) - 24f

                                cannonProjectiles.add(
                                    CannonProjectile(
                                        id = cb.id,
                                        color = cb.color,
                                        startX = muzzleX,
                                        startY = muzzleY,
                                        controlX = ctrlX.coerceIn(22f, 274f),
                                        controlY = ctrlY,
                                        targetX = destX,
                                        targetY = destY,
                                        targetCol = if (hasValidBucket) cb.targetCol else -1,
                                        targetBrick = if (hasValidBucket) brick else null,
                                        elapsed = 0f,
                                        delay = (idx % 4) * 0.012f,
                                        duration = 0.32f + (idx % 3) * 0.025f
                                    )
                                )
                            }
                            // Clear collected list immediately on launch so balls never duplicate!
                            cannonCollectedBalls.clear()

                            // Spawn billowing muzzle smoke puffs
                            for (sIdx in 0 until 8) {
                                val sAng = (rad - 0.55f) + Random.nextFloat() * 1.15f
                                val sSpd = 28f + Random.nextFloat() * 48f
                                cannonSmokePuffs.add(
                                    CannonSmokePuff(
                                        x = muzzleX + cos(sAng) * 8f,
                                        y = muzzleY + sin(sAng) * 8f,
                                        vx = cos(sAng) * sSpd,
                                        vy = sin(sAng) * sSpd - 16f,
                                        baseScale = 0.72f + Random.nextFloat() * 0.45f,
                                        maxLife = 0.68f + Random.nextFloat() * 0.25f
                                    )
                                )
                            }
                            // Explosion starburst sparks
                            for (sp in 0 until 24) {
                                val ang = (rad - 0.9f) + Random.nextFloat() * 1.8f
                                val spd = 65f + Random.nextFloat() * 125f
                                particles.add(
                                    Particle(
                                        x = muzzleX,
                                        y = muzzleY,
                                        vx = cos(ang) * spd,
                                        vy = sin(ang) * spd,
                                        color = if (sp % 3 == 0) 0xFFFFFFFF else if (sp % 2 == 0) 0xFFFFD84D else 0xFF00E5FF,
                                        size = 3.0f + Random.nextFloat() * 3.0f,
                                        maxLife = 0.48f + Random.nextFloat() * 0.2f
                                    )
                                )
                            }
                        }

                        if (cannonTimer >= cannonTotalDuration && cannonProjectiles.isEmpty()) {
                            isCannonActive = false
                            cannonTimer = 0f
                            cannonHasFired = false
                        }
                    }

                    // Update Cannon Smoke Puffs
                    if (cannonSmokePuffs.isNotEmpty()) {
                        val puffsToRemove = mutableListOf<CannonSmokePuff>()
                        for (puff in cannonSmokePuffs) {
                            puff.life += dt
                            if (puff.life >= puff.maxLife) {
                                puffsToRemove.add(puff)
                            } else {
                                puff.x += puff.vx * dt
                                puff.y += puff.vy * dt
                            }
                        }
                        cannonSmokePuffs.removeAll(puffsToRemove)
                    }

                    // Update Cannon Fired Projectiles -> deliver into front OR behind buckets
                    if (cannonProjectiles.isNotEmpty()) {
                        for (proj in cannonProjectiles) {
                            proj.elapsed += dt
                            if (!proj.landed && proj.elapsed >= proj.delay + proj.duration) {
                                proj.landed = true
                                val col = columns.getOrNull(proj.targetCol)
                                val brick = proj.targetBrick
                                if (col != null && brick != null && col.bricks.contains(brick)) {
                                    brick.inFlight = maxOf(0, brick.inFlight - 1)
                                    brick.filled++
                                    col.lightPulseTimer = 0.78f
                                    when (brick.filled) {
                                        1 -> {
                                            brick.animState = "hit_1"
                                            brick.animTimer = 0f
                                            SoundManager.playDropHole(0)
                                        }
                                        2 -> {
                                            brick.animState = "hit_2"
                                            brick.animTimer = 0f
                                            SoundManager.playDropHole(1)
                                        }
                                        else -> {
                                            brick.animState = "hit_3"
                                            brick.animTimer = 0f
                                            SoundManager.playDropHole(2)
                                        }
                                    }
                                    for (sp in 0 until 4) {
                                        particles.add(
                                            Particle(
                                                x = proj.targetX,
                                                y = proj.targetY,
                                                vx = (Random.nextFloat() - 0.5f) * 65f,
                                                vy = -25f - Random.nextFloat() * 45f,
                                                color = 0xFFFFE494,
                                                size = 2.6f,
                                                maxLife = 0.35f
                                            )
                                        )
                                    }
                                } else {
                                    // Excess ball with no matching bucket hole: sparkling firework pop
                                    SoundManager.playPopBurst()
                                    for (sp in 0 until 6) {
                                        val ang = sp * (2f * PI.toFloat() / 6f)
                                        particles.add(
                                            Particle(
                                                x = proj.targetX,
                                                y = proj.targetY,
                                                vx = cos(ang) * 55f,
                                                vy = sin(ang) * 55f,
                                                color = 0xFF38E8FF,
                                                size = 2.6f,
                                                maxLife = 0.35f
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        cannonProjectiles.removeAll { it.landed }
                    }

                    // Smoothly recover upper grid container scale & finalize magnet_receiving state
                    val scaleLerp = (dt * 14f).coerceIn(0f, 1f)
                    for (r in 0 until GameConstants.GRID_ROWS) {
                        for (c in 0 until GameConstants.GRID_COLS) {
                            val cell = grid[r][c]
                            if (cell.scaleX != 1f) {
                                cell.scaleX += (1f - cell.scaleX) * scaleLerp
                                if (kotlin.math.abs(1f - cell.scaleX) < 0.005f) cell.scaleX = 1f
                            }
                            if (cell.scaleY != 1f) {
                                cell.scaleY += (1f - cell.scaleY) * scaleLerp
                                if (kotlin.math.abs(1f - cell.scaleY) < 0.005f) cell.scaleY = 1f
                            }
                            if (cell.animState == "magnet_receiving") {
                                val stillReceiving = magnetReturningBalls.any {
                                    !it.landed && it.targetRow == r && it.targetCol == c
                                }
                                if (!stillReceiving) {
                                    cell.animState = "idle"
                                }
                            } else if (cell.kind == CellKind.DISPENSER) {
                                if (cell.animState == "cave_dispensing") {
                                    cell.animTimer += dt
                                    if (cell.animTimer >= 0.56f) {
                                        cell.animState = "idle"
                                        cell.animTimer = 0f
                                    }
                                } else {
                                    cell.animTimer = 0f
                                }
                            } else if (cell.kind == CellKind.BLOCK && cell.animState == "dispensing") {
                                cell.animTimer += dt
                                val p = ((cell.animTimer - 0.08f) / 0.36f).coerceIn(0f, 1f)
                                val ease = 1f - (1f - p) * (1f - p) * (1f - p)
                                when (cell.direction) {
                                    DispenserDirection.DOWN -> {
                                        cell.offsetX = 0f
                                        cell.offsetY = -30f * (1f - ease)
                                    }
                                    DispenserDirection.RIGHT -> {
                                        cell.offsetX = -30f * (1f - ease)
                                        cell.offsetY = 0f
                                    }
                                    DispenserDirection.LEFT -> {
                                        cell.offsetX = 30f * (1f - ease)
                                        cell.offsetY = 0f
                                    }
                                }
                                val popScale = 0.72f + 0.28f * ease + 0.045f * sin(p * PI.toFloat())
                                cell.scaleX = popScale
                                cell.scaleY = popScale
                                if (cell.animTimer >= 0.48f) {
                                    cell.offsetX = 0f
                                    cell.offsetY = 0f
                                    cell.scaleX = 1f
                                    cell.scaleY = 1f
                                    cell.animState = "idle"
                                    cell.animTimer = 0f
                                }
                            }
                        }
                    }

                    // Detect full chain with no matching front bucket below
                    if (gameState == "playing") {
                        val isStuck = GameRules.isChainFullAndStuck(
                            beltBalls = beltBalls,
                            beltCapacity = beltCapacity,
                            columns = columns,
                            droppingBallsCount = droppingBalls.size,
                            cannonProjectilesCount = cannonProjectiles.size,
                            isMagnetActive = isMagnetActive,
                            isCannonActive = isCannonActive
                        )
                        if (isStuck) {
                            chainStuckTimer += dt
                            if (chainStuckTimer >= 0.45f) {
                                val selected = GameRules.selectCleanUpBeltBalls(beltBalls.toList(), 8)
                                cleanUpHighlightedIds = selected.map { it.id }.toSet()
                                SoundManager.playBlocked()
                                gameState = "chain_stuck"
                            }
                        } else {
                            chainStuckTimer = 0f
                        }
                    }
                }
            }
        }
    }

    // Touch Handling in true pixel coordinates
    fun handleTouch(rawX: Float, rawY: Float) {
        val s = viewScale
        if (s <= 0f) return

        val gameX = (rawX - viewOffsetX) / s
        val gameY = (rawY - viewOffsetY) / s

        // 1. Pause Button: top-left (x in 4..68, y in 4..68)
        if (gameX in 4f..68f && gameY in 4f..68f) {
            SoundManager.playTap()
            isPaused = true
            return
        }

        if (isPaused || gameState != "playing") return

        val arenaGameX = gameX
        val arenaGameY = gameY - GameConstants.ARENA_SHIFT_Y

        // 2. Powerups placed at bottom of arena (drawn inside arenaOffsetY at powerupY = 605f):
        // Centered symmetrically around arena center 148f: Magnet (88f), Shuffle (148f), Cannon (208f)
        // Magnet hitbox: x in 58..118, y in 598..660
        if (arenaGameX in 58f..118f && arenaGameY in 598f..660f) {
            if (magnetCount > 0 && beltBalls.isNotEmpty() && magnetReturningBalls.isEmpty() && !isCannonActive) {
                val assignments = GameRules.planMagnetReturn(grid, beltBalls.toList())
                if (assignments.isNotEmpty()) {
                    magnetCount = (magnetCount - 1).coerceAtLeast(0)
                    isMagnetActive = true
                    magnetPulseTimer = 1.05f
                    magnetBtnAnimTimer = powerupBtnAnimDuration
                    activePowerupOverlayType = "magnet"
                    powerupOverlayTimer = powerupOverlayDuration
                    isTutorialDismissed = true
                    SoundManager.playPowerup()

                    // Celebratory center electric motes
                    for (sp in 0 until 14) {
                        val angle = Random.nextFloat() * 2f * PI.toFloat()
                        val spd = 45f + Random.nextFloat() * 85f
                        particles.add(
                            Particle(
                                x = 148f + cos(angle) * 14f,
                                y = 342f + sin(angle) * 14f,
                                vx = cos(angle) * spd,
                                vy = sin(angle) * spd,
                                color = if (sp % 2 == 0) 0xFF38E8FF else 0xFFFFE15A,
                                size = 2.6f + Random.nextFloat() * 2.4f,
                                maxLife = 0.55f
                            )
                        )
                    }

                    val staggerStep = if (assignments.size > 1) {
                        minOf(0.040f, 0.44f / assignments.size)
                    } else {
                        0f
                    }
                    val pulledIds = mutableSetOf<Int>()

                    assignments.forEachIndexed { idx, item ->
                        pulledIds.add(item.beltBall.id)
                        val coords = BeltPath.getBeltCoordinates(item.beltBall.s)
                        val cx = GameConstants.GRID_X_CENTERS[item.targetCol]
                        val cy = GameConstants.GRID_Y_CENTERS[item.targetRow]
                        val slotRow = item.targetSlotIndex / 3
                        val slotCol = item.targetSlotIndex % 3
                        val targetX = cx + (crateColNormX[slotCol] - 0.5f) * 34f
                        val targetY = cy + (crateRowNormY[slotRow] - 0.5f) * 33f

                        val midX = (coords.x + targetX) * 0.5f
                        val arcSide = if (coords.x < targetX) -22f else 22f
                        val controlX = (midX + arcSide).coerceIn(22f, 274f)
                        val controlY = (coords.y * 0.32f + targetY * 0.68f) - 20f

                        magnetReturningBalls.add(
                            MagnetReturningBall(
                                id = item.beltBall.id,
                                color = item.beltBall.color,
                                startX = coords.x,
                                startY = coords.y,
                                controlX = controlX,
                                controlY = controlY,
                                targetX = targetX,
                                targetY = targetY,
                                targetRow = item.targetRow,
                                targetCol = item.targetCol,
                                targetSlotIndex = item.targetSlotIndex,
                                elapsed = 0f,
                                delay = idx * staggerStep,
                                duration = 0.46f
                            )
                        )
                    }
                    beltBalls.removeAll { pulledIds.contains(it.id) }
                } else {
                    magnetBtnShakeTimer = 0.30f
                    SoundManager.playBlocked()
                }
            } else {
                magnetBtnShakeTimer = 0.30f
                SoundManager.playBlocked()
            }
            return
        }

        // Shuffle hitbox: x in 118..178, y in 598..660
        if (arenaGameX > 118f && arenaGameX <= 178f && arenaGameY in 598f..660f) {
            if (shuffleCount > 0 && !isCannonActive) {
                val result = GameRules.performShufflePowerup(columns, beltBalls, physics.balls)
                if (result.shuffled) {
                    shuffleCount = (shuffleCount - 1).coerceAtLeast(0)
                    shuffleBtnAnimTimer = powerupBtnAnimDuration
                    activePowerupOverlayType = "shuffle"
                    powerupOverlayTimer = powerupOverlayDuration
                    isTutorialDismissed = true
                    SoundManager.playPowerup()
                    for (sp in 0 until 14) {
                        val angle = Random.nextFloat() * 2f * PI.toFloat()
                        val spd = 45f + Random.nextFloat() * 85f
                        particles.add(
                            Particle(
                                x = 148f + cos(angle) * 14f,
                                y = 342f + sin(angle) * 14f,
                                vx = cos(angle) * spd,
                                vy = sin(angle) * spd,
                                color = if (sp % 2 == 0) 0xFF38E8FF else 0xFFFFD84D,
                                size = 2.6f + Random.nextFloat() * 2.4f,
                                maxLife = 0.55f
                            )
                        )
                    }
                    for (colIdx in result.swappedColumns) {
                        val col = columns.getOrNull(colIdx) ?: continue
                        if (col.bricks.isNotEmpty()) {
                            val front = col.bricks[0]
                            if (front.filled < GameConstants.BRICK_HOLES && front.animState == "idle") {
                                front.animState = "activate"
                                front.animTimer = 0f
                            }
                        }
                        for (sp in 0 until 6) {
                            particles.add(
                                Particle(
                                    x = col.x + (Random.nextFloat() - 0.5f) * 28f,
                                    y = GameConstants.BALTI_SOCKET_Y + (Random.nextFloat() - 0.5f) * 18f,
                                    vx = (Random.nextFloat() - 0.5f) * 90f,
                                    vy = -35f - Random.nextFloat() * 55f,
                                    color = 0xFFFFE600,
                                    size = 2.8f + Random.nextFloat() * 2.2f,
                                    maxLife = 0.45f
                                )
                            )
                        }
                    }
                } else {
                    shuffleBtnShakeTimer = 0.30f
                    SoundManager.playBlocked()
                }
            } else {
                shuffleBtnShakeTimer = 0.30f
                SoundManager.playBlocked()
            }
            return
        }

        // Cannon Booster hitbox: x in 178..238, y in 598..660
        if (arenaGameX > 178f && arenaGameX <= 238f && arenaGameY in 598f..660f) {
            if (cannonCount > 0 && beltBalls.isNotEmpty() && magnetReturningBalls.isEmpty() && !isCannonActive) {
                val assignments = GameRules.planCannonBlast(columns, beltBalls.toList())
                if (assignments.isNotEmpty()) {
                    cannonCount = (cannonCount - 1).coerceAtLeast(0)
                    isCannonActive = true
                    cannonTimer = 0f
                    cannonHasFired = false
                    cannonBtnAnimTimer = powerupBtnAnimDuration
                    isTutorialDismissed = true
                    SoundManager.playPowerup()

                    cannonCollectedBalls.clear()
                    val stagger = if (assignments.size > 1) minOf(0.032f, 0.36f / assignments.size) else 0f
                    var sumTargetX = 0f
                    var validTargetCount = 0

                    assignments.forEachIndexed { idx, item ->
                        val coords = BeltPath.getBeltCoordinates(item.beltBall.s)
                        if (item.targetCol in columns.indices) {
                            sumTargetX += columns[item.targetCol].x
                            validTargetCount++
                        }
                        cannonCollectedBalls.add(
                            CannonCollectedBall(
                                id = item.beltBall.id,
                                color = item.beltBall.color,
                                startX = coords.x,
                                startY = coords.y,
                                targetCol = item.targetCol,
                                targetBrick = item.targetBrick,
                                delay = idx * stagger,
                                swirlSeed = idx * 1.17f
                            )
                        )
                    }
                    // Clear ALL conveyor belt balls from actual game state at once (zero duplicates!)
                    beltBalls.clear()

                    // Compute smooth aim tilt angle toward target bucket centroid
                    val avgTargetX = if (validTargetCount > 0) sumTargetX / validTargetCount else 155f
                    cannonAimAngleDeg = ((avgTargetX - 132f) * 0.14f + 16f).coerceIn(6f, 30f)
                } else {
                    cannonBtnShakeTimer = 0.30f
                    SoundManager.playBlocked()
                }
            } else {
                cannonBtnShakeTimer = 0.30f
                SoundManager.playBlocked()
            }
            return
        }

        // 3. Upper Grid Blocks (using same arena coordinates)

        val halfW = 18f
        val halfH = 17f

        // PASS 1: Check for exposed playable BLOCKs or DISPENSERs (or Magnet tap)
        for (r in GameConstants.GRID_ROWS - 1 downTo 0) {
            for (c in 0 until GameConstants.GRID_COLS) {
                val cx = GameConstants.GRID_X_CENTERS[c]
                val cy = GameConstants.GRID_Y_CENTERS[r]

                if (arenaGameX in (cx - halfW)..(cx + halfW) &&
                    arenaGameY in (cy - halfH)..(cy + halfH)
                ) {
                    val cell = grid[r][c]

                    if ((cell.kind == CellKind.BLOCK || (cell.kind == CellKind.LOCKED && cell.linkGroup != null)) &&
                        cell.animState != "magnet_receiving" &&
                        GameRules.isExposed(grid, r, c)
                    ) {
                        isFunnelLocked = GameRules.shouldBlockBlockTap(
                            waitingBallsCount = physics.balls.size,
                            currentlyLocked = isFunnelLocked,
                            maxWaitingBalls = maxWaitingBalls,
                            resumeWaitingBalls = resumeWaitingBalls
                        )
                        if (isFunnelLocked) {
                            SoundManager.playBlocked()
                            funnelFullAlertTimer = 1.4f
                            return
                        }
                        isTutorialDismissed = true
                        val burstCells = GameRules.collectLinkedBurstCells(grid, r, c)
                        SoundManager.playPopBurst()
                        for ((br, bc) in burstCells) {
                            val bCell = grid[br][bc]
                            val burstColor = bCell.color ?: MarbleColor.PURPLE
                            val spawnCount = bCell.ballCount.coerceIn(1, GameConstants.BALLS_PER_BLOCK)
                            val bcx = GameConstants.GRID_X_CENTERS[bc]
                            val bcy = GameConstants.GRID_Y_CENTERS[br]
                            grid[br][bc] = GridCell(kind = CellKind.EMPTY)
                            physics.spawnBurst(bcx, bcy, burstColor, spawnCount)
                            score += 50
                            if (burstCells.size > 1) {
                                for (sp in 0 until 8) {
                                    val ang = sp * (2f * PI.toFloat() / 8f)
                                    val spd = 36f + Random.nextFloat() * 44f
                                    particles.add(
                                        Particle(
                                            x = bcx,
                                            y = bcy,
                                            vx = cos(ang) * spd,
                                            vy = sin(ang) * spd,
                                            color = if (sp % 2 == 0) 0xFFFFFFFF else 0xFFFFE494,
                                            size = 2.6f,
                                            shape = ParticleShape.SPARKLE,
                                            maxLife = 0.36f
                                        )
                                    )
                                }
                            }
                        }
                        isFunnelLocked = GameRules.shouldBlockBlockTap(
                            waitingBallsCount = physics.balls.size,
                            currentlyLocked = isFunnelLocked,
                            maxWaitingBalls = maxWaitingBalls,
                            resumeWaitingBalls = resumeWaitingBalls
                        )
                        val gridRes = GameRules.updateGridAfterChange(grid)
                        for (disp in gridRes.dispensedCells) {
                            val dispX = GameConstants.GRID_X_CENTERS[disp.dispenserC]
                            val dispY = GameConstants.GRID_Y_CENTERS[disp.dispenserR]
                            for (sp in 0 until 8) {
                                particles.add(
                                    Particle(
                                        x = dispX + (Random.nextFloat() - 0.5f) * 20f,
                                        y = dispY + 10f,
                                        vx = (Random.nextFloat() - 0.5f) * 70f,
                                        vy = 20f + Random.nextFloat() * 55f,
                                        color = 0xFF38BDF8,
                                        size = 2.6f,
                                        maxLife = 0.42f
                                    )
                                )
                            }
                        }
                        return
                    } else if (cell.kind == CellKind.DISPENSER && (cell.counter ?: 0) > 0) {
                        val (targetR, targetC, startOx, startOy) = when (cell.direction) {
                            DispenserDirection.DOWN -> listOf(r + 1, c, 0, -28)
                            DispenserDirection.RIGHT -> listOf(r, c + 1, -28, 0)
                            DispenserDirection.LEFT -> listOf(r, c - 1, 28, 0)
                        }
                        if (targetR in 0 until GameConstants.GRID_ROWS &&
                            targetC in 0 until GameConstants.GRID_COLS &&
                            grid[targetR][targetC].kind == CellKind.EMPTY
                        ) {
                            SoundManager.playPopBurst()
                            val nextColor = if (cell.queue.isNotEmpty()) cell.queue.removeAt(0) else MarbleColor.PURPLE
                            cell.color = nextColor
                            cell.counter = (cell.counter ?: 1) - 1
                            cell.animState = "cave_dispensing"
                            cell.animTimer = 0f
                            grid[targetR][targetC] = GridCell(
                                kind = CellKind.BLOCK,
                                color = nextColor,
                                direction = cell.direction,
                                animState = "dispensing",
                                animTimer = 0f,
                                scaleX = 0.65f,
                                scaleY = 0.65f,
                                offsetX = startOx.toFloat(),
                                offsetY = startOy.toFloat()
                            )
                            for (sp in 0 until 8) {
                                val pVx = when (cell.direction) {
                                    DispenserDirection.RIGHT -> 25f + Random.nextFloat() * 50f
                                    DispenserDirection.LEFT -> -25f - Random.nextFloat() * 50f
                                    DispenserDirection.DOWN -> (Random.nextFloat() - 0.5f) * 70f
                                }
                                val pVy = when (cell.direction) {
                                    DispenserDirection.DOWN -> 20f + Random.nextFloat() * 55f
                                    else -> (Random.nextFloat() - 0.5f) * 55f
                                }
                                particles.add(
                                    Particle(
                                        x = cx + when (cell.direction) {
                                            DispenserDirection.RIGHT -> 10f
                                            DispenserDirection.LEFT -> -10f
                                            DispenserDirection.DOWN -> (Random.nextFloat() - 0.5f) * 20f
                                        },
                                        y = cy + when (cell.direction) {
                                            DispenserDirection.DOWN -> 10f
                                            else -> (Random.nextFloat() - 0.5f) * 16f
                                        },
                                        vx = pVx,
                                        vy = pVy,
                                        color = 0xFF38BDF8,
                                        size = 2.6f,
                                        maxLife = 0.42f
                                    )
                                )
                            }
                            GameRules.updateGridAfterChange(grid)
                            return
                        }
                    }
                }
            }
        }

        // PASS 2: If no playable block was hit, check for LOCKED blocks to trigger blocked sound feedback
        for (r in 0 until GameConstants.GRID_ROWS) {
            for (c in 0 until GameConstants.GRID_COLS) {
                val cx = GameConstants.GRID_X_CENTERS[c]
                val cy = GameConstants.GRID_Y_CENTERS[r]

                if (arenaGameX in (cx - halfW)..(cx + halfW) &&
                    arenaGameY in (cy - halfH)..(cy + halfH)
                ) {
                    val cell = grid[r][c]
                    if (cell.kind == CellKind.LOCKED) {
                        SoundManager.playBlocked()
                        return
                    }
                }
            }
        }
    }

    val currentHandleTouch by rememberUpdatedState(::handleTouch)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF002766))
    ) {
        // FULLSCREEN 60FPS CANVAS GAME RENDERER
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(currentLevel, levelResetNonce) {
                    detectTapGestures { tapOffset ->
                        currentHandleTouch(tapOffset.x, tapOffset.y)
                    }
                }
        ) {
            val canvas = drawContext.canvas.nativeCanvas
            val canvasW = size.width
            val canvasH = size.height

            // 1. Calculate pixel scale and centered offsets
            val scaleX = canvasW / GameConstants.BASE_WIDTH
            val scaleY = canvasH / GameConstants.BASE_HEIGHT
            val scale = minOf(scaleX, scaleY)
            val offsetX = (canvasW - GameConstants.BASE_WIDTH * scale) / 2f
            val offsetY = (canvasH - GameConstants.BASE_HEIGHT * scale) / 2f
            val arenaOffsetY = offsetY + GameConstants.ARENA_SHIFT_Y * scale

            viewScale = scale
            viewOffsetX = offsetX
            viewOffsetY = offsetY
            viewArenaOffsetY = arenaOffsetY

            // 2. Draw Fullscreen Background
            if (bgBitmap != null) {
                canvas.drawBitmap(
                    bgBitmap,
                    null,
                    Rect(0, 0, canvasW.toInt(), canvasH.toInt()),
                    null
                )
            } else {
                drawRect(Color(0xFF008AE6))
            }

            // 3. Draw Scaled Arena Elements (shifted up slightly for perfect framing)
            canvas.save()
            canvas.translate(offsetX, arenaOffsetY)
            canvas.scale(scale, scale)

            // Cyber Game Frame
            if (cyberFrameBitmap != null) {
                canvas.drawBitmap(
                    cyberFrameBitmap,
                    null,
                    RectF(0f, 117f, 296f, 606f),
                    null
                )
            }

            // Upper Grid Crates (Aligned to prevent overlapping neighboring crates)
            val crateW = 34f
            val crateH = 33f

            fun isCrateRenderable(cell: GridCell): Boolean {
                return (cell.kind == CellKind.BLOCK || cell.kind == CellKind.LOCKED) &&
                    !(cell.kind == CellKind.BLOCK && cell.animState == "dispensing" && cell.animTimer < 0.16f)
            }

            // Connected-pair crisp white outer frame pass (rendered beneath linked crates)
            val linkFrameFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                style = Paint.Style.FILL
            }
            val linkFrameShadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#30000000")
                style = Paint.Style.STROKE
                strokeWidth = 1.2f
            }
            for (r in 0 until GameConstants.GRID_ROWS) {
                for (c in 0 until GameConstants.GRID_COLS) {
                    val cell = grid[r][c]
                    val group = cell.linkGroup ?: continue
                    if (!isCrateRenderable(cell)) continue
                    val cx = GameConstants.GRID_X_CENTERS[c] + cell.offsetX
                    val cy = GameConstants.GRID_Y_CENTERS[r] + cell.offsetY
                    val hw = (crateW * cell.scaleX) / 2f + 0.4f
                    val hh = (crateH * cell.scaleY) / 2f + 0.3f

                    var drewPair = false
                    if (c + 1 < GameConstants.GRID_COLS) {
                        val right = grid[r][c + 1]
                        if (right.linkGroup == group && isCrateRenderable(right)) {
                            val rcx = GameConstants.GRID_X_CENTERS[c + 1] + right.offsetX
                            val rcy = GameConstants.GRID_Y_CENTERS[r] + right.offsetY
                            val pairRect = RectF(
                                minOf(cx - hw, rcx - hw),
                                minOf(cy - hh, rcy - hh),
                                maxOf(cx + hw, rcx + hw),
                                maxOf(cy + hh, rcy + hh)
                            )
                            canvas.drawRoundRect(pairRect, 7.2f, 7.2f, linkFrameFill)
                            canvas.drawRoundRect(pairRect, 7.2f, 7.2f, linkFrameShadow)
                            drewPair = true
                        }
                    }
                    if (r + 1 < GameConstants.GRID_ROWS) {
                        val below = grid[r + 1][c]
                        if (below.linkGroup == group && isCrateRenderable(below)) {
                            val bcx = GameConstants.GRID_X_CENTERS[c] + below.offsetX
                            val bcy = GameConstants.GRID_Y_CENTERS[r + 1] + below.offsetY
                            val pairRect = RectF(
                                minOf(cx - hw, bcx - hw),
                                minOf(cy - hh, bcy - hh),
                                maxOf(cx + hw, bcx + hw),
                                maxOf(cy + hh, bcy + hh)
                            )
                            canvas.drawRoundRect(pairRect, 7.2f, 7.2f, linkFrameFill)
                            canvas.drawRoundRect(pairRect, 7.2f, 7.2f, linkFrameShadow)
                            drewPair = true
                        }
                    }
                    if (!drewPair) {
                        val hasLeft = c > 0 && grid[r][c - 1].linkGroup == group && isCrateRenderable(grid[r][c - 1])
                        val hasAbove = r > 0 && grid[r - 1][c].linkGroup == group && isCrateRenderable(grid[r - 1][c])
                        if (!hasLeft && !hasAbove) {
                            val singleRect = RectF(cx - hw, cy - hh, cx + hw, cy + hh)
                            canvas.drawRoundRect(singleRect, 7.2f, 7.2f, linkFrameFill)
                            canvas.drawRoundRect(singleRect, 7.2f, 7.2f, linkFrameShadow)
                        }
                    }
                }
            }

            // Pass 1: Draw BLOCK and LOCKED cells (emerging blocks clip cleanly at the cave mouth plane)
            for (r in 0 until GameConstants.GRID_ROWS) {
                for (c in 0 until GameConstants.GRID_COLS) {
                    val cell = grid[r][c]
                    val cx = GameConstants.GRID_X_CENTERS[c]
                    val cy = GameConstants.GRID_Y_CENTERS[r]

                    when (cell.kind) {
                        CellKind.BLOCK -> {
                            val isEmergingFromCave = cell.animState == "dispensing"
                            if (isEmergingFromCave && cell.animTimer < 0.08f) {
                                continue
                            }
                            val col = cell.color ?: MarbleColor.PURPLE
                            val crateFullBmp = AssetLoader.getCrateFull(context, col)
                            val crateOpenBmp = AssetLoader.getCrateOpen(context, col)
                            val linkInset = if (cell.linkGroup != null) 3.0f else 0f
                            val scaledW = (crateW - linkInset) * cell.scaleX
                            val scaledH = (crateH - linkInset) * cell.scaleY
                            val blockCx = cx + cell.offsetX
                            val blockCy = cy + cell.offsetY
                            val dstRect = RectF(
                                blockCx - scaledW / 2f,
                                blockCy - scaledH / 2f,
                                blockCx + scaledW / 2f,
                                blockCy + scaledH / 2f
                            )

                            canvas.save()
                            if (isEmergingFromCave) {
                                // Clip the back side so the bucket only emerges out of the dispenser mouth, never poking behind the dome
                                when (cell.direction) {
                                    DispenserDirection.DOWN -> {
                                        canvas.clipRect(
                                            -12f,
                                            cy - GameConstants.CELL_PITCH_Y + 4f,
                                            GameConstants.BASE_WIDTH + 12f,
                                            GameConstants.BASE_HEIGHT + 12f
                                        )
                                    }
                                    DispenserDirection.RIGHT -> {
                                        canvas.clipRect(
                                            cx - GameConstants.CELL_PITCH_X + 4f,
                                            -12f,
                                            GameConstants.BASE_WIDTH + 12f,
                                            GameConstants.BASE_HEIGHT + 12f
                                        )
                                    }
                                    DispenserDirection.LEFT -> {
                                        canvas.clipRect(
                                            -12f,
                                            -12f,
                                            cx + GameConstants.CELL_PITCH_X - 4f,
                                            GameConstants.BASE_HEIGHT + 12f
                                        )
                                    }
                                }
                            }

                            if (cell.animState == "magnet_receiving") {
                                val magnetGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    color = android.graphics.Color.parseColor("#6600F0FF")
                                    style = Paint.Style.STROKE
                                    strokeWidth = 2.4f
                                }
                                canvas.drawRoundRect(
                                    RectF(dstRect.left - 2f, dstRect.top - 2f, dstRect.right + 2f, dstRect.bottom + 2f),
                                    7f,
                                    7f,
                                    magnetGlow
                                )
                            }

                            if (cell.isMystery && cell.animState != "magnet_receiving") {
                                val mysteryBmp = AssetLoader.loadBitmap(context, "crates/crate_mystery.png")
                                    ?: crateFullBmp
                                if (mysteryBmp != null) {
                                    canvas.drawBitmap(mysteryBmp, null, dstRect, null)
                                }
                            } else if (cell.ballCount >= GameConstants.BALLS_PER_BLOCK &&
                                cell.animState != "magnet_receiving" &&
                                crateFullBmp != null
                            ) {
                                canvas.drawBitmap(crateFullBmp, null, dstRect, null)
                            } else {
                                val containerBmp = crateOpenBmp ?: crateFullBmp
                                if (containerBmp != null) {
                                    canvas.drawBitmap(containerBmp, null, dstRect, null)
                                }
                                val seatedCount = cell.ballCount.coerceIn(0, GameConstants.BALLS_PER_BLOCK)
                                if (seatedCount > 0) {
                                    val brX = scaledW * 0.153f
                                    val brY = scaledH * 0.156f
                                    for (sRow in 0..2) {
                                        for (sCol in intArrayOf(0, 2, 1)) {
                                            val slotIdx = sRow * 3 + sCol
                                            if (slotIdx >= seatedCount) continue
                                            val slotBmp = AssetLoader.getFluffy(context, col, crateSlotMoods[slotIdx])
                                                ?: AssetLoader.getBall(context, col)
                                                ?: continue
                                            val bx = dstRect.left + crateColNormX[sCol] * scaledW
                                            val by = dstRect.top + crateRowNormY[sRow] * scaledH
                                            canvas.drawBitmap(
                                                slotBmp,
                                                null,
                                                RectF(bx - brX, by - brY, bx + brX, by + brY),
                                                null
                                            )
                                        }
                                    }
                                }
                            }
                            canvas.restore()
                        }

                        CellKind.LOCKED -> {
                            val col = cell.color ?: MarbleColor.PURPLE
                            val bmp = if (cell.isMystery) {
                                AssetLoader.loadBitmap(context, "crates/crate_mystery.png")
                                    ?: AssetLoader.getCrateClosed(context, col)
                            } else {
                                AssetLoader.getCrateClosed(context, col)
                            }

                            if (bmp != null) {
                                val linkInset = if (cell.linkGroup != null) 1.5f else 0f
                                canvas.drawBitmap(
                                    bmp,
                                    null,
                                    RectF(
                                        cx - crateW / 2f + linkInset,
                                        cy - crateH / 2f + linkInset,
                                        cx + crateW / 2f - linkInset,
                                        cy + crateH / 2f - linkInset
                                    ),
                                    null
                                )
                            }
                        }

                        else -> {}
                    }
                }
            }

            // Pass 2: Draw DISPENSER hoods ON TOP so emerging buckets visibly slide out from inside the cave rim
            for (r in 0 until GameConstants.GRID_ROWS) {
                for (c in 0 until GameConstants.GRID_COLS) {
                    val cell = grid[r][c]
                    if (cell.kind != CellKind.DISPENSER) continue

                    val cx = GameConstants.GRID_X_CENTERS[c]
                    val cy = GameConstants.GRID_Y_CENTERS[r]
                    val countNum = cell.counter ?: 0
                    val isDispensing = cell.animState == "cave_dispensing"
                    val t = cell.animTimer

                    // Completely still when idle (frame 0, scale 1f); puffs open only while actively dispensing
                    val hoodFrameIndex = if (isDispensing) {
                        when {
                            t < 0.06f -> 1
                            t < 0.12f -> 2
                            t < 0.18f -> 3
                            t < 0.24f -> 4
                            t < 0.36f -> 5
                            t < 0.44f -> 3
                            t < 0.50f -> 2
                            else -> 1
                        }
                    } else {
                        0
                    }

                    val scaleFX: Float
                    val scaleFY: Float
                    val shakeX: Float
                    if (isDispensing) {
                        when {
                            t < 0.12f -> {
                                scaleFX = 0.96f; scaleFY = 1.05f; shakeX = 0f
                            }
                            t < 0.36f -> {
                                scaleFX = 1.08f; scaleFY = 0.99f; shakeX = 0f
                            }
                            t < 0.48f -> {
                                scaleFX = 1.03f; scaleFY = 0.99f; shakeX = sin(t * 60f) * 0.6f
                            }
                            else -> {
                                scaleFX = 1f; scaleFY = 1f; shakeX = 0f
                            }
                        }
                    } else if (countNum > 0) {
                        scaleFX = 1f; scaleFY = 1f; shakeX = 0f
                    } else {
                        scaleFX = 0.95f; scaleFY = 0.95f; shakeX = 0f
                    }

                    val dispW = 47f * scaleFX
                    val dispH = 44f * scaleFY
                    val dispCy = cy - 6.5f
                    val dispRect = RectF(
                        cx + shakeX - dispW / 2f,
                        dispCy - dispH / 2f,
                        cx + shakeX + dispW / 2f,
                        dispCy + dispH / 2f
                    )

                    if (isDispensing && t in 0.06f..0.44f) {
                        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.parseColor("#5538BDF8")
                        }
                        canvas.drawOval(
                            RectF(cx - 16f, dispCy - 12f, cx + 16f, dispCy + 18f),
                            glowPaint
                        )
                    }

                    val dispBmp = AssetLoader.getDispenserHoodFrame(context, hoodFrameIndex)
                        ?: AssetLoader.getCrystalCaveFrame(context, cell.direction, "idle")
                    if (dispBmp != null) {
                        val dispPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                            isDither = true
                            alpha = if (countNum <= 0 && !isDispensing) 175 else 255
                        }
                        val dirAngle = when (cell.direction) {
                            DispenserDirection.DOWN -> 0f
                            DispenserDirection.RIGHT -> -90f
                            DispenserDirection.LEFT -> 90f
                        }
                        canvas.save()
                        if (dirAngle != 0f) {
                            canvas.rotate(dirAngle, cx, cy)
                        }
                        canvas.drawBitmap(dispBmp, null, dispRect, dispPaint)
                        canvas.restore()
                    }

                    // Bold outlined counter digit centered on the blue dome
                    if (countNum > 0) {
                        val badgeCx = when (cell.direction) {
                            DispenserDirection.DOWN -> cx + shakeX
                            DispenserDirection.RIGHT -> cx - 4.0f
                            DispenserDirection.LEFT -> cx + 4.0f
                        }
                        val badgeCy = when (cell.direction) {
                            DispenserDirection.DOWN -> cy + 0.5f
                            DispenserDirection.RIGHT, DispenserDirection.LEFT -> cy + 4.5f
                        }
                        val countOutlinePaint = Paint(textPaint).apply {
                            textSize = 13.5f
                            style = Paint.Style.STROKE
                            strokeWidth = 3.2f
                            strokeJoin = Paint.Join.ROUND
                            color = android.graphics.Color.parseColor("#FF0B1528")
                        }
                        val countFillPaint = Paint(textPaint).apply {
                            textSize = 13.5f
                            style = Paint.Style.FILL
                            color = android.graphics.Color.WHITE
                        }
                        canvas.drawText("$countNum", badgeCx, badgeCy, countOutlinePaint)
                        canvas.drawText("$countNum", badgeCx, badgeCy, countFillPaint)
                    }
                }
            }

            // Connected-pair dual-color pill bridge overlay
            fun linkPillArgb(cell: GridCell): Int {
                return if (cell.isMystery) {
                    android.graphics.Color.parseColor("#7A8B99")
                } else {
                    GameConstants.COLOR_MAP[cell.color ?: MarbleColor.PURPLE]?.hex?.toArgb()
                        ?: android.graphics.Color.WHITE
                }
            }
            val pillWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                style = Paint.Style.FILL
            }
            val pillOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#44000000")
                style = Paint.Style.STROKE
                strokeWidth = 0.8f
            }
            val pillHalfPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
            }
            val pillDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                strokeWidth = 1.0f
            }
            for (r in 0 until GameConstants.GRID_ROWS) {
                for (c in 0 until GameConstants.GRID_COLS) {
                    val cell = grid[r][c]
                    val group = cell.linkGroup ?: continue
                    if (!isCrateRenderable(cell)) continue
                    val cx = GameConstants.GRID_X_CENTERS[c] + cell.offsetX
                    val cy = GameConstants.GRID_Y_CENTERS[r] + cell.offsetY

                    if (c + 1 < GameConstants.GRID_COLS) {
                        val right = grid[r][c + 1]
                        if (right.linkGroup == group && isCrateRenderable(right)) {
                            val mx = (cx + GameConstants.GRID_X_CENTERS[c + 1] + right.offsetX) * 0.5f
                            val my = (cy + GameConstants.GRID_Y_CENTERS[r] + right.offsetY) * 0.5f
                            val outer = RectF(mx - 5.4f, my - 3.7f, mx + 5.4f, my + 3.7f)
                            val inner = RectF(mx - 4.3f, my - 2.6f, mx + 4.3f, my + 2.6f)
                            canvas.drawRoundRect(outer, 3.7f, 3.7f, pillWhitePaint)
                            canvas.drawRoundRect(outer, 3.7f, 3.7f, pillOutlinePaint)
                            val innerPath = Path().apply { addRoundRect(inner, 2.6f, 2.6f, Path.Direction.CW) }
                            canvas.save()
                            canvas.clipPath(innerPath)
                            pillHalfPaint.color = linkPillArgb(cell)
                            canvas.drawRect(inner.left, inner.top, mx, inner.bottom, pillHalfPaint)
                            pillHalfPaint.color = linkPillArgb(right)
                            canvas.drawRect(mx, inner.top, inner.right, inner.bottom, pillHalfPaint)
                            canvas.drawLine(mx, inner.top, mx, inner.bottom, pillDividerPaint)
                            canvas.restore()
                        }
                    }

                    if (r + 1 < GameConstants.GRID_ROWS) {
                        val below = grid[r + 1][c]
                        if (below.linkGroup == group && isCrateRenderable(below)) {
                            val mx = (cx + GameConstants.GRID_X_CENTERS[c] + below.offsetX) * 0.5f
                            val my = (cy + GameConstants.GRID_Y_CENTERS[r + 1] + below.offsetY) * 0.5f
                            val outer = RectF(mx - 4.1f, my - 4.9f, mx + 4.1f, my + 4.9f)
                            val inner = RectF(mx - 3.0f, my - 3.8f, mx + 3.0f, my + 3.8f)
                            canvas.drawRoundRect(outer, 4.1f, 4.1f, pillWhitePaint)
                            canvas.drawRoundRect(outer, 4.1f, 4.1f, pillOutlinePaint)
                            val innerPath = Path().apply { addRoundRect(inner, 3.0f, 3.0f, Path.Direction.CW) }
                            canvas.save()
                            canvas.clipPath(innerPath)
                            pillHalfPaint.color = linkPillArgb(cell)
                            canvas.drawRect(inner.left, inner.top, inner.right, my, pillHalfPaint)
                            pillHalfPaint.color = linkPillArgb(below)
                            canvas.drawRect(inner.left, my, inner.right, inner.bottom, pillHalfPaint)
                            canvas.drawLine(inner.left, my, inner.right, my, pillDividerPaint)
                            canvas.restore()
                        }
                    }
                }
            }

            // Funnel Physics Balls - Rendered as animated Emoticon Fluffies!
            for (b in physics.balls) {
                val fluffyBmp = AssetLoader.getFluffy(context, b.color, b.mood)
                val r = b.radius * 1.35f
                if (fluffyBmp != null) {
                    canvas.drawBitmap(
                        fluffyBmp,
                        null,
                        RectF(b.x - r, b.y - r, b.x + r, b.y + r),
                        null
                    )
                }
            }

            // Funnel Waiting Capacity Indicator (shows when near full or locked)
            val waitingCount = physics.balls.size
            if (isFunnelLocked || funnelFullAlertTimer > 0f || waitingCount >= (maxWaitingBalls - 8)) {
                val pillColor = if (isFunnelLocked) {
                    android.graphics.Color.parseColor("#E93D5A")
                } else {
                    android.graphics.Color.parseColor("#0B5FA7")
                }
                val pillBorderColor = if (isFunnelLocked) {
                    android.graphics.Color.parseColor("#FFD700")
                } else {
                    android.graphics.Color.parseColor("#8CE4FD")
                }
                val pillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = pillColor
                    alpha = 225
                    style = Paint.Style.FILL
                }
                val pillStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = pillBorderColor
                    style = Paint.Style.STROKE
                    strokeWidth = 1.4f
                }
                val pillW = if (isFunnelLocked || funnelFullAlertTimer > 0f) 76f else 42f
                val pillRect = RectF(264f - pillW, 405f, 264f, 421f)
                canvas.drawRoundRect(pillRect, 8f, 8f, pillBgPaint)
                canvas.drawRoundRect(pillRect, 8f, 8f, pillStrokePaint)
                textPaint.textSize = 8.5f
                textPaint.color = android.graphics.Color.WHITE
                val label = if (isFunnelLocked || funnelFullAlertTimer > 0f) {
                    "FULL $waitingCount/$maxWaitingBalls"
                } else {
                    "$waitingCount/$maxWaitingBalls"
                }
                canvas.drawText(label, pillRect.centerX(), pillRect.centerY() + 3f, textPaint)
            }

            // Conveyor Belt Stadium Track Rendering (Matching exact conveyorRenderer.ts)
            val beltCenterY = (GameConstants.BELT_TOP_Y + GameConstants.BELT_BOTTOM_Y) / 2f // 474
            val capsuleX = 36f
            val capsuleW = 224f
            val capsuleY = beltCenterY - 22f
            val capsuleH = 44f
            val capsuleRx = 22f

            // 1. Stadium Capsule Fill with linear gradient
            val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    capsuleX, capsuleY, capsuleX, capsuleY + capsuleH,
                    android.graphics.Color.parseColor("#947E9F"),
                    android.graphics.Color.parseColor("#705979"),
                    Shader.TileMode.CLAMP
                )
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(RectF(capsuleX, capsuleY, capsuleX + capsuleW, capsuleY + capsuleH), capsuleRx, capsuleRx, trackPaint)

            // 2. Thick clean white outer capsule border rim (subtle cyan speed glow when upper grid is cleared)
            val isBeltBoostedVisual = currentBeltSpeed > GameConstants.BELT_SPEED * 1.15f
            if (isBeltBoostedVisual) {
                val boostGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.parseColor("#4D5CF2FF")
                    style = Paint.Style.STROKE
                    strokeWidth = 7.5f
                }
                canvas.drawRoundRect(RectF(capsuleX, capsuleY, capsuleX + capsuleW, capsuleY + capsuleH), capsuleRx, capsuleRx, boostGlowPaint)
            }
            val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isBeltBoostedVisual) {
                    android.graphics.Color.parseColor("#B8FAFF")
                } else {
                    android.graphics.Color.parseColor("#E5EBFF")
                }
                style = Paint.Style.STROKE
                strokeWidth = 3.5f
            }
            canvas.drawRoundRect(RectF(capsuleX, capsuleY, capsuleX + capsuleW, capsuleY + capsuleH), capsuleRx, capsuleRx, rimPaint)

            // 3. Crisp white center slot pill divider
            val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(RectF(58f, beltCenterY - 1.75f, 238f, beltCenterY + 1.75f), 1.75f, 1.75f, dividerPaint)

            // 4. 24 Dark Dusty Purple Circular Socket Indentations (#5E4E6C)
            val socketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#5E4E6C")
                style = Paint.Style.FILL
            }
            val socketBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#4B3C58")
                style = Paint.Style.STROKE
                strokeWidth = 0.8f
            }
            val slotSpacing = BeltPath.TOTAL_PATH_LEN / beltCapacity
            val socketRadius = if (beltCapacity > 24) 6.5f else 8.2f
            for (i in 0 until beltCapacity) {
                val s = (i * slotSpacing + beltScrollDistance) % BeltPath.TOTAL_PATH_LEN
                val coords = BeltPath.getBeltCoordinates(s)
                canvas.drawCircle(coords.x, coords.y, socketRadius, socketPaint)
                canvas.drawCircle(coords.x, coords.y, socketRadius, socketBorderPaint)
            }

            // 5. Conveyor Belt Fluffies with interactive animated expressions (winking, blinking, content)
            val beltBallScale = if (beltCapacity > 24) 0.85f else 1f
            for (b in beltBalls) {
                val coords = BeltPath.getBeltCoordinates(b.s)
                val fluffyBmp = AssetLoader.getFluffy(context, b.color, b.mood)
                val r = GameConstants.BALL_RADIUS_BELT * 1.35f * b.scale * beltBallScale

                if (fluffyBmp != null) {
                    canvas.drawBitmap(
                        fluffyBmp,
                        null,
                        RectF(coords.x - r, coords.y - r, coords.x + r, coords.y + r),
                        null
                    )
                }
            }

            // Dropping Happy Fluffies
            for (d in droppingBalls) {
                val t = d.progress
                val curX = d.startX + (d.targetX - d.startX) * t
                val linearY = d.startY + (d.targetY - d.startY) * t
                val arcHeight = -14f * sin(t * PI.toFloat())
                val curY = linearY + arcHeight

                val fluffyBmp = AssetLoader.getFluffy(context, d.color, d.mood)
                val r = GameConstants.BALL_RADIUS_BELT * 1.35f * (beltBallScale + (1f - beltBallScale) * t)

                if (fluffyBmp != null) {
                    canvas.drawBitmap(
                        fluffyBmp,
                        null,
                        RectF(curX - r, curY - r, curX + r, curY + r),
                        null
                    )
                }
            }

            // Stacked Buckets in each of the 4 columns
            // Rendered via authentic 192x192 bucket atlases across all 16 colors
            val baltiW = 54f
            val baltiH = 54f

            for (col in columns) {
                val stack = col.bricks
                val count = stack.size
                if (count == 0) continue

                // A. Draw upcoming queued buckets behind the front active bucket
                for (idx in min(count - 1, 2) downTo 1) {
                    val queuedBrick = stack[idx]
                    val stackScale = 1f - idx * 0.12f
                    val stackOffsetY = -idx * 8f
                    val qW = baltiW * stackScale
                    val qH = baltiH * stackScale

                    val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        alpha = (190 * (1f - idx * 0.18f)).toInt()
                    }

                    val (sheetToUse, rowIndex) = AssetLoader.resolveBucketAtlas(context, queuedBrick.color)

                    if (sheetToUse != null) {
                        val qFrameIdx = AssetLoader.getBucketFrameIndex(queuedBrick.animState, queuedBrick.animTimer, queuedBrick.filled)
                        val srcRect = Rect(qFrameIdx * 192, rowIndex * 192, (qFrameIdx + 1) * 192, (rowIndex + 1) * 192)
                        val dstRectF = RectF(
                            col.x - qW / 2f,
                            GameConstants.BALTI_SOCKET_Y + stackOffsetY - qH / 2f,
                            col.x + qW / 2f,
                            GameConstants.BALTI_SOCKET_Y + stackOffsetY + qH / 2f
                        )
                        canvas.drawBitmap(sheetToUse, srcRect, dstRectF, dimPaint)
                    } else {
                        val queuedBmp = AssetLoader.getRailBucket(context, queuedBrick.color)
                        if (queuedBmp != null) {
                            canvas.drawBitmap(
                                queuedBmp,
                                null,
                                RectF(
                                    col.x - qW / 2f,
                                    GameConstants.BALTI_SOCKET_Y + stackOffsetY - qH / 2f,
                                    col.x + qW / 2f,
                                    GameConstants.BALTI_SOCKET_Y + stackOffsetY + qH / 2f
                                ),
                                dimPaint
                            )
                        }
                    }

                    // Show peeking Fluffies inside behind-buckets if Cannon Booster filled them!
                    if (queuedBrick.filled > 0 && queuedBrick.animState != "vanish") {
                        BucketFluffyRenderer.drawPeekingFluffies(
                            canvas = canvas,
                            context = context,
                            color = queuedBrick.color,
                            centerX = col.x,
                            bucketCenterY = GameConstants.BALTI_SOCKET_Y + stackOffsetY,
                            baltiSize = qW,
                            filledCount = queuedBrick.filled,
                            alpha = (dimPaint.alpha + 40).coerceIn(0, 235)
                        )
                    }
                }

                // B. Draw front active bucket using spritesheet atlas animation
                val frontBrick = stack[0]
                val (sheetFrontToUse, frontRowIndex) = AssetLoader.resolveBucketAtlas(context, frontBrick.color)

                if (sheetFrontToUse != null) {
                    val frameIndex = AssetLoader.getBucketFrameIndex(frontBrick.animState, frontBrick.animTimer, frontBrick.filled)
                    val srcRect = Rect(frameIndex * 192, frontRowIndex * 192, (frameIndex + 1) * 192, (frontRowIndex + 1) * 192)
                    val dstRectF = RectF(
                        col.x - baltiW / 2f,
                        GameConstants.BALTI_SOCKET_Y - baltiH / 2f,
                        col.x + baltiW / 2f,
                        GameConstants.BALTI_SOCKET_Y + baltiH / 2f
                    )
                    canvas.drawBitmap(sheetFrontToUse, srcRect, dstRectF, null)
                } else {
                    val frontBmp = AssetLoader.getRailBucket(context, frontBrick.color)
                    if (frontBmp != null) {
                        canvas.drawBitmap(
                            frontBmp,
                            null,
                            RectF(
                                col.x - GameConstants.BALTI_WIDTH / 2f,
                                GameConstants.BALTI_SOCKET_Y - GameConstants.BALTI_HEIGHT / 2f,
                                col.x + GameConstants.BALTI_WIDTH / 2f,
                                GameConstants.BALTI_SOCKET_Y + GameConstants.BALTI_HEIGHT / 2f
                            ),
                            null
                        )
                    }
                }

                // Cute Fluffies peeking comfortably inside bucket with illuminated contrast & blinking eyes
                if (frontBrick.filled > 0 && frontBrick.animState != "vanish") {
                    BucketFluffyRenderer.drawPeekingFluffies(
                        canvas = canvas,
                        context = context,
                        color = frontBrick.color,
                        centerX = col.x,
                        bucketCenterY = GameConstants.BALTI_SOCKET_Y,
                        baltiSize = baltiW,
                        filledCount = frontBrick.filled,
                        alpha = 255,
                        moodProvider = { h ->
                            val blinkCycle = ((System.currentTimeMillis() / 1000f) + h * 0.6f + col.x * 0.05f) % 2.8f
                            when {
                                blinkCycle < 0.45f -> "eyes_closed"
                                blinkCycle in 1.4f..1.7f -> "wink"
                                else -> "content"
                            }
                        }
                    )
                }

                // C. Remaining Queue Badge Indicator below the bucket
                val badgeY = GameConstants.BALTI_SOCKET_Y + 27f
                val badgeW = 32f
                val badgeH = 13f
                val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.parseColor("#D91D0A35")
                    style = Paint.Style.FILL
                }
                val badgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.parseColor("#F5D061")
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                }
                canvas.drawRoundRect(RectF(col.x - badgeW / 2f, badgeY, col.x + badgeW / 2f, badgeY + badgeH), 6f, 6f, badgePaint)
                canvas.drawRoundRect(RectF(col.x - badgeW / 2f, badgeY, col.x + badgeW / 2f, badgeY + badgeH), 6f, 6f, badgeBorderPaint)

                textPaint.textSize = 8.5f
                textPaint.color = if (count > 1) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#FFE494")
                val label = if (count > 1) "x$count" else "LAST"
                canvas.drawText(label, col.x, badgeY + 9.5f, textPaint)
            }

            // Fluffies joyfully flying out of completed buckets into the air!
            for (eff in fluffyEffects) {
                val fluffyBmp = AssetLoader.getFluffy(context, eff.color, "happy_open")
                if (fluffyBmp != null) {
                    val r = 12.5f * eff.scale
                    val effPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        alpha = (eff.alpha * 255).toInt().coerceIn(0, 255)
                    }
                    canvas.drawBitmap(
                        fluffyBmp,
                        null,
                        RectF(eff.x - r, eff.y - r, eff.x + r, eff.y + r),
                        effPaint
                    )
                }
            }

            // Magnet Returning Balls (lifting out of chain and fitting smoothly into upper containers)
            for (mb in magnetReturningBalls) {
                if (mb.elapsed < mb.delay) {
                    val wobble = sin(mb.elapsed * 42f) * 1.2f
                    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.parseColor("#9900F0FF")
                        style = Paint.Style.STROKE
                        strokeWidth = 1.6f
                    }
                    val beltR = GameConstants.BALL_RADIUS_BELT * 1.35f
                    canvas.drawCircle(mb.startX + wobble, mb.startY, beltR + 2f, haloPaint)
                    val fluffyBmp = AssetLoader.getFluffy(context, mb.color, "surprised")
                    if (fluffyBmp != null) {
                        canvas.drawBitmap(
                            fluffyBmp,
                            null,
                            RectF(
                                mb.startX + wobble - beltR,
                                mb.startY - beltR,
                                mb.startX + wobble + beltR,
                                mb.startY + beltR
                            ),
                            null
                        )
                    }
                } else {
                    val t = ((mb.elapsed - mb.delay) / mb.duration).coerceIn(0f, 1f)
                    val e = 1f - (1f - t) * (1f - t) * (1f - t)

                    fun bezierAt(u: Float): Pair<Float, Float> {
                        val inv = 1f - u
                        val px = inv * inv * mb.startX + 2f * inv * u * mb.controlX + u * u * mb.targetX
                        val py = inv * inv * mb.startY + 2f * inv * u * mb.controlY + u * u * mb.targetY
                        return Pair(px, py)
                    }

                    // Glowing magnetic flight trail
                    for (trailIdx in 1..2) {
                        val trailE = (e - trailIdx * 0.08f).coerceAtLeast(0f)
                        if (trailE > 0f) {
                            val (tx, ty) = bezierAt(trailE)
                            val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = android.graphics.Color.parseColor("#7CE7FF")
                                alpha = (130 / trailIdx)
                            }
                            canvas.drawCircle(tx, ty, 3.8f - trailIdx * 0.8f, trailPaint)
                        }
                    }

                    val (curX, curY) = bezierAt(e)
                    val startR = GameConstants.BALL_RADIUS_BELT * 1.35f
                    val endR = 0.154f * 33f
                    val curR = startR + (endR - startR) * e

                    // Soft cyan aura around flying ball
                    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.parseColor("#6600F0FF")
                    }
                    canvas.drawCircle(curX, curY, curR + 2.2f * (1f - e), glowPaint)

                    val sprite = if (e < 0.72f) {
                        AssetLoader.getFluffy(context, mb.color, "happy_open")
                            ?: AssetLoader.getBall(context, mb.color)
                    } else {
                        AssetLoader.getFluffy(context, mb.color, "content")
                            ?: AssetLoader.getBall(context, mb.color)
                    }
                    if (sprite != null) {
                        canvas.drawBitmap(
                            sprite,
                            null,
                            RectF(curX - curR, curY - curR, curX + curR, curY + curR),
                            null
                        )
                    }
                }
            }

            // Particles
            for (p in particles) {
                val pPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = (p.color.toInt() and 0x00FFFFFF) or ((p.alpha * 255).toInt() shl 24)
                }
                canvas.drawCircle(p.x, p.y, p.size, pPaint)
            }

            // AAA Center-Screen Power-Up Surge (Big Sky-Blue Magnet / Shuffle + Volumetric Lighting & Lightning)
            val overlayType = activePowerupOverlayType
            if (overlayType != null && powerupOverlayTimer > 0f) {
                val p = (1f - powerupOverlayTimer / powerupOverlayDuration).coerceIn(0f, 1f)
                val centerX = 148f
                val centerY = 342f
                val isMagnetOverlay = overlayType == "magnet"

                val overlayScale = when {
                    p < 0.15f -> {
                        // Elastic back-out pop-in
                        val t = p / 0.15f
                        val inv = 1f - t
                        0.20f + 0.98f * (1f - inv * inv * inv)
                    }
                    p < 0.27f -> {
                        val t = (p - 0.15f) / 0.12f
                        1.18f - 0.18f * t
                    }
                    p < 0.78f -> {
                        1.0f + sin(p * 28f) * 0.045f
                    }
                    else -> {
                        // Heroic subtle expansion as it dissolves into light
                        val t = (p - 0.78f) / 0.22f
                        1.0f + 0.18f * t
                    }
                }

                val overlayAlpha = when {
                    p < 0.09f -> (p / 0.09f)
                    p > 0.78f -> ((1f - p) / 0.22f)
                    else -> 1f
                }.coerceIn(0f, 1f)

                val primaryGlowHex = if (isMagnetOverlay) "#00E5FF" else "#FFE15A"
                val secondaryGlowHex = if (isMagnetOverlay) "#38BDF8" else "#00D4FF"

                // 0. Subtle cinematic focus vignette across the arena so sky-blue & gold lighting pops
                val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        centerX,
                        centerY,
                        250f,
                        intArrayOf(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.parseColor("#3D061A3A"),
                            android.graphics.Color.parseColor("#75041129")
                        ),
                        floatArrayOf(0.28f, 0.68f, 1f),
                        Shader.TileMode.CLAMP
                    )
                    alpha = (210 * overlayAlpha).toInt().coerceIn(0, 255)
                }
                canvas.drawRect(-12f, -16f, 308f, 676f, vignettePaint)

                // Brief opening light flash pulse (first 0.14s)
                if (p < 0.14f) {
                    val flashAlpha = (1f - p / 0.14f)
                    val flashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.parseColor("#E0F8FF")
                        alpha = (85 * flashAlpha * flashAlpha).toInt().coerceIn(0, 255)
                    }
                    canvas.drawCircle(centerX, centerY, 150f * (0.6f + p * 3f), flashPaint)
                }

                // 1. Rotating Volumetric God-Ray Beams behind the centerpiece
                val rayCount = 8
                val baseRayRot = p * 0.95f
                for (rIdx in 0 until rayCount) {
                    val rayAngle = baseRayRot + rIdx * (2f * PI.toFloat() / rayCount)
                    val rayLen = (118f + (rIdx % 2) * 26f) * (0.85f + 0.15f * overlayScale)
                    val halfSpread = if (rIdx % 2 == 0) 0.13f else 0.09f
                    val rayPath = android.graphics.Path().apply {
                        moveTo(centerX, centerY)
                        lineTo(
                            centerX + cos(rayAngle - halfSpread) * rayLen,
                            centerY + sin(rayAngle - halfSpread) * rayLen
                        )
                        lineTo(
                            centerX + cos(rayAngle + halfSpread) * rayLen,
                            centerY + sin(rayAngle + halfSpread) * rayLen
                        )
                        close()
                    }
                    val rayColor = if (rIdx % 2 == 0) primaryGlowHex else "#FFFFFF"
                    val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = RadialGradient(
                            centerX,
                            centerY,
                            rayLen.coerceAtLeast(1f),
                            intArrayOf(
                                android.graphics.Color.parseColor(rayColor),
                                android.graphics.Color.TRANSPARENT
                            ),
                            floatArrayOf(0.15f, 1f),
                            Shader.TileMode.CLAMP
                        )
                        alpha = ((if (rIdx % 2 == 0) 115 else 85) * overlayAlpha).toInt().coerceIn(0, 255)
                    }
                    canvas.drawPath(rayPath, rayPaint)
                }

                // 2. Multi-Stop Luminous Radial Bloom
                val haloR = 114f * overlayScale
                val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        centerX,
                        centerY,
                        haloR.coerceAtLeast(1f),
                        intArrayOf(
                            android.graphics.Color.parseColor("#FFFFFF"),
                            android.graphics.Color.parseColor(primaryGlowHex),
                            android.graphics.Color.parseColor(secondaryGlowHex),
                            android.graphics.Color.TRANSPARENT
                        ),
                        floatArrayOf(0f, 0.25f, 0.56f, 1f),
                        Shader.TileMode.CLAMP
                    )
                    alpha = (210 * overlayAlpha).toInt().coerceIn(0, 255)
                }
                canvas.drawCircle(centerX, centerY, haloR, haloPaint)

                // 3. Triple Expanding Electric Shockwave Rings
                for (ringIdx in 0 until 3) {
                    val ringP = ((p * 1.40f) - ringIdx * 0.17f).coerceIn(0f, 1f)
                    if (ringP > 0f && ringP < 1f) {
                        val ringRadius = 26f + ringP * 124f
                        val ringHex = when (ringIdx) {
                            0 -> primaryGlowHex
                            1 -> "#FFFFFF"
                            else -> secondaryGlowHex
                        }
                        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.parseColor(ringHex)
                            alpha = ((1f - ringP) * 225 * overlayAlpha).toInt().coerceIn(0, 255)
                            style = Paint.Style.STROKE
                            strokeWidth = (3.8f - ringIdx * 0.6f) * (1f - ringP * 0.45f)
                        }
                        canvas.drawCircle(centerX, centerY, ringRadius, ringPaint)
                    }
                }

                // 4. Magnetic Energy Tethers shooting down to returning balls (when Magnet is pulling)
                if (isMagnetOverlay && magnetReturningBalls.isNotEmpty()) {
                    val tetherOuter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.parseColor("#00E5FF")
                        alpha = (165 * overlayAlpha).toInt().coerceIn(0, 255)
                        style = Paint.Style.STROKE
                        strokeWidth = 3.2f
                        strokeCap = Paint.Cap.ROUND
                    }
                    val tetherCore = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.WHITE
                        alpha = (210 * overlayAlpha).toInt().coerceIn(0, 255)
                        style = Paint.Style.STROKE
                        strokeWidth = 1.2f
                        strokeCap = Paint.Cap.ROUND
                    }
                    val maxTethers = minOf(6, magnetReturningBalls.size)
                    for (tIdx in 0 until maxTethers) {
                        val mb = magnetReturningBalls[tIdx]
                        val e = ((mb.elapsed - mb.delay) / mb.duration).coerceIn(0f, 1f)
                        if (e < 0.85f) {
                            val inv = 1f - e
                            val bx = inv * inv * mb.startX + 2f * inv * e * mb.controlX + e * e * mb.targetX
                            val by = inv * inv * mb.startY + 2f * inv * e * mb.controlY + e * e * mb.targetY
                            val arcPath = android.graphics.Path().apply {
                                moveTo(centerX, centerY + 12f)
                                quadTo(
                                    (centerX + bx) * 0.5f + if (tIdx % 2 == 0) -18f else 18f,
                                    (centerY + by) * 0.5f,
                                    bx,
                                    by
                                )
                            }
                            canvas.drawPath(arcPath, tetherOuter)
                            canvas.drawPath(arcPath, tetherCore)
                        }
                    }
                }

                // 5. Branched Jagged Electric Lightning Bolts ("Bijlia") crackling outward
                val boltPhase = (p * 24f).toInt()
                val boltOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.parseColor(primaryGlowHex)
                    alpha = (225 * overlayAlpha).toInt().coerceIn(0, 255)
                    style = Paint.Style.STROKE
                    strokeWidth = 4.4f
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
                val boltCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.WHITE
                    alpha = (255 * overlayAlpha).toInt().coerceIn(0, 255)
                    style = Paint.Style.STROKE
                    strokeWidth = 1.7f
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
                val sparkTipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.WHITE
                    alpha = (240 * overlayAlpha).toInt().coerceIn(0, 255)
                }

                val numBolts = 10
                val reachScale = sin(p.coerceIn(0.05f, 0.95f) * PI.toFloat()).coerceIn(0.25f, 1f)
                for (bIdx in 0 until numBolts) {
                    val rng = kotlin.random.Random(bIdx * 97 + boltPhase * 31)
                    val baseAngle = (bIdx * (2f * PI.toFloat() / numBolts)) + (rng.nextFloat() - 0.5f) * 0.28f
                    val startDist = 30f * overlayScale
                    val endDist = (84f + rng.nextFloat() * 62f) * reachScale

                    val perpX = -sin(baseAngle)
                    val perpY = cos(baseAngle)

                    val boltPath = android.graphics.Path()
                    val branchPath = android.graphics.Path()
                    val segments = 4
                    var tipX = centerX
                    var tipY = centerY
                    var hasBranch = false
                    for (seg in 0..segments) {
                        val frac = seg.toFloat() / segments
                        val dist = startDist + (endDist - startDist) * frac
                        val jitter = if (seg == 0) 0f else (rng.nextFloat() - 0.5f) * 20f * (0.4f + frac * 0.6f)
                        val px = centerX + cos(baseAngle) * dist + perpX * jitter
                        val py = centerY + sin(baseAngle) * dist + perpY * jitter
                        if (seg == 0) {
                            boltPath.moveTo(px, py)
                        } else {
                            boltPath.lineTo(px, py)
                            if (seg == 2 && bIdx % 2 == 0) {
                                hasBranch = true
                                val forkAngle = baseAngle + (if (rng.nextBoolean()) 0.48f else -0.48f)
                                val forkLen = 22f + rng.nextFloat() * 14f
                                branchPath.moveTo(px, py)
                                branchPath.lineTo(
                                    px + cos(forkAngle) * (forkLen * 0.5f),
                                    py + sin(forkAngle) * (forkLen * 0.5f)
                                )
                                branchPath.lineTo(
                                    px + cos(forkAngle + 0.18f) * forkLen,
                                    py + sin(forkAngle + 0.18f) * forkLen
                                )
                            }
                        }
                        tipX = px
                        tipY = py
                    }

                    canvas.drawPath(boltPath, boltOuterPaint)
                    canvas.drawPath(boltPath, boltCorePaint)
                    if (hasBranch) {
                        canvas.drawPath(branchPath, boltOuterPaint)
                        canvas.drawPath(branchPath, boltCorePaint)
                    }
                    canvas.drawCircle(tipX, tipY, 2.3f, sparkTipPaint)
                }

                // 6. Big AAA Centerpiece Animated Sprite (5 progressive frames with smooth crossfade & hero tilt)
                val fxFrames = if (isMagnetOverlay) magnetFxFrames else shuffleFxFrames
                val fallbackBtn = if (isMagnetOverlay) magnetBtnFrames.firstOrNull() else shuffleBtnFrames.firstOrNull()
                val halfIcon = 72f * overlayScale
                val iconRect = RectF(
                    -halfIcon,
                    -halfIcon,
                    halfIcon,
                    halfIcon
                )
                val tiltDeg = if (isMagnetOverlay) {
                    sin(p * 16f) * 4.5f
                } else {
                    sin(p * PI.toFloat()) * 16f * (if (p < 0.5f) 1f else -0.6f)
                }

                canvas.save()
                canvas.translate(centerX, centerY + if (isMagnetOverlay) sin(p * 12f) * 3f else 0f)
                canvas.rotate(tiltDeg)

                if (fxFrames.isNotEmpty()) {
                    // Progressive animation from fx_0 (vortex start) -> fx_4 (peak starburst/cyclone)
                    val frameFloat = (p * (fxFrames.size - 0.15f)).coerceIn(0f, (fxFrames.size - 1).toFloat())
                    val f0 = frameFloat.toInt().coerceIn(0, fxFrames.size - 1)
                    val f1 = (f0 + 1).coerceIn(0, fxFrames.size - 1)
                    val blend = (frameFloat - f0).coerceIn(0f, 1f)

                    val p0 = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                        alpha = ((1f - blend * 0.85f) * 255f * overlayAlpha).toInt().coerceIn(0, 255)
                    }
                    canvas.drawBitmap(fxFrames[f0], null, iconRect, p0)

                    if (f1 != f0 && blend > 0.05f) {
                        val p1 = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                            alpha = (blend * 255f * overlayAlpha).toInt().coerceIn(0, 255)
                        }
                        canvas.drawBitmap(fxFrames[f1], null, iconRect, p1)
                    }
                } else if (fallbackBtn != null) {
                    val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                        alpha = (255 * overlayAlpha).toInt().coerceIn(0, 255)
                    }
                    canvas.drawBitmap(fallbackBtn, null, iconRect, iconPaint)
                }
                canvas.restore()
            }

            // Cannon Booster Full 6-Stage Sequence (Entrance -> Suction into Bowl -> Blue Charge & Aim -> Blast/Recoil -> Smoke -> Exit)
            if (isCannonActive || cannonProjectiles.isNotEmpty() || cannonSmokePuffs.isNotEmpty()) {
                val t = cannonTimer
                val cannonAnchorX = 132f
                val cannonAnchorY = 348f
                val drawSize = 176f

                // Entrance (0..0.38s), Active (0.38..2.75s), Exit (2.75..3.15s)
                val enterExitScale = when {
                    t < 0.32f -> {
                        val p = (t / 0.32f).coerceIn(0f, 1f)
                        val inv = 1f - p
                        0.20f + 0.92f * (1f - inv * inv * inv)
                    }
                    t < 0.44f -> {
                        val p = (t - 0.32f) / 0.12f
                        1.12f - 0.12f * p
                    }
                    t > 2.75f -> {
                        val p = ((t - 2.75f) / 0.40f).coerceIn(0f, 1f)
                        1.0f - 0.80f * (p * p)
                    }
                    else -> 1.0f
                }

                val cannonAlpha = when {
                    t < 0.12f -> t / 0.12f
                    t > 2.82f -> (1f - (t - 2.82f) / 0.33f)
                    else -> 1f
                }.coerceIn(0f, 1f)

                // Aiming tilt toward bucket firing direction (ramps during charge 1.15..1.72, holds during fire, relaxes in smoke)
                val currentAimDeg = when {
                    t < 1.10f -> 0f
                    t < 1.68f -> {
                        val p = (t - 1.10f) / 0.58f
                        val ease = p * p * (3f - 2f * p)
                        cannonAimAngleDeg * ease
                    }
                    t < 2.25f -> cannonAimAngleDeg
                    t < 2.75f -> {
                        val p = (t - 2.25f) / 0.50f
                        cannonAimAngleDeg * (1f - p)
                    }
                    else -> 0f
                }

                // Recoil kick backward-up-left right at cannonFireMoment (1.72..2.06s)
                val recoilAmt = if (t in cannonFireMoment..(cannonFireMoment + 0.34f)) {
                    val rp = (t - cannonFireMoment) / 0.34f
                    sin(rp * PI.toFloat()) * (1f - rp * 0.45f)
                } else 0f
                val aimRad = currentAimDeg * (PI.toFloat() / 180f)
                val cx = cannonAnchorX - cos(aimRad) * 14f * recoilAmt
                val cy = cannonAnchorY - sin(aimRad) * 10f * recoilAmt - (1f - enterExitScale) * 18f

                if (isCannonActive && cannonAlpha > 0.01f) {
                    // Subtle arena vignette so cannon, bowl balls, and blue charge glow pop
                    val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = RadialGradient(
                            cx,
                            cy,
                            255f,
                            intArrayOf(
                                android.graphics.Color.TRANSPARENT,
                                android.graphics.Color.parseColor("#35061A3A"),
                                android.graphics.Color.parseColor("#68041129")
                            ),
                            floatArrayOf(0.30f, 0.70f, 1f),
                            Shader.TileMode.CLAMP
                        )
                        alpha = (195 * cannonAlpha).toInt().coerceIn(0, 255)
                    }
                    canvas.drawRect(-12f, -16f, 308f, 676f, vignettePaint)

                    // Portal vortex ring beneath cannon during entrance & charging
                    if (cannonFxRing != null && t < 1.85f) {
                        val ringAlpha = when {
                            t < 0.22f -> t / 0.22f
                            t > 1.65f -> (1f - (t - 1.65f) / 0.20f)
                            else -> 0.85f
                        }.coerceIn(0f, 1f) * cannonAlpha
                        val ringR = 54f * enterExitScale * (1f + 0.08f * sin(t * 9f))
                        canvas.save()
                        canvas.translate(cx, cy + 16f * enterExitScale)
                        canvas.scale(1f, 0.42f)
                        canvas.rotate(t * 140f)
                        val rPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                            alpha = (210 * ringAlpha).toInt().coerceIn(0, 255)
                        }
                        canvas.drawBitmap(cannonFxRing, null, RectF(-ringR, -ringR, ringR, ringR), rPaint)
                        canvas.restore()
                    }

                    // Compute world-space Glass Bowl Opening & Interior Center (aligned to norm (0.36, 0.235) and (0.36, 0.375))
                    val sScaled = drawSize * enterExitScale
                    val localBowlOpenX = (0.36f - 0.36f) * sScaled
                    val localBowlOpenY = (0.235f - 0.62f) * sScaled
                    val bowlOpenX = cx + localBowlOpenX * cos(aimRad) - localBowlOpenY * sin(aimRad)
                    val bowlOpenY = cy + localBowlOpenX * sin(aimRad) + localBowlOpenY * cos(aimRad)

                    val localBowlCenterX = (0.36f - 0.36f) * sScaled
                    val localBowlCenterY = (0.378f - 0.62f) * sScaled
                    val bowlCenterX = cx + localBowlCenterX * cos(aimRad) - localBowlCenterY * sin(aimRad)
                    val bowlCenterY = cy + localBowlCenterX * sin(aimRad) + localBowlCenterY * cos(aimRad)

                    // Charging aura intensity (ramps 0.95s..1.72s)
                    val chargeIntensity = if (t in 0.95f..1.76f) {
                        ((t - 0.95f) / 0.77f).coerceIn(0f, 1f)
                    } else 0f

                    // 1. Draw collected REAL game balls already INSIDE the transparent glass bowl (BEHIND the glass dome sprite!)
                    val flyingBalls = mutableListOf<Pair<CannonCollectedBall, Float>>()
                    val totalCollected = cannonCollectedBalls.size.coerceAtLeast(1)
                    cannonCollectedBalls.forEachIndexed { idx, cb ->
                        val flyStart = 0.24f + cb.delay
                        val flyDur = 0.44f
                        val flyP = ((t - flyStart) / flyDur).coerceIn(0f, 1f)
                        if (flyP < 1f) {
                            flyingBalls.add(cb to flyP)
                        } else {
                            // Ball is inside the glass bowl: stack & swirl organically within the bowl cavity!
                            val colFrac = idx.toFloat() / totalCollected
                            val swirlSpeed = 2.4f + chargeIntensity * 9.5f
                            val ang = cb.swirlSeed + t * swirlSpeed
                            val maxRx = 16.5f * enterExitScale
                            val maxRy = 13.0f * enterExitScale
                            val ringFrac = 0.25f + 0.72f * ((idx % 5) / 4f)
                            val bx = bowlCenterX + cos(ang) * maxRx * ringFrac
                            val by = bowlCenterY + (0.28f - colFrac * 0.45f) * maxRy + sin(ang * 1.3f) * maxRy * 0.32f
                            val br = 7.4f * enterExitScale

                            val ballSprite = AssetLoader.getFluffy(
                                context,
                                cb.color,
                                if (chargeIntensity > 0.35f) "surprised" else "happy_open"
                            ) ?: AssetLoader.getBall(context, cb.color)
                            if (ballSprite != null) {
                                val bPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                                    alpha = (255 * cannonAlpha).toInt().coerceIn(0, 255)
                                }
                                canvas.drawBitmap(ballSprite, null, RectF(bx - br, by - br, bx + br, by + br), bPaint)
                            }
                        }
                    }

                    // 2. Blue electric charging glow behind cannon during charge phase
                    if (chargeIntensity > 0.01f) {
                        val glowR = 84f * enterExitScale * (0.85f + 0.25f * chargeIntensity)
                        val cGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            shader = RadialGradient(
                                cx,
                                cy - 16f,
                                glowR.coerceAtLeast(1f),
                                intArrayOf(
                                    android.graphics.Color.parseColor("#CC38E8FF"),
                                    android.graphics.Color.parseColor("#6600B4FF"),
                                    android.graphics.Color.TRANSPARENT
                                ),
                                floatArrayOf(0f, 0.55f, 1f),
                                Shader.TileMode.CLAMP
                            )
                            alpha = (230 * chargeIntensity * cannonAlpha).toInt().coerceIn(0, 255)
                        }
                        canvas.drawCircle(cx, cy - 16f, glowR, cGlow)
                    }

                    // 3. Draw Cannon Sprite Frame (semi-transparent glass bowl naturally shows the real balls inside!)
                    val cannonBmp: Bitmap? = when {
                        t < 0.96f -> cannonIdleBitmap
                        t < 1.72f -> {
                            val cp = ((t - 0.96f) / 0.76f).coerceIn(0f, 0.999f)
                            val fIdx = (cp * cannonChargeFrames.size).toInt().coerceIn(0, cannonChargeFrames.lastIndex)
                            cannonChargeFrames.getOrNull(fIdx) ?: cannonIdleBitmap
                        }
                        t < 1.84f -> cannonFireFrames.getOrNull(0) ?: cannonFireFrames.lastOrNull()
                        t < 2.08f -> cannonFireFrames.getOrNull(1) ?: cannonFireFrames.lastOrNull()
                        t < 2.82f -> {
                            val sp = ((t - 2.08f) / 0.74f).coerceIn(0f, 0.999f)
                            val fIdx = (sp * cannonSmokeFrames.size).toInt().coerceIn(0, cannonSmokeFrames.lastIndex)
                            cannonSmokeFrames.getOrNull(fIdx) ?: cannonIdleBitmap
                        }
                        else -> cannonIdleBitmap
                    }

                    if (cannonBmp != null) {
                        val left = -0.36f * sScaled
                        val top = -0.62f * sScaled
                        val dstRect = RectF(left, top, left + sScaled, top + sScaled)
                        val cPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                            alpha = (255 * cannonAlpha).toInt().coerceIn(0, 255)
                        }
                        canvas.save()
                        canvas.translate(cx, cy)
                        canvas.rotate(currentAimDeg)
                        canvas.drawBitmap(cannonBmp, null, dstRect, cPaint)
                        canvas.restore()
                    }

                    // 4. Extra blue charging sparks orbiting the cannon bowl
                    if (chargeIntensity > 0.05f && cannonFxSpark != null) {
                        val sparkCount = 6
                        for (sIdx in 0 until sparkCount) {
                            val sAng = t * 5.2f + sIdx * (2f * PI.toFloat() / sparkCount)
                            val sx = cx + cos(sAng) * 52f * enterExitScale
                            val sy = cy - 22f * enterExitScale + sin(sAng) * 38f * enterExitScale
                            val sr = (7f + 4f * chargeIntensity) * (0.7f + 0.3f * sin(t * 14f + sIdx))
                            val spPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                                alpha = (235 * chargeIntensity * cannonAlpha).toInt().coerceIn(0, 255)
                            }
                            canvas.drawBitmap(cannonFxSpark, null, RectF(sx - sr, sy - sr, sx + sr, sy + sr), spPaint)
                        }
                    }

                    // 5. Golden starburst muzzle flare & shockwave flash right at blast moment
                    if (t in cannonFireMoment..(cannonFireMoment + 0.34f)) {
                        val bp = (t - cannonFireMoment) / 0.34f
                        val localMuzzleX = (0.506f - 0.36f) * sScaled
                        val localMuzzleY = (0.577f - 0.62f) * sScaled
                        val mx = cx + localMuzzleX * cos(aimRad) - localMuzzleY * sin(aimRad)
                        val my = cy + localMuzzleX * sin(aimRad) + localMuzzleY * cos(aimRad)

                        val shockR = 24f + bp * 95f
                        val shockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.parseColor("#FFE15A")
                            alpha = ((1f - bp) * 210).toInt().coerceIn(0, 255)
                            style = Paint.Style.STROKE
                            strokeWidth = 3.5f * (1f - bp * 0.5f)
                        }
                        canvas.drawCircle(mx, my, shockR, shockPaint)

                        if (cannonFxFlash != null) {
                            val fR = (28f + 42f * sin(bp * PI.toFloat())) * enterExitScale
                            val fPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                                alpha = ((1f - bp) * 245).toInt().coerceIn(0, 255)
                            }
                            canvas.save()
                            canvas.translate(mx + cos(aimRad) * 14f, my + sin(aimRad) * 14f)
                            canvas.rotate(bp * 45f)
                            canvas.drawBitmap(cannonFxFlash, null, RectF(-fR, -fR, fR, fR), fPaint)
                            canvas.restore()
                        }
                    }

                    // 6. Real balls currently swooping from the conveyor belt INTO the top opening of the glass bowl
                    for ((cb, flyP) in flyingBalls) {
                        val ease = 1f - (1f - flyP) * (1f - flyP)
                        val midX = (cb.startX + bowlOpenX) * 0.5f
                        val ctrlX = midX + (if (cb.startX < bowlOpenX) -22f else 22f)
                        val ctrlY = minOf(cb.startY, bowlOpenY) - 48f
                        val inv = 1f - ease
                        val bx = inv * inv * cb.startX + 2f * inv * ease * ctrlX + ease * ease * bowlOpenX
                        val by = inv * inv * cb.startY + 2f * inv * ease * ctrlY + ease * ease * bowlOpenY
                        val br = GameConstants.BALL_RADIUS_BELT * (1.35f - 0.35f * ease)

                        // Suction energy ribbon behind incoming ball
                        val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.parseColor("#00E5FF")
                            alpha = (135 * (1f - ease * 0.4f)).toInt().coerceIn(0, 255)
                            style = Paint.Style.STROKE
                            strokeWidth = 2.2f
                        }
                        canvas.drawLine(bx, by, bowlOpenX, bowlOpenY, trailPaint)

                        val ballSprite = AssetLoader.getFluffy(context, cb.color, "surprised")
                            ?: AssetLoader.getBall(context, cb.color)
                        if (ballSprite != null) {
                            canvas.drawBitmap(ballSprite, null, RectF(bx - br, by - br, bx + br, by + br), null)
                        }
                    }
                }

                // 7. Billowing Cannon Smoke Puffs (isolated smoke sprite)
                if (cannonFxSmoke != null) {
                    for (puff in cannonSmokePuffs) {
                        val sp = (puff.life / puff.maxLife).coerceIn(0f, 1f)
                        val sr = 22f * puff.baseScale * (0.65f + sp * 0.95f)
                        val sPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                            alpha = ((1f - sp) * 215).toInt().coerceIn(0, 255)
                        }
                        canvas.drawBitmap(
                            cannonFxSmoke,
                            null,
                            RectF(puff.x - sr, puff.y - sr, puff.x + sr, puff.y + sr),
                            sPaint
                        )
                    }
                }

                // 8. Fired Cannon Projectiles blasting simultaneously into matching front & behind buckets!
                for (proj in cannonProjectiles) {
                    val e = ((proj.elapsed - proj.delay) / proj.duration).coerceIn(0f, 1f)
                    if (e <= 0f) continue
                    val inv = 1f - e
                    val px = inv * inv * proj.startX + 2f * inv * e * proj.controlX + e * e * proj.targetX
                    val py = inv * inv * proj.startY + 2f * inv * e * proj.controlY + e * e * proj.targetY

                    // Glowing fiery-cyan speed trail behind projectile
                    for (tr in 1..3) {
                        val te = (e - tr * 0.06f).coerceIn(0f, 1f)
                        val tinv = 1f - te
                        val tx = tinv * tinv * proj.startX + 2f * tinv * te * proj.controlX + te * te * proj.targetX
                        val ty = tinv * tinv * proj.startY + 2f * tinv * te * proj.controlY + te * te * proj.targetY
                        val trPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.parseColor(if (tr == 1) "#FFE15A" else "#00E5FF")
                            alpha = (200 - tr * 48).coerceIn(0, 255)
                        }
                        canvas.drawCircle(tx, ty, 5.2f - tr * 1.1f, trPaint)
                    }

                    val r = GameConstants.BALL_RADIUS_BELT * (1.28f - 0.15f * e)
                    val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.parseColor("#88FFE15A")
                    }
                    canvas.drawCircle(px, py, r + 2.5f, auraPaint)

                    val ballBmp = AssetLoader.getFluffy(context, proj.color, "happy_open")
                        ?: AssetLoader.getBall(context, proj.color)
                    if (ballBmp != null) {
                        canvas.drawBitmap(ballBmp, null, RectF(px - r, py - r, px + r, py + r), null)
                    }
                }
            }

            // Level 1 Animated Pressing Tutorial Hand
            if (currentLevel == 1 && !isTutorialDismissed && handCursorBitmap != null) {
                val tx = GameConstants.GRID_X_CENTERS[2]
                val ty = GameConstants.GRID_Y_CENTERS[5]

                // Exact 3.2s Keyframe-accurate Tutorial Animation from original web game
                val cycleMs = 3200L
                val timeInCycle = (System.currentTimeMillis() % cycleMs).toFloat()
                val progress = timeInCycle / cycleMs.toFloat()

                // Tutorial Hand Keyframes (0..10% fade in at 65,75; 26..34% at 0,0 scale 1; 40..50% press scale 0.84; 58..72% release; 90..100% fade out)
                var handAlpha = 1f
                var handX = 0f
                var handY = 0f
                var handScale = 1f

                when {
                    progress < 0.10f -> {
                        val p = progress / 0.10f
                        handAlpha = p
                        handX = 65f
                        handY = 75f
                        handScale = 1.15f
                    }
                    progress < 0.26f -> {
                        val p = (progress - 0.10f) / 0.16f
                        val ease = p * p * (3f - 2f * p)
                        handAlpha = 1f
                        handX = 65f * (1f - ease)
                        handY = 75f * (1f - ease)
                        handScale = 1.15f - ease * 0.15f
                    }
                    progress < 0.34f -> {
                        handAlpha = 1f
                        handX = 0f
                        handY = 0f
                        handScale = 1f
                    }
                    progress < 0.40f -> {
                        // Press down
                        val p = (progress - 0.34f) / 0.06f
                        handAlpha = 1f
                        handX = p * 2f
                        handY = p * 2f
                        handScale = 1f - p * 0.16f
                    }
                    progress < 0.50f -> {
                        // Held pressed
                        handAlpha = 1f
                        handX = 2f
                        handY = 2f
                        handScale = 0.84f
                    }
                    progress < 0.58f -> {
                        // Release press
                        val p = (progress - 0.50f) / 0.08f
                        handAlpha = 1f
                        handX = 2f * (1f - p)
                        handY = 2f * (1f - p)
                        handScale = 0.84f + p * 0.16f
                    }
                    progress < 0.72f -> {
                        handAlpha = 1f
                        handX = 0f
                        handY = 0f
                        handScale = 1f
                    }
                    progress < 0.90f -> {
                        val p = (progress - 0.72f) / 0.18f
                        handAlpha = 1f - p
                        handX = p * 65f
                        handY = p * 75f
                        handScale = 1f + p * 0.15f
                    }
                    else -> {
                        handAlpha = 0f
                        handX = 65f
                        handY = 75f
                        handScale = 1.15f
                    }
                }

                // Tutorial Ring Keyframes (0..38% hidden; 40% expands from 0.3x; 62% fades out at 1.7x)
                if (progress in 0.39f..0.63f) {
                    val ringProgress = ((progress - 0.39f) / 0.24f).coerceIn(0f, 1f)
                    val ringScale = 0.3f + ringProgress * 1.4f
                    val ringAlpha = (1f - ringProgress) * 0.95f
                    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.WHITE
                        alpha = (ringAlpha * 255).toInt().coerceIn(0, 255)
                        style = Paint.Style.STROKE
                        strokeWidth = 3f
                    }
                    canvas.drawCircle(tx, ty, 20f * ringScale, ringPaint)
                }

                // Render tutorial hand
                if (handAlpha > 0f) {
                    val handPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        alpha = (handAlpha * 255).toInt().coerceIn(0, 255)
                    }
                    canvas.save()
                    canvas.translate(tx + handX, ty + handY)
                    canvas.scale(handScale, handScale)
                    canvas.drawBitmap(
                        handCursorBitmap,
                        null,
                        RectF(-16f, -15f, 28f, 31f),
                        handPaint
                    )
                    canvas.restore()
                }
            }

            // AAA Animated Powerups (Magnet & Shuffle) at bottom of the arena
            val powerupY = 605f
            val ambientSec = (System.currentTimeMillis() % 60_000L) / 1000f

            fun drawPowerupButton(
                centerX: Float,
                centerY: Float,
                frames: List<Bitmap>,
                count: Int,
                animTimer: Float,
                shakeTimer: Float,
                isActiveGlow: Boolean,
                idleGlintOffsetSec: Float
            ) {
                val isAnimating = animTimer > 0f
                val animP = if (isAnimating) (1f - animTimer / powerupBtnAnimDuration).coerceIn(0f, 1f) else 0f

                // Press squish -> elastic overshoot -> settle, or gentle idle breathing
                val btnScale = when {
                    isAnimating && animP < 0.09f -> 1f - (animP / 0.09f) * 0.16f
                    isAnimating && animP < 0.26f -> {
                        val t = (animP - 0.09f) / 0.17f
                        0.84f + 0.34f * sin(t * PI.toFloat() * 0.5f)
                    }
                    isAnimating && animP < 0.48f -> {
                        val t = (animP - 0.26f) / 0.22f
                        1.18f - 0.18f * t
                    }
                    isAnimating -> 1.0f + sin(animP * 22f) * 0.03f
                    count > 0 -> 1.0f + 0.028f * sin((ambientSec + idleGlintOffsetSec) * 2.8f)
                    else -> 0.96f
                }

                val shakeX = if (shakeTimer > 0f) {
                    sin(shakeTimer * 48f) * 4.2f * (shakeTimer / 0.30f)
                } else 0f

                val bx = centerX + shakeX
                val by = centerY

                // Ambient glow aura behind active / available medal
                if (count > 0 || isActiveGlow || isAnimating) {
                    val auraScale = if (isAnimating) 1.28f else 1.05f
                    val auraR = 30f * auraScale
                    val auraAlpha = when {
                        isAnimating -> (210 * (1f - animP * 0.45f)).toInt()
                        isActiveGlow -> 175
                        else -> (80 + 30 * sin((ambientSec + idleGlintOffsetSec) * 2.8f)).toInt()
                    }.coerceIn(0, 255)
                    val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = RadialGradient(
                            bx,
                            by,
                            auraR,
                            intArrayOf(
                                android.graphics.Color.parseColor(if (isAnimating || isActiveGlow) "#00E5FF" else "#38BDF8"),
                                android.graphics.Color.TRANSPARENT
                            ),
                            floatArrayOf(0.35f, 1f),
                            Shader.TileMode.CLAMP
                        )
                        alpha = auraAlpha
                    }
                    canvas.drawCircle(bx, by, auraR, auraPaint)
                }

                // Expanding shockwave ring on button tap
                if (isAnimating || isActiveGlow) {
                    val ringP = if (isAnimating) animP else ((ambientSec * 1.4f) % 1f)
                    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.parseColor(if (ringP < 0.4f) "#FFE15A" else "#00E5FF")
                        alpha = ((1f - ringP) * 210).toInt().coerceIn(0, 255)
                        style = Paint.Style.STROKE
                        strokeWidth = 2.6f * (1f - ringP * 0.4f)
                    }
                    canvas.drawCircle(bx, by, 22f + ringP * 14f, ringPaint)
                }

                // Select progressive sprite frame (padded sprite gives room for outer rays & stars!)
                val frameIdx = when {
                    isAnimating && frames.size > 1 -> {
                        val seq = when (frames.size) {
                            6 -> intArrayOf(1, 2, 3, 4, 5, 5, 4, 2, 0)
                            5 -> intArrayOf(1, 2, 3, 4, 4, 3, 2, 0)
                            else -> intArrayOf(0)
                        }
                        val sIdx = (animP * seq.size).toInt().coerceIn(0, seq.lastIndex)
                        seq[sIdx].coerceIn(0, frames.lastIndex)
                    }
                    count > 0 && frames.size > 1 -> {
                        // Subtle periodic lightning/pop glint every ~4.2s on idle available button
                        val glintPhase = (ambientSec + idleGlintOffsetSec) % 4.2f
                        if (glintPhase < 0.22f) 1 else 0
                    }
                    else -> 0
                }

                val bmp = frames.getOrNull(frameIdx) ?: frames.firstOrNull()
                if (bmp != null) {
                    // Since sprites include outer star/ray margin around the center ring (288px canvas),
                    // draw at 31f half-size so the gold medal matches ~48dp while stars/rays burst outside!
                    val halfDraw = 31f * btnScale
                    val btnPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                        alpha = if (count > 0 || isAnimating) 255 else 135
                    }
                    canvas.drawBitmap(
                        bmp,
                        null,
                        RectF(bx - halfDraw, by - halfDraw, bx + halfDraw, by + halfDraw),
                        btnPaint
                    )
                }

                // Crisp high-contrast count badge at bottom-right of medal
                val badgeX = centerX + 20f + shakeX * 0.5f
                val badgeY = centerY + 19f
                val badgeShadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.parseColor("#55000000")
                }
                canvas.drawCircle(badgeX, badgeY + 1.2f, 9.2f, badgeShadow)

                val badgeFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.parseColor(
                        when {
                            isActiveGlow || isAnimating -> "#00C8FF"
                            count > 0 -> "#FF3B6B"
                            else -> "#64748B"
                        }
                    )
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(badgeX, badgeY, 8.8f, badgeFill)

                val badgeBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.WHITE
                    style = Paint.Style.STROKE
                    strokeWidth = 1.4f
                }
                canvas.drawCircle(badgeX, badgeY, 8.8f, badgeBorder)

                textPaint.textSize = 9.8f
                textPaint.color = android.graphics.Color.WHITE
                canvas.drawText("$count", badgeX, badgeY + 3.4f, textPaint)
            }

            // Magnet (left)
            drawPowerupButton(
                centerX = 88f,
                centerY = powerupY + 24f,
                frames = magnetBtnFrames,
                count = magnetCount,
                animTimer = magnetBtnAnimTimer,
                shakeTimer = magnetBtnShakeTimer,
                isActiveGlow = isMagnetActive,
                idleGlintOffsetSec = 0f
            )

            // Shuffle (center)
            drawPowerupButton(
                centerX = 148f,
                centerY = powerupY + 24f,
                frames = shuffleBtnFrames,
                count = shuffleCount,
                animTimer = shuffleBtnAnimTimer,
                shakeTimer = shuffleBtnShakeTimer,
                isActiveGlow = isShuffling,
                idleGlintOffsetSec = 1.4f
            )

            // Cannon Booster (3rd powerup, right)
            drawPowerupButton(
                centerX = 208f,
                centerY = powerupY + 24f,
                frames = cannonBtnFrames,
                count = cannonCount,
                animTimer = cannonBtnAnimTimer,
                shakeTimer = cannonBtnShakeTimer,
                isActiveGlow = isCannonActive,
                idleGlintOffsetSec = 2.8f
            )

            canvas.restore()

        }

        // Chain Full / Stuck Clean Up Overlay (sits under Top HUD so Pause & Coin Rack stay bright & clickable)
        if (gameState == "chain_stuck" && loseLifeWarningMode == null && !showLevelFailedModal) {
            val highlightedBalls = beltBalls.filter { cleanUpHighlightedIds.contains(it.id) }
            ChainFullCleanUpOverlay(
                coins = currentCoins,
                beltCapacity = beltCapacity,
                highlightedBeltBalls = highlightedBalls,
                viewScale = viewScale,
                viewOffsetX = viewOffsetX,
                viewArenaOffsetY = viewArenaOffsetY,
                grid = grid,
                funnelBalls = physics.balls.toList(),
                beltBalls = beltBalls.toList(),
                beltScrollDistance = beltScrollDistance,
                onWatchAdCleanUp = {
                    executeCleanUp()
                },
                onCoinsCleanUp = {
                    if (currentCoins >= CLEAN_UP_COIN_COST) {
                        currentCoins -= CLEAN_UP_COIN_COST
                        onAddCoins(-CLEAN_UP_COIN_COST)
                        executeCleanUp()
                    }
                },
                onQuit = {
                    loseLifeWarningMode = LoseLifeWarningMode.OUT_OF_SPACE
                }
            )
        }

        // Handle hardware/gesture Back press during gameplay or overlays
        BackHandler(
            enabled = loseLifeWarningMode == null && !showLevelFailedModal && !showTryAgainOutOfLivesModal
        ) {
            SoundManager.playTap()
            when {
                isPaused -> isPaused = false
                gameState == "won" -> onReturnHome()
                gameState == "chain_stuck" -> loseLifeWarningMode = LoseLifeWarningMode.OUT_OF_SPACE
                else -> loseLifeWarningMode = LoseLifeWarningMode.LEAVE_LEVEL
            }
        }

        // Top Header Bar (matches HomeScreen and original game: Pause left, Lives + Coins right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp, top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pause button (top-left)
            if (pauseNormalBitmap != null) {
                val pausePress = rememberBouncyPressState(pressedScale = 0.86f)
                Image(
                    bitmap = pauseNormalBitmap.asImageBitmap(),
                    contentDescription = "Pause",
                    modifier = Modifier
                        .size(50.dp, 54.dp)
                        .testTag("pause_button")
                        .bouncyClickTarget(pausePress) {
                            SoundManager.playTap()
                            isPaused = true
                        }
                        .bouncyScale(pausePress)
                )
            } else {
                Spacer(modifier = Modifier.size(50.dp, 54.dp))
            }

            // Lives + Coin Pills (top-right)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LivesHudPill(
                    lifeSnapshot = effectiveLifeSnapshot,
                    compact = true
                )
                UniversalCoinRack(
                    bitmap = coinRackBitmap,
                    coins = currentCoins,
                    compact = true,
                    showPlusButton = true
                )
            }
        }

        // Pause Menu Dialog
        if (isPaused) {
            PauseMenuDialog(
                context = context,
                soundMuted = soundMuted,
                onDismiss = { isPaused = false },
                onRestart = {
                    SoundManager.playTap()
                    isPaused = false
                    gameState = "playing"
                    isMagnetActive = false
                    isShuffling = false
                    isCannonActive = false
                    levelResetNonce++
                },
                onToggleSound = {
                    SoundManager.enabled = !SoundManager.enabled
                    soundMuted = !SoundManager.enabled
                },
                onHome = {
                    SoundManager.playTap()
                    isPaused = false
                    if (gameState == "won") {
                        onReturnHome()
                    } else {
                        loseLifeWarningMode = LoseLifeWarningMode.LEAVE_LEVEL
                    }
                }
            )
        }

        // Lose-Life Warning Modal ("You will lose a life!" - Reference 1)
        loseLifeWarningMode?.let { warningMode ->
            LoseLifeWarningModal(
                mode = warningMode,
                coins = currentCoins,
                onWatchAdPlayOn = {
                    loseLifeWarningMode = null
                    executeCleanUp()
                },
                onCoinsPlayOn = {
                    if (currentCoins >= CLEAN_UP_COIN_COST) {
                        currentCoins -= CLEAN_UP_COIN_COST
                        onAddCoins(-CLEAN_UP_COIN_COST)
                        loseLifeWarningMode = null
                        executeCleanUp()
                    }
                },
                onKeepPlaying = {
                    loseLifeWarningMode = null
                },
                onConfirmLoseLife = {
                    val before = effectiveLifeSnapshot.lives
                    levelFailedBeforeLives = before
                    effectiveLifeSnapshot = LivesManager.consumeOneLife(
                        lives = effectiveLifeSnapshot.lives,
                        nextRefillAtMs = effectiveLifeSnapshot.nextRefillAtMs,
                        nowMs = System.currentTimeMillis()
                    )
                    onLoseLife()
                    loseLifeWarningMode = null
                    showLevelFailedModal = true
                }
            )
        }

        // Level Failed Modal (5 Hearts Life Drop + "+1" Watch Ad + "Try Again" - Reference 2)
        if (showLevelFailedModal) {
            LevelFailedModal(
                levelNumber = currentLevel,
                beforeLives = levelFailedBeforeLives,
                lifeSnapshot = effectiveLifeSnapshot,
                onWatchAdPlusOneLife = {
                    effectiveLifeSnapshot = LivesManager.addOneLife(
                        lives = effectiveLifeSnapshot.lives,
                        nextRefillAtMs = effectiveLifeSnapshot.nextRefillAtMs,
                        nowMs = System.currentTimeMillis()
                    )
                    onGainOneLife()
                },
                onTryAgain = {
                    if (effectiveLifeSnapshot.lives > 0) {
                        showLevelFailedModal = false
                        isPaused = false
                        isTutorialDismissed = false
                        isMagnetActive = false
                        isShuffling = false
                        isCannonActive = false
                        gameState = "playing"
                        levelResetNonce++
                    } else {
                        showTryAgainOutOfLivesModal = true
                    }
                },
                onCloseToHome = {
                    showLevelFailedModal = false
                    onReturnHome()
                }
            )
        }

        if (showTryAgainOutOfLivesModal) {
            OutOfLivesModal(
                lifeSnapshot = effectiveLifeSnapshot,
                coins = currentCoins,
                onWatchAdForLife = {
                    effectiveLifeSnapshot = LivesManager.addLives(
                        lives = effectiveLifeSnapshot.lives,
                        nextRefillAtMs = effectiveLifeSnapshot.nextRefillAtMs,
                        nowMs = System.currentTimeMillis(),
                        count = LivesManager.AD_REWARD_LIVES
                    )
                    onWatchAdForLife()
                    showTryAgainOutOfLivesModal = false
                },
                onCoinsRefillAll = {
                    if (currentCoins >= LivesManager.REFILL_COIN_COST) {
                        currentCoins -= LivesManager.REFILL_COIN_COST
                        effectiveLifeSnapshot = LivesManager.refillAllLives()
                        onRefillLivesWithCoins()
                    }
                    showTryAgainOutOfLivesModal = false
                },
                onDismiss = { showTryAgainOutOfLivesModal = false }
            )
        }

        LaunchedEffect(gameState) {
            if (gameState == "won") {
                onLevelWon()
            }
        }

        // Level Complete Screen
        if (gameState == "won") {
            LevelCompleteModal(
                levelNumber = currentLevel,
                coins = currentCoins,
                onPause = { isPaused = true },
                onNextLevel = {
                    val nextLvl = currentLevel + 1
                    isTutorialDismissed = false
                    isMagnetActive = false
                    isShuffling = false
                    isCannonActive = false
                    isPaused = false
                    gameState = "playing"
                    currentLevel = nextLvl
                    onLevelAdvanced(nextLvl)
                },
                onAddCoins = { amount ->
                    currentCoins += amount
                    onAddCoins(amount)
                }
            )
        }
    }
    }
}

@Composable
private fun PauseMenuDialog(
    context: Context,
    soundMuted: Boolean,
    onDismiss: () -> Unit,
    onRestart: () -> Unit,
    onToggleSound: () -> Unit,
    onHome: () -> Unit
) {
    val panelBmp = remember { AssetLoader.loadBitmap(context, "pause_menu/pause_panel.png") }
    val resumeBmp = remember { AssetLoader.loadBitmap(context, "pause_menu/btn_resume_normal.png") }
    val restartBmp = remember { AssetLoader.loadBitmap(context, "pause_menu/btn_restart_normal.png") }
    val soundBmp = remember { AssetLoader.loadBitmap(context, "pause_menu/btn_sound_normal.png") }
    val homeBmp = remember { AssetLoader.loadBitmap(context, "pause_menu/btn_home_normal.png") }
    val closeBmp = remember { AssetLoader.loadBitmap(context, "pause_menu/btn_close_normal.png") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {},
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(280.dp)
                .height(340.dp),
            contentAlignment = Alignment.Center
        ) {
            if (panelBmp != null) {
                Image(
                    bitmap = panelBmp.asImageBitmap(),
                    contentDescription = "Pause Panel",
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (closeBmp != null) {
                val closePress = rememberBouncyPressState(pressedScale = 0.85f)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 10.dp, end = 12.dp)
                        .size(48.dp)
                        .bouncyClickTarget(closePress, role = Role.Button) {
                            SoundManager.playTap()
                            onDismiss()
                        }
                        .bouncyScale(closePress),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = closeBmp.asImageBitmap(),
                        contentDescription = "Close",
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .padding(top = 70.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (resumeBmp != null) {
                    val resumePress = rememberBouncyPressState(pressedScale = 0.88f)
                    Image(
                        bitmap = resumeBmp.asImageBitmap(),
                        contentDescription = "Resume",
                        modifier = Modifier
                            .width(170.dp)
                            .height(44.dp)
                            .bouncyClickTarget(resumePress) { onDismiss() }
                            .bouncyScale(resumePress)
                    )
                }

                if (restartBmp != null) {
                    val restartPress = rememberBouncyPressState(pressedScale = 0.88f)
                    Image(
                        bitmap = restartBmp.asImageBitmap(),
                        contentDescription = "Restart",
                        modifier = Modifier
                            .width(170.dp)
                            .height(44.dp)
                            .bouncyClickTarget(restartPress) { onRestart() }
                            .bouncyScale(restartPress)
                    )
                }

                if (soundBmp != null) {
                    val soundPress = rememberBouncyPressState(pressedScale = 0.88f)
                    Image(
                        bitmap = soundBmp.asImageBitmap(),
                        contentDescription = "Sound",
                        modifier = Modifier
                            .width(170.dp)
                            .height(44.dp)
                            .bouncyClickTarget(soundPress) { onToggleSound() }
                            .bouncyScale(soundPress)
                    )
                }

                if (homeBmp != null) {
                    val homePress = rememberBouncyPressState(pressedScale = 0.88f)
                    Image(
                        bitmap = homeBmp.asImageBitmap(),
                        contentDescription = "Home",
                        modifier = Modifier
                            .width(170.dp)
                            .height(44.dp)
                            .bouncyClickTarget(homePress) { onHome() }
                            .bouncyScale(homePress)
                    )
                }
            }
        }
    }
}
