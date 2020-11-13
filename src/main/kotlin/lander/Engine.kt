package lander

import kotlin.math.*

data class Vector2(val x: Double, val y: Double) {
    constructor(x: Int, y: Int) : this(x.toDouble(), y.toDouble())

    operator fun unaryMinus(): Vector2 = Vector2(-x, -y)
    operator fun plus(other: Vector2): Vector2 = Vector2(x + other.x, y + other.y)
    operator fun minus(other: Vector2): Vector2 = Vector2(x - other.x, y - other.y)
    operator fun times(scalar: Double): Vector2 = Vector2(x * scalar, y * scalar)
    operator fun div(scalar: Double): Vector2 = Vector2(x / scalar, y / scalar)
    operator fun Double.times(other: Vector2): Vector2 = other * this

    fun norm2(): Double = (x * x + y * y)
    fun norm(): Double = sqrt(norm2())
    override fun toString(): String = "$x $y"
}


fun Int.toRadian() = this * PI / 180.0
infix fun Double.almostEquals(other: Double) = abs(this - other) < EPS
fun distance(p: Vector2, q: Vector2) = sqrt((p.x - q.x) * (p.x - q.x) + (p.y - q.y) * (p.y - q.y))

class Action(val rotation: Int, val thrust: Int) {
    override fun toString(): String = "$rotation $thrust"
}

// represents parameters of the simulation in a given step
data class EngineParams(
    var path: List<Vector2> = listOf(), // path so far
    var milestone: Double = 0.0, // milestone of landing - used with flatMilestone to get distance along surface.
    var position: Vector2 = Vector2(0, 0), // position of the aircraft
    var velocity: Vector2 = Vector2(0, 0), // velocity of the aircraft
    var fuel: Int = 0, // fuel left
    var yaw: Int= 0, // current yaw
    var power: Int = 0, // current power of thrusters
) {
    fun acceptableLanding() = (
            yaw == 0
            && abs(milestone - engine.flatMilestone) < engine.flatExtents
            && abs(velocity.y) <= 40.0
            && abs(velocity.x) <= 20)

    // copy doesn't preserve path on purpose
    fun copy(): EngineParams = EngineParams(listOf(position), milestone, position.copy(), velocity.copy(), fuel, yaw, power)
    override fun toString(): String {
        return """
            |Pos=($position) Vel=($velocity) Yaw=$yaw 
            |Fuel=$fuel Power=$power Distance=${(milestone-engine.flatMilestone).toInt()}
            """.trimMargin()
    }
}

lateinit var engine: Engine

// Physics engine calculating state of simulation.
class Engine(val surface: Array<Vector2>) {

    /** center of landing site along the surface */
    val flatMilestone: Double

    /** 2x extents is flat zone width */
    val flatExtents: Double

    /** length along surface */
    val surfaceLength: Double

    /** parameters of realtime simulation */
    lateinit var params: EngineParams

    private val G = 3.711 // gravitational force
    private val ANGULAR_SPEED = 15 // 15 degrees per second

    /** initializes map data - extents and milestone */
    init {
        var s = 0.0
        var milestone = 0.0
        var extent = 0.0
        surface.asList().zipWithNext().forEach { (p1, p2) ->
            if (p1.y almostEquals p2.y) { // found flat segment
                extent = (p2.x - p1.x) / 2 // middle of flat zone
                milestone = s + extent
            }
            s += distance(p1, p2)
        }
        flatMilestone = milestone
        flatExtents = extent
        surfaceLength = s
    }

    // reads input and overwrites parameters. Logs the path
    fun calibrate(){
        params.position = Vector2(input.nextInt(), input.nextInt())
        params.path = params.path + params.position // log position
        params.velocity = Vector2(input.nextInt(), input.nextInt())
        params.fuel = input.nextInt()
        params.yaw = input.nextInt()
        params.power = input.nextInt()
    }

    /** simulates flight, returns parameters on land contact (1 frame after collision) */
    fun simulateFlight(actions: List<Action>): EngineParams {
        val actionIterator = actions.iterator()
        var action = Action(0, 0) // dummy action
        val simParams = params.copy() // simParams used to simulate steps
        do {
            if (actionIterator.hasNext())
                action = actionIterator.next()
        } while (!moveAndCheckCollided(action, simParams))
        return simParams
    }

    // https://www.codingame.com/forum/t/mars-lander-puzzle-discussion/32/129
    /** Simulates next turn. Returns true if collided. collision params in */
    fun moveAndCheckCollided(action: Action, params: EngineParams): Boolean {
        var simTime = 0.0

        // -------- save previous state
        val oldPosition = params.position


        // -------- update control params
        params.power = min(
            params.fuel,
            when {
                action.thrust < params.power && params.power > 0 -> params.power - 1
                action.thrust > params.power && params.power < 4 -> params.power + 1
                else -> params.power
            }
        )
        val targetYaw = min(90, max(-90, action.rotation))
        params.fuel -= params.power

        // -------- fixed step simulation - 1sec - log path

        // rotate
        params.yaw = when { // clamp rotation
            targetYaw > params.yaw -> min(params.yaw + ANGULAR_SPEED, targetYaw)
            targetYaw < params.yaw -> max(params.yaw - ANGULAR_SPEED, targetYaw)
            else -> params.yaw
        }

        // update position
        val gravityForce = Vector2(0.0, -G)
        val thrustForce = Vector2(-sin(params.yaw.toRadian()), cos(params.yaw.toRadian())) * params.power.toDouble()
        val accel = gravityForce + thrustForce
        params.velocity += accel
        params.position += params.velocity + accel / 2.0

        params.path = params.path + params.position

        // -------- collision check

        // bounds check
        if (params.position.x < 0 || params.position.x > 6999) {
            params.milestone = surfaceLength + params.position.y // make it artificially climb down the walls
            return true
        } else if (params.position.y > 3000) {
            // artificially climb towards walls
            params.milestone = surfaceLength + params.position.y + 3500 - abs(params.position.x - 3500)
            return true
        }

        // surface check
        params.milestone = 0.0
        for ((p1, p2) in surface.asList().zipWithNext()) {
            if (collides(oldPosition, params.position, p1, p2)) {
                params.milestone += distance(p1, (oldPosition + params.position) / 2.0)
                return true
            }
            params.milestone += distance(p1, p2)
        }
        return false
    }
}
