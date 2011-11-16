package de.fuberlin.wiwiss.silk.learning.active

import de.fuberlin.wiwiss.silk.datasource.Source
import de.fuberlin.wiwiss.silk.util.task.ValueTask
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.entity.{Link, Path}
import de.fuberlin.wiwiss.silk.config.LinkSpecification
import de.fuberlin.wiwiss.silk.learning.reproduction.ReproductionTask
import de.fuberlin.wiwiss.silk.learning.cleaning.CleanPopulationTask
import de.fuberlin.wiwiss.silk.linkagerule.{Operator, LinkageRule}
import de.fuberlin.wiwiss.silk.linkagerule.similarity.{Comparison, Aggregation}
import de.fuberlin.wiwiss.silk.linkagerule.input.{PathInput, TransformInput}
import de.fuberlin.wiwiss.silk.learning.{LearningInput, LearningTask, LearningConfiguration}
import util.control.Breaks._
import de.fuberlin.wiwiss.silk.evaluation.{LinkageRuleEvaluator, ReferenceEntities}
import de.fuberlin.wiwiss.silk.learning.individual.{FitnessFunction, Population}
import de.fuberlin.wiwiss.silk.learning.generation.{GeneratePopulationTask, LinkageRuleGenerator}

//TODO support canceling
class ActiveLearningTask(sources: Traversable[Source],
                         linkSpec: LinkSpecification,
                         paths: DPair[Seq[Path]],
                         referenceEntities: ReferenceEntities = ReferenceEntities.empty,
                         var pool: Traversable[Link] = Traversable.empty,
                         var population: Population = Population()) extends ValueTask[Seq[Link]](Seq.empty) {

  def links = value.get

  override protected def execute(): Seq[Link] = {
    //Build unlabeled pool
    if(pool.isEmpty) {
      updateStatus("Loading")
      pool = executeSubTask(new GeneratePoolTask(sources, linkSpec, paths), 0.5)
    }

    //Build population
    val config = LearningConfiguration.load()
    val generator = LinkageRuleGenerator(ReferenceEntities.fromEntities(pool.map(_.entities.get), Nil), config.components)

    if(population.isEmpty) {
      updateStatus("Generating population", 0.6)
      population = executeSubTask(new GeneratePopulationTask(Traversable(linkSpec.rule), generator, config), 0.7)
    }

    //Evolve population
    val completeEntities = CompleteReferenceLinks(referenceEntities, pool, population)
    val fitnessFunction = new FitnessFunction(completeEntities, pool)

    val prevFitness = population.bestIndividual.fitness
    for(i <- 1 to 20 if i == 1 || population.bestIndividual.fitness < prevFitness) {
      population = executeSubTask(new ReproductionTask(population, fitnessFunction, generator, config), 0.7 + i / 10.0)
    }

    //Sample links
    updateStatus("Sampling", 0.9)

    val valLinks = new SampleFromPopulationTask(population, pool.toSeq, completeEntities).apply()
    value.update(valLinks)

    //Clean population
    if(referenceEntities.isDefined)
      population = executeSubTask(new CleanPopulationTask(population, fitnessFunction, generator))

    valLinks
  }

  private def formatRule(rule: LinkageRule) = {
    def collectPaths(op: Operator): Seq[Path] = op match {
      case agg: Aggregation => agg.operators.flatMap(collectPaths)
      case cmp: Comparison => cmp.inputs.flatMap(collectPaths)
      case t: TransformInput => t.inputs.flatMap(collectPaths)
      case i: PathInput => Seq(i.path)
    }

    val paths = collectPaths(rule.operator.get)
    val shortPaths  = paths.map(_.serialize.split("[/#]").last.init)

    shortPaths.mkString(" ")
  }
}

object ActiveLearningTask {
  def empty = new ActiveLearningTask(Traversable.empty, LinkSpecification(), DPair.fill(Seq.empty), ReferenceEntities.empty)
}