package com.example.game.assets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.game.model.DispenserDirection
import com.example.game.model.MarbleColor
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

object AssetLoader {
    private val cache = ConcurrentHashMap<String, Bitmap>()

    fun loadBitmap(context: Context, relativePath: String): Bitmap? {
        val cached = cache[relativePath]
        if (cached != null && !cached.isRecycled) return cached

        return try {
            val assetPath = "images/$relativePath"
            context.assets.open(assetPath).use { stream: InputStream ->
                val opts = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val bitmap = BitmapFactory.decodeStream(stream, null, opts)
                if (bitmap != null) {
                    cache[relativePath] = bitmap
                }
                bitmap
            }
        } catch (e: Exception) {
            null
        }
    }

    // Preload key bitmaps on app launch or screen init
    fun preloadBitmaps(context: Context) {
        val criticalAssets = listOf(
            "radiant_cyan_blue_gradient.png",
            "cyber_game_frame.png",
            "coin_rack.png",
            "bucket_spritesheet.png",
            "bucket_spritesheet_purple.png",
            "bucket_spritesheet_extra.png",
            "slotbar/slotbar_lights_on.png",
            "slotbar/slotbar_lights_off.png",
            "tropical_beach_resort_entrance.png",
            "splash_screen.png",
            "hand_cursor_trimmed.png",
            "powerups/magnet_btn_0.png",
            "powerups/magnet_btn_1.png",
            "powerups/magnet_btn_2.png",
            "powerups/magnet_btn_3.png",
            "powerups/magnet_btn_4.png",
            "powerups/magnet_btn_5.png",
            "powerups/magnet_fx_0.png",
            "powerups/magnet_fx_1.png",
            "powerups/magnet_fx_2.png",
            "powerups/magnet_fx_3.png",
            "powerups/magnet_fx_4.png",
            "powerups/shuffle_btn_0.png",
            "powerups/shuffle_btn_1.png",
            "powerups/shuffle_btn_2.png",
            "powerups/shuffle_btn_3.png",
            "powerups/shuffle_btn_4.png",
            "powerups/shuffle_fx_0.png",
            "powerups/shuffle_fx_1.png",
            "powerups/shuffle_fx_2.png",
            "powerups/shuffle_fx_3.png",
            "powerups/shuffle_fx_4.png",
            "powerups/cannon_btn_0.png",
            "powerups/cannon_btn_1.png",
            "powerups/cannon_btn_2.png",
            "powerups/cannon_btn_3.png",
            "powerups/cannon_btn_4.png",
            "powerups/cannon_idle.png",
            "powerups/cannon_charge_0.png",
            "powerups/cannon_charge_1.png",
            "powerups/cannon_charge_2.png",
            "powerups/cannon_charge_3.png",
            "powerups/cannon_charge_4.png",
            "powerups/cannon_fire_0.png",
            "powerups/cannon_fire_1.png",
            "powerups/cannon_smoke_0.png",
            "powerups/cannon_smoke_1.png",
            "powerups/cannon_smoke_2.png",
            "powerups/cannon_smoke_3.png",
            "powerups/cannon_fx_smoke.png",
            "powerups/cannon_fx_flash.png",
            "powerups/cannon_fx_ring.png",
            "powerups/cannon_fx_spark.png",
            "nav/nav_home.png",
            "nav/nav_shop.png",
            "nav/nav_settings.png",
            "profile/btn_level_green.png",
            "profile/btn_play_green_pill.png",
            "crates/crate_mystery.png",
            "cave/dispenser_0.png",
            "cave/dispenser_1.png",
            "cave/dispenser_2.png",
            "cave/dispenser_3.png",
            "cave/dispenser_4.png",
            "cave/cave_down_idle.png",
            "cave/cave_down_glow.png",
            "cave/cave_down_tray.png",
            "cave/cave_right_idle.png",
            "cave/cave_right_glow.png",
            "cave/cave_right_tray.png",
            "cave/cave_left_idle.png",
            "cave/cave_left_glow.png",
            "cave/cave_left_tray.png",
            "cave/dispenser_hood_0.png",
            "cave/dispenser_hood_1.png",
            "cave/dispenser_hood_2.png",
            "cave/dispenser_hood_3.png",
            "cave/dispenser_hood_4.png",
            "cave/dispenser_hood_5.png"
        )
        for (path in criticalAssets) {
            loadBitmap(context, path)
        }

        val logoAssets = listOf(
            "logo/wood_plaque.png",
            "logo/wordmark_floofies.png",
            "logo/banner_marble_sort.png",
            "logo/leaf_1.png",
            "logo/leaf_2.png",
            "logo/leaf_3.png",
            "logo/leaf_4.png",
            "logo/leaf_5.png",
            "logo/leaf_6.png",
            "logo/marble_pink.png",
            "logo/marble_yellow.png",
            "logo/marble_cyan.png",
            "logo/marble_purple.png",
            "logo/marble_green.png",
            "logo/marble_orange.png",
            "logo/star_1.png",
            "logo/star_2.png",
            "logo/sparkle_big.png",
            "logo/sparkle_med.png",
            "logo/sparkle_small.png",
            "logo/floofie_happy_open.png",
            "logo/floofie_wink.png",
            "logo/floofie_laugh.png",
            "logo/floofie_surprised.png",
            "logo/floofie_smile.png",
            "logo/floofie_gentle.png",
            "logo/floofie_wave.png",
            "logo/floofie_cheer.png",
            "logo/floofie_sway.png"
        )
        val splashAssets = listOf(
            "splash/splash_bg.png",
            "logo/cloud.png"
        )
        for (path in splashAssets) {
            loadBitmap(context, path)
        }

        for (path in logoAssets) {
            loadBitmap(context, path)
        }

        for (color in MarbleColor.entries) {
            val key = color.key
            loadBitmap(context, "crates/crate_open_$key.png")
            loadBitmap(context, "crates/crate_closed_$key.png")
            loadBitmap(context, "crates/crate_full_$key.png")
            loadBitmap(context, "rail_buckets/bucket_$key.png")
            loadBitmap(context, "balls/ball_$key.png")
            loadBitmap(context, "cave/tray_$key.png")
            
            // Preload primary fluffy moods
            listOf("happy_open", "wink", "eyes_closed", "surprised", "curious", "content", "angry_1", "angry_2").forEach { mood ->
                loadBitmap(context, "fluffies/fluffy_${key}_$mood.png")
            }
        }

        val storyAssets = listOf(
            "story/hero_happy.png",
            "story/hero_wink.png",
            "story/hero_laugh.png",
            "story/hero_shocked.png",
            "story/hero_scared.png",
            "story/hero_dash.png",
            "story/hero_worried.png",
            "story/villain_grin.png",
            "story/villain_point.png",
            "story/villain_angry.png",
            "story/ufo_villain.png",
            "story/ufo_beam.png",
            "story/ufo_tilted.png",
            "story/vortex_purple.png",
            "story/vortex_swirl.png",
            "story/cage_trapped.png",
            "story/bubble_big.png",
            "story/cloud_puff.png",
            "story/speech_bubble.png",
            "story/star_gold.png",
            "story/heart_red.png"
        )
        for (path in storyAssets) {
            loadBitmap(context, path)
        }
    }

    private fun getColorKey(color: MarbleColor): String {
        return color.key
    }

    fun getFluffy(context: Context, color: MarbleColor, mood: String = "content"): Bitmap? {
        val colorKey = getColorKey(color)
        return loadBitmap(context, "fluffies/fluffy_${colorKey}_$mood.png")
            ?: loadBitmap(context, "fluffies/fluffy_${colorKey}_happy_open.png")
            ?: getBall(context, color)
    }

    fun getCrateOpen(context: Context, color: MarbleColor): Bitmap? {
        return loadBitmap(context, "crates/crate_open_${getColorKey(color)}.png")
    }

    fun getCrateClosed(context: Context, color: MarbleColor): Bitmap? {
        return loadBitmap(context, "crates/crate_closed_${getColorKey(color)}.png")
    }

    fun getCrateFull(context: Context, color: MarbleColor): Bitmap? {
        return loadBitmap(context, "crates/crate_full_${getColorKey(color)}.png")
    }

    fun getRailBucket(context: Context, color: MarbleColor): Bitmap? {
        return loadBitmap(context, "rail_buckets/bucket_${getColorKey(color)}.png")
    }

    fun getBall(context: Context, color: MarbleColor): Bitmap? {
        return loadBitmap(context, "balls/ball_${getColorKey(color)}.png")
    }

    fun getBucketSpritesheet(context: Context): Bitmap? {
        return loadBitmap(context, "bucket_spritesheet.png")
    }

    fun getBucketSpritesheetPurple(context: Context): Bitmap? {
        return loadBitmap(context, "bucket_spritesheet_purple.png")
    }

    fun getBucketSpritesheetExtra(context: Context): Bitmap? {
        return loadBitmap(context, "bucket_spritesheet_extra.png")
    }

    fun resolveBucketAtlas(context: Context, color: MarbleColor): Pair<Bitmap?, Int> {
        return when (color) {
            MarbleColor.PURPLE -> Pair(getBucketSpritesheetPurple(context) ?: getBucketSpritesheet(context), 0)
            MarbleColor.PINK -> Pair(getBucketSpritesheet(context), 0)
            MarbleColor.ICE -> Pair(getBucketSpritesheet(context), 1)
            MarbleColor.YELLOW -> Pair(getBucketSpritesheet(context), 2)
            MarbleColor.LIME -> Pair(getBucketSpritesheet(context), 3)
            MarbleColor.CYAN -> Pair(getBucketSpritesheet(context), 4)
            MarbleColor.ORANGE -> Pair(getBucketSpritesheet(context), 5)
            MarbleColor.RED -> Pair(getBucketSpritesheet(context), 6)
            MarbleColor.BLACK -> Pair(getBucketSpritesheetExtra(context), 0)
            MarbleColor.DARK_GREEN -> Pair(getBucketSpritesheetExtra(context), 1)
            MarbleColor.BLUE -> Pair(getBucketSpritesheetExtra(context), 2)
            MarbleColor.BROWN -> Pair(getBucketSpritesheetExtra(context), 3)
            MarbleColor.MINT -> Pair(getBucketSpritesheetExtra(context), 4)
            MarbleColor.WHITE -> Pair(getBucketSpritesheetExtra(context), 5)
            MarbleColor.MAROON -> Pair(getBucketSpritesheetExtra(context), 6)
            MarbleColor.LIGHT_PINK -> Pair(getBucketSpritesheetExtra(context), 7)
        }
    }

    fun getBucketFrameIndex(animState: String, animTimer: Float, filled: Int): Int {
        return when (animState) {
            "queued" -> 0
            "activate" -> {
                val p = (animTimer / 0.16f).coerceIn(0f, 1f)
                (1 + (p * 3).toInt()).coerceIn(1, 4)
            }
            "hit_1" -> {
                val p = (animTimer / 0.16f).coerceIn(0f, 1f)
                (5 + (p * 3).toInt()).coerceIn(5, 8)
            }
            "hit_2" -> {
                val p = (animTimer / 0.16f).coerceIn(0f, 1f)
                (9 + (p * 3).toInt()).coerceIn(9, 12)
            }
            "hit_3" -> {
                val p = (animTimer / 0.18f).coerceIn(0f, 1f)
                (13 + (p * 3).toInt()).coerceIn(13, 16)
            }
            "hold_full" -> 16
            "vanish" -> {
                val p = (animTimer / 0.25f).coerceIn(0f, 1f)
                (17 + (p * 7).toInt()).coerceIn(17, 24)
            }
            else -> {
                when (filled) {
                    0 -> 4
                    1 -> 8
                    2 -> 12
                    else -> 16
                }
            }
        }
    }

    fun getDispenserFrame(context: Context, frameIndex: Int): Bitmap? {
        val idx = frameIndex.coerceIn(0, 4)
        return loadBitmap(context, "cave/dispenser_$idx.png")
            ?: loadBitmap(context, "cave/cave_0.png")
    }

    fun getDispenserHoodFrame(context: Context, frameIndex: Int): Bitmap? {
        val idx = frameIndex.coerceIn(0, 5)
        return loadBitmap(context, "cave/dispenser_hood_$idx.png")
    }

    fun getCrystalCaveFrame(
        context: Context,
        direction: DispenserDirection,
        phase: String
    ): Bitmap? {
        val dirKey = when (direction) {
            DispenserDirection.DOWN -> "down"
            DispenserDirection.RIGHT -> "right"
            DispenserDirection.LEFT -> "left"
        }
        val phaseKey = when (phase) {
            "glow", "tray" -> phase
            else -> "idle"
        }
        return loadBitmap(context, "cave/cave_${dirKey}_${phaseKey}.png")
            ?: loadBitmap(context, "cave/cave_${dirKey}_idle.png")
            ?: getDispenserFrame(context, 0)
    }

    fun getDispenserTray(context: Context, color: MarbleColor): Bitmap? {
        return loadBitmap(context, "cave/tray_${getColorKey(color)}.png")
    }

    fun getMagnetBtnFrame(context: Context, frameIndex: Int): Bitmap? {
        val idx = frameIndex.coerceIn(0, 5)
        return loadBitmap(context, "powerups/magnet_btn_$idx.png")
            ?: loadBitmap(context, "magnet_btn_0.png")
    }

    fun getMagnetFxFrame(context: Context, frameIndex: Int): Bitmap? {
        val idx = frameIndex.coerceIn(0, 4)
        return loadBitmap(context, "powerups/magnet_fx_$idx.png")
    }

    fun getShuffleBtnFrame(context: Context, frameIndex: Int): Bitmap? {
        val idx = frameIndex.coerceIn(0, 4)
        return loadBitmap(context, "powerups/shuffle_btn_$idx.png")
            ?: loadBitmap(context, "shuffle_btn_0.png")
    }

    fun getShuffleFxFrame(context: Context, frameIndex: Int): Bitmap? {
        val idx = frameIndex.coerceIn(0, 4)
        return loadBitmap(context, "powerups/shuffle_fx_$idx.png")
    }

    fun getCannonBtnFrame(context: Context, frameIndex: Int): Bitmap? {
        val idx = frameIndex.coerceIn(0, 4)
        return loadBitmap(context, "powerups/cannon_btn_$idx.png")
    }

    fun getCannonIdleFrame(context: Context): Bitmap? {
        return loadBitmap(context, "powerups/cannon_idle.png")
    }

    fun getCannonChargeFrame(context: Context, frameIndex: Int): Bitmap? {
        val idx = frameIndex.coerceIn(0, 4)
        return loadBitmap(context, "powerups/cannon_charge_$idx.png")
    }

    fun getCannonFireFrame(context: Context, frameIndex: Int): Bitmap? {
        val idx = frameIndex.coerceIn(0, 1)
        return loadBitmap(context, "powerups/cannon_fire_$idx.png")
    }

    fun getCannonSmokeFrame(context: Context, frameIndex: Int): Bitmap? {
        val idx = frameIndex.coerceIn(0, 3)
        return loadBitmap(context, "powerups/cannon_smoke_$idx.png")
    }
}
