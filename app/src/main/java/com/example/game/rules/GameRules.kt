package com.example.game.rules

import com.example.game.constants.GameConstants
import com.example.game.model.BeltBall
import com.example.game.model.Brick
import com.example.game.model.BrickColumn
import com.example.game.model.CellKind
import com.example.game.model.DispenserDirection
import com.example.game.model.FunnelBall
import com.example.game.model.GridCell
import com.example.game.model.MarbleColor
import kotlin.math.abs

data class RevealedCell(val r: Int, val c: Int, val color: MarbleColor)
data class DispensedCell(val r: Int, val c: Int, val color: MarbleColor, val dispenserR: Int, val dispenserC: Int)
data class GridUpdateResult(
    val revealedCells: List<RevealedCell>,
    val dispensedCells: List<DispensedCell>,
    val newlyExposed: List<Pair<Int, Int>>
)

data class MatchResult(val matchedColIndex: Int, val targetHoleIndex: Int)
data class ShuffleResult(val shuffled: Boolean, val swappedColumns: List<Int>)

object GameRules {
    fun shouldBlockBlockTap(
        waitingBallsCount: Int,
        currentlyLocked: Boolean,
        maxWaitingBalls: Int = GameConstants.MAX_WAITING_BALLS,
        resumeWaitingBalls: Int = GameConstants.RESUME_WAITING_BALLS
    ): Boolean {
        if (waitingBallsCount >= maxWaitingBalls) return true
        if (currentlyLocked) {
            return waitingBallsCount > resumeWaitingBalls
        }
        return false
    }

    fun isExposed(grid: Array<Array<GridCell>>, r: Int, c: Int): Boolean {
        if (r < 0 || r >= GameConstants.GRID_ROWS || c < 0 || c >= GameConstants.GRID_COLS) return false
        val cell = grid[r][c]
        if (cell.kind != CellKind.BLOCK) return false

        // Exit row (row 5) is open to the funnel below
        if (r == GameConstants.EXIT_ROW) return true

        val neighbors = arrayOf(
            Pair(1, 0),
            Pair(-1, 0),
            Pair(0, 1),
            Pair(0, -1)
        )

        return neighbors.any { (dr, dc) ->
            val nr = r + dr
            val nc = c + dc
            if (nr in 0 until GameConstants.GRID_ROWS && nc in 0 until GameConstants.GRID_COLS) {
                grid[nr][nc].kind == CellKind.EMPTY
            } else false
        }
    }

    fun updateGridAfterChange(grid: Array<Array<GridCell>>): GridUpdateResult {
        val revealed = mutableListOf<RevealedCell>()
        val dispensed = mutableListOf<DispensedCell>()
        val newlyExposed = mutableListOf<Pair<Int, Int>>()

        // 1. Reveal locked tiles adjacent to any empty cell
        for (r in 0 until GameConstants.GRID_ROWS) {
            for (c in 0 until GameConstants.GRID_COLS) {
                val cell = grid[r][c]
                if (cell.kind == CellKind.LOCKED) {
                    val hasEmptyNeighbor = arrayOf(
                        Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1)
                    ).any { (dr, dc) ->
                        val nr = r + dr
                        val nc = c + dc
                        if (nr in 0 until GameConstants.GRID_ROWS && nc in 0 until GameConstants.GRID_COLS) {
                            grid[nr][nc].kind == CellKind.EMPTY
                        } else false
                    }

                    if (hasEmptyNeighbor) {
                        cell.kind = CellKind.BLOCK
                        cell.animState = "revealing"
                        cell.animTimer = 0f
                        cell.color?.let { revealed.add(RevealedCell(r, c, it)) }
                    }
                }
            }
        }

        // 2. Check dispensers (if target slot in dispenser's facing direction is empty and counter > 0, slide block in)
        for (r in 0 until GameConstants.GRID_ROWS) {
            for (c in 0 until GameConstants.GRID_COLS) {
                val cell = grid[r][c]
                if (cell.kind == CellKind.DISPENSER && (cell.counter ?: 0) > 0) {
                    val (targetR, targetC, startOx, startOy) = when (cell.direction) {
                        DispenserDirection.DOWN -> listOf(r + 1, c, 0, -28)
                        DispenserDirection.RIGHT -> listOf(r, c + 1, -28, 0)
                        DispenserDirection.LEFT -> listOf(r, c - 1, 28, 0)
                    }
                    if (targetR in 0 until GameConstants.GRID_ROWS &&
                        targetC in 0 until GameConstants.GRID_COLS &&
                        grid[targetR][targetC].kind == CellKind.EMPTY
                    ) {
                        val nextColor = if (cell.queue.isNotEmpty()) cell.queue.removeAt(0) else MarbleColor.PURPLE
                        cell.color = nextColor
                        cell.counter = (cell.counter ?: 1) - 1
                        cell.animState = "cave_dispensing"
                        cell.animTimer = 0f
                        grid[targetR][targetC] = GridCell(
                            kind = CellKind.BLOCK,
                            color = nextColor,
                            direction = cell.direction,
                            animState = "dispensing",
                            animTimer = 0f,
                            scaleX = 0.65f,
                            scaleY = 0.65f,
                            offsetX = startOx.toFloat(),
                            offsetY = startOy.toFloat()
                        )
                        dispensed.add(DispensedCell(targetR, targetC, nextColor, r, c))
                    }
                }
            }
        }

        // 3. Newly exposed check
        for (r in 0 until GameConstants.GRID_ROWS) {
            for (c in 0 until GameConstants.GRID_COLS) {
                if (grid[r][c].kind == CellKind.BLOCK && isExposed(grid, r, c)) {
                    newlyExposed.add(Pair(r, c))
                }
            }
        }

        return GridUpdateResult(revealed, dispensed, newlyExposed)
    }

    fun tryMatchBeltBall(
        ballX: Float,
        ballColor: MarbleColor,
        columns: List<BrickColumn>,
        nowMs: Long
    ): MatchResult? {
        for (i in columns.indices) {
            val col = columns[i]
            val colX = GameConstants.BRICK_COLUMNS_X[i]

            if (abs(ballX - colX) <= GameConstants.MATCH_X_TOLERANCE) {
                if (col.bricks.isEmpty()) continue
                val topBrick = col.bricks[0]

                if (topBrick.color == ballColor) {
                    val capacityUsed = topBrick.filled + topBrick.inFlight
                    if (capacityUsed < GameConstants.BRICK_HOLES) {
                        if (nowMs - col.lastDropTime >= GameConstants.MIN_DROP_INTERVAL_MS) {
                            col.lastDropTime = nowMs
                            topBrick.inFlight++
                            return MatchResult(matchedColIndex = i, targetHoleIndex = capacityUsed)
                        }
                    }
                }
            }
        }
        return null
    }

    fun isLevelWon(columns: List<BrickColumn>): Boolean {
        return columns.all { it.bricks.isEmpty() }
    }

    fun performShufflePowerup(
        columns: List<BrickColumn>,
        beltBalls: List<BeltBall>,
        physicsBalls: List<FunnelBall>
    ): ShuffleResult {
        val activeColors = (beltBalls.map { it.color } + physicsBalls.map { it.color }).toSet()
        val swappedCols = mutableListOf<Int>()
        var didSwap = false

        fun canSwapFront(col: BrickColumn): Boolean {
            if (col.bricks.size <= 1) return false
            val front = col.bricks[0]
            return front.filled == 0 && front.inFlight == 0 &&
                front.animState != "hit_3" && front.animState != "hold_full" && front.animState != "vanish"
        }

        for (i in columns.indices) {
            val col = columns[i]
            if (canSwapFront(col)) {
                val topColor = col.bricks[0].color
                if (!activeColors.contains(topColor)) {
                    val matchingIdx = col.bricks.indexOfFirst { it != col.bricks[0] && activeColors.contains(it.color) }
                    if (matchingIdx > 0) {
                        val promoted = col.bricks.removeAt(matchingIdx)
                        col.bricks.add(0, promoted)
                        swappedCols.add(i)
                        didSwap = true
                    }
                }
            }
        }

        if (!didSwap) {
            for (i in columns.indices) {
                val col = columns[i]
                if (canSwapFront(col)) {
                    val temp = col.bricks[0]
                    col.bricks[0] = col.bricks[1]
                    col.bricks[1] = temp
                    swappedCols.add(i)
                    didSwap = true
                }
            }
        }

        return ShuffleResult(
            shuffled = didSwap || columns.any { it.bricks.isNotEmpty() },
            swappedColumns = if (swappedCols.isNotEmpty()) swappedCols else listOf(0, 1, 2, 3).take(columns.size)
        )
    }

    data class MagnetBallAssignment(
        val beltBall: BeltBall,
        val targetRow: Int,
        val targetCol: Int,
        val targetSlotIndex: Int
    )

    fun planMagnetReturn(
        grid: Array<Array<GridCell>>,
        beltBalls: List<BeltBall>
    ): List<MagnetBallAssignment> {
        if (beltBalls.isEmpty()) return emptyList()

        val assignments = mutableListOf<MagnetBallAssignment>()

        // Candidate slots ordered bottom-to-top, center-out so returned containers are exposed & accessible
        val colPriority = listOf(2, 3, 1, 4, 0, 5)
        val availableEmptySlots = ArrayDeque<Pair<Int, Int>>()
        val fallbackWallSlots = ArrayDeque<Pair<Int, Int>>()
        for (r in GameConstants.GRID_ROWS - 1 downTo 0) {
            for (c in colPriority) {
                when (grid[r][c].kind) {
                    CellKind.EMPTY -> availableEmptySlots.addLast(Pair(r, c))
                    CellKind.WALL -> if (r >= 1) fallbackWallSlots.addLast(Pair(r, c))
                    else -> Unit
                }
            }
        }

        // Track reserved slot count on each container so multiple balls fill slots 0..8 cleanly
        val reservedSlots = mutableMapOf<Pair<Int, Int>, Int>()
        val ballsByColor = beltBalls.groupBy { it.color }

        for ((color, colorBalls) in ballsByColor) {
            var idx = 0

            // 1. First top up any exposed partial BLOCK of the same color that has < 9 balls
            for (r in GameConstants.GRID_ROWS - 1 downTo 0) {
                if (idx >= colorBalls.size) break
                for (c in colPriority) {
                    if (idx >= colorBalls.size) break
                    val cell = grid[r][c]
                    if (cell.kind == CellKind.BLOCK &&
                        cell.color == color &&
                        !cell.isMystery &&
                        cell.animState != "magnet_receiving" &&
                        isExposed(grid, r, c)
                    ) {
                        val pos = Pair(r, c)
                        val alreadyUsed = reservedSlots[pos] ?: cell.ballCount.coerceIn(0, GameConstants.BALLS_PER_BLOCK)
                        var slot = alreadyUsed
                        while (slot < GameConstants.BALLS_PER_BLOCK && idx < colorBalls.size) {
                            cell.animState = "magnet_receiving"
                            cell.animTimer = 0f
                            assignments.add(
                                MagnetBallAssignment(
                                    beltBall = colorBalls[idx],
                                    targetRow = r,
                                    targetCol = c,
                                    targetSlotIndex = slot
                                )
                            )
                            slot++
                            idx++
                        }
                        reservedSlots[pos] = slot
                    }
                }
            }

            // 2. Place remaining balls of this color into new upper container(s) (up to 9 per container)
            while (idx < colorBalls.size) {
                val targetPos = if (availableEmptySlots.isNotEmpty()) {
                    availableEmptySlots.removeFirst()
                } else if (fallbackWallSlots.isNotEmpty()) {
                    fallbackWallSlots.removeFirst()
                } else {
                    break
                }

                val r = targetPos.first
                val c = targetPos.second
                grid[r][c] = GridCell(
                    kind = CellKind.BLOCK,
                    color = color,
                    ballCount = 0,
                    animState = "magnet_receiving",
                    animTimer = 0f,
                    scaleX = 0.25f,
                    scaleY = 0.25f
                )

                var slot = 0
                while (slot < GameConstants.BALLS_PER_BLOCK && idx < colorBalls.size) {
                    assignments.add(
                        MagnetBallAssignment(
                            beltBall = colorBalls[idx],
                            targetRow = r,
                            targetCol = c,
                            targetSlotIndex = slot
                        )
                    )
                    slot++
                    idx++
                }
                reservedSlots[targetPos] = slot
            }
        }

        return assignments
    }

    data class CannonTargetAssignment(
        val beltBall: BeltBall,
        val targetCol: Int,
        val targetBrick: Brick?
    )

    fun planCannonBlast(
        columns: List<BrickColumn>,
        beltBalls: List<BeltBall>
    ): List<CannonTargetAssignment> {
        if (beltBalls.isEmpty()) return emptyList()

        val assignments = mutableListOf<CannonTargetAssignment>()

        for (ball in beltBalls) {
            var chosenCol = -1
            var chosenBrick: Brick? = null

            // 1. Prioritize matching front active buckets (index 0) with free capacity
            var bestFrontScore = -1
            for (cIdx in columns.indices) {
                val front = columns[cIdx].bricks.firstOrNull() ?: continue
                if (front.color == ball.color &&
                    front.animState != "vanish" &&
                    front.animState != "hold_full" &&
                    (front.filled + front.inFlight) < GameConstants.BRICK_HOLES
                ) {
                    val fillScore = front.filled + front.inFlight
                    if (fillScore > bestFrontScore) {
                        bestFrontScore = fillScore
                        chosenCol = cIdx
                        chosenBrick = front
                    }
                }
            }

            // 2. If no front bucket matches or has room, target matching buckets behind in any column!
            if (chosenBrick == null) {
                val maxDepth = columns.maxOfOrNull { it.bricks.size } ?: 0
                outer@ for (depth in 1 until maxDepth) {
                    var bestBehindScore = -1
                    for (cIdx in columns.indices) {
                        val brick = columns[cIdx].bricks.getOrNull(depth) ?: continue
                        if (brick.color == ball.color &&
                            brick.animState != "vanish" &&
                            brick.animState != "hold_full" &&
                            (brick.filled + brick.inFlight) < GameConstants.BRICK_HOLES
                        ) {
                            val fillScore = brick.filled + brick.inFlight
                            if (fillScore > bestBehindScore) {
                                bestBehindScore = fillScore
                                chosenCol = cIdx
                                chosenBrick = brick
                            }
                        }
                    }
                    if (chosenBrick != null) break@outer
                }
            }

            if (chosenBrick != null) {
                chosenBrick.inFlight++
            }
            assignments.add(
                CannonTargetAssignment(
                    beltBall = ball,
                    targetCol = chosenCol,
                    targetBrick = chosenBrick
                )
            )
        }

        return assignments
    }
}
