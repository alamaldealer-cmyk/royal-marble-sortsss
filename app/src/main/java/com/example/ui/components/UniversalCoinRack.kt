package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FredokaFontFamily

/**
 * Universal Coin Rack Component across all screens (Home, Shop, Settings, GameBoard, LevelCompleteModal).
 * Increased size for optimal readability: width 125.dp, height 52.dp, fontSize 15.sp.
 */
@Composable
fun UniversalCoinRack(
    bitmap: Bitmap?,
    coins: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 125.dp, height = 52.dp),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Coin Rack",
                modifier = Modifier.fillMaxSize()
            )
        }
        Text(
            text = coins.toString(),
            fontFamily = FredokaFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
            color = Color.White,
            modifier = Modifier.offset(x = 15.dp, y = 3.dp)
        )
    }
}
