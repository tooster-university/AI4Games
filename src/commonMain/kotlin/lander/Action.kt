package lander

class Action(val rotation: Int, val thrust: Int) {
    override fun toString(): String = "$rotation $thrust"
}