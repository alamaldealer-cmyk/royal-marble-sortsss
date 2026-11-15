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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
import com.example.game.sound.SoundManager
import com.example.ui.theme.FredokaFontFamily
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun LevelIntroScreen(
    levelNumber: Int,
    onComplete: () -> Unit,
    durationMs: Long = 4500L
) {
    val context = LocalContext.current

    val bgBitmap = remember { AssetLoader.loadBitmap(context, "splash/splash_bg.png") }
    val storyCaptureBitmap = remember { AssetLoader.loadBitmap(context, "cutscene/story_capture.png") }
    val ufoBitmap = remember { AssetLoader.loadBitmap(context, "cutscene/ufo.png") }
    val villainBitmap = remember { AssetLoader.loadBitmap(context, "cutscene/villain.png") }
    val bubbleCageBitmap = remember { AssetLoader.loadBitmap(context, "cutscene/bubble_cage.png") }

    var animProgress by remember { mutableFloatStateOf(0f) }
    var isSkipping by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val startNs = System.nanoTime()
        val durationNs = durationMs.toFloat() * 1_000_000f
        while (true) {
            var finished = false
            withFrameNanos { frameNs ->
                val raw = ((frameNs - startNs) / durationNs).coerceIn(0f, 1f)
                animProgress = raw
                if (raw >= 1f) {
                    finished = true
                }
            }
            if (finished) break
        }
        delay(150)
        onComplete()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "cutscene_loop")
    val ufoHoverY by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ufoHover"
    )
    val beamPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beamPulse"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0055B8))
            .testTag("level_intro_screen")
    ) {
        val screenW = maxWidth
        val screenH = maxHeight

        // 1. Background
        if (bgBitmap != null) {
            Image(
                bitmap = bgBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // 2. Cinematic Story Panel / Action Illustration
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Level Title Pill
            Box(
                modifier = Modifier
                    .background(Color(0xE60B5FA7), RoundedCornerShape(20.dp))
                    .border(2.5.dp, Color(0xFFFFD700), RoundedCornerShape(20.dp))
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "LEVEL $levelNumber - DANGER!",
                    fontFamily = FredokaFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = Color(0xFFFFE600),
                    letterSpacing = 1.5.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Action Illustration Box (UFO descending & snatching Floofies)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .aspectRatio(475f / 290f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0x99001D4A))
                    .border(3.dp, Color(0xFF00C4FF), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (storyCaptureBitmap != null) {
                    Image(
                        bitmap = storyCaptureBitmap.asImageBitmap(),
                        contentDescription = "Villain Capturing Floofies",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Animated UFO descending from top during phase 1 -> 2
                val ufoAnimY = when {
                    animProgress < 0.25f -> -120f + (animProgress / 0.25f) * 120f
                    else -> 0f + ufoHoverY
                }

                if (ufoBitmap != null) {
                    Image(
                        bitmap = ufoBitmap.asImageBitmap(),
                        contentDescription = "Lord Shadow Puff UFO",
                        modifier = Modifier
                            .size(170.dp, 100.dp)
                            .align(Alignment.TopCenter)
                            .offset(y = ufoAnimY.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                // Tractor beam appearing in phase 2
                if (animProgress in 0.25f..0.90f) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(beamPulse)
                    ) {
                        val startX = size.width * 0.5f
                        val startY = size.height * 0.35f
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(startX - 20f, startY)
                            lineTo(startX + 20f, startY)
                            lineTo(startX + 80f, size.height * 0.85f)
                            lineTo(startX - 80f, size.height * 0.85f)
                            close()
                        }
                        drawPath(
                            path = path,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFE93DFF).copy(alpha = 0.65f),
                                    Color(0xFF9B35FF).copy(alpha = 0.35f),
                                    Color.Transparent
                                )
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Story Text Description
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(Color(0xD90B3866), RoundedCornerShape(16.dp))
                    .border(2.dp, Color(0xFF7CE7FF), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val storyText = when {
                    animProgress < 0.3f -> "Oh no! Lord Shadow Puff's UFO has appeared above the peaceful island!"
                    animProgress < 0.7f -> "The evil tractor beam is capturing our cute Floofies into bubbles!"
                    else -> "Hurry! Solve the marble sort puzzle and rescue your friends!"
                }
                Text(
                    text = storyText,
                    fontFamily = FredokaFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        // 3. Skip Button at Top-Right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xCC000000))
                .border(2.dp, Color(0xFFFFD700), RoundedCornerShape(16.dp))
                .clickable {
                    if (!isSkipping) {
                        isSkipping = true
                        SoundManager.playTap()
                        onComplete()
                    }
                }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "SKIP >>",
                fontFamily = FredokaFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                color = Color(0xFFFFE600)
            )
        }
    }
}
