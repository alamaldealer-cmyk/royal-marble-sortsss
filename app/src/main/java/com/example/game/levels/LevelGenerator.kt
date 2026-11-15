package com.example.game.levels

import com.example.game.constants.GameConstants
import com.example.game.model.CellKind
import com.example.game.model.DispenserDirection
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
        exactFrontRow: List<MarbleColor>? = null,
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

        val columns = listOf(
            mutableListOf<MarbleColor>(),
            mutableListOf<MarbleColor>(),
            mutableListOf<MarbleColor>(),
            mutableListOf<MarbleColor>()
        )

        if (exactFrontRow != null && exactFrontRow.size == 4) {
            exactFrontRow.forEachIndexed { colIdx, frontColor ->
                val idx = brickList.indexOf(frontColor)
                if (idx >= 0) {
                    columns[colIdx].add(brickList.removeAt(idx))
                }
            }
            for (color in brickList) {
                val targetCol = columns.indices.minByOrNull { columns[it].size } ?: 0
                columns[targetCol].add(color)
            }
        } else {
            for (prefColor in preferredFrontColors.reversed()) {
                val idx = brickList.indexOf(prefColor)
                if (idx > 0) {
                    brickList.removeAt(idx)
                    brickList.add(0, prefColor)
                }
            }

            brickList.forEachIndexed { idx, color ->
                columns[idx % 4].add(color)
            }
        }

        return columns
    }

    private fun newWallGrid(): Array<Array<GridCell>> {
        return Array(GameConstants.GRID_ROWS) {
            Array(GameConstants.GRID_COLS) { GridCell(kind = CellKind.WALL) }
        }
    }

    fun getLevel1(): LevelData {
        val grid = newWallGrid()
        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)

        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.YELLOW)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.CYAN)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.YELLOW)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.CYAN, MarbleColor.YELLOW, MarbleColor.YELLOW, MarbleColor.CYAN),
            seed = 101L
        )
        return LevelData(1, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel2(): LevelData {
        val grid = newWallGrid()
        grid[3][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME)

        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)

        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.YELLOW)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.YELLOW)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.LIME, MarbleColor.RED, MarbleColor.LIME, MarbleColor.LIME),
            seed = 202L
        )
        return LevelData(2, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel3(): LevelData {
        val grid = newWallGrid()
        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)
        grid[3][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)

        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)

        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.YELLOW)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.RED)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.YELLOW)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.YELLOW, MarbleColor.YELLOW, MarbleColor.CYAN, MarbleColor.YELLOW),
            seed = 303L
        )
        return LevelData(3, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel4(): LevelData {
        val grid = newWallGrid()
        grid[2][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME)
        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)

        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)
        grid[3][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)

        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME)

        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIME)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.YELLOW)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.ORANGE)
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.ORANGE)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.ORANGE, MarbleColor.ORANGE, MarbleColor.BLUE, MarbleColor.ORANGE),
            seed = 404L
        )
        return LevelData(4, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel5(): LevelData {
        val grid = newWallGrid()
        grid[3][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN, isMystery = true)
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, isMystery = true)

        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN, isMystery = true)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME, isMystery = true)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN, isMystery = true)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, isMystery = true)

        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.PURPLE)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIME)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.YELLOW)
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIME)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.LIME, MarbleColor.CYAN, MarbleColor.LIME, MarbleColor.LIME),
            seed = 505L
        )
        return LevelData(5, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel6(): LevelData {
        val grid = newWallGrid()
        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, isMystery = true)
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, isMystery = true)
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, isMystery = true)

        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, isMystery = true)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, isMystery = false)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, isMystery = true)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME, isMystery = true)

        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIME)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.ORANGE)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.YELLOW)
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.YELLOW)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.PURPLE, MarbleColor.PURPLE, MarbleColor.PURPLE, MarbleColor.YELLOW),
            seed = 606L
        )
        return LevelData(6, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel7(): LevelData {
        val grid = newWallGrid()
        grid[2][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, isMystery = true)
        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK, isMystery = true)
        grid[2][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN, isMystery = true)
        grid[2][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED, isMystery = true)

        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)
        grid[3][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)

        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK, isMystery = true)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, isMystery = true)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, isMystery = true)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN, isMystery = true)

        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.YELLOW)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.CYAN)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.RED)
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIGHT_PINK)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.LIGHT_PINK, MarbleColor.PURPLE, MarbleColor.YELLOW, MarbleColor.PURPLE),
            seed = 707L
        )
        return LevelData(7, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel8(): LevelData {
        val grid = newWallGrid()
        grid[2][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, isMystery = true)
        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME, isMystery = true)
        grid[2][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, isMystery = true)

        grid[3][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, isMystery = true)
        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MINT)
        grid[3][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME)
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, isMystery = true)

        grid[4][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MINT, isMystery = true)
        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MINT)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED, isMystery = true)

        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.RED)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.RED)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.RED)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.ORANGE, MarbleColor.ORANGE, MarbleColor.ORANGE, MarbleColor.LIME),
            seed = 808L
        )
        return LevelData(8, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel9(): LevelData {
        val grid = newWallGrid()
        grid[1][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, isMystery = true)

        grid[2][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK, isMystery = true)
        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE, isMystery = true)
        grid[2][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, isMystery = true)

        grid[3][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MINT, isMystery = true)
        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK, isMystery = true)
        grid[3][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, isMystery = true)
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE, isMystery = true)
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED, isMystery = true)

        grid[4][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE, isMystery = true)
        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK, isMystery = true)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, isMystery = true)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MINT, isMystery = true)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, isMystery = true)

        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.RED)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIGHT_PINK)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.MINT)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.LIGHT_PINK, MarbleColor.PURPLE, MarbleColor.LIGHT_PINK, MarbleColor.LIGHT_PINK),
            seed = 909L
        )
        return LevelData(9, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel10(): LevelData {
        val grid = newWallGrid()
        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)
        grid[2][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)
        grid[2][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)

        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)
        grid[3][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)

        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[4][4] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.LIGHT_PINK, MarbleColor.BLUE, MarbleColor.CYAN)
        )

        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIGHT_PINK)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BLUE)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIGHT_PINK)
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.CYAN)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.BLUE, MarbleColor.LIGHT_PINK, MarbleColor.BLUE, MarbleColor.CYAN),
            seed = 1010L
        )
        return LevelData(10, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel11(): LevelData {
        val grid = newWallGrid()
        // 4x4 with 2 top Cave Dispensers (3 each) at (2,1) and (2,4)
        grid[2][1] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.ORANGE, MarbleColor.YELLOW, MarbleColor.BLUE)
        )
        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)
        grid[2][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[2][4] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.CYAN, MarbleColor.YELLOW, MarbleColor.ORANGE)
        )

        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, isMystery = true)
        grid[3][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, isMystery = true)
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN, isMystery = true)
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, isMystery = true)

        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, isMystery = true)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE, isMystery = true)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, isMystery = true)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, isMystery = true)

        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.ORANGE)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.ORANGE)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.YELLOW)
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.YELLOW)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.YELLOW, MarbleColor.ORANGE, MarbleColor.ORANGE, MarbleColor.YELLOW),
            seed = 1111L
        )
        return LevelData(11, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel12(): LevelData {
        val grid = newWallGrid()
        // Asymmetric 5-col layout with Cave Dispenser(3) at (3,5)
        grid[2][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)
        grid[2][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)
        grid[2][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLACK)

        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)
        grid[3][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN)
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN)
        grid[3][5] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.BLUE, MarbleColor.DARK_GREEN, MarbleColor.LIGHT_PINK)
        )

        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLACK)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[4][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)

        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BLACK)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIGHT_PINK)
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.DARK_GREEN)
        grid[5][5] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BLUE)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.BLUE, MarbleColor.BLACK, MarbleColor.BLUE, MarbleColor.BLUE),
            seed = 1212L
        )
        return LevelData(12, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel13(): LevelData {
        val grid = newWallGrid()
        // 4x5 tower with right notch at (2,4) & Cave Dispenser(3) at (3,2)
        grid[1][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, isMystery = true)
        grid[1][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, isMystery = true)
        grid[1][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE, isMystery = true)
        grid[1][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, isMystery = true)

        grid[2][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, isMystery = true)
        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN, isMystery = true)
        grid[2][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME, isMystery = true)
        // grid[2][4] remains WALL notch

        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)
        grid[3][2] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.PURPLE, MarbleColor.YELLOW, MarbleColor.BLUE)
        )
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)

        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)

        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIME)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.CYAN)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIGHT_PINK)
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.YELLOW)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.BLUE, MarbleColor.YELLOW, MarbleColor.YELLOW, MarbleColor.PURPLE),
            seed = 1313L
        )
        return LevelData(13, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel14(): LevelData {
        val grid = newWallGrid()
        // 5-col fortress with twin top peaks (1,1) & (1,3) and center Dispenser(3) at (3,2)
        grid[1][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, isMystery = true)
        grid[1][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME, isMystery = true)

        grid[2][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, isMystery = true)
        grid[2][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME)
        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, isMystery = true)
        grid[2][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME)
        grid[2][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK, isMystery = true)

        grid[3][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MAROON)
        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, isMystery = true)
        grid[3][2] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.PURPLE, MarbleColor.LIME, MarbleColor.ORANGE)
        )
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MAROON, isMystery = true)
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME)

        grid[4][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK, isMystery = true)
        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, isMystery = true)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, isMystery = true)

        grid[5][0] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.ORANGE)
        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIGHT_PINK)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.ORANGE)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.MAROON)
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIME)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.MAROON, MarbleColor.LIGHT_PINK, MarbleColor.MAROON, MarbleColor.LIGHT_PINK),
            seed = 1414L
        )
        return LevelData(14, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel15(): LevelData {
        val grid = newWallGrid()
        // 5-col stepped dome with center Dispenser(3) at (2,2)
        grid[1][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[1][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[1][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME)

        grid[2][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)
        grid[2][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)
        grid[2][2] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.LIME, MarbleColor.BLUE, MarbleColor.CYAN)
        )
        grid[2][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[2][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)

        grid[3][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)
        grid[3][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME)
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME)
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)

        grid[4][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)

        grid[5][0] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.RED)
        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.ORANGE)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.ORANGE)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.CYAN)
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.PURPLE)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.BLUE, MarbleColor.CYAN, MarbleColor.BLUE, MarbleColor.ORANGE),
            seed = 1515L
        )
        return LevelData(15, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel16(): LevelData {
        val grid = newWallGrid()
        // 5-col stepped layout with 2 Cave Dispensers(3) at (2,0) and (4,4)
        grid[2][0] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.DARK_GREEN, MarbleColor.MAROON, MarbleColor.MINT)
        )
        grid[2][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)
        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN)
        grid[2][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MAROON)

        grid[3][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN)
        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[3][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MAROON)
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLACK)

        grid[4][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLACK)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN)
        grid[4][4] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.MINT, MarbleColor.BLACK, MarbleColor.BLUE)
        )

        grid[5][0] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BLACK)
        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BLUE)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.MINT)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BLUE)
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.MINT)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.DARK_GREEN, MarbleColor.DARK_GREEN, MarbleColor.MINT, MarbleColor.PURPLE),
            seed = 1616L
        )
        return LevelData(16, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel17(): LevelData {
        val grid = newWallGrid()
        // 6-col full chamber with 2 Dispensers(3) at (1,1) & (3,4), and center 2-col stone block at (5,2)-(5,3)
        grid[1][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.WHITE, isMystery = true)
        grid[1][1] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.YELLOW, MarbleColor.BROWN, MarbleColor.CYAN)
        )
        grid[1][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, isMystery = true)
        grid[1][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, isMystery = true)
        grid[1][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.WHITE, isMystery = true)
        grid[1][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, isMystery = true)

        grid[2][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[2][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN, isMystery = true)
        grid[2][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, isMystery = true)
        grid[2][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[2][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.WHITE)

        grid[3][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN)
        grid[3][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.WHITE, isMystery = true)
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE, isMystery = true)
        grid[3][4] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.WHITE, MarbleColor.YELLOW, MarbleColor.BLUE)
        )
        grid[3][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.WHITE)

        grid[4][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK)
        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK, isMystery = true)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, isMystery = true)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.WHITE)
        grid[4][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)

        grid[5][0] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIGHT_PINK)
        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BLUE)
        // grid[5][2] and grid[5][3] remain WALL stone barrier
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BROWN)
        grid[5][5] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BROWN)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.WHITE, MarbleColor.PURPLE, MarbleColor.BLUE, MarbleColor.WHITE),
            seed = 1717L
        )
        return LevelData(17, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel18(): LevelData {
        val grid = newWallGrid()
        // Stepped diamond (3 - 5 - 6 - 6 - 5 - 3) with twin 4-count Cave Dispensers at (2,1) & (2,4)
        grid[0][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN, isMystery = true)
        grid[0][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME, isMystery = true)
        grid[0][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, isMystery = true)

        grid[1][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE, isMystery = true)
        grid[1][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED, isMystery = true)
        grid[1][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN, isMystery = true)
        grid[1][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.WHITE, isMystery = true)
        grid[1][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME, isMystery = true)

        grid[2][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, isMystery = true)
        grid[2][1] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 4,
            queue = mutableListOf(MarbleColor.LIME, MarbleColor.CYAN, MarbleColor.BLUE, MarbleColor.WHITE)
        )
        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME, isMystery = true)
        grid[2][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN, isMystery = true)
        grid[2][4] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 4,
            queue = mutableListOf(MarbleColor.RED, MarbleColor.CYAN, MarbleColor.LIME, MarbleColor.YELLOW)
        )
        grid[2][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED, isMystery = true)

        grid[3][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN, isMystery = true)
        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME, isMystery = true)
        grid[3][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, isMystery = true)
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.WHITE, isMystery = true)
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE, isMystery = true)
        grid[3][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN, isMystery = true)

        grid[4][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME, isMystery = true)
        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.WHITE)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN, isMystery = true)

        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIME)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.CYAN)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.YELLOW)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.BLUE, MarbleColor.CYAN, MarbleColor.LIME, MarbleColor.LIME),
            seed = 1818L
        )
        return LevelData(18, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel19(): LevelData {
        val grid = newWallGrid()
        // 5-col tower with 2 Cave Dispensers(3) at (3,0) & (2,4), and stone gap at (5,3)
        grid[1][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[1][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN)
        grid[1][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)

        grid[2][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)
        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[2][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLACK)
        grid[2][4] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.CYAN, MarbleColor.LIGHT_PINK, MarbleColor.PURPLE)
        )

        grid[3][0] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.PURPLE, MarbleColor.BROWN, MarbleColor.BLUE)
        )
        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK)
        grid[3][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN)
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)

        grid[4][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK)
        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK)

        grid[5][0] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.ORANGE)
        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIGHT_PINK)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BLACK)
        // grid[5][3] remains WALL stone recess
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIGHT_PINK)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.BLUE, MarbleColor.BLUE, MarbleColor.ORANGE, MarbleColor.LIGHT_PINK),
            seed = 1919L
        )
        return LevelData(19, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel20(): LevelData {
        val grid = newWallGrid()
        // 5-col stepped tower with right-side 3-wide mystery tower & Dispenser(3) at (2,0)
        grid[0][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE, isMystery = true)
        grid[0][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED, isMystery = true)
        grid[0][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN, isMystery = true)

        grid[1][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED, isMystery = true)
        grid[1][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE, isMystery = true)
        grid[1][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN, isMystery = true)

        grid[2][0] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.RED, MarbleColor.BLUE, MarbleColor.DARK_GREEN)
        )
        grid[2][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)
        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[2][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[2][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN)

        grid[3][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)
        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK)
        grid[3][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME)

        grid[4][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN)
        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK)

        grid[5][0] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BLUE)
        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.YELLOW)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.RED)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.DARK_GREEN, MarbleColor.RED, MarbleColor.DARK_GREEN, MarbleColor.LIME),
            seed = 2020L
        )
        return LevelData(20, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel31(): LevelData {
        val grid = newWallGrid()
        // 5-col tower with 1x2 vertical stone pillar at (3,2)-(4,2) & Cave Dispenser(3) at (3,1)
        grid[2][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)
        grid[2][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME)
        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)
        grid[2][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ICE)
        grid[2][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)

        grid[3][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[3][1] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.LIME, MarbleColor.LIGHT_PINK, MarbleColor.RED)
        )
        // grid[3][2] & grid[4][2] remain WALL vertical stone pillar
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)

        grid[4][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ICE)
        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)

        grid[5][0] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.RED)
        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIGHT_PINK)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.RED)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIME)
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BLUE)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.ORANGE, MarbleColor.RED, MarbleColor.ORANGE, MarbleColor.BLUE),
            seed = 3131L
        )
        return LevelData(31, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel32(): LevelData {
        val grid = newWallGrid()
        // Asymmetric 5-col tower with 2 Cave Dispensers(3) at (2,0) & (2,4)
        grid[1][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN)
        grid[1][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)

        grid[2][0] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.RED, MarbleColor.PURPLE, MarbleColor.MAROON)
        )
        grid[2][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[2][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK)
        grid[2][4] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.BROWN, MarbleColor.CYAN, MarbleColor.LIGHT_PINK)
        )

        grid[3][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)
        grid[3][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)

        grid[4][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN)
        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MAROON)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)

        grid[5][0] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.MAROON)
        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.ICE)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.MAROON)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIGHT_PINK)
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIGHT_PINK)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.RED, MarbleColor.LIGHT_PINK, MarbleColor.BROWN, MarbleColor.BLUE),
            seed = 3232L
        )
        return LevelData(32, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel33(): LevelData {
        val grid = newWallGrid()
        // Stepped left-tall tower with 3x3 mystery cluster & 2 Dispensers(3) at (2,3) & (4,0)
        grid[1][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN, isMystery = true)
        grid[1][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN, isMystery = true)
        grid[1][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLACK, isMystery = true)

        grid[2][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, isMystery = true)
        grid[2][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE, isMystery = true)
        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN, isMystery = true)
        grid[2][3] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.DARK_GREEN, MarbleColor.YELLOW, MarbleColor.PURPLE)
        )
        grid[2][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)

        grid[3][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK, isMystery = true)
        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN, isMystery = true)
        grid[3][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE, isMystery = true)
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN)

        grid[4][0] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.ORANGE, MarbleColor.CYAN, MarbleColor.LIGHT_PINK)
        )
        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)

        grid[5][0] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.PURPLE)
        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.ORANGE)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.DARK_GREEN)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.CYAN)
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BLUE)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.CYAN, MarbleColor.BLUE, MarbleColor.BLUE, MarbleColor.CYAN),
            seed = 3333L
        )
        return LevelData(33, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel34(): LevelData {
        val grid = newWallGrid()
        // 4-col stepped tower with 1x2 stone pillar at (3,2)-(4,2) & LEFT-facing Dispenser(2) at (4,4)
        grid[1][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)
        grid[1][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[1][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)

        grid[2][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)
        grid[2][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[2][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)

        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME)
        // grid[3][2] & grid[4][2] remain WALL vertical stone pillar
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)

        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)
        grid[4][4] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 2,
            direction = DispenserDirection.LEFT,
            queue = mutableListOf(MarbleColor.PURPLE, MarbleColor.BLUE)
        )

        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIME)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.PINK)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.YELLOW)
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.PURPLE)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.YELLOW, MarbleColor.BLUE, MarbleColor.LIME, MarbleColor.PURPLE),
            seed = 3434L
        )
        return LevelData(34, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel35(): LevelData {
        val grid = newWallGrid()
        // 6x6 full chamber with twin center Dispensers(3) at (3,2) & (3,3) and corner mystery clusters
        grid[0][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, isMystery = true)
        grid[0][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN, isMystery = true)
        grid[0][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)
        grid[0][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MAROON)
        grid[0][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, isMystery = true)
        grid[0][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK, isMystery = true)

        grid[1][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, isMystery = true)
        grid[1][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE, isMystery = true)
        grid[1][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)
        grid[1][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MAROON)
        grid[1][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, isMystery = true)
        grid[1][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.WHITE, isMystery = true)

        grid[2][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)
        grid[2][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)
        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN)
        grid[2][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN)
        grid[2][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)
        grid[2][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN)

        grid[3][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE, isMystery = true)
        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK, isMystery = true)
        grid[3][2] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.PURPLE, MarbleColor.YELLOW, MarbleColor.DARK_GREEN)
        )
        grid[3][3] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.BLUE, MarbleColor.ORANGE, MarbleColor.LIGHT_PINK)
        )
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, isMystery = true)
        grid[3][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE, isMystery = true)

        grid[4][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN)
        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MAROON)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)
        grid[4][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)

        grid[5][0] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.YELLOW)
        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BROWN)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.YELLOW)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIGHT_PINK)
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIGHT_PINK)
        grid[5][5] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.WHITE)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.DARK_GREEN, MarbleColor.BLUE, MarbleColor.ORANGE, MarbleColor.LIGHT_PINK),
            seed = 3535L
        )
        return LevelData(35, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel36(): LevelData {
        val grid = newWallGrid()
        // 4x6 layout with 2 RIGHT-facing Cave Dispensers(2) at (1,1) & (3,1) separated by stone notch (2,1)
        grid[0][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)
        grid[0][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)
        grid[0][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[0][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)

        grid[1][1] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 2,
            direction = DispenserDirection.RIGHT,
            queue = mutableListOf(MarbleColor.ORANGE, MarbleColor.PURPLE)
        )
        grid[1][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, isMystery = true)
        grid[1][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, isMystery = true)
        grid[1][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK, isMystery = true)

        // grid[2][1] remains WALL recess between dispensers
        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[2][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)
        grid[2][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)

        grid[3][1] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 2,
            direction = DispenserDirection.RIGHT,
            queue = mutableListOf(MarbleColor.ORANGE, MarbleColor.YELLOW)
        )
        grid[3][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED, isMystery = true)
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, isMystery = true)
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE, isMystery = true)

        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)

        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.YELLOW)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIGHT_PINK)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIGHT_PINK)
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BLUE)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.ORANGE, MarbleColor.LIGHT_PINK, MarbleColor.RED, MarbleColor.RED),
            seed = 3636L
        )
        return LevelData(36, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel37(): LevelData {
        val grid = newWallGrid()
        // 4x5 alternating stripe layout with 2 top Dispensers(3) at (1,1) & (1,3) + 8 mystery crates
        grid[1][1] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.MINT, MarbleColor.LIME, MarbleColor.RED)
        )
        grid[1][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE, isMystery = true)
        grid[1][3] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.MINT, MarbleColor.BLUE, MarbleColor.LIME)
        )
        grid[1][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED, isMystery = true)

        grid[2][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)
        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME, isMystery = true)
        grid[2][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[2][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MINT, isMystery = true)

        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MINT)
        grid[3][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE, isMystery = true)
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MINT, isMystery = true)

        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MINT)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED, isMystery = true)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MINT, isMystery = true)

        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIME)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIME)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BLUE)
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BLUE)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.RED, MarbleColor.MINT, MarbleColor.LIME, MarbleColor.BLUE),
            seed = 3737L
        )
        return LevelData(37, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel38(): LevelData {
        val grid = newWallGrid()
        // 6-row stepped pyramid (2-4-6-6-6-4) with 2 offset Dispensers(3) at (3,0) & (4,3)
        grid[0][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED, isMystery = true)
        grid[0][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MINT, isMystery = true)

        grid[1][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, isMystery = true)
        grid[1][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ICE)
        grid[1][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[1][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MINT, isMystery = true)

        grid[2][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MINT, isMystery = true)
        grid[2][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.WHITE)
        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[2][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[2][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLACK)
        grid[2][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, isMystery = true)

        grid[3][0] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.MINT, MarbleColor.ORANGE, MarbleColor.RED)
        )
        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLACK, isMystery = true)
        grid[3][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MINT)
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.WHITE, isMystery = true)
        grid[3][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MINT)

        grid[4][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.WHITE)
        grid[4][3] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.MINT, MarbleColor.WHITE, MarbleColor.PURPLE)
        )
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)
        grid[4][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.WHITE)

        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.MINT)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BLACK)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.ORANGE)
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.RED)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.RED, MarbleColor.RED, MarbleColor.MINT, MarbleColor.BLACK),
            seed = 3838L
        )
        return LevelData(38, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel39(): LevelData {
        val grid = newWallGrid()
        // L-shaped tower (3-wide top rows 0..3, 5-wide bottom rows 4..5) with 2 Dispensers(3) at (0,2) & (3,0)
        grid[0][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, isMystery = true)
        grid[0][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MAROON)
        grid[0][2] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.LIME, MarbleColor.YELLOW, MarbleColor.MAROON)
        )

        grid[1][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[1][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[1][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, isMystery = true)

        grid[2][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME, isMystery = true)
        grid[2][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME)
        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)

        grid[3][0] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.PURPLE, MarbleColor.ORANGE, MarbleColor.MAROON)
        )
        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MAROON, isMystery = true)
        grid[3][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN, isMystery = true)

        grid[4][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MAROON)
        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MAROON)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, isMystery = true)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN)

        grid[5][0] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BROWN)
        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.ICE)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.PURPLE)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.YELLOW)
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.YELLOW)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.MAROON, MarbleColor.BROWN, MarbleColor.ORANGE, MarbleColor.BROWN),
            seed = 3939L
        )
        return LevelData(39, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel40(): LevelData {
        val grid = newWallGrid()
        // 6x6 pyramid (4-6-6-6-6-4) with 16 top mystery crates & twin center Dispensers(3) at (3,2) & (3,3)
        grid[0][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.WHITE, isMystery = true)
        grid[0][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN, isMystery = true)
        grid[0][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, isMystery = true)
        grid[0][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED, isMystery = true)

        grid[1][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MINT, isMystery = true)
        grid[1][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.WHITE, isMystery = true)
        grid[1][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN, isMystery = true)
        grid[1][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, isMystery = true)
        grid[1][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.WHITE, isMystery = true)
        grid[1][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, isMystery = true)

        grid[2][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, isMystery = true)
        grid[2][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.WHITE, isMystery = true)
        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN, isMystery = true)
        grid[2][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, isMystery = true)
        grid[2][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.WHITE, isMystery = true)
        grid[2][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN, isMystery = true)

        grid[3][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)
        grid[3][2] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.CYAN, MarbleColor.WHITE, MarbleColor.YELLOW)
        )
        grid[3][3] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.WHITE, MarbleColor.CYAN, MarbleColor.MINT)
        )
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MINT)
        grid[3][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)

        grid[4][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.WHITE)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.WHITE)
        grid[4][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)

        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BROWN)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.CYAN)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.WHITE)
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.DARK_GREEN)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.CYAN, MarbleColor.CYAN, MarbleColor.YELLOW, MarbleColor.CYAN),
            seed = 4040L
        )
        return LevelData(40, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel21(): LevelData {
        val grid = newWallGrid()
        // 6-col asymmetric tower with 2 top Dispensers(3) at (1,0) and (1,4)
        grid[1][0] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.YELLOW, MarbleColor.CYAN, MarbleColor.BROWN)
        )
        grid[1][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[1][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[1][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[1][4] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.YELLOW, MarbleColor.DARK_GREEN, MarbleColor.BLACK)
        )

        grid[2][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[2][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, isMystery = true)
        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN, isMystery = true)
        grid[2][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN, isMystery = true)
        grid[2][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN)

        grid[3][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN, isMystery = true)
        grid[3][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLACK, isMystery = true)
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK, isMystery = true)
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN)
        grid[3][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN)

        grid[4][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK)
        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, isMystery = true)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN, isMystery = true)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED, isMystery = true)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN)
        grid[4][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN)

        grid[5][0] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BLACK)
        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.DARK_GREEN)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.CYAN)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.RED)
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BLACK)
        grid[5][5] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.CYAN)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.CYAN, MarbleColor.CYAN, MarbleColor.BLACK, MarbleColor.DARK_GREEN),
            seed = 2121L
        )
        return LevelData(21, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel22(): LevelData {
        val grid = newWallGrid()
        // 5-col stepped dome with center Dispenser(3) at (2,2) & 11 mystery crates
        grid[1][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED, isMystery = true)
        grid[1][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE, isMystery = true)
        grid[1][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN, isMystery = true)

        grid[2][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, isMystery = true)
        grid[2][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, isMystery = true)
        grid[2][2] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.CYAN, MarbleColor.ORANGE, MarbleColor.RED)
        )
        grid[2][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE, isMystery = true)
        grid[2][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN, isMystery = true)

        grid[3][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, isMystery = true)
        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED, isMystery = true)
        grid[3][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE, isMystery = true)
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN, isMystery = true)

        grid[4][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)

        grid[5][0] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BROWN)
        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.CYAN)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BLUE)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BROWN)
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BLUE)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.RED, MarbleColor.BLUE, MarbleColor.BLUE, MarbleColor.CYAN),
            seed = 2222L
        )
        return LevelData(22, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel23(): LevelData {
        val grid = newWallGrid()
        // 3-col central shaft (c=1..3) flanked by 4 sideways Cave Dispensers(2): RIGHT at c=0, LEFT at c=4
        grid[1][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN, isMystery = true)
        grid[1][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, isMystery = true)
        grid[1][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED, isMystery = true)

        grid[2][0] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 2,
            direction = DispenserDirection.RIGHT,
            queue = mutableListOf(MarbleColor.ORANGE, MarbleColor.BROWN)
        )
        grid[2][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLACK)
        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)
        grid[2][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN)
        grid[2][4] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 2,
            direction = DispenserDirection.LEFT,
            queue = mutableListOf(MarbleColor.YELLOW, MarbleColor.CYAN)
        )

        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLACK, isMystery = true)
        grid[3][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, isMystery = true)
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, isMystery = true)

        grid[4][0] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 2,
            direction = DispenserDirection.RIGHT,
            queue = mutableListOf(MarbleColor.RED, MarbleColor.ORANGE)
        )
        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[4][4] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 2,
            direction = DispenserDirection.LEFT,
            queue = mutableListOf(MarbleColor.BLACK, MarbleColor.YELLOW)
        )

        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.RED)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.ORANGE)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.WHITE)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.BLACK, MarbleColor.RED, MarbleColor.YELLOW, MarbleColor.CYAN),
            seed = 2323L
        )
        return LevelData(23, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel24(): LevelData {
        val grid = newWallGrid()
        // 4-col x 6-row hourglass with stone waist at r=2 (cols 1 and 4 are WALL)
        grid[0][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, isMystery = true)
        grid[0][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, isMystery = true)
        grid[0][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, isMystery = true)
        grid[0][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME, isMystery = true)

        grid[1][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED, isMystery = true)
        grid[1][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE, isMystery = true)
        grid[1][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME, isMystery = true)
        grid[1][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, isMystery = true)

        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, isMystery = true)
        grid[2][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, isMystery = true)

        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED, isMystery = true)
        grid[3][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, isMystery = true)
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE, isMystery = true)
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME, isMystery = true)

        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)

        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BLUE)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.YELLOW)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIME)
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.RED)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.RED, MarbleColor.PURPLE, MarbleColor.LIME, MarbleColor.ORANGE),
            seed = 2424L
        )
        return LevelData(24, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel25(): LevelData {
        val grid = newWallGrid()
        // 5-col waisted chamber with 4-count Cave Dispenser at (2,2)
        grid[0][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME, isMystery = true)
        grid[0][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, isMystery = true)
        grid[0][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.WHITE, isMystery = true)
        grid[0][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE, isMystery = true)
        grid[0][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED, isMystery = true)

        grid[1][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED, isMystery = true)
        grid[1][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.WHITE, isMystery = true)
        grid[1][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME, isMystery = true)
        grid[1][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, isMystery = true)
        grid[1][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.WHITE, isMystery = true)

        grid[2][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, isMystery = true)
        grid[2][2] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 4,
            queue = mutableListOf(MarbleColor.LIME, MarbleColor.ORANGE, MarbleColor.RED, MarbleColor.WHITE)
        )
        grid[2][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME, isMystery = true)

        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME)
        grid[3][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.WHITE)

        grid[4][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)
        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)

        grid[5][0] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIME)
        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.RED)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.RED)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIME)
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.WHITE)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.LIME, MarbleColor.LIME, MarbleColor.ORANGE, MarbleColor.WHITE),
            seed = 2525L
        )
        return LevelData(25, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel26(): LevelData {
        val grid = newWallGrid()
        // Stepped pyramid adapted to 6x6 with twin 3-count Cave Dispensers at (3,2) & (3,4)
        grid[1][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)
        grid[1][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)
        grid[1][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)

        grid[2][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)
        grid[2][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[2][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[2][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)

        grid[3][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)
        grid[3][2] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.LIGHT_PINK, MarbleColor.LIME, MarbleColor.PURPLE)
        )
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK)
        grid[3][4] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.LIGHT_PINK, MarbleColor.YELLOW, MarbleColor.CYAN)
        )
        grid[3][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)

        grid[4][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME)
        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[4][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)

        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.ORANGE)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIGHT_PINK)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.CYAN)
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIGHT_PINK)
        grid[5][5] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.YELLOW)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.YELLOW, MarbleColor.RED, MarbleColor.LIGHT_PINK, MarbleColor.CYAN),
            seed = 2626L
        )
        return LevelData(26, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel27(): LevelData {
        val grid = newWallGrid()
        // Staircase castle with DOWN Dispenser(3) at (0,0) and RIGHT-facing Dispenser(3) at (3,0)
        grid[0][0] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            direction = DispenserDirection.DOWN,
            queue = mutableListOf(MarbleColor.MAROON, MarbleColor.LIGHT_PINK, MarbleColor.MINT)
        )
        grid[0][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MAROON)

        grid[1][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MAROON)
        grid[1][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[1][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[1][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)

        grid[2][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK)
        grid[2][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MINT)
        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN)
        grid[2][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN)
        grid[2][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)

        grid[3][0] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            direction = DispenserDirection.RIGHT,
            queue = mutableListOf(MarbleColor.BLACK, MarbleColor.PURPLE, MarbleColor.LIGHT_PINK)
        )
        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MINT)
        grid[3][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MINT)
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MINT)

        grid[4][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK)
        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLACK)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLACK)

        grid[5][0] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.MAROON)
        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.ORANGE)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.PURPLE)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIGHT_PINK)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.LIGHT_PINK, MarbleColor.LIGHT_PINK, MarbleColor.MINT, MarbleColor.ORANGE),
            seed = 2727L
        )
        return LevelData(27, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel28(): LevelData {
        val grid = newWallGrid()
        // 5x5 checkerboard of mystery crates & colors with center stone gap at (5,2)
        grid[1][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN, isMystery = true)
        grid[1][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[1][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, isMystery = true)
        grid[1][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)
        grid[1][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, isMystery = true)

        grid[2][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)
        grid[2][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN, isMystery = true)
        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)
        grid[2][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED, isMystery = true)
        grid[2][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN)

        grid[3][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, isMystery = true)
        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[3][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MINT, isMystery = true)
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, isMystery = true)

        grid[4][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)
        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MINT, isMystery = true)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, isMystery = true)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MINT)

        grid[5][0] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.MINT)
        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BROWN)
        // grid[5][2] remains WALL stone recess
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.PURPLE)
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BLUE)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.ORANGE, MarbleColor.DARK_GREEN, MarbleColor.ORANGE, MarbleColor.PURPLE),
            seed = 2828L
        )
        return LevelData(28, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel29(): LevelData {
        val grid = newWallGrid()
        // 5x5 tower with 1x2 vertical stone pillar at (2,3)-(3,3) & Cave Dispenser(3) at (3,1)
        grid[1][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, isMystery = true)
        grid[1][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN, isMystery = true)
        grid[1][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE, isMystery = true)
        grid[1][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, isMystery = true)
        grid[1][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK, isMystery = true)

        grid[2][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[2][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)
        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)
        // grid[2][3] & grid[3][3] remain WALL vertical stone pillar
        grid[2][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)

        grid[3][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN, isMystery = true)
        grid[3][1] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.ORANGE, MarbleColor.LIGHT_PINK, MarbleColor.YELLOW)
        )
        grid[3][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED, isMystery = true)
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, isMystery = true)

        grid[4][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN)
        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PINK)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN)

        grid[5][0] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.RED)
        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BLUE)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BROWN)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BLUE)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.ORANGE, MarbleColor.PINK, MarbleColor.ORANGE, MarbleColor.BLUE),
            seed = 2929L
        )
        return LevelData(29, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel30(): LevelData {
        val grid = newWallGrid()
        // 6-col pyramid with 4 Cave Dispensers(3): top twin (0,2),(0,3) & side twin (3,0),(3,5)
        grid[0][2] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.PURPLE, MarbleColor.RED, MarbleColor.WHITE)
        )
        grid[0][3] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.LIGHT_PINK, MarbleColor.YELLOW, MarbleColor.CYAN)
        )

        grid[1][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, isMystery = true)
        grid[1][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED, isMystery = true)
        grid[1][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.WHITE, isMystery = true)
        grid[1][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, isMystery = true)

        grid[2][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME, isMystery = true)
        grid[2][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.WHITE, isMystery = true)
        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED, isMystery = true)
        grid[2][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, isMystery = true)
        grid[2][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK, isMystery = true)
        grid[2][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN, isMystery = true)

        grid[3][0] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.LIME, MarbleColor.PURPLE, MarbleColor.WHITE)
        )
        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, isMystery = true)
        grid[3][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MAROON, isMystery = true)
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN, isMystery = true)
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.WHITE, isMystery = true)
        grid[3][5] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.RED, MarbleColor.LIGHT_PINK, MarbleColor.YELLOW)
        )

        grid[4][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN)
        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.MAROON)
        grid[4][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.WHITE)

        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIME)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.CYAN)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIME)
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.PURPLE)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.LIME, MarbleColor.RED, MarbleColor.LIME, MarbleColor.LIGHT_PINK),
            seed = 3030L
        )
        return LevelData(30, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel41(): LevelData {
        val grid = newWallGrid()
        // U-topped 4-column chamber (r=1..5, c=1..4) introducing connected pairs (linkGroups 1..4)
        grid[1][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE, linkGroup = 1)
        grid[1][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED, linkGroup = 2)

        grid[2][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE, linkGroup = 1)
        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[2][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME)
        grid[2][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, linkGroup = 2)

        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)
        grid[3][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)

        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, linkGroup = 3)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, linkGroup = 3)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE, linkGroup = 4)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME, linkGroup = 4)

        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.CYAN)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.PURPLE)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIME)
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.ORANGE)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.LIME, MarbleColor.ORANGE, MarbleColor.ORANGE, MarbleColor.BLUE),
            seed = 4141L
        )
        return LevelData(41, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel42(): LevelData {
        val grid = newWallGrid()
        // Asymmetric 5-column (c=1..5) staircase layout with 5 connected pairs (linkGroups 1..5)
        grid[0][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN)
        grid[0][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, linkGroup = 1)

        grid[1][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[1][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLACK, linkGroup = 1)
        grid[1][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)
        grid[1][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, linkGroup = 2)

        grid[2][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, linkGroup = 3)
        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN, linkGroup = 3)
        grid[2][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)
        grid[2][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLACK, linkGroup = 2)
        grid[2][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN)

        // grid[3][1] remains WALL notch
        grid[3][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK, linkGroup = 4)
        grid[3][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, linkGroup = 4)

        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN, linkGroup = 5)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLACK, linkGroup = 5)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[4][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK)

        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.DARK_GREEN)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.DARK_GREEN)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BLACK)
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIGHT_PINK)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.LIGHT_PINK, MarbleColor.ORANGE, MarbleColor.LIGHT_PINK, MarbleColor.CYAN),
            seed = 4242L
        )
        return LevelData(42, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel43(): LevelData {
        val grid = newWallGrid()
        // Stepped 6-col layout with 2 Dispensers(3) at (0,2),(1,5) & 4 connected pairs (including bottom row (5,2)-(5,3))
        grid[0][2] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.PURPLE, MarbleColor.LIGHT_PINK, MarbleColor.YELLOW)
        )
        grid[0][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[0][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN, linkGroup = 1)

        grid[1][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)
        grid[1][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[1][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK)
        grid[1][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME, linkGroup = 1)
        grid[1][5] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.CYAN, MarbleColor.WHITE, MarbleColor.RED)
        )

        grid[2][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK, linkGroup = 2)
        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[2][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN)
        grid[2][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ICE)
        grid[2][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN)

        grid[3][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN)
        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME, linkGroup = 2)
        grid[3][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN)
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME, linkGroup = 3)
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN, linkGroup = 3)
        grid[3][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)

        grid[4][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.WHITE)
        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[4][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)

        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.WHITE)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BROWN, linkGroup = 4)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.RED, linkGroup = 4)
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.YELLOW)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.YELLOW, MarbleColor.LIGHT_PINK, MarbleColor.WHITE, MarbleColor.ICE),
            seed = 4343L
        )
        return LevelData(43, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel44(): LevelData {
        val grid = newWallGrid()
        // 6-row grid with 2 Dispensers(3) at (1,0),(2,5), 5 vertical connected pairs & mystery crates (incl. (5,3))
        grid[0][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED, linkGroup = 1)
        grid[0][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.WHITE)

        grid[1][0] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.LIME, MarbleColor.ORANGE, MarbleColor.WHITE)
        )
        grid[1][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED, isMystery = true, linkGroup = 1)
        grid[1][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.WHITE, isMystery = true)
        grid[1][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME)
        grid[1][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, isMystery = true, linkGroup = 2)

        grid[2][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.WHITE)
        grid[2][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, linkGroup = 3)
        grid[2][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[2][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, isMystery = true, linkGroup = 2)
        grid[2][5] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.PURPLE, MarbleColor.BLUE, MarbleColor.RED)
        )

        grid[3][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, linkGroup = 4)
        grid[3][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, linkGroup = 3)
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, linkGroup = 5)
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.WHITE)
        grid[3][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME)

        grid[4][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)
        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED, linkGroup = 4)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE, isMystery = true, linkGroup = 5)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)
        grid[4][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)

        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.WHITE)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIME)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.RED, isMystery = true)
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.ORANGE)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.WHITE, MarbleColor.ORANGE, MarbleColor.PURPLE, MarbleColor.WHITE),
            seed = 4444L
        )
        return LevelData(44, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel45(): LevelData {
        val grid = newWallGrid()
        // 6-col chamber with 3 Dispensers(3) at (1,0),(1,4),(3,2) & 4 horizontal connected pairs
        grid[0][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN, linkGroup = 1)
        grid[0][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE, linkGroup = 1)

        grid[1][0] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.LIGHT_PINK, MarbleColor.RED, MarbleColor.BLUE)
        )
        grid[1][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN)
        grid[1][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)
        grid[1][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[1][4] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.PURPLE, MarbleColor.RED, MarbleColor.LIGHT_PINK)
        )

        grid[2][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)
        grid[2][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN)
        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK)
        grid[2][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)
        grid[2][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED, linkGroup = 2)
        grid[2][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK, linkGroup = 2)

        grid[3][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)
        grid[3][2] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.ORANGE, MarbleColor.DARK_GREEN, MarbleColor.CYAN)
        )
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN)
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[3][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)

        grid[4][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN, linkGroup = 3)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, linkGroup = 3)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN, linkGroup = 4)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN, linkGroup = 4)
        grid[4][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)

        grid[5][0] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.CYAN)
        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.CYAN)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIGHT_PINK)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.RED)
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIGHT_PINK)
        grid[5][5] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.ORANGE)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.DARK_GREEN, MarbleColor.DARK_GREEN, MarbleColor.PURPLE, MarbleColor.DARK_GREEN),
            seed = 4545L
        )
        return LevelData(45, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel46(): LevelData {
        val grid = newWallGrid()
        // 6-col brown temple with 1x2 vertical stone pillar at (2,1)-(3,1) & twin Dispensers(3) at (2,2),(2,3)
        grid[0][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.WHITE)
        grid[0][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)
        grid[0][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[0][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLACK)

        grid[1][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.WHITE)
        grid[1][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK)
        grid[1][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)
        grid[1][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME)
        grid[1][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN)
        grid[1][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK)

        grid[2][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        // grid[2][1] & grid[3][1] remain WALL vertical stone pillar
        grid[2][2] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.RED, MarbleColor.BLACK, MarbleColor.PURPLE)
        )
        grid[2][3] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.ORANGE, MarbleColor.BLACK, MarbleColor.RED)
        )
        grid[2][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK)
        grid[2][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN)

        grid[3][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN)
        grid[3][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN)
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME)
        grid[3][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)

        grid[4][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK)
        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLACK)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[4][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)

        grid[5][0] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BLACK)
        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIME)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.ORANGE)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.ORANGE)
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.PURPLE)
        grid[5][5] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BLACK)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.LIGHT_PINK, MarbleColor.PURPLE, MarbleColor.ORANGE, MarbleColor.LIME),
            seed = 4646L
        )
        return LevelData(46, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel47(): LevelData {
        val grid = newWallGrid()
        // Horizontal 2x1 stone beam at (2,2)-(2,3), twin Dispensers(3) at (3,2),(3,3) & 12 mystery crates
        grid[0][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN, isMystery = true)
        grid[0][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN, isMystery = true)
        grid[0][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, isMystery = true)
        grid[0][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED, isMystery = true)

        grid[1][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN)
        grid[1][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[1][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)
        grid[1][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN)
        grid[1][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)
        grid[1][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLACK)

        grid[2][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN, isMystery = true)
        grid[2][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.WHITE, isMystery = true)
        // grid[2][2] & grid[2][3] remain WALL horizontal stone beam
        grid[2][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, isMystery = true)
        grid[2][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN, isMystery = true)

        grid[3][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, isMystery = true)
        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN, isMystery = true)
        grid[3][2] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.RED, MarbleColor.YELLOW, MarbleColor.DARK_GREEN)
        )
        grid[3][3] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.BLACK, MarbleColor.PURPLE, MarbleColor.CYAN)
        )
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN, isMystery = true)
        grid[3][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED, isMystery = true)

        grid[4][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.WHITE)

        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.PURPLE)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BLACK)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BROWN)
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.DARK_GREEN)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.BLACK, MarbleColor.BLACK, MarbleColor.RED, MarbleColor.DARK_GREEN),
            seed = 4747L
        )
        return LevelData(47, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel48(): LevelData {
        val grid = newWallGrid()
        // Split-tower twin wings flanking 2x3 center WALL (0..2, 2..3) & twin Dispensers(4) at (3,2),(3,3)
        grid[0][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED, isMystery = true)
        grid[0][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)
        grid[0][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME)
        grid[0][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE, isMystery = true)

        grid[1][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)
        grid[1][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK, isMystery = true)
        grid[1][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN, isMystery = true)
        grid[1][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)

        grid[2][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE, isMystery = true)
        grid[2][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)
        grid[2][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME)
        grid[2][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED, isMystery = true)

        grid[3][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK, isMystery = true)
        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, isMystery = true)
        grid[3][2] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 4,
            queue = mutableListOf(MarbleColor.RED, MarbleColor.BLUE, MarbleColor.CYAN, MarbleColor.ORANGE)
        )
        grid[3][3] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 4,
            queue = mutableListOf(MarbleColor.LIGHT_PINK, MarbleColor.CYAN, MarbleColor.PURPLE, MarbleColor.RED)
        )
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED, isMystery = true)
        grid[3][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, isMystery = true)

        grid[4][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[4][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)

        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIGHT_PINK)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.ORANGE)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIME)
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.RED)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.PURPLE, MarbleColor.RED, MarbleColor.LIGHT_PINK, MarbleColor.RED),
            seed = 4848L
        )
        return LevelData(48, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel49(): LevelData {
        val grid = newWallGrid()
        // 6-col chamber with bottom stone notch (5,2), 2 Dispensers(3) at (1,0),(2,5) & 3 horizontal linked pairs
        grid[0][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, linkGroup = 1)
        grid[0][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, linkGroup = 1)
        grid[0][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME)
        grid[0][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)

        grid[1][0] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.LIME, MarbleColor.RED, MarbleColor.LIGHT_PINK)
        )
        grid[1][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME)
        grid[1][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[1][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[1][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN)

        grid[2][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN)
        grid[2][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME, linkGroup = 2)
        grid[2][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN, linkGroup = 2)
        grid[2][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK)
        grid[2][5] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.RED, MarbleColor.BROWN, MarbleColor.LIME)
        )

        grid[3][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[3][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[3][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE)
        grid[3][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)

        grid[4][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME)
        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIGHT_PINK, linkGroup = 3)
        grid[4][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED, linkGroup = 3)

        grid[5][0] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.YELLOW)
        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIGHT_PINK)
        // grid[5][2] remains WALL center stone notch
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.ORANGE)
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.PURPLE)
        grid[5][5] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.ORANGE)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.PURPLE, MarbleColor.RED, MarbleColor.LIME, MarbleColor.BROWN),
            seed = 4949L
        )
        return LevelData(49, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun getLevel50(): LevelData {
        val grid = newWallGrid()
        // 6x6 Grand Finale fortress with L-shaped stone core (2,2),(3,2),(3,3), top corner Dispensers(3) & 12 mystery crates
        grid[0][0] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.RED, MarbleColor.CYAN, MarbleColor.BROWN)
        )
        grid[0][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME)
        grid[0][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE, isMystery = true)
        grid[0][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, isMystery = true)
        grid[0][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN)
        grid[0][5] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 3,
            queue = mutableListOf(MarbleColor.YELLOW, MarbleColor.ORANGE, MarbleColor.PURPLE)
        )

        grid[1][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[1][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[1][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN, isMystery = true)
        grid[1][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN, isMystery = true)
        grid[1][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN)
        grid[1][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)

        grid[2][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.DARK_GREEN, isMystery = true)
        grid[2][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED, isMystery = true)
        // grid[2][2], grid[3][2], grid[3][3] remain WALL L-shaped center core
        grid[2][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN)
        grid[2][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.LIME, isMystery = true)
        grid[2][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, isMystery = true)

        grid[3][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED, isMystery = true)
        grid[3][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BLUE, isMystery = true)
        grid[3][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, isMystery = true)
        grid[3][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, isMystery = true)

        grid[4][0] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE)
        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.CYAN)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[4][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE)
        grid[4][5] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.BROWN)

        grid[5][0] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BLUE)
        grid[5][1] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.LIME)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.DARK_GREEN)
        grid[5][3] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.DARK_GREEN)
        grid[5][4] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.YELLOW)
        grid[5][5] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.YELLOW)

        val columns = buildBalancedColumns(
            grid = grid,
            exactFrontRow = listOf(MarbleColor.RED, MarbleColor.YELLOW, MarbleColor.BLUE, MarbleColor.ORANGE),
            seed = 5050L
        )
        return LevelData(50, grid, columns, GameConstants.BALLS_PER_BLOCK)
    }

    fun generateSolvableLevel(levelNum: Int): LevelData {
        return when (levelNum) {
            1 -> getLevel1()
            2 -> getLevel2()
            3 -> getLevel3()
            4 -> getLevel4()
            5 -> getLevel5()
            6 -> getLevel6()
            7 -> getLevel7()
            8 -> getLevel8()
            9 -> getLevel9()
            10 -> getLevel10()
            11 -> getLevel11()
            12 -> getLevel12()
            13 -> getLevel13()
            14 -> getLevel14()
            15 -> getLevel15()
            16 -> getLevel16()
            17 -> getLevel17()
            18 -> getLevel18()
            19 -> getLevel19()
            20 -> getLevel20()
            21 -> getLevel21()
            22 -> getLevel22()
            23 -> getLevel23()
            24 -> getLevel24()
            25 -> getLevel25()
            26 -> getLevel26()
            27 -> getLevel27()
            28 -> getLevel28()
            29 -> getLevel29()
            30 -> getLevel30()
            31 -> getLevel31()
            32 -> getLevel32()
            33 -> getLevel33()
            34 -> getLevel34()
            35 -> getLevel35()
            36 -> getLevel36()
            37 -> getLevel37()
            38 -> getLevel38()
            39 -> getLevel39()
            40 -> getLevel40()
            41 -> getLevel41()
            42 -> getLevel42()
            43 -> getLevel43()
            44 -> getLevel44()
            45 -> getLevel45()
            46 -> getLevel46()
            47 -> getLevel47()
            48 -> getLevel48()
            49 -> getLevel49()
            50 -> getLevel50()
            else -> generateProceduralLevel(levelNum)
        }
    }

    private fun generateProceduralLevel(levelNum: Int): LevelData {
        val colors = mutableListOf(
            MarbleColor.PURPLE, MarbleColor.PINK, MarbleColor.ICE,
            MarbleColor.YELLOW, MarbleColor.LIME, MarbleColor.CYAN,
            MarbleColor.ORANGE, MarbleColor.RED,
            MarbleColor.BLACK, MarbleColor.DARK_GREEN, MarbleColor.BLUE,
            MarbleColor.BROWN, MarbleColor.MINT, MarbleColor.WHITE,
            MarbleColor.MAROON, MarbleColor.LIGHT_PINK
        )

        val grid = newWallGrid()

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

        // Add Cave Dispensers for level >= 41
        if (levelNum >= 41) {
            val numDispensers = 2
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
