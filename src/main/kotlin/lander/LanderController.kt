package lander

import kotlin.math.*

typealias Path2D = List<Vector2>

// represents parameters of the simulation in a given step
class LanderParams(
    var path: Path2D = listOf(), // path so far
    var milestone: Double = 0.0, // milestone of landing - used with flatMilestone to get distance along surface.
    var position: Vector2 = Vector2(0, 0), // position of the aircraft
    var velocity: Vector2 = Vector2(0, 0), // velocity of the aircraft
    var fuel: Int = 0, // fuel left
    var yaw: Int = 0, // current yaw
    var power: Int = 0, // current power of thrusters
) {
    // copy doesn't preserve path on purpose
    fun deepCopy(): LanderParams =
        LanderParams(
            path = listOf(position.copy()),
            milestone = milestone,
            position = position.copy(),
            velocity = velocity.copy(),
            fuel = fuel,
            yaw = yaw,
            power = power
        )

}

class LanderController(
    val io: IO,
    val surface: Surface,
    var landerParams: LanderParams
) {


    val initialParams: LanderParams
    private val G = 3.711 // gravitational force
    private val ANGULAR_SPEED = 15 // 15 degrees per second

    private fun Int.toRadian() = this * PI / 180.0
    infix fun Double.almostEquals(other: Double) = abs(this - other) < EPS

    init {
        io.error(landerParams.pretty())
        io.visualization.terrain = surface.terrain
        initialParams = landerParams.deepCopy()
    }

    fun LanderParams.landingSucceeded() = (
            yaw == 0
            && abs(milestone - surface.flatMilestone) < surface.flatExtents
            && abs(velocity.y) <= 40.0
            && abs(velocity.x) <= 20)

    fun LanderParams.pretty(): String {
        return """
            |Pos=($position) Vel=($velocity) Yaw=$yaw 
            |Fuel=$fuel Power=$power Distance=${(milestone - surface.flatMilestone).toInt()}
            """.trimMargin()
    }

    /** simulates flight, returns parameters on land contact (1 frame after collision) */
    fun LanderParams.simulateUntilCollision(actions: List<Action>): LanderParams {
        val actionIterator = actions.iterator()
        do {
            val action = actionIterator.next() // unsafe use of iterator to catch errors for too short chromosomes
        } while (!stepAndCheckCollision(action))
        return this
    }

    // https://www.codingame.com/forum/t/mars-lander-puzzle-discussion/32/129
    /** Overwrites params with simulation. Returns true if collided.*/
    fun LanderParams.stepAndCheckCollision(action: Action) : Boolean{
        var simTime = 0.0

        // -------- save previous state
        val oldPosition = position.copy()


        // -------- update control params
        power = min(
            fuel,
            when {
                action.thrust < power && power > 0 -> power - 1
                action.thrust > power && power < 4 -> power + 1
                else -> power
            }
        )
        val targetYaw = min(90, max(-90, action.rotation))
        fuel -= power

        // -------- fixed step simulation - 1sec - log path

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

        path = path + position

        // -------- collision check
        // bounds check
        if (position.x < 0 || position.x > 6999) {
            milestone = surface.surfaceLength + position.y // make it artificially climb down the walls
            return true
        } else if (position.y > 3000) {
            // artificially climb towards walls
            milestone = surface.surfaceLength + position.y + 3500 - abs(position.x - 3500)
            return true
        }

        // surface check
        milestone = 0.0
        for ((p1, p2) in surface.terrain.zipWithNext()) {
            if (collides(oldPosition, position, p1, p2)) {
                milestone += distance(p1, (oldPosition + position) / 2.0)
                return true
            }
            milestone += distance(p1, p2)
        }
        return false
    }

    fun simulate(actions: List<Action>): LanderParams = landerParams.deepCopy().simulateUntilCollision(actions)
}


fun main(args: Array<String>) {

    // initialize IO with map data
    val io = IO(MAP.valueOf(args[0]))

    // ------- read terrain
    val n = io.nextInt() // the number of points used to draw the surface of Mars.
    val terrain = mutableListOf<Vector2>()
    for (i in 0 until n)
        terrain.add(Vector2(io.nextInt().toDouble(), io.nextInt().toDouble()))
    // ------- read initial params
    val controller = LanderController(io, Surface(terrain), io.nextParams())
    controller.rollingHorizonSolver(muLambdaEvolver(200, 150), LanderController::penalty2)
//    controller.rollingHorizonSolver(nonEvolver(), LanderController::penalty1)
}