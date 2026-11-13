package com.example.game.assets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
            "slotbar/slotbar_lights_on.png",
            "slotbar/slotbar_lights_off.png",
            "tropical_beach_resort_entrance.png",
            "splash_screen.png",
            "hand_cursor_trimmed.png",
            "magnet_btn_0.png",
            "shuffle_btn_0.png",
            "nav/nav_home.png",
            "nav/nav_shop.png",
            "nav/nav_settings.png",
            "profile/btn_level_green.png",
            "crates/crate_mystery.png"
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
        val splashAssets = buildList {
            add("splash/splash_bg.png")
            add("splash/banner_intro_00.png")
            for (i in 0..11) add("splash/bar_%02d.png".format(i))
            for (i in 0..3) add("splash/title_%02d.png".format(i))
            for (i in 0..3) add("splash/lockup_%02d.png".format(i))
            for (i in 0..7) add("splash/group_%02d.png".format(i))
            for (i in 0..8) add("splash/dash_%02d.png".format(i))
        }
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
            
            // Preload primary fluffy moods
            listOf("happy_open", "wink", "eyes_closed", "surprised", "curious", "content", "angry_1", "angry_2").forEach { mood ->
                loadBitmap(context, "fluffies/fluffy_${key}_$mood.png")
            }
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
}
