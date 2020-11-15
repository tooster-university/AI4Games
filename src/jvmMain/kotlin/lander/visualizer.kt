package lander

import com.github.nwillc.ksvg.RenderMode
import com.github.nwillc.ksvg.elements.SVG
import java.io.File
import java.lang.StringBuilder

lateinit var image: SVG

/** creates SVG with surface */
fun newImage() {
    image =  SVG.svg(true) {
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
    }.also { it.addPath(surface, "3", "black") }
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
    addPath(population[0].path,  "1", "red")
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