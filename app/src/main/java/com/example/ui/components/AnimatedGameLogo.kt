package com.example.ui.components

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
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.example.game.assets.AssetLoader
import com.example.game.sound.SoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class FloofiePoseFrame(
    val path: String,
    val durationMs: Long
)

private val FLOOFIE_LOGO_SEQUENCE = listOf(
    FloofiePoseFrame("logo/floofie_happy_open.png", 1700L),
    FloofiePoseFrame("logo/floofie_wink.png", 650L),
    FloofiePoseFrame("logo/floofie_smile.png", 1100L),
    FloofiePoseFrame("logo/floofie_wave.png", 1200L),
    FloofiePoseFrame("logo/floofie_laugh.png", 950L),
    FloofiePoseFrame("logo/floofie_gentle.png", 950L),
    FloofiePoseFrame("logo/floofie_cheer.png", 900L),
    FloofiePoseFrame("logo/floofie_surprised.png", 600L),
    FloofiePoseFrame("logo/floofie_sway.png", 950L)
)

@Composable
fun AnimatedGameLogo(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Load all modular sprites once
    val woodPlaque = remember { AssetLoader.loadBitmap(context, "logo/wood_plaque.png") }
    val wordmarkFloofies = remember { AssetLoader.loadBitmap(context, "logo/wordmark_floofies.png") }
    val bannerMarbleSort = remember { AssetLoader.loadBitmap(context, "logo/banner_marble_sort.png") }

    val leafLeftTop = remember { AssetLoader.loadBitmap(context, "logo/leaf_1.png") }
    val leafLeftBot = remember { AssetLoader.loadBitmap(context, "logo/leaf_6.png") }
    val leafRightTop = remember { AssetLoader.loadBitmap(context, "logo/leaf_3.png") }
    val leafRightMid = remember { AssetLoader.loadBitmap(context, "logo/leaf_4.png") }
    val leafRightBot = remember { AssetLoader.loadBitmap(context, "logo/leaf_5.png") }

    val marblePink = remember { AssetLoader.loadBitmap(context, "logo/marble_pink.png") }
    val marbleYellow = remember { AssetLoader.loadBitmap(context, "logo/marble_yellow.png") }
    val marbleCyan = remember { AssetLoader.loadBitmap(context, "logo/marble_cyan.png") }
    val marblePurple = remember { AssetLoader.loadBitmap(context, "logo/marble_purple.png") }
    val marbleGreen = remember { AssetLoader.loadBitmap(context, "logo/marble_green.png") }
    val marbleOrange = remember { AssetLoader.loadBitmap(context, "logo/marble_orange.png") }

    val star1 = remember { AssetLoader.loadBitmap(context, "logo/star_1.png") }
    val star2 = remember { AssetLoader.loadBitmap(context, "logo/star_2.png") }
    val sparkleBig = remember { AssetLoader.loadBitmap(context, "logo/sparkle_big.png") }
    val sparkleMed = remember { AssetLoader.loadBitmap(context, "logo/sparkle_med.png") }
    val sparkleSmall = remember { AssetLoader.loadBitmap(context, "logo/sparkle_small.png") }

    val floofieFrames = remember {
        FLOOFIE_LOGO_SEQUENCE.map { frame ->
            AssetLoader.loadBitmap(context, frame.path)
        }
    }

    // Current expression frame & playful tap reaction
    var poseIndex by remember { mutableIntStateOf(0) }
    val tapBounce = remember { Animatable(1f) }
    val floofiePop = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            val waitMs = FLOOFIE_LOGO_SEQUENCE[poseIndex].durationMs
            delay(waitMs)
            poseIndex = (poseIndex + 1) % FLOOFIE_LOGO_SEQUENCE.size
            // Subtle rubbery pop whenever expression changes
            floofiePop.snapTo(-4f)
            floofiePop.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "logo_animations")

    // Main title breathing & float
    val titleScale by infiniteTransition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "title_scale"
    )
    val bannerScale by infiniteTransition.animateFloat(
        initialValue = 1.02f,
        targetValue = 0.975f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "banner_scale"
    )
    val floofieBobY by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floofie_bob"
    )
    val floofieTilt by infiniteTransition.animateFloat(
        initialValue = -3.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floofie_tilt"
    )

    // Foliage sway
    val leafSwayLeft by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "leaf_sway_left"
    )
    val leafSwayRight by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "leaf_sway_right"
    )

    // Marble floating offsets
    val marbleBobA by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "marble_bob_a"
    )
    val marbleBobB by infiniteTransition.animateFloat(
        initialValue = 5f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "marble_bob_b"
    )
    val marbleBobC by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "marble_bob_c"
    )

    // Sparkle & Star twinkles
    val twinkleAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "twinkle_1"
    )
    val twinkleAlpha2 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1150, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "twinkle_2"
    )
    val starSpin by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star_spin"
    )

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Aspect ratio of logo assembly is ~1.45 : 1 (W x 0.69*W)
        val canvasWidth: Dp = min(maxWidth, maxHeight * 1.42f)
        val canvasHeight: Dp = canvasWidth * 0.70f

        Box(
            modifier = Modifier
                .size(width = canvasWidth, height = canvasHeight)
                .graphicsLayer {
                    scaleX = tapBounce.value
                    scaleY = tapBounce.value
                }
                .testTag("home_animated_logo")
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    SoundManager.playFluffyCheer()
                    poseIndex = 6 // Jump to excited cheer pose
                    scope.launch {
                        tapBounce.snapTo(0.92f)
                        tapBounce.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // 1. Back swaying tropical leaves (Left side)
            if (leafLeftTop != null) {
                Image(
                    bitmap = leafLeftTop.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(canvasWidth * 0.16f)
                        .offset(x = -canvasWidth * 0.40f, y = -canvasHeight * 0.02f)
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0.9f, 0.7f)
                            rotationZ = leafSwayLeft - 14f
                        },
                    contentScale = ContentScale.Fit
                )
            }
            if (leafLeftBot != null) {
                Image(
                    bitmap = leafLeftBot.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(canvasWidth * 0.18f)
                        .offset(x = -canvasWidth * 0.41f, y = canvasHeight * 0.12f)
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0.9f, 0.5f)
                            rotationZ = -leafSwayLeft - 24f
                        },
                    contentScale = ContentScale.Fit
                )
            }

            // 2. Back swaying tropical leaves (Right side)
            if (leafRightTop != null) {
                Image(
                    bitmap = leafRightTop.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(canvasWidth * 0.19f)
                        .offset(x = canvasWidth * 0.38f, y = -canvasHeight * 0.02f)
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0.1f, 0.7f)
                            rotationZ = leafSwayRight - 10f
                        },
                    contentScale = ContentScale.Fit
                )
            }
            if (leafRightMid != null) {
                Image(
                    bitmap = leafRightMid.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(canvasWidth * 0.17f)
                        .offset(x = canvasWidth * 0.41f, y = canvasHeight * 0.09f)
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0.1f, 0.5f)
                            rotationZ = -leafSwayRight + 6f
                        },
                    contentScale = ContentScale.Fit
                )
            }
            if (leafRightBot != null) {
                Image(
                    bitmap = leafRightBot.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(canvasWidth * 0.15f)
                        .offset(x = canvasWidth * 0.38f, y = canvasHeight * 0.19f)
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0.1f, 0.3f)
                            rotationZ = leafSwayRight + 16f
                        },
                    contentScale = ContentScale.Fit
                )
            }

            // 3. Wooden Plaque Base (behind "Floofies" & lower edge)
            if (woodPlaque != null) {
                Image(
                    bitmap = woodPlaque.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(width = canvasWidth * 0.78f, height = canvasHeight * 0.40f)
                        .offset(y = canvasHeight * 0.17f),
                    contentScale = ContentScale.FillBounds
                )
            }

            // 4. Floating glossy marbles around Floofie & plaque
            if (marbleYellow != null) {
                Image(
                    bitmap = marbleYellow.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(canvasWidth * 0.115f)
                        .offset(x = -canvasWidth * 0.25f, y = -canvasHeight * 0.22f)
                        .graphicsLayer { translationY = marbleBobA },
                    contentScale = ContentScale.Fit
                )
            }
            if (marblePink != null) {
                Image(
                    bitmap = marblePink.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(canvasWidth * 0.115f)
                        .offset(x = -canvasWidth * 0.33f, y = -canvasHeight * 0.14f)
                        .graphicsLayer { translationY = marbleBobB },
                    contentScale = ContentScale.Fit
                )
            }
            if (marbleCyan != null) {
                Image(
                    bitmap = marbleCyan.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(canvasWidth * 0.088f)
                        .offset(x = -canvasWidth * 0.29f, y = -canvasHeight * 0.08f)
                        .graphicsLayer { translationY = marbleBobC },
                    contentScale = ContentScale.Fit
                )
            }
            if (marblePurple != null) {
                Image(
                    bitmap = marblePurple.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(canvasWidth * 0.082f)
                        .offset(x = canvasWidth * 0.20f, y = -canvasHeight * 0.22f)
                        .graphicsLayer { translationY = marbleBobB },
                    contentScale = ContentScale.Fit
                )
            }
            if (marbleGreen != null) {
                Image(
                    bitmap = marbleGreen.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(canvasWidth * 0.115f)
                        .offset(x = canvasWidth * 0.285f, y = -canvasHeight * 0.19f)
                        .graphicsLayer { translationY = marbleBobA },
                    contentScale = ContentScale.Fit
                )
            }
            if (marbleOrange != null) {
                Image(
                    bitmap = marbleOrange.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(canvasWidth * 0.105f)
                        .offset(x = canvasWidth * 0.34f, y = -canvasHeight * 0.09f)
                        .graphicsLayer { translationY = marbleBobC },
                    contentScale = ContentScale.Fit
                )
            }

            // 5. Animated Blue Floofie Mascot peeking above wordmark
            val activeFloofie = floofieFrames.getOrNull(poseIndex) ?: floofieFrames.firstOrNull()
            if (activeFloofie != null) {
                Image(
                    bitmap = activeFloofie.asImageBitmap(),
                    contentDescription = "Floofie Mascot",
                    modifier = Modifier
                        .size(width = canvasWidth * 0.37f, height = canvasWidth * 0.325f)
                        .offset(x = -canvasWidth * 0.015f, y = -canvasHeight * 0.23f)
                        .graphicsLayer {
                            translationY = floofieBobY + floofiePop.value
                            rotationZ = floofieTilt
                            transformOrigin = TransformOrigin(0.5f, 0.85f)
                        },
                    contentScale = ContentScale.Fit
                )
            }

            // 6. "Floofies" Bubble Wordmark in front of Floofie paws & plaque
            if (wordmarkFloofies != null) {
                Image(
                    bitmap = wordmarkFloofies.asImageBitmap(),
                    contentDescription = "Floofies Marble Sort",
                    modifier = Modifier
                        .size(width = canvasWidth * 0.86f, height = canvasHeight * 0.44f)
                        .offset(y = canvasHeight * 0.02f)
                        .graphicsLayer {
                            scaleX = titleScale
                            scaleY = titleScale
                        },
                    contentScale = ContentScale.Fit
                )
            }

            // 7. "Marble Sort" Curved Blue Banner below "Floofies"
            if (bannerMarbleSort != null) {
                Image(
                    bitmap = bannerMarbleSort.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(width = canvasWidth * 0.68f, height = canvasHeight * 0.31f)
                        .offset(y = canvasHeight * 0.25f)
                        .graphicsLayer {
                            scaleX = bannerScale
                            scaleY = bannerScale
                        },
                    contentScale = ContentScale.Fit
                )
            }

            // 8. Twinkling Stars & Sparkles around the Logo
            if (star1 != null) {
                Image(
                    bitmap = star1.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(canvasWidth * 0.095f)
                        .offset(x = -canvasWidth * 0.37f, y = -canvasHeight * 0.32f)
                        .alpha(0.55f + 0.45f * twinkleAlpha1)
                        .graphicsLayer {
                            val s = 0.8f + 0.28f * twinkleAlpha1
                            scaleX = s
                            scaleY = s
                            rotationZ = starSpin
                        },
                    contentScale = ContentScale.Fit
                )
            }
            if (star2 != null) {
                Image(
                    bitmap = star2.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(canvasWidth * 0.085f)
                        .offset(x = canvasWidth * 0.37f, y = -canvasHeight * 0.31f)
                        .alpha(0.55f + 0.45f * twinkleAlpha2)
                        .graphicsLayer {
                            val s = 0.8f + 0.28f * twinkleAlpha2
                            scaleX = s
                            scaleY = s
                            rotationZ = -starSpin
                        },
                    contentScale = ContentScale.Fit
                )
            }
            if (sparkleBig != null) {
                Image(
                    bitmap = sparkleBig.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(canvasWidth * 0.068f)
                        .offset(x = -canvasWidth * 0.19f, y = -canvasHeight * 0.37f)
                        .alpha(twinkleAlpha2)
                        .graphicsLayer {
                            val s = 0.65f + 0.45f * twinkleAlpha2
                            scaleX = s
                            scaleY = s
                        },
                    contentScale = ContentScale.Fit
                )
            }
            if (sparkleMed != null) {
                Image(
                    bitmap = sparkleMed.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(canvasWidth * 0.058f)
                        .offset(x = canvasWidth * 0.20f, y = -canvasHeight * 0.36f)
                        .alpha(twinkleAlpha1)
                        .graphicsLayer {
                            val s = 0.65f + 0.45f * twinkleAlpha1
                            scaleX = s
                            scaleY = s
                        },
                    contentScale = ContentScale.Fit
                )
            }
            if (sparkleSmall != null) {
                Image(
                    bitmap = sparkleSmall.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(canvasWidth * 0.05f)
                        .offset(x = canvasWidth * 0.32f, y = canvasHeight * 0.30f)
                        .alpha(twinkleAlpha1)
                        .graphicsLayer {
                            val s = 0.7f + 0.4f * twinkleAlpha1
                            scaleX = s
                            scaleY = s
                        },
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}
