package com.example

import com.example.game.constants.GameConstants
import com.example.game.levels.LevelGenerator
import com.example.game.model.CellKind
import com.example.game.model.MarbleColor
import com.example.game.rules.GameRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun level1_isValid() {
        val lvl = LevelGenerator.getLevel1()
        assertEquals(1, lvl.levelNumber)
        assertEquals(4, lvl.columns.size)
        // Exit row blocks are exposed
        assertTrue(GameRules.isExposed(lvl.grid, 5, 2))
        assertTrue(GameRules.isExposed(lvl.grid, 5, 3))
    }

    @Test
    fun level35_isSolvableConfiguration() {
        val lvl = LevelGenerator.getLevel35()
        assertEquals(35, lvl.levelNumber)
        assertEquals(4, lvl.columns.size)
        // Exit row blocks are exposed
        assertTrue(GameRules.isExposed(lvl.grid, 5, 1))
        assertTrue(GameRules.isExposed(lvl.grid, 5, 2))
    }

    @Test
    fun allLevels_1to50_haveExactBlockToBucketBalance() {
        val bucketsPerBlock = GameConstants.BALLS_PER_BLOCK / GameConstants.BRICK_HOLES // 9 / 3 = 3
        for (levelNum in 1..50) {
            val lvl = LevelGenerator.generateSolvableLevel(levelNum)

            val blockCounts = mutableMapOf<MarbleColor, Int>()
            MarbleColor.entries.forEach { blockCounts[it] = 0 }

            for (r in 0 until GameConstants.GRID_ROWS) {
                for (c in 0 until GameConstants.GRID_COLS) {
                    val cell = lvl.grid[r][c]
                    if (cell.kind == CellKind.BLOCK || cell.kind == CellKind.LOCKED) {
                        val color = cell.color!!
                        blockCounts[color] = (blockCounts[color] ?: 0) + 1
                    } else if (cell.kind == CellKind.DISPENSER) {
                        cell.queue.forEach { qColor ->
                            blockCounts[qColor] = (blockCounts[qColor] ?: 0) + 1
                        }
                    }
                }
            }

            val bucketCounts = mutableMapOf<MarbleColor, Int>()
            MarbleColor.entries.forEach { bucketCounts[it] = 0 }
            lvl.columns.forEach { col ->
                col.forEach { bColor ->
                    bucketCounts[bColor] = (bucketCounts[bColor] ?: 0) + 1
                }
            }

            for (color in MarbleColor.entries) {
                assertEquals(
                    "Level $levelNum color $color bucket count mismatch",
                    (blockCounts[color] ?: 0) * bucketsPerBlock,
                    bucketCounts[color] ?: 0
                )
            }
        }
    }

    @Test
    fun level2_isDeterministicAndPlayable() {
        val a = LevelGenerator.generateSolvableLevel(2)
        val b = LevelGenerator.generateSolvableLevel(2)
        assertEquals(a.columns, b.columns)
        for (r in 0 until GameConstants.GRID_ROWS) {
            for (c in 0 until GameConstants.GRID_COLS) {
                assertEquals(a.grid[r][c].kind, b.grid[r][c].kind)
                assertEquals(a.grid[r][c].color, b.grid[r][c].color)
            }
        }
        assertTrue(GameRules.isExposed(a.grid, 5, 1))
        assertTrue(GameRules.isExposed(a.grid, 5, 2))
        assertTrue(GameRules.isExposed(a.grid, 5, 3))
        assertTrue(GameRules.isExposed(a.grid, 5, 4))
    }

    @Test
    fun funnelLimit_blocksAt30AndUnlocksAt29() {
        assertEquals(30, GameConstants.MAX_WAITING_BALLS)
        assertEquals(29, GameConstants.RESUME_WAITING_BALLS)
        // Below 30 -> allowed
        assertEquals(false, GameRules.shouldBlockBlockTap(27, currentlyLocked = false))
        assertEquals(false, GameRules.shouldBlockBlockTap(29, currentlyLocked = false))
        // Hits 30 or above -> blocked
        assertEquals(true, GameRules.shouldBlockBlockTap(30, currentlyLocked = false))
        assertEquals(true, GameRules.shouldBlockBlockTap(36, currentlyLocked = false))
        // Stays blocked at 30 while locked, unlocks as soon as 29 remain
        assertEquals(true, GameRules.shouldBlockBlockTap(30, currentlyLocked = true))
        assertEquals(false, GameRules.shouldBlockBlockTap(29, currentlyLocked = true))
    }
}
