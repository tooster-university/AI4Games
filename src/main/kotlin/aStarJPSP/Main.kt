package aStarJPSP

import java.util.*
import java.io.*
import java.math.*

fun main(args: Array<String>) {
    val input = Scanner(System.`in`)
    val width = input.nextInt() // Width of the map
    val height = input.nextInt() // Height of the map
    var mapString = ""
    for (i in 0 until height) {
        mapString += input.next() + "\n" // A single row of the map consisting of passable terrain ('.') and walls ('#')
    }

    val map = Map(mapString)

    map.tiles.flatten().forEach { tile ->
        if (tile is Map.Tile.EmptyTile)
            println("${tile.col} ${tile.row} " + tile.distance.values.joinToString(separator = " "))
    }
    // Write an action using println()
    // To debug: System.err.println("Debug messages...");


    // For each empty tile of the map, a line containing "column row N NE E SE S SW W NW".
    println("0 0 -1 -1 -1 -1 -1 -1 -1 -1")
    println("1 2 -1 3 -2 4 0 0 2 -2")
    println("...")
}