package UTTT

import java.util.*

class UtttState : GameState {
    private val grid = Array(3) { arrayOf(TttState(), TttState(), TttState()) }
    protected var winner = Player.NONE
    private var freeSpaces = 9

    @Throws(InvalidAction::class)
    override fun play(action: Action): Player {
        if (action.row < 0 || action.row >= 9 || action.col < 0 || action.col >= 9) throw InvalidAction("Invalid move!")
        val subgrid = grid[action.row / 3][action.col / 3]
        var winner = subgrid.play(action)
        if (winner !== Player.NONE || subgrid.freeSpaces == 0) { // winner or draw
            --freeSpaces
            winner = checkWinner()
        }
        return winner
    }

    override fun getValidActions(player: Player): List<Action> {
        val validActions: MutableList<Action> = ArrayList()
        if (winner === Player.NONE) {
            for (x in 0..2) for (y in 0..2) validActions.addAll(grid[x][y].getValidActions(player))
        }
        return validActions
    }

    private fun checkWinner(): Player {
        for (i in 0..2) {
            // check rows
            if (grid[i][0].winner !== Player.NONE && grid[i][0] === grid[i][1] && grid[i][0] === grid[i][2])
                return grid[i][0].winner

            // check cols
            if (grid[0][i].winner !== Player.NONE && grid[0][i] === grid[1][i] && grid[0][i] === grid[2][i])
                return grid[0][i].winner
        }

        // check diags
        if (grid[0][0].winner !== Player.NONE && grid[0][0] === grid[1][1] && grid[0][0] === grid[2][2])
            return grid[0][0].winner
        if (grid[2][0].winner !== Player.NONE && grid[2][0] === grid[1][1] && grid[2][0] === grid[0][2])
            return grid[2][0].winner
        return Player.NONE
    }

    override fun clone(): UtttState {
        val clonedState = UtttState()
        for (x in 0..2) for (y in 0..2) clonedState.grid[x][y] = grid[x][y].clone()
        clonedState.winner = winner
        clonedState.freeSpaces = freeSpaces
        return clonedState
    }
}