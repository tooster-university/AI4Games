package lander

import java.util.*
import kotlin.math.*
import kotlin.random.Random

data class Vector2(val x: Double, val y: Double) {
    constructor(x: Int, y: Int) : this(x.toDouble(), y.toDouble())

    operator fun unaryMinus(): Vector2 = Vector2(-x, -y)
    operator fun plus(other: Vector2): Vector2 = Vector2(x + other.x, y + other.y)
    operator fun minus(other: Vector2): Vector2 = Vector2(x - other.x, y - other.y)
    operator fun times(scalar: Double): Vector2 = Vector2(x * scalar, y * scalar)
    operator fun div(scalar: Double): Vector2 = Vector2(x / scalar, y / scalar)
    operator fun Double.times(other: Vector2): Vector2 = other * this

    fun norm2(): Double = (x*x + y*y)
    fun norm(): Double = sqrt(norm2())
    override fun toString(): String = "$x $y"
}


fun Int.toRadian() = this * PI / 180.0
infix fun Double.almostEquals(other: Double) = abs(this - other) < EPS
fun distance(p: Vector2, q: Vector2) = sqrt((p.x - q.x) * (p.x - q.x) + (p.y - q.y) * (p.y - q.y))

class Action(val rotation: Int, val thrust: Int){
    override fun toString(): String = "$rotation $thrust"
}

lateinit var engine: Engine

// Physics engine calculating state of simulation.
class Engine(
    val surface: Array<Vector2>,
    var initialPosition: Vector2,
    var initialVelocity: Vector2,
    var initialFuel: Int,
    var initialYaw: Int,
    var initialPower: Int
) {

    var path: MutableList<Vector2> = mutableListOf()
    var position: Vector2 = initialPosition
    var velocity: Vector2 = initialVelocity
    var fuel: Int = initialFuel
    var yaw: Int = initialFuel
    var power: Int = initialPower

    var landing = false

    val flatMilestone: Double // center of landing site along the surface
    val flatExtents: Double // 2x extents is flat zone width
    var crashMilestone: Double = 0.0 // where lander crashed

    private val G = 3.711
    private val ANGULAR_SPEED = 15 // 15 degrees per second

    private var targetYaw = 0

    fun restart() {
        position = initialPosition
        velocity = initialVelocity
        fuel = initialFuel
        yaw = initialYaw
        power = initialPower
        landing = true
        path.clear()
        path.add(position)
    }

    init {
        var s = 0.0
        var milestone = 0.0
        var extent = 0.0
        surface.asList().zipWithNext().forEach { (p1, p2) ->
            if (p1.y almostEquals p2.y) { // found flat segment
                extent = (p2.x - p1.x) / 2 // middle of flat zone
                milestone = s + extent
                return@forEach
            } else {
                s += distance(p1, p2)
            }
        }
        flatMilestone = milestone
        flatExtents = extent
    }

    // https://www.codingame.com/forum/t/mars-lander-puzzle-discussion/32/129
    private fun simulate(): Vector2 {

        // rotate
        yaw = when { // clamp rotation
            targetYaw > yaw -> min(yaw + ANGULAR_SPEED, targetYaw)
            targetYaw < yaw -> max(yaw - ANGULAR_SPEED, targetYaw)
            else -> yaw
        }

        // update position
        val gravityForce = Vector2(0.0, -G)
        val thrustForce = Vector2(-sin(yaw.toRadian()), cos(yaw.toRadian())) * power.toDouble()
        val accel = gravityForce + thrustForce
        velocity += accel
        position += velocity + accel / 2.0
        return position
    }

    // Simulates next turn. Returns true if collided
    fun moveAndCheckCollided(action: Action) {
        var simTime = 0.0

        // -------- save previous state
        val oldPosition = position


        // -------- update control params
        power = min(
            fuel,
            when {
                action.thrust < power && power > 0 -> power - 1
                action.thrust > power && power < 4 -> power + 1
                else -> power
            }
        )
        targetYaw = min(90, max(-90, action.rotation))
        fuel -= power

        // -------- fixed step simulation - 1sec - log path
        path.add(simulate())

        // -------- collision check
        var milestone = 0.0 // distance along surface (left to right)

        // bounds check
        if (position.x < 0 || position.x > 6999) {
            crashMilestone = flatMilestone + position.y
            landing = false
            return
        } else if (position.y > 3000) {
            crashMilestone = flatMilestone + position.y + position.x
            landing = false
            return
        }
        // surface check
        for ((p1, p2) in surface.asList().zipWithNext()) {
            if (collides(oldPosition, position, p1, p2)) {
                crashMilestone = milestone + distance(p1, (oldPosition + position) / 2.0)
                landing = false
                return
            } else {
                milestone += distance(p1, p2)
            }
        }
    }
}
