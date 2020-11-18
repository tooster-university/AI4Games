package lander

import kotlin.math.*

typealias Path2D = List<Vector2>

// represents parameters of the simulation in a given step
class LanderParams(
    var path: Path2D = listOf(), // path so far
    var distanceFromFlat: Double = 0.0, //
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
            distanceFromFlat = distanceFromFlat,
            position = position.copy(),
            velocity = velocity.copy(),
            fuel = fuel,
            yaw = yaw,
            power = power
        )

    fun pretty(): String {
        return """
            |   Pos=(%.0f %.0f) Vel=(%.2f %.2f) Yaw=%d 
            |   Fuel=%d Power=%d Distance=%.0f
            """.trimMargin().format(position.x, position.y, velocity.x, velocity.y, yaw, fuel, power, distanceFromFlat)
    }
}

class LanderController(
    val io: IO,
    val surface: Surface,
    var landerParams: LanderParams,
) {


    val initialParams: LanderParams
    private val G = 3.711 // gravitational force
    private val ANGULAR_SPEED = 15 // 15 degrees per second

    infix fun Double.almostEquals(other: Double) = abs(this - other) < EPS

    init {
        io.error(landerParams.pretty())
        io.visualization.terrain = surface.terrain
        initialParams = landerParams.deepCopy()
    }

    fun LanderParams.landingSucceeded() = (
            yaw == 0
            && distanceFromFlat <= surface.flatExtents
            && abs(velocity.y) <= 40.0
            && abs(velocity.x) <= 20)


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
    fun LanderParams.stepAndCheckCollision(action: Action): Boolean {
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
        when {
            position.x < 0 -> {
                distanceFromFlat = surface.flatMilestone + position.y
                return true
            }
            position.x > 6999 -> {
                distanceFromFlat = surface.surfaceLength - surface.flatMilestone + position.y
                return true
            }
            position.y > 3000 -> {
                // artificially climb towards walls
                distanceFromFlat = min(
                    surface.flatMilestone + position.y + position.x,
                    surface.surfaceLength - surface.flatMilestone + position.y + 7000 - position.x
                )
                return true
            }

            // surface check by walking along
            else -> {
                var milestone = 0.0 // point on surface that was hit (0 is start of surface)
                for ((p1, p2) in surface.terrain.zipWithNext()) {
                    if (collides(oldPosition, position, p1, p2)) {
                        milestone += distance(p1, (oldPosition + position) / 2.0) // center of segment as hitpoint
                        distanceFromFlat = abs(surface.flatMilestone - milestone)
                        return true
                    }
                    milestone += distance(p1, p2) // walk along the surface
                }
                return false
            }
        }

    }

    fun simulate(actions: List<Action>): LanderParams = landerParams.deepCopy().simulateUntilCollision(actions)
}


fun main(args: Array<String>) {

    // initialize IO with map data
    val io = IO(if(args.isEmpty()) MAP.EXAMPLE else MAP.valueOf(args[0]))

    // ------- read terrain
    val n = io.nextInt() // the number of points used to draw the surface of Mars.
    val terrain = mutableListOf<Vector2>()
    for (i in 0 until n) {
        val (x, y) = io.nextInt() to io.nextInt()
        terrain.add(Vector2(x, y))
    }
    // ------- read initial params
    val controller = LanderController(io, Surface(terrain), io.nextParams())
    controller.rollingHorizonSolver(
        rouletteEvolver(
            populationSize = 200,
            chromosomeLength = 300,
            eliteSize = 40,
//            mutationProbability = 0.2,
        ),
        LanderController::score1,
        visualizationInterval = 20,
        evolverRounds = -95
    )
//    controller.rollingHorizonSolver(muLambdaEvolver(200, 150), LanderController::penalty2)
//    controller.rollingHorizonSolver(nonEvolver(), LanderController::penalty1)
}