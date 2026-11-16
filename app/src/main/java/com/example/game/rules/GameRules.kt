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

    fun isDirectlyExposed(grid: Array<Array<GridCell>>, r: Int, c: Int): Boolean {
        if (r < 0 || r >= GameConstants.GRID_ROWS || c < 0 || c >= GameConstants.GRID_COLS) return false
        val cell = grid[r][c]
        if (cell.kind != CellKind.BLOCK) return false
        if (cell.fromMagnet) return true

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

    fun isExposed(grid: Array<Array<GridCell>>, r: Int, c: Int): Boolean {
        if (r < 0 || r >= GameConstants.GRID_ROWS || c < 0 || c >= GameConstants.GRID_COLS) return false
        val cell = grid[r][c]
        if (isDirectlyExposed(grid, r, c)) return true

        val group = cell.linkGroup ?: return false
        if (cell.kind != CellKind.BLOCK && cell.kind != CellKind.LOCKED) return false
        for (rr in 0 until GameConstants.GRID_ROWS) {
            for (cc in 0 until GameConstants.GRID_COLS) {
                if (grid[rr][cc].linkGroup == group && isDirectlyExposed(grid, rr, cc)) {
                    return true
                }
            }
        }
        return false
    }

    fun collectLinkedBurstCells(grid: Array<Array<GridCell>>, r: Int, c: Int): List<Pair<Int, Int>> {
        if (r < 0 || r >= GameConstants.GRID_ROWS || c < 0 || c >= GameConstants.GRID_COLS) return emptyList()
        val origin = grid[r][c]
        if (origin.kind != CellKind.BLOCK && origin.kind != CellKind.LOCKED) return emptyList()
        val group = origin.linkGroup ?: return listOf(Pair(r, c))

        val burstList = mutableListOf(Pair(r, c))
        for (rr in 0 until GameConstants.GRID_ROWS) {
            for (cc in 0 until GameConstants.GRID_COLS) {
                if (rr == r && cc == c) continue
                val candidate = grid[rr][cc]
                if (candidate.linkGroup == group &&
                    (candidate.kind == CellKind.BLOCK || candidate.kind == CellKind.LOCKED) &&
                    candidate.animState != "magnet_receiving"
                ) {
                    burstList.add(Pair(rr, cc))
                }
            }
        }
        return burstList
    }

    fun updateGridAfterChange(grid: Array<Array<GridCell>>): GridUpdateResult {
        val revealed = mutableListOf<RevealedCell>()
        val dispensed = mutableListOf<DispensedCell>()
        val newlyExposed = mutableListOf<Pair<Int, Int>>()

        // 1. Check dispensers FIRST (if target slot in dispenser's facing direction is empty and counter > 0, refill it
        // immediately so adjacent LOCKED buckets stay covered/locked until the dispenser actually runs out)
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

        // 2. Reveal locked tiles adjacent to any slot that actually remains empty
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
                        cell.isMystery = false
                        cell.animState = "revealing"
                        cell.animTimer = 0f
                        cell.color?.let { revealed.add(RevealedCell(r, c, it)) }
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
        nowMs: Long,
        minDropIntervalMs: Long = GameConstants.MIN_DROP_INTERVAL_MS
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
                        if (nowMs - col.lastDropTime >= minDropIntervalMs) {
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

    fun areUpperBucketsCleared(grid: Array<Array<GridCell>>): Boolean {
        for (r in 0 until GameConstants.GRID_ROWS) {
            for (c in 0 until GameConstants.GRID_COLS) {
                val cell = grid[r][c]
                when (cell.kind) {
                    CellKind.BLOCK, CellKind.LOCKED -> return false
                    CellKind.DISPENSER -> {
                        if ((cell.counter ?: 0) > 0 || cell.queue.isNotEmpty()) {
                            return false
                        }
                    }
                    CellKind.EMPTY, CellKind.WALL -> Unit
                }
            }
        }
        return true
    }

    fun targetBeltSpeed(
        grid: Array<Array<GridCell>>,
        hasReturningBalls: Boolean = false
    ): Float {
        return if (!hasReturningBalls && areUpperBucketsCleared(grid)) {
            GameConstants.BELT_SPEED_FAST
        } else {
            GameConstants.BELT_SPEED
        }
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
        val swappedCols = mutableSetOf<Int>()
        var didSwap = false

        fun isBrickLockedOrFinishing(brick: Brick): Boolean {
            return brick.inFlight > 0 ||
                brick.filled >= GameConstants.BRICK_HOLES ||
                brick.animState == "hit_3" ||
                brick.animState == "hold_full" ||
                brick.animState == "vanish"
        }

        fun canSwapFront(col: BrickColumn): Boolean {
            if (col.bricks.size <= 1) return false
            val front = col.bricks[0]
            return front.filled == 0 && !isBrickLockedOrFinishing(front)
        }

        // 1. Same-column promotion when front is empty (filled == 0) & non-matching
        for (i in columns.indices) {
            val col = columns[i]
            if (canSwapFront(col)) {
                val topColor = col.bricks[0].color
                if (!activeColors.contains(topColor)) {
                    val matchingIdx = (1 until col.bricks.size).firstOrNull { idx ->
                        val b = col.bricks[idx]
                        !isBrickLockedOrFinishing(b) && activeColors.contains(b.color)
                    } ?: -1
                    if (matchingIdx > 0) {
                        val promoted = col.bricks.removeAt(matchingIdx)
                        col.bricks.add(0, promoted)
                        swappedCols.add(i)
                        didSwap = true
                    }
                }
            }
        }

        // 2. Cross-column rescue: if any active color is still trapped behind a bucket
        // (including behind a partially-filled front bucket), move it to an empty column
        // or swap it to the front of another column.
        if (activeColors.isNotEmpty()) {
            val currentFrontColors = columns.mapNotNull { it.bricks.firstOrNull()?.color }.toMutableSet()
            val trappedLocations = mutableListOf<Pair<Int, Int>>()
            for (cIdx in columns.indices) {
                val col = columns[cIdx]
                for (bIdx in 1 until col.bricks.size) {
                    val b = col.bricks[bIdx]
                    if (!isBrickLockedOrFinishing(b) &&
                        activeColors.contains(b.color) &&
                        !currentFrontColors.contains(b.color)
                    ) {
                        trappedLocations.add(Pair(cIdx, bIdx))
                    }
                }
            }

            for ((srcColIdx, _) in trappedLocations) {
                val srcCol = columns[srcColIdx]
                val bIdx = (1 until srcCol.bricks.size).firstOrNull { idx ->
                    val b = srcCol.bricks[idx]
                    !isBrickLockedOrFinishing(b) &&
                        activeColors.contains(b.color) &&
                        !currentFrontColors.contains(b.color)
                } ?: continue

                val emptyColIdx = columns.indices.firstOrNull { columns[it].bricks.isEmpty() }
                if (emptyColIdx != null) {
                    val rescued = srcCol.bricks.removeAt(bIdx)
                    columns[emptyColIdx].bricks.add(0, rescued)
                    currentFrontColors.add(rescued.color)
                    swappedCols.add(srcColIdx)
                    swappedCols.add(emptyColIdx)
                    didSwap = true
                    continue
                }

                // Prefer swapping with another column's non-matching filled==0 front brick
                val targetColIdx = columns.indices.firstOrNull { dstIdx ->
                    if (dstIdx == srcColIdx) return@firstOrNull false
                    val dstFront = columns[dstIdx].bricks.firstOrNull() ?: return@firstOrNull false
                    dstFront.filled == 0 && !isBrickLockedOrFinishing(dstFront) &&
                        !activeColors.contains(dstFront.color)
                } ?: columns.indices.firstOrNull { dstIdx ->
                    if (dstIdx == srcColIdx) return@firstOrNull false
                    val dstFront = columns[dstIdx].bricks.firstOrNull() ?: return@firstOrNull false
                    dstFront.filled == 0 && !isBrickLockedOrFinishing(dstFront)
                }

                if (targetColIdx != null) {
                    val dstCol = columns[targetColIdx]
                    val rescued = srcCol.bricks[bIdx]
                    val displaced = dstCol.bricks[0]
                    srcCol.bricks[bIdx] = displaced
                    dstCol.bricks[0] = rescued
                    currentFrontColors.add(rescued.color)
                    swappedCols.add(srcColIdx)
                    swappedCols.add(targetColIdx)
                    didSwap = true
                    continue
                }

                // Last resort in same column if front is not inFlight (e.g. partially filled blocking last bucket)
                val srcFront = srcCol.bricks.firstOrNull()
                if (srcFront != null && !isBrickLockedOrFinishing(srcFront)) {
                    val rescued = srcCol.bricks.removeAt(bIdx)
                    srcCol.bricks.add(0, rescued)
                    currentFrontColors.add(rescued.color)
                    swappedCols.add(srcColIdx)
                    didSwap = true
                }
            }
        }

        // 3. Fallback: rotate within multi-brick columns or swap differing front buckets
        if (!didSwap) {
            for (i in columns.indices) {
                val col = columns[i]
                if (canSwapFront(col) && !isBrickLockedOrFinishing(col.bricks[1]) && col.bricks[0].color != col.bricks[1].color) {
                    val temp = col.bricks[0]
                    col.bricks[0] = col.bricks[1]
                    col.bricks[1] = temp
                    swappedCols.add(i)
                    didSwap = true
                }
            }
        }

        if (!didSwap) {
            val swappableColIndices = columns.indices.filter { idx ->
                val front = columns[idx].bricks.firstOrNull() ?: return@filter false
                front.filled == 0 && !isBrickLockedOrFinishing(front)
            }
            if (swappableColIndices.size >= 2) {
                val firstIdx = swappableColIndices[0]
                val partnerIdx = swappableColIndices.drop(1).firstOrNull {
                    columns[it].bricks[0].color != columns[firstIdx].bricks[0].color
                }
                if (partnerIdx != null) {
                    val temp = columns[firstIdx].bricks[0]
                    columns[firstIdx].bricks[0] = columns[partnerIdx].bricks[0]
                    columns[partnerIdx].bricks[0] = temp
                    swappedCols.add(firstIdx)
                    swappedCols.add(partnerIdx)
                    didSwap = true
                }
            }
        }

        return ShuffleResult(
            shuffled = didSwap,
            swappedColumns = swappedCols.toList()
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

        // Candidate slots ordered via BFS from EXIT_ROW (row 5) so every returned container
        // stays clearable and never gets trapped behind WALLs or exhausted DISPENSERs.
        val colPriority = listOf(2, 3, 1, 4, 0, 5)
        val neighbors = arrayOf(Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1))
        val availableEmptySlots = ArrayDeque<Pair<Int, Int>>()
        val fallbackWallSlots = ArrayDeque<Pair<Int, Int>>()

        val visitedEmpty = Array(GameConstants.GRID_ROWS) { BooleanArray(GameConstants.GRID_COLS) }
        val bfsEmpty = ArrayDeque<Pair<Int, Int>>()
        for (c in colPriority) {
            val cell = grid[GameConstants.EXIT_ROW][c]
            if (cell.kind == CellKind.EMPTY || cell.kind == CellKind.BLOCK) {
                visitedEmpty[GameConstants.EXIT_ROW][c] = true
                bfsEmpty.addLast(Pair(GameConstants.EXIT_ROW, c))
            }
        }
        while (bfsEmpty.isNotEmpty()) {
            val (r, c) = bfsEmpty.removeFirst()
            if (grid[r][c].kind == CellKind.EMPTY) {
                availableEmptySlots.addLast(Pair(r, c))
            }
            for ((dr, dc) in neighbors) {
                val nr = r + dr
                val nc = c + dc
                if (nr in 0 until GameConstants.GRID_ROWS &&
                    nc in 0 until GameConstants.GRID_COLS &&
                    !visitedEmpty[nr][nc] &&
                    (grid[nr][nc].kind == CellKind.EMPTY || grid[nr][nc].kind == CellKind.BLOCK)
                ) {
                    visitedEmpty[nr][nc] = true
                    bfsEmpty.addLast(Pair(nr, nc))
                }
            }
        }

        // Fallback WALL slots also ordered outward from EXIT_ROW / reachable EMPTY cells
        val visitedWall = Array(GameConstants.GRID_ROWS) { BooleanArray(GameConstants.GRID_COLS) }
        val bfsWall = ArrayDeque<Pair<Int, Int>>()
        for (r in 0 until GameConstants.GRID_ROWS) {
            for (c in colPriority) {
                if (visitedEmpty[r][c]) {
                    visitedWall[r][c] = true
                    bfsWall.addLast(Pair(r, c))
                }
            }
        }
        for (c in colPriority) {
            if (!visitedWall[GameConstants.EXIT_ROW][c] && grid[GameConstants.EXIT_ROW][c].kind == CellKind.WALL) {
                visitedWall[GameConstants.EXIT_ROW][c] = true
                bfsWall.addLast(Pair(GameConstants.EXIT_ROW, c))
            }
        }
        while (bfsWall.isNotEmpty()) {
            val (r, c) = bfsWall.removeFirst()
            if (grid[r][c].kind == CellKind.WALL) {
                fallbackWallSlots.addLast(Pair(r, c))
            }
            for ((dr, dc) in neighbors) {
                val nr = r + dr
                val nc = c + dc
                if (nr in 1 until GameConstants.GRID_ROWS &&
                    nc in 0 until GameConstants.GRID_COLS &&
                    !visitedWall[nr][nc] &&
                    grid[nr][nc].kind == CellKind.WALL
                ) {
                    visitedWall[nr][nc] = true
                    bfsWall.addLast(Pair(nr, nc))
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
                            cell.fromMagnet = true
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
                    scaleY = 0.25f,
                    fromMagnet = true
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

    fun isChainFullAndStuck(
        beltBalls: List<BeltBall>,
        beltCapacity: Int,
        columns: List<BrickColumn>,
        droppingBallsCount: Int,
        cannonProjectilesCount: Int,
        isMagnetActive: Boolean,
        isCannonActive: Boolean
    ): Boolean {
        if (beltBalls.size < beltCapacity) return false
        if (columns.all { it.bricks.isEmpty() }) return false
        if (droppingBallsCount > 0 || cannonProjectilesCount > 0 || isMagnetActive || isCannonActive) {
            return false
        }

        for (col in columns) {
            val front = col.bricks.firstOrNull() ?: continue
            if (front.filled >= GameConstants.BRICK_HOLES ||
                front.animState == "hit_3" ||
                front.animState == "hold_full" ||
                front.animState == "vanish"
            ) {
                return false
            }
            if ((front.filled + front.inFlight) < GameConstants.BRICK_HOLES &&
                beltBalls.any { it.color == front.color }
            ) {
                return false
            }
        }
        return true
    }

    fun selectCleanUpBeltBalls(
        beltBalls: List<BeltBall>,
        count: Int = 8
    ): List<BeltBall> {
        if (beltBalls.isEmpty() || count <= 0) return emptyList()
        if (beltBalls.size <= count) return beltBalls

        val centerX = (GameConstants.BELT_LEFT_CENTER_X + GameConstants.BELT_RIGHT_CENTER_X) * 0.5f
        val topY = GameConstants.BELT_TOP_Y

        return beltBalls
            .sortedWith(
                compareBy<BeltBall> { ball ->
                    val coords = com.example.game.belt.BeltPath.getBeltCoordinates(ball.s)
                    val lanePenalty = if (!coords.isBottomLane && coords.y <= topY + 4f) 0f else 1000f
                    lanePenalty + abs(coords.x - centerX) + abs(coords.y - topY) * 3f
                }.thenBy { it.id }
            )
            .take(count)
    }

    data class CleanUpReturnItem(
        val id: Int,
        val color: MarbleColor,
        val startX: Float,
        val startY: Float,
        val isFromFunnel: Boolean
    )

    data class CleanUpBallAssignment(
        val item: CleanUpReturnItem,
        val targetRow: Int,
        val targetCol: Int,
        val targetSlotIndex: Int
    )

    fun planCleanUpReturn(
        grid: Array<Array<GridCell>>,
        items: List<CleanUpReturnItem>
    ): List<CleanUpBallAssignment> {
        if (items.isEmpty()) return emptyList()

        val assignments = mutableListOf<CleanUpBallAssignment>()

        val colPriority = listOf(2, 3, 1, 4, 0, 5)
        val neighbors = arrayOf(Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1))
        val availableEmptySlots = ArrayDeque<Pair<Int, Int>>()
        val fallbackWallSlots = ArrayDeque<Pair<Int, Int>>()

        val visitedEmpty = Array(GameConstants.GRID_ROWS) { BooleanArray(GameConstants.GRID_COLS) }
        val bfsEmpty = ArrayDeque<Pair<Int, Int>>()
        for (c in colPriority) {
            val cell = grid[GameConstants.EXIT_ROW][c]
            if (cell.kind == CellKind.EMPTY || cell.kind == CellKind.BLOCK) {
                visitedEmpty[GameConstants.EXIT_ROW][c] = true
                bfsEmpty.addLast(Pair(GameConstants.EXIT_ROW, c))
            }
        }
        while (bfsEmpty.isNotEmpty()) {
            val (r, c) = bfsEmpty.removeFirst()
            if (grid[r][c].kind == CellKind.EMPTY) {
                availableEmptySlots.addLast(Pair(r, c))
            }
            for ((dr, dc) in neighbors) {
                val nr = r + dr
                val nc = c + dc
                if (nr in 0 until GameConstants.GRID_ROWS &&
                    nc in 0 until GameConstants.GRID_COLS &&
                    !visitedEmpty[nr][nc] &&
                    (grid[nr][nc].kind == CellKind.EMPTY || grid[nr][nc].kind == CellKind.BLOCK)
                ) {
                    visitedEmpty[nr][nc] = true
                    bfsEmpty.addLast(Pair(nr, nc))
                }
            }
        }

        // Include any disconnected EMPTY cells as backup before consuming WALLs
        for (r in GameConstants.GRID_ROWS - 1 downTo 0) {
            for (c in colPriority) {
                if (!visitedEmpty[r][c] && grid[r][c].kind == CellKind.EMPTY) {
                    visitedEmpty[r][c] = true
                    availableEmptySlots.addLast(Pair(r, c))
                }
            }
        }

        // Fallback WALL slots ordered outward from reachable cells / EXIT_ROW
        val visitedWall = Array(GameConstants.GRID_ROWS) { BooleanArray(GameConstants.GRID_COLS) }
        val bfsWall = ArrayDeque<Pair<Int, Int>>()
        for (r in 0 until GameConstants.GRID_ROWS) {
            for (c in colPriority) {
                if (visitedEmpty[r][c]) {
                    visitedWall[r][c] = true
                    bfsWall.addLast(Pair(r, c))
                }
            }
        }
        for (c in colPriority) {
            if (!visitedWall[GameConstants.EXIT_ROW][c] && grid[GameConstants.EXIT_ROW][c].kind == CellKind.WALL) {
                visitedWall[GameConstants.EXIT_ROW][c] = true
                bfsWall.addLast(Pair(GameConstants.EXIT_ROW, c))
            }
        }
        while (bfsWall.isNotEmpty()) {
            val (r, c) = bfsWall.removeFirst()
            if (grid[r][c].kind == CellKind.WALL) {
                fallbackWallSlots.addLast(Pair(r, c))
            }
            for ((dr, dc) in neighbors) {
                val nr = r + dr
                val nc = c + dc
                if (nr in 0 until GameConstants.GRID_ROWS &&
                    nc in 0 until GameConstants.GRID_COLS &&
                    !visitedWall[nr][nc] &&
                    grid[nr][nc].kind == CellKind.WALL
                ) {
                    visitedWall[nr][nc] = true
                    bfsWall.addLast(Pair(nr, nc))
                }
            }
        }
        for (r in GameConstants.GRID_ROWS - 1 downTo 0) {
            for (c in colPriority) {
                if (!visitedWall[r][c] && grid[r][c].kind == CellKind.WALL) {
                    visitedWall[r][c] = true
                    fallbackWallSlots.addLast(Pair(r, c))
                }
            }
        }

        val reservedSlots = mutableMapOf<Pair<Int, Int>, Int>()
        val itemsByColor = items.groupBy { it.color }

        for ((color, colorItems) in itemsByColor) {
            var idx = 0

            // 1. Top up any exposed partial BLOCK of the same color that has < 9 balls
            for (r in GameConstants.GRID_ROWS - 1 downTo 0) {
                if (idx >= colorItems.size) break
                for (c in colPriority) {
                    if (idx >= colorItems.size) break
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
                        while (slot < GameConstants.BALLS_PER_BLOCK && idx < colorItems.size) {
                            cell.animState = "magnet_receiving"
                            cell.animTimer = 0f
                            cell.fromMagnet = true
                            assignments.add(
                                CleanUpBallAssignment(
                                    item = colorItems[idx],
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
            while (idx < colorItems.size) {
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
                    scaleY = 0.25f,
                    fromMagnet = true
                )

                var slot = 0
                while (slot < GameConstants.BALLS_PER_BLOCK && idx < colorItems.size) {
                    assignments.add(
                        CleanUpBallAssignment(
                            item = colorItems[idx],
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

    data class CleanUpPreviewCell(
        val row: Int,
        val col: Int,
        val kind: CellKind,
        val color: MarbleColor?,
        val isMystery: Boolean,
        val ballCount: Int,
        val linkGroup: Int?
    )

    data class CleanUpPreviewFunnelBall(
        val id: Int,
        val color: MarbleColor,
        val x: Float,
        val y: Float,
        val radius: Float
    )

    data class CleanUpPreviewBeltBall(
        val id: Int,
        val color: MarbleColor,
        val x: Float,
        val y: Float,
        val isHighlighted: Boolean,
        val isReturned: Boolean
    )

    data class CleanUpPreviewFlight(
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
        val delayFrac: Float
    )

    data class CleanUpPreviewSnapshot(
        val beltCapacity: Int,
        val beltScrollDistance: Float,
        val initialCells: List<CleanUpPreviewCell>,
        val targetCells: List<CleanUpPreviewCell>,
        val funnelBalls: List<CleanUpPreviewFunnelBall>,
        val beltBalls: List<CleanUpPreviewBeltBall>,
        val flights: List<CleanUpPreviewFlight>
    )

    fun buildCleanUpPreviewSnapshot(
        grid: Array<Array<GridCell>>,
        funnelBalls: List<FunnelBall>,
        beltBalls: List<BeltBall>,
        highlightedBeltIds: Set<Int>,
        beltCapacity: Int,
        beltScrollDistance: Float = 0f
    ): CleanUpPreviewSnapshot {
        val crateColNormX = floatArrayOf(0.258f, 0.500f, 0.742f)
        val crateRowNormY = floatArrayOf(0.268f, 0.488f, 0.708f)

        val clonedGrid = Array(GameConstants.GRID_ROWS) { r ->
            Array(GameConstants.GRID_COLS) { c ->
                grid[r][c].deepCopy()
            }
        }

        val initialCells = buildList {
            for (r in 0 until GameConstants.GRID_ROWS) {
                for (c in 0 until GameConstants.GRID_COLS) {
                    val cell = clonedGrid[r][c]
                    add(
                        CleanUpPreviewCell(
                            row = r,
                            col = c,
                            kind = cell.kind,
                            color = cell.color,
                            isMystery = cell.isMystery,
                            ballCount = cell.ballCount,
                            linkGroup = cell.linkGroup
                        )
                    )
                }
            }
        }

        val selectedBelt = if (highlightedBeltIds.isNotEmpty()) {
            beltBalls.filter { highlightedBeltIds.contains(it.id) }.ifEmpty {
                selectCleanUpBeltBalls(beltBalls, 8)
            }
        } else {
            selectCleanUpBeltBalls(beltBalls, 8)
        }
        val selectedIds = selectedBelt.map { it.id }.toSet()

        val cleanUpItems = buildList {
            for (fb in funnelBalls) {
                add(
                    CleanUpReturnItem(
                        id = fb.id,
                        color = fb.color,
                        startX = fb.x,
                        startY = fb.y,
                        isFromFunnel = true
                    )
                )
            }
            for (bb in selectedBelt) {
                val coords = com.example.game.belt.BeltPath.getBeltCoordinates(bb.s)
                add(
                    CleanUpReturnItem(
                        id = bb.id,
                        color = bb.color,
                        startX = coords.x,
                        startY = coords.y,
                        isFromFunnel = false
                    )
                )
            }
        }

        val assignments = planCleanUpReturn(clonedGrid, cleanUpItems)
        val returnedIds = assignments.map { it.item.id }.toSet()
        val returnedCountsPerCell = mutableMapOf<Pair<Int, Int>, Int>()

        val staggerMax = 0.22f
        val staggerStep = if (assignments.size > 1) {
            minOf(0.015f, staggerMax / (assignments.size - 1))
        } else {
            0f
        }

        val flights = assignments.mapIndexed { idx, assigned ->
            val key = Pair(assigned.targetRow, assigned.targetCol)
            returnedCountsPerCell[key] = (returnedCountsPerCell[key] ?: 0) + 1

            val cx = GameConstants.GRID_X_CENTERS[assigned.targetCol]
            val cy = GameConstants.GRID_Y_CENTERS[assigned.targetRow]
            val slotRow = assigned.targetSlotIndex / 3
            val slotCol = assigned.targetSlotIndex % 3
            val targetX = cx + (crateColNormX[slotCol] - 0.5f) * 34f
            val targetY = cy + (crateRowNormY[slotRow] - 0.5f) * 33f
            val startX = assigned.item.startX
            val startY = assigned.item.startY
            val midX = (startX + targetX) * 0.5f
            val arcSide = if (startX < targetX) -22f else 22f
            val controlX = (midX + arcSide).coerceIn(22f, 274f)
            val controlY = (startY * 0.32f + targetY * 0.68f) - 20f

            CleanUpPreviewFlight(
                id = assigned.item.id,
                color = assigned.item.color,
                startX = startX,
                startY = startY,
                controlX = controlX,
                controlY = controlY,
                targetX = targetX,
                targetY = targetY,
                targetRow = assigned.targetRow,
                targetCol = assigned.targetCol,
                targetSlotIndex = assigned.targetSlotIndex,
                delayFrac = idx * staggerStep
            )
        }

        val targetCells = buildList {
            for (r in 0 until GameConstants.GRID_ROWS) {
                for (c in 0 until GameConstants.GRID_COLS) {
                    val cell = clonedGrid[r][c]
                    val added = returnedCountsPerCell[Pair(r, c)] ?: 0
                    val finalCount = (cell.ballCount + added).coerceIn(0, GameConstants.BALLS_PER_BLOCK)
                    add(
                        CleanUpPreviewCell(
                            row = r,
                            col = c,
                            kind = cell.kind,
                            color = cell.color,
                            isMystery = cell.isMystery,
                            ballCount = finalCount,
                            linkGroup = cell.linkGroup
                        )
                    )
                }
            }
        }

        val previewFunnelBalls = funnelBalls.map { fb ->
            CleanUpPreviewFunnelBall(
                id = fb.id,
                color = fb.color,
                x = fb.x,
                y = fb.y,
                radius = fb.radius
            )
        }

        val previewBeltBalls = beltBalls.map { bb ->
            val coords = com.example.game.belt.BeltPath.getBeltCoordinates(bb.s)
            CleanUpPreviewBeltBall(
                id = bb.id,
                color = bb.color,
                x = coords.x,
                y = coords.y,
                isHighlighted = selectedIds.contains(bb.id),
                isReturned = returnedIds.contains(bb.id)
            )
        }

        return CleanUpPreviewSnapshot(
            beltCapacity = beltCapacity,
            beltScrollDistance = beltScrollDistance,
            initialCells = initialCells,
            targetCells = targetCells,
            funnelBalls = previewFunnelBalls,
            beltBalls = previewBeltBalls,
            flights = flights
        )
    }
}
