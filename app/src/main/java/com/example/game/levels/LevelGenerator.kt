package com.example.game.levels

import com.example.game.constants.GameConstants
import com.example.game.model.CellKind
import com.example.game.model.GridCell
import com.example.game.model.LevelData
import com.example.game.model.MarbleColor
import kotlin.random.Random

object LevelGenerator {

    /**
     * Mathematical Guarantee:
     * Each block/crate on the grid contains 9 balls.
     * Each bucket holds 3 balls.
     * Therefore, for EVERY color C:
     * Number_of_Buckets(C) MUST EXACTLY EQUAL Number_of_Blocks(C) * 3
     * This guarantees 0 leftover empty buckets and 0 stranded balls across all levels!
     */
    fun buildBalancedColumns(
        grid: Array<Array<GridCell>>,
        preferredFrontColors: List<MarbleColor> = emptyList(),
        seed: Long = 42L
    ): List<List<MarbleColor>> {
        val colorCounts = mutableMapOf<MarbleColor, Int>()
        MarbleColor.entries.forEach { colorCounts[it] = 0 }

        for (r in 0 until GameConstants.GRID_ROWS) {
            for (c in 0 until GameConstants.GRID_COLS) {
                val cell = grid[r][c]
                if (cell.kind == CellKind.BLOCK || cell.kind == CellKind.LOCKED) {
                    cell.color?.let { colorCounts[it] = (colorCounts[it] ?: 0) + 1 }
                } else if (cell.kind == CellKind.DISPENSER) {
                    cell.queue.forEach { qColor ->
                        colorCounts[qColor] = (colorCounts[qColor] ?: 0) + 1
                    }
                }
            }
        }

        val brickList = mutableListOf<MarbleColor>()
        for ((color, count) in colorCounts) {
            val numBuckets = count * (GameConstants.BALLS_PER_BLOCK / GameConstants.BRICK_HOLES) // count * 3
            for (i in 0 until numBuckets) {
                brickList.add(color)
            }
        }

        brickList.shuffle(Random(seed))

        // Ensure preferred colors (e.g. PURPLE) appear at front buckets
        for (prefColor in preferredFrontColors.reversed()) {
            val idx = brickList.indexOf(prefColor)
            if (idx > 0) {
                brickList.removeAt(idx)
                brickList.add(0, prefColor)
            }
        }

        val columns = listOf(
            mutableListOf<MarbleColor>(),
            mutableListOf<MarbleColor>(),
            mutableListOf<MarbleColor>(),
            mutableListOf<MarbleColor>()
        )

        brickList.forEachIndexed { idx, color ->
            columns[idx % 4].add(color)
        }

        return columns
    }

    fun getLevel1(): LevelData {
        val grid = Array(GameConstants.GRID_ROWS) {
            Array(GameConstants.GRID_COLS) { GridCell(kind = CellKind.WALL) }
        }

        // Top blocks with PURPLE, PINK, CYAN, ORANGE
        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PINK)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)

        // Exposed exit row with ICE (White), LIME, YELLOW, RED - all 8 colors present!
        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.ICE)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIME)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.YELLOW)
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.RED)

        // Dynamically calculate exact 1:3 bucket ratio for all 8 colors
        val columns = buildBalancedColumns(
            grid = grid,
            preferredFrontColors = listOf(MarbleColor.ICE, MarbleColor.PURPLE, MarbleColor.LIME),
            seed = 101L
        )

        return LevelData(
            levelNumber = 1,
            grid = grid,
            columns = columns,
            ballsPerBlock = GameConstants.BALLS_PER_BLOCK
        )
    }

    fun getLevel35(): LevelData {
        val grid = Array(GameConstants.GRID_ROWS) {
            Array(GameConstants.GRID_COLS) { GridCell(kind = CellKind.WALL) }
        }

        fun setCell(r: Int, c: Int, kind: CellKind, color: MarbleColor?, isMystery: Boolean = false) {
            grid[r][c] = GridCell(kind = kind, color = color, isMystery = isMystery)
        }

        // r0
        setCell(0, 2, CellKind.LOCKED, MarbleColor.CYAN, true)
        setCell(0, 3, CellKind.LOCKED, MarbleColor.ORANGE, true)

        // r1
        setCell(1, 1, CellKind.LOCKED, MarbleColor.LIME, true)
        setCell(1, 2, CellKind.LOCKED, MarbleColor.RED, false)
        setCell(1, 3, CellKind.LOCKED, MarbleColor.PURPLE, false)
        setCell(1, 4, CellKind.LOCKED, MarbleColor.PINK, true)

        // r2
        setCell(2, 0, CellKind.LOCKED, MarbleColor.YELLOW, true)
        setCell(2, 1, CellKind.LOCKED, MarbleColor.YELLOW, false)
        setCell(2, 2, CellKind.LOCKED, MarbleColor.CYAN, false)
        setCell(2, 3, CellKind.LOCKED, MarbleColor.PURPLE, false)
        setCell(2, 4, CellKind.LOCKED, MarbleColor.ICE, false)
        setCell(2, 5, CellKind.LOCKED, MarbleColor.RED, true)

        // r3: Dispenser 1 at (3, 0)
        grid[3][0] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.PURPLE, MarbleColor.ICE, MarbleColor.CYAN)
        )
        setCell(3, 1, CellKind.LOCKED, MarbleColor.PURPLE, true)
        setCell(3, 2, CellKind.LOCKED, MarbleColor.PURPLE, false)
        setCell(3, 3, CellKind.LOCKED, MarbleColor.LIME, false)
        setCell(3, 4, CellKind.LOCKED, MarbleColor.YELLOW, true)
        setCell(3, 5, CellKind.LOCKED, MarbleColor.LIME, false)

        // r4: Dispenser 2 at (4, 3)
        setCell(4, 0, CellKind.LOCKED, MarbleColor.PURPLE, false)
        setCell(4, 1, CellKind.LOCKED, MarbleColor.ORANGE, false)
        setCell(4, 2, CellKind.LOCKED, MarbleColor.YELLOW, false)
        grid[4][3] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.PINK, MarbleColor.LIME, MarbleColor.PURPLE)
        )
        setCell(4, 4, CellKind.LOCKED, MarbleColor.PINK, false)
        setCell(4, 5, CellKind.LOCKED, MarbleColor.YELLOW, false)

        // r5: Exit row - open and exposed
        setCell(5, 1, CellKind.BLOCK, MarbleColor.LIME)
        setCell(5, 2, CellKind.BLOCK, MarbleColor.ICE)
        setCell(5, 3, CellKind.BLOCK, MarbleColor.PINK)
        setCell(5, 4, CellKind.BLOCK, MarbleColor.PURPLE)

        val columns = buildBalancedColumns(grid, seed = 3535L)

        return LevelData(35, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun generateSolvableLevel(levelNum: Int): LevelData {
        if (levelNum == 35) return getLevel35()
        if (levelNum == 1) return getLevel1()

        val colors = mutableListOf(
            MarbleColor.PINK, MarbleColor.ICE,
            MarbleColor.YELLOW, MarbleColor.LIME, MarbleColor.CYAN
        )
        if (levelNum > 10) colors.add(MarbleColor.ORANGE)
        if (levelNum > 20) colors.add(MarbleColor.RED)

        val grid = Array(GameConstants.GRID_ROWS) {
            Array(GameConstants.GRID_COLS) { GridCell(kind = CellKind.WALL) }
        }

        val mask = arrayOf(
            intArrayOf(0, 0, 1, 1, 0, 0),
            intArrayOf(0, 1, 1, 1, 1, 0),
            intArrayOf(1, 1, 1, 1, 1, 1),
            intArrayOf(1, 1, 1, 1, 1, 1),
            intArrayOf(1, 1, 1, 1, 1, 1),
            intArrayOf(0, 1, 1, 1, 1, 0)
        )

        val rng = Random(levelNum * 1009L + 31L)

        for (r in 0 until GameConstants.GRID_ROWS) {
            for (c in 0 until GameConstants.GRID_COLS) {
                if (mask[r][c] == 0) continue

                val randomColor = colors[rng.nextInt(colors.size)]
                val isLocked = r < 5
                val isMystery = isLocked && (r == 0 || (r == 1 && (c == 1 || c == 4)) || (r == 2 && (c == 0 || c == 5)))
                grid[r][c] = GridCell(
                    kind = if (isLocked) CellKind.LOCKED else CellKind.BLOCK,
                    color = randomColor,
                    isMystery = isMystery
                )
            }
        }

        // Add Cave Dispensers for level >= 5
        if (levelNum >= 5) {
            val numDispensers = if (levelNum >= 10) 2 else 1
            val candidates = listOf(
                Pair(3, 0), Pair(3, 5), Pair(4, 3), Pair(2, 0)
            ).filter { mask[it.first][it.second] == 1 && grid[it.first][it.second].kind == CellKind.LOCKED }

            for (i in 0 until minOf(numDispensers, candidates.size)) {
                val pos = candidates[i]
                val queue = mutableListOf(
                    colors[rng.nextInt(colors.size)],
                    colors[rng.nextInt(colors.size)],
                    colors[rng.nextInt(colors.size)]
                )

                grid[pos.first][pos.second] = GridCell(
                    kind = CellKind.DISPENSER,
                    counter = queue.size,
                    queue = queue
                )
            }
        }

        val exitRowColors = (0 until GameConstants.GRID_COLS)
            .mapNotNull { c -> grid[GameConstants.EXIT_ROW][c].color }
            .distinct()

        val columns = buildBalancedColumns(
            grid = grid,
            preferredFrontColors = exitRowColors,
            seed = levelNum * 777L
        )

        return LevelData(levelNum, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }
}
