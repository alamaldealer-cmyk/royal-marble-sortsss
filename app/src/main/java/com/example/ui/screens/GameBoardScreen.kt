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
import com.example.ui.components.UniversalCoinRack
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.game.assets.AssetLoader
import com.example.game.belt.BeltPath
import com.example.game.constants.GameConstants
import com.example.game.levels.LevelGenerator
import com.example.game.model.BeltBall
import com.example.game.model.Brick
import com.example.game.model.BrickColumn
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
import com.example.ui.components.LevelCompleteModal
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun GameBoardScreen(
    levelNumber: Int,
    coins: Int,
    onReturnHome: () -> Unit,
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
    val magnetBtnBitmap = remember { AssetLoader.loadBitmap(context, "magnet_btn_0.png") }
    val shuffleBtnBitmap = remember { AssetLoader.loadBitmap(context, "shuffle_btn_0.png") }
    val magnetLightningStrip = remember { AssetLoader.loadBitmap(context, "magnet_lightning_strip.png") }
    val shuffleLightningStrip = remember { AssetLoader.loadBitmap(context, "shuffle_lightning_strip.png") }

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
        var gameState by remember { mutableStateOf("playing") }
        var isPaused by remember { mutableStateOf(false) }
        var soundMuted by remember { mutableStateOf(!SoundManager.enabled) }

        var magnetCount by remember { mutableIntStateOf(3) }
        var shuffleCount by remember { mutableIntStateOf(3) }
        var isMagnetActive by remember { mutableStateOf(false) }
        var isShuffling by remember { mutableStateOf(false) }
        var isTutorialDismissed by remember { mutableStateOf(false) }
        var isFunnelLocked by remember { mutableStateOf(false) }
        var funnelFullAlertTimer by remember { mutableFloatStateOf(0f) }

        // Screen scale parameters in PIXELS
        var viewScale by remember { mutableFloatStateOf(1f) }
        var viewOffsetX by remember { mutableFloatStateOf(0f) }
        var viewOffsetY by remember { mutableFloatStateOf(0f) }
        var viewArenaOffsetY by remember { mutableFloatStateOf(0f) }
        val slotClipPath = remember { Path() }
        val crateColNormX = remember { floatArrayOf(0.262f, 0.500f, 0.738f) }
        val crateRowNormY = remember { floatArrayOf(0.276f, 0.493f, 0.710f) }

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
        var magnetPulseTimer by remember { mutableFloatStateOf(0f) }
        var activePowerupOverlayType by remember { mutableStateOf<String?>(null) } // "magnet" or "shuffle"
        var powerupOverlayTimer by remember { mutableFloatStateOf(0f) }
        val powerupOverlayDuration = 0.85f
        var nextDropId by remember { mutableIntStateOf(5000) }
        var beltScrollDistance by remember { mutableFloatStateOf(0f) }

    // Reset on level change or restart
    LaunchedEffect(currentLevel, levelResetNonce) {
        isTutorialDismissed = false
        isFunnelLocked = false
        funnelFullAlertTimer = 0f
        isMagnetActive = false
        isShuffling = false
        magnetPulseTimer = 0f
        activePowerupOverlayType = null
        powerupOverlayTimer = 0f
        gameState = "playing"
        isPaused = false
        physics.balls.clear()
        beltBalls.clear()
        droppingBalls.clear()
        particles.clear()
        fluffyEffects.clear()
        magnetReturningBalls.clear()
        beltScrollDistance = 0f
    }

    // 60FPS Game Loop
    LaunchedEffect(isPaused, gameState, currentLevel, levelResetNonce) {
        var lastTime = System.nanoTime()

        while (true) {
            withFrameNanos { frameTimeNanos ->
                val dt = min((frameTimeNanos - lastTime) / 1_000_000_000f, 0.05f)
                lastTime = frameTimeNanos

                if (!isPaused && gameState == "playing") {
                    // 1. Advance conveyor belt
                    beltScrollDistance = (beltScrollDistance + GameConstants.BELT_SPEED * dt) % BeltPath.TOTAL_PATH_LEN
                    val slotSpacing = BeltPath.TOTAL_PATH_LEN / GameConstants.BELT_CAPACITY

                    // Check feed point eligibility
                    var feedSlotIndex = -1
                    var minFeedDist = Float.MAX_VALUE

                    for (k in 0 until GameConstants.BELT_CAPACITY) {
                        val slotS = (k * slotSpacing + beltScrollDistance) % BeltPath.TOTAL_PATH_LEN
                        val dist = BeltPath.circularDistance(slotS, BeltPath.FEED_POINT_S)
                        if (dist < minFeedDist) {
                            minFeedDist = dist
                            feedSlotIndex = k
                        }
                    }

                    val occupiedSlots = beltBalls.map { it.slotIndex }.toSet()
                    val canFeed = feedSlotIndex != -1 &&
                            minFeedDist <= 4.5f &&
                            !occupiedSlots.contains(feedSlotIndex) &&
                            beltBalls.size < GameConstants.BELT_CAPACITY

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

                    // Keep funnel lock synced: locks at 24+ waiting balls, unlocks once <= 23 remain
                    isFunnelLocked = GameRules.shouldBlockBlockTap(physics.balls.size, isFunnelLocked)
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

                    for (ball in beltBalls) {
                        if (BeltPath.isSOnBottomLane(ball.s)) {
                            val coords = BeltPath.getBeltCoordinates(ball.s)
                            val match = GameRules.tryMatchBeltBall(coords.x, ball.color, columns, nowMs)
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
                                        duration = 0.18f,
                                        colIndex = match.matchedColIndex,
                                        holeIndex = match.targetHoleIndex,
                                        mood = "happy_open"
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
                            if (col.bricks.isNotEmpty()) {
                                val topBrick = col.bricks[0]
                                topBrick.inFlight = maxOf(0, topBrick.inFlight - 1)
                                topBrick.filled++
                                col.lightPulseTimer = 0.65f

                                when (topBrick.filled) {
                                    1 -> {
                                        topBrick.animState = "hit_1"
                                        topBrick.animTimer = 0f
                                        SoundManager.playDropHole(0)
                                    }
                                    2 -> {
                                        topBrick.animState = "hit_2"
                                        topBrick.animTimer = 0f
                                        SoundManager.playDropHole(1)
                                    }
                                    else -> {
                                        topBrick.animState = "hit_3"
                                        topBrick.animTimer = 0f
                                        SoundManager.playDropHole(2)
                                    }
                                }
                            }
                        }
                    }
                    droppingBalls.removeAll(dropsToRemove)

                    // Brick animations and completion
                    for (col in columns) {
                        if (col.bricks.isNotEmpty()) {
                            val top = col.bricks[0]
                            when (top.animState) {
                                "activate", "hit_1", "hit_2" -> {
                                    top.animTimer += dt
                                    if (top.animTimer >= 0.16f) {
                                        top.animState = "idle"
                                        top.animTimer = 0f
                                    }
                                }
                                "hit_3" -> {
                                    top.animTimer += dt
                                    if (top.animTimer >= 0.18f) {
                                        top.animState = "hold_full"
                                        top.animTimer = 0f
                                    }
                                }
                                "hold_full" -> {
                                    top.animTimer += dt
                                    if (top.animTimer >= 0.16f) {
                                        top.animState = "vanish"
                                        top.animTimer = 0f
                                        SoundManager.playBrickClear()
                                        score += 300

                                        // Particles explosion
                                        for (p in 0 until 14) {
                                            particles.add(
                                                Particle(
                                                    x = col.x + (Random.nextFloat() - 0.5f) * 36f,
                                                    y = GameConstants.BALTI_SOCKET_Y + (Random.nextFloat() - 0.5f) * 16f,
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
                                                    color = top.color,
                                                    startX = col.x + holeOffsets[h],
                                                    startY = GameConstants.BALTI_SOCKET_Y - 4f,
                                                    vx = vxs[h],
                                                    vy = -160f - Random.nextFloat() * 40f,
                                                    x = col.x + holeOffsets[h],
                                                    y = GameConstants.BALTI_SOCKET_Y - 4f
                                                )
                                            )
                                        }

                                        col.bricks.removeAt(0)
                                        if (col.bricks.isNotEmpty()) {
                                            col.bricks[0].animState = "activate"
                                            col.bricks[0].animTimer = 0f
                                        }

                                        if (GameRules.isLevelWon(columns)) {
                                            gameState = "won"
                                        }
                                    }
                                }
                            }
                        }
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

                    if (powerupOverlayTimer > 0f) {
                        powerupOverlayTimer = (powerupOverlayTimer - dt).coerceAtLeast(0f)
                        if (powerupOverlayTimer <= 0f) {
                            activePowerupOverlayType = null
                        }
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
                                cell.animTimer += dt
                                if (cell.animState == "cave_dispensing" && cell.animTimer >= 0.58f) {
                                    cell.animState = "idle"
                                    cell.animTimer = 0f
                                }
                            } else if (cell.kind == CellKind.BLOCK && cell.animState == "dispensing") {
                                cell.animTimer += dt
                                val p = ((cell.animTimer - 0.16f) / 0.34f).coerceIn(0f, 1f)
                                val ease = 1f - (1f - p) * (1f - p) * (1f - p)
                                cell.offsetY = -28f * (1f - ease)
                                val popScale = 0.65f + 0.35f * ease
                                cell.scaleX = popScale
                                cell.scaleY = popScale
                                if (cell.animTimer >= 0.50f) {
                                    cell.offsetY = 0f
                                    cell.scaleX = 1f
                                    cell.scaleY = 1f
                                    cell.animState = "idle"
                                    cell.animTimer = 0f
                                }
                            }
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
        // Magnet drawn at RectF(86f, 605f, 134f, 653f) -> hitbox with badge padding: x in 80..140, y in 598..660
        if (arenaGameX in 80f..140f && arenaGameY in 598f..660f) {
            if (magnetCount > 0 && beltBalls.isNotEmpty() && magnetReturningBalls.isEmpty()) {
                val assignments = GameRules.planMagnetReturn(grid, beltBalls.toList())
                if (assignments.isNotEmpty()) {
                    magnetCount = (magnetCount - 1).coerceAtLeast(0)
                    isMagnetActive = true
                    magnetPulseTimer = 0.90f
                    activePowerupOverlayType = "magnet"
                    powerupOverlayTimer = powerupOverlayDuration
                    isTutorialDismissed = true
                    SoundManager.playPowerup()

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
                    SoundManager.playBlocked()
                }
            } else {
                SoundManager.playBlocked()
            }
            return
        }

        // Shuffle drawn at RectF(146f, 605f, 194f, 653f) -> hitbox with badge padding: x in 140..202, y in 598..660
        if (arenaGameX > 140f && arenaGameX <= 202f && arenaGameY in 598f..660f) {
            if (shuffleCount > 0) {
                val result = GameRules.performShufflePowerup(columns, beltBalls, physics.balls)
                if (result.shuffled) {
                    shuffleCount = (shuffleCount - 1).coerceAtLeast(0)
                    activePowerupOverlayType = "shuffle"
                    powerupOverlayTimer = powerupOverlayDuration
                    isTutorialDismissed = true
                    SoundManager.playPowerup()
                    for (colIdx in result.swappedColumns) {
                        val col = columns.getOrNull(colIdx) ?: continue
                        if (col.bricks.isNotEmpty()) {
                            col.bricks[0].animState = "activate"
                            col.bricks[0].animTimer = 0f
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
                    SoundManager.playBlocked()
                }
            } else {
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

                    if (cell.kind == CellKind.BLOCK &&
                        cell.animState != "magnet_receiving" &&
                        GameRules.isExposed(grid, r, c)
                    ) {
                        isFunnelLocked = GameRules.shouldBlockBlockTap(physics.balls.size, isFunnelLocked)
                        if (isFunnelLocked) {
                            SoundManager.playBlocked()
                            funnelFullAlertTimer = 1.4f
                            return
                        }
                        isTutorialDismissed = true
                        val burstColor = cell.color ?: MarbleColor.PURPLE
                        val spawnCount = cell.ballCount.coerceIn(1, GameConstants.BALLS_PER_BLOCK)
                        grid[r][c] = GridCell(kind = CellKind.EMPTY)
                        SoundManager.playPopBurst()
                        physics.spawnBurst(cx, cy, burstColor, spawnCount)
                        isFunnelLocked = GameRules.shouldBlockBlockTap(physics.balls.size, isFunnelLocked)
                        score += 50
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
                        val belowR = r + 1
                        if (belowR < GameConstants.GRID_ROWS && grid[belowR][c].kind == CellKind.EMPTY) {
                            SoundManager.playPopBurst()
                            val nextColor = if (cell.queue.isNotEmpty()) cell.queue.removeAt(0) else MarbleColor.PURPLE
                            cell.color = nextColor
                            cell.counter = (cell.counter ?: 1) - 1
                            cell.animState = "cave_dispensing"
                            cell.animTimer = 0f
                            grid[belowR][c] = GridCell(
                                kind = CellKind.BLOCK,
                                color = nextColor,
                                animState = "dispensing",
                                animTimer = 0f,
                                scaleX = 0.65f,
                                scaleY = 0.65f,
                                offsetY = -28f
                            )
                            for (sp in 0 until 8) {
                                particles.add(
                                    Particle(
                                        x = cx + (Random.nextFloat() - 0.5f) * 20f,
                                        y = cy + 10f,
                                        vx = (Random.nextFloat() - 0.5f) * 70f,
                                        vy = 20f + Random.nextFloat() * 55f,
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
            for (r in 0 until GameConstants.GRID_ROWS) {
                for (c in 0 until GameConstants.GRID_COLS) {
                    val cell = grid[r][c]
                    val cx = GameConstants.GRID_X_CENTERS[c]
                    val cy = GameConstants.GRID_Y_CENTERS[r]

                    when (cell.kind) {
                        CellKind.BLOCK -> {
                            if (cell.animState == "dispensing" && cell.animTimer < 0.16f) {
                                continue
                            }
                            val col = cell.color ?: MarbleColor.PURPLE
                            val crateFullBmp = AssetLoader.getCrateFull(context, col)
                            val crateOpenBmp = AssetLoader.getCrateOpen(context, col)
                            val scaledW = crateW * cell.scaleX
                            val scaledH = crateH * cell.scaleY
                            val blockCy = cy + cell.offsetY
                            val dstRect = RectF(
                                cx - scaledW / 2f,
                                blockCy - scaledH / 2f,
                                cx + scaledW / 2f,
                                blockCy + scaledH / 2f
                            )

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

                            if (cell.ballCount >= GameConstants.BALLS_PER_BLOCK &&
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
                                    if (crateFullBmp != null) {
                                        val clipR = 0.123f * minOf(scaledW, scaledH)
                                        for (slotIdx in 0 until seatedCount) {
                                            val sRow = slotIdx / 3
                                            val sCol = slotIdx % 3
                                            val slotCx = dstRect.left + crateColNormX[sCol] * scaledW
                                            val slotCy = dstRect.top + crateRowNormY[sRow] * scaledH
                                            canvas.save()
                                            slotClipPath.reset()
                                            slotClipPath.addCircle(slotCx, slotCy, clipR, Path.Direction.CW)
                                            canvas.clipPath(slotClipPath)
                                            canvas.drawBitmap(crateFullBmp, null, dstRect, null)
                                            canvas.restore()
                                        }
                                    } else {
                                        val ballBmp = AssetLoader.getBall(context, col)
                                            ?: AssetLoader.getFluffy(context, col, "content")
                                        if (ballBmp != null) {
                                            val brX = 4.6f * cell.scaleX
                                            val brY = 4.6f * cell.scaleY
                                            for (slotIdx in 0 until seatedCount) {
                                                val sRow = slotIdx / 3
                                                val sCol = slotIdx % 3
                                                val bx = dstRect.left + crateColNormX[sCol] * scaledW
                                                val by = dstRect.top + crateRowNormY[sRow] * scaledH
                                                canvas.drawBitmap(
                                                    ballBmp,
                                                    null,
                                                    RectF(bx - brX, by - brY, bx + brX, by + brY),
                                                    null
                                                )
                                            }
                                        }
                                    }
                                }
                            }
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
                                canvas.drawBitmap(
                                    bmp,
                                    null,
                                    RectF(cx - crateW / 2f, cy - crateH / 2f, cx + crateW / 2f, cy + crateH / 2f),
                                    null
                                )
                            }
                        }

                        CellKind.DISPENSER -> {
                            val countNum = cell.counter ?: 0
                            val isDispensing = cell.animState == "cave_dispensing"
                            val t = cell.animTimer

                            val frameIdx = when {
                                isDispensing && t < 0.12f -> 1
                                isDispensing && t < 0.24f -> 2
                                isDispensing && t < 0.46f -> 3
                                isDispensing -> 4
                                countNum <= 0 -> 4
                                else -> 0
                            }

                            val scaleFX: Float
                            val scaleFY: Float
                            val shakeX: Float
                            if (isDispensing) {
                                when {
                                    t < 0.12f -> {
                                        scaleFX = 1.06f; scaleFY = 0.94f; shakeX = 0f
                                    }
                                    t < 0.24f -> {
                                        scaleFX = 0.97f; scaleFY = 1.05f; shakeX = 0f
                                    }
                                    t < 0.46f -> {
                                        scaleFX = 1.04f; scaleFY = 1.02f; shakeX = sin(t * 75f) * 1.2f
                                    }
                                    else -> {
                                        scaleFX = 1f; scaleFY = 1f; shakeX = 0f
                                    }
                                }
                            } else if (countNum > 0) {
                                val pulse = 1f + 0.025f * sin(t * 3.5f + c * 0.9f)
                                scaleFX = pulse
                                scaleFY = pulse
                                shakeX = 0f
                            } else {
                                scaleFX = 0.96f; scaleFY = 0.96f; shakeX = 0f
                            }

                            val dispW = 34f * scaleFX
                            val dispH = 31f * scaleFY
                            val dispRect = RectF(
                                cx + shakeX - dispW / 2f,
                                cy - dispH / 2f,
                                cx + shakeX + dispW / 2f,
                                cy + dispH / 2f
                            )

                            if (isDispensing && t in 0.12f..0.46f) {
                                val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    color = android.graphics.Color.parseColor("#6638BDF8")
                                }
                                canvas.drawOval(
                                    RectF(dispRect.left - 3f, dispRect.top, dispRect.right + 3f, dispRect.bottom + 5f),
                                    glowPaint
                                )
                            }

                            val dispBmp = AssetLoader.getDispenserFrame(context, frameIdx)
                            if (dispBmp != null) {
                                val alphaPaint = if (countNum <= 0 && !isDispensing) {
                                    Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = 175 }
                                } else null
                                canvas.drawBitmap(dispBmp, null, dispRect, alphaPaint)
                            }

                            // Colored tray emerging from mouth during burst phase
                            if (isDispensing && t in 0.18f..0.42f) {
                                val emergingColor = cell.color ?: cell.queue.firstOrNull() ?: MarbleColor.PURPLE
                                val trayBmp = AssetLoader.getDispenserTray(context, emergingColor)
                                if (trayBmp != null) {
                                    val slide = ((t - 0.18f) / 0.24f).coerceIn(0f, 1f)
                                    val trayW = 20f + 8f * slide
                                    val trayH = 18f + 7f * slide
                                    val trayY = cy + 6f + slide * 13f
                                    canvas.drawBitmap(
                                        trayBmp,
                                        null,
                                        RectF(cx - trayW / 2f, trayY - trayH / 2f, cx + trayW / 2f, trayY + trayH / 2f),
                                        null
                                    )
                                }
                            }

                            // Counter badge in glossy dome center
                            if (countNum > 0) {
                                val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    color = android.graphics.Color.parseColor("#D9082952")
                                    style = Paint.Style.FILL
                                }
                                val badgeStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    color = android.graphics.Color.parseColor("#F5D061")
                                    style = Paint.Style.STROKE
                                    strokeWidth = 1f
                                }
                                val badgeRect = RectF(cx - 8f, cy - 11f, cx + 8f, cy + 1f)
                                canvas.drawRoundRect(badgeRect, 5f, 5f, badgePaint)
                                canvas.drawRoundRect(badgeRect, 5f, 5f, badgeStroke)
                                textPaint.textSize = 9.5f
                                textPaint.color = android.graphics.Color.WHITE
                                canvas.drawText("$countNum", cx, cy - 2f, textPaint)
                            }
                        }

                        else -> {}
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

            // Funnel Waiting Capacity Indicator (shows when >= 16 or funnel is locked at 24+)
            val waitingCount = physics.balls.size
            if (isFunnelLocked || funnelFullAlertTimer > 0f || waitingCount >= 16) {
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
                    "FULL $waitingCount/${GameConstants.MAX_WAITING_BALLS}"
                } else {
                    "$waitingCount/${GameConstants.MAX_WAITING_BALLS}"
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

            // 2. Thick clean white outer capsule border rim
            val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#E5EBFF")
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
            val slotSpacing = BeltPath.TOTAL_PATH_LEN / GameConstants.BELT_CAPACITY
            for (i in 0 until GameConstants.BELT_CAPACITY) {
                val s = (i * slotSpacing + beltScrollDistance) % BeltPath.TOTAL_PATH_LEN
                val coords = BeltPath.getBeltCoordinates(s)
                canvas.drawCircle(coords.x, coords.y, 8.2f, socketPaint)
                canvas.drawCircle(coords.x, coords.y, 8.2f, socketBorderPaint)
            }

            // 5. Conveyor Belt Fluffies with interactive animated expressions (winking, blinking, content)
            for (b in beltBalls) {
                val coords = BeltPath.getBeltCoordinates(b.s)
                val fluffyBmp = AssetLoader.getFluffy(context, b.color, b.mood)
                val r = GameConstants.BALL_RADIUS_BELT * 1.35f * b.scale

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
                val r = GameConstants.BALL_RADIUS_BELT * 1.35f

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
            // Rendered via authentic 192x192 bucket_spritesheet.png atlas and bucket_spritesheet_purple.png
            val bucketSheetBmp = AssetLoader.getBucketSpritesheet(context)
            val bucketSheetPurpleBmp = AssetLoader.getBucketSpritesheetPurple(context)
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

                    val isPurple = queuedBrick.color == MarbleColor.PURPLE
                    val sheetToUse = if (isPurple && bucketSheetPurpleBmp != null) bucketSheetPurpleBmp else bucketSheetBmp

                    if (sheetToUse != null) {
                        val rowIndex = if (isPurple && bucketSheetPurpleBmp != null) 0 else getBucketRowIndex(queuedBrick.color)
                        val srcRect = Rect(0, rowIndex * 192, 192, (rowIndex + 1) * 192)
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
                }

                // B. Draw front active bucket using spritesheet atlas animation
                val frontBrick = stack[0]
                val isPurpleFront = frontBrick.color == MarbleColor.PURPLE
                val sheetFrontToUse = if (isPurpleFront && bucketSheetPurpleBmp != null) bucketSheetPurpleBmp else bucketSheetBmp

                if (sheetFrontToUse != null) {
                    val rowIndex = if (isPurpleFront && bucketSheetPurpleBmp != null) 0 else getBucketRowIndex(frontBrick.color)
                    val frameIndex = getBucketFrameIndex(frontBrick.animState, frontBrick.animTimer, frontBrick.filled)
                    val srcRect = Rect(frameIndex * 192, rowIndex * 192, (frameIndex + 1) * 192, (rowIndex + 1) * 192)
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

                // Cute Fluffies peeking comfortably inside bucket with opening and closing eyes
                if (frontBrick.filled > 0 && frontBrick.animState != "vanish") {
                    val holeOffsets = floatArrayOf(-11.5f, 0f, 11.5f)
                    val countFilled = minOf(frontBrick.filled, 3)
                    for (h in 0 until countFilled) {
                        val blinkCycle = ((System.currentTimeMillis() / 1000f) + h * 0.6f + col.x * 0.05f) % 2.8f
                        val inBucketMood = if (blinkCycle < 0.45f) "eyes_closed" else if (blinkCycle in 1.4f..1.7f) "wink" else "content"
                        val inBucketFluffy = AssetLoader.getFluffy(context, frontBrick.color, inBucketMood)
                        if (inBucketFluffy != null) {
                            val fx = col.x + holeOffsets[h]
                            val fy = GameConstants.BALTI_SOCKET_Y - 2.5f
                            val fr = 7.5f
                            canvas.drawBitmap(
                                inBucketFluffy,
                                null,
                                RectF(fx - fr, fy - fr, fx + fr, fy + fr),
                                null
                            )
                        }
                    }
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
                    val endR = 0.123f * 33f
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
                        AssetLoader.getBall(context, mb.color)
                            ?: AssetLoader.getFluffy(context, mb.color, "content")
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

            // Dramatic Center-Screen Power-Up Surge (Big Magnet / Shuffle + Cracking Lightning Bolts)
            val overlayType = activePowerupOverlayType
            if (overlayType != null && powerupOverlayTimer > 0f) {
                val p = (1f - powerupOverlayTimer / powerupOverlayDuration).coerceIn(0f, 1f)
                val centerX = 148f
                val centerY = 348f

                val overlayScale = when {
                    p < 0.16f -> {
                        val t = p / 0.16f
                        0.15f + 1.05f * (1f - (1f - t) * (1f - t))
                    }
                    p < 0.28f -> {
                        val t = (p - 0.16f) / 0.12f
                        1.20f - 0.20f * t
                    }
                    p < 0.76f -> {
                        1.0f + sin(p * 34f) * 0.045f
                    }
                    else -> {
                        val t = (p - 0.76f) / 0.24f
                        1.0f - 0.75f * (t * t)
                    }
                }

                val overlayAlpha = when {
                    p < 0.10f -> (p / 0.10f)
                    p > 0.76f -> ((1f - p) / 0.24f)
                    else -> 1f
                }.coerceIn(0f, 1f)

                val isMagnetOverlay = overlayType == "magnet"
                val primaryGlowHex = if (isMagnetOverlay) "#00F0FF" else "#FFE600"
                val secondaryGlowHex = if (isMagnetOverlay) "#38BDF8" else "#C084FC"

                // 1. Soft radial energy burst halo behind the big icon
                val haloR = 88f * overlayScale
                val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        centerX,
                        centerY,
                        haloR.coerceAtLeast(1f),
                        intArrayOf(
                            android.graphics.Color.parseColor(primaryGlowHex),
                            android.graphics.Color.parseColor(secondaryGlowHex),
                            android.graphics.Color.TRANSPARENT
                        ),
                        floatArrayOf(0f, 0.45f, 1f),
                        Shader.TileMode.CLAMP
                    )
                    alpha = (185 * overlayAlpha).toInt().coerceIn(0, 255)
                }
                canvas.drawCircle(centerX, centerY, haloR, haloPaint)

                // 2. Expanding electric shockwave rings
                for (ringIdx in 0 until 2) {
                    val ringP = ((p * 1.45f) - ringIdx * 0.22f).coerceIn(0f, 1f)
                    if (ringP > 0f && ringP < 1f) {
                        val ringRadius = 28f + ringP * 112f
                        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.parseColor(if (ringIdx == 0) primaryGlowHex else "#FFFFFF")
                            alpha = ((1f - ringP) * 210 * overlayAlpha).toInt().coerceIn(0, 255)
                            style = Paint.Style.STROKE
                            strokeWidth = 3.2f * (1f - ringP * 0.5f)
                        }
                        canvas.drawCircle(centerX, centerY, ringRadius, ringPaint)
                    }
                }

                // 3. Jagged electric lightning bolts ("bijlia") crackling outward in all directions!
                val boltPhase = (p * 22f).toInt()
                val boltOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.parseColor(primaryGlowHex)
                    alpha = (230 * overlayAlpha).toInt().coerceIn(0, 255)
                    style = Paint.Style.STROKE
                    strokeWidth = 4.6f
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
                val boltCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.WHITE
                    alpha = (255 * overlayAlpha).toInt().coerceIn(0, 255)
                    style = Paint.Style.STROKE
                    strokeWidth = 1.9f
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
                val sparkTipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.WHITE
                    alpha = (240 * overlayAlpha).toInt().coerceIn(0, 255)
                }

                val numBolts = 10
                val reachScale = sin(p.coerceIn(0.05f, 0.95f) * PI.toFloat()).coerceIn(0.2f, 1f)
                for (bIdx in 0 until numBolts) {
                    val rng = kotlin.random.Random(bIdx * 97 + boltPhase * 31)
                    val baseAngle = (bIdx * (2f * PI.toFloat() / numBolts)) + (rng.nextFloat() - 0.5f) * 0.25f
                    val startDist = 26f * overlayScale
                    val endDist = (78f + rng.nextFloat() * 58f) * reachScale

                    val perpX = -sin(baseAngle)
                    val perpY = cos(baseAngle)

                    val boltPath = android.graphics.Path()
                    val segments = 4
                    var tipX = centerX
                    var tipY = centerY
                    for (seg in 0..segments) {
                        val frac = seg.toFloat() / segments
                        val dist = startDist + (endDist - startDist) * frac
                        val jitter = if (seg == 0) 0f else (rng.nextFloat() - 0.5f) * 22f * (0.4f + frac * 0.6f)
                        val px = centerX + cos(baseAngle) * dist + perpX * jitter
                        val py = centerY + sin(baseAngle) * dist + perpY * jitter
                        if (seg == 0) {
                            boltPath.moveTo(px, py)
                        } else {
                            boltPath.lineTo(px, py)
                        }
                        tipX = px
                        tipY = py
                    }

                    canvas.drawPath(boltPath, boltOuterPaint)
                    canvas.drawPath(boltPath, boltCorePaint)
                    canvas.drawCircle(tipX, tipY, 2.4f, sparkTipPaint)
                }

                // 4. Big Animated Center Power-up Sprite (6-frame lightning strip)
                val strip = if (isMagnetOverlay) magnetLightningStrip else shuffleLightningStrip
                val fallbackBtn = if (isMagnetOverlay) magnetBtnBitmap else shuffleBtnBitmap
                val halfIcon = 54f * overlayScale
                val iconRect = RectF(
                    centerX - halfIcon,
                    centerY - halfIcon,
                    centerX + halfIcon,
                    centerY + halfIcon
                )
                val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    alpha = (255 * overlayAlpha).toInt().coerceIn(0, 255)
                }

                if (strip != null && strip.width >= 6) {
                    val frameW = strip.width / 6
                    val frameIdx = ((p * 12f).toInt() % 6).coerceIn(0, 5)
                    val srcRect = Rect(frameIdx * frameW, 0, (frameIdx + 1) * frameW, strip.height)
                    canvas.drawBitmap(strip, srcRect, iconRect, iconPaint)
                } else if (fallbackBtn != null) {
                    canvas.drawBitmap(fallbackBtn, null, iconRect, iconPaint)
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

            // Powerups (🧲 Magnet & 🔀 Shuffle) positioned at the bottom of the arena
            val powerupY = 605f
            // Magnet (bottom left of center)
            if (magnetPulseTimer > 0f || isMagnetActive) {
                val pulseP = (magnetPulseTimer / 0.90f).coerceIn(0f, 1f)
                val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.parseColor("#00F0FF")
                    alpha = (180 * maxOf(pulseP, if (isMagnetActive) 0.65f else 0f)).toInt().coerceIn(0, 255)
                    style = Paint.Style.STROKE
                    strokeWidth = 2.6f
                }
                canvas.drawCircle(110f, powerupY + 24f, 25f + (1f - pulseP) * 6f, ringPaint)
            }
            if (magnetBtnBitmap != null) {
                canvas.drawBitmap(
                    magnetBtnBitmap,
                    null,
                    RectF(86f, powerupY, 134f, powerupY + 48f),
                    null
                )
            }
            val magnetBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor(if (isMagnetActive) "#00F0FF" else "#FF3D68")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(130f, powerupY + 44f, 8f, magnetBadgePaint)
            textPaint.textSize = 9.5f
            textPaint.color = android.graphics.Color.WHITE
            canvas.drawText("$magnetCount", 130f, powerupY + 47.5f, textPaint)

            // Shuffle (bottom right of center)
            if (shuffleBtnBitmap != null) {
                canvas.drawBitmap(
                    shuffleBtnBitmap,
                    null,
                    RectF(146f, powerupY, 194f, powerupY + 48f),
                    null
                )
            }
            val shuffleBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#FF3D68")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(190f, powerupY + 44f, 8f, shuffleBadgePaint)
            canvas.drawText("$shuffleCount", 190f, powerupY + 47.5f, textPaint)

            canvas.restore()

        }

        // Top Header Bar (matches HomeScreen and original game exactly: top 8.dp, horizontal padding)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp, top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pause button (top-left)
            if (pauseNormalBitmap != null) {
                Image(
                    bitmap = pauseNormalBitmap.asImageBitmap(),
                    contentDescription = "Pause",
                    modifier = Modifier
                        .size(52.dp, 56.dp)
                        .testTag("pause_button")
                        .clickable {
                            SoundManager.playTap()
                            isPaused = true
                        }
                )
            } else {
                Spacer(modifier = Modifier.size(52.dp, 56.dp))
            }

            // Coin Rack (top-right) - Exact same UniversalCoinRack component, size, and alignment
            UniversalCoinRack(
                bitmap = coinRackBitmap,
                coins = currentCoins
            )
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
                    levelResetNonce++
                },
                onToggleSound = {
                    SoundManager.enabled = !SoundManager.enabled
                    soundMuted = !SoundManager.enabled
                },
                onHome = {
                    SoundManager.playTap()
                    isPaused = false
                    onReturnHome()
                }
            )
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
                Image(
                    bitmap = closeBmp.asImageBitmap(),
                    contentDescription = "Close",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 18.dp, end = 20.dp)
                        .size(32.dp)
                        .clickable { onDismiss() }
                )
            }

            Column(
                modifier = Modifier
                    .padding(top = 70.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (resumeBmp != null) {
                    Image(
                        bitmap = resumeBmp.asImageBitmap(),
                        contentDescription = "Resume",
                        modifier = Modifier
                            .width(170.dp)
                            .height(44.dp)
                            .clickable { onDismiss() }
                    )
                }

                if (restartBmp != null) {
                    Image(
                        bitmap = restartBmp.asImageBitmap(),
                        contentDescription = "Restart",
                        modifier = Modifier
                            .width(170.dp)
                            .height(44.dp)
                            .clickable { onRestart() }
                    )
                }

                if (soundBmp != null) {
                    Image(
                        bitmap = soundBmp.asImageBitmap(),
                        contentDescription = "Sound",
                        modifier = Modifier
                            .width(170.dp)
                            .height(44.dp)
                            .clickable { onToggleSound() }
                    )
                }

                if (homeBmp != null) {
                    Image(
                        bitmap = homeBmp.asImageBitmap(),
                        contentDescription = "Home",
                        modifier = Modifier
                            .width(170.dp)
                            .height(44.dp)
                            .clickable { onHome() }
                    )
                }
            }
        }
    }
}

private fun getBucketRowIndex(color: MarbleColor): Int {
    return when (color) {
        MarbleColor.PURPLE, MarbleColor.PINK -> 0
        MarbleColor.ICE -> 1
        MarbleColor.YELLOW -> 2
        MarbleColor.LIME -> 3
        MarbleColor.CYAN -> 4
        MarbleColor.ORANGE -> 5
        MarbleColor.RED -> 6
    }
}

private fun getBucketFrameIndex(animState: String, animTimer: Float, filled: Int): Int {
    return when (animState) {
        "queued" -> 0
        "activate" -> {
            val p = (animTimer / 0.16f).coerceIn(0f, 1f)
            (1 + (p * 3).toInt()).coerceIn(1, 4)
        }
        "hit_1" -> {
            val p = (animTimer / 0.16f).coerceIn(0f, 1f)
            (5 + (p * 3).toInt()).coerceIn(5, 8)
        }
        "hit_2" -> {
            val p = (animTimer / 0.16f).coerceIn(0f, 1f)
            (9 + (p * 3).toInt()).coerceIn(9, 12)
        }
        "hit_3" -> {
            val p = (animTimer / 0.18f).coerceIn(0f, 1f)
            (13 + (p * 3).toInt()).coerceIn(13, 16)
        }
        "hold_full" -> 16
        "vanish" -> {
            val p = (animTimer / 0.25f).coerceIn(0f, 1f)
            (17 + (p * 7).toInt()).coerceIn(17, 24)
        }
        else -> {
            when (filled) {
                0 -> 4
                1 -> 8
                2 -> 12
                else -> 16
            }
        }
    }
}
