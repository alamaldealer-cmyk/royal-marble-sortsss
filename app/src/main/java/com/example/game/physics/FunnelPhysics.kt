package com.example.game.physics

import com.example.game.constants.GameConstants
import com.example.game.model.FunnelBall
import com.example.game.model.MarbleColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class FunnelBoundarySegment(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val nx: Float,
    val ny: Float,
    val length: Float
)

class FunnelPhysics {
    val balls: MutableList<FunnelBall> = mutableListOf()
    private var nextId = 1
    private val walls: MutableList<FunnelBoundarySegment> = mutableListOf()

    init {
        initWalls()
    }

    private fun initWalls() {
        val rawSegments = listOf(
            floatArrayOf(18f, 120f, 18f, 400f),
            floatArrayOf(18f, 400f, 74f, 420f),
            floatArrayOf(74f, 420f, 130f, 436f),
            floatArrayOf(130f, 436f, 130f, 458f),
            floatArrayOf(166f, 458f, 166f, 436f),
            floatArrayOf(166f, 436f, 222f, 420f),
            floatArrayOf(222f, 420f, 278f, 400f),
            floatArrayOf(278f, 400f, 278f, 120f)
        )

        for (s in rawSegments) {
            val dx = s[2] - s[0]
            val dy = s[3] - s[1]
            val len = hypot(dx, dy)
            val nx = dy / len
            val ny = -dx / len
            walls.add(FunnelBoundarySegment(s[0], s[1], s[2], s[3], nx, ny, len))
        }
    }

    fun spawnBurst(centerX: Float, centerY: Float, color: MarbleColor, count: Int = GameConstants.BALLS_PER_BLOCK) {
        for (i in 0 until count) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val dist = Random.nextFloat() * 8f
            val px = centerX + cos(angle) * dist
            val py = centerY + sin(angle) * dist

            val speed = 60f + Random.nextFloat() * 80f
            val spreadAngle = (PI * 0.15f + Random.nextFloat() * PI.toFloat() * 0.7f).toFloat()
            val vx = (cos(spreadAngle) * speed * if (Random.nextBoolean()) 1f else -1f)
            val vy = (sin(spreadAngle) * speed + 30f)

            balls.add(
                FunnelBall(
                    id = nextId++,
                    color = color,
                    x = px,
                    y = py,
                    vx = vx,
                    vy = vy,
                    radius = GameConstants.BALL_RADIUS_FUNNEL
                )
            )
        }
    }

    fun update(dt: Float, gateOpen: Boolean): FunnelBall? {
        val subSteps = 4
        val subDt = min(dt, 0.05f) / subSteps
        val restitution = 0.3f
        val friction = 0.02f

        var ballToFeed: FunnelBall? = null

        for (step in 0 until subSteps) {
            // 1. Forces and velocity integration
            for (b in balls) {
                b.vy += GameConstants.GRAVITY * subDt
                b.vx *= (1f - 0.01f * subDt)
                b.vy *= (1f - 0.01f * subDt)

                b.x += b.vx * subDt
                b.y += b.vy * subDt
            }

            // 2. Ball-Wall collisions
            for (b in balls) {
                for (wall in walls) {
                    val dx = b.x - wall.x1
                    val dy = b.y - wall.y1
                    val segDx = wall.x2 - wall.x1
                    val segDy = wall.y2 - wall.y1

                    var t = (dx * segDx + dy * segDy) / (wall.length * wall.length)
                    t = max(0f, min(1f, t))

                    val closestX = wall.x1 + t * segDx
                    val closestY = wall.y1 + t * segDy

                    val distX = b.x - closestX
                    val distY = b.y - closestY
                    val distance = hypot(distX, distY)

                    if (distance < b.radius) {
                        val overlap = b.radius - distance
                        b.x += wall.nx * overlap
                        b.y += wall.ny * overlap

                        val velNormal = b.vx * wall.nx + b.vy * wall.ny
                        if (velNormal < 0f) {
                            b.vx -= (1f + restitution) * velNormal * wall.nx
                            b.vy -= (1f + restitution) * velNormal * wall.ny
                            b.vx *= (1f - friction)
                            b.vy *= (1f - friction)
                        }
                    }
                }

                // Invisible gate at neck bottom (y=456, x in 128..168)
                if (!gateOpen) {
                    if (b.y + b.radius >= 456f && b.x >= 128f && b.x <= 168f) {
                        b.y = 456f - b.radius
                        if (b.vy > 0f) b.vy = -b.vy * 0.2f
                    }
                }
            }

            // 3. Ball-Ball soft collisions
            val count = balls.size
            for (i in 0 until count) {
                val b1 = balls[i]
                for (j in i + 1 until count) {
                    val b2 = balls[j]
                    val dx = b2.x - b1.x
                    val dy = b2.y - b1.y
                    val distSq = dx * dx + dy * dy
                    val minDist = b1.radius + b2.radius

                    if (distSq < minDist * minDist && distSq > 0.0001f) {
                        val dist = sqrt(distSq)
                        val overlap = 0.5f * (minDist - dist)
                        val nx = dx / dist
                        val ny = dy / dist

                        b1.x -= nx * overlap
                        b1.y -= ny * overlap
                        b2.x += nx * overlap
                        b2.y += ny * overlap

                        val dvx = b2.vx - b1.vx
                        val dvy = b2.vy - b1.vy
                        val normalVel = dvx * nx + dvy * ny

                        if (normalVel < 0f) {
                            val impulse = -(1f + restitution) * normalVel * 0.5f
                            b1.vx -= nx * impulse
                            b1.vy -= ny * impulse
                            b2.vx += nx * impulse
                            b2.vy += ny * impulse
                        }
                    }
                }
            }
        }

        // 4. Feed gate: if gate open, lowest ball in neck exits into belt
        if (gateOpen && ballToFeed == null) {
            var lowestIndex = -1
            var lowestY = 0f

            for (i in balls.indices) {
                val b = balls[i]
                if (b.x in 128f..168f && b.y >= 436f) {
                    if (b.y > lowestY) {
                        lowestY = b.y
                        lowestIndex = i
                    }
                }
            }

            if (lowestIndex != -1 && lowestY >= 442f) {
                ballToFeed = balls.removeAt(lowestIndex)
            }
        }

        return ballToFeed
    }
}
