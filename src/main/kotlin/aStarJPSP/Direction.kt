package aStarJPSP

import java.util.*
import kotlin.math.abs

enum class Direction {
    N, NE, E, SE, S, SW, W, NW;

    companion object {
        val values = values()
        val cardinals = values.filter { it.cardinal }
        val diagonals = values.filter { it.diagonal }

        /** value from rose, where 0=N, 1=NE ... and other are relative to N */
        fun getRose(index: Int) = values[Math.floorMod(index, values.size)]
    }

    val cardinal get() = this.ordinal % 2 == 0
    val diagonal get() = !cardinal
    infix fun orthogonalTo(other: Direction): Boolean = abs(this.ordinal - other.ordinal) == 2
    fun rotated(cwOffset: Int) = getRose(this.ordinal + cwOffset)
    val LEFT_FRONT get() = rotated(-1) // counter clockwise
    val RIGHT_FRONT get() = rotated(+1) // clockwise
    val LEFT get() = rotated(-2)
    val RIGHT get() = rotated(+2)
    val LEFT_BACK get() = rotated(-3)
    val RIGHT_BACK get() = rotated(+3)
    val BACK get() = rotated(+4)

    val nextDirections: EnumSet<Direction>
        get() =
            if (diagonal) EnumSet.of(LEFT_FRONT, this, RIGHT_FRONT)
            else EnumSet.of(LEFT, LEFT_FRONT, this, RIGHT_FRONT, RIGHT)
}