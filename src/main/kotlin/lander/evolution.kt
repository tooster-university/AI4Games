package lander

import java.util.*
import kotlin.math.*
import kotlin.random.Random

typealias Gene = Action
typealias Chromosome = List<Gene>
typealias Population = List<Chromosome>



// mutates gene completely and randomly
fun Gene.randomMutation() = Random.nextGene()

fun Random.nextGene() = Action(Random.nextInt(-90, 91), Random.nextInt(0, 5))
fun Random.nextChromosome(length: Int = 300) = List(length) { Random.nextGene() }
fun Random.nextPopulation(size: Int = 100) = List(size) { Random.nextChromosome() }

// randomly mutate single gene
fun Gene.mutate(probability: Double = 0.05) = if (Random.nextDouble() <= probability) Random.nextGene() else this

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

// evaluates a chromosome with simulation. All landing parameters are stored in engine until reset
// offset is used to allow evolution in the middle of simulation
fun Chromosome.simulate(geneOffset: Int) {
    engine.restart()
    // offset chromosome
    val geneIterator = this.subList(geneOffset, size).iterator()
    var lastAction = Action(0, 0)

    while (engine.landing) {
        if (geneIterator.hasNext())
            lastAction = geneIterator.next()
        engine.moveAndCheckCollided(lastAction)
    }
}

fun Population.rank(fitness: (chromosome: Chromosome) -> Double): Population =
    this.sortedBy { chromosome: Chromosome ->
        chromosome.simulate(stepsPassed)
        fitness(chromosome)
    }

// how many steps till start passed
var stepsPassed = 0

// fitness method 1: polynomial from distance to landing site, fuel left and landing velocity
fun polyFitness(chromosome: Chromosome): Double =
    0 +
            1.0 * abs(engine.crashMilestone - engine.flatMilestone).pow(3) -
            1.0 * engine.fuel.toDouble().pow(2) + // make fuel more
            1.0 * engine.velocity.norm() +
            1.0 * abs(engine.yaw)

// less is better
fun linFit(chromosome: Chromosome): Double =
    0 +
            1000 * abs(engine.crashMilestone - engine.flatMilestone) - // closer = better
            100 * engine.fuel.toDouble() + // more fuel = better
            10 * engine.velocity.norm() + // smaller relative velocity = better
            1 * abs(engine.yaw) // smaller angle = better

fun crashFit(chromosome: Chromosome): Double = abs(engine.crashMilestone - engine.flatMilestone)

// mu+lambda evolution - no crossover, only mutations
// mu: how many best chromosomes to mutate
// lambda: how many worst to replace with mu
// here I don't use gaussian mutation but uniform mutation
fun muLambdaSolver(fitFunction: (Chromosome) -> Double) {
    val (mu, lambda) = 20 to 5

    // rolling horizon population adjustment

    var population = Random.nextPopulation(mu + lambda)
    var populationNumber = 0
    var bestSoFar = population[0]

    while (true) {

        val solverStart = System.currentTimeMillis()
        // 95ms to evolve as much populations as possible
        while (System.currentTimeMillis() - solverStart < 995) { // 995 miliseconds to log populations
            ++populationNumber
            val ranking = population.rank(fitFunction)
            // update visualization
            ranking.visualize(populationNumber)

            population = ranking.subList(0, mu) + ranking.subList(mu, mu + lambda).map {
                it.subList(0, stepsPassed) + it.subList(stepsPassed, it.size).uniformMutate(0.1)  // mutate leftover genes
            }
        }

        // print next move of the best chromosome
        val best = population.first()
        println(best[min(best.size - 1, stepsPassed)]) // print best move
        ++stepsPassed
    }
}

val surface = mutableListOf<Vector2>()


fun main(args: Array<String>) {
    val input = Scanner(System.`in`)
    val N = input.nextInt() // the number of points used to draw the surface of Mars.

    // initialize the engine
    for (i in 0 until N) {
        val landX = input.nextInt() // X coordinate of a surface point. (0 to 6999)
        val landY =
            input.nextInt() // Y coordinate of a surface point. By linking all the points together in a sequential fashion, you form the surface of Mars.
        surface.add(Vector2(landX.toDouble(), landY.toDouble()))
    }


    engine = Engine(
        surface.toTypedArray(), // map
        Vector2(input.nextInt(), input.nextInt()), // position
        Vector2(input.nextInt(), input.nextInt()), // velocity
        input.nextInt(), // fuel
        input.nextInt(), // rotation
        input.nextInt()  // power
    )

    saveVisualizationToFile(0)


    System.err.printf(
        "X=%dm, Y=%dm, HSpeed=%dm/s, VSpeed=%dm/s\nfuel=%d, yaw=%d°, power=%d\n",
        engine.position.x.toInt(),
        engine.position.y.toInt(),
        engine.velocity.x.toInt(),
        engine.velocity.y.toInt(),
        engine.fuel,
        engine.yaw,
        engine.power
    )

    muLambdaSolver(::crashFit)
}

// 6   0 1500 1000 2000 2000 500 3500 500 5000 1500 6999 1000
// 5000 2500 -50 0 1000 90 0

// 7 0 100 1000 500 1500 1500 3000 1000 4000 150 5500 150 6999 800
// 2500 2700 0 0 550 0 0