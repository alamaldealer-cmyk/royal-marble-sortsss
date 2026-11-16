package com.example.ui.components

import android.graphics.Paint
import android.graphics.RectF
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
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.assets.AssetLoader
import com.example.game.belt.BeltPath
import com.example.game.constants.GameConstants
import com.example.game.model.BeltBall
import com.example.game.model.CellKind
import com.example.game.model.FunnelBall
import com.example.game.model.GridCell
import com.example.game.model.MarbleColor
import com.example.game.rules.GameRules
import com.example.game.sound.SoundManager
import com.example.ui.theme.FredokaFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun OutOfSpaceModal(
    coins: Int,
    previewSnapshot: GameRules.CleanUpPreviewSnapshot? = null,
    onWatchAdPlayOn: () -> Unit,
    onCoinsPlayOn: () -> Unit,
    onCloseToHome: () -> Unit
) {
    val context = LocalContext.current

    val panelBmp = remember { AssetLoader.loadBitmap(context, "cleanup/panel_out_of_space.png") }
    val closeXBmp = remember { AssetLoader.loadBitmap(context, "cleanup/btn_close_x.png") }
    val watchAdBmp = remember { AssetLoader.loadBitmap(context, "cleanup/btn_play_on_watch_ad.png") }
    val coinsBtnBmp = remember { AssetLoader.loadBitmap(context, "cleanup/btn_play_on_coins.png") }

    val modalScale = remember { Animatable(0.90f) }
    val modalAlpha = remember { Animatable(0.15f) }

    var insufficientCoinsTick by remember { mutableIntStateOf(0) }
    var showNotEnoughCoinsBanner by remember { mutableStateOf(false) }
    val coinsShakeOffset = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            modalAlpha.animateTo(1f, tween(durationMillis = 140))
        }
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
            .testTag("out_of_space_modal")
            .background(Color.Black.copy(alpha = 0.56f))
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
                .widthIn(max = 356.dp)
                .fillMaxWidth(0.88f)
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
                    .aspectRatio(0.76f)
            ) {
                // Panel Background
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

                // Header Row (Top ~14.5% of panel_out_of_space.png)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.145f)
                        .align(Alignment.TopCenter)
                ) {
                    OutlinedCartoonText(
                        text = "Out of Space!",
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
                            .testTag("out_of_space_close_button")
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
                                modifier = Modifier.size(40.dp),
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

                // Main Content Body (Bottom ~85.5% of panel)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.855f)
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // 1. Live Animated Preview Box showing exact current board & post-Clean Up recovery
                    CleanUpPreviewWindow(
                        snapshot = previewSnapshot,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.52f)
                            .testTag("out_of_space_preview")
                    )

                    // 2. Explanation Copy
                    OutlinedCartoonText(
                        text = "Clear up the board and free\nsome space to keep playing!",
                        fontSize = 15.sp,
                        lineHeight = 19.sp,
                        strokeColor = Color(0xFF17457D),
                        strokeWidth = 5.5f,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    // 3. Play On! Action Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Play On! Watch Ad
                        val watchAdPress = rememberBouncyPressState(pressedScale = 0.90f)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(2584f / 1793f)
                                .minimumInteractiveComponentSize()
                                .testTag("out_of_space_watch_ad_button")
                                .bouncyClickTarget(watchAdPress, role = Role.Button) {
                                    SoundManager.playTap()
                                    onWatchAdPlayOn()
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

                        // Right: Play On! 900 Coins
                        val coinsPress = rememberBouncyPressState(pressedScale = 0.90f)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .offset { IntOffset(x = coinsShakeOffset.value.roundToInt(), y = 0) }
                                .aspectRatio(2481f / 1738f)
                                .minimumInteractiveComponentSize()
                                .testTag("out_of_space_coins_button")
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
    }
}

@Composable
private fun OutlinedCartoonText(
    text: String,
    fontSize: TextUnit,
    strokeColor: Color,
    strokeWidth: Float,
    modifier: Modifier = Modifier,
    lineHeight: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign = TextAlign.Center
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = text,
            textAlign = textAlign,
            style = TextStyle(
                fontFamily = FredokaFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = fontSize,
                lineHeight = lineHeight,
                color = strokeColor,
                drawStyle = Stroke(
                    width = strokeWidth,
                    join = StrokeJoin.Round
                ),
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
            style = TextStyle(
                fontFamily = FredokaFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = fontSize,
                lineHeight = lineHeight,
                color = Color.White
            )
        )
    }
}

@Composable
private fun CleanUpPreviewWindow(
    snapshot: GameRules.CleanUpPreviewSnapshot?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cyberFrameBitmap = remember { AssetLoader.loadBitmap(context, "cyber_game_frame.png") }
    val bgBitmap = remember { AssetLoader.loadBitmap(context, "radiant_cyan_blue_gradient.png") }
    val crateColNormX = remember { floatArrayOf(0.258f, 0.500f, 0.742f) }
    val crateRowNormY = remember { floatArrayOf(0.268f, 0.488f, 0.708f) }
    val crateSlotMoods = remember {
        arrayOf(
            "happy_open", "wink", "curious",
            "content", "happy_open", "wink",
            "curious", "content", "happy_open"
        )
    }

    val effectiveSnapshot = remember(snapshot) {
        snapshot ?: buildFallbackPreviewSnapshot()
    }

    val initialMap = remember(effectiveSnapshot) {
        effectiveSnapshot.initialCells.associateBy { Pair(it.row, it.col) }
    }
    val targetMap = remember(effectiveSnapshot) {
        effectiveSnapshot.targetCells.associateBy { Pair(it.row, it.col) }
    }
    val flightsByCell = remember(effectiveSnapshot) {
        effectiveSnapshot.flights.groupBy { Pair(it.targetRow, it.targetCol) }
    }

    val transition = rememberInfiniteTransition(label = "cleanup_preview_loop")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2700, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "preview_cycle"
    )

    Box(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1F66B8))
            .border(2.5.dp, Color(0xFF3E6494), RoundedCornerShape(14.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvas = drawContext.canvas.nativeCanvas
            val w = size.width
            val h = size.height

            // 1. Game background gradient across the preview card
            if (bgBitmap != null) {
                canvas.drawBitmap(bgBitmap, null, RectF(0f, 0f, w, h), null)
            } else {
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF2875CC), Color(0xFF0E3E82))
                    )
                )
            }

            // Frame real arena bounds cleanly (upper grid + funnel + conveyor belt)
            val viewTopY = 162f
            val viewBottomY = 504f
            val viewHeight = viewBottomY - viewTopY
            val scale = h / viewHeight
            val offsetX = (w - GameConstants.BASE_WIDTH * scale) * 0.5f
            val offsetY = -viewTopY * scale

            canvas.save()
            canvas.translate(offsetX, offsetY)
            canvas.scale(scale, scale)

            // 2. Real Cyber Game Frame (grid background + funnel silhouette)
            if (cyberFrameBitmap != null) {
                canvas.drawBitmap(
                    cyberFrameBitmap,
                    null,
                    RectF(0f, 117f, 296f, 606f),
                    null
                )
            } else {
                val frameBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.parseColor("#8CA8C7")
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(RectF(18f, 165f, 278f, 445f), 16f, 16f, frameBgPaint)
            }

            // Timeline phases:
            // 0.00..0.18: Real current crowded board
            // 0.18..0.34: Golden aura highlights funnel balls + 8 chain balls
            // 0.32..0.72: Balls fly upward along real Bezier arcs into exact target cells
            // 0.72..0.95: Freed-up funnel & belt + newly created upper containers held clearly
            // 0.95..1.00: Smooth fade reset
            val flightDuration = 0.24f
            val auraStrength = when {
                progress < 0.16f -> 0f
                progress < 0.34f -> (progress - 0.16f) / 0.18f
                progress < 0.72f -> 1f - ((progress - 0.34f) / 0.38f)
                else -> 0f
            }

            // Helper to draw a crisp real or fallback Fluffy marble
            fun drawMiniFluffy(x: Float, y: Float, radius: Float, color: MarbleColor, mood: String = "surprised") {
                val bmp = AssetLoader.getFluffy(context, color, mood)
                if (bmp != null) {
                    canvas.drawBitmap(bmp, null, RectF(x - radius, y - radius, x + radius, y + radius), null)
                } else {
                    val cDef = GameConstants.COLOR_MAP[color]
                    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        this.color = (cDef?.hex ?: Color.Yellow).toArgb()
                        style = Paint.Style.FILL
                    }
                    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        this.color = (cDef?.border ?: Color.DarkGray).toArgb()
                        style = Paint.Style.STROKE
                        strokeWidth = 1.4f
                    }
                    canvas.drawCircle(x, y, radius * 0.85f, fillPaint)
                    canvas.drawCircle(x, y, radius * 0.85f, borderPaint)
                }
            }

            // 3. Draw Upper Grid Cells (transitioning dynamically from initialCells -> targetCells)
            val crateW = 34f
            val crateH = 33f
            for (r in 0 until GameConstants.GRID_ROWS) {
                for (c in 0 until GameConstants.GRID_COLS) {
                    val key = Pair(r, c)
                    val initCell = initialMap[key] ?: continue
                    val targetCell = targetMap[key] ?: initCell
                    val cellFlights = flightsByCell[key].orEmpty()

                    val cx = GameConstants.GRID_X_CENTERS[c]
                    val cy = GameConstants.GRID_Y_CENTERS[r]

                    var arrivedCount = 0
                    var earliestLaunch = 1f
                    for (fl in cellFlights) {
                        val fStart = 0.30f + fl.delayFrac
                        if (fStart < earliestLaunch) earliestLaunch = fStart
                        val localT = ((progress - fStart) / flightDuration).coerceIn(0f, 1f)
                        if (localT >= 1f) arrivedCount++
                    }

                    val wasBlock = initCell.kind == CellKind.BLOCK
                    val willBeBlock = targetCell.kind == CellKind.BLOCK
                    val isNewContainer = !wasBlock && willBeBlock

                    val displayCell = when {
                        isNewContainer && progress >= earliestLaunch -> targetCell
                        isNewContainer -> initCell
                        else -> if (arrivedCount > 0) targetCell else initCell
                    }

                    val animScale = if (isNewContainer && progress >= earliestLaunch) {
                        val popP = ((progress - earliestLaunch) / 0.14f).coerceIn(0f, 1f)
                        0.30f + 0.70f * FastOutSlowInEasing.transform(popP)
                    } else {
                        1f
                    }

                    val activeBallCount = if (displayCell.kind == CellKind.BLOCK) {
                        if (isNewContainer) {
                            arrivedCount.coerceIn(0, GameConstants.BALLS_PER_BLOCK)
                        } else {
                            (initCell.ballCount + arrivedCount).coerceIn(0, GameConstants.BALLS_PER_BLOCK)
                        }
                    } else {
                        0
                    }

                    when (displayCell.kind) {
                        CellKind.BLOCK -> {
                            val col = displayCell.color ?: MarbleColor.ORANGE
                            val linkInset = if (displayCell.linkGroup != null) 3.0f else 0f
                            val sw = (crateW - linkInset) * animScale
                            val sh = (crateH - linkInset) * animScale
                            val dst = RectF(cx - sw / 2f, cy - sh / 2f, cx + sw / 2f, cy + sh / 2f)

                            if (cellFlights.isNotEmpty() && progress >= earliestLaunch && arrivedCount < cellFlights.size) {
                                val magnetGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    color = android.graphics.Color.parseColor("#88FFE875")
                                    style = Paint.Style.STROKE
                                    strokeWidth = 2.2f
                                }
                                canvas.drawRoundRect(
                                    RectF(dst.left - 1.8f, dst.top - 1.8f, dst.right + 1.8f, dst.bottom + 1.8f),
                                    6.5f,
                                    6.5f,
                                    magnetGlow
                                )
                            }

                            val crateFullBmp = AssetLoader.getCrateFull(context, col)
                            val crateOpenBmp = AssetLoader.getCrateOpen(context, col)
                            if (displayCell.isMystery && cellFlights.isEmpty()) {
                                val mysteryBmp = AssetLoader.loadBitmap(context, "crates/crate_mystery.png")
                                    ?: crateFullBmp
                                if (mysteryBmp != null) {
                                    canvas.drawBitmap(mysteryBmp, null, dst, null)
                                }
                            } else if (activeBallCount >= GameConstants.BALLS_PER_BLOCK &&
                                (cellFlights.isEmpty() || progress >= 0.74f) &&
                                crateFullBmp != null
                            ) {
                                canvas.drawBitmap(crateFullBmp, null, dst, null)
                            } else {
                                val containerBmp = crateOpenBmp ?: crateFullBmp
                                if (containerBmp != null) {
                                    canvas.drawBitmap(containerBmp, null, dst, null)
                                } else {
                                    val cDef = GameConstants.COLOR_MAP[col]
                                    val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                        this.color = (cDef?.hex ?: Color.Cyan).toArgb()
                                        style = Paint.Style.FILL
                                    }
                                    canvas.drawRoundRect(dst, 5f, 5f, boxPaint)
                                }
                                if (activeBallCount > 0) {
                                    val brX = sw * 0.153f
                                    val brY = sh * 0.156f
                                    for (sRow in 0..2) {
                                        for (sCol in intArrayOf(0, 2, 1)) {
                                            val slotIdx = sRow * 3 + sCol
                                            if (slotIdx >= activeBallCount) continue
                                            val bx = dst.left + crateColNormX[sCol] * sw
                                            val by = dst.top + crateRowNormY[sRow] * sh
                                            drawMiniFluffy(bx, by, minOf(brX, brY), col, crateSlotMoods[slotIdx])
                                        }
                                    }
                                }
                            }
                        }

                        CellKind.LOCKED -> {
                            val col = displayCell.color ?: MarbleColor.PURPLE
                            val crateClosed = AssetLoader.getCrateClosed(context, col)
                                ?: AssetLoader.getCrateFull(context, col)
                            val dst = RectF(cx - crateW / 2f, cy - crateH / 2f, cx + crateW / 2f, cy + crateH / 2f)
                            if (crateClosed != null) {
                                canvas.drawBitmap(crateClosed, null, dst, null)
                            } else {
                                val lockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    color = android.graphics.Color.parseColor("#6B7A8C")
                                    style = Paint.Style.FILL
                                }
                                canvas.drawRoundRect(dst, 5f, 5f, lockPaint)
                            }
                        }

                        CellKind.DISPENSER -> {
                            val dispBmp = AssetLoader.getDispenserFrame(context, 0)
                            val dst = RectF(cx - crateW / 2f, cy - crateH / 2f, cx + crateW / 2f, cy + crateH / 2f)
                            if (dispBmp != null) {
                                canvas.drawBitmap(dispBmp, null, dst, null)
                            }
                        }

                        else -> Unit
                    }
                }
            }

            // 4. Conveyor Belt Stadium Capsule & Sockets
            val beltCenterY = (GameConstants.BELT_TOP_Y + GameConstants.BELT_BOTTOM_Y) / 2f
            val capsuleX = 36f
            val capsuleW = 224f
            val capsuleY = beltCenterY - 22f
            val capsuleH = 44f
            val capsuleRx = 22f
            val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#826C8D")
                style = Paint.Style.FILL
            }
            val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#E5EBFF")
                style = Paint.Style.STROKE
                strokeWidth = 3.5f
            }
            val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(RectF(capsuleX, capsuleY, capsuleX + capsuleW, capsuleY + capsuleH), capsuleRx, capsuleRx, trackPaint)
            canvas.drawRoundRect(RectF(capsuleX, capsuleY, capsuleX + capsuleW, capsuleY + capsuleH), capsuleRx, capsuleRx, rimPaint)
            canvas.drawRoundRect(RectF(58f, beltCenterY - 1.75f, 238f, beltCenterY + 1.75f), 1.75f, 1.75f, dividerPaint)

            val socketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#5E4E6C")
                style = Paint.Style.FILL
            }
            val cap = effectiveSnapshot.beltCapacity.coerceAtLeast(1)
            val slotSpacing = BeltPath.TOTAL_PATH_LEN / cap
            val socketRadius = if (cap > 24) 6.5f else 8.2f
            for (i in 0 until cap) {
                val s = (i * slotSpacing + effectiveSnapshot.beltScrollDistance) % BeltPath.TOTAL_PATH_LEN
                val coords = BeltPath.getBeltCoordinates(s)
                canvas.drawCircle(coords.x, coords.y, socketRadius, socketPaint)
            }

            // 5. Real Belt Balls that stay on the conveyor belt
            val beltBallScale = if (cap > 24) 0.85f else 1f
            val beltRadius = GameConstants.BALL_RADIUS_BELT * 1.35f * beltBallScale
            for (bb in effectiveSnapshot.beltBalls) {
                if (!bb.isReturned) {
                    drawMiniFluffy(bb.x, bb.y, beltRadius, bb.color, "content")
                }
            }

            // 6. Golden Aura Glow around funnel + top chain during charge-up
            if (auraStrength > 0.02f) {
                val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.parseColor("#FFE875")
                    alpha = (120 * auraStrength).toInt().coerceIn(0, 255)
                    style = Paint.Style.FILL
                }
                for (bb in effectiveSnapshot.beltBalls) {
                    if (bb.isHighlighted) {
                        canvas.drawCircle(bb.x, bb.y, beltRadius * 1.42f, glowPaint)
                    }
                }
            }

            // 7. Real Returned Balls (Funnel + Highlighted 8 Chain Balls) waiting -> flying -> landed
            val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
            }
            val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#FFF08A")
                style = Paint.Style.FILL
            }

            for (fl in effectiveSnapshot.flights) {
                val fStart = 0.30f + fl.delayFrac
                val localT = ((progress - fStart) / flightDuration).coerceIn(0f, 1f)
                val eased = FastOutSlowInEasing.transform(localT)
                val baseR = GameConstants.BALL_RADIUS_FUNNEL * 1.32f

                if (progress < fStart) {
                    // Sitting in its exact real pre-cleanup position on funnel or belt
                    val jitter = if (auraStrength > 0f) sin(progress * 42f + fl.startX * 0.2f) * 1.1f else 0f
                    if (auraStrength > 0f) {
                        haloPaint.alpha = (165 * auraStrength).toInt().coerceIn(0, 255)
                        canvas.drawCircle(fl.startX + jitter, fl.startY, baseR * 1.32f, haloPaint)
                    }
                    drawMiniFluffy(fl.startX + jitter, fl.startY, baseR, fl.color, "surprised")
                } else if (localT < 1f) {
                    val inv = 1f - eased
                    val curX = inv * inv * fl.startX + 2f * inv * eased * fl.controlX + eased * eased * fl.targetX
                    val curY = inv * inv * fl.startY + 2f * inv * eased * fl.controlY + eased * eased * fl.targetY

                    // Golden motion trail
                    for (step in 1..3) {
                        val te = (eased - step * 0.08f).coerceIn(0f, 1f)
                        val tinv = 1f - te
                        val tx = tinv * tinv * fl.startX + 2f * tinv * te * fl.controlX + te * te * fl.targetX
                        val ty = tinv * tinv * fl.startY + 2f * tinv * te * fl.controlY + te * te * fl.targetY
                        trailPaint.color = android.graphics.Color.parseColor("#FFE45E")
                        trailPaint.alpha = (145 - step * 35).coerceIn(0, 255)
                        canvas.drawCircle(tx, ty, baseR * (0.72f - step * 0.14f), trailPaint)
                    }

                    haloPaint.alpha = 185
                    canvas.drawCircle(curX, curY, baseR * 1.25f, haloPaint)
                    val shrinkScale = 1f - eased * 0.42f
                    drawMiniFluffy(curX, curY, baseR * shrinkScale, fl.color, "happy_open")
                }
            }

            // 8. Celebration sparkles when space clears up (progress 0.68..0.92)
            if (progress in 0.68f..0.92f) {
                val sp = ((progress - 0.68f) / 0.24f).coerceIn(0f, 1f)
                val spAlpha = ((1f - sp) * 210).toInt().coerceIn(0, 255)
                val sparkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.parseColor("#FFF078")
                    alpha = spAlpha
                    style = Paint.Style.FILL
                }
                for (i in 0 until 6) {
                    val ang = i * (PI.toFloat() / 3f) + 0.25f
                    val dist = 12f + sp * 34f
                    val sx = 148f + cos(ang) * dist
                    val sy = 415f + sin(ang) * (dist * 0.45f)
                    canvas.drawCircle(sx, sy, 3.2f * (1f - sp * 0.4f), sparkPaint)
                }
            }

            canvas.restore()
        }
    }
}

private fun buildFallbackPreviewSnapshot(): GameRules.CleanUpPreviewSnapshot {
    val grid = Array(GameConstants.GRID_ROWS) { r ->
        Array(GameConstants.GRID_COLS) { c ->
            when {
                r == 0 && c == 0 -> GridCell(kind = CellKind.BLOCK, color = MarbleColor.ORANGE, ballCount = 9)
                r == 0 && c == 5 -> GridCell(kind = CellKind.BLOCK, color = MarbleColor.CYAN, ballCount = 9)
                r == 1 && c == 0 -> GridCell(kind = CellKind.BLOCK, color = MarbleColor.RED, ballCount = 9)
                r == 1 && c == 5 -> GridCell(kind = CellKind.BLOCK, color = MarbleColor.YELLOW, ballCount = 9)
                else -> GridCell(kind = CellKind.EMPTY)
            }
        }
    }
    val funnel = listOf(
        FunnelBall(1, MarbleColor.ORANGE, 118f, 408f, 0f, 0f),
        FunnelBall(2, MarbleColor.BLUE, 134f, 416f, 0f, 0f),
        FunnelBall(3, MarbleColor.YELLOW, 150f, 412f, 0f, 0f),
        FunnelBall(4, MarbleColor.LIME, 166f, 416f, 0f, 0f),
        FunnelBall(5, MarbleColor.ORANGE, 142f, 430f, 0f, 0f),
        FunnelBall(6, MarbleColor.BLUE, 156f, 432f, 0f, 0f)
    )
    val spacing = BeltPath.TOTAL_PATH_LEN / 24f
    val belt = (0 until 24).map { idx ->
        BeltBall(
            id = 100 + idx,
            color = if (idx < 14) MarbleColor.ORANGE else MarbleColor.BLUE,
            slotIndex = idx,
            s = idx * spacing
        )
    }
    return GameRules.buildCleanUpPreviewSnapshot(
        grid = grid,
        funnelBalls = funnel,
        beltBalls = belt,
        highlightedBeltIds = emptySet(),
        beltCapacity = 24
    )
}
