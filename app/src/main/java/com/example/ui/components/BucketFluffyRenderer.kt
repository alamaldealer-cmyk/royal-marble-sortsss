package com.example.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.ui.graphics.toArgb
import com.example.game.assets.AssetLoader
import com.example.game.constants.GameConstants
import com.example.game.model.MarbleColor

object BucketFluffyRenderer {
    private val darkBoostFilters: Map<MarbleColor, ColorMatrixColorFilter> by lazy {
        mapOf(
            MarbleColor.BLACK to buildLiftFilter(scale = 1.18f, offset = 30f, saturation = 1.0f),
            MarbleColor.MAROON to buildLiftFilter(scale = 1.24f, offset = 22f, saturation = 1.18f),
            MarbleColor.BLUE to buildLiftFilter(scale = 1.24f, offset = 22f, saturation = 1.16f),
            MarbleColor.DARK_GREEN to buildLiftFilter(scale = 1.22f, offset = 20f, saturation = 1.16f),
            MarbleColor.BROWN to buildLiftFilter(scale = 1.18f, offset = 18f, saturation = 1.14f)
        )
    }

    private fun buildLiftFilter(scale: Float, offset: Float, saturation: Float): ColorMatrixColorFilter {
        val satMatrix = ColorMatrix().apply { setSaturation(saturation) }
        val liftMatrix = ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, offset,
                0f, scale, 0f, 0f, offset,
                0f, 0f, scale, 0f, offset,
                0f, 0f, 0f, 1f, 0f
            )
        )
        satMatrix.postConcat(liftMatrix)
        return ColorMatrixColorFilter(satMatrix)
    }

    private fun isDarkColor(color: MarbleColor): Boolean {
        return color == MarbleColor.BLACK ||
            color == MarbleColor.DARK_GREEN ||
            color == MarbleColor.BLUE ||
            color == MarbleColor.BROWN ||
            color == MarbleColor.MAROON
    }

    private fun rimGlowColor(color: MarbleColor): Int {
        return when (color) {
            MarbleColor.BLACK -> AndroidColor.parseColor("#E6F0FF")
            MarbleColor.DARK_GREEN -> AndroidColor.parseColor("#88FFB2")
            MarbleColor.BLUE -> AndroidColor.parseColor("#8CD6FF")
            MarbleColor.BROWN -> AndroidColor.parseColor("#FFD999")
            MarbleColor.MAROON -> AndroidColor.parseColor("#FF8FB1")
            else -> GameConstants.COLOR_MAP[color]?.highlight?.toArgb() ?: AndroidColor.WHITE
        }
    }

    /**
     * Renders peeking fluffies inside a bucket opening with an illuminated pocket glow,
     * crisp rim separation halo, and dark-color luminance lift so dark balls stay vibrant and legible.
     */
    fun drawPeekingFluffies(
        canvas: Canvas,
        context: Context,
        color: MarbleColor,
        centerX: Float,
        bucketCenterY: Float,
        baltiSize: Float,
        filledCount: Int,
        alpha: Int = 255,
        moodProvider: (slotIndex: Int) -> String = { "content" }
    ) {
        val count = minOf(filledCount, 3)
        if (count <= 0 || alpha <= 0) return

        val dark = isDarkColor(color)
        val glowHex = rimGlowColor(color)
        val fy = bucketCenterY - baltiSize * if (dark) 0.125f else 0.085f
        val fr = baltiSize * if (dark) 0.155f else 0.145f
        val holeOffsets = floatArrayOf(-baltiSize * 0.215f, 0f, baltiSize * 0.215f)
        val alphaNorm = (alpha / 255f).coerceIn(0f, 1f)

        // 1. Soft inner bucket pocket illumination behind the balls
        val pocketRx = baltiSize * 0.38f
        val pocketGlowR = pocketRx.coerceAtLeast(1f)
        val pocketAlpha = ((if (dark) 185 else 85) * alphaNorm).toInt().coerceIn(0, 255)
        if (pocketAlpha > 0) {
            val pocketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    centerX,
                    fy + baltiSize * 0.02f,
                    pocketGlowR,
                    intArrayOf(
                        AndroidColor.argb((210 * alphaNorm).toInt().coerceIn(0, 255), 255, 255, 255),
                        glowHex,
                        AndroidColor.TRANSPARENT
                    ),
                    floatArrayOf(0f, 0.45f, 1f),
                    Shader.TileMode.CLAMP
                )
                this.alpha = pocketAlpha
            }
            canvas.save()
            canvas.translate(centerX, fy + baltiSize * 0.02f)
            canvas.scale(1.18f, 0.56f)
            canvas.drawCircle(0f, 0f, pocketGlowR, pocketPaint)
            canvas.restore()
        }

        val darkFilter = darkBoostFilters[color]

        for (h in 0 until count) {
            val fx = centerX + holeOffsets[h]

            // 2. Crisp rim halo behind each ball so dark fur never blends into dark bucket walls
            if (dark) {
                val haloR = fr * 1.32f
                val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        fx,
                        fy,
                        haloR.coerceAtLeast(1f),
                        intArrayOf(
                            AndroidColor.WHITE,
                            glowHex,
                            AndroidColor.TRANSPARENT
                        ),
                        floatArrayOf(0.55f, 0.80f, 1f),
                        Shader.TileMode.CLAMP
                    )
                    this.alpha = (225 * alphaNorm).toInt().coerceIn(0, 255)
                }
                canvas.drawCircle(fx, fy, haloR, haloPaint)
            }

            // 3. Draw Fluffy sprite (with gentle brightness/vibrancy boost for dark colors)
            val fluffyBmp = AssetLoader.getFluffy(context, color, moodProvider(h))
            if (fluffyBmp != null) {
                val ballPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                    this.alpha = alpha.coerceIn(0, 255)
                    if (darkFilter != null) {
                        colorFilter = darkFilter
                    }
                }
                canvas.drawBitmap(
                    fluffyBmp,
                    null,
                    RectF(fx - fr, fy - fr, fx + fr, fy + fr),
                    ballPaint
                )

                // 4. Subtle glossy top rim highlight so dark fur catches light cleanly
                if (dark) {
                    val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = RadialGradient(
                            fx - fr * 0.25f,
                            fy - fr * 0.48f,
                            (fr * 0.72f).coerceAtLeast(1f),
                            intArrayOf(
                                AndroidColor.argb((95 * alphaNorm).toInt().coerceIn(0, 255), 255, 255, 255),
                                AndroidColor.TRANSPARENT
                            ),
                            floatArrayOf(0f, 1f),
                            Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawCircle(fx, fy - fr * 0.16f, fr * 0.78f, sheenPaint)
                }
            }
        }
    }
}
