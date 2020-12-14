package aStarJPSP

import aStarJPSP.Direction.*
import java.util.*
import java.io.*
import java.math.*

fun main(args: Array<String>) {
    val input = Scanner(System.`in`)
    val width = input.nextInt() // Width of the map
    val height = input.nextInt() // Height of the map
    var mapRows = mutableListOf<String>()
    for (i in 0 until height) {
        mapRows.add(input.next()) // A single row of the map consisting of passable terrain ('.') and walls ('#')
    }

    val map = Map(mapRows.joinToString(separator = "\n"))

    map.tiles.flatten().forEach { tile ->
        if (tile is Map.Tile.EmptyTile) {
            println(
                "${tile.col} ${tile.row}\t ${
                    tile.distance.values.joinToString(" ") { it.toString().padStart(3, ' ') }
                }"
            )
        }
    }
    // Write an action using println()
    // To debug: System.err.println("Debug messages...");
}