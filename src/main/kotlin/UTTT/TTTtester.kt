package UTTT

import java.util.*

fun main() {
    val input = Scanner(System.`in`)
    val gameState = TttState()
    // game loop
    while (true) {
        val opponentRow = input.nextInt()
        val opponentCol = input.nextInt()
        gameState.play(Action(Player.X, opponentRow, opponentCol))
        System.err.println(gameState.winner)
        val action = findOptimalAction(1000, Player.O, gameState)
        gameState.play(action)
        System.err.println(gameState.winner)
        println("${action.row} ${action.col}")
    }
}