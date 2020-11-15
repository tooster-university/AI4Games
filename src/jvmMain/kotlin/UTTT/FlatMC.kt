package UTTT

data class actionQuality(var simCnt: Int, var reward: Int)


fun findOptimalAction(timeBoundMilis: Long, currentPlayer: Player, gameState: GameState): Action {
    var lastStepDuration: Long = 0
    var timeLeft = timeBoundMilis
    var stepStartTime: Long

    // shouldn't happen - findOptimalActions should not be run on terminal state
    val actionStateMap: MutableMap<Action, actionQuality> = mutableMapOf()
    val validActions = gameState.getValidActions(currentPlayer)
    if (validActions.isEmpty()) return Action(Player.NONE, -1, -1)

    while (timeLeft - lastStepDuration > 0) {
        stepStartTime = System.currentTimeMillis()
        // pick random action
        val randomAction = validActions.random()
        randomAction.player = currentPlayer

        // simulate after random state
        val winner = playRandomSim(gameState.clone(), randomAction)

        // update statistics
        val stateQuality = actionStateMap.getOrDefault(randomAction, actionQuality(0, 0))
        stateQuality.simCnt++
        stateQuality.reward += currentPlayer.index * winner.index // reward: win=+1 draw = 0 lose=-1
        actionStateMap[randomAction] = stateQuality

        // measure step size
        lastStepDuration = System.currentTimeMillis() - stepStartTime
        timeLeft -= lastStepDuration
    }

    // return best quality action i.e. one with highest average reward/sims
    return actionStateMap.maxByOrNull { (_, quality) ->
        quality.reward.toFloat() / quality.simCnt.toFloat()
    }!!.key
}

// return winner or EMPTY on draw
private fun playRandomSim(mutableState: GameState, action: Action): Player {
    val winner = mutableState.play(action)
    val validActions = mutableState.getValidActions(action.player.nextPlayer())
    return if (validActions.isEmpty())
        winner
    else playRandomSim(mutableState, validActions.random().apply { player = action.player.nextPlayer() })
}