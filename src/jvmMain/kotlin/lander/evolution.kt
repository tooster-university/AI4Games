package lander

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

fun Chromosome.evaluate(): LanderParams = engine.simulateFlight(this)

typealias PopulationEvolver = (Population) -> Population
typealias FitnessFunction = LanderParams.() -> Double
typealias Ranking = List<Triple<Chromosome, LanderParams, Double>>

/** solver using rolling horizon tactic. PopulationMutator
 * @param evolve    Function evolving the population. Returns sorted chromosomes with fitness.
 *                  if population passed to evolver is empty, then it should create starting population
 */
fun rollingHorizonSolver(evolve: PopulationEvolver, fitness: FitnessFunction) {
    var horizon = 0
    var population = evolve(emptyList())
    var rollingChromosome = listOf<Gene>() // resulting chromosome aka series of taken actions
    var populationNr = 0 // ordinal of current population

    while (true) {
        val solverStart = System.currentTimeMillis() // when solver started
        lateinit var ranking: Ranking
        // evolve population as much as possible in given timeframe
        while (System.currentTimeMillis() - solverStart < 995) { // FIXME: fit as much based on extrapolated time
            // create ranking as 3 column
            ranking = population.map {
                val ev = it.evaluate()
                val fit = ev.fitness()
                Triple(it, ev, fit)
            }.sortedBy { it.third }

            population = evolve(ranking.map { it.first })
            ++populationNr

        }

        ranking.subList(1, ranking.size).forEach { image.addPath(it.second.path, "1", "lime") }
        image.addPath(ranking[0].second.path, "1", "red")
        image.renderPicture(populationNr)
        newImage()


        val best = population[0]
        // System.err.println("BEST: ${ranking[0].third}\n${ranking[0].second}")

        ++horizon

        println("${best[0]}") // output best action from chromosome
        rollingChromosome = rollingChromosome + best[0] // append new gene to rolling chromosome
        population.map { it.subList(1, it.size) } // remove first genes


        if (!local)
            engine.calibrate(readSensors()) // read new input
        else {
            val p = engine.params
            if (engine.moveAndCheckCollided(best[0], p)) {
                System.err.println("OK? ${engine.params.acceptableLanding()}\n${engine.params}")
                return
            }
            engine.params = p
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

fun PopulationEvolver.smoother(): PopulationEvolver = { p -> this(p).map { it.smoothen() } }

fun readSensors(): LanderParams = LanderParams(
    position = Vector2(input.nextInt(), input.nextInt()),
    velocity = Vector2(input.nextInt(), input.nextInt()),
    fuel = input.nextInt(),
    yaw = input.nextInt(),
    power = input.nextInt()
)

fun landerControl(surface: List<Vector2>, initialParams: LanderParams) {

    val N = input.nextInt() // the number of points used to draw the surface of Mars.

    // initialize the engine
    for (i in 0 until N) {
        val landX = input.nextInt() // X coordinate of a surface point. (0 to 6999)
        val landY =
            input.nextInt() // Y coordinate of a surface point. By linking all the points together in a sequential fashion, you form the surface of Mars.
        surface.add(Vector2(landX.toDouble(), landY.toDouble()))
    }

    engine = Engine(surface.toTypedArray())
    engine.params = readSensors()

    newImage()


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

    rollingHorizonSolver(muLambdaEvolver(200, 50), LanderParams::penalty2)
}

/*

// example
 6   0 1500 1000 2000 2000 500 3500 500 5000 1500 6999 1000
 5000 2500 -50 0 1000 90 0

// easy on right
 7 0 100 1000 500 1500 1500 3000 1000 4000 150 5500 150 6999 800
 2500 2700 0 0 550 0 0

// initial speed, correct side
 10 0 100 1000 500 1500 100 3000 100 3500 500 3700 200 5000 1500 5800 300 6000 1000 6999 2000
 6500 2800 -100 0 600 600 0

// initial speed, wrong side
7 0 100 1000 500 1500 1500 3000 1000 4000 150 5500 150 6999 800
6500 2800 -90 0 750 750 0

// deep canyon
20 0 1000 300 1500 350 1400 500 2000 800 1800 1000 2500 1200 2100 1500 2400 2000 1000 2200 500 2500 100 2900 800 3000 500 3200 1000 3500 2000 3800 800 4000 200 5000 200 5500 1500 6999 2800
500 2700 100 0 800 800 0
 */