package lander

import kotlin.math.*

// fitness method 1: polynomial from distance to landing site, fuel left and landing velocity
fun LanderParams.polyFit(): Double = (
        1.0 * abs(milestone - engine.flatMilestone).pow(3)
        - 1.0 * fuel.toDouble().pow(2) + // make fuel more
        +1.0 * velocity.norm() +
        +1.0 * abs(yaw))


// less is better
fun LanderParams.linFit(): Double = (
        1000 * abs(milestone - engine.flatMilestone) // closer = better
        - 100 * fuel.toDouble() // more fuel = better
        + 10 * velocity.norm()  // smaller relative velocity = better
        + 1 * abs(yaw)) // smaller angle = better


// closes to fit center = best
fun LanderParams.crashFit(): Double = abs(milestone - engine.flatMilestone)

// penalty for landing site is the same anywhere for the landing site.
fun LanderParams.penalty1(): Double {
    var penalty = 0.0

    if (abs(milestone - engine.flatMilestone) > engine.flatExtents - 100)
        penalty += abs(milestone - engine.flatMilestone)

    penalty -= fuel * 0.5


    if (velocity.norm() > sqrt(20.0 * 20 + 40 * 40))
        penalty += velocity.norm() * 10

//    if (yaw != 0)
//        penalty += abs(yaw)/2

    return penalty
}

fun LanderParams.penalty2(): Double {

    val vel = velocity.norm()
    val dd = abs(milestone - engine.flatMilestone)

    val score = when {
        // 0-100: crashed somewhere, calculate score by distance to landing area
        dd > engine.flatExtents -> 100 - (100 * (dd / engine.flatExtents)) - 0.1 * max(vel - 100, 0.0)
        // 100-200: crashed into landing area, calculate score by speed above safety
        abs(velocity.y) > 40 || 20 < abs(velocity.x) -> {
            var (xPen, yPen) = 0 to 0
            if (20 < abs(velocity.x)) xPen = ((abs(velocity.x) - 20) / 2).toInt()
            if (abs(velocity.y) > 40) yPen = ((-40 - velocity.y) / 2).toInt()
            return (200 - xPen - yPen).toDouble()
        }
        // 200-300: landed safely, calculate score by fuel remaining

        else -> (200 + (100 * this.fuel / engine.params.fuel)).toDouble()

    }

    return -score
}