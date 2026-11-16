package com.example

import com.example.game.belt.BeltPath
import com.example.game.constants.GameConstants
import com.example.game.levels.LevelGenerator
import com.example.game.lives.LivesManager
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

        assertSame(frontRed, assignments[0].targetBrick)
        assertSame(frontRed, assignments[1].targetBrick)
        assertEquals(2, frontRed.inFlight)

        assertSame(behindRed, assignments[2].targetBrick)
        assertSame(behindRed, assignments[3].targetBrick)
        assertEquals(1, assignments[2].targetCol)
        assertEquals(2, behindRed.inFlight)

        assertNull(assignments[4].targetBrick)
        assertEquals(-1, assignments[4].targetCol)
    }

    @Test
    fun levelsOneThroughFifty_areBalancedAndFullyClearable() {
        for (lvl in 1..50) {
            val data = LevelGenerator.generateSolvableLevel(lvl)
            assertEquals(4, data.columns.size)
            assertTrue("Level $lvl has empty column", data.columns.all { it.isNotEmpty() })

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
        grid[5][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BLUE, linkGroup = 1)
        grid[4][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.YELLOW, linkGroup = 1)
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
        grid[3][5] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 1,
            queue = mutableListOf(MarbleColor.ORANGE)
        )
        grid[4][4] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.PURPLE, isMystery = true)
        grid[4][5] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.BLUE)
        grid[5][5] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.RED)

        grid[4][5] = GridCell(kind = CellKind.EMPTY)
        val firstRes = GameRules.updateGridAfterChange(grid)
        assertEquals(1, firstRes.dispensedCells.size)
        assertTrue(firstRes.revealedCells.isEmpty())
        assertEquals(CellKind.BLOCK, grid[4][5].kind)
        assertEquals(MarbleColor.ORANGE, grid[4][5].color)
        assertEquals(0, grid[3][5].counter)
        assertEquals(CellKind.LOCKED, grid[4][4].kind)
        assertTrue(grid[4][4].isMystery)
        assertTrue(!GameRules.isExposed(grid, 4, 4))

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

        for (r in listOf(5, 4, 3)) {
            val cell = grid[r][2]
            assertEquals(CellKind.BLOCK, cell.kind)
            assertTrue(cell.fromMagnet)
            assertTrue("Magnet bucket at ($r,2) should stay unlocked", GameRules.isExposed(grid, r, 2))
        }
        assertEquals(CellKind.LOCKED, grid[4][1].kind)
    }

    @Test
    fun chainFullAndStuck_detectsOnlyWhenBeltFullAndNoFrontBucketMatches() {
        val columns = listOf(
            BrickColumn(x = 69f, bricks = mutableListOf(Brick(color = MarbleColor.RED, filled = 0))),
            BrickColumn(x = 122f, bricks = mutableListOf(Brick(color = MarbleColor.CYAN, filled = 1))),
            BrickColumn(x = 175f, bricks = mutableListOf(Brick(color = MarbleColor.YELLOW, filled = 0))),
            BrickColumn(x = 227f, bricks = mutableListOf(Brick(color = MarbleColor.PURPLE, filled = 0)))
        )

        val beltNotFull = (1..23).map { i ->
            BeltBall(id = i, color = MarbleColor.ORANGE, slotIndex = i - 1, s = i * 17f)
        }
        assertTrue(!GameRules.isChainFullAndStuck(beltNotFull, 24, columns, 0, 0, false, false))

        val beltFullNoMatch = (1..24).map { i ->
            BeltBall(id = i, color = if (i <= 16) MarbleColor.ORANGE else MarbleColor.BLUE, slotIndex = i - 1, s = i * 17f)
        }
        assertTrue(GameRules.isChainFullAndStuck(beltFullNoMatch, 24, columns, 0, 0, false, false))

        val beltWithOneMatch = beltFullNoMatch.dropLast(1) + BeltBall(id = 99, color = MarbleColor.CYAN, slotIndex = 23, s = 100f)
        assertTrue(!GameRules.isChainFullAndStuck(beltWithOneMatch, 24, columns, 0, 0, false, false))

        columns[0].bricks[0].animState = "vanish"
        assertTrue(!GameRules.isChainFullAndStuck(beltFullNoMatch, 24, columns, 0, 0, false, false))
    }

    @Test
    fun selectCleanUpBeltBalls_picksEightBallsFromTopStraightLane() {
        val spacing = BeltPath.TOTAL_PATH_LEN / 24f
        val belt = (0 until 24).map { idx ->
            BeltBall(
                id = idx + 1,
                color = if (idx % 2 == 0) MarbleColor.ORANGE else MarbleColor.BLUE,
                slotIndex = idx,
                s = idx * spacing
            )
        }

        val selected = GameRules.selectCleanUpBeltBalls(belt, 8)
        assertEquals(8, selected.size)
        for (ball in selected) {
            val coords = BeltPath.getBeltCoordinates(ball.s)
            assertTrue("Expected top lane ball, got bottom lane at s=${ball.s}", !coords.isBottomLane)
        }
    }

    @Test
    fun planCleanUpReturn_returnsAllFunnelBallsPlusEightChainBallsWithoutLoss() {
        val grid = Array(GameConstants.GRID_ROWS) {
            Array(GameConstants.GRID_COLS) { GridCell(kind = CellKind.WALL) }
        }
        grid[5][2] = GridCell(kind = CellKind.EMPTY)
        grid[5][3] = GridCell(kind = CellKind.EMPTY)
        grid[4][2] = GridCell(kind = CellKind.EMPTY)
        grid[4][3] = GridCell(kind = CellKind.EMPTY)

        val items = buildList {
            for (i in 1..12) {
                add(GameRules.CleanUpReturnItem(id = i, color = MarbleColor.ORANGE, startX = 148f, startY = 410f, isFromFunnel = true))
            }
            for (i in 13..24) {
                add(GameRules.CleanUpReturnItem(id = i, color = MarbleColor.BLUE, startX = 148f, startY = 420f, isFromFunnel = true))
            }
            for (i in 25..32) {
                add(GameRules.CleanUpReturnItem(id = i, color = MarbleColor.PINK, startX = 120f + i, startY = 462.5f, isFromFunnel = false))
            }
        }

        val assignments = GameRules.planCleanUpReturn(grid, items)
        assertEquals(32, assignments.size)
        val byColor = assignments.groupBy { it.item.color }
        assertEquals(12, byColor[MarbleColor.ORANGE]?.size)
        assertEquals(12, byColor[MarbleColor.BLUE]?.size)
        assertEquals(8, byColor[MarbleColor.PINK]?.size)
    }

    @Test
    fun buildCleanUpPreviewSnapshot_simulatesExactPostAdStateWithoutMutatingLiveGrid() {
        val grid = Array(GameConstants.GRID_ROWS) {
            Array(GameConstants.GRID_COLS) { GridCell(kind = CellKind.EMPTY) }
        }
        grid[0][0] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.ORANGE, ballCount = 9)

        val funnel = listOf(
            com.example.game.model.FunnelBall(1, MarbleColor.BLUE, 140f, 410f, 0f, 0f),
            com.example.game.model.FunnelBall(2, MarbleColor.BLUE, 155f, 418f, 0f, 0f)
        )
        val belt = (0 until 24).map { idx ->
            BeltBall(
                id = 100 + idx,
                color = if (idx < 8) MarbleColor.LIME else MarbleColor.RED,
                slotIndex = idx,
                s = idx * (BeltPath.TOTAL_PATH_LEN / 24f)
            )
        }
        val selectedIds = GameRules.selectCleanUpBeltBalls(belt, 8).map { it.id }.toSet()

        val snapshot = GameRules.buildCleanUpPreviewSnapshot(
            grid = grid,
            funnelBalls = funnel,
            beltBalls = belt,
            highlightedBeltIds = selectedIds,
            beltCapacity = 24
        )

        // Original grid must remain untouched (still EMPTY at [5][2])
        assertEquals(CellKind.EMPTY, grid[5][2].kind)
        // Snapshot captures 2 funnel balls + 8 selected chain balls = 10 flights
        assertEquals(10, snapshot.flights.size)
        assertEquals(2, snapshot.funnelBalls.size)
        assertEquals(24, snapshot.beltBalls.size)
        assertEquals(8, snapshot.beltBalls.count { it.isHighlighted })
        assertEquals(16, snapshot.beltBalls.count { !it.isReturned })
        // Target cells should contain the newly created container(s) while initialCells stayed EMPTY
        assertEquals(
            CellKind.EMPTY,
            snapshot.initialCells.first { it.row == 5 && it.col == 2 }.kind
        )
        assertEquals(
            CellKind.BLOCK,
            snapshot.targetCells.first { it.row == 5 && it.col == 2 }.kind
        )
    }

    @Test
    fun targetBeltSpeed_increasesOnlyWhenUpperGridBucketsAreCleared() {
        val grid = Array(GameConstants.GRID_ROWS) {
            Array(GameConstants.GRID_COLS) { GridCell(kind = CellKind.EMPTY) }
        }
        grid[0][0] = GridCell(kind = CellKind.WALL)
        grid[1][1] = GridCell(kind = CellKind.DISPENSER, counter = 0)

        // Only WALL + exhausted DISPENSER + EMPTY -> upper buckets cleared -> fast speed
        assertEquals(true, GameRules.areUpperBucketsCleared(grid))
        assertEquals(GameConstants.BELT_SPEED_FAST, GameRules.targetBeltSpeed(grid), 0.01f)

        // While balls are returning via Magnet/CleanUp, speed stays normal
        assertEquals(
            GameConstants.BELT_SPEED,
            GameRules.targetBeltSpeed(grid, hasReturningBalls = true),
            0.01f
        )

        // Active DISPENSER -> not cleared
        grid[1][1] = GridCell(
            kind = CellKind.DISPENSER,
            counter = 1,
            queue = mutableListOf(MarbleColor.BLUE)
        )
        assertEquals(false, GameRules.areUpperBucketsCleared(grid))
        assertEquals(GameConstants.BELT_SPEED, GameRules.targetBeltSpeed(grid), 0.01f)

        // LOCKED crate -> not cleared
        grid[1][1] = GridCell(kind = CellKind.EMPTY)
        grid[2][2] = GridCell(kind = CellKind.LOCKED, color = MarbleColor.RED)
        assertEquals(false, GameRules.areUpperBucketsCleared(grid))
        assertEquals(GameConstants.BELT_SPEED, GameRules.targetBeltSpeed(grid), 0.01f)

        // Regular BLOCK -> not cleared
        grid[2][2] = GridCell(kind = CellKind.BLOCK, color = MarbleColor.ORANGE)
        assertEquals(false, GameRules.areUpperBucketsCleared(grid))
        assertEquals(GameConstants.BELT_SPEED, GameRules.targetBeltSpeed(grid), 0.01f)

        // Clearing last BLOCK -> speeds up
        grid[2][2] = GridCell(kind = CellKind.EMPTY)
        assertEquals(true, GameRules.areUpperBucketsCleared(grid))
        assertEquals(GameConstants.BELT_SPEED_FAST, GameRules.targetBeltSpeed(grid), 0.01f)
    }

    @Test
    fun livesManager_consumesAndRefillsEvery20Minutes() {
        val t0 = 1_000_000L

        // Start full (5 lives) -> status is "Full"
        val full = LivesManager.reconcileLives(5, 0L, t0)
        assertEquals(5, full.lives)
        assertEquals("Full", LivesManager.formatStatusText(full))

        // Losing 1 life drops 5 -> 4 and starts a 20:00 timer
        val afterFirstLoss = LivesManager.consumeOneLife(full.lives, full.nextRefillAtMs, t0)
        assertEquals(4, afterFirstLoss.lives)
        assertEquals(t0 + LivesManager.REFILL_DURATION_MS, afterFirstLoss.nextRefillAtMs)
        assertEquals("20:00", LivesManager.formatStatusText(afterFirstLoss))

        // 1 second later -> 19:59
        val oneSecLater = LivesManager.reconcileLives(
            afterFirstLoss.lives,
            afterFirstLoss.nextRefillAtMs,
            t0 + 1000L
        )
        assertEquals(4, oneSecLater.lives)
        assertEquals("19:59", LivesManager.formatStatusText(oneSecLater))

        // Losing another life at +5 minutes preserves the original countdown (now 15:00 remaining)
        val fiveMinLaterMs = t0 + 5L * 60L * 1000L
        val afterSecondLoss = LivesManager.consumeOneLife(
            afterFirstLoss.lives,
            afterFirstLoss.nextRefillAtMs,
            fiveMinLaterMs
        )
        assertEquals(3, afterSecondLoss.lives)
        assertEquals(afterFirstLoss.nextRefillAtMs, afterSecondLoss.nextRefillAtMs)
        assertEquals("15:00", LivesManager.formatStatusText(afterSecondLoss))

        // At +20 minutes -> +1 life (3 -> 4), next timer still has 20:00 left
        val twentyMinMs = t0 + LivesManager.REFILL_DURATION_MS
        val refilledOne = LivesManager.reconcileLives(
            afterSecondLoss.lives,
            afterSecondLoss.nextRefillAtMs,
            twentyMinMs
        )
        assertEquals(4, refilledOne.lives)
        assertEquals("20:00", LivesManager.formatStatusText(refilledOne))

        // At +40 minutes -> back to 5 ("Full")
        val fortyMinMs = t0 + 2L * LivesManager.REFILL_DURATION_MS
        val refilledAll = LivesManager.reconcileLives(
            afterSecondLoss.lives,
            afterSecondLoss.nextRefillAtMs,
            fortyMinMs
        )
        assertEquals(5, refilledAll.lives)
        assertEquals(0L, refilledAll.nextRefillAtMs)
        assertEquals("Full", LivesManager.formatStatusText(refilledAll))
    }

    @Test
    fun livesManager_watchAdAddsThreeLivesAndCapsAtMax() {
        val t0 = 5_000_000L
        val targetAt = t0 + 12L * 60L * 1000L // 12:00 remaining

        assertEquals(3, LivesManager.AD_REWARD_LIVES)

        // 0 -> 3 lives, preserves remaining 20-min refill countdown
        val fromZero = LivesManager.addLives(0, targetAt, t0, LivesManager.AD_REWARD_LIVES)
        assertEquals(3, fromZero.lives)
        assertEquals(targetAt, fromZero.nextRefillAtMs)
        assertEquals("12:00", LivesManager.formatStatusText(fromZero))

        // 1 -> 4 lives, preserves remaining refill countdown
        val fromOne = LivesManager.addLives(1, targetAt, t0, LivesManager.AD_REWARD_LIVES)
        assertEquals(4, fromOne.lives)
        assertEquals(targetAt, fromOne.nextRefillAtMs)

        // 2 -> 5 lives (Full), clears timer
        val fromTwo = LivesManager.addLives(2, targetAt, t0, LivesManager.AD_REWARD_LIVES)
        assertEquals(5, fromTwo.lives)
        assertEquals(0L, fromTwo.nextRefillAtMs)
        assertEquals("Full", LivesManager.formatStatusText(fromTwo))

        // 4 -> 5 lives (capped at MAX_LIVES), clears timer
        val fromFour = LivesManager.addLives(4, targetAt, t0, LivesManager.AD_REWARD_LIVES)
        assertEquals(5, fromFour.lives)
        assertEquals(0L, fromFour.nextRefillAtMs)
    }

    @Test
    fun jackpotEventManager_runs24HourCycleAndResetsAfter24Hours() {
        val t0 = 10_000_000L
        val initial = com.example.game.jackpot.JackpotEventManager.reconcile(0, 0L, t0)
        assertEquals(0, initial.claimedSteps)
        assertEquals(5, initial.remainingSteps)
        assertEquals(t0 + com.example.game.jackpot.JackpotEventManager.CYCLE_DURATION_MS, initial.cycleEndAtMs)
        assertEquals(
            "24:00:00",
            com.example.game.jackpot.JackpotEventManager.formatCountdownTimer(initial.remainingMs)
        )

        // Claim all 5 tiers (50, 100, 200, 400, 800) within the same 24h cycle
        var state = initial
        for (i in 1..5) {
            state = com.example.game.jackpot.JackpotEventManager.claimNextStep(
                claimedSteps = state.claimedSteps,
                cycleEndAtMs = state.cycleEndAtMs,
                nowMs = t0 + i * 1000L
            )
            assertEquals(i, state.claimedSteps)
        }
        assertEquals(true, state.allClaimed)
        assertEquals(0, state.remainingSteps)
        assertEquals(
            "23:59:55",
            com.example.game.jackpot.JackpotEventManager.formatCountdownTimer(state.remainingMs)
        )

        // Before 24h completes -> remains allClaimed
        val before24h = com.example.game.jackpot.JackpotEventManager.reconcile(
            claimedSteps = state.claimedSteps,
            cycleEndAtMs = state.cycleEndAtMs,
            nowMs = state.cycleEndAtMs - 5_000L
        )
        assertEquals(true, before24h.allClaimed)
        assertEquals(
            "00:00:05",
            com.example.game.jackpot.JackpotEventManager.formatCountdownTimer(before24h.remainingMs)
        )

        // Once 24 real hours pass -> resets back to 0 claimed (50 Free + Ads available again)
        val after24h = com.example.game.jackpot.JackpotEventManager.reconcile(
            claimedSteps = state.claimedSteps,
            cycleEndAtMs = state.cycleEndAtMs,
            nowMs = state.cycleEndAtMs
        )
        assertEquals(0, after24h.claimedSteps)
        assertEquals(false, after24h.allClaimed)
        assertEquals(5, after24h.remainingSteps)
        assertEquals(
            state.cycleEndAtMs + com.example.game.jackpot.JackpotEventManager.CYCLE_DURATION_MS,
            after24h.cycleEndAtMs
        )
    }
}
