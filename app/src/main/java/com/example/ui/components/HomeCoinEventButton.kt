package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.assets.AssetLoader
import com.example.game.jackpot.JackpotEventManager
import com.example.game.sound.SoundManager
import com.example.ui.theme.FredokaFontFamily
import kotlinx.coroutines.delay

@Composable
fun HomeCoinEventButton(
    modifier: Modifier = Modifier,
    badgeCount: Int = 5,
    progressCurrent: Int = 5,
    progressMax: Int = 5,
    remainingMs: Long? = null,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val frames = remember {
        (0 until 16).map { idx ->
            AssetLoader.getCoinDomeFrame(context, idx)
        }
    }

    var frameIndex by remember { mutableIntStateOf(0) }
    var fallbackRemainingMs by remember { mutableLongStateOf(JackpotEventManager.CYCLE_DURATION_MS) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(75L)
            frameIndex = (frameIndex + 1) % 16
        }
    }

    LaunchedEffect(remainingMs == null) {
        if (remainingMs == null) {
            while (true) {
                delay(1000L)
                fallbackRemainingMs = if (fallbackRemainingMs > 1000L) {
                    fallbackRemainingMs - 1000L
                } else {
                    JackpotEventManager.CYCLE_DURATION_MS
                }
            }
        }
    }

    val effectiveRemainingMs = remainingMs ?: fallbackRemainingMs
    val timerText = remember(effectiveRemainingMs) {
        JackpotEventManager.formatCountdownTimer(effectiveRemainingMs)
    }

    val pressState = rememberBouncyPressState(pressedScale = 0.89f)
    val activeBitmap = frames.getOrNull(frameIndex) ?: frames.firstOrNull()

    BoxWithConstraints(
        modifier = modifier
            .width(96.dp)
            .aspectRatio(284f / 250f)
            .minimumInteractiveComponentSize()
            .semantics {
                contentDescription = "Coin Event, $badgeCount notifications, $progressCurrent of $progressMax, $timerText"
            }
            .testTag("home_coin_event_button")
            .bouncyClickTarget(
                state = pressState,
                role = Role.Button
            ) {
                SoundManager.playTap()
                onClick()
            }
            .bouncyScale(pressState)
    ) {
        val w = maxWidth
        val h = maxHeight

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clearAndSetSemantics {}
        ) {
            // 1. Animated 16-frame Coin Dome + Pink Chevron + Blue Bottom Pill
            if (activeBitmap != null) {
                Image(
                    bitmap = activeBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.High
                )
            }

        // 2. Top-Left Green Circle Badge ("5")
        val badgeSize = w * 0.28f
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = w * 0.01f, y = h * 0.085f)
                .size(badgeSize)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF8CFF24),
                            Color(0xFF45D400),
                            Color(0xFF219E00)
                        )
                    )
                )
                .border(1.6.dp, Color(0xFF156600), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Subtle glossy top highlight inside green circle
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x66FFFFFF),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = size.height * 0.55f
                    ),
                    radius = size.minDimension * 0.42f,
                    center = Offset(size.width * 0.5f, size.height * 0.36f)
                )
            }
            OutlinedEventText(
                text = badgeCount.toString(),
                fontSize = with(density) { (badgeSize * 0.56f).toSp() },
                strokeColor = Color(0xFF0E4700),
                strokeWidthPx = with(density) { 2.8.dp.toPx() }
            )
        }

        // 3. Pink Chevron Counter ("5/5")
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = h * 0.565f)
                .fillMaxWidth(0.55f),
            contentAlignment = Alignment.Center
        ) {
            OutlinedEventText(
                text = "$progressCurrent/$progressMax",
                fontSize = with(density) { (w * 0.132f).toSp() },
                strokeColor = Color(0xFF2B001F),
                strokeWidthPx = with(density) { 2.6.dp.toPx() }
            )
        }

        // 4. Bottom Blue Pill Countdown Timer ("12:03:26")
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = -(h * 0.105f))
                .fillMaxWidth(0.84f),
            contentAlignment = Alignment.Center
        ) {
            OutlinedEventText(
                text = timerText,
                fontSize = with(density) { (w * 0.145f).toSp() },
                strokeColor = Color(0xFF002766),
                strokeWidthPx = with(density) { 2.8.dp.toPx() }
            )
        }
        }
    }
}

@Composable
private fun OutlinedEventText(
    text: String,
    fontSize: TextUnit,
    strokeColor: Color,
    strokeWidthPx: Float
) {
    Box(contentAlignment = Alignment.Center) {
        Text(
            text = text,
            modifier = Modifier.clearAndSetSemantics {},
            fontFamily = FredokaFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = fontSize,
            color = strokeColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            style = TextStyle(
                drawStyle = Stroke(
                    width = strokeWidthPx,
                    join = StrokeJoin.Round
                )
            )
        )
        Text(
            text = text,
            fontFamily = FredokaFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = fontSize,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1,
            style = TextStyle(
                shadow = Shadow(
                    color = Color(0x66000000),
                    offset = Offset(0f, 1.5f),
                    blurRadius = 2f
                )
            )
        )
    }
}
