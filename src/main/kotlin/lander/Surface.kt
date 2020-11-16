package lander

class Surface(val terrain: List<Vector2>){
    /** center of landing site along the surface */
    val flatMilestone: Double

    /** 2x extents is flat zone width */
    val flatExtents: Double

    /** length along surface */
    val surfaceLength: Double

    init {
        var s = 0.0
        var milestone = 0.0
        var extent = 0.0
        terrain.zipWithNext().forEach { (p1, p2) ->
            if (p1.y almostEquals p2.y) { // found flat segment
                extent = distance(p1, p2) / 2 // middle of flat zone
                milestone = s + extent
            }
            s += distance(p1, p2)
        }
        flatMilestone = milestone
        flatExtents = extent
        surfaceLength = s
    }
}