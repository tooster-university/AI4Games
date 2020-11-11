package UTTT

interface GameState {
    // returns empty collection if state is terminal
    fun getValidActions(player: Player): List<Action>

    @Throws(InvalidAction::class)
    fun play(action: Action): Player
    fun clone(): GameState
}