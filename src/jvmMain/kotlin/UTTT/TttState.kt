package UTTT

import java.util.*
import UTTT.Player.*

open class TttState : GameState {
    private var grid = Array(3) { arrayOf(NONE, NONE, NONE) }
    var winner: Player = NONE
    var freeSpaces = 9

    override fun getValidActions(player: Player): List<Action> {
        val validActions: MutableList<Action> = ArrayList()
        if (winner === NONE) {
            for (x in 0..2) {
                for (y in 0..2) {
                    if (grid[x][y] === NONE) {
                        validActions.add(Action(player, x, y))
                    }
                }
            }
        }
        return validActions
    }

    @Throws(InvalidAction::class)
    override fun play(action: Action): Player {
        if (action.row < 0 || action.row >= 3
            || action.col < 0 || action.col >= 3
            || grid[action.row][action.col] !== NONE
        ) {
            throw InvalidAction("Invalid move!")
        }


        // update grid
        grid[action.row][action.col] = action.player
        --freeSpaces
        winner = checkWinner()
        return winner
    }

    override fun clone(): TttState {
        val clonedState = TttState()
        for (x in 0..2) for (y in 0..2) clonedState.grid[x][y] = grid[x][y]
        clonedState.winner = winner
        clonedState.freeSpaces = freeSpaces
        return clonedState
    }

    private fun checkWinner(): Player {
        for (i in 0..2) {
            // check rows
            if (grid[i][0] !== NONE && grid[i][0] === grid[i][1] && grid[i][0] === grid[i][2]) return grid[i][0]

            // check cols
            if (grid[0][i] !== NONE && grid[0][i] === grid[1][i] && grid[0][i] === grid[2][i]) return grid[0][i]
        }

        // check diags
        if (grid[0][0] !== NONE && grid[0][0] === grid[1][1] && grid[0][0] === grid[2][2]) return grid[0][0]
        return if (grid[2][0] !== NONE && grid[2][0] === grid[1][1] && grid[2][0] === grid[0][2]) grid[2][0] else NONE
    }

    override fun toString(): String {
        val sb = StringBuilder();
        for (x in 0..2) {
            for (y in 0..2) {
                sb.append(
                    when (grid[x][y]) {
                        NONE -> '_';
                        O -> 'O';
                        X -> 'X';
                    }
                );
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}