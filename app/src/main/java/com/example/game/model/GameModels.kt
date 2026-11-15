package com.example.game.model

enum class MarbleColor(val key: String) {
    PURPLE("purple"),
    PINK("pink"),
    ICE("ice"),
    YELLOW("yellow"),
    LIME("lime"),
    CYAN("cyan"),
    ORANGE("orange"),
    RED("red"),
    BLACK("black"),
    DARK_GREEN("dark_green"),
    BLUE("blue"),
    BROWN("brown"),
    MINT("mint"),
    WHITE("white"),
    MAROON("maroon"),
    LIGHT_PINK("light_pink");

    companion object {
        fun fromKey(key: String): MarbleColor {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: PURPLE
        }
    }
}

enum class CellKind {
    EMPTY,
    BLOCK,
    LOCKED,
    DISPENSER,
    WALL
}

enum class DispenserDirection {
    DOWN,
    RIGHT,
    LEFT
}

data class GridCell(
    var kind: CellKind = CellKind.WALL,
    var color: MarbleColor? = null,
    var isMystery: Boolean = false,
    var counter: Int? = null,
    val queue: MutableList<MarbleColor> = mutableListOf(),
    var direction: DispenserDirection = DispenserDirection.DOWN,
    var animState: String = "idle", // idle, pressed, bursting, revealing, dispensing, popBounced, blocked, emptying, cave_dispensing
    var animTimer: Float = 0f,
    var burstSpawned: Boolean = false,
    var spawnedCount: Int = 0,
    var scaleX: Float = 1f,
    var scaleY: Float = 1f,
    var offsetX: Float = 0f,
    var offsetY: Float = 0f,
    var ballCount: Int = 9
) {
    fun deepCopy(): GridCell {
        return GridCell(
            kind = kind,
            color = color,
            isMystery = isMystery,
            counter = counter,
            queue = queue.toMutableList(),
            direction = direction,
            animState = animState,
            animTimer = animTimer,
            burstSpawned = burstSpawned,
            spawnedCount = spawnedCount,
            scaleX = scaleX,
            scaleY = scaleY,
            offsetX = offsetX,
            offsetY = offsetY,
            ballCount = ballCount
        )
    }
}

data class Brick(
    val color: MarbleColor,
    var filled: Int = 0, // 0..3
    var inFlight: Int = 0,
    var animState: String = "idle", // idle, activate, hit_1, hit_2, hit_3, hold_full, vanish, completing
    var animTimer: Float = 0f
)

data class BrickColumn(
    val x: Float,
    val bricks: MutableList<Brick> = mutableListOf(),
    var lastDropTime: Long = 0L,
    var shiftOffsetY: Float = 0f,
    var enterTimer: Float = 0f,
    var lightPulseTimer: Float = 0f
)

data class BeltBall(
    val id: Int,
    val color: MarbleColor,
    var slotIndex: Int,
    var s: Float,
    var scale: Float = 1f,
    var mood: String = "content", // content, wink, happy_open, curious
    var animTimer: Float = 0f
)

data class FunnelBall(
    val id: Int,
    val color: MarbleColor,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var radius: Float = 7f,
    var waitTime: Float = 0f,
    var mood: String = "surprised", // surprised, happy_open, angry_1, angry_2, content
    var animTimer: Float = 0f
)

data class DroppingBall(
    val id: Int,
    val color: MarbleColor,
    val startX: Float,
    val startY: Float,
    val targetX: Float,
    val targetY: Float,
    var progress: Float = 0f,
    val duration: Float = 0.18f,
    val colIndex: Int,
    val holeIndex: Int,
    var mood: String = "happy_open"
)

enum class ParticleShape {
    STAR,
    CIRCLE,
    SPARKLE
}

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Long,
    val size: Float,
    var alpha: Float = 1f,
    var life: Float = 0f,
    val maxLife: Float = 0.5f,
    val shape: ParticleShape = ParticleShape.SPARKLE
)

data class FluffyVanishEffect(
    val id: Int,
    val color: MarbleColor,
    val startX: Float,
    val startY: Float,
    val vx: Float,
    val vy: Float,
    var x: Float,
    var y: Float,
    var scale: Float = 1f,
    var alpha: Float = 1f,
    var progress: Float = 0f,
    val duration: Float = 0.45f
)

data class MagnetReturningBall(
    val id: Int,
    val color: MarbleColor,
    val startX: Float,
    val startY: Float,
    val controlX: Float,
    val controlY: Float,
    val targetX: Float,
    val targetY: Float,
    val targetRow: Int,
    val targetCol: Int,
    val targetSlotIndex: Int,
    var elapsed: Float = 0f,
    val delay: Float = 0f,
    val duration: Float = 0.46f,
    var landed: Boolean = false
)

data class CannonCollectedBall(
    val id: Int,
    val color: MarbleColor,
    val startX: Float,
    val startY: Float,
    val targetCol: Int,
    val targetBrick: Brick?,
    val delay: Float,
    val swirlSeed: Float
)

data class CannonProjectile(
    val id: Int,
    val color: MarbleColor,
    val startX: Float,
    val startY: Float,
    val controlX: Float,
    val controlY: Float,
    val targetX: Float,
    val targetY: Float,
    val targetCol: Int,
    val targetBrick: Brick?,
    var elapsed: Float = 0f,
    val delay: Float = 0f,
    val duration: Float = 0.34f,
    var landed: Boolean = false
)

data class CannonSmokePuff(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val baseScale: Float,
    var life: Float = 0f,
    val maxLife: Float = 0.72f
)

data class LevelData(
    val levelNumber: Int,
    val grid: Array<Array<GridCell>>,
    val columns: List<List<MarbleColor>>,
    val ballsPerBlock: Int = 9
)
