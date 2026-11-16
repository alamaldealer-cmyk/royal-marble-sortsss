package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.assets.AssetLoader
import com.example.game.lives.LifeSnapshot
import com.example.game.lives.LivesManager
import com.example.game.sound.SoundManager
import com.example.ui.theme.FredokaFontFamily

private val HudBarTextColor = Color(0xFF47535D)
private val HeartNumberStrokeColor = Color(0xFF8C0915)

/**
 * Top HUD Heart / Lives Pill matching the attached reference:
 * Glossy white pill bar on the right ("Full" or "MM:SS" countdown in dark slate grey)
 * with an overlapping red glossy heart on the left showing the current life count (5..0).
 */
@Composable
fun LivesHudPill(
    lifeSnapshot: LifeSnapshot,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val barBmp = remember { AssetLoader.loadBitmap(context, "hud/hud_bar_life.png") }
    val heartBmp = remember { AssetLoader.loadBitmap(context, "hud/hud_heart.png") }

    val totalHeight = if (compact) 38.dp else 44.dp
    val barHeight = if (compact) 31.dp else 36.dp
    val barWidth = if (compact) 80.dp else 92.dp
    val heartWidth = if (compact) 39.dp else 45.dp
    val heartHeight = if (compact) 36.dp else 41.dp
    val barStartOffset = if (compact) 16.dp else 18.dp
    val totalWidth = barStartOffset + barWidth

    val statusText = LivesManager.formatStatusText(lifeSnapshot)
    val statusFontSize = when {
        compact && lifeSnapshot.isFull -> 14.sp
        compact -> 13.sp
        lifeSnapshot.isFull -> 16.5.sp
        else -> 15.sp
    }
    val heartFontSize = if (compact) 17.5.sp else 20.sp

    val pressState = rememberBouncyPressState(pressedScale = 0.92f)
    val baseModifier = modifier
        .size(width = totalWidth, height = totalHeight)
        .let { m ->
            if (onClick != null) {
                m.minimumInteractiveComponentSize()
                    .bouncyClickTarget(pressState, role = Role.Button) {
                        SoundManager.playTap()
                        onClick()
                    }
                    .bouncyScale(pressState)
            } else {
                m
            }
        }

    Box(
        modifier = baseModifier.testTag("lives_hud_pill"),
        contentAlignment = Alignment.CenterStart
    ) {
        // 1. Glossy White Pill Background + "Full" / Timer Text
        Box(
            modifier = Modifier
                .offset(x = barStartOffset)
                .size(width = barWidth, height = barHeight)
                .align(Alignment.CenterStart),
            contentAlignment = Alignment.Center
        ) {
            if (barBmp != null) {
                Image(
                    bitmap = barBmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFF4F7FA))
                        .border(1.5.dp, Color(0xFFBFC8D2), RoundedCornerShape(50))
                )
            }

            // Center text inside the exposed right part of the pill (to the right of heart)
            val textStartPadding = heartWidth - barStartOffset - 2.dp
            Text(
                text = statusText,
                fontFamily = FredokaFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = statusFontSize,
                color = HudBarTextColor,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(start = textStartPadding, end = 6.dp)
                    .testTag("lives_hud_status_text")
            )
        }

        // 2. Overlapping Glossy Red Heart + Life Count Number
        Box(
            modifier = Modifier
                .size(width = heartWidth, height = heartHeight)
                .align(Alignment.CenterStart),
            contentAlignment = Alignment.Center
        ) {
            if (heartBmp != null) {
                Image(
                    bitmap = heartBmp.asImageBitmap(),
                    contentDescription = "Lives ${lifeSnapshot.lives}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(heartHeight)
                        .clip(CircleShape)
                        .background(Color(0xFFE11D48))
                )
            }

            val numStr = lifeSnapshot.lives.toString()
            Box(
                modifier = Modifier
                    .offset(y = (-1.5).dp)
                    .testTag("lives_hud_count_text"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = numStr,
                    modifier = Modifier.clearAndSetSemantics {},
                    style = TextStyle(
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = heartFontSize,
                        color = HeartNumberStrokeColor,
                        drawStyle = Stroke(
                            width = if (compact) 4.2f else 5.2f,
                            join = StrokeJoin.Round
                        ),
                        shadow = Shadow(
                            color = Color(0x664A040B),
                            offset = Offset(0f, 2f),
                            blurRadius = 2f
                        )
                    )
                )
                Text(
                    text = numStr,
                    style = TextStyle(
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = heartFontSize,
                        color = Color.White
                    )
                )
            }
        }
    }
}

/**
 * Top HUD Coins Pill using the user's new white pill bar (`hud_bar_coin.png`),
 * overlapping golden `$` coin (`hud_coin.png`), and green `+` button (`hud_plus.png`).
 * Preserves `UniversalCoinRack` signature for drop-in use across Home, Shop, Settings,
 * GameBoardScreen, and LevelCompleteModal.
 */
@Composable
fun UniversalCoinRack(
    bitmap: Bitmap? = null,
    coins: Int,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    showPlusButton: Boolean = true,
    onPlusClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val barBmp = remember { AssetLoader.loadBitmap(context, "hud/hud_bar_coin.png") ?: bitmap }
    val coinBmp = remember { AssetLoader.loadBitmap(context, "hud/hud_coin.png") }
    val plusBmp = remember { AssetLoader.loadBitmap(context, "hud/hud_plus.png") }

    val totalHeight = if (compact) 38.dp else 44.dp
    val barHeight = if (compact) 31.dp else 36.dp
    val barWidth = if (compact) 90.dp else 104.dp
    val coinSize = if (compact) 37.dp else 43.dp
    val plusSize = if (compact) 17.dp else 20.dp
    val barStartOffset = if (compact) 16.dp else 18.dp
    val totalWidth = barStartOffset + barWidth

    val coinText = coins.toString()
    val coinFontSize = when {
        compact && coinText.length >= 6 -> 11.5.sp
        compact && coinText.length >= 5 -> 12.5.sp
        compact -> 13.5.sp
        coinText.length >= 6 -> 13.sp
        coinText.length >= 5 -> 14.5.sp
        else -> 16.sp
    }

    val pressState = rememberBouncyPressState(pressedScale = 0.92f)
    val baseModifier = modifier
        .size(width = totalWidth, height = totalHeight)
        .let { m ->
            if (onPlusClick != null) {
                m.minimumInteractiveComponentSize()
                    .bouncyClickTarget(pressState, role = Role.Button) {
                        SoundManager.playTap()
                        onPlusClick()
                    }
                    .bouncyScale(pressState)
            } else {
                m
            }
        }

    Box(
        modifier = baseModifier.testTag("coins_hud_pill"),
        contentAlignment = Alignment.CenterStart
    ) {
        // 1. Glossy White Pill Background + Coin Count
        Box(
            modifier = Modifier
                .offset(x = barStartOffset)
                .size(width = barWidth, height = barHeight)
                .align(Alignment.CenterStart),
            contentAlignment = Alignment.Center
        ) {
            if (barBmp != null) {
                Image(
                    bitmap = barBmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFF4F7FA))
                        .border(1.5.dp, Color(0xFFBFC8D2), RoundedCornerShape(50))
                )
            }

            val textStartPadding = coinSize - barStartOffset + 2.dp
            Text(
                text = coinText,
                fontFamily = FredokaFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = coinFontSize,
                color = HudBarTextColor,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(start = textStartPadding, end = 6.dp)
                    .testTag("coins_hud_count_text")
            )
        }

        // 2. Overlapping Golden '$' Coin on Left
        Box(
            modifier = Modifier
                .size(coinSize)
                .align(Alignment.CenterStart),
            contentAlignment = Alignment.Center
        ) {
            if (coinBmp != null) {
                Image(
                    bitmap = coinBmp.asImageBitmap(),
                    contentDescription = "Coins $coins",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color(0xFFFFCA1A))
                        .border(2.dp, Color(0xFFC28200), CircleShape)
                )
            }
        }

        // 3. Green '+' Button tucked at bottom-right of the Golden Coin
        if (showPlusButton) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(
                        x = coinSize - plusSize * 0.72f,
                        y = (-1).dp
                    )
                    .size(plusSize)
                    .testTag("coins_hud_plus_button"),
                contentAlignment = Alignment.Center
            ) {
                if (plusBmp != null) {
                    Image(
                        bitmap = plusBmp.asImageBitmap(),
                        contentDescription = "Add Coins",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFF2ED400))
                            .border(1.5.dp, Color(0xFF0A6600), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            fontFamily = FredokaFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
