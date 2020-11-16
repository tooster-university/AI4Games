package lander

import java.awt.Color
import java.util.*
import kotlin.math.*
import kotlin.random.Random

typealias Gene = Action
typealias Chromosome = List<Gene>
typealias Population = List<Chromosome>


// mutates gene completely and randomly
fun Gene.randomMutation() = Random.nextGene()

fun Random.nextGene(): Gene = Action(Random.nextInt(-90, 90 + 1), Random.nextInt(0, 4 + 1))
fun Random.nextMarkovGene(gene: Action): Gene = Action(
    Random.nextInt(max(-90, gene.rotation - 15), min(90, gene.rotation + 15) + 1),
    Random.nextInt(max(0, gene.thrust - 1), min(4, gene.thrust + 1))
)

fun Random.nextChromosome(length: Int): Chromosome = List(length) { Random.nextGene() }
fun Random.nextMarkovChromosome(length: Int): Chromosome {
    val ch = mutableListOf(Random.nextGene())
    repeat(length - 1) { ch.add(Random.nextMarkovGene(ch.last())) }
    return ch
}

fun Random.nextPopulation(populationSize: Int, chromosomeLength: Int) =
    List(populationSize) { Random.nextChromosome(chromosomeLength) }

fun Random.nextMarkovPopulation(populationSize: Int, chromosomeLength: Int) =
    List(populationSize) { Random.nextMarkovChromosome(chromosomeLength) }

// randomly mutate single gene
fun Gene.mutate(probability: Double = 0.05) = if (Random.nextDouble() <= probability) Random.nextGene() else this

// averages neighboring genes - returns first + averaged pairwise from second
fun Chromosome.smoothen() = this.subList(0, 1) + zipWithNext { g1, g2 ->
    Gene(((g1.rotation + g2.rotation) / 2.0).roundToInt(), ((g1.thrust + g2.thrust) / 2.0).roundToInt())
}

// mutates chromosome uniformly - each gene has given chance to mutate
fun Chromosome.uniformMutate(probability: Double = 0.05) =
    this.map { gene -> if (Random.nextDouble() <= probability) Random.nextGene() else gene }

// one crossover point
infix fun Chromosome.singleCrossover(other: Chromosome): Pair<Chromosome, Chromosome> {
    val p1 = Random.nextInt(size)
    return (this.subList(0, p1) + other.subList(p1, size)) to
            (other.subList(0, p1) + this.subList(p1, size))
}

// two crossover points
infix fun Chromosome.doubleCrossover(other: Chromosome): Pair<Chromosome, Chromosome> {
    val (a, b) = (Random.nextInt(size) to Random.nextInt(size))
    val (p1, p2) = (min(a, b) to max(a, b))
    return (this.subList(0, p1) + other.subList(p1, p2) + this.subList(p2, size)) to
            (other.subList(0, p1) + this.subList(p1, p2) + other.subList(p2, size))
}

fun Chromosome.uniformCrossover(other: Chromosome, probability: Double = 0.5): Pair<Chromosome, Chromosome> {
    assert(this.size == other.size)
    return this.zip(other).fold(mutableListOf<Gene>() to mutableListOf<Gene>(),
                                { (c1, c2), (g1, g2) ->
                                    return if (Random.nextDouble() <= probability)
                                        (c1 + g1) to (c2 + g2)
                                    else
                                        (c1 + g2) to (c2 + g1)
                                })
}

// returns fitness of this chromosome by simulating it's run.
//  this particular function focuses on 3 factors in this order:
//   1. distance along surface from the landing site
//   2. fuel
//   3. landing speed - as norm of velocity vector
//

fun Chromosome.evaluate(): EngineParams = engine.simulateFlight(this)

typealias PopulationEvolver = (Population) -> Population
typealias FitnessFunction = EngineParams.() -> Double
typealias Ranking = List<Triple<Chromosome, EngineParams, Double>>

/** solver using rolling horizon tactic. PopulationMutator
 * @param evolve    Function evolving the population. Returns sorted chromosomes with fitness.
 *                  if population passed to evolver is empty, then it should create starting population
 */
fun rollingHorizonSolver(evolve: PopulationEvolver, fitness: FitnessFunction) {
    var horizon = 0
    var population = evolve(emptyList())
    var rollingChromosome = listOf<Gene>() // resulting chromosome aka series of taken actions
    var populationNr = 0 // ordinal of current population

    io.clear()
    io.drawPopulation(population.map { it.evaluate() }, 0)
    io.paint()

    while (true) {
        val solverStart = System.currentTimeMillis() // when solver started
        lateinit var ranking: Ranking
        var evolves = 0 // if evolverRounds is > 0 then at most that many evolves would roll
        // evolve population as much as possible in given timeframe
        while (evolves < evolverRounds || evolverRounds < 0 && System.currentTimeMillis() - solverStart < 995) { // FIXME: fit as much based on extrapolated time
            ++evolves
            // create ranking as 3 column
            ranking = population.map {
                val ev = it.evaluate()
                val fit = ev.fitness()
                Triple(it, ev, fit)
            }.sortedBy { it.third }

            population = evolve(ranking.map { it.first })
            ++populationNr

        }
        io.clear()
        io.drawPopulation(ranking.map { it.second }, populationNr)
//        ranking.subList(1, ranking.size)
//            .forEach { io.drawPath(it.second.path, Color.GREEN)/*image.addPath(it.second.path, "1", "lime")*/ }
//        io.drawPath(ranking[0].second.path, Color.RED)        //image.addPath(ranking[0].second.path, "1", "red")
        //image.renderPicture(populationNr)
        //newImage()
        io.paint()
        io.error("best: ${ranking[0].third}")


        val best = population[0]
        // System.err.println("BEST: ${ranking[0].third}\n${ranking[0].second}")

        ++horizon

        println("${best[0]}") // output best action from chromosome
        rollingChromosome = rollingChromosome + best[0] // append new gene to rolling chromosome
        population.map { it.subList(1, it.size) } // remove first genes

        // engine.calibrate() // read new input

        if (engine.moveAndCheckCollided(best[0], engine.params)) {
            System.err.println("OK? ${engine.params.acceptableLanding()}\n${engine.params}")
            return
        }
    }
}

// mu+lambda evolution - no crossover, only mutations
// mu: how many best chromosomes to mutate
// lambda: how many worst to replace with mu
// here I don't use gaussian mutation but uniform mutation
// this is not a proper mu, lambda - it just mutates worst lambda solutions keeping the best
fun muLambdaEvolver(mu: Int, lambda: Int): PopulationEvolver = { ranking ->
    val chromosomeLength = 200
    if (ranking.isEmpty()) {
        Random.nextPopulation(mu + lambda, chromosomeLength)
    } else {
        // mutate leftover genes
        ranking.subList(0, mu) + ranking.subList(0, lambda).map { it.uniformMutate(0.1) }
    }
}

/** for debugging - should return constant population so the path should be the same as simulation*/
fun nonEvolver(): PopulationEvolver = { ranking ->
    if (ranking.isEmpty())
        Random.nextMarkovPopulation(1, 200)
    else
        ranking
}

fun PopulationEvolver.smoother(): PopulationEvolver = { p -> this(p).map { it.smoothen() } }

// how many steps till start passed
val surface = mutableListOf<Vector2>()
val evolverRounds = 1
lateinit var io: IO

fun main(args: Array<String>) {
    io = IO(MAP.valueOf(args[0]))

    val N = io.nextInt() // the number of points used to draw the surface of Mars.

    // initialize the engine
    for (i in 0 until N)
        surface.add(Vector2(io.nextInt().toDouble(), io.nextInt().toDouble()))

    engine = Engine(surface.toTypedArray())
    engine.params = EngineParams()
    engine.calibrate()

    System.err.printf(
        "X=%dm, Y=%dm, HSpeed=%dm/s, VSpeed=%dm/s\nfuel=%d, yaw=%d°, power=%d\n",
        engine.params.position.x.toInt(),
        engine.params.position.y.toInt(),
        engine.params.velocity.x.toInt(),
        engine.params.velocity.y.toInt(),
        engine.params.fuel,
        engine.params.yaw,
        engine.params.power
    )

//    rollingHorizonSolver(muLambdaEvolver(200, 50), EngineParams::penalty1)
    rollingHorizonSolver(nonEvolver(), EngineParams::penalty1)
}