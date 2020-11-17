package lander
// from https://cp-algorithms.com/geometry/segments-intersection.html

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

const val EPS = 1e-6

fun onSegment(p: Vector2, q: Vector2, r: Vector2) =
    min(p.x, r.x) <= q.x && q.x <= max(p.x, r.x) &&
    min(p.y, r.y) <= q.y && q.y <= max(p.y, r.y)

fun orientation(p: Vector2, q: Vector2, r: Vector2): Int {
    val prod = (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y)
    if (abs(prod) < EPS) return 0
    return if (prod > 0) 1 else 2
}

// returns distance along surface from the landing site
fun collides(a1: Vector2, a2: Vector2, b1: Vector2, b2: Vector2): Boolean {
    val o1 = orientation(a1, a2, b1)
    val o2 = orientation(a1, a2, b2)
    val o3 = orientation(b1, b2, a1)
    val o4 = orientation(b1, b2, a2)

    // General case

    // General case
    if (o1 != o2 && o3 != o4) return true

    // Special Cases

    // Special Cases
    if (o1 == 0 && onSegment(a1, b1, a2)) return true
    if (o2 == 0 && onSegment(a1, b2, a2)) return true
    if (o3 == 0 && onSegment(b1, a1, b2)) return true
    return o4 == 0 && onSegment(b1, a2, b2)
}


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

fun distance(p: Vector2, q: Vector2) = sqrt((p.x - q.x) * (p.x - q.x) + (p.y - q.y) * (p.y - q.y))

infix fun Double.almostEquals(other: Double) = abs(this - other) < EPS

data class Action(val rotation: Int, val thrust: Int) {
    override fun toString(): String = "$rotation $thrust"
}

fun lerp(a: Double, b: Double, fraction: Double): Double = a + (b - a) * fraction
fun lerp(a: Int, b: Int, fraction: Double): Int = lerp(a.toDouble(), b.toDouble(), fraction).toInt()
fun lerp(a: Vector2, b: Vector2, fraction: Double): Vector2 =
    Vector2(lerp(a.x, b.x, fraction), lerp(a.y, b.y, fraction))

fun lerp(a: Action, b: Action, fraction: Double): Action =
    Action(lerp(a.rotation, b.rotation, fraction), lerp(a.thrust, b.thrust, fraction))
