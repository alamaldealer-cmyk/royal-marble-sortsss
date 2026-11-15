package com.example

import com.example.game.constants.GameConstants
import com.example.game.levels.LevelGenerator
import com.example.game.model.BeltBall
import com.example.game.model.Brick
import com.example.game.model.BrickColumn
import com.example.game.model.CellKind
import com.example.game.model.GridCell
import com.example.game.model.MarbleColor
import com.example.game.rules.GameRules
import com.example.game.story.StoryCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun storyCatalog_hasTenChaptersEveryTenLevels() {
        assertEquals(10, StoryCatalog.chapters.size)

        val expectedTriggers = listOf(1, 11, 21, 31, 41, 51, 61, 71, 81, 91)
        expectedTriggers.forEachIndexed { idx, lvl ->
            val episode = StoryCatalog.storyForStartingLevel(lvl)
            assertNotNull("Expected story at level $lvl", episode)
            assertEquals(idx + 1, episode!!.chapterNumber)
            assert(episode.beats.isNotEmpty())
        }

        // Intermediate levels should not trigger a story
        listOf(2, 5, 10, 15, 20, 50, 99, 100).forEach { lvl ->
            assertNull("Unexpected story at level $lvl", StoryCatalog.storyForStartingLevel(lvl))
        }
    }

    @Test
    fun magnetReturn_sixLimeBalls_assignsSingleContainerWithSixSlots() {
        val grid = Array(GameConstants.GRID_ROWS) {
            Array(GameConstants.GRID_COLS) { GridCell(kind = CellKind.WALL) }
        }
        grid[5][2] = GridCell(kind = CellKind.EMPTY)
        grid[5][3] = GridCell(kind = CellKind.EMPTY)

        val belt = (1..6).map { id ->
            BeltBall(id = id, color = MarbleColor.LIME, slotIndex = id, s = id * 14f)
        }

        val assignments = GameRules.planMagnetReturn(grid, belt)
        assertEquals(6, assignments.size)

        val targetCell = assignments.first().let { grid[it.targetRow][it.targetCol] }
        assertEquals(CellKind.BLOCK, targetCell.kind)
        assertEquals(MarbleColor.LIME, targetCell.color)
        assertEquals("magnet_receiving", targetCell.animState)
        assertEquals(listOf(0, 1, 2, 3, 4, 5), assignments.map { it.targetSlotIndex })
        assertTrue(assignments.all { it.targetRow == 5 && it.targetCol == 2 })
    }

    @Test
    fun magnetReturn_mixedColorsOverNine_splitsCleanlyIntoExactContainers() {
        val grid = Array(GameConstants.GRID_ROWS) {
            Array(GameConstants.GRID_COLS) { GridCell(kind = CellKind.EMPTY) }
        }

        val belt = buildList {
            for (i in 1..6) add(BeltBall(id = i, color = MarbleColor.LIME, slotIndex = i, s = i * 10f))
            for (i in 7..17) add(BeltBall(id = i, color = MarbleColor.PINK, slotIndex = i, s = i * 10f))
        }

        val assignments = GameRules.planMagnetReturn(grid, belt)
        assertEquals(17, assignments.size)

        val byTarget = assignments.groupBy { Pair(it.targetRow, it.targetCol) }
        assertEquals(3, byTarget.size) // 1 LIME (6) + 2 PINK (9 + 2)
        val counts = byTarget.values.map { it.size }.sorted()
        assertEquals(listOf(2, 6, 9), counts)
    }

    @Test
    fun capacityAndBallSize_updatedToTwentyFourWithLargerBalls() {
        assertEquals(24, GameConstants.BELT_CAPACITY)
        assertEquals(24, GameConstants.MAX_WAITING_BALLS)
        assertEquals(23, GameConstants.RESUME_WAITING_BALLS)
        assertTrue(GameConstants.BALL_RADIUS_BELT > 7.0f)
        assertTrue(GameConstants.BALL_RADIUS_FUNNEL > 7.0f)
        assertTrue(GameRules.shouldBlockBlockTap(24, false))
        assertTrue(GameRules.shouldBlockBlockTap(24, true))
        assertTrue(!GameRules.shouldBlockBlockTap(23, true))
    }

    @Test
    fun caveDispenser_ejectsBucketWithAnimationStateAndOffset() {
        val grid = Array(GameConstants.GRID_ROWS) {
            Array(GameConstants.GRID_COLS) { GridCell(kind = CellKind.WALL) }
        }
        grid[3][0] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 2,
            queue = mutableListOf(MarbleColor.CYAN, MarbleColor.YELLOW)
        )
        grid[4][0] = GridCell(kind = CellKind.EMPTY)

        val result = GameRules.updateGridAfterChange(grid)
        assertEquals(1, result.dispensedCells.size)
        assertEquals(MarbleColor.CYAN, result.dispensedCells.first().color)

        val dispenser = grid[3][0]
        assertEquals(1, dispenser.counter)
        assertEquals("cave_dispensing", dispenser.animState)
        assertEquals(MarbleColor.CYAN, dispenser.color)

        val dispensedBlock = grid[4][0]
        assertEquals(CellKind.BLOCK, dispensedBlock.kind)
        assertEquals(MarbleColor.CYAN, dispensedBlock.color)
        assertEquals("dispensing", dispensedBlock.animState)
        assertEquals(-28f, dispensedBlock.offsetY, 0.01f)
    }

    @Test
    fun shufflePowerup_endgameRescuesBucketBehindPartiallyFilledFront() {
        val columns = listOf(
            BrickColumn(
                x = GameConstants.BRICK_COLUMNS_X[0],
                bricks = mutableListOf(
                    Brick(color = MarbleColor.BLUE, filled = 1),
                    Brick(color = MarbleColor.RED, filled = 0)
                )
            ),
            BrickColumn(x = GameConstants.BRICK_COLUMNS_X[1], bricks = mutableListOf()),
            BrickColumn(x = GameConstants.BRICK_COLUMNS_X[2], bricks = mutableListOf()),
            BrickColumn(x = GameConstants.BRICK_COLUMNS_X[3], bricks = mutableListOf())
        )
        val beltBalls = (1..3).map { id ->
            BeltBall(id = id, color = MarbleColor.RED, slotIndex = id, s = id * 15f)
        }

        val res = GameRules.performShufflePowerup(columns, beltBalls, emptyList())
        assertTrue(res.shuffled)
        assertTrue(columns.any { it.bricks.firstOrNull()?.color == MarbleColor.RED })
    }

    @Test
    fun shufflePowerup_returnsFalseWhenOnlySingleBucketRemains() {
        val columns = listOf(
            BrickColumn(
                x = GameConstants.BRICK_COLUMNS_X[0],
                bricks = mutableListOf(Brick(color = MarbleColor.RED, filled = 0))
            ),
            BrickColumn(x = GameConstants.BRICK_COLUMNS_X[1], bricks = mutableListOf()),
            BrickColumn(x = GameConstants.BRICK_COLUMNS_X[2], bricks = mutableListOf()),
            BrickColumn(x = GameConstants.BRICK_COLUMNS_X[3], bricks = mutableListOf())
        )
        val beltBalls = listOf(BeltBall(id = 1, color = MarbleColor.RED, slotIndex = 0, s = 10f))

        val res = GameRules.performShufflePowerup(columns, beltBalls, emptyList())
        assertFalse(res.shuffled)
    }

    @Test
    fun magnetReturn_avoidsDispensersAndKeepsAllReturnedBlocksReachableFromExitRow() {
        val grid = Array(GameConstants.GRID_ROWS) {
            Array(GameConstants.GRID_COLS) { GridCell(kind = CellKind.EMPTY) }
        }
        // Exhausted dispenser & walls matching an endgame grid with obstacles
        grid[5][2] = GridCell(kind = CellKind.WALL)
        grid[3][0] = GridCell(kind = CellKind.DISPENSER, counter = 0)
        grid[4][0] = GridCell(kind = CellKind.WALL)
        grid[2][0] = GridCell(kind = CellKind.EMPTY)

        val belt = listOf(
            BeltBall(id = 1, color = MarbleColor.RED, slotIndex = 0, s = 10f),
            BeltBall(id = 2, color = MarbleColor.BLUE, slotIndex = 1, s = 25f),
            BeltBall(id = 3, color = MarbleColor.LIME, slotIndex = 2, s = 40f)
        )

        val assignments = GameRules.planMagnetReturn(grid, belt)
        assertEquals(3, assignments.size)
        // Every assigned block must be clearable by tapping exposed blocks iteratively
        while (true) {
            val exposedPos = (0 until GameConstants.GRID_ROWS).flatMap { r ->
                (0 until GameConstants.GRID_COLS).mapNotNull { c ->
                    if (grid[r][c].kind == CellKind.BLOCK && GameRules.isExposed(grid, r, c)) Pair(r, c) else null
                }
            }
            if (exposedPos.isEmpty()) break
            for ((r, c) in exposedPos) {
                grid[r][c] = GridCell(kind = CellKind.EMPTY)
            }
        }
        val remainingBlocks = (0 until GameConstants.GRID_ROWS).sumOf { r ->
            (0 until GameConstants.GRID_COLS).count { c -> grid[r][c].kind == CellKind.BLOCK }
        }
        assertEquals(0, remainingBlocks)
    }

    @Test
    fun cannonBlast_assignsFrontAndBehindBucketsWithoutOverfill() {
        val frontBrick = Brick(color = MarbleColor.RED, filled = 2, inFlight = 0)
        val behindBrick = Brick(color = MarbleColor.BLUE, filled = 0, inFlight = 0)
        val columns = listOf(
            BrickColumn(x = GameConstants.BRICK_COLUMNS_X[0], bricks = mutableListOf(frontBrick, behindBrick)),
            BrickColumn(x = GameConstants.BRICK_COLUMNS_X[1], bricks = mutableListOf()),
            BrickColumn(x = GameConstants.BRICK_COLUMNS_X[2], bricks = mutableListOf()),
            BrickColumn(x = GameConstants.BRICK_COLUMNS_X[3], bricks = mutableListOf())
        )
        val beltBalls = listOf(
            BeltBall(id = 1, color = MarbleColor.RED, slotIndex = 0, s = 10f),
            BeltBall(id = 2, color = MarbleColor.BLUE, slotIndex = 1, s = 25f),
            BeltBall(id = 3, color = MarbleColor.BLUE, slotIndex = 2, s = 40f),
            BeltBall(id = 4, color = MarbleColor.BLUE, slotIndex = 3, s = 55f)
        )

        val assignments = GameRules.planCannonBlast(columns, beltBalls)
        assertEquals(4, assignments.size)
        assertEquals(1, frontBrick.inFlight)
        assertEquals(3, behindBrick.inFlight)
        assertTrue(assignments.all { it.targetBrick != null })
    }

    @Test
    fun levels1To100_haveExactBallAndBucketBalance() {
        for (lvl in 1..100) {
            val data = LevelGenerator.generateSolvableLevel(lvl)
            val blockBallsByColor = mutableMapOf<MarbleColor, Int>()
            for (r in 0 until GameConstants.GRID_ROWS) {
                for (c in 0 until GameConstants.GRID_COLS) {
                    val cell = data.grid[r][c]
                    when (cell.kind) {
                        CellKind.BLOCK, CellKind.LOCKED -> {
                            val color = cell.color!!
                            blockBallsByColor[color] = (blockBallsByColor[color] ?: 0) + data.ballsPerBlock
                        }
                        CellKind.DISPENSER -> {
                            assertEquals("Dispenser counter mismatch on level $lvl", cell.queue.size, cell.counter)
                            for (qColor in cell.queue) {
                                blockBallsByColor[qColor] = (blockBallsByColor[qColor] ?: 0) + data.ballsPerBlock
                            }
                        }
                        else -> Unit
                    }
                }
            }

            val bucketHolesByColor = mutableMapOf<MarbleColor, Int>()
            for (col in data.columns) {
                for (color in col) {
                    bucketHolesByColor[color] = (bucketHolesByColor[color] ?: 0) + GameConstants.BRICK_HOLES
                }
            }
            assertEquals("Color ball/bucket mismatch on level $lvl", blockBallsByColor, bucketHolesByColor)
        }
    }
}
