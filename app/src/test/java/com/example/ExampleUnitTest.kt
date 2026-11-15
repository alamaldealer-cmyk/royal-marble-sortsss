package com.example

import com.example.game.constants.GameConstants
import com.example.game.levels.LevelGenerator
import com.example.game.model.BeltBall
import com.example.game.model.Brick
import com.example.game.model.BrickColumn
import com.example.game.model.CellKind
import com.example.game.model.DispenserDirection
import com.example.game.model.GridCell
import com.example.game.model.MarbleColor
import com.example.game.rules.GameRules
import com.example.game.story.StoryCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
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
    fun capacityAndBallSize_upgradeFromTwentyFourToThirtyAfterLevelFifteen() {
        assertEquals(24, GameConstants.BELT_CAPACITY)
        assertEquals(24, GameConstants.MAX_WAITING_BALLS)
        assertEquals(23, GameConstants.RESUME_WAITING_BALLS)
        assertEquals(30, GameConstants.UPGRADED_BELT_CAPACITY)
        assertEquals(30, GameConstants.UPGRADED_MAX_WAITING_BALLS)
        assertEquals(29, GameConstants.UPGRADED_RESUME_WAITING_BALLS)
        assertTrue(GameConstants.BALL_RADIUS_BELT > 7.0f)
        assertTrue(GameConstants.BALL_RADIUS_FUNNEL > 7.0f)

        // Levels 1..15 stay at 24
        for (lvl in 1..15) {
            assertEquals(24, GameConstants.beltCapacityForLevel(lvl))
            assertEquals(24, GameConstants.maxWaitingBallsForLevel(lvl))
            assertEquals(23, GameConstants.resumeWaitingBallsForLevel(lvl))
        }
        assertTrue(GameRules.shouldBlockBlockTap(24, false))
        assertTrue(GameRules.shouldBlockBlockTap(24, true))
        assertTrue(!GameRules.shouldBlockBlockTap(23, true))

        // Levels 16+ expand to 30 on both chain and funnel
        listOf(16, 17, 25, 40, 50).forEach { lvl ->
            val cap = GameConstants.beltCapacityForLevel(lvl)
            val maxWait = GameConstants.maxWaitingBallsForLevel(lvl)
            val resumeWait = GameConstants.resumeWaitingBallsForLevel(lvl)
            assertEquals(30, cap)
            assertEquals(30, maxWait)
            assertEquals(29, resumeWait)
            assertTrue(!GameRules.shouldBlockBlockTap(24, false, maxWait, resumeWait))
            assertTrue(!GameRules.shouldBlockBlockTap(29, false, maxWait, resumeWait))
            assertTrue(GameRules.shouldBlockBlockTap(30, false, maxWait, resumeWait))
            assertTrue(GameRules.shouldBlockBlockTap(30, true, maxWait, resumeWait))
            assertTrue(!GameRules.shouldBlockBlockTap(29, true, maxWait, resumeWait))
        }
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
    fun directionalCaveDispensers_ejectLeftAndRightWithSideOffsets() {
        val grid = Array(GameConstants.GRID_ROWS) {
            Array(GameConstants.GRID_COLS) { GridCell(kind = CellKind.WALL) }
        }
        grid[2][0] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 1,
            direction = DispenserDirection.RIGHT,
            queue = mutableListOf(MarbleColor.ORANGE)
        )
        grid[2][1] = GridCell(kind = CellKind.EMPTY)
        grid[2][4] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 1,
            direction = DispenserDirection.LEFT,
            queue = mutableListOf(MarbleColor.LIME)
        )
        grid[2][3] = GridCell(kind = CellKind.EMPTY)

        val result = GameRules.updateGridAfterChange(grid)
        assertEquals(2, result.dispensedCells.size)

        val rightDispensed = grid[2][1]
        assertEquals(CellKind.BLOCK, rightDispensed.kind)
        assertEquals(MarbleColor.ORANGE, rightDispensed.color)
        assertEquals(-28f, rightDispensed.offsetX, 0.01f)
        assertEquals(0f, rightDispensed.offsetY, 0.01f)

        val leftDispensed = grid[2][3]
        assertEquals(CellKind.BLOCK, leftDispensed.kind)
        assertEquals(MarbleColor.LIME, leftDispensed.color)
        assertEquals(28f, leftDispensed.offsetX, 0.01f)
        assertEquals(0f, leftDispensed.offsetY, 0.01f)
    }

    @Test
    fun cannonBlast_targetsFrontAndBehindBucketsAndClearsAllBeltBalls() {
        val frontRed = Brick(color = MarbleColor.RED, filled = 1, inFlight = 0)
        val behindRed = Brick(color = MarbleColor.RED, filled = 0, inFlight = 0)
        val frontCyan = Brick(color = MarbleColor.CYAN, filled = 0, inFlight = 0)
        val deepCyan = Brick(color = MarbleColor.CYAN, filled = 0, inFlight = 0)

        val columns = listOf(
            BrickColumn(x = 50f, bricks = mutableListOf(frontRed, deepCyan)),
            BrickColumn(x = 115f, bricks = mutableListOf(frontCyan, behindRed))
        )

        // 4 RED balls (2 go to frontRed since filled=1, 2 spill into behindRed at depth 1!)
        // 1 YELLOW ball (no matching bucket -> targetBrick null, still included to clear belt 100%)
        val belt = listOf(
            BeltBall(id = 1, color = MarbleColor.RED, slotIndex = 0, s = 10f),
            BeltBall(id = 2, color = MarbleColor.RED, slotIndex = 1, s = 20f),
            BeltBall(id = 3, color = MarbleColor.RED, slotIndex = 2, s = 30f),
            BeltBall(id = 4, color = MarbleColor.RED, slotIndex = 3, s = 40f),
            BeltBall(id = 5, color = MarbleColor.YELLOW, slotIndex = 4, s = 50f)
        )

        val assignments = GameRules.planCannonBlast(columns, belt)
        assertEquals(5, assignments.size)
        assertEquals(belt.map { it.id }, assignments.map { it.beltBall.id })

        // First two RED balls fill remaining 2 holes of frontRed
        assertSame(frontRed, assignments[0].targetBrick)
        assertSame(frontRed, assignments[1].targetBrick)
        assertEquals(2, frontRed.inFlight)

        // Next two RED balls seamlessly target behindRed (queued behind frontCyan!)
        assertSame(behindRed, assignments[2].targetBrick)
        assertSame(behindRed, assignments[3].targetBrick)
        assertEquals(1, assignments[2].targetCol)
        assertEquals(2, behindRed.inFlight)

        // Unmatched YELLOW ball is still returned for firework pop with no bucket target
        assertNull(assignments[4].targetBrick)
        assertEquals(-1, assignments[4].targetCol)
    }

    @Test
    fun levelsOneThroughFifty_areBalancedAndFullyClearable() {
        for (lvl in 1..50) {
            val data = LevelGenerator.generateSolvableLevel(lvl)
            assertEquals(4, data.columns.size)
            assertTrue("Level $lvl has empty column", data.columns.all { it.isNotEmpty() })

            // 1. Exact 1:3 block-to-bucket balance per color
            val expectedBucketsByColor = mutableMapOf<MarbleColor, Int>()
            for (r in 0 until GameConstants.GRID_ROWS) {
                for (c in 0 until GameConstants.GRID_COLS) {
                    val cell = data.grid[r][c]
                    if (cell.kind == CellKind.BLOCK || cell.kind == CellKind.LOCKED) {
                        val col = cell.color!!
                        expectedBucketsByColor[col] = (expectedBucketsByColor[col] ?: 0) + 3
                    } else if (cell.kind == CellKind.DISPENSER) {
                        cell.queue.forEach { qCol ->
                            expectedBucketsByColor[qCol] = (expectedBucketsByColor[qCol] ?: 0) + 3
                        }
                    }
                }
            }
            val actualBucketsByColor = data.columns.flatten().groupingBy { it }.eachCount()
            assertEquals("Level $lvl bucket balance mismatch", expectedBucketsByColor, actualBucketsByColor)

            // 2. Simulate clearing all exposed blocks (including linked pairs) step-by-step -> 0 blocks/locked/queued left
            val simGrid = Array(GameConstants.GRID_ROWS) { r ->
                Array(GameConstants.GRID_COLS) { c -> data.grid[r][c].deepCopy() }
            }
            var steps = 0
            while (steps < 120) {
                var clearedAny = false
                for (r in 0 until GameConstants.GRID_ROWS) {
                    for (c in 0 until GameConstants.GRID_COLS) {
                        if (GameRules.isExposed(simGrid, r, c)) {
                            val burst = GameRules.collectLinkedBurstCells(simGrid, r, c)
                            for ((br, bc) in burst) {
                                simGrid[br][bc] = GridCell(kind = CellKind.EMPTY)
                            }
                            GameRules.updateGridAfterChange(simGrid)
                            clearedAny = true
                            steps++
                        }
                    }
                }
                if (!clearedAny) break
            }
            for (r in 0 until GameConstants.GRID_ROWS) {
                for (c in 0 until GameConstants.GRID_COLS) {
                    val cell = simGrid[r][c]
                    assertTrue(
                        "Level $lvl cell ($r,$c) stuck as ${cell.kind} counter=${cell.counter}",
                        cell.kind == CellKind.WALL || cell.kind == CellKind.EMPTY ||
                            (cell.kind == CellKind.DISPENSER && (cell.counter ?: 0) == 0)
                    )
                }
            }
        }
    }

    @Test
    fun connectedCrates_openingOneAutomaticallyOpensPartner() {
        val grid = Array(GameConstants.GRID_ROWS) {
            Array(GameConstants.GRID_COLS) { GridCell(kind = CellKind.WALL) }
        }
        // Row 5 (exit row): exposed BLOCK at (5,1), linked horizontally to LOCKED (5,2)? No, put vertical pair (4,2)-(5,2) and hidden pair (2,2)-(2,3)
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BLUE, linkGroup = 1)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, linkGroup = 1)
        // Unexposed linked pair higher up
        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.ORANGE, linkGroup = 2)
        grid[2][3] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED, linkGroup = 2)

        assertTrue(GameRules.isExposed(grid, 5, 2))
        assertTrue(GameRules.isExposed(grid, 4, 2))
        assertTrue(!GameRules.isExposed(grid, 2, 2))
        assertTrue(!GameRules.isExposed(grid, 2, 3))

        val burst = GameRules.collectLinkedBurstCells(grid, 5, 2)
        assertEquals(setOf(Pair(5, 2), Pair(4, 2)), burst.toSet())
        for ((br, bc) in burst) {
            grid[br][bc] = GridCell(kind = CellKind.EMPTY)
        }
        GameRules.updateGridAfterChange(grid)
        assertEquals(CellKind.EMPTY, grid[5][2].kind)
        assertEquals(CellKind.EMPTY, grid[4][2].kind)
    }

    @Test
    fun shufflePowerup_endgameRescuesBucketBehindPartiallyFilledFront() {
        val frontPartialBlue = Brick(color = MarbleColor.BLUE, filled = 2, inFlight = 0)
        val trappedRed = Brick(color = MarbleColor.RED, filled = 0, inFlight = 0)
        val columns = listOf(
            BrickColumn(x = 69f, bricks = mutableListOf(frontPartialBlue, trappedRed)),
            BrickColumn(x = 122f, bricks = mutableListOf()),
            BrickColumn(x = 175f, bricks = mutableListOf()),
            BrickColumn(x = 227f, bricks = mutableListOf())
        )
        val belt = listOf(BeltBall(id = 1, color = MarbleColor.RED, slotIndex = 0, s = 20f))

        val res = GameRules.performShufflePowerup(columns, belt, emptyList())
        assertTrue(res.shuffled)
        assertEquals(MarbleColor.RED, columns[1].bricks.firstOrNull()?.color)
        assertEquals(MarbleColor.BLUE, columns[0].bricks.firstOrNull()?.color)
    }

    @Test
    fun shufflePowerup_returnsFalseWhenOnlySingleBucketRemains() {
        val onlyBucket = Brick(color = MarbleColor.PURPLE, filled = 1, inFlight = 0)
        val columns = listOf(
            BrickColumn(x = 69f, bricks = mutableListOf(onlyBucket)),
            BrickColumn(x = 122f, bricks = mutableListOf()),
            BrickColumn(x = 175f, bricks = mutableListOf()),
            BrickColumn(x = 227f, bricks = mutableListOf())
        )
        val res = GameRules.performShufflePowerup(columns, emptyList(), emptyList())
        assertTrue(!res.shuffled)
    }

    @Test
    fun magnetReturn_avoidsDispensersAndKeepsAllReturnedBlocksReachableFromExitRow() {
        val grid = Array(GameConstants.GRID_ROWS) {
            Array(GameConstants.GRID_COLS) { GridCell(kind = CellKind.WALL) }
        }
        // Dispensers blocking upper row 2, open path along row 5 -> (4,2)
        grid[2][2] = GridCell(kind = CellKind.DISPENSER, counter = 0)
        grid[2][3] = GridCell(kind = CellKind.DISPENSER, counter = 0)
        grid[4][2] = GridCell(kind = CellKind.EMPTY)
        grid[5][2] = GridCell(kind = CellKind.EMPTY)

        val belt = (1..12).map { id ->
            BeltBall(id = id, color = MarbleColor.CYAN, slotIndex = id, s = id * 10f)
        }
        val assignments = GameRules.planMagnetReturn(grid, belt)
        assertEquals(12, assignments.size)
        for (a in assignments) {
            assertTrue("Should not overwrite dispenser", a.targetRow != 2)
        }
        assertTrue(GameRules.isExposed(grid, 5, 2))
    }

    @Test
    fun levels1To100_haveExactBallAndBucketBalance() {
        for (lvl in 1..100) {
            val data = LevelGenerator.generateSolvableLevel(lvl)
            val totalGridBlocks = (0 until GameConstants.GRID_ROWS).sumOf { r ->
                (0 until GameConstants.GRID_COLS).count { c ->
                    val cell = data.grid[r][c]
                    cell.kind == CellKind.BLOCK || cell.kind == CellKind.LOCKED
                }
            } + (0 until GameConstants.GRID_ROWS).sumOf { r ->
                (0 until GameConstants.GRID_COLS).sumOf { c ->
                    val cell = data.grid[r][c]
                    if (cell.kind == CellKind.DISPENSER) cell.queue.size else 0
                }
            }
            assertEquals("Level $lvl total bucket count mismatch", totalGridBlocks * 3, data.columns.flatten().size)
        }
    }

    @Test
    fun allSixteenColors_appearInLevelsOneThroughFifty() {
        val seenColors = mutableSetOf<MarbleColor>()
        for (lvl in 1..50) {
            val data = LevelGenerator.generateSolvableLevel(lvl)
            seenColors.addAll(data.columns.flatten())
        }
        assertEquals(MarbleColor.entries.toSet(), seenColors)
    }

    @Test
    fun sixteenColors_haveDefinitionsAndDisplayNames() {
        assertEquals(16, MarbleColor.entries.size)
        val displayNames = mutableSetOf<String>()
        for (color in MarbleColor.entries) {
            assertNotNull("Missing COLOR_MAP entry for $color", GameConstants.COLOR_MAP[color])
            val label = GameConstants.colorDisplayName(color)
            assertTrue("Empty label for $color", label.isNotBlank())
            assertTrue("Duplicate label $label", displayNames.add(label))
            assertEquals(color, MarbleColor.fromKey(color.key))
        }
    }

    @Test
    fun dispenserRefill_keepsAdjacentLockedBucketCoveredUntilDispenserEmpties() {
        val grid = Array(GameConstants.GRID_ROWS) {
            Array(GameConstants.GRID_COLS) { GridCell(kind = CellKind.WALL) }
        }
        // Dispenser at (3,5) facing DOWN toward (4,5); adjacent LOCKED mystery bucket at (4,4)
        grid[3][5] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 1,
            queue = mutableListOf(MarbleColor.ORANGE)
        )
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, isMystery = true)
        grid[4][5] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BLUE)
        grid[5][5] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.RED)

        // 1. Tap front block (4,5) while dispenser still has 1 bucket left -> dispenser refills (4,5)
        grid[4][5] = GridCell(kind = CellKind.EMPTY)
        val firstRes = GameRules.updateGridAfterChange(grid)
        assertEquals(1, firstRes.dispensedCells.size)
        assertTrue(firstRes.revealedCells.isEmpty())
        assertEquals(CellKind.BLOCK, grid[4][5].kind)
        assertEquals(MarbleColor.ORANGE, grid[4][5].color)
        assertEquals(0, grid[3][5].counter)
        // Adjacent locked bucket MUST remain LOCKED & covered while slot (4,5) is refilled!
        assertEquals(CellKind.LOCKED, grid[4][4].kind)
        assertTrue(grid[4][4].isMystery)
        assertTrue(!GameRules.isExposed(grid, 4, 4))

        // 2. Now dispenser is empty (counter == 0); pop the last bucket at (4,5) -> (4,5) stays EMPTY,
        // so (4,4) unlocks AND removes cover/mystery at the exact same moment!
        grid[4][5] = GridCell(kind = CellKind.EMPTY)
        val secondRes = GameRules.updateGridAfterChange(grid)
        assertTrue(secondRes.dispensedCells.isEmpty())
        assertEquals(1, secondRes.revealedCells.size)
        assertEquals(CellKind.BLOCK, grid[4][4].kind)
        assertTrue(!grid[4][4].isMystery)
        assertTrue(GameRules.isExposed(grid, 4, 4))
    }

    @Test
    fun magnetReturn_upperCreatedBucketsAlwaysStayUnlocked() {
        val grid = Array(GameConstants.GRID_ROWS) {
            Array(GameConstants.GRID_COLS) { GridCell(kind = CellKind.WALL) }
        }
        // Only a vertical pocket of EMPTY slots (5,2), (4,2), (3,2) surrounded by WALL/LOCKED
        grid[5][2] = GridCell(kind = CellKind.EMPTY)
        grid[4][2] = GridCell(kind = CellKind.EMPTY)
        grid[3][2] = GridCell(kind = CellKind.EMPTY)
        grid[4][1] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW)

        val belt = buildList {
            for (i in 1..6) add(BeltBall(id = i, color = MarbleColor.RED, slotIndex = i, s = i * 10f))
            for (i in 7..12) add(BeltBall(id = i, color = MarbleColor.CYAN, slotIndex = i, s = i * 10f))
            for (i in 13..18) add(BeltBall(id = i, color = MarbleColor.LIME, slotIndex = i, s = i * 10f))
        }

        val assignments = GameRules.planMagnetReturn(grid, belt)
        assertEquals(18, assignments.size)

        // All 3 buckets created up top at (5,2), (4,2), and (3,2) — even middle (4,2) with no empty neighbor —
        // MUST be BLOCK (unlocked) and exposed/clickable!
        for (r in listOf(5, 4, 3)) {
            val cell = grid[r][2]
            assertEquals(CellKind.BLOCK, cell.kind)
            assertTrue(cell.fromMagnet)
            assertTrue("Magnet bucket at ($r,2) should stay unlocked", GameRules.isExposed(grid, r, 2))
        }
        // Surrounding original LOCKED cell remains untouched/locked
        assertEquals(CellKind.LOCKED, grid[4][1].kind)
    }
}
