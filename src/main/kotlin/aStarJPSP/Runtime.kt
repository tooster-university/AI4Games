package aStarJPSP

import java.util.*

fun main(args: Array<String>) {
    val input = Scanner(System.`in`)
    val (width, height) = (input.nextInt() to input.nextInt()) // map dimensions

    val (sourceCol, sourceRow) = (input.nextInt() to input.nextInt()) // coordinate of the starting tile
    val (goalCol, goalRow) = (input.nextInt() to input.nextInt()) // coordinate of the goal tile

    val openCnt = input.nextInt() // number of open tiles on the map
    val openList = mutableListOf<List<Int>>()
    for (i in 0 until openCnt) {
        val line = mutableListOf<Int>()
        repeat(10) { line.add(input.nextInt()) } // col, row, N, NE, ...
        openList.add(line)
    }

    System.err.println(
        """
        |$width $height
        |$sourceCol $sourceRow
        |$goalCol $goalRow
        |$openCnt
        |${openList.map { it.joinToString(" ") }.joinToString("\n")}
        """.trim().trimMargin()
    )

    val map = Map(width, height, openList)

    // In order of nodes visited by the JPS+ algorithm, a line containing "nodeColumn nodeRow parentColumn parentRow givenCost".
    println("$sourceCol $sourceRow -1 -1 0.00")

    // game loop
    while (true) {

        // Write an action using println()
        // To debug: System.err.println("Debug messages...");


        // In order of nodes visited by the JPS+ algorithm, a line containing "nodeColumn nodeRow parentColumn parentRow givenCost".
        println("3 4 0 2 3.14")
    }
}