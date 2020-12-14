package aStarJPSP

import aStarJPSP.Map.Tile.*
import aStarJPSP.Direction.*
import java.util.*

class Map constructor(val representation: String) {
    val width: Int
    val height: Int
    val borders: EnumMap<Direction, Int> // offset of borders, so N border is top etc.
    val tiles: Array<Array<Tile>>

    companion object {
        val deltas = EnumMap(
            mapOf(
                N to (0 to -1), E to (1 to 0), S to (0 to 1), W to (-1 to 0),
                NE to (1 to -1), SE to (1 to 1), SW to (-1 to 1), NW to (-1 to -1)
            )
        )
    }

    init {
        val lines = representation.split('\n');
        height = lines.size
        width = lines[0].length
        borders = EnumMap(mapOf(N to 0, E to width - 1, S to height - 1, W to 0))
        tiles = Array(height) { row ->
            Array(width) { col ->
                if (lines[row][col] == '.') EmptyTile(row, col) else WallTile(row, col)
            }
        }

        // 0. find primary jump points
        tiles.flatten().forEach { tile: Tile ->
            if (tile is EmptyTile) tile.primaryJumpPoints.addAll(Direction.values.filter { fromDir ->
/*@FUCKOFF*/    tile.neighbor(fromDir.LEFT_BACK) is WallTile && tile.neighbor(fromDir.LEFT) is EmptyTile ||
/*@FUCKON*/     tile.neighbor(fromDir.RIGHT_BACK) is WallTile && tile.neighbor(fromDir.RIGHT) is EmptyTile
            })
        }

        fun cardinalSweep(main: Direction) { // main is direction of sweep
            val cross = if (main == N || main == S) E else S
            var mainTile = tileAt(if (main == N) height - 1 else 0, if (main == W) width - 1 else 0)
            do { // major loop
                var crossTile = mainTile

                var (count, jpSeen) = -1 to false

                do { // minor loop

                    if (crossTile is WallTile) {
                        count = -1
                        jpSeen = false
                        crossTile.distance[main.BACK] = 0
                        continue
                    }
                    val tile = crossTile as DataTile

                    tile.distance[main.BACK] = if (jpSeen) ++count else --count
                    if (tile is EmptyTile && tile.primaryJumpPoints.contains(main.BACK)) {
                        count = 0
                        jpSeen = true;
                    }

                    crossTile = crossTile.neighbor(cross)
                } while (crossTile !is BorderTile)

                mainTile = mainTile.neighbor(main)
            } while (mainTile !is BorderTile)
        }

        cardinalSweep(E)
        cardinalSweep(W)
        cardinalSweep(S)
        cardinalSweep(N)

        fun diagonalSweep(main: Direction) {
            val cross = if (main == N || main == S) E else S
            var mainTile = tileAt(if (main == N) height - 1 else 0, if (main == W) width - 1 else 0)
            do { // major loop
                var crossTile = mainTile
                do { // minor loop
                    if (crossTile !is EmptyTile) continue
                    val tile = crossTile as EmptyTile

                    val B = tile.neighbor(main.BACK)
                    val LB = tile.neighbor(main.LEFT_BACK)
                    val RB = tile.neighbor(main.RIGHT_BACK)
                    val L = tile.neighbor(main.LEFT)
                    val R = tile.neighbor(main.RIGHT)

                    // left-back diagonal
                    if (B !is EmptyTile || LB !is EmptyTile || L !is EmptyTile)
                        tile.distance[main.LEFT_BACK] = 0 // wall one away
                    else if (LB.distance[main.BACK]!! > 0 || LB.distance[main.LEFT]!! > 0)
                        tile.distance[main.LEFT_BACK] = 1 // straight jump point
                    else {
                        val jmpDist = LB.distance[main.LEFT_BACK]!!
                        tile.distance[main.LEFT_BACK] = jmpDist + if (jmpDist > 0) 1 else -1
                    }

                    // right-back diagonal
                    if (B !is EmptyTile || RB !is EmptyTile || R !is EmptyTile)
                        tile.distance[main.RIGHT_BACK] = 0 // wall one away
                    else if (RB.distance[main.BACK]!! > 0 || RB.distance[main.RIGHT]!! > 0)
                        tile.distance[main.RIGHT_BACK] = 1 // straight jump point
                    else {
                        val jmpDist = RB.distance[main.RIGHT_BACK]!!
                        tile.distance[main.RIGHT_BACK] = jmpDist + if (jmpDist > 0) 1 else -1
                    }

                    crossTile = crossTile.neighbor(cross)
                } while (crossTile !is BorderTile)

                mainTile = mainTile.neighbor(main)
            } while (mainTile !is BorderTile)
        }

        cardinalSweep(E)
        cardinalSweep(W)
        cardinalSweep(S)
        cardinalSweep(N)
    }

    fun tileAt(row: Int, col: Int): Tile =
        tiles.getOrElse(row) { emptyArray() }.getOrElse(col) { BorderTile(row, col) }

    fun Tile.neighbor(dir: Direction): Tile {
        val (dr, dc) = deltas[dir]!!
        return tileAt(row + dr, col + dc)
    }

    fun EmptyTile.canMoveTowards(dir: Direction): Boolean {
        val targetNode = neighbor(dir)
        if (targetNode is WallTile || targetNode is BorderTile) return false
        return dir.cardinal || dir.diagonal && neighbor(dir.LEFT_FRONT) is EmptyTile || neighbor(dir.RIGHT_FRONT) is EmptyTile
    }

    // 2D iteration over array major along main, minor along cross
    @Deprecated("use flatmap instead or copy sweep code")
    private fun forEach(
        mainDirection: Direction,
        crossDirection: Direction,
        action: (Tile, main: Direction, cross: Direction) -> Unit
    ) {
        assert(mainDirection.cardinal && crossDirection orthogonalTo mainDirection)
        val rowMajor = mainDirection == N || mainDirection == S
        for (major in borders[mainDirection.BACK]!!..borders[mainDirection]!!)
            for (minor in borders[crossDirection.BACK]!!..borders[crossDirection]!!)
                action(if (rowMajor) tileAt(major, minor) else tileAt(minor, major), mainDirection, crossDirection)
    }

    sealed class Tile(val row: Int, val col: Int) {
        class BorderTile(row: Int, col: Int) : Tile(row, col) // this one is fictional bounding box tile
        open class DataTile(row: Int, col: Int) : Tile(row, col) {
            val distance: EnumMap<Direction, Int> = EnumMap(Direction::class.java)
        }

        class WallTile(row: Int, col: Int) : DataTile(row, col)
        class EmptyTile(row: Int, col: Int) : DataTile(row, col) {
            val primaryJumpPoints: EnumSet<Direction> = EnumSet.noneOf(Direction::class.java)
        }
    }
}