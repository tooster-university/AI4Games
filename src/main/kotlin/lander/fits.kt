package lander

import kotlin.math.*

// fitness method 1: polynomial from distance to landing site, fuel left and landing velocity
fun LanderController.polyFit(params: LanderParams): Double = (
        1.0 * abs(params.milestone - surface.flatMilestone).pow(3)
        - 1.0 * params.fuel.toDouble().pow(2) + // make fuel more
        +1.0 * params.velocity.norm() +
        +1.0 * abs(params.yaw))


// less is better
fun LanderController.linFit(params: LanderParams): Double = (
        1000 * abs(params.milestone - surface.flatMilestone) // closer = better
        - 100 * params.fuel.toDouble() // more fuel = better
        + 10 * params.velocity.norm()  // smaller relative velocity = better
        + 1 * abs(params.yaw)) // smaller angle = better


// closes to fit center = best
fun LanderController.crashFit(params: LanderParams): Double = abs(params.milestone - surface.flatMilestone)

// penalty for landing site is the same anywhere for the landing site.
fun LanderController.penalty1(params: LanderParams): Double {
    var penalty = 0.0

    if (abs(params.milestone - surface.flatMilestone) > surface.flatExtents - 100)
        penalty += abs(params.milestone - surface.flatMilestone)

    penalty -= params.fuel * 0.5


    if (params.velocity.norm() > sqrt(20.0 * 20 + 40 * 40))
        penalty += params.velocity.norm() * 10

//    if (yaw != 0)
//        penalty += abs(yaw)/2

    return penalty
}

fun LanderController.penalty2(params: LanderParams): Double {

    val vel = params.velocity
    val dd = abs(params.milestone - surface.flatMilestone)

    var penalty = 0.0
    // 1. distance penalty - above 100 when farther than crash site, if < 0.9 from landing zone then 0 penalty
    if(dd > surface.flatExtents){
        penalty += 100 + 100 * (dd-surface.flatExtents)/surface.flatExtents
    } else if (dd > surface.flatExtents*0.9)
        penalty += (dd/surface.flatExtents)

    // 2. velocity penalty
    if(-params.velocity.y > 40) // linear penalty above 40
        penalty += 30 + -params.velocity.y
    else if(-params.velocity.y > 35) // small penalty in 40-35 region
        penalty += 30 + -params.velocity.y

    if(-params.velocity.x > 20) // linear penalty above 20
        penalty += 15 + -params.velocity.x
    else if(-params.velocity.x > 15) // small penalty in 20-15 region
        penalty += 15 + -params.velocity.x

    // 0-15 penalty for yaw
    penalty += 10*abs(params.yaw)/90

    // small prize for fuel
    penalty -= (params.fuel/initialParams.fuel)*5

    return penalty
}