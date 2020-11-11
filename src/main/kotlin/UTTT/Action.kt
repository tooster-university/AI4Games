package UTTT

data class Action(var player: Player, val row: Int, val col: Int) {
    override fun toString(): String {
        return "$row $col"
    }

    override fun equals(other: Any?): Boolean {
        return if (other is Action) {
            col == other.col && row == other.row
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        var result = row
        result = 31 * result + col
        return result
    }
}

class InvalidAction(message: String?) : Exception(message) {
    companion object {
        private const val serialVersionUID = -8185589153224401564L
    }
}