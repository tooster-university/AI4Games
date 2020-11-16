package lander

import kotlin.math.*
import kotlin.random.Random

typealias Gene = Action
typealias Chromosome = List<Gene>
typealias Population = List<Chromosome>
typealias Ranking = List<Rank>
typealias PopulationEvolver = (Ranking) -> Population
typealias FitnessFunction = LanderController.(params: LanderParams) -> Double

data class Rank(val chromosome: Chromosome, val params: LanderParams, val fitness: Double)

val Ranking.population: Population get() = map { it.chromosome }

// ----------------------------------------------- evolution extensions on primitive data types ------------------------
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

/** randomly mutate single gene with probability */
fun Gene.mutate(probability: Double = 0.05) = if (Random.nextDouble() <= probability) Random.nextGene() else this.copy()

fun Chromosome.copy() = this.map { it.copy() }

// averages neighboring genes - returns first + averaged pairwise from second
fun Chromosome.smoothen() = this.subList(0, 1) + zipWithNext { g1, g2 ->
    Gene(((g1.rotation + g2.rotation) / 2.0).roundToInt(), ((g1.thrust + g2.thrust) / 2.0).roundToInt())
}

// mutates chromosome uniformly - each gene has given chance to mutate
fun Chromosome.uniformMutate(probability: Double = 0.05) =
    this.map { gene -> if (Random.nextDouble() <= probability) Random.nextGene() else gene.copy() }

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

// ------------------------------------------------------- CONTROLLER --------------------------------------------------

/** solver using rolling horizon tactic.
 * @param evolve    Function evolving the population. Returns sorted chromosomes with fitness.
 *                  if population passed to evolver is empty, then it should create starting population
 */
fun LanderController.rollingHorizonSolver(evolve: PopulationEvolver, fitness: FitnessFunction) {
    var horizon = 0
    var ranking: Ranking = emptyList()
    var rollingChromosome: Chromosome = emptyList() // resulting chromosome aka series of taken actions
    var populationNr = 0 // ordinal of current popsulation

    val evolverRounds = -1
    val visualizationInterval = 50
//    var previousBest: Rank = Rank(emptyList(), LanderParams(), Double.POSITIVE_INFINITY)

    while (true) {
        val solverStart = System.currentTimeMillis() // when solver started
        var evolves = 0 // if evolverRounds is > 0 then at most that many evolves would roll
        // evolve population as much as possible in given timeframe
        while (evolves < evolverRounds || evolverRounds < 0 && System.currentTimeMillis() - solverStart < 995) { // FIXME: fit as much based on extrapolated time
            ++evolves

            // evolve population, rank create ranking using
            ranking = evolve(ranking).map {
                val params = simulate(it)
                val fit = fitness(params)
                Rank(it, params, fit)
            }.sortedBy { it.fitness }
            // visualize every nth population
            if (populationNr % visualizationInterval == 0) {
                io.visualization.bestTrajectory = ranking[0].params.path
                io.visualization.trajectories = ranking.subList(1, ranking.size).map { it.params.path }
                io.visualization.populationNumber = populationNr
                io.visualization.repaint()
            }

            ++populationNr
        }

//        ranking.subList(1, ranking.size)
//            .forEach { io.drawPath(it.second.path, Color.GREEN)/*image.addPath(it.second.path, "1", "lime")*/ }
//        io.drawPath(ranking[0].second.path, Color.RED)        //image.addPath(ranking[0].second.path, "1", "red")
//        image.renderPicture(populationNr)
//        newImage()

        val best = ranking[0]
        io.error("best: ${best.fitness}\n${best.params.pretty()}")

//        if(best.fitness > previousBest.fitness){
//            io.println(fitness(landerParams.deepCopy().simulateUntilCollision(previousBest.chromosome.subList(1, previousBest.chromosome.size))))
//        } else {
//            previousBest = Rank(best.chromosome.copy(), best.params.deepCopy(), best.fitness)
//        }

        // io.error("BEST: ${ranking[0].third}\n${ranking[0].second}")

        ++horizon

        io.println("${best.chromosome[0]}") // output best action from chromosome
        rollingChromosome = rollingChromosome + best.chromosome[0] // append new gene to rolling chromosome

        // landerParams = io.nextParams()
        if (landerParams.stepAndCheckCollision(best.chromosome[0])) {
            io.error("OK? ${landerParams.landingSucceeded()}\n${landerParams.pretty()}")
            return
        }

        ranking = ranking.map {
            Rank(
                it.chromosome.subList(1, it.chromosome.size),
                it.params,
                it.fitness
            )
        } // remove first genes
    }
}

// mu+lambda evolution - no crossover, only mutations
// mu: how many best chromosomes to mutate
// lambda: how many worst to replace with mu
// here I don't use gaussian mutation but uniform mutation
// this is not a proper mu, lambda - it just mutates worst lambda solutions keeping the best
fun muLambdaEvolver(mu: Int, lambda: Int): PopulationEvolver = { ranking ->
    if (ranking.isEmpty()) {
        Random.nextMarkovPopulation(mu + lambda, 200).map { it.smoothen() }
    } else {
        // mutate leftover genes
        val p = ranking.population
        val original = p.subList(0, mu)
        val mutated = p.subList(0, lambda).map { it.uniformMutate(1.0) }
        original + mutated
    }
}

fun rouletteEvolver(populationSize: Int, eliteSize: Int = 6): PopulationEvolver = { _ranking ->
    if (_ranking.isEmpty()) {
        Random.nextMarkovPopulation(populationSize, 200).map { it.smoothen() }
    } else {
        val ranking = if (_ranking.size > populationSize) _ranking.subList(0, populationSize) else _ranking

        // elitism
        val children: Population = ranking.subList(0, eliteSize).population.toMutableList()
        val sum = ranking.fold(0.0, { s, rank -> s + rank.fitness }) // normalization
        val cumulative = ranking.runningFold()
        while(children.size < populationSize) {
            val p1 = ranking[ranking.]
            val p2 = ranking[]
        }


        children
    }
}

/** for debugging - should return constant population so the path should be the same as simulation*/
fun nonEvolver(): PopulationEvolver = { ranking ->
    if (ranking.isEmpty())
        Random.nextMarkovPopulation(100, 200)
    else
        ranking.population
}