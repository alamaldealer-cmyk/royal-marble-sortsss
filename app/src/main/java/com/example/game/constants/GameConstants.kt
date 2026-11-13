package com.example.game.constants

import androidx.compose.ui.graphics.Color
import com.example.game.model.MarbleColor
import kotlin.math.PI

object GameConstants {
    const val BASE_WIDTH = 296f
    const val BASE_HEIGHT = 640f

    const val BALLS_PER_BLOCK = 9
    const val BELT_SPEED = 120f // px/s
    const val BELT_CAPACITY = 30
    const val BALL_SPACING = 447.398f / BELT_CAPACITY
    const val GRAVITY = 1000f // px/s^2 down
    const val NECK_WIDTH = 38f
    const val BRICK_HOLES = 3
    const val MAX_WAITING_BALLS = 30
    const val RESUME_WAITING_BALLS = 29

    const val BALL_RADIUS_FUNNEL = 7f
    const val BALL_RADIUS_BELT = 7.0f
    const val MIN_DROP_INTERVAL_MS = 60L

    const val GRID_COLS = 6
    const val GRID_ROWS = 6
    const val CELL_SIZE = 30f
    const val CELL_RADIUS = 7f
    const val CELL_PITCH_X = 35f
    const val CELL_PITCH_Y = 33f

    val GRID_X_CENTERS = floatArrayOf(60f, 95f, 130f, 165f, 200f, 235f)
    val GRID_Y_CENTERS = floatArrayOf(195f, 228f, 261f, 294f, 327f, 360f)
    const val EXIT_ROW = 5

    const val ARENA_SHIFT_Y = -42f

    val BRICK_COLUMNS_X = floatArrayOf(69f, 122f, 175f, 227f)
    const val BALTI_WIDTH = 42f
    const val BALTI_HEIGHT = 44f
    const val BALTI_TOP_Y = 530f
    const val BALTI_SOCKET_Y = 548f
    const val BRICK_TOP_Y = BALTI_SOCKET_Y
    const val BRICK_WIDTH = BALTI_WIDTH
    const val BRICK_TOP_HEIGHT = BALTI_HEIGHT
    const val BRICK_STACK_PITCH = 23f
    val BRICK_HOLE_OFFSETS = floatArrayOf(-11f, 0f, 11f)
    const val BRICK_HOLE_DIAMETER = 12f
    const val MATCH_X_TOLERANCE = 25f

    // Belt Stadium Path
    const val BELT_TOP_Y = 463.5f
    const val BELT_BOTTOM_Y = 484.5f
    const val BELT_LEFT_CENTER_X = 58f
    const val BELT_RIGHT_CENTER_X = 238f
    const val BELT_CENTER_RADIUS = 10.5f
    const val BELT_STRAIGHT_LENGTH = 180f
    val BELT_ARC_LENGTH = (PI * BELT_CENTER_RADIUS).toFloat() // ~32.9867f
    val BELT_TOTAL_LENGTH = 2f * BELT_STRAIGHT_LENGTH + 2f * BELT_ARC_LENGTH // ~425.973f

    data class ColorDef(
        val hex: Color,
        val dark: Color,
        val border: Color,
        val highlight: Color,
        val dot: Color
    )

    val COLOR_MAP = mapOf(
        MarbleColor.PURPLE to ColorDef(
            hex = Color(0xFFFF8FD6),
            dark = Color(0xFFD757A7),
            border = Color(0xFFAD2D7E),
            highlight = Color(0xFFFFD3F0),
            dot = Color(0xFFDB67B0)
        ),
        MarbleColor.PINK to ColorDef(
            hex = Color(0xFFFF8FD6),
            dark = Color(0xFFD757A7),
            border = Color(0xFFAD2D7E),
            highlight = Color(0xFFFFD3F0),
            dot = Color(0xFFDB67B0)
        ),
        MarbleColor.ICE to ColorDef(
            hex = Color(0xFFE6F4FA),
            dark = Color(0xFFA6C8DC),
            border = Color(0xFF7FA9C2),
            highlight = Color(0xFFFFFFFF),
            dot = Color(0xFFBFDCEB)
        ),
        MarbleColor.YELLOW to ColorDef(
            hex = Color(0xFFFFD21F),
            dark = Color(0xFFC89D00),
            border = Color(0xFF9C7800),
            highlight = Color(0xFFFFE87D),
            dot = Color(0xFFD6A600)
        ),
        MarbleColor.LIME to ColorDef(
            hex = Color(0xFFA8F028),
            dark = Color(0xFF73B80C),
            border = Color(0xFF538A04),
            highlight = Color(0xFFD9FBA4),
            dot = Color(0xFF81C914)
        ),
        MarbleColor.CYAN to ColorDef(
            hex = Color(0xFF27C4F4),
            dark = Color(0xFF0C94C0),
            border = Color(0xFF086E8F),
            highlight = Color(0xFF8CE4FD),
            dot = Color(0xFF17A3D1)
        ),
        MarbleColor.ORANGE to ColorDef(
            hex = Color(0xFFFF8A1F),
            dark = Color(0xFFCF5F00),
            border = Color(0xFF9E4400),
            highlight = Color(0xFFFFC285),
            dot = Color(0xFFDE6804)
        ),
        MarbleColor.RED to ColorDef(
            hex = Color(0xFFF23B5C),
            dark = Color(0xFFB71C38),
            border = Color(0xFF870F24),
            highlight = Color(0xFFFA8EA2),
            dot = Color(0xFFC92443)
        )
    )
}
