package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.assets.AssetLoader
import com.example.ui.theme.FredokaFontFamily

@Stable
class BouncyPressState internal constructor(
    val interactionSource: MutableInteractionSource,
    private val scaleAnim: Animatable<Float, *>,
    private val pressedScale: Float,
    private val onTriggerPulse: () -> Unit
) {
    val scale: Float get() = scaleAnim.value
    val pressProgress: Float
        get() {
            val denom = (1f - pressedScale).coerceAtLeast(0.01f)
            return ((1f - scaleAnim.value) / denom).coerceIn(-0.25f, 1.35f)
        }

    fun triggerPulse() {
        onTriggerPulse()
    }
}

@Composable
fun rememberBouncyPressState(
    pressedScale: Float = 0.88f
): BouncyPressState {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scaleAnim = remember { Animatable(1f) }
    var pulseNonce by remember { mutableIntStateOf(0) }
    var lastHandledPulse by remember { mutableIntStateOf(0) }

    LaunchedEffect(isPressed, pulseNonce) {
        if (pulseNonce != lastHandledPulse) {
            lastHandledPulse = pulseNonce
            if (scaleAnim.value > pressedScale + 0.02f) {
                scaleAnim.animateTo(
                    targetValue = pressedScale,
                    animationSpec = tween(durationMillis = 65, easing = FastOutSlowInEasing)
                )
            }
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        } else if (isPressed) {
            scaleAnim.animateTo(
                targetValue = pressedScale,
                animationSpec = tween(durationMillis = 65, easing = FastOutSlowInEasing)
            )
        } else {
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    return remember(interactionSource, scaleAnim, pressedScale) {
        BouncyPressState(
            interactionSource = interactionSource,
            scaleAnim = scaleAnim,
            pressedScale = pressedScale,
            onTriggerPulse = { pulseNonce++ }
        )
    }
}

fun Modifier.bouncyScale(state: BouncyPressState): Modifier =
    this.graphicsLayer {
        val s = state.scale
        scaleX = s
        scaleY = s
    }

fun Modifier.bouncyClickTarget(
    state: BouncyPressState,
    enabled: Boolean = true,
    role: Role? = null,
    onClick: () -> Unit
): Modifier =
    this.clickable(
        interactionSource = state.interactionSource,
        indication = null,
        enabled = enabled,
        role = role
    ) {
        state.triggerPulse()
        onClick()
    }

@Composable
fun GreenPlayPillButton(
    text: String,
    contentDescription: String,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit? = null,
    widthFraction: Float = 0.64f,
    letterSpacing: TextUnit = 1.8.sp,
    strokeWidth: Float = 7f
) {
    val labelSize = fontSize ?: when {
        text.contains(":") -> 25.sp
        text.length > 9 -> 28.sp
        else -> 32.sp
    }

    GreenRingPillButtonContent(
        text = text,
        contentDescription = contentDescription,
        testTag = testTag,
        labelSize = labelSize,
        letterSpacing = letterSpacing,
        strokeWidth = strokeWidth,
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .widthIn(max = 250.dp)
            .aspectRatio(778f / 232f)
    )
}

@Composable
fun GreenRingPillButtonContent(
    text: String,
    contentDescription: String,
    testTag: String,
    labelSize: TextUnit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    letterSpacing: TextUnit = 1.8.sp,
    strokeWidth: Float = 7f
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val greenInnerBmp = remember {
        AssetLoader.loadBitmap(context, "profile/btn_green_pill_inner.png")
            ?: AssetLoader.loadBitmap(context, "profile/btn_play_green_pill.png")
    }
    val goldRingBmp = remember {
        AssetLoader.loadBitmap(context, "profile/btn_green_pill_ring.png")
    }
    val press = rememberBouncyPressState(pressedScale = 0.89f)
    val pressProgress = press.pressProgress
    val wholeSinkPx = with(density) { 2.5.dp.toPx() }
    val innerSinkPx = with(density) { 1.4.dp.toPx() }

    Box(
        modifier = modifier
            .testTag(testTag)
            .semantics { this.contentDescription = contentDescription }
            .bouncyClickTarget(press, role = Role.Button, onClick = onClick)
            .graphicsLayer {
                val outerScale = 1f - 0.075f * pressProgress
                scaleX = outerScale
                scaleY = outerScale
                translationY = wholeSinkPx * pressProgress
            },
        contentAlignment = Alignment.Center
    ) {
        // 1. Inner Glossy Green Pill + Label Text tucked snugly behind the golden ring (zero gap)
        val innerModifier = if (goldRingBmp != null) {
            Modifier
                .fillMaxWidth(0.955f)
                .fillMaxHeight(0.845f)
                .clip(RoundedCornerShape(percent = 38))
        } else {
            Modifier.fillMaxSize()
        }

        Box(
            modifier = innerModifier
                .graphicsLayer {
                    scaleY = 1f - 0.045f * pressProgress
                    translationY = innerSinkPx * pressProgress
                },
            contentAlignment = Alignment.Center
        ) {
            if (greenInnerBmp != null) {
                Image(
                    bitmap = greenInnerBmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds,
                    filterQuality = FilterQuality.High
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(40.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF4ADE80), Color(0xFF22C55E), Color(0xFF15803D))
                            )
                        )
                        .border(3.dp, Color(0xFFFFD700), RoundedCornerShape(40.dp))
                )
            }

            // Physical press darkening overlay on the green pill
            val shadeAlpha = (0.18f * pressProgress).coerceIn(0f, 0.24f)
            if (shadeAlpha > 0.005f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = shadeAlpha))
                )
            }

            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = text,
                    modifier = Modifier.clearAndSetSemantics {},
                    maxLines = 1,
                    style = TextStyle(
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = labelSize,
                        color = Color(0xFF034400),
                        letterSpacing = letterSpacing,
                        drawStyle = Stroke(
                            width = strokeWidth,
                            join = StrokeJoin.Round
                        ),
                        shadow = Shadow(
                            color = Color(0xAA012800),
                            offset = Offset(0f, 3f),
                            blurRadius = 4f
                        )
                    )
                )
                Text(
                    text = text,
                    maxLines = 1,
                    style = TextStyle(
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = labelSize,
                        color = Color.White,
                        letterSpacing = letterSpacing
                    )
                )
            }
        }

        // 2. Outer Golden Ring Frame drawn ON TOP so it hugs the green button seamlessly as one piece
        if (goldRingBmp != null) {
            Image(
                bitmap = goldRingBmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
                filterQuality = FilterQuality.High
            )
        }
    }
}
