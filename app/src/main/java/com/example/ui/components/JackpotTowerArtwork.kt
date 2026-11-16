package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.FredokaFontFamily
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private data class TierSpec(
    val amount: Int,
    val yTopFrac: Float,
    val yBotFrac: Float,
    val yCenterFrac: Float,
    val topColors: List<Color>,
    val edgeColor: Color
)

// Ordered bottom-to-top matching claim progression: 50 -> 100 -> 200 -> 400 -> 800
private val TOWER_TIERS = listOf(
    TierSpec(
        amount = 50,
        yTopFrac = 0.802f,
        yBotFrac = 0.916f,
        yCenterFrac = 0.858f,
        topColors = listOf(Color(0xFFFF4EA3), Color(0xFFFF1B79), Color(0xFFD10B5C)),
        edgeColor = Color(0xFF8C043E)
    ),
    TierSpec(
        amount = 100,
        yTopFrac = 0.690f,
        yBotFrac = 0.802f,
        yCenterFrac = 0.745f,
        topColors = listOf(Color(0xFFC35CFF), Color(0xFF9C27F5), Color(0xFF7511C9)),
        edgeColor = Color(0xFF4A0688)
    ),
    TierSpec(
        amount = 200,
        yTopFrac = 0.574f,
        yBotFrac = 0.688f,
        yCenterFrac = 0.630f,
        topColors = listOf(Color(0xFFFFB22E), Color(0xFFFF820E), Color(0xFFD94B00)),
        edgeColor = Color(0xFF8F2E00)
    ),
    TierSpec(
        amount = 400,
        yTopFrac = 0.452f,
        yBotFrac = 0.570f,
        yCenterFrac = 0.510f,
        topColors = listOf(Color(0xFFF45CE0), Color(0xFFD424B8), Color(0xFFA0118C)),
        edgeColor = Color(0xFF6B045E)
    ),
    TierSpec(
        amount = 800,
        yTopFrac = 0.258f,
        yBotFrac = 0.456f,
        yCenterFrac = 0.376f,
        topColors = listOf(Color(0xFFFF418D), Color(0xFFEA166C), Color(0xFFBD064F)),
        edgeColor = Color(0xFF7B0233)
    )
)

private val RAINBOW_LED_COLORS = listOf(
    Color(0xFFFF2F92),
    Color(0xFFFF595E),
    Color(0xFFFF922B),
    Color(0xFFFFD43B),
    Color(0xFF94D82D),
    Color(0xFF20C997),
    Color(0xFF22B8CF),
    Color(0xFF339AF0),
    Color(0xFF5C7CFA),
    Color(0xFF9775FA),
    Color(0xFFD6336C),
    Color(0xFFFF922B),
    Color(0xFFFFE066),
    Color(0xFF51CF66),
    Color(0xFF22D3EE),
    Color(0xFF38BDF8)
)

@Composable
fun JackpotTowerMachine(
    activeTierIndex: Int,
    allClaimed: Boolean,
    isCelebratingBurst: Boolean,
    hudCoinBmp: Bitmap?,
    coinFrames: List<Bitmap>,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val safeTierIdx = activeTierIndex.coerceIn(0, TOWER_TIERS.lastIndex)

    val infiniteTransition = rememberInfiniteTransition(label = "jackpot_machine_lights")
    val lightsPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "lights_phase"
    )
    val marqueeStep by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "marquee_step"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(620, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "active_glow_pulse"
    )
    val arrowInwardNudge by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(480, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrow_inward_nudge"
    )
    val burstProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(680, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "burst_coins"
    )

    val animatedTierCenterY by animateFloatAsState(
        targetValue = TOWER_TIERS[safeTierIdx].yCenterFrac,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tier_center_y"
    )
    val animatedTierTopY by animateFloatAsState(
        targetValue = TOWER_TIERS[safeTierIdx].yTopFrac,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tier_top_y"
    )
    val animatedTierBotY by animateFloatAsState(
        targetValue = TOWER_TIERS[safeTierIdx].yBotFrac,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tier_bot_y"
    )

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(0.67f)
            .testTag("jackpot_tower_sprite")
    ) {
        val w = maxWidth
        val h = maxHeight

        // Rock-steady tower machine base + animated lights/bulbs/sparkles/active-tier highlight
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cw = size.width
            val ch = size.height

            val bodyTopY = ch * 0.235f
            val bodyBotY = ch * 0.955f
            val outerLeftTopX = cw * 0.145f
            val outerRightTopX = cw * 0.855f
            val outerLeftBotX = cw * 0.068f
            val outerRightBotX = cw * 0.932f
            val pillarW = cw * 0.094f

            fun xLeftAt(y: Float): Float {
                val t = ((y - bodyTopY) / (bodyBotY - bodyTopY)).coerceIn(0f, 1f)
                return outerLeftTopX + (outerLeftBotX - outerLeftTopX) * t
            }

            fun xRightAt(y: Float): Float {
                val t = ((y - bodyTopY) / (bodyBotY - bodyTopY)).coerceIn(0f, 1f)
                return outerRightTopX + (outerRightBotX - outerRightTopX) * t
            }

            // 1. Soft floor drop shadow
            drawOval(
                color = Color(0x55000000),
                topLeft = Offset(cw * 0.05f, ch * 0.945f),
                size = Size(cw * 0.90f, ch * 0.048f)
            )

            // 2. Top purple dome + stacked glossy gold coins crown
            drawCoinCrown(
                cw = cw,
                ch = ch,
                hudCoinBmp = hudCoinBmp,
                lightsPhase = lightsPhase,
                glowPulse = glowPulse,
                isCelebratingBurst = isCelebratingBurst,
                burstProgress = burstProgress,
                coinFrames = coinFrames
            )

            // 3. Dark purple inner cabinet trapezoid
            val bodyPath = Path().apply {
                moveTo(outerLeftTopX + pillarW * 0.45f, bodyTopY)
                lineTo(outerRightTopX - pillarW * 0.45f, bodyTopY)
                lineTo(outerRightBotX - pillarW * 0.45f, bodyBotY)
                lineTo(outerLeftBotX + pillarW * 0.45f, bodyBotY)
                close()
            }
            drawPath(
                path = bodyPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF43056E),
                        Color(0xFF2D0252),
                        Color(0xFF22003F)
                    ),
                    startY = bodyTopY,
                    endY = bodyBotY
                )
            )
            // Subtle inner dark vignette top shadow under cornice
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0x66000000), Color.Transparent),
                    startY = bodyTopY,
                    endY = bodyTopY + ch * 0.04f
                ),
                topLeft = Offset(outerLeftTopX + pillarW * 0.4f, bodyTopY),
                size = Size((outerRightTopX - outerLeftTopX) - pillarW * 0.8f, ch * 0.04f)
            )

            // 4. Five Tier Banners (50, 100, 200, 400, plus 800 top marquee chevron)
            for (idx in 0..3) {
                val tier = TOWER_TIERS[idx]
                val tierPath = buildCurvedTierPath(
                    cw = cw,
                    ch = ch,
                    yTop = ch * tier.yTopFrac,
                    yBot = ch * tier.yBotFrac,
                    xLeftAt = ::xLeftAt,
                    xRightAt = ::xRightAt,
                    pillarW = pillarW
                )
                drawTierBanner(
                    path = tierPath,
                    colors = tier.topColors,
                    edgeColor = tier.edgeColor,
                    yTop = ch * tier.yTopFrac,
                    yBot = ch * tier.yBotFrac
                )
            }

            // 5. Top 800 Blue-Bulb Marquee Triangle/Chevron
            drawTop800Chevron(
                cw = cw,
                ch = ch,
                marqueeStep = marqueeStep,
                glowPulse = glowPulse,
                isActive = !allClaimed && safeTierIdx == 4
            )

            // 6. Active Tier Glowing Gold Selection Highlight (moves cleanly from 50 -> 800)
            if (!allClaimed) {
                if (safeTierIdx < 4) {
                    val hlPath = buildCurvedTierPath(
                        cw = cw,
                        ch = ch,
                        yTop = ch * animatedTierTopY,
                        yBot = ch * animatedTierBotY,
                        xLeftAt = ::xLeftAt,
                        xRightAt = ::xRightAt,
                        pillarW = pillarW,
                        extraExpandX = cw * 0.012f,
                        extraExpandY = ch * 0.004f
                    )
                    drawPath(
                        path = hlPath,
                        color = Color(0xFFFFEE55).copy(alpha = 0.42f * glowPulse),
                        style = Stroke(width = cw * 0.036f, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = hlPath,
                        color = Color(0xFFFFD400),
                        style = Stroke(width = cw * 0.019f, join = StrokeJoin.Round)
                    )
                }
            }

            // 7. Left & Right Gold-Framed Rainbow Light Pillars (lights animate, pillars stay fixed!)
            drawRainbowPillar(
                isLeft = true,
                yTop = bodyTopY,
                yBot = bodyBotY,
                pillarW = pillarW,
                xOuterTop = outerLeftTopX,
                xOuterBot = outerLeftBotX,
                lightsPhase = lightsPhase
            )
            drawRainbowPillar(
                isLeft = false,
                yTop = bodyTopY,
                yBot = bodyBotY,
                pillarW = pillarW,
                xOuterTop = outerRightTopX,
                xOuterBot = outerRightBotX,
                lightsPhase = lightsPhase
            )

            // 8. Top Purple Cornice Ledge & Bottom Gold Pedestal Base Bar + Corner Feet
            drawCorniceAndBase(
                cw = cw,
                ch = ch,
                bodyTopY = bodyTopY,
                bodyBotY = bodyBotY,
                outerLeftTopX = outerLeftTopX,
                outerRightTopX = outerRightTopX,
                outerLeftBotX = outerLeftBotX,
                outerRightBotX = outerRightBotX,
                pillarW = pillarW
            )
        }

        // 9. Crisp Fredoka Tier Numbers (800, 400, 200, 100, 50)
        for ((idx, tier) in TOWER_TIERS.withIndex()) {
            val isClaimed = idx < safeTierIdx || allClaimed
            val isActive = !allClaimed && idx == safeTierIdx
            val fontSizeSp = with(density) {
                (w * if (tier.amount == 800) 0.105f else 0.095f).toSp()
            }
            val yCenterDp = h * tier.yCenterFrac
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = yCenterDp - (h * 0.038f))
                    .alpha(if (isClaimed) 0.72f else 1f),
                contentAlignment = Alignment.Center
            ) {
                val strokeW = with(density) { 3.2.dp.toPx() }
                val numberColor = if (isActive) Color(0xFFFFF7B2) else Color(0xFFFFE57F)
                Text(
                    text = tier.amount.toString(),
                    fontFamily = FredokaFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = fontSizeSp,
                    color = Color(0xFF4A032D),
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        drawStyle = Stroke(width = strokeW, join = StrokeJoin.Round)
                    )
                )
                Text(
                    text = tier.amount.toString(),
                    fontFamily = FredokaFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = fontSizeSp,
                    color = numberColor,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color(0x9935001E),
                            offset = Offset(0f, 2f),
                            blurRadius = 3f
                        )
                    )
                )
            }
        }

        // 10. Animated Green Arrows INSIDE left & right edges of active tier pointing inward
        if (!allClaimed) {
            val arrowSize = (w * 0.125f).coerceIn(28.dp, 38.dp)
            val tierYTopFrac = animatedTierTopY
            val tierYBotFrac = animatedTierBotY
            val tierCenterFrac = animatedTierCenterY

            // Horizontal position hugs inside edge of active tier trapezoid
            val bodySlopeT = ((tierCenterFrac - 0.235f) / (0.955f - 0.235f)).coerceIn(0f, 1f)
            val edgeInsetFrac = if (safeTierIdx == 4) {
                0.235f
            } else {
                val outerLeft = 0.145f + (0.068f - 0.145f) * bodySlopeT
                outerLeft + 0.098f
            }

            val inwardOffsetDp = (arrowInwardNudge * 4.5f).dp
            val arrowYDp = h * ((tierYTopFrac + tierYBotFrac) * 0.5f + if (safeTierIdx == 4) 0.018f else -0.008f) - arrowSize * 0.5f

            // Left Green Arrow pointing RIGHT (▶) inside left edge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(
                        x = w * edgeInsetFrac + inwardOffsetDp,
                        y = arrowYDp
                    )
                    .size(arrowSize)
                    .testTag("jackpot_arrow_left")
            ) {
                GlossyGreenArrowCanvas(pointsRight = true, modifier = Modifier.fillMaxSize())
            }

            // Right Green Arrow pointing LEFT (◀) inside right edge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(
                        x = -(w * edgeInsetFrac + inwardOffsetDp),
                        y = arrowYDp
                    )
                    .size(arrowSize)
                    .testTag("jackpot_arrow_right")
            ) {
                GlossyGreenArrowCanvas(pointsRight = false, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun GlossyGreenArrowCanvas(
    pointsRight: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cw = size.width
        val ch = size.height
        val path = Path().apply {
            if (pointsRight) {
                moveTo(cw * 0.16f, ch * 0.12f)
                quadraticTo(cw * 0.20f, ch * 0.04f, cw * 0.30f, ch * 0.10f)
                lineTo(cw * 0.88f, ch * 0.43f)
                quadraticTo(cw * 0.96f, ch * 0.50f, cw * 0.88f, ch * 0.57f)
                lineTo(cw * 0.30f, ch * 0.90f)
                quadraticTo(cw * 0.20f, ch * 0.96f, cw * 0.16f, ch * 0.88f)
                quadraticTo(cw * 0.11f, ch * 0.50f, cw * 0.16f, ch * 0.12f)
                close()
            } else {
                moveTo(cw * 0.84f, ch * 0.12f)
                quadraticTo(cw * 0.80f, ch * 0.04f, cw * 0.70f, ch * 0.10f)
                lineTo(cw * 0.12f, ch * 0.43f)
                quadraticTo(cw * 0.04f, ch * 0.50f, cw * 0.12f, ch * 0.57f)
                lineTo(cw * 0.70f, ch * 0.90f)
                quadraticTo(cw * 0.80f, ch * 0.96f, cw * 0.84f, ch * 0.88f)
                quadraticTo(cw * 0.89f, ch * 0.50f, cw * 0.84f, ch * 0.12f)
                close()
            }
        }

        // Soft drop shadow
        drawPath(
            path = path,
            color = Color(0x77000000),
            style = Stroke(width = cw * 0.14f, join = StrokeJoin.Round)
        )
        // Deep green border
        drawPath(
            path = path,
            color = Color(0xFF0A4D00),
            style = Stroke(width = cw * 0.11f, join = StrokeJoin.Round)
        )
        // Bright lime-emerald glossy fill
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFB4FF2A),
                    Color(0xFF56DF00),
                    Color(0xFF199B00)
                )
            )
        )
        // Top highlight glint
        drawCircle(
            color = Color(0x77FFFFFF),
            radius = cw * 0.11f,
            center = Offset(if (pointsRight) cw * 0.34f else cw * 0.66f, ch * 0.28f)
        )
    }
}

private fun DrawScope.drawCoinCrown(
    cw: Float,
    ch: Float,
    hudCoinBmp: Bitmap?,
    lightsPhase: Float,
    glowPulse: Float,
    isCelebratingBurst: Boolean,
    burstProgress: Float,
    coinFrames: List<Bitmap>
) {
    // Purple arched dome behind coin pile
    val domePath = Path().apply {
        moveTo(cw * 0.12f, ch * 0.225f)
        cubicTo(
            cw * 0.20f, ch * 0.055f,
            cw * 0.80f, ch * 0.055f,
            cw * 0.88f, ch * 0.225f
        )
        close()
    }
    drawPath(
        path = domePath,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFD946EF), Color(0xFF9333EA), Color(0xFF581C87)),
            startY = ch * 0.06f,
            endY = ch * 0.23f
        )
    )
    drawPath(
        path = domePath,
        color = Color(0xFFF0ABFC).copy(alpha = 0.45f),
        style = Stroke(width = cw * 0.012f)
    )

    // Warm radiant glow behind hero coin
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xBBFFE680).copy(alpha = 0.65f * glowPulse),
                Color(0x55FF66FF),
                Color.Transparent
            ),
            center = Offset(cw * 0.50f, ch * 0.105f),
            radius = cw * 0.28f
        ),
        radius = cw * 0.28f,
        center = Offset(cw * 0.50f, ch * 0.105f)
    )

    // Layered coin pile using high-res hud_coin.png
    val coinPlacements = listOf(
        Triple(0.24f, 0.190f, -22f),
        Triple(0.76f, 0.190f, 22f),
        Triple(0.32f, 0.155f, -15f),
        Triple(0.68f, 0.155f, 15f),
        Triple(0.41f, 0.185f, -8f),
        Triple(0.59f, 0.185f, 8f),
        Triple(0.20f, 0.212f, -28f),
        Triple(0.80f, 0.212f, 28f),
        Triple(0.36f, 0.210f, 6f),
        Triple(0.64f, 0.210f, -6f),
        Triple(0.50f, 0.192f, 0f),
        Triple(0.43f, 0.132f, -12f),
        Triple(0.57f, 0.132f, 12f)
    )
    val smallCoinR = cw * 0.062f
    for ((xFrac, yFrac, tilt) in coinPlacements) {
        val cx = cw * xFrac
        val cy = ch * yFrac
        if (hudCoinBmp != null) {
            rotate(degrees = tilt, pivot = Offset(cx, cy)) {
                drawContext.canvas.nativeCanvas.drawBitmap(
                    hudCoinBmp,
                    null,
                    RectF(cx - smallCoinR, cy - smallCoinR * 0.84f, cx + smallCoinR, cy + smallCoinR * 0.84f),
                    null
                )
            }
        } else {
            drawCircle(Color(0xFFFFCA1A), smallCoinR, Offset(cx, cy))
        }
    }

    // Center hero "$" coin perched at crown peak
    val heroR = cw * 0.102f
    val heroCx = cw * 0.50f
    val heroCy = ch * 0.095f
    if (hudCoinBmp != null) {
        drawContext.canvas.nativeCanvas.drawBitmap(
            hudCoinBmp,
            null,
            RectF(heroCx - heroR, heroCy - heroR, heroCx + heroR, heroCy + heroR),
            null
        )
    } else {
        drawCircle(Color(0xFFFFD700), heroR, Offset(heroCx, heroCy))
    }

    // Animated twinkling star glints across the coin pile (static machine, glowing glints)
    val glints = listOf(
        Offset(cw * 0.34f, ch * 0.12f),
        Offset(cw * 0.65f, ch * 0.115f),
        Offset(cw * 0.50f, ch * 0.045f),
        Offset(cw * 0.24f, ch * 0.175f),
        Offset(cw * 0.76f, ch * 0.175f)
    )
    glints.forEachIndexed { idx, pt ->
        val twinkle = (0.5f + 0.5f * sin((lightsPhase + idx * 0.21f) * 2f * PI.toFloat())).coerceIn(0f, 1f)
        val starR = cw * 0.028f * (0.4f + 0.75f * twinkle)
        val alpha = 0.25f + 0.75f * twinkle
        drawLine(
            Color.White.copy(alpha = alpha),
            Offset(pt.x - starR, pt.y),
            Offset(pt.x + starR, pt.y),
            strokeWidth = cw * 0.008f,
            cap = StrokeCap.Round
        )
        drawLine(
            Color.White.copy(alpha = alpha),
            Offset(pt.x, pt.y - starR),
            Offset(pt.x, pt.y + starR),
            strokeWidth = cw * 0.008f,
            cap = StrokeCap.Round
        )
    }

    // Celebration coin burst around top crown when claiming (without wobbling the machine!)
    if (isCelebratingBurst && coinFrames.isNotEmpty()) {
        val frameIdx = ((burstProgress * 8f).toInt()) % coinFrames.size
        val bmp = coinFrames[frameIdx]
        val coinSize = cw * 0.11f
        val burstAngles = listOf(-145f, -115f, -65f, -35f, -165f, -15f)
        burstAngles.forEachIndexed { idx, deg ->
            val rad = deg * (PI.toFloat() / 180f)
            val dist = cw * (0.14f + burstProgress * (0.26f + (idx % 2) * 0.08f))
            val bx = heroCx + cos(rad) * dist
            val by = heroCy + sin(rad) * dist + burstProgress * burstProgress * ch * 0.07f
            val half = coinSize * 0.5f * (1f - 0.25f * burstProgress)
            drawContext.canvas.nativeCanvas.drawBitmap(
                bmp,
                null,
                RectF(bx - half, by - half, bx + half, by + half),
                null
            )
        }
    }
}

private fun buildCurvedTierPath(
    cw: Float,
    ch: Float,
    yTop: Float,
    yBot: Float,
    xLeftAt: (Float) -> Float,
    xRightAt: (Float) -> Float,
    pillarW: Float,
    extraExpandX: Float = 0f,
    extraExpandY: Float = 0f
): Path {
    val margin = pillarW * 0.58f + cw * 0.028f - extraExpandX
    val lTop = xLeftAt(yTop) + margin
    val rTop = xRightAt(yTop) - margin
    val lBot = xLeftAt(yBot) + margin
    val rBot = xRightAt(yBot) - margin
    val archLift = ch * 0.022f
    val yt = yTop - extraExpandY
    val yb = yBot + extraExpandY
    val rCorner = cw * 0.026f

    return Path().apply {
        moveTo(lTop + rCorner, yt + archLift * 0.45f)
        quadraticTo(cw * 0.5f, yt - archLift, rTop - rCorner, yt + archLift * 0.45f)
        quadraticTo(rTop, yt + archLift * 0.55f, rBot, yb - rCorner * 0.4f)
        quadraticTo(rBot - rCorner * 0.3f, yb + archLift * 0.2f, rBot - rCorner, yb + archLift * 0.2f)
        quadraticTo(cw * 0.5f, yb - archLift * 0.85f, lBot + rCorner, yb + archLift * 0.2f)
        quadraticTo(lBot + rCorner * 0.3f, yb + archLift * 0.2f, lBot, yb - rCorner * 0.4f)
        quadraticTo(lTop, yt + archLift * 0.55f, lTop + rCorner, yt + archLift * 0.45f)
        close()
    }
}

private fun DrawScope.drawTierBanner(
    path: Path,
    colors: List<Color>,
    edgeColor: Color,
    yTop: Float,
    yBot: Float
) {
    drawPath(
        path = path,
        color = Color(0x66000000),
        style = Stroke(width = 8f, join = StrokeJoin.Round)
    )
    drawPath(
        path = path,
        brush = Brush.verticalGradient(colors = colors, startY = yTop, endY = yBot)
    )
    drawPath(
        path = path,
        color = Color.White.copy(alpha = 0.28f),
        style = Stroke(width = 2.4f, join = StrokeJoin.Round)
    )
    drawPath(
        path = path,
        color = edgeColor,
        style = Stroke(width = 3.2f, join = StrokeJoin.Round)
    )
}

private fun DrawScope.drawTop800Chevron(
    cw: Float,
    ch: Float,
    marqueeStep: Float,
    glowPulse: Float,
    isActive: Boolean
) {
    val apexY = ch * 0.258f
    val sideY = ch * 0.440f
    val botMidY = ch * 0.415f
    val leftX = cw * 0.205f
    val rightX = cw * 0.795f

    val outerChevron = Path().apply {
        moveTo(cw * 0.50f, apexY)
        lineTo(rightX, sideY)
        quadraticTo(cw * 0.818f, ch * 0.468f, cw * 0.760f, ch * 0.464f)
        quadraticTo(cw * 0.50f, botMidY, cw * 0.240f, ch * 0.464f)
        quadraticTo(cw * 0.182f, ch * 0.468f, leftX, sideY)
        close()
    }

    // Active gold halo if 800 is selected
    if (isActive) {
        drawPath(
            path = outerChevron,
            color = Color(0xFFFFEA00).copy(alpha = 0.55f * glowPulse),
            style = Stroke(width = cw * 0.045f, join = StrokeJoin.Round)
        )
    }

    // Blue marquee border frame
    drawPath(
        path = outerChevron,
        color = Color(0xFF0A2463),
        style = Stroke(width = cw * 0.056f, join = StrokeJoin.Round)
    )
    drawPath(
        path = outerChevron,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF60A5FA), Color(0xFF2563EB), Color(0xFF1E40AF)),
            startY = apexY,
            endY = ch * 0.47f
        ),
        style = Stroke(width = cw * 0.040f, join = StrokeJoin.Round)
    )

    // Inner pink glossy triangle fill
    val innerChevron = Path().apply {
        moveTo(cw * 0.50f, apexY + ch * 0.025f)
        lineTo(rightX - cw * 0.036f, sideY - ch * 0.004f)
        quadraticTo(cw * 0.50f, botMidY - ch * 0.015f, leftX + cw * 0.036f, sideY - ch * 0.004f)
        close()
    }
    drawPath(
        path = innerChevron,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFFF4093), Color(0xFFEA126B), Color(0xFFC10751)),
            startY = apexY,
            endY = sideY
        )
    )

    // Animated golden marquee bulbs around blue triangle frame
    val bulbs = buildList {
        add(Offset(cw * 0.50f, apexY))
        for (i in 1..4) {
            val t = i / 5f
            add(Offset(cw * 0.50f + (rightX - cw * 0.50f) * t, apexY + (sideY - apexY) * t))
            add(Offset(cw * 0.50f - (cw * 0.50f - leftX) * t, apexY + (sideY - apexY) * t))
        }
        add(Offset(rightX - cw * 0.008f, sideY + ch * 0.008f))
        add(Offset(leftX + cw * 0.008f, sideY + ch * 0.008f))
        for (i in 1..3) {
            val t = i / 4f
            val bx = cw * 0.25f + cw * 0.50f * t
            val by = ch * 0.456f - ch * 0.026f * sin(t * PI.toFloat())
            add(Offset(bx, by))
        }
    }

    bulbs.forEachIndexed { idx, pt ->
        val wave = 0.5f + 0.5f * sin((marqueeStep + idx * 0.65f) * PI.toFloat())
        val isHot = wave > 0.55f
        if (isHot) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xCCFFF8B0), Color(0x44FFD400), Color.Transparent),
                    center = pt,
                    radius = cw * 0.028f
                ),
                radius = cw * 0.028f,
                center = pt
            )
        }
        drawCircle(
            color = if (isHot) Color(0xFFFFF59D) else Color(0xFFD97706),
            radius = cw * 0.010f,
            center = pt
        )
        drawCircle(
            color = if (isHot) Color.White else Color(0xFFFBBF24),
            radius = cw * 0.005f,
            center = Offset(pt.x - cw * 0.002f, pt.y - cw * 0.002f)
        )
    }
}

private fun DrawScope.drawRainbowPillar(
    isLeft: Boolean,
    yTop: Float,
    yBot: Float,
    pillarW: Float,
    xOuterTop: Float,
    xOuterBot: Float,
    lightsPhase: Float
) {
    val dir = if (isLeft) 1f else -1f
    val xInnerTop = xOuterTop + dir * pillarW
    val xInnerBot = xOuterBot + dir * pillarW

    val pillarPath = Path().apply {
        moveTo(minOf(xOuterTop, xInnerTop), yTop)
        lineTo(maxOf(xOuterTop, xInnerTop), yTop)
        lineTo(maxOf(xOuterBot, xInnerBot), yBot)
        lineTo(minOf(xOuterBot, xInnerBot), yBot)
        close()
    }

    // Gold 3D cylindrical casing
    drawPath(
        path = pillarPath,
        brush = Brush.horizontalGradient(
            colors = listOf(Color(0xFFB45309), Color(0xFFFFD84D), Color(0xFFF59E0B), Color(0xFFB45309)),
            startX = minOf(xOuterBot, xInnerBot),
            endX = maxOf(xOuterBot, xInnerBot)
        )
    )

    // Animated Rainbow LED Bar Segments inside the pillar
    val segCount = 16
    val inset = pillarW * 0.24f
    val segTotalH = (yBot - yTop) * 0.90f
    val segStartY = yTop + (yBot - yTop) * 0.05f
    val segH = segTotalH / segCount

    for (i in 0 until segCount) {
        val t0 = i.toFloat() / segCount
        val t1 = (i + 0.86f) / segCount
        val sy0 = segStartY + t0 * segTotalH
        val sy1 = segStartY + t1 * segTotalH

        val bodyT0 = ((sy0 - yTop) / (yBot - yTop)).coerceIn(0f, 1f)
        val bodyT1 = ((sy1 - yTop) / (yBot - yTop)).coerceIn(0f, 1f)

        val o0 = xOuterTop + (xOuterBot - xOuterTop) * bodyT0 + dir * inset
        val i0 = o0 + dir * (pillarW - inset * 2f)
        val o1 = xOuterTop + (xOuterBot - xOuterTop) * bodyT1 + dir * inset
        val i1 = o1 + dir * (pillarW - inset * 2f)

        val segPath = Path().apply {
            moveTo(o0, sy0)
            lineTo(i0, sy0)
            lineTo(i1, sy1)
            lineTo(o1, sy1)
            close()
        }

        val baseColor = RAINBOW_LED_COLORS[i % RAINBOW_LED_COLORS.size]
        // Chase wave flowing up from bottom to top
        val posNorm = 1f - (i.toFloat() / segCount)
        val dist = abs(((posNorm - lightsPhase + 1.5f) % 1f) - 0.5f)
        val chaseBoost = ((0.22f - dist) / 0.22f).coerceIn(0f, 1f)
        val litColor = Color(
            red = (baseColor.red + (1f - baseColor.red) * chaseBoost * 0.72f).coerceIn(0f, 1f),
            green = (baseColor.green + (1f - baseColor.green) * chaseBoost * 0.72f).coerceIn(0f, 1f),
            blue = (baseColor.blue + (1f - baseColor.blue) * chaseBoost * 0.72f).coerceIn(0f, 1f),
            alpha = 1f
        )

        drawPath(path = segPath, color = litColor)
        if (chaseBoost > 0.15f) {
            val centerPt = Offset((o0 + i0) * 0.5f, (sy0 + sy1) * 0.5f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.65f * chaseBoost), Color.Transparent),
                    center = centerPt,
                    radius = pillarW * 0.65f
                ),
                radius = pillarW * 0.65f,
                center = centerPt
            )
        }
    }
}

private fun DrawScope.drawCorniceAndBase(
    cw: Float,
    ch: Float,
    bodyTopY: Float,
    bodyBotY: Float,
    outerLeftTopX: Float,
    outerRightTopX: Float,
    outerLeftBotX: Float,
    outerRightBotX: Float,
    pillarW: Float
) {
    // Top purple glossy cornice ledge above pillars
    val ledgeLeft = outerLeftTopX - cw * 0.04f
    val ledgeRight = outerRightTopX + cw * 0.04f
    val ledgeTop = bodyTopY - ch * 0.025f
    val ledgeH = ch * 0.042f
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFE879F9), Color(0xFF9333EA), Color(0xFF581C87)),
            startY = ledgeTop,
            endY = ledgeTop + ledgeH
        ),
        topLeft = Offset(ledgeLeft, ledgeTop),
        size = Size(ledgeRight - ledgeLeft, ledgeH),
        cornerRadius = CornerRadius(ledgeH * 0.5f, ledgeH * 0.5f)
    )
    drawRoundRect(
        color = Color(0xFFF5D0FE).copy(alpha = 0.5f),
        topLeft = Offset(ledgeLeft, ledgeTop),
        size = Size(ledgeRight - ledgeLeft, ledgeH),
        cornerRadius = CornerRadius(ledgeH * 0.5f, ledgeH * 0.5f),
        style = Stroke(width = 2.2f)
    )

    // Bottom horizontal gold base bar
    val baseH = ch * 0.036f
    val baseY = bodyBotY - baseH * 0.55f
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFFFEA70), Color(0xFFF59E0B), Color(0xFFB45309)),
            startY = baseY,
            endY = baseY + baseH
        ),
        topLeft = Offset(outerLeftBotX, baseY),
        size = Size(outerRightBotX - outerLeftBotX, baseH),
        cornerRadius = CornerRadius(baseH * 0.45f, baseH * 0.45f)
    )

    // Left & right cylindrical gold pedestal feet
    val footW = pillarW * 1.24f
    val footH = ch * 0.052f
    val footY = bodyBotY - footH * 0.55f
    for (fx in listOf(outerLeftBotX - pillarW * 0.12f, outerRightBotX - pillarW * 1.12f)) {
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFF08A), Color(0xFFF59E0B), Color(0xFFB45309)),
                startY = footY,
                endY = footY + footH
            ),
            topLeft = Offset(fx, footY),
            size = Size(footW, footH),
            cornerRadius = CornerRadius(footH * 0.45f, footH * 0.45f)
        )
        drawRoundRect(
            color = Color(0xFF92400E),
            topLeft = Offset(fx, footY),
            size = Size(footW, footH),
            cornerRadius = CornerRadius(footH * 0.45f, footH * 0.45f),
            style = Stroke(width = 2.4f)
        )
    }
}
