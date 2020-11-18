package lander

import kotlin.math.*

// fitness method 1: polynomial from distance to landing site, fuel left and landing velocity
fun LanderController.polyPenalty(params: LanderParams): Double = -(
        1.0 * params.distanceFromFlat.pow(3)
        - 1.0 * params.fuel.toDouble().pow(2) + // make fuel more
        +1.0 * params.velocity.norm() +
        +1.0 * abs(params.yaw))


// less is better
fun LanderController.linPenalty(params: LanderParams): Double = -(
        1000 * params.distanceFromFlat  // closer = better
        - 100 * params.fuel.toDouble() // more fuel = better
        + 10 * params.velocity.norm()  // smaller relative velocity = better
        + 1 * abs(params.yaw)) // smaller angle = better


// closes to fit center = best
fun LanderController.crashPenalty(params: LanderParams): Double = -abs(params.distanceFromFlat)

// penalty for landing site is the same anywhere for the landing site.
fun LanderController.penalty1(params: LanderParams): Double {
    var penalty = 0.0

    if (params.distanceFromFlat > surface.flatExtents - 100)
        penalty += params.distanceFromFlat

    penalty -= params.fuel * 0.5


    if (params.velocity.norm() > sqrt(20.0 * 20 + 40 * 40))
        penalty += params.velocity.norm() * 10

//    if (yaw != 0)
//        penalty += abs(yaw)/2

    return -penalty
}

fun LanderController.penalty2(params: LanderParams): Double {

    val vel = params.velocity

    var penalty = 0.0
    // 1. distance penalty - above 100 when farther than crash site, if < 0.9 from landing zone then 0 penalty
    if (params.distanceFromFlat > surface.flatExtents) {
        penalty += 100 + 100 * params.distanceFromFlat / surface.flatExtents
    } else if (params.distanceFromFlat > surface.flatExtents * 0.9)
        penalty += (params.distanceFromFlat / surface.flatExtents)

    // 2. velocity penalty
    if (-params.velocity.y > 40) // linear penalty above 40
        penalty += 15 + -params.velocity.y * 2
    else if (-params.velocity.y > 35) // small penalty in 40-35 region
        penalty += 15 + -params.velocity.y

    if (-params.velocity.x > 20) // linear penalty above 20
        penalty += 15 + -params.velocity.x
    else if (-params.velocity.x > 15) // small penalty in 20-15 region
        penalty += 15 + -params.velocity.x

    // 0-15 penalty for yaw
    penalty += 10 * abs(params.yaw) / 90

    // small prize for fuel
    penalty -= (params.fuel / initialParams.fuel) * 5

    return -penalty
}

fun LanderController.score1(params: LanderParams): Double {
    val vel = params.velocity
    val flatMile = max(surface.surfaceLength - surface.flatMilestone, surface.flatMilestone)

    // score caluclation
    return when {
        params.distanceFromFlat > surface.flatExtents -> // outside landing zone
            lerp(90.0, 0.0, params.distanceFromFlat / surface.surfaceLength)
        params.distanceFromFlat > surface.flatExtents * 0.5 -> // in margin of landing zone
            lerp(100.0, 90.0, params.distanceFromFlat / surface.flatExtents)
        else -> { // take velocity into consideration
            // speed on boundary is factor = 1.0, speed 0 is factor 0. Metric is infinity (box)
            val vFactor = max(abs(params.velocity.x)/20.0, abs(params.velocity.y)/40.0)
            100.0 + when {
                vFactor > 1.0 -> lerp(90.0, 0.0, vFactor/10.0 + 0.1) // >100% terminal speed - 90--0
                vFactor > 0.95 -> lerp(100.0, 90.0, vFactor ) // 95%-100% of terminal speed - 100--90
                else -> {
                    100.0 + (if(params.yaw == 0) 50.0 else 0.0) + 10*params.fuel/initialParams.fuel
                }
            }
        }
    }
}

