package aStarJPSP

import aStarJPSP.Map.Tile
import java.util.*
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sqrt
import aStarJPSP.Direction.*
import java.lang.IllegalArgumentException

fun main(args: Array<String>) {
    val input = Scanner(System.`in`)
    val (width, height) = (input.nextInt() to input.nextInt()) // map dimensions

    val (sourceCol, sourceRow) = (input.nextInt() to input.nextInt()) // coordinate of the starting tile
    val (goalCol, goalRow) = (input.nextInt() to input.nextInt()) // coordinate of the goal tile

    val openCnt = input.nextInt() // number of open tiles on the map
    val precomputedTiles = mutableListOf<List<Int>>()
    for (i in 0 until openCnt) {
        val line = listOf(
            input.nextInt(), input.nextInt(),
            input.nextInt(), input.nextInt(), input.nextInt(), input.nextInt(),
            input.nextInt(), input.nextInt(), input.nextInt(), input.nextInt()
        )
//        repeat(10) { line.add(input.nextInt()) } // col, row, N, NE, ...
        precomputedTiles.add(line)
    }

//    System.err.println(
//         """
//         |$width $height
//         |$sourceCol $sourceRow
//         |$goalCol $goalRow
//         |$openCnt
//         """.trim().trimMargin()
//     )

//     openList.map { it.map {it.toString().padStart(3, ' ')}.joinToString(" ") }.forEach {System.err.println(it)}

    val map = Map(width, height, precomputedTiles)

    map.pathFind(map.tileAt(sourceRow, sourceCol) as Tile.EmptyTile, map.tileAt(goalRow, goalCol) as Tile.EmptyTile)
}

fun Map.pathFind(sourceTile: Tile.EmptyTile, goalTile: Tile.EmptyTile) {

    // open list doesn't conform to any standard data structures. It requires:
    //  i) lookup by tile coordinates
    //  ii) ordering on cost
    //  iii) updating existing values
    // thus an ugly hack of PQ + removeIf + insert is used, given the PQ sizes are small
    val openList = PriorityQueue<Node>() { n1, n2 ->
        when {
            n1.tile sameAs goalTile -> -1 // goal tile is always the best
            n2.tile sameAs goalTile -> 1 //  ^
            else -> sign(n1.finalCost - n2.finalCost).toInt()
        }
    } // nodes to consider
    val closedSet = mutableSetOf<Tile>() // found path

    openList.add(Node(sourceTile)) // with dummy parent data

    val SQRT2 = sqrt(2.0)

    while (openList.isNotEmpty()) {
        val node = openList.poll()
        val tile = node.tile as Tile.EmptyTile

        closedSet.add(tile) // prevent adding expanded nodes

        // 0. print processed node - puzzle output
        println(
            "${tile.col} ${tile.row} " +
            "${(node.parent?.tile?.col ?: -1).toString().padStart(3, ' ')} " +
            "${(node.parent?.tile?.row ?: -1).toString().padStart(3, ' ')} " +
            node.givenCost.toString().padStart(5, ' ')
        )

        if (tile == goalTile) return // goal reached CHECKME maybe we should early exit when ADDING to queue instead

        // 1. for the starting node all directions are valid
        val validDirections =
            if (node.parent == null) EnumSet.allOf(Direction::class.java)
            else node.from.nextDirections

        validDirections.forEach dir@{ dir ->
            var nextTile: Tile? = null // aka target node
            var givenCost: Double = Double.NaN

            val minDist = min(tile rowDist goalTile, tile colDist goalTile)
            val distance = tile.distance[dir]!!

            // 2. consider jump points
            when {
                // i) target is in cardinal direction and closer than wall / JP - target JP
                dir.cardinal &&
                tile.exactlyTargets(goalTile, dir) &&
                tile axialDist goalTile <= abs(distance) -> {
                    nextTile = goalTile
                    givenCost = node.givenCost + (tile axialDist goalTile)
                }
                // ii) target in general diagonal direction - target JP
                dir.diagonal &&
                minDist > 0 && // if minDist is 0 we may loop forever
                tile.generallyTargets(goalTile, dir) &&
                (minDist <= abs(distance)) -> {
                    nextTile = tile.neighbor(dir, minDist)
                    givenCost = node.givenCost + (SQRT2 * minDist)
                }
                // iii) regular JP
                distance > 0 -> {
                    nextTile = tile.neighbor(dir, distance)
                    givenCost = distance * (if (dir.diagonal) SQRT2 else 1.0) + node.givenCost
                }
            }

            // 3. regular A*
            if (nextTile != null) {
                
                val found = openList.find { it.tile sameAs nextTile }
                if (!closedSet.contains(nextTile) && found == null){
                    openList.add(Node(nextTile, node, dir, givenCost, givenCost + (nextTile octileDistance goalTile)))
                } else if (found != null && givenCost < found.givenCost) {
                    openList.removeIf { it.tile sameAs nextTile }
                    openList.add(Node(nextTile, node, dir, givenCost, givenCost + (nextTile octileDistance goalTile)))
                }
            }
        }
    }

    // if goal not reached
    println("NO PATH")
}

// Node must hold information tile, parent (starting node has null),
//  direction it came from (starting doesn't matter) and cost to work
class Node(
    val tile: Tile,
    val parent: Node? = null,
    val from: Direction = N,
    val givenCost: Double = 0.0,
    val finalCost: Double = 0.0
);


fun Tile.exactlyTargets(target: Tile, direction: Direction): Boolean {
    return when (direction) {
        N -> col == target.col && target.row < row
        S -> col == target.col && target.row > row
        W -> row == target.row && target.col < col
        E -> row == target.row && target.col > col
        else -> throw IllegalArgumentException("direction must be cardinal")
    }
}

fun Tile.generallyTargets(target: Tile, direction: Direction): Boolean {
    return when (direction) { // CHECKME: maybe it must be strictly bigger/smaller ?
        NE -> target.col >= col && target.row <= row
        SE -> target.col >= col && target.row >= row
        NW -> target.col <= col && target.row <= row
        SW -> target.col <= col && target.row >= row
        else -> throw IllegalArgumentException("direction must be diagonal")
    }
}

// under strict assumption that tiles are coaxial
infix fun Tile.axialDist(other: Tile) = abs(col - other.col + row - other.row)
infix fun Tile.rowDist(other: Tile) = abs(row - other.row)
infix fun Tile.colDist(other: Tile) = abs(col - other.col)

infix fun Tile.octileDistance(other: Tile): Double {
    val (dx, dy) = abs(row - other.row) to abs(col - other.col)
    return (dx + dy) + (sqrt(2.0) - 2) * min(dx, dy)
}
