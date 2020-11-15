package UTTT

enum class Player {
    O {
        override val index: Int = -1

        override fun nextPlayer(): Player {
            return X
        }
    },  // means either no field on board
    NONE {
        override val index: Int = 0

        override fun nextPlayer(): Player {
            return NONE
        }
    },
    X {
        override val index: Int = 1

        override fun nextPlayer(): Player {
            return O
        }
    };

    abstract val index: Int
    abstract fun nextPlayer(): Player
}