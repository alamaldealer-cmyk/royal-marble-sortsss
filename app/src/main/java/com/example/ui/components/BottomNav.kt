package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.game.assets.AssetLoader
import com.example.game.sound.SoundManager

enum class NavTab {
    HOME,
    SHOP,
    SETTINGS
}

data class HitArea(val left: Float, val top: Float, val width: Float, val height: Float)

object NavHitAreas {
    val areas = mapOf(
        NavTab.HOME to mapOf(
            NavTab.SHOP to HitArea(0.0615f, 0.2847f, 0.1972f, 0.5694f),
            NavTab.HOME to HitArea(0.3128f, 0.1215f, 0.3743f, 0.7326f),
            NavTab.SETTINGS to HitArea(0.7423f, 0.2847f, 0.1972f, 0.5694f)
        ),
        NavTab.SHOP to mapOf(
            NavTab.SHOP to HitArea(0.0615f, 0.1215f, 0.3743f, 0.7326f),
            NavTab.HOME to HitArea(0.4920f, 0.2847f, 0.1972f, 0.5694f),
            NavTab.SETTINGS to HitArea(0.7423f, 0.2847f, 0.1972f, 0.5694f)
        ),
        NavTab.SETTINGS to mapOf(
            NavTab.SHOP to HitArea(0.0615f, 0.2847f, 0.1972f, 0.5694f),
            NavTab.HOME to HitArea(0.3096f, 0.2847f, 0.1972f, 0.5694f),
            NavTab.SETTINGS to HitArea(0.5599f, 0.1215f, 0.3743f, 0.7326f)
        )
    )
}

@Composable
fun BottomNav(
    activeTab: NavTab,
    onTabChange: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val navImageName = when (activeTab) {
        NavTab.SHOP -> "nav/nav_shop.png"
        NavTab.SETTINGS -> "nav/nav_settings.png"
        NavTab.HOME -> "nav/nav_home.png"
    }

    val navBitmap: Bitmap? = remember(navImageName) {
        AssetLoader.loadBitmap(context, navImageName)
    }

    val currentAreas = NavHitAreas.areas[activeTab] ?: NavHitAreas.areas[NavTab.HOME]!!

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(943f / 288f)
    ) {
        val totalWidth = maxWidth
        val totalHeight = maxHeight

        if (navBitmap != null) {
            Image(
                bitmap = navBitmap.asImageBitmap(),
                contentDescription = "Bottom Navigation",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
        }

        // Hit areas
        for (tab in NavTab.entries) {
            val area = currentAreas[tab] ?: continue

            Box(
                modifier = Modifier
                    .offset(x = totalWidth * area.left, y = totalHeight * area.top)
                    .width(totalWidth * area.width)
                    .height(totalHeight * area.height)
                    .testTag("nav_tab_${tab.name.lowercase()}")
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        SoundManager.playTap()
                        onTabChange(tab)
                    }
            )
        }
    }
}
