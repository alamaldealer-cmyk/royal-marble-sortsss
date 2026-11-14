package com.example.game.story

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.game.model.MarbleColor

enum class StoryBackdrop(
    @DrawableRes val drawableRes: Int? = null,
    val assetPath: String? = null
) {
    PEACEFUL_ISLAND(drawableRes = R.drawable.img_story_peaceful_island),
    TROPICAL_RESORT(assetPath = "tropical_beach_resort_entrance.png"),
    CRYSTAL_CAVES(drawableRes = R.drawable.img_story_crystal_caves),
    TWILIGHT_VORTEX(drawableRes = R.drawable.img_story_twilight_vortex),
    FROST_PEAKS(drawableRes = R.drawable.img_story_frost_peaks),
    CANDY_CLOUD_ARENA(assetPath = "splash/splash_bg.png"),
    UFO_MOTHERSHIP(drawableRes = R.drawable.img_story_ufo_mothership)
}

enum class StoryStageChoreography {
    PEACEFUL_PLAY,        // Hero & baby Floofies playing happily in foreground
    VILLAIN_ARRIVAL,      // UFO & Lord Shadow Puff swoop down from the sky; Hero shocked
    ABDUCTION_BEAM,       // UFO tractor beam pulls trapped Floofies in bubbles/cages upward
    CAVE_TRAP,            // Trapped Floofies inside glowing cages + spinning vortex
    VORTEX_SHOWDOWN,      // Lord Shadow Puff points angrily beside swirling cosmic portal
    HERO_CHARGE,          // Hero Blue dashes forward with power sparkles & resolve
    CELEBRATION_FIESTA    // All Floofies bouncing & cheering together with stars & hearts
}

enum class StorySfxCue {
    CHEER,
    ALERT,
    POWERUP,
    POP,
    WIN,
    UFO_ARRIVE,
    UFO_BEAM,
    HERO_DASH
}

data class StoryBeat(
    val sceneLabel: String,
    val speaker: String,
    val dialogue: String,
    val choreography: StoryStageChoreography,
    val heroSprite: String = "story/hero_happy.png",
    val villainSprite: String? = null,
    val ufoSprite: String? = null,
    val babyColors: List<MarbleColor> = listOf(
        MarbleColor.PINK,
        MarbleColor.YELLOW,
        MarbleColor.LIME,
        MarbleColor.PURPLE,
        MarbleColor.ORANGE
    ),
    val sfxCue: StorySfxCue = StorySfxCue.POP
)

data class StoryEpisode(
    val chapterNumber: Int,
    val triggerLevel: Int,
    val isFinaleEpilogue: Boolean = false,
    val levelRangeLabel: String,
    val title: String,
    val subtitle: String,
    val backdrop: StoryBackdrop,
    val accentColor: Color,
    val beats: List<StoryBeat>
)
