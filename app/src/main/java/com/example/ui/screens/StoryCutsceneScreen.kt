package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.assets.AssetLoader
import com.example.game.sound.SoundManager
import com.example.game.story.StoryBeat
import com.example.game.story.StoryEpisode
import com.example.game.story.StorySfxCue
import com.example.game.story.StoryStageChoreography
import com.example.ui.theme.FredokaFontFamily
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun StoryCutsceneScreen(
    episode: StoryEpisode,
    levelToStart: Int,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    var beatIndex by remember(episode.chapterNumber) { mutableIntStateOf(0) }
    val beats = episode.beats
    val currentBeat = beats[beatIndex.coerceIn(0, beats.lastIndex)]
    val isLastBeat = beatIndex >= beats.lastIndex

    BackHandler {
        SoundManager.stopStorySfx()
        SoundManager.playTap()
        onComplete()
    }

    DisposableEffect(Unit) {
        onDispose {
            SoundManager.stopStorySfx()
        }
    }

    // Play beat sound cue and auto-advance non-final beats
    LaunchedEffect(episode.chapterNumber, beatIndex) {
        SoundManager.stopStorySfx()
        when (currentBeat.sfxCue) {
            StorySfxCue.CHEER -> SoundManager.playFluffyCheer()
            StorySfxCue.ALERT -> SoundManager.playBlocked()
            StorySfxCue.POWERUP -> SoundManager.playPowerup()
            StorySfxCue.POP -> SoundManager.playPopBurst()
            StorySfxCue.WIN -> SoundManager.playWin()
            StorySfxCue.UFO_ARRIVE -> SoundManager.playUfoArrive()
            StorySfxCue.UFO_BEAM -> SoundManager.playUfoBeam()
            StorySfxCue.HERO_DASH -> SoundManager.playStoryWhoosh()
        }
        if (!isLastBeat) {
            val holdMs = if (currentBeat.choreography == StoryStageChoreography.ABDUCTION_BEAM) {
                5400L
            } else {
                4400L
            }
            delay(holdMs)
            beatIndex++
        }
    }

    // Continuous background, UFO hover, vortex & abduction tractor-beam motion
    val infiniteTransition = rememberInfiniteTransition(label = "story_motion")
    val kenBurnsScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.09f,
        animationSpec = infiniteRepeatable(
            animation = tween(8500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ken_burns"
    )
    val floatBob by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_bob"
    )
    val vortexSpin by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vortex_spin"
    )
    val abductionLoop by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "abduction_loop"
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

    // Beat entrance, UFO swoop-in & ONE-SHOT abduction animations
    val beatEnter = remember { Animatable(0f) }
    val ufoArrival = remember { Animatable(0f) }
    val abductionProgress = remember { Animatable(0f) }
    LaunchedEffect(episode.chapterNumber, beatIndex) {
        beatEnter.snapTo(0f)
        ufoArrival.snapTo(0f)
        beatEnter.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }
    LaunchedEffect(episode.chapterNumber, beatIndex) {
        ufoArrival.animateTo(
            targetValue = 1f,
            animationSpec = tween(1050, easing = FastOutSlowInEasing)
        )
    }
    LaunchedEffect(episode.chapterNumber, beatIndex) {
        abductionProgress.snapTo(0f)
        if (currentBeat.choreography == StoryStageChoreography.ABDUCTION_BEAM) {
            delay(260L)
            abductionProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 3800, easing = LinearEasing)
            )
        }
    }

    val assetBackdrop = remember(episode.backdrop) {
        episode.backdrop.assetPath?.let { AssetLoader.loadBitmap(context, it) }
    }
    val btnLevelGreen = remember {
        AssetLoader.loadBitmap(context, "profile/btn_level_green.png")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF071330))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                SoundManager.playTap()
                if (!isLastBeat) {
                    SoundManager.stopStorySfx()
                    beatIndex++
                }
            }
            .testTag("story_cutscene_screen")
    ) {
        // 1. Full-screen 9:16 Backdrop with gentle Ken Burns zoom
        val drawableRes = episode.backdrop.drawableRes
        if (drawableRes != null) {
            Image(
                painter = painterResource(id = drawableRes),
                contentDescription = episode.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = kenBurnsScale
                        scaleY = kenBurnsScale
                    }
            )
        } else if (assetBackdrop != null) {
            Image(
                bitmap = assetBackdrop.asImageBitmap(),
                contentDescription = episode.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = kenBurnsScale
                        scaleY = kenBurnsScale
                    }
            )
        }

        // Atmospheric top + bottom gradient scrim for crystal-clear contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color(0x99020B24),
                        0.28f to Color.Transparent,
                        0.62f to Color(0x33020B24),
                        1.0f to Color(0xE6040D26)
                    )
                )
        )

        // 2. Twinkling magic stars across the sky
        Canvas(modifier = Modifier.fillMaxSize()) {
            val starSeeds = listOf(
                Offset(0.12f, 0.22f),
                Offset(0.28f, 0.16f),
                Offset(0.50f, 0.19f),
                Offset(0.78f, 0.14f),
                Offset(0.88f, 0.28f),
                Offset(0.18f, 0.45f),
                Offset(0.84f, 0.48f)
            )
            starSeeds.forEachIndexed { idx, pos ->
                val phase = ((sparklePhase + idx * 0.14f) % 1f) * 2f * PI.toFloat()
                val alpha = 0.25f + 0.65f * ((sin(phase) + 1f) * 0.5f)
                val r = (3.dp.toPx()) + (2.5.dp.toPx()) * ((cos(phase) + 1f) * 0.5f)
                drawCircle(
                    color = episode.accentColor.copy(alpha = alpha * 0.65f),
                    radius = r * 1.9f,
                    center = Offset(size.width * pos.x, size.height * pos.y)
                )
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = r * 0.8f,
                    center = Offset(size.width * pos.x, size.height * pos.y)
                )
            }
        }

        // 3. Main Animated Stage (UFOs, Live Tractor Beam, Rising Floofies, Villain, Hero)
        StorySpriteStage(
            beat = currentBeat,
            enterProgress = beatEnter.value,
            ufoArrivalProgress = ufoArrival.value,
            abductionProgress = abductionProgress.value,
            abductionLoop = abductionLoop,
            floatBob = floatBob,
            vortexSpin = vortexSpin,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(top = 56.dp, bottom = 235.dp)
        )

        // 4. Safe-Area HUD: Clean Skip Button + Bottom Dialogue Card
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar: Skip Button only (no chapter number clutter)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xCC0B2B66))
                        .border(2.dp, Color(0xFF7CE7FF), RoundedCornerShape(24.dp))
                        .clickable {
                            SoundManager.stopStorySfx()
                            SoundManager.playTap()
                            onComplete()
                        }
                        .padding(horizontal = 18.dp)
                        .testTag("story_skip_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SKIP >>",
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Bottom Dialogue Card & Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .align(Alignment.CenterHorizontally),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Progress Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    beats.forEachIndexed { idx, _ ->
                        val active = idx == beatIndex
                        Box(
                            modifier = Modifier
                                .height(10.dp)
                                .width(if (active) 26.dp else 10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active) Color(0xFFFFE600) else Color.White.copy(alpha = 0.45f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (active) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                // Story Dialogue Panel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(16.dp, RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xEE0C3375), Color(0xF5061B47))
                            )
                        )
                        .border(2.5.dp, episode.accentColor, RoundedCornerShape(24.dp))
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    AnimatedContent(
                        targetState = beatIndex,
                        transitionSpec = {
                            fadeIn(tween(240)) togetherWith fadeOut(tween(180))
                        },
                        label = "dialogue_anim"
                    ) { targetIdx ->
                        val beat = beats[targetIdx.coerceIn(0, beats.lastIndex)]
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(episode.accentColor.copy(alpha = 0.25f))
                                    .border(1.dp, episode.accentColor, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = beat.speaker.uppercase(),
                                    fontFamily = FredokaFontFamily,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    color = Color(0xFFFFE600),
                                    letterSpacing = 0.8.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = beat.dialogue,
                                fontFamily = FredokaFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                lineHeight = 22.sp,
                                color = Color.White,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isLastBeat) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
                                    )
                                )
                                .border(2.dp, Color(0xFFBAE6FD), RoundedCornerShape(26.dp))
                                .clickable {
                                    SoundManager.stopStorySfx()
                                    SoundManager.playTap()
                                    beatIndex++
                                }
                                .testTag("story_next_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "NEXT SCENE >",
                                fontFamily = FredokaFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .widthIn(max = 340.dp)
                                .height(82.dp)
                                .testTag("story_start_button")
                                .clickable {
                                    SoundManager.stopStorySfx()
                                    SoundManager.playPowerup()
                                    onComplete()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (btnLevelGreen != null) {
                                Image(
                                    bitmap = btnLevelGreen.asImageBitmap(),
                                    contentDescription = if (episode.isFinaleEpilogue) {
                                        "Back Home"
                                    } else {
                                        "Play Level $levelToStart"
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(41.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color(0xFF4ADE80), Color(0xFF22C55E), Color(0xFF15803D))
                                            )
                                        )
                                        .border(3.dp, Color(0xFFFFD700), RoundedCornerShape(41.dp))
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (episode.isFinaleEpilogue) {
                                        "BACK HOME"
                                    } else {
                                        "LEVEL $levelToStart"
                                    },
                                    fontFamily = FredokaFontFamily,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 28.sp,
                                    color = Color.White,
                                    letterSpacing = 2.sp
                                )
                                Text(
                                    text = if (episode.isFinaleEpilogue) {
                                        "ISLAND COMPLETE!"
                                    } else {
                                        "TAP TO PLAY"
                                    },
                                    fontFamily = FredokaFontFamily,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = Color(0xFFFFE27D),
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StorySpriteStage(
    beat: StoryBeat,
    enterProgress: Float,
    ufoArrivalProgress: Float,
    abductionProgress: Float,
    abductionLoop: Float,
    floatBob: Float,
    vortexSpin: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val heroBmp = remember(beat.heroSprite) { AssetLoader.loadBitmap(context, beat.heroSprite) }
    val villainBmp = remember(beat.villainSprite) {
        beat.villainSprite?.let { AssetLoader.loadBitmap(context, it) }
    }
    val ufoBmp = remember(beat.ufoSprite) {
        beat.ufoSprite?.let { AssetLoader.loadBitmap(context, it) }
    }
    val bubbleBmp = remember { AssetLoader.loadBitmap(context, "story/bubble_big.png") }
    val cageBmp = remember { AssetLoader.loadBitmap(context, "story/cage_trapped.png") }
    val vortexBmp = remember { AssetLoader.loadBitmap(context, "story/vortex_purple.png") }
    val cloudBmp = remember { AssetLoader.loadBitmap(context, "story/cloud_puff.png") }
    val starBmp = remember { AssetLoader.loadBitmap(context, "story/star_gold.png") }
    val heartBmp = remember { AssetLoader.loadBitmap(context, "story/heart_red.png") }
    val speechBubbleBmp = remember { AssetLoader.loadBitmap(context, "story/speech_bubble.png") }

    val babyBitmaps = remember(beat.babyColors, beat.choreography) {
        val mood = when (beat.choreography) {
            StoryStageChoreography.PEACEFUL_PLAY,
            StoryStageChoreography.CELEBRATION_FIESTA -> "happy_open"
            StoryStageChoreography.ABDUCTION_BEAM,
            StoryStageChoreography.CAVE_TRAP -> "surprised"
            else -> "curious"
        }
        beat.babyColors.mapNotNull { color ->
            AssetLoader.getFluffy(context, color, mood)
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val stageW = maxWidth
        val stageH = maxHeight

        // Exact UFO anchor position so tractor beam and rising Floofies align with the UFO emitter
        val ufoW = 228.dp
        val ufoH = 152.dp
        val isAbduction = beat.choreography == StoryStageChoreography.ABDUCTION_BEAM
        val isArrival = beat.choreography == StoryStageChoreography.VILLAIN_ARRIVAL
        val ufoTargetCenterX = stageW * 0.63f
        val ufoFlyX = if (isArrival) {
            (-ufoW * 0.6f) + (ufoTargetCenterX + ufoW * 0.6f) * ufoArrivalProgress
        } else {
            ufoTargetCenterX + sin(abductionLoop * 2f * PI.toFloat()).dp * 6f
        }
        val ufoTopY = if (isArrival) {
            (-90.dp) + (104.dp * ufoArrivalProgress) + (floatBob * 0.6f).dp
        } else {
            10.dp + (floatBob * 0.7f).dp
        }
        val ufoEmitterY = ufoTopY + ufoH * 0.84f

        // 1. Real-time Tractor Beam Cone + Rising Energy Rings (tapers off right after all babies are inside)
        if ((isAbduction || (isArrival && ufoArrivalProgress > 0.55f)) && ufoBmp != null) {
            val abductionTailFade = if (isAbduction && abductionProgress > 0.92f) {
                (1f - (abductionProgress - 0.92f) / 0.08f).coerceIn(0f, 1f)
            } else {
                1f
            }
            val beamAlpha = if (isAbduction) {
                abductionTailFade
            } else {
                ((ufoArrivalProgress - 0.55f) / 0.45f).coerceIn(0f, 1f)
            }
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = ufoFlyX.toPx()
                val topY = ufoEmitterY.toPx()
                val botY = size.height * 0.94f
                val topHalf = 24.dp.toPx()
                val botHalf = 112.dp.toPx()

                val beamPath = Path().apply {
                    moveTo(centerX - topHalf, topY)
                    lineTo(centerX + topHalf, topY)
                    lineTo(centerX + botHalf, botY)
                    lineTo(centerX - botHalf, botY)
                    close()
                }

                // Outer magenta-purple tractor cone
                drawPath(
                    path = beamPath,
                    brush = Brush.verticalGradient(
                        0.0f to Color(0xCC22D3EE).copy(alpha = 0.78f * beamAlpha),
                        0.45f to Color(0xAAEC4899).copy(alpha = 0.50f * beamAlpha),
                        1.0f to Color(0x33A855F7).copy(alpha = 0.16f * beamAlpha),
                        startY = topY,
                        endY = botY
                    )
                )

                // Inner bright cyan core beam
                val innerPath = Path().apply {
                    moveTo(centerX - topHalf * 0.55f, topY)
                    lineTo(centerX + topHalf * 0.55f, topY)
                    lineTo(centerX + botHalf * 0.55f, botY)
                    lineTo(centerX - botHalf * 0.55f, botY)
                    close()
                }
                drawPath(
                    path = innerPath,
                    brush = Brush.verticalGradient(
                        0.0f to Color.White.copy(alpha = 0.65f * beamAlpha),
                        0.6f to Color(0xFF67E8F9).copy(alpha = 0.30f * beamAlpha),
                        1.0f to Color.Transparent,
                        startY = topY,
                        endY = botY
                    )
                )

                // Ground glow pool at bottom of tractor beam
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFE879F9).copy(alpha = 0.55f * beamAlpha),
                            Color.Transparent
                        ),
                        center = Offset(centerX, botY),
                        radius = botHalf
                    ),
                    topLeft = Offset(centerX - botHalf, botY - 18.dp.toPx()),
                    size = Size(botHalf * 2f, 36.dp.toPx())
                )

                // 3 Rising energy rings traveling UP the tractor beam into the UFO
                for (ringIdx in 0 until 3) {
                    val t = (abductionLoop + ringIdx * 0.333f) % 1f
                    // t=0 at bottom -> t=1 at top UFO emitter
                    val ry = botY + (topY - botY) * t
                    val halfW = botHalf + (topHalf - botHalf) * t
                    val ringAlpha = sin(t * PI.toFloat()).coerceIn(0f, 1f) * 0.85f * beamAlpha
                    drawOval(
                        color = Color(0xFF67E8F9).copy(alpha = ringAlpha),
                        topLeft = Offset(centerX - halfW, ry - 8.dp.toPx() * (1f - 0.5f * t)),
                        size = Size(halfW * 2f, 16.dp.toPx() * (1f - 0.5f * t)),
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }
            }
        }

        // 2. Spinning Purple Vortex behind threats
        if ((beat.choreography == StoryStageChoreography.VORTEX_SHOWDOWN ||
                beat.choreography == StoryStageChoreography.CAVE_TRAP) && vortexBmp != null
        ) {
            Image(
                bitmap = vortexBmp.asImageBitmap(),
                contentDescription = "Purple Vortex",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-18).dp, y = 14.dp)
                    .size(145.dp)
                    .graphicsLayer {
                        rotationZ = vortexSpin
                        scaleX = 0.7f + 0.3f * enterProgress
                        scaleY = 0.7f + 0.3f * enterProgress
                    }
            )
        }

        // 3. Baby Floofies — One-shot Abduction UP into UFO (never teleport back down!)
        if (babyBitmaps.isNotEmpty()) {
            when (beat.choreography) {
                StoryStageChoreography.ABDUCTION_BEAM -> {
                    val abductCount = babyBitmaps.size.coerceAtMost(4)
                    val flightDuration = 0.40f
                    val startStep = if (abductCount > 1) 0.50f / (abductCount - 1) else 0f

                    for (idx in 0 until abductCount) {
                        val startT = 0.04f + idx * startStep
                        val endT = startT + flightDuration
                        // Once a baby reaches the UFO, it stays GONE forever!
                        if (abductionProgress >= endT) continue

                        val bmp = babyBitmaps[idx]
                        val localT = if (abductionProgress <= startT) {
                            0f
                        } else {
                            ((abductionProgress - startT) / flightDuration).coerceIn(0f, 1f)
                        }
                        val pull = if (localT <= 0f) 0f else localT * localT * (0.35f + 0.65f * localT)

                        val startY = stageH * 0.86f
                        val endY = ufoTopY + ufoH * 0.68f
                        val groundSpread = ((idx - (abductCount - 1) / 2f) * 44f).dp
                        val waitBob = if (localT == 0f) sin(abductionLoop * 8f * PI.toFloat() + idx) * 3f else 0f
                        val swayX = if (localT > 0f) {
                            (sin(localT * 4f * PI.toFloat() + idx) * (10f * (1f - pull))).dp
                        } else {
                            0.dp
                        }

                        val curY = startY + (endY - startY) * pull + waitBob.dp
                        val curX = ufoFlyX + groundSpread * (1f - pull * 0.92f) + swayX

                        val bubbleScale = (1.05f - 0.65f * pull).coerceIn(0.35f, 1.05f)
                        val bubbleAlpha = if (localT > 0.80f) {
                            ((1f - localT) / 0.20f).coerceIn(0f, 1f)
                        } else {
                            1f
                        }

                        val boxSize = 78.dp
                        Box(
                            modifier = Modifier
                                .offset(x = curX - boxSize / 2f, y = curY - boxSize / 2f)
                                .size(boxSize)
                                .alpha(bubbleAlpha)
                                .graphicsLayer {
                                    scaleX = bubbleScale
                                    scaleY = bubbleScale
                                    rotationZ = if (localT > 0f) sin(localT * 3f * PI.toFloat()) * 10f else 0f
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Baby Floofie Abducted into UFO",
                                modifier = Modifier.size(50.dp)
                            )
                            if (bubbleBmp != null) {
                                Image(
                                    bitmap = bubbleBmp.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .alpha(if (localT > 0f) 0.82f else 0.45f)
                                )
                            }
                        }
                    }
                }

                StoryStageChoreography.PEACEFUL_PLAY,
                StoryStageChoreography.CELEBRATION_FIESTA -> {
                    val rowCount = babyBitmaps.size.coerceAtMost(6)
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 14.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy((-4).dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        babyBitmaps.take(rowCount).forEachIndexed { idx, bmp ->
                            val bounce = sin((floatBob / 8f) * PI.toFloat() + idx * 0.9f) * 9f
                            Box(contentAlignment = Alignment.TopCenter) {
                                if (beat.choreography == StoryStageChoreography.CELEBRATION_FIESTA && idx % 2 == 0 && heartBmp != null) {
                                    Image(
                                        bitmap = heartBmp.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(22.dp)
                                            .offset(y = (-22 + bounce * 0.5f).dp)
                                    )
                                }
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Baby Floofie",
                                    modifier = Modifier
                                        .size(56.dp)
                                        .offset(y = bounce.dp)
                                )
                            }
                        }
                    }
                }

                StoryStageChoreography.VILLAIN_ARRIVAL -> {
                    // Startled babies still on ground right as the UFO arrives before abduction
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 20.dp, bottom = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        babyBitmaps.take(3).forEachIndexed { idx, bmp ->
                            val jitter = sin(abductionLoop * 6f * PI.toFloat() + idx) * 4f
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Startled Floofie",
                                modifier = Modifier
                                    .size(48.dp)
                                    .offset(
                                        x = jitter.dp,
                                        y = (floatBob * 0.4f * (if (idx % 2 == 0) 1f else -1f)).dp
                                    )
                            )
                        }
                    }
                }

                // In HERO_CHARGE, VORTEX_SHOWDOWN, and CAVE_TRAP, babies have already been
                // abducted or locked in cages — keep the ground completely clear!
                StoryStageChoreography.CAVE_TRAP,
                StoryStageChoreography.VORTEX_SHOWDOWN,
                StoryStageChoreography.HERO_CHARGE -> Unit
            }
        }

        // 4. Clean Isolated UFO hovering ABOVE the tractor beam & rising Floofies (so they disappear INTO it!)
        if (ufoBmp != null) {
            val tiltAngle = if (isArrival) {
                (1f - ufoArrivalProgress) * -14f + sin(abductionLoop * 2f * PI.toFloat()) * 3f
            } else {
                sin(abductionLoop * 2f * PI.toFloat()) * 3.5f
            }
            Image(
                bitmap = ufoBmp.asImageBitmap(),
                contentDescription = "Lord Shadow Puff UFO",
                modifier = Modifier
                    .offset(x = ufoFlyX - ufoW / 2f, y = ufoTopY)
                    .size(width = ufoW, height = ufoH)
                    .graphicsLayer {
                        rotationZ = tiltAngle
                    },
                contentScale = ContentScale.Fit
            )
        }

        // 5. Villain standing / pointing in VORTEX_SHOWDOWN or non-UFO villain scenes
        if (villainBmp != null && beat.choreography == StoryStageChoreography.VORTEX_SHOWDOWN) {
            Image(
                bitmap = villainBmp.asImageBitmap(),
                contentDescription = "Lord Shadow Puff",
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = (-16).dp + (80f * (1f - enterProgress)).dp, y = (floatBob * 0.5f).dp)
                    .size(164.dp)
                    .alpha(enterProgress.coerceIn(0f, 1f)),
                contentScale = ContentScale.Fit
            )
        } else if (villainBmp != null && ufoBmp == null) {
            Image(
                bitmap = villainBmp.asImageBitmap(),
                contentDescription = "Lord Shadow Puff",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-18).dp, y = 24.dp + (floatBob * 0.6f).dp)
                    .size(154.dp),
                contentScale = ContentScale.Fit
            )
        }

        // 6. Trapped Cages in CAVE_TRAP
        if (beat.choreography == StoryStageChoreography.CAVE_TRAP && cageBmp != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = (-18).dp, y = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                repeat(2) { idx ->
                    Image(
                        bitmap = cageBmp.asImageBitmap(),
                        contentDescription = "Trapped Floofie Cage",
                        modifier = Modifier
                            .size(96.dp)
                            .offset(y = (floatBob * (if (idx == 0) 0.5f else -0.5f)).dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        // 7. Hero Blue Floofie in foreground left / center-left
        if (heroBmp != null) {
            val isDash = beat.choreography == StoryStageChoreography.HERO_CHARGE
            val heroSize = if (isDash) 168.dp else 148.dp
            val heroDashSweep = if (isDash) (sin(abductionLoop * 2f * PI.toFloat()) * 10f) else 0f
            val heroEnterX = if (isDash) (-140f * (1f - enterProgress)) + heroDashSweep else (-50f * (1f - enterProgress))
            val heroOffsetY = if (isDash) floatBob * 0.45f else floatBob * 0.65f

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 10.dp)
                    .offset(x = heroEnterX.dp, y = heroOffsetY.dp)
            ) {
                if (isDash && cloudBmp != null) {
                    Image(
                        bitmap = cloudBmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(92.dp)
                            .align(Alignment.BottomStart)
                            .offset(x = (-18).dp, y = 8.dp)
                            .alpha(0.75f)
                    )
                }

                Image(
                    bitmap = heroBmp.asImageBitmap(),
                    contentDescription = "Hero Blue Floofie",
                    modifier = Modifier
                        .size(heroSize)
                        .graphicsLayer {
                            val pop = 0.85f + 0.15f * enterProgress
                            scaleX = pop
                            scaleY = pop
                        },
                    contentScale = ContentScale.Fit
                )

                if (beat.choreography == StoryStageChoreography.HERO_CHARGE && starBmp != null) {
                    Image(
                        bitmap = starBmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(34.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-6).dp)
                    )
                } else if (beat.choreography == StoryStageChoreography.PEACEFUL_PLAY &&
                    speechBubbleBmp != null &&
                    (beat.heroSprite.contains("happy") ||
                        beat.heroSprite.contains("wink") ||
                        beat.heroSprite.contains("laugh"))
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 28.dp, y = (-26).dp)
                            .size(width = 76.dp, height = 42.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = speechBubbleBmp.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds
                        )
                        Text(
                            text = "Yay!",
                            fontFamily = FredokaFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = Color(0xFF5E3A10),
                            modifier = Modifier.offset(y = (-3).dp)
                        )
                    }
                }
            }
        }
    }
}
