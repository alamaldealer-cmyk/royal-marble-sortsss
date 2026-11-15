package com.example.ui.screens

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.components.BucketFluffyRenderer
import com.example.ui.components.bouncyClickTarget
import com.example.ui.components.bouncyScale
import com.example.ui.components.rememberBouncyPressState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.assets.AssetLoader
import com.example.game.constants.GameConstants
import com.example.game.model.MarbleColor
import com.example.game.sound.SoundManager
import com.example.ui.theme.FredokaFontFamily
import kotlin.math.PI
import kotlin.math.sin

private val TEST_FLUFFY_MOODS = listOf(
    "happy_open" to "Happy",
    "wink" to "Wink",
    "eyes_closed" to "Laugh",
    "surprised" to "Shock",
    "content" to "Calm",
    "curious" to "Curious",
    "angry_1" to "Angry 1",
    "angry_2" to "Angry 2"
)

@Composable
fun BucketColorTestScreen(
    onBack: () -> Unit
) {
    BackHandler {
        SoundManager.playTap()
        onBack()
    }

    val context = LocalContext.current
    val allColors = MarbleColor.entries

    // Start on index 8 (BLACK - first of the 8 newly updated bucket colors); all 16 colors remain 1 tap away.
    var colorIndex by remember { mutableIntStateOf(8) }
    var isAutoPlaying by remember { mutableStateOf(true) }

    // Live bucket simulation state
    var animState by remember { mutableStateOf("activate") } // queued, activate, idle, hit_1, hit_2, hit_3, hold_full, vanish
    var animTimer by remember { mutableFloatStateOf(0f) }
    var filledCount by remember { mutableIntStateOf(0) }
    var dropProgress by remember { mutableFloatStateOf(-1f) } // >= 0 means ball dropping into opening
    var holdPauseTimer by remember { mutableFloatStateOf(0f) }

    val activeColor = allColors[colorIndex]
    val nextColor = allColors[(colorIndex + 1) % allColors.size]
    val isNewColor = colorIndex >= 8
    val colorDef = GameConstants.COLOR_MAP[activeColor] ?: GameConstants.COLOR_MAP[MarbleColor.PURPLE]!!

    fun triggerReplayForCurrent() {
        animState = "activate"
        animTimer = 0f
        filledCount = 0
        dropProgress = -1f
        holdPauseTimer = 0f
    }

    fun selectColor(index: Int) {
        colorIndex = ((index % allColors.size) + allColors.size) % allColors.size
        triggerReplayForCurrent()
    }

    fun triggerManualDrop() {
        when (animState) {
            "vanish", "hold_full", "hit_3" -> {
                triggerReplayForCurrent()
            }
            "queued" -> {
                animState = "activate"
                animTimer = 0f
            }
            else -> {
                if (dropProgress < 0f && filledCount < 3) {
                    dropProgress = 0f
                    SoundManager.playDropHole(filledCount)
                }
            }
        }
    }

    // 60fps simulation loop driving authentic bucket & ball drop lifecycle
    LaunchedEffect(Unit) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastNanos == 0L) {
                    lastNanos = frameNanos
                    return@withFrameNanos
                }
                val dt = ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                lastNanos = frameNanos

                animTimer += dt

                // Handle dropping ball animation into bucket opening
                if (dropProgress >= 0f) {
                    dropProgress += dt / 0.22f
                    if (dropProgress >= 1f) {
                        dropProgress = -1f
                        if (filledCount < 3) {
                            filledCount++
                            animState = when (filledCount) {
                                1 -> "hit_1"
                                2 -> "hit_2"
                                else -> "hit_3"
                            }
                            animTimer = 0f
                        }
                    }
                }

                when (animState) {
                    "activate" -> {
                        if (animTimer >= 0.24f) {
                            animState = "idle"
                            animTimer = 0f
                            holdPauseTimer = 0f
                        }
                    }
                    "idle" -> {
                        if (isAutoPlaying && dropProgress < 0f && filledCount < 3) {
                            holdPauseTimer += dt
                            if (holdPauseTimer >= 0.22f) {
                                holdPauseTimer = 0f
                                dropProgress = 0f
                            }
                        }
                    }
                    "hit_1", "hit_2" -> {
                        if (animTimer >= 0.22f) {
                            animState = "idle"
                            animTimer = 0f
                            holdPauseTimer = 0f
                        }
                    }
                    "hit_3" -> {
                        if (animTimer >= 0.24f) {
                            animState = "hold_full"
                            animTimer = 0f
                        }
                    }
                    "hold_full" -> {
                        if (animTimer >= 0.36f) {
                            animState = "vanish"
                            animTimer = 0f
                            SoundManager.playBrickClear()
                        }
                    }
                    "vanish" -> {
                        if (animTimer >= 0.50f) {
                            if (isAutoPlaying) {
                                colorIndex = (colorIndex + 1) % allColors.size
                            }
                            triggerReplayForCurrent()
                        }
                    }
                }
            }
        }
    }

    val closedBmp = remember(activeColor) { AssetLoader.getCrateClosed(context, activeColor) }
    val openBmp = remember(activeColor) { AssetLoader.getCrateOpen(context, activeColor) }
    val fullBmp = remember(activeColor) { AssetLoader.getCrateFull(context, activeColor) }
    val trayBmp = remember(activeColor) { AssetLoader.getDispenserTray(context, activeColor) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF004099),
                        Color(0xFF002466),
                        Color(0xFF00143D)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .widthIn(max = 560.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val backPress = rememberBouncyPressState(pressedScale = 0.86f)
                IconButton(
                    onClick = {
                        backPress.triggerPulse()
                        SoundManager.playTap()
                        onBack()
                    },
                    interactionSource = backPress.interactionSource,
                    modifier = Modifier
                        .size(48.dp)
                        .bouncyScale(backPress)
                        .clip(CircleShape)
                        .background(Color(0xFF0B5FA7))
                        .border(2.dp, Color(0xFFFFD700), CircleShape)
                        .testTag("color_test_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Home",
                        tint = Color.White
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "BUCKET & COLOR TEST",
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Color(0xFFFFE600),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${colorIndex + 1} / ${allColors.size} • ${if (isNewColor) "NEW COLOR" else "ORIGINAL COLOR"}",
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (isNewColor) Color(0xFF55FFB0) else Color(0xFF8CE4FD)
                    )
                }

                // Auto-Cycle Play/Pause Button
                val autoPress = rememberBouncyPressState(pressedScale = 0.86f)
                IconButton(
                    onClick = {
                        autoPress.triggerPulse()
                        SoundManager.playTap()
                        isAutoPlaying = !isAutoPlaying
                    },
                    interactionSource = autoPress.interactionSource,
                    modifier = Modifier
                        .size(48.dp)
                        .bouncyScale(autoPress)
                        .clip(CircleShape)
                        .background(if (isAutoPlaying) Color(0xFF22C55E) else Color(0xFFE93D5A))
                        .border(2.dp, Color.White, CircleShape)
                        .testTag("color_auto_toggle")
                ) {
                    Icon(
                        imageVector = if (isAutoPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isAutoPlaying) "Pause Auto Cycle" else "Resume Auto Cycle",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scrollable showcase content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Live Animated Bucket Showcase Stage
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF0A4A94), Color(0xFF072D6B), Color(0xFF041C47))
                            )
                        )
                        .border(2.5.dp, Color(0xFF00C4FF), RoundedCornerShape(22.dp))
                        .padding(10.dp)
                ) {
                    // Status pill top-left
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xCC00163B))
                            .border(1.dp, Color(0xFF00C4FF), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "STATE: ${animState.uppercase()} • $filledCount/3",
                            fontFamily = FredokaFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }

                    // Mode pill top-right
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isAutoPlaying) Color(0xFF15803D) else Color(0xFF9A3412))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isAutoPlaying) "AUTO 1-BY-1" else "MANUAL TEST",
                            fontFamily = FredokaFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = Color.White
                        )
                    }

                    // Live Canvas rendering queued next bucket behind + active animated bucket + dropping fluffy + peeking fluffs
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 18.dp)
                    ) {
                        val canvas = drawContext.canvas.nativeCanvas
                        val centerX = size.width / 2f
                        val bucketCenterY = size.height * 0.62f
                        val baltiSize = minOf(size.height * 0.64f, 118.dp.toPx())

                        // Draw Next Queued Bucket peeking behind
                        val (nextSheet, nextRow) = AssetLoader.resolveBucketAtlas(context, nextColor)
                        if (nextSheet != null) {
                            val qW = baltiSize * 0.78f
                            val qH = baltiSize * 0.78f
                            val qSrc = Rect(0, nextRow * 192, 192, (nextRow + 1) * 192)
                            val qDst = RectF(
                                centerX - qW / 2f,
                                bucketCenterY - baltiSize * 0.24f - qH / 2f,
                                centerX + qW / 2f,
                                bucketCenterY - baltiSize * 0.24f + qH / 2f
                            )
                            val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = 165 }
                            canvas.drawBitmap(nextSheet, qSrc, qDst, dimPaint)
                        }

                        // Draw Active Bucket using exact game atlas & frame index
                        val (activeSheet, activeRow) = AssetLoader.resolveBucketAtlas(context, activeColor)
                        val frameIndex = AssetLoader.getBucketFrameIndex(animState, animTimer, filledCount)
                        if (activeSheet != null) {
                            val srcRect = Rect(
                                frameIndex * 192,
                                activeRow * 192,
                                (frameIndex + 1) * 192,
                                (activeRow + 1) * 192
                            )
                            val dstRect = RectF(
                                centerX - baltiSize / 2f,
                                bucketCenterY - baltiSize / 2f,
                                centerX + baltiSize / 2f,
                                bucketCenterY + baltiSize / 2f
                            )
                            canvas.drawBitmap(activeSheet, srcRect, dstRect, null)
                        }

                        // Draw Peeking Fluffies inside filled holes with bright rim separation & dark-color lift
                        if (filledCount > 0 && animState != "vanish") {
                            BucketFluffyRenderer.drawPeekingFluffies(
                                canvas = canvas,
                                context = context,
                                color = activeColor,
                                centerX = centerX,
                                bucketCenterY = bucketCenterY,
                                baltiSize = baltiSize,
                                filledCount = filledCount,
                                alpha = 255,
                                moodProvider = { if (animState == "hold_full") "happy_open" else "content" }
                            )
                        }

                        // Draw Dropping Fluffy flying into opening
                        if (dropProgress >= 0f) {
                            val t = dropProgress.coerceIn(0f, 1f)
                            val targetHole = minOf(filledCount, 2)
                            val holeOffsets = floatArrayOf(-baltiSize * 0.21f, 0f, baltiSize * 0.21f)
                            val startX = centerX
                            val startY = bucketCenterY - baltiSize * 0.72f
                            val endX = centerX + holeOffsets[targetHole]
                            val endY = bucketCenterY - baltiSize * 0.08f
                            val curX = startX + (endX - startX) * t
                            val curY = startY + (endY - startY) * t - 12.dp.toPx() * sin(t * PI.toFloat())
                            val ballR = baltiSize * 0.16f
                            val dropBmp = AssetLoader.getFluffy(context, activeColor, "happy_open")
                            if (dropBmp != null) {
                                canvas.drawBitmap(
                                    dropBmp,
                                    null,
                                    RectF(curX - ballR, curY - ballR, curX + ballR, curY + ballR),
                                    null
                                )
                            }
                        }

                        // Vanish joyful fluffy celebration pop
                        if (animState == "vanish") {
                            val p = (animTimer / 0.50f).coerceIn(0f, 1f)
                            val flyBmp = AssetLoader.getFluffy(context, activeColor, "eyes_closed")
                            if (flyBmp != null) {
                                val alpha = ((1f - p) * 255).toInt().coerceIn(0, 255)
                                val popPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.alpha = alpha }
                                val spreads = floatArrayOf(-28.dp.toPx(), 0f, 28.dp.toPx())
                                val r = baltiSize * 0.15f * (1f + p * 0.35f)
                                spreads.forEachIndexed { idx, dx ->
                                    val fy = bucketCenterY - p * (55.dp.toPx() + idx * 8.dp.toPx())
                                    val fx = centerX + dx * (0.4f + p * 0.8f)
                                    canvas.drawBitmap(
                                        flyBmp,
                                        null,
                                        RectF(fx - r, fy - r, fx + r, fy + r),
                                        popPaint
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Prev / Active Color Name / Next Stepper + Manual Drop Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val prevPress = rememberBouncyPressState(pressedScale = 0.86f)
                    IconButton(
                        onClick = {
                            prevPress.triggerPulse()
                            SoundManager.playTap()
                            selectColor(colorIndex - 1)
                        },
                        interactionSource = prevPress.interactionSource,
                        modifier = Modifier
                            .size(48.dp)
                            .bouncyScale(prevPress)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0B5FA7))
                            .border(2.dp, Color(0xFF00C4FF), RoundedCornerShape(14.dp))
                            .testTag("color_prev_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous Color",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF001C4D))
                            .border(2.dp, colorDef.highlight, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(colorDef.hex)
                                    .border(1.5.dp, Color.White, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = GameConstants.colorDisplayName(activeColor).uppercase(),
                                fontFamily = FredokaFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    val nextPress = rememberBouncyPressState(pressedScale = 0.86f)
                    IconButton(
                        onClick = {
                            nextPress.triggerPulse()
                            SoundManager.playTap()
                            selectColor(colorIndex + 1)
                        },
                        interactionSource = nextPress.interactionSource,
                        modifier = Modifier
                            .size(48.dp)
                            .bouncyScale(nextPress)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0B5FA7))
                            .border(2.dp, Color(0xFF00C4FF), RoundedCornerShape(14.dp))
                            .testTag("color_next_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next Color",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                // Manual Test Action Row: Step Drop Ball & Replay Animation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val dropPress = rememberBouncyPressState(pressedScale = 0.90f)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .bouncyScale(dropPress)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF4ADE80), Color(0xFF22C55E), Color(0xFF15803D))
                                )
                            )
                            .border(2.dp, Color(0xFFA8F028), RoundedCornerShape(14.dp))
                            .bouncyClickTarget(dropPress) { triggerManualDrop() }
                            .testTag("test_step_drop"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (filledCount < 3) "DROP BALL (${filledCount + 1}/3)" else "REPLAY BUCKET",
                            fontFamily = FredokaFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }

                    val replayPress = rememberBouncyPressState(pressedScale = 0.86f)
                    IconButton(
                        onClick = {
                            replayPress.triggerPulse()
                            SoundManager.playTap()
                            triggerReplayForCurrent()
                        },
                        interactionSource = replayPress.interactionSource,
                        modifier = Modifier
                            .size(48.dp)
                            .bouncyScale(replayPress)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF7C3AED))
                            .border(2.dp, Color(0xFFE9D5FF), RoundedCornerShape(14.dp))
                            .testTag("test_replay_cycle")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Replay Bucket Cycle",
                            tint = Color.White
                        )
                    }
                }

                // 3. Crates & Tray Assets Preview Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xCC06285E))
                        .border(1.5.dp, Color(0xFF00C4FF), RoundedCornerShape(18.dp))
                        .padding(10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "CRATES & CAVE TRAY",
                            fontFamily = FredokaFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = Color(0xFFFFE600),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            AssetThumb("Closed", closedBmp)
                            AssetThumb("Open", openBmp)
                            AssetThumb("Full (9)", fullBmp)
                            AssetThumb("Tray", trayBmp)
                        }
                    }
                }

                // 4. All 8 Fluffy Expressions Preview Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xCC06285E))
                        .border(1.5.dp, Color(0xFF00C4FF), RoundedCornerShape(18.dp))
                        .padding(10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "FLUFFY EXPRESSIONS (8 MOODS)",
                            fontFamily = FredokaFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = Color(0xFFFFE600),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        for (rowIdx in 0..1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                for (colIdx in 0..3) {
                                    val (moodKey, moodLabel) = TEST_FLUFFY_MOODS[rowIdx * 4 + colIdx]
                                    val bmp = remember(activeColor, moodKey) {
                                        AssetLoader.getFluffy(context, activeColor, moodKey)
                                    }
                                    AssetThumb(moodLabel, bmp, thumbSize = 46)
                                }
                            }
                        }
                    }
                }

                // 5. All 16 Colors Quick Switcher (2 rows x 8)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xCC06285E))
                        .border(1.5.dp, Color(0xFF00C4FF), RoundedCornerShape(18.dp))
                        .padding(10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "ALL 16 COLORS (TAP TO JUMP)",
                            fontFamily = FredokaFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = Color(0xFFFFE600),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        for (groupRow in 0..1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                for (slot in 0..7) {
                                    val idx = groupRow * 8 + slot
                                    val c = allColors[idx]
                                    val selected = idx == colorIndex
                                    val bucketThumb = remember(c) { AssetLoader.getRailBucket(context, c) }
                                    val swatchPress = rememberBouncyPressState(pressedScale = 0.85f)
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .bouncyScale(swatchPress)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (selected) Color(0xFF1D4ED8) else Color(0xFF001945))
                                            .border(
                                                width = if (selected) 2.5.dp else 1.dp,
                                                color = if (selected) Color(0xFFFFE600) else Color(0xFF38BDF8),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .bouncyClickTarget(swatchPress) {
                                                SoundManager.playTap()
                                                selectColor(idx)
                                            }
                                            .padding(4.dp)
                                            .testTag("swatch_${c.key}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (bucketThumb != null) {
                                            Image(
                                                bitmap = bucketThumb.asImageBitmap(),
                                                contentDescription = GameConstants.colorDisplayName(c),
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun AssetThumb(
    label: String,
    bitmap: android.graphics.Bitmap?,
    thumbSize: Int = 56
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width((thumbSize + 14).dp)
    ) {
        Box(
            modifier = Modifier
                .size(thumbSize.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF00173B))
                .border(1.dp, Color(0x6600C4FF), RoundedCornerShape(12.dp))
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = label,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontFamily = FredokaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = Color(0xFFE0F2FE),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
