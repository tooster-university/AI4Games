package lander

import kotlin.math.*

typealias Path = List<Vector2>

/** online */
class LanderController(
    private val surface: Surface,
    var landerParams: LanderParams
) {

    private val G = 3.711 // gravitational force
    private val ANGULAR_SPEED = 15 // 15 degrees per second

    private fun Int.toRadian() = this * PI / 180.0
    infix fun Double.almostEquals(other: Double) = abs(this - other) < EPS

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
    private fun LanderParams.simulateUntilCollision(actions: List<Action>): LanderParams {
        val actionIterator = actions.iterator()
        var action: Action
        var oldPosition: Vector2
        do {
            action = actionIterator.next() // unsafe use of iterator to catch errors for too short chromosomes
            oldPosition = position.copy()
            step(action)
        } while (!collided(oldPosition, surface))
        return this
    }

    // https://www.codingame.com/forum/t/mars-lander-puzzle-discussion/32/129
    /** Overwrites params with simulation. Returns true if collided.*/
    private fun LanderParams.step(action: Action) {
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
    }

    private fun LanderParams.collided(oldPosition: Vector2, surface: Surface): Boolean {
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

    fun simulate(actions: List<Action>): LanderParams = landerParams.copy().simulateUntilCollision(actions)
}