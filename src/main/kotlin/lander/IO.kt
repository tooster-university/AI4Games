package lander

import com.github.nwillc.ksvg.RenderMode
import com.github.nwillc.ksvg.elements.SVG
import java.awt.*
import java.io.*
import java.lang.StringBuilder
import java.util.*
import javax.swing.JFrame
import javax.swing.JPanel
import kotlin.math.*

enum class MAP {
    EXAMPLE, EASY_RIGHT, INITIAL_SPEED_CORRECT_SIDE, INITIAL_SPEED_WRONG_SIDE, DEEP_CANYON, HIGH_GROUND
}

val maps = mapOf(
    MAP.EXAMPLE to
            """6   0 1500 1000 2000 2000 500 3500 500 5000 1500 6999 1000
        5000 2500 -50 0 1000 90 0""",

    MAP.EASY_RIGHT to
            """7 0 100 1000 500 1500 1500 3000 1000 4000 150 5500 150 6999 800
        2500 2700 0 0 550 0 0""",

    MAP.INITIAL_SPEED_CORRECT_SIDE to
            """10 0 100 1000 500 1500 100 3000 100 3500 500 3700 200 5000 1500 5800 300 6000 1000 6999 2000
        6500 2800 -100 0 90 600 0""",

    MAP.INITIAL_SPEED_WRONG_SIDE to
            """7 0 100 1000 500 1500 1500 3000 1000 4000 150 5500 150 6999 800
        6500 2800 -90 0 90 750 0""",

    MAP.DEEP_CANYON to
            """20 0 1000 300 1500 350 1400 500 2000 800 1800 1000 2500 1200 2100 1500 2400 2000 1000 2200 500 2500 100 2900 800 3000 500 3200 1000 3500 2000 3800 800 4000 200 5000 200 5500 1500 6999 2800
        500 2700 100 0 800 -90 0""",

    MAP.HIGH_GROUND to
            """ 20 0 1000 300 1500 350 1400 500 2100 1500 2100 2000 200 2500 500 2900 300 3000 200 3200 1000 3500 500 3800 800 4000 200 4200 800 4800 600 5000 1200 5500 900 6000 500 6500 300 6999 500
        6500 2700 -50 0 1000 90 0 
            """.trimIndent()
)

class IO(map: MAP) {
    private val frame = JFrame("lander")
    private val input = Scanner(StringReader(maps.getValue(map)))

    val visualization = Visualization()

    class Visualization : JPanel() {

        var terrain: Path2D = emptyList()
        var trajectories: List<Path2D> = emptyList()
        var best : Rank = Rank(emptyList(), LanderParams(), 0.0)
        var populationNumber: Int = 0

        val infoHeight = 150
        val dimensions = Dimension(900, (900 * 3.0/7.0).toInt() + infoHeight)

        init {
            background = Color.lightGray
        }

        override fun getPreferredSize(): Dimension = dimensions

        private fun Graphics2D.drawPath(path: Path2D, color: Color) {
            this.color = color
            drawPolyline(
                path.map { (width * it.x / 7000.0).toInt() }.toIntArray(),
                path.map { ((height - infoHeight) * (1.0 - it.y / 3000.0)).toInt() }.toIntArray(),
                path.size
            )
        }

        override fun paintComponent(g: Graphics?) {
            super.paintComponent(g)
            val g2 = g as Graphics2D
            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            )
            g2.drawPath(terrain, Color.BLACK)
            trajectories.forEach {
                g2.drawPath(it, Color.GREEN)
            }
            g2.drawPath(best.params.path, Color.RED)
            g2.color = Color.BLACK
            g2.drawString("Population $populationNumber", 10, 30)
            g2.fillRect(0, height-infoHeight, width, infoHeight)
            val step = (width - 10.0)/best.chromosome.size
            best.chromosome.fold( Vector2(step.toInt(), height - infoHeight/2) ) { off, action ->
                g2.color = when(action.thrust) {
                    1 -> Color(0x3a, 0x36, 0x85)
                    2 -> Color(0x75, 0x2f, 0x68)
                    3 -> Color(0xa9, 0x27, 0x4b)
                    4 -> Color(0xdd, 0x1e, 0x32)
                    else -> Color(0xee, 0x28, 0x33)
                }
                val dv = Vector2(-sin(action.rotation.toRadian()), cos(action.rotation.toRadian()))*(step*action.thrust)
                g2.drawLine(off.x.toInt(), off.y.toInt(), (off.x+dv.x).toInt(), (off.y - dv.y).toInt() )
                Vector2(off.x + step, off.y)
            }
            g2.color = Color.WHITE
            g2.drawString("Best params:   ${best.params.pretty()}", 10, height - infoHeight + 30)
        }
    }

    init {
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.add(visualization)
        frame.pack()
        frame.isVisible = true
    }

    fun error(s: String) = System.err.println(s)
    fun nextInt() = input.nextInt()
    fun nextParams() = LanderParams(
        position = Vector2(nextInt(), nextInt()),
        velocity = Vector2(nextInt(), nextInt()),
        fuel = nextInt(),
        yaw = nextInt(),
        power = nextInt(),
    )
    fun println(s: String) = System.out.println(s)

    // ------------------------------------------------- SVG -----------------------------------------------------------

    lateinit var image: SVG

    /** creates SVG with surface */
    fun newImage() {
        image = SVG.svg(true) {
            width = "700"
            height = "300"
            style {
                body = """
            svg { fill: white; }            
        """.trimIndent()
            }
            rect {
                x = "0"
                y = "0"
                width = "100%"
                height = "100%"
                fill = "gray"
            }
        }.also { it.addPath(visualization.terrain, "3", "black") }
    }

    /** add path to image */
    fun SVG.addPath(path: List<Vector2>, width: String, color: String) {
        // to project point to svg-space vector
        fun project(vec: Vector2): String = "${(vec.x / 10).toInt()} ${(300 - vec.y / 10).toInt()}"
        path {
            fill = "none"
            stroke = color
            strokeWidth = width
            d = path.subList(1, path.size)
                .joinToString(
                    prefix = "M ${project(path[0])} ",
                    separator = " "
                ) { p -> "L ${project(p)}" }
        }
        // if cannot write then we want to see error why
    }

    /** add population to image. First will be printed in greener color */
    fun SVG.addPopulation(population: List<LanderParams>, populationNumber: Int) {
        population.subList(1, population.size).forEach {
            addPath(it.path, "1", "lime")
        }
        addPath(population[0].path, "1", "red")
    }

    /** Renders picture to svg file */
    fun SVG.renderPicture(populationNumber: Int, filename: String = "lander.svg") {
        val sb = StringBuilder()

        text {
            x = "20"
            y = "20"
            body = "Population: $populationNumber"
            fontFamily = "monospace"
            fontSize = "20px"
            fill = "black"
        }

        render(sb, RenderMode.FILE)

        val image = File(filename)
        image.createNewFile() // unsecure create file but whatever
        image.writeText(sb.toString())
    }
}