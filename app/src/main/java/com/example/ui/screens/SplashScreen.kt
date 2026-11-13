package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.assets.AssetLoader
import com.example.ui.theme.FredokaFontFamily
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

private data class DashingFluffySpec(
    val assetIndex: Int,
    val fromLeft: Boolean,
    val startDelayFrac: Float,
    val durationFrac: Float,
    val yFracFromTop: Float,
    val arcDp: Float
)

private val DASHING_FLUFFIES = listOf(
    DashingFluffySpec(assetIndex = 0, fromLeft = true, startDelayFrac = 0.00f, durationFrac = 0.20f, yFracFromTop = 0.44f, arcDp = 34f),  // Purple
    DashingFluffySpec(assetIndex = 7, fromLeft = false, startDelayFrac = 0.04f, durationFrac = 0.20f, yFracFromTop = 0.40f, arcDp = 30f), // Cyan
    DashingFluffySpec(assetIndex = 2, fromLeft = true, startDelayFrac = 0.09f, durationFrac = 0.20f, yFracFromTop = 0.54f, arcDp = 24f),  // Green
    DashingFluffySpec(assetIndex = 5, fromLeft = false, startDelayFrac = 0.13f, durationFrac = 0.20f, yFracFromTop = 0.52f, arcDp = 26f), // Red
    DashingFluffySpec(assetIndex = 6, fromLeft = true, startDelayFrac = 0.18f, durationFrac = 0.20f, yFracFromTop = 0.47f, arcDp = 28f),  // Pink
    DashingFluffySpec(assetIndex = 4, fromLeft = false, startDelayFrac = 0.22f, durationFrac = 0.20f, yFracFromTop = 0.56f, arcDp = 22f), // Orange
    DashingFluffySpec(assetIndex = 8, fromLeft = false, startDelayFrac = 0.27f, durationFrac = 0.20f, yFracFromTop = 0.45f, arcDp = 24f), // Ice
    DashingFluffySpec(assetIndex = 3, fromLeft = true, startDelayFrac = 0.31f, durationFrac = 0.20f, yFracFromTop = 0.42f, arcDp = 32f)   // Golden
)

@Composable
fun SplashScreen(
    onComplete: () -> Unit,
    durationMs: Long = 6400L // 6400ms progress + 280ms ready hold + 320ms fade = 7.0 seconds total
) {
    val context = LocalContext.current

    // Background & Sprite Frames
    val bgBitmap = remember {
        AssetLoader.loadBitmap(context, "splash/splash_bg.png")
            ?: AssetLoader.loadBitmap(context, "splash_screen.png")
    }
    val bannerIntroBitmap = remember {
        AssetLoader.loadBitmap(context, "splash/banner_intro_00.png")
    }
    val titleFrames = remember {
        (0..3).mapNotNull { AssetLoader.loadBitmap(context, "splash/title_%02d.png".format(it)) }
    }
    val lockupFrames = remember {
        (0..3).mapNotNull { AssetLoader.loadBitmap(context, "splash/lockup_%02d.png".format(it)) }
    }
    val barFrames = remember {
        (0..11).mapNotNull { AssetLoader.loadBitmap(context, "splash/bar_%02d.png".format(it)) }
    }
    val dashBitmaps = remember {
        (0..8).mapNotNull { AssetLoader.loadBitmap(context, "splash/dash_%02d.png".format(it)) }
    }

    var normProgress by remember { mutableFloatStateOf(0f) }
    var progressPct by remember { mutableIntStateOf(0) }
    var isFading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val startNs = System.nanoTime()
        val durationNs = durationMs.coerceAtLeast(1000L) * 1_000_000f
        while (true) {
            var done = false
            withFrameNanos { frameNs ->
                val raw = ((frameNs - startNs) / durationNs).coerceIn(0f, 1f)
                // Smooth ease-in-out progress curve
                val eased = raw * raw * (3f - 2f * raw)
                normProgress = raw
                progressPct = (eased * 100f).toInt().coerceIn(0, 100)
                if (raw >= 1f) {
                    done = true
                }
            }
            if (done) break
        }
        progressPct = 100
        delay(280)
        isFading = true
        delay(320)
        onComplete()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "splash_anim")
    val sunburstPulse by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sunburst"
    )
    val floatBobY by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_bob"
    )
    val sparkleSpin by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkle_spin"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .alpha(if (isFading) 0f else 1f)
            .background(Color(0xFF0077E6))
            .testTag("splash_screen")
    ) {
        val screenW = maxWidth
        val screenH = maxHeight

        // 1. Whimsical Candy Cloud Arena Background
        if (bgBitmap != null) {
            Image(
                bitmap = bgBitmap.asImageBitmap(),
                contentDescription = "Floofies Arena Background",
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("splash_background"),
                contentScale = ContentScale.Crop
            )
        }

        // 2. Animated Sunburst & Twinkling Magic Sparkles in the Sky
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width * 0.5f, size.height * 0.56f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFF8B8).copy(alpha = sunburstPulse * 0.55f),
                        Color(0xFF6EE7FF).copy(alpha = sunburstPulse * 0.22f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.width * 0.65f
                ),
                center = center,
                radius = size.width * 0.65f
            )

            // Extra twinkling sparkles around centerpiece
            val sparkleCoords = listOf(
                Offset(0.20f, 0.28f),
                Offset(0.82f, 0.25f),
                Offset(0.14f, 0.44f),
                Offset(0.86f, 0.42f),
                Offset(0.32f, 0.20f),
                Offset(0.70f, 0.19f)
            )
            sparkleCoords.forEachIndexed { idx, pt ->
                val phase = ((sparkleSpin + idx * 0.17f) % 1f) * 2f * PI.toFloat()
                val glow = (0.35f + 0.65f * ((sin(phase) + 1f) * 0.5f)).coerceIn(0f, 1f)
                val cx = size.width * pt.x
                val cy = size.height * pt.y
                val r = (5.dp.toPx()) * (0.6f + 0.6f * glow)
                drawCircle(
                    color = Color.White.copy(alpha = glow * 0.85f),
                    radius = r,
                    center = Offset(cx, cy)
                )
            }
        }

        // 3. Dashing Fluffies Streaking Across Into the Arena (progress 0.00 -> 0.51)
        if (dashBitmaps.isNotEmpty()) {
            DASHING_FLUFFIES.forEach { spec ->
                val bmp = dashBitmaps.getOrNull(spec.assetIndex)
                val localT = ((normProgress - spec.startDelayFrac) / spec.durationFrac).coerceIn(0f, 1f)
                if (bmp != null && normProgress >= spec.startDelayFrac && localT < 1f) {
                    val ease = 1f - (1f - localT) * (1f - localT)
                    val startX = if (spec.fromLeft) -screenW * 0.58f else screenW * 0.58f
                    val endX = 0.dp
                    val curX = startX + (endX - startX) * ease
                    val arcLift = -spec.arcDp * sin(localT * PI.toFloat())
                    val curAlpha = when {
                        localT < 0.15f -> localT / 0.15f
                        localT > 0.78f -> (1f - localT) / 0.22f
                        else -> 1f
                    }.coerceIn(0f, 1f)
                    val scale = 1.05f - 0.35f * ease

                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(
                                x = curX,
                                y = screenH * spec.yFracFromTop + arcLift.dp
                            )
                            .size(width = 88.dp, height = 50.dp)
                            .alpha(curAlpha)
                            .graphicsLayer {
                                scaleX = if (spec.fromLeft) scale else -scale
                                scaleY = scale
                            },
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        // 4. Centerpiece Hero Lockup (Pop-in Fluffies Title -> Full Grand Fluffies Group Celebration)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-24).dp)
                .fillMaxWidth(0.88f)
                .widthIn(max = 360.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                normProgress < 0.14f && bannerIntroBitmap != null -> {
                    val p = (normProgress / 0.14f).coerceIn(0f, 1f)
                    val popScale = 0.45f + 0.62f * sin(p * PI.toFloat() * 0.65f)
                    Image(
                        bitmap = bannerIntroBitmap.asImageBitmap(),
                        contentDescription = "Floofies Splash Banner",
                        modifier = Modifier
                            .fillMaxWidth(0.86f)
                            .aspectRatio(260f / 142f)
                            .graphicsLayer {
                                scaleX = popScale
                                scaleY = popScale
                                translationY = floatBobY
                            },
                        contentScale = ContentScale.Fit
                    )
                }

                normProgress < 0.36f && titleFrames.isNotEmpty() -> {
                    val subP = ((normProgress - 0.14f) / 0.22f).coerceIn(0f, 0.999f)
                    val frameIdx = ((subP * 2f * titleFrames.size).toInt() % titleFrames.size).coerceIn(0, titleFrames.lastIndex)
                    val popBoost = 1f + 0.06f * sin(subP * PI.toFloat())
                    Image(
                        bitmap = titleFrames[frameIdx].asImageBitmap(),
                        contentDescription = "Floofies Group Appearing",
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .aspectRatio(260f / 142f)
                            .graphicsLayer {
                                scaleX = popBoost
                                scaleY = popBoost
                                translationY = floatBobY
                            },
                        contentScale = ContentScale.Fit
                    )
                }

                lockupFrames.isNotEmpty() -> {
                    val subP = ((normProgress - 0.36f) / 0.64f).coerceIn(0f, 0.999f)
                    // Cycle smoothly through celebration expressions 0..3 across remaining ~4.1s
                    val frameIdx = when {
                        normProgress >= 0.90f -> 3
                        else -> (subP * 10f).toInt() % lockupFrames.size
                    }.coerceIn(0, lockupFrames.lastIndex)
                    val introScale = if (normProgress < 0.44f) {
                        val s = (normProgress - 0.36f) / 0.08f
                        0.88f + 0.12f * s
                    } else 1f

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            bitmap = lockupFrames[frameIdx].asImageBitmap(),
                            contentDescription = "Floofies Marble Sort",
                            modifier = Modifier
                                .fillMaxWidth(0.98f)
                                .aspectRatio(256f / 210f)
                                .graphicsLayer {
                                    scaleX = introScale
                                    scaleY = introScale
                                    translationY = floatBobY
                                },
                            contentScale = ContentScale.Fit
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF0040B3), Color(0xFF0080FF), Color(0xFF0040B3))
                                    ),
                                    RoundedCornerShape(16.dp)
                                )
                                .border(2.dp, Color(0xFFFFE382), RoundedCornerShape(16.dp))
                                .padding(horizontal = 18.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "MARBLE SORT",
                                fontFamily = FredokaFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = Color(0xFFFFE600),
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }
            }
        }

        // 5. Animated Yellow Floofie Running Loading Bar above Bottom Podium
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (barFrames.isNotEmpty()) {
                val barFrameIdx = if (progressPct >= 97) {
                    barFrames.lastIndex
                } else {
                    ((progressPct / 97f) * (barFrames.size - 1)).toInt().coerceIn(0, barFrames.lastIndex - 1)
                }

                Image(
                    bitmap = barFrames[barFrameIdx].asImageBitmap(),
                    contentDescription = "Loading Progress $progressPct%",
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .widthIn(max = 310.dp)
                        .aspectRatio(260f / 116f)
                        .testTag("splash_loading_bar"),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .background(Color(0xB3002159), RoundedCornerShape(14.dp))
                    .border(1.5.dp, Color(0xFF7CE7FF), RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 5.dp),
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
