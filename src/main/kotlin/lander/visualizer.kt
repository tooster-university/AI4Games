package lander

import com.github.nwillc.ksvg.RenderMode
import com.github.nwillc.ksvg.elements.SVG
import java.io.File
import java.lang.StringBuilder

lateinit var visualization: SVG
lateinit var visualizationFile: File
val visualize = true

// creates SVG from engine path
fun updateVisualization(path: List<Vector2>, sw: String, sc: String) {
    fun svgify(vec: Vector2): String = "${(vec.x/10).toInt()} ${(300 - vec.y/10).toInt()}"
    visualization.path {
        fill = "none"
        stroke = sc
        strokeWidth = sw
        d = path.subList(1, path.size)
            .joinToString(
                prefix = "M ${svgify(path[0])} ",
                separator = " "
            ) { p -> "L ${svgify(p)}" }
    }
    // if cannot write then we want to see error why
}



fun saveVisualizationToFile(populationNumber: Int) {
    if (::visualization.isInitialized) { // only when a visualization already exists
        val sb = StringBuilder()
        visualization.text {
            x = "20"
            y = "20"
            body = "Population: $populationNumber"
            fontFamily = "monospace"
            fontSize = "20px"
            fill = "black"
        }
        visualization.render(sb, RenderMode.FILE)

        if (!::visualizationFile.isInitialized) {
            visualizationFile = File("visualization.svg")
            visualizationFile.createNewFile() // unsecure create file but whatever
        }

        visualizationFile.writeText(sb.toString())
    }
    visualization = SVG.svg(true) {
        width = "700"
        height = "300"
        style { body = """
            svg { fill: white; }            
        """.trimIndent()}
    }
    updateVisualization(surface, "3", "black")
}

// print population. First will be printed in greener color
fun Population.visualize(populationNumber: Int) {
    this.subList(1, size).forEach {
        it.simulate(stepsPassed)
        updateVisualization(engine.path, "1", "lime")
    }
    this[0].simulate(stepsPassed)
//    System.err.println("#${}")
    updateVisualization(engine.path, "1", "red")
    saveVisualizationToFile(populationNumber)
}