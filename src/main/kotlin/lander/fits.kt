package lander

import kotlin.math.abs
import kotlin.math.pow

// fitness method 1: polynomial from distance to landing site, fuel left and landing velocity
fun EngineParams.polyFit(): Double = (
        1.0 * abs(milestone - engine.flatMilestone).pow(3)
        - 1.0 * fuel.toDouble().pow(2) + // make fuel more
        +1.0 * velocity.norm() +
        +1.0 * abs(yaw))




// less is better
fun EngineParams.linFit(): Double = (
        1000 * abs(milestone - engine.flatMilestone) // closer = better
        - 100 * fuel.toDouble() // more fuel = better
        + 10 * velocity.norm()  // smaller relative velocity = better
        + 1 * abs(yaw)) // smaller angle = better




fun EngineParams.crashFit(): Double = abs(milestone - engine.flatMilestone)