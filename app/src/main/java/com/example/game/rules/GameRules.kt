package com.example.game.rules

import com.example.game.constants.GameConstants
import com.example.game.model.BeltBall
import com.example.game.model.BrickColumn
import com.example.game.model.CellKind
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
    fun shouldBlockBlockTap(waitingBallsCount: Int, currentlyLocked: Boolean): Boolean {
        if (waitingBallsCount >= GameConstants.MAX_WAITING_BALLS) return true
        if (currentlyLocked) {
            return waitingBallsCount > GameConstants.RESUME_WAITING_BALLS
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

        // 2. Check dispensers (if empty below and counter > 0, slide block in)
        for (r in 0 until GameConstants.GRID_ROWS) {
            for (c in 0 until GameConstants.GRID_COLS) {
                val cell = grid[r][c]
                if (cell.kind == CellKind.DISPENSER && (cell.counter ?: 0) > 0) {
                    val belowR = r + 1
                    if (belowR < GameConstants.GRID_ROWS && grid[belowR][c].kind == CellKind.EMPTY) {
                        val nextColor = if (cell.queue.isNotEmpty()) cell.queue.removeAt(0) else MarbleColor.PURPLE
                        cell.counter = (cell.counter ?: 1) - 1
                        cell.animState = "cave_dispensing"
                        cell.animTimer = 0f
                        grid[belowR][c] = GridCell(
                            kind = CellKind.BLOCK,
                            color = nextColor,
                            animState = "dispensing",
                            animTimer = 0f,
                            offsetY = -15f
                        )
                        dispensed.add(DispensedCell(belowR, c, nextColor, r, c))
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
}
