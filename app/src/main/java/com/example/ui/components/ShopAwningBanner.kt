package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun ShopAwningBanner(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .testTag("shop_awning_banner")
            .semantics { contentDescription = "Shop Awning" }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val stripeCount = 7
            val stripeW = w / stripeCount
            val scallopHeight = (stripeW * 0.38f).coerceIn(14.dp.toPx(), h * 0.24f)
            val bodyBottomY = h - scallopHeight - 5.dp.toPx()
            val lipOffsetY = 4.5.dp.toPx()
            val outlineWidth = 1.8.dp.toPx()

            // 1. Soft drop shadow beneath the awning onto the dark royal blue shop background
            val shadowPath = Path().apply {
                moveTo(0f, 0f)
                lineTo(w, 0f)
                lineTo(w, bodyBottomY + 6.dp.toPx())
                for (i in stripeCount - 1 downTo 0) {
                    val leftX = i * stripeW
                    val rightX = if (i == stripeCount - 1) w else leftX + stripeW
                    cubicTo(
                        rightX, bodyBottomY + scallopHeight + 7.dp.toPx(),
                        leftX, bodyBottomY + scallopHeight + 7.dp.toPx(),
                        leftX, bodyBottomY + 6.dp.toPx()
                    )
                }
                close()
            }
            drawPath(
                path = shadowPath,
                color = Color(0x6600081F)
            )

            // 2. Alternating HD Red & White Stripes + 3D Scalloped Lip & Curved Tabs
            for (i in 0 until stripeCount) {
                val isRed = i % 2 == 0
                val leftX = i * stripeW
                val rightX = if (i == stripeCount - 1) w else (i + 1) * stripeW
                val segW = rightX - leftX

                val panelPath = Path().apply {
                    moveTo(leftX, 0f)
                    lineTo(rightX, 0f)
                    lineTo(rightX, bodyBottomY)
                    cubicTo(
                        rightX, bodyBottomY + scallopHeight * 1.15f,
                        leftX, bodyBottomY + scallopHeight * 1.15f,
                        leftX, bodyBottomY
                    )
                    close()
                }

                // 2a. 3D underside lip (folded edge at bottom of each scallop)
                val lipPath = Path().apply {
                    moveTo(leftX, bodyBottomY - 1f)
                    lineTo(rightX, bodyBottomY - 1f)
                    lineTo(rightX, bodyBottomY + lipOffsetY)
                    cubicTo(
                        rightX, bodyBottomY + scallopHeight * 1.15f + lipOffsetY,
                        leftX, bodyBottomY + scallopHeight * 1.15f + lipOffsetY,
                        leftX, bodyBottomY + lipOffsetY
                    )
                    close()
                }
                drawPath(
                    path = lipPath,
                    color = if (isRed) Color(0xFF7E0716) else Color(0xFF9EB0CC)
                )
                drawPath(
                    path = lipPath,
                    color = if (isRed) Color(0xFF4D030C) else Color(0xFF5D6F8E),
                    style = Stroke(width = outlineWidth, join = StrokeJoin.Round)
                )

                // 2b. Main glossy fabric vertical gradient
                val mainBrush = if (isRed) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFF3A4D),
                            Color(0xFFE9162B),
                            Color(0xFFC80C22),
                            Color(0xFFA9081B)
                        ),
                        startY = 0f,
                        endY = bodyBottomY + scallopHeight
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFFFFF),
                            Color(0xFFF9FBFF),
                            Color(0xFFEAF0FA),
                            Color(0xFFD7E1F2)
                        ),
                        startY = 0f,
                        endY = bodyBottomY + scallopHeight
                    )
                }
                drawPath(path = panelPath, brush = mainBrush)

                // 2c. Cylindrical highlight down the center of each stripe
                drawPath(
                    path = panelPath,
                    brush = Brush.horizontalGradient(
                        colors = if (isRed) {
                            listOf(
                                Color(0x28000000),
                                Color(0x26FFFFFF),
                                Color(0x28000000)
                            )
                        } else {
                            listOf(
                                Color(0x185B7399),
                                Color(0x33FFFFFF),
                                Color(0x185B7399)
                            )
                        },
                        startX = leftX,
                        endX = rightX
                    )
                )

                // 2d. Glossy bottom-rim highlight on each scallop curve
                val rimHighlightPath = Path().apply {
                    moveTo(leftX + segW * 0.14f, bodyBottomY + scallopHeight * 0.18f)
                    cubicTo(
                        leftX + segW * 0.25f, bodyBottomY + scallopHeight * 0.88f,
                        rightX - segW * 0.25f, bodyBottomY + scallopHeight * 0.88f,
                        rightX - segW * 0.14f, bodyBottomY + scallopHeight * 0.18f
                    )
                }
                drawPath(
                    path = rimHighlightPath,
                    color = if (isRed) Color(0x55FF9CA6) else Color(0xCCFFFFFF),
                    style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                )

                // 2e. Subtle vertical seam line between panels
                if (i > 0) {
                    drawLine(
                        color = Color(0x4D2A050D),
                        start = Offset(leftX, 0f),
                        end = Offset(leftX, bodyBottomY),
                        strokeWidth = 1.4.dp.toPx()
                    )
                }
            }

            // 3. Top glossy sheen band across the awning canopy
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x40FFFFFF),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = h * 0.34f
                ),
                topLeft = Offset.Zero,
                size = Size(w, h * 0.34f)
            )

            // 4. HD Fluffy Snow Cap & Icicles along the top rim
            val snowBaseY = h * 0.13f
            val snowStep = 9.dp.toPx()
            val snowWavePoints = listOf(
                0.00f to 1.0f,
                0.08f to 2.1f,
                0.16f to 1.4f,
                0.25f to 0.8f,
                0.34f to 1.8f,
                0.44f to 1.2f,
                0.53f to 0.8f,
                0.62f to 1.5f,
                0.71f to 0.7f,
                0.80f to 1.3f,
                0.88f to 0.9f,
                0.94f to 1.9f,
                1.00f to 1.0f
            )

            val snowWaveOutline = Path().apply {
                moveTo(0f, snowBaseY + snowWavePoints.first().second * snowStep)
                for (p in 0 until snowWavePoints.size - 1) {
                    val (x1f, d1) = snowWavePoints[p]
                    val (x2f, d2) = snowWavePoints[p + 1]
                    val x1 = x1f * w
                    val y1 = snowBaseY + d1 * snowStep
                    val x2 = x2f * w
                    val y2 = snowBaseY + d2 * snowStep
                    val midX = (x1 + x2) * 0.5f
                    val ctrlY = maxOf(y1, y2) + 4.dp.toPx()
                    quadraticTo(midX, ctrlY, x2, y2)
                }
            }

            val snowPath = Path().apply {
                addPath(snowWaveOutline)
                lineTo(w, 0f)
                lineTo(0f, 0f)
                close()
            }

            // Soft shadow under snow rim
            drawPath(
                path = snowWaveOutline,
                color = Color(0x36162D59),
                style = Stroke(width = 4.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Snow body with icy sky-blue gradient shading so it pops cleanly over white stripes too
            drawPath(
                path = snowPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFF0F7FF),
                        Color(0xFFCDE5FF),
                        Color(0xFF9AC8FF)
                    ),
                    startY = 0f,
                    endY = snowBaseY + 22.dp.toPx()
                )
            )

            // Crisp icy-blue cartoon outline along bottom wave of snow
            drawPath(
                path = snowWaveOutline,
                color = Color(0xFF5689C9),
                style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // 5. Crisp snow dots on the red panels
            val dots = listOf(
                Triple(0.06f, 0.56f, 3.2f),
                Triple(0.11f, 0.72f, 2.0f),
                Triple(0.35f, 0.62f, 2.8f),
                Triple(0.64f, 0.50f, 2.2f),
                Triple(0.92f, 0.66f, 3.0f),
                Triple(0.89f, 0.45f, 1.9f)
            )
            for ((xf, yf, rDp) in dots) {
                val cx = xf * w
                val cy = yf * h
                val r = rDp.dp.toPx()
                drawCircle(
                    color = Color(0x331B3668),
                    radius = r + 0.8.dp.toPx(),
                    center = Offset(cx, cy + 1.dp.toPx())
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, Color(0xFFD0E7FF)),
                        center = Offset(cx - r * 0.25f, cy - r * 0.25f),
                        radius = r * 1.4f
                    ),
                    radius = r,
                    center = Offset(cx, cy)
                )
            }
        }
    }
}
