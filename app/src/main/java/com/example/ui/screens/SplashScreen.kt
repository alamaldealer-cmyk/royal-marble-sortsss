package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.assets.AssetLoader
import com.example.ui.components.AnimatedGameLogo
import com.example.ui.theme.FredokaFontFamily
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private data class MarbleCometSpec(
    val assetPath: String,
    val trailColor: Color,
    val fromLeft: Boolean,
    val startDelayFrac: Float,
    val durationFrac: Float,
    val yFracFromTop: Float,
    val arcHeightFrac: Float,
    val sizeDp: Float
)

private val SPLASH_COMETS = listOf(
    MarbleCometSpec("logo/marble_cyan.png", Color(0xFF38BDF8), fromLeft = true, startDelayFrac = 0.00f, durationFrac = 0.20f, yFracFromTop = 0.36f, arcHeightFrac = 0.08f, sizeDp = 42f),
    MarbleCometSpec("logo/marble_pink.png", Color(0xFFFF6EC7), fromLeft = false, startDelayFrac = 0.04f, durationFrac = 0.20f, yFracFromTop = 0.32f, arcHeightFrac = 0.07f, sizeDp = 40f),
    MarbleCometSpec("logo/marble_yellow.png", Color(0xFFFFD83B), fromLeft = true, startDelayFrac = 0.09f, durationFrac = 0.20f, yFracFromTop = 0.48f, arcHeightFrac = 0.06f, sizeDp = 44f),
    MarbleCometSpec("logo/marble_green.png", Color(0xFF4ADE80), fromLeft = false, startDelayFrac = 0.13f, durationFrac = 0.20f, yFracFromTop = 0.45f, arcHeightFrac = 0.07f, sizeDp = 38f),
    MarbleCometSpec("logo/marble_purple.png", Color(0xFFC084FC), fromLeft = true, startDelayFrac = 0.18f, durationFrac = 0.20f, yFracFromTop = 0.41f, arcHeightFrac = 0.08f, sizeDp = 40f),
    MarbleCometSpec("logo/marble_orange.png", Color(0xFFFB923C), fromLeft = false, startDelayFrac = 0.22f, durationFrac = 0.20f, yFracFromTop = 0.51f, arcHeightFrac = 0.06f, sizeDp = 42f)
)

private fun easeOutBack(t: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    val x = (t - 1f).coerceIn(-1f, 0f)
    return 1f + c3 * x * x * x + c1 * x * x
}

@Composable
fun SplashScreen(
    onComplete: () -> Unit,
    durationMs: Long = 6400L // 6400ms progress + 280ms ready hold + 320ms smooth fade = 7.0 seconds total
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // Crisp High-Resolution Assets Only (no upscaled sprite cutouts)
    val bgBitmap = remember {
        AssetLoader.loadBitmap(context, "splash/splash_bg.png")
            ?: AssetLoader.loadBitmap(context, "splash_screen.png")
    }
    val cloudBitmap = remember { AssetLoader.loadBitmap(context, "logo/cloud.png") }
    val runnerHappy = remember { AssetLoader.loadBitmap(context, "logo/floofie_happy_open.png") }
    val runnerCheer = remember { AssetLoader.loadBitmap(context, "logo/floofie_cheer.png") }
    val runnerWink = remember { AssetLoader.loadBitmap(context, "logo/floofie_wink.png") }
    val starGold = remember { AssetLoader.loadBitmap(context, "logo/star_1.png") }
    val cometBitmaps = remember {
        SPLASH_COMETS.associate { spec ->
            spec.assetPath to AssetLoader.loadBitmap(context, spec.assetPath)
        }
    }

    var normProgress by remember { mutableFloatStateOf(0f) }
    var smoothProgress by remember { mutableFloatStateOf(0f) }
    var progressPct by remember { mutableIntStateOf(0) }
    var isFading by remember { mutableStateOf(false) }

    val screenAlpha by animateFloatAsState(
        targetValue = if (isFading) 0f else 1f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "splash_fade_out"
    )

    LaunchedEffect(Unit) {
        val startNs = System.nanoTime()
        val durationNs = durationMs.coerceAtLeast(1000L) * 1_000_000f
        while (true) {
            var done = false
            withFrameNanos { frameNs ->
                val raw = ((frameNs - startNs) / durationNs).coerceIn(0f, 1f)
                // Smooth cubic ease-in-out progress curve at full display refresh rate
                val eased = raw * raw * (3f - 2f * raw)
                normProgress = raw
                smoothProgress = eased
                progressPct = (eased * 100f).toInt().coerceIn(0, 100)
                if (raw >= 1f) {
                    done = true
                }
            }
            if (done) break
        }
        normProgress = 1f
        smoothProgress = 1f
        progressPct = 100
        delay(280)
        isFading = true
        delay(320)
        onComplete()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "splash_motion")
    val sunburstPulse by infiniteTransition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.68f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sunburst"
    )
    val floatBobY by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_bob"
    )
    val sparklePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkle_phase"
    )
    val stripePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bar_stripes"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .alpha(screenAlpha)
            .background(Color(0xFF0077E6))
            .testTag("splash_screen")
    ) {
        val screenW = maxWidth
        val screenH = maxHeight
        val bgScale = 1.0f + 0.055f * smoothProgress

        // 1. High-Res Whimsical Candy Cloud Arena Background with Gentle 60fps Ken Burns Zoom
        if (bgBitmap != null) {
            Image(
                bitmap = bgBitmap.asImageBitmap(),
                contentDescription = "Floofies Arena Background",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = bgScale
                        scaleY = bgScale
                    }
                    .testTag("splash_background"),
                contentScale = ContentScale.Crop
            )
        }

        // 2. Smooth 60fps Canvas Sunburst, 4-Point Sparkle Flares & Streaking Marble Comet Trails
        Canvas(modifier = Modifier.fillMaxSize()) {
            val heroCenter = Offset(size.width * 0.5f, size.height * 0.45f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFF8B8).copy(alpha = sunburstPulse * 0.58f),
                        Color(0xFF6EE7FF).copy(alpha = sunburstPulse * 0.24f),
                        Color.Transparent
                    ),
                    center = heroCenter,
                    radius = size.width * 0.68f
                ),
                center = heroCenter,
                radius = size.width * 0.68f
            )

            // Crisp vector 4-point diamond stars twinkling in the sky
            val starSeeds = listOf(
                Offset(0.16f, 0.20f),
                Offset(0.84f, 0.19f),
                Offset(0.12f, 0.38f),
                Offset(0.88f, 0.36f),
                Offset(0.28f, 0.14f),
                Offset(0.73f, 0.13f),
                Offset(0.22f, 0.62f),
                Offset(0.79f, 0.60f)
            )
            starSeeds.forEachIndexed { idx, pt ->
                val phase = ((sparklePhase + idx * 0.13f) % 1f) * 2f * PI.toFloat()
                val glow = (0.30f + 0.70f * ((sin(phase) + 1f) * 0.5f)).coerceIn(0f, 1f)
                val cx = size.width * pt.x
                val cy = size.height * pt.y
                val arm = (9.dp.toPx()) * (0.55f + 0.55f * glow)
                val inner = arm * 0.24f

                drawCircle(
                    color = Color(0xFFFFF3B0).copy(alpha = glow * 0.32f),
                    radius = arm * 1.15f,
                    center = Offset(cx, cy)
                )
                val starPath = Path().apply {
                    moveTo(cx, cy - arm)
                    lineTo(cx + inner, cy - inner)
                    lineTo(cx + arm, cy)
                    lineTo(cx + inner, cy + inner)
                    lineTo(cx, cy + arm)
                    lineTo(cx - inner, cy + inner)
                    lineTo(cx - arm, cy)
                    lineTo(cx - inner, cy - inner)
                    close()
                }
                drawPath(
                    path = starPath,
                    color = Color.White.copy(alpha = 0.55f + 0.45f * glow)
                )
            }

            // Smooth 60fps glowing comet tails behind streaking marbles
            SPLASH_COMETS.forEach { spec ->
                val localT = ((normProgress - spec.startDelayFrac) / spec.durationFrac).coerceIn(0f, 1f)
                if (normProgress >= spec.startDelayFrac && localT > 0f && localT < 1f) {
                    val tailAlpha = when {
                        localT < 0.15f -> localT / 0.15f
                        localT > 0.78f -> (1f - localT) / 0.22f
                        else -> 1f
                    }.coerceIn(0f, 1f)

                    val steps = 8
                    val startX = if (spec.fromLeft) -0.10f * size.width else 1.10f * size.width
                    val endX = size.width * 0.5f
                    val baseY = size.height * spec.yFracFromTop
                    val arcH = size.height * spec.arcHeightFrac

                    fun evalPos(tVal: Float): Offset {
                        val e = 1f - (1f - tVal) * (1f - tVal)
                        val px = startX + (endX - startX) * e
                        val py = baseY - arcH * sin(tVal * PI.toFloat())
                        return Offset(px, py)
                    }

                    for (s in 0 until steps) {
                        val t0 = (localT - (s + 1) * 0.032f).coerceAtLeast(0f)
                        val t1 = (localT - s * 0.032f).coerceAtLeast(0f)
                        if (t1 > t0) {
                            val p0 = evalPos(t0)
                            val p1 = evalPos(t1)
                            val frac = 1f - s.toFloat() / steps
                            drawLine(
                                color = spec.trailColor.copy(alpha = tailAlpha * frac * 0.55f),
                                start = p0,
                                end = p1,
                                strokeWidth = (18.dp.toPx()) * frac,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = Color.White.copy(alpha = tailAlpha * frac * 0.45f),
                                start = p0,
                                end = p1,
                                strokeWidth = (7.dp.toPx()) * frac,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }

        // 3. Crisp Glossy Streaking Marbles Gliding smoothly on Bezier Arcs Into Center
        SPLASH_COMETS.forEach { spec ->
            val bmp = cometBitmaps[spec.assetPath]
            val localT = ((normProgress - spec.startDelayFrac) / spec.durationFrac).coerceIn(0f, 1f)
            if (bmp != null && normProgress >= spec.startDelayFrac && localT < 1f) {
                val ease = 1f - (1f - localT) * (1f - localT)
                val startX = if (spec.fromLeft) -screenW * 0.60f else screenW * 0.60f
                val curX = startX * (1f - ease)
                val arcLift = -screenH * spec.arcHeightFrac * sin(localT * PI.toFloat())
                val curAlpha = when {
                    localT < 0.14f -> localT / 0.14f
                    localT > 0.78f -> (1f - localT) / 0.22f
                    else -> 1f
                }.coerceIn(0f, 1f)
                val marbleScale = 1.10f - 0.35f * ease

                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(
                            x = curX,
                            y = screenH * spec.yFracFromTop + arcLift - (spec.sizeDp / 2f).dp
                        )
                        .size(spec.sizeDp.dp)
                        .alpha(curAlpha)
                        .graphicsLayer {
                            scaleX = marbleScale
                            scaleY = marbleScale
                            rotationZ = (if (spec.fromLeft) 1f else -1f) * localT * 360f
                        },
                    contentScale = ContentScale.Fit
                )
            }
        }

        // 4. Razor-Sharp Modular Centerpiece Lockup (Zero Torn Edges or Abrupt Phase Jumps)
        val cloudPop = easeOutBack(((normProgress - 0.02f) / 0.24f).coerceIn(0f, 1f))
        val logoPop = easeOutBack(((normProgress - 0.10f) / 0.28f).coerceIn(0f, 1f))
        val logoAlpha = ((normProgress - 0.06f) / 0.14f).coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-34).dp)
                .fillMaxWidth(0.92f)
                .widthIn(max = 390.dp),
            contentAlignment = Alignment.Center
        ) {
            if (cloudBitmap != null) {
                Image(
                    bitmap = cloudBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 32.dp)
                        .fillMaxWidth(0.86f)
                        .aspectRatio(307f / 133f)
                        .alpha((cloudPop.coerceIn(0f, 1f)) * 0.94f)
                        .graphicsLayer {
                            val s = 0.60f + 0.40f * cloudPop
                            scaleX = s
                            scaleY = s
                            translationY = floatBobY * 0.45f
                        },
                    contentScale = ContentScale.Fit
                )
            }

            AnimatedGameLogo(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .alpha(logoAlpha)
                    .graphicsLayer {
                        val s = (0.35f + 0.65f * logoPop).coerceAtLeast(0.01f)
                        scaleX = s
                        scaleY = s
                        translationY = floatBobY * 0.7f
                    },
                testTagName = "splash_animated_logo",
                enableTap = false
            )
        }

        // 5. Silky 60fps Glossy Candy Loading Bar + Bouncing Floofie Runner
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .fillMaxWidth()
                .padding(horizontal = 26.dp)
                .padding(bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .widthIn(max = 330.dp)
                    .height(82.dp)
                    .testTag("splash_loading_bar"),
                contentAlignment = Alignment.BottomCenter
            ) {
                val trackW = maxWidth
                val barHeightDp = 28.dp
                val runnerSizeDp = 52.dp
                val trackPaddingPx = with(density) { 4.dp.toPx() }

                val isReady = progressPct >= 100
                val bounceFreq = 12f
                val bounceSin = abs(sin(smoothProgress * PI.toFloat() * bounceFreq))
                val runnerJumpDp = if (isReady) {
                    -10f + floatBobY * 0.8f
                } else {
                    -10f * bounceSin
                }
                val runnerSquashX = if (isReady) 1.06f else 1.0f + (1f - bounceSin) * 0.08f
                val runnerSquashY = if (isReady) 0.96f else 1.0f - (1f - bounceSin) * 0.08f
                val runnerTilt = if (isReady) 0f else sin(smoothProgress * PI.toFloat() * bounceFreq) * 5f

                // Continuous 60fps Candy Progress Tube
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeightDp)
                        .align(Alignment.BottomCenter)
                ) {
                    val corner = size.height / 2f
                    // Soft outer shadow
                    drawRoundRect(
                        color = Color(0x55001642),
                        topLeft = Offset(0f, 3.dp.toPx()),
                        size = size,
                        cornerRadius = CornerRadius(corner, corner)
                    )
                    // Royal blue outer frame
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFF073B8C))
                        ),
                        size = size,
                        cornerRadius = CornerRadius(corner, corner)
                    )
                    // Deep navy inner track
                    val innerPad = trackPaddingPx
                    val innerW = (size.width - innerPad * 2f).coerceAtLeast(1f)
                    val innerH = (size.height - innerPad * 2f).coerceAtLeast(1f)
                    val innerCorner = innerH / 2f
                    drawRoundRect(
                        color = Color(0xFF052159),
                        topLeft = Offset(innerPad, innerPad),
                        size = Size(innerW, innerH),
                        cornerRadius = CornerRadius(innerCorner, innerCorner)
                    )

                    // Smooth continuous cyan candy fill
                    val fillW = (innerW * smoothProgress).coerceIn(if (smoothProgress > 0f) innerH else 0f, innerW)
                    if (fillW > 0f) {
                        val fillClip = Path().apply {
                            addRoundRect(
                                RoundRect(
                                    rect = Rect(innerPad, innerPad, innerPad + fillW, innerPad + innerH),
                                    cornerRadius = CornerRadius(innerCorner, innerCorner)
                                )
                            )
                        }
                        clipPath(fillClip) {
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    listOf(Color(0xFF0EA5E9), Color(0xFF22D3EE), Color(0xFF7CE7FF))
                                ),
                                topLeft = Offset(innerPad, innerPad),
                                size = Size(fillW, innerH)
                            )

                            // Animated diagonal glossy candy stripes gliding at 60fps
                            val stripePitch = 22.dp.toPx()
                            val shift = stripePhase * stripePitch
                            var sx = -innerH + shift
                            while (sx < fillW + innerH) {
                                val stripePath = Path().apply {
                                    moveTo(innerPad + sx, innerPad + innerH)
                                    lineTo(innerPad + sx + innerH * 0.55f, innerPad)
                                    lineTo(innerPad + sx + innerH * 0.55f + stripePitch * 0.42f, innerPad)
                                    lineTo(innerPad + sx + stripePitch * 0.42f, innerPad + innerH)
                                    close()
                                }
                                drawPath(
                                    path = stripePath,
                                    color = Color.White.copy(alpha = 0.24f)
                                )
                                sx += stripePitch
                            }

                            // Top glossy glass highlight strip
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.38f),
                                topLeft = Offset(innerPad + 3.dp.toPx(), innerPad + 2.dp.toPx()),
                                size = Size((fillW - 6.dp.toPx()).coerceAtLeast(0f), innerH * 0.34f),
                                cornerRadius = CornerRadius(innerCorner, innerCorner)
                            )
                        }
                    }

                    // Crisp white-cyan specular outer rim stroke
                    drawRoundRect(
                        color = Color(0xCCBAE6FD),
                        size = size,
                        cornerRadius = CornerRadius(corner, corner),
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Smooth golden dust sparkles trailing behind the runner's fill tip
                    val tipX = innerPad + fillW
                    if (!isReady && fillW > 12f) {
                        for (sp in 0 until 4) {
                            val spFrac = ((sparklePhase * 3f + sp * 0.25f) % 1f)
                            val px = tipX - spFrac * 26.dp.toPx()
                            val py = innerPad - (4.dp.toPx() + sin(spFrac * PI.toFloat()) * 10.dp.toPx())
                            drawCircle(
                                color = Color(0xFFFFE600).copy(alpha = (1f - spFrac) * 0.85f),
                                radius = (3.5.dp.toPx()) * (1f - spFrac * 0.5f),
                                center = Offset(px, py)
                            )
                        }
                    }
                }

                // Bouncing High-Res Hero Floofie Runner Riding the Live Progress Tip
                val runnerBmp = when {
                    isReady -> runnerCheer ?: runnerHappy
                    ((smoothProgress * 8f).toInt() % 5 == 4) -> runnerWink ?: runnerHappy
                    else -> runnerHappy
                }
                if (runnerBmp != null) {
                    val minX = 6.dp
                    val maxX = (trackW - runnerSizeDp - 4.dp).coerceAtLeast(minX)
                    val runnerX = minX + (maxX - minX) * smoothProgress

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(
                                x = runnerX,
                                y = -barHeightDp + 6.dp + runnerJumpDp.dp
                            )
                            .size(runnerSizeDp)
                    ) {
                        if (isReady && starGold != null) {
                            Image(
                                bitmap = starGold.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-6).dp)
                            )
                        }
                        Image(
                            bitmap = runnerBmp.asImageBitmap(),
                            contentDescription = "Floofie Runner",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = runnerSquashX
                                    scaleY = runnerSquashY
                                    rotationZ = runnerTilt
                                },
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .background(Color(0xC803235E), RoundedCornerShape(16.dp))
                    .border(1.5.dp, Color(0xFF7CE7FF), RoundedCornerShape(16.dp))
                    .padding(horizontal = 18.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (progressPct >= 100) "READY!" else "LOADING...",
                    fontFamily = FredokaFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = Color.White,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "$progressPct%",
                    fontFamily = FredokaFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = Color(0xFFFFE382)
                )
            }
        }
    }
}
