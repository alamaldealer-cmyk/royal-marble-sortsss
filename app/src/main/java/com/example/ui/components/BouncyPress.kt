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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.assets.AssetLoader
import com.example.ui.theme.FredokaFontFamily

@Stable
class BouncyPressState internal constructor(
    val interactionSource: MutableInteractionSource,
    private val scaleAnim: Animatable<Float, *>,
    private val onTriggerPulse: () -> Unit
) {
    val scale: Float get() = scaleAnim.value

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
            if (scaleAnim.value > pressedScale + 0.03f) {
                scaleAnim.animateTo(
                    targetValue = pressedScale,
                    animationSpec = tween(durationMillis = 55, easing = FastOutSlowInEasing)
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

    return remember(interactionSource, scaleAnim) {
        BouncyPressState(
            interactionSource = interactionSource,
            scaleAnim = scaleAnim,
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val btnPlayGreenPill = remember {
        AssetLoader.loadBitmap(context, "profile/btn_play_green_pill.png")
    }
    val press = rememberBouncyPressState(pressedScale = 0.88f)
    val labelSize = if (text.length > 9) 28.sp else 32.sp

    Box(
        modifier = modifier
            .fillMaxWidth(0.64f)
            .widthIn(max = 250.dp)
            .aspectRatio(640f / 200f)
            .bouncyScale(press)
            .testTag(testTag)
            .bouncyClickTarget(press, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (btnPlayGreenPill != null) {
            Image(
                bitmap = btnPlayGreenPill.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
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
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                maxLines = 1,
                style = TextStyle(
                    fontFamily = FredokaFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = labelSize,
                    color = Color(0xFF034400),
                    letterSpacing = 1.8.sp,
                    drawStyle = Stroke(
                        width = 7f,
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
                    letterSpacing = 1.8.sp
                )
            )
        }
    }
}
