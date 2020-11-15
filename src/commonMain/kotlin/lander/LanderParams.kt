package lander

import kotlinx.serialization.Serializable

// represents parameters of the simulation in a given step
@Serializable
data class LanderParams(
    var path: List<Vector2> = listOf(), // path so far
    var milestone: Double = 0.0, // milestone of landing - used with flatMilestone to get distance along surface.
    var position: Vector2 = Vector2(0, 0), // position of the aircraft
    var velocity: Vector2 = Vector2(0, 0), // velocity of the aircraft
    var fuel: Int = 0, // fuel left
    var yaw: Int= 0, // current yaw
    var power: Int = 0, // current power of thrusters
) {
    // copy doesn't preserve path on purpose
    fun copy(): LanderParams = LanderParams(listOf(position), milestone, position.copy(), velocity.copy(), fuel, yaw, power)

}