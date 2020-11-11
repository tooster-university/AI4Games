package lander
// from https://cp-algorithms.com/geometry/segments-intersection.html

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

const val EPS = 1e-6

fun onSegment(p: Vector2, q: Vector2, r: Vector2) =
    min(p.x, r.x) <= q.x && q.x <= max(p.x, r.x) &&
            min(p.y, r.y) <= q.y && q.y <= max(p.y, r.y)

fun orientation(p: Vector2, q: Vector2, r: Vector2): Int {
    val prod = (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y)
    if (abs(prod) < EPS) return 0
    return if(prod > 0) 1 else 2
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