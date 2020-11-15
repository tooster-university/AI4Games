package lander

import kotlinx.serialization.Serializable
import kotlin.math.sqrt

@Serializable
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
