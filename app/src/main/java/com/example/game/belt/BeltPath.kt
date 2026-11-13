package com.example.game.belt

import com.example.game.constants.GameConstants
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

object BeltPath {
    val STRAIGHT_LEN = GameConstants.BELT_STRAIGHT_LENGTH // 180f
    val ARC_LEN = (PI * GameConstants.BELT_CENTER_RADIUS).toFloat() // ~32.9867f
    val TOTAL_PATH_LEN = 2f * STRAIGHT_LEN + 2f * ARC_LEN // ~425.9734f

    val SEG0_END = STRAIGHT_LEN // 180f
    val SEG1_END = SEG0_END + ARC_LEN // ~212.9867f
    val SEG2_END = SEG1_END + STRAIGHT_LEN // ~392.9867f
    val SEG3_END = TOTAL_PATH_LEN // ~425.9734f

    const val FEED_POINT_S = 238f - 148f // 90.0f

    data class BeltCoord(val x: Float, val y: Float, val isBottomLane: Boolean)

    fun getBeltCoordinates(rawS: Float): BeltCoord {
        var s = rawS % TOTAL_PATH_LEN
        if (s < 0) s += TOTAL_PATH_LEN

        val centerY = (GameConstants.BELT_TOP_Y + GameConstants.BELT_BOTTOM_Y) / 2f

        // Segment 0: Top lane (x goes 238 -> 58, y = BELT_TOP_Y)
        if (s < SEG0_END) {
            val x = GameConstants.BELT_RIGHT_CENTER_X - s
            return BeltCoord(x, GameConstants.BELT_TOP_Y, false)
        }

        // Segment 1: Left U-turn around (58, centerY)
        if (s < SEG1_END) {
            val u = (s - SEG0_END) / ARC_LEN
            val angle = -PI / 2.0 - u * PI
            val x = GameConstants.BELT_LEFT_CENTER_X + (GameConstants.BELT_CENTER_RADIUS * cos(angle)).toFloat()
            val y = centerY + (GameConstants.BELT_CENTER_RADIUS * sin(angle)).toFloat()
            return BeltCoord(x, y, false)
        }

        // Segment 2: Bottom lane (x goes 58 -> 238, y = BELT_BOTTOM_Y)
        if (s < SEG2_END) {
            val d = s - SEG1_END
            val x = GameConstants.BELT_LEFT_CENTER_X + d
            return BeltCoord(x, GameConstants.BELT_BOTTOM_Y, true)
        }

        // Segment 3: Right U-turn around (238, centerY)
        val u = (s - SEG2_END) / ARC_LEN
        val angle = PI / 2.0 - u * PI
        val x = GameConstants.BELT_RIGHT_CENTER_X + (GameConstants.BELT_CENTER_RADIUS * cos(angle)).toFloat()
        val y = centerY + (GameConstants.BELT_CENTER_RADIUS * sin(angle)).toFloat()
        return BeltCoord(x, y, false)
    }

    fun isSOnBottomLane(rawS: Float): Boolean {
        var s = rawS % TOTAL_PATH_LEN
        if (s < 0) s += TOTAL_PATH_LEN
        return s in SEG1_END..SEG2_END
    }

    fun circularDistance(s1: Float, s2: Float): Float {
        var m1 = s1 % TOTAL_PATH_LEN
        if (m1 < 0) m1 += TOTAL_PATH_LEN
        var m2 = s2 % TOTAL_PATH_LEN
        if (m2 < 0) m2 += TOTAL_PATH_LEN
        val diff = abs(m1 - m2)
        return min(diff, TOTAL_PATH_LEN - diff)
    }
}
