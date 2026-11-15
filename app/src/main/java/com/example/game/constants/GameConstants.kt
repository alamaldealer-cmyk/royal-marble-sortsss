package com.example.game.constants

import androidx.compose.ui.graphics.Color
import com.example.game.model.MarbleColor
import kotlin.math.PI

object GameConstants {
    const val BASE_WIDTH = 296f
    const val BASE_HEIGHT = 640f

    const val BALLS_PER_BLOCK = 9
    const val BELT_SPEED = 120f // px/s
    const val BELT_CAPACITY = 24
    const val UPGRADED_BELT_CAPACITY = 30
    const val GRAVITY = 1000f // px/s^2 down
    const val NECK_WIDTH = 38f
    const val BRICK_HOLES = 3
    const val MAX_WAITING_BALLS = 24
    const val RESUME_WAITING_BALLS = 23
    const val UPGRADED_MAX_WAITING_BALLS = 30
    const val UPGRADED_RESUME_WAITING_BALLS = 29
    const val CAPACITY_UPGRADE_AFTER_LEVEL = 15

    fun beltCapacityForLevel(levelNumber: Int): Int {
        return if (levelNumber > CAPACITY_UPGRADE_AFTER_LEVEL) UPGRADED_BELT_CAPACITY else BELT_CAPACITY
    }

    fun maxWaitingBallsForLevel(levelNumber: Int): Int {
        return if (levelNumber > CAPACITY_UPGRADE_AFTER_LEVEL) UPGRADED_MAX_WAITING_BALLS else MAX_WAITING_BALLS
    }

    fun resumeWaitingBallsForLevel(levelNumber: Int): Int {
        return if (levelNumber > CAPACITY_UPGRADE_AFTER_LEVEL) UPGRADED_RESUME_WAITING_BALLS else RESUME_WAITING_BALLS
    }

    const val BALL_RADIUS_FUNNEL = 7.9f
    const val BALL_RADIUS_BELT = 8.0f
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
    const val BELT_TOP_Y = 462.5f
    const val BELT_BOTTOM_Y = 485.5f
    const val BELT_LEFT_CENTER_X = 58f
    const val BELT_RIGHT_CENTER_X = 238f
    const val BELT_CENTER_RADIUS = 11.5f
    const val BELT_STRAIGHT_LENGTH = 180f
    val BELT_ARC_LENGTH = (PI * BELT_CENTER_RADIUS).toFloat() // ~36.1283f
    val BELT_TOTAL_LENGTH = 2f * BELT_STRAIGHT_LENGTH + 2f * BELT_ARC_LENGTH // ~432.2566f
    val BALL_SPACING = BELT_TOTAL_LENGTH / BELT_CAPACITY // ~18.0107f

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
        ),
        MarbleColor.BLACK to ColorDef(
            hex = Color(0xFF3A3D45),
            dark = Color(0xFF1D1F24),
            border = Color(0xFF0F1014),
            highlight = Color(0xFF727785),
            dot = Color(0xFF2A2C33)
        ),
        MarbleColor.DARK_GREEN to ColorDef(
            hex = Color(0xFF129E1E),
            dark = Color(0xFF0A6A11),
            border = Color(0xFF054709),
            highlight = Color(0xFF55D95E),
            dot = Color(0xFF0D8017)
        ),
        MarbleColor.BLUE to ColorDef(
            hex = Color(0xFF0E54F0),
            dark = Color(0xFF0737AB),
            border = Color(0xFF042375),
            highlight = Color(0xFF6299FF),
            dot = Color(0xFF0A44C9)
        ),
        MarbleColor.BROWN to ColorDef(
            hex = Color(0xFFB5621E),
            dark = Color(0xFF7C3F0E),
            border = Color(0xFF522705),
            highlight = Color(0xFFE09654),
            dot = Color(0xFF995015)
        ),
        MarbleColor.MINT to ColorDef(
            hex = Color(0xFF25E693),
            dark = Color(0xFF10AD6B),
            border = Color(0xFF087849),
            highlight = Color(0xFF85FFD0),
            dot = Color(0xFF18C97F)
        ),
        MarbleColor.WHITE to ColorDef(
            hex = Color(0xFFF3F7FC),
            dark = Color(0xFFC4D0DF),
            border = Color(0xFF98A7BA),
            highlight = Color(0xFFFFFFFF),
            dot = Color(0xFFDAE3EE)
        ),
        MarbleColor.MAROON to ColorDef(
            hex = Color(0xFFAD0E32),
            dark = Color(0xFF75061F),
            border = Color(0xFF4A0212),
            highlight = Color(0xFFE25B79),
            dot = Color(0xFF8F0A28)
        ),
        MarbleColor.LIGHT_PINK to ColorDef(
            hex = Color(0xFFFF69C6),
            dark = Color(0xFFDB3A9E),
            border = Color(0xFFA82076),
            highlight = Color(0xFFFFB0E2),
            dot = Color(0xFFEE50B2)
        )
    )

    fun colorDisplayName(color: MarbleColor): String = when (color) {
        MarbleColor.PURPLE -> "Purple"
        MarbleColor.PINK -> "Hot Pink"
        MarbleColor.ICE -> "Ice Blue"
        MarbleColor.YELLOW -> "Yellow"
        MarbleColor.LIME -> "Lime Green"
        MarbleColor.CYAN -> "Cyan"
        MarbleColor.ORANGE -> "Orange"
        MarbleColor.RED -> "Red"
        MarbleColor.BLACK -> "Black"
        MarbleColor.DARK_GREEN -> "Dark Green"
        MarbleColor.BLUE -> "Royal Blue"
        MarbleColor.BROWN -> "Brown"
        MarbleColor.MINT -> "Mint Green"
        MarbleColor.WHITE -> "White"
        MarbleColor.MAROON -> "Maroon"
        MarbleColor.LIGHT_PINK -> "Baby Pink"
    }
}
