package de.fuberlin.wiwiss.silk.learning.generation

import util.Random
import de.fuberlin.wiwiss.silk.learning.individual.{LinkConditionNode, AggregationNode}
import de.fuberlin.wiwiss.silk.evaluation.ReferenceInstances
import de.fuberlin.wiwiss.silk.learning.LearningConfiguration.Components

class LinkConditionGenerator(comparisonGenerators: IndexedSeq[ComparisonGenerator], components: Components) {

  private val aggregations = "max" :: "min" :: "average" :: Nil

  private val minOperatorCount = 1

  private val maxOperatorCount = 2

  def apply() = {
    if(components.aggregations) {
      LinkConditionNode(Some(generateAggregation()))
    } else {
      LinkConditionNode(Some(generateComparison()))
    }
  }

  /**
   * Generates a random aggregation node.
   */
  private def generateAggregation(): AggregationNode = {
    //Choose a random aggregation
    val aggregation = aggregations(Random.nextInt(aggregations.size))

    //Choose a random operator count
    val operatorCount = minOperatorCount + Random.nextInt(maxOperatorCount - minOperatorCount + 1)

    //Generate operators
    val operators =
      for (i <- List.range(1, operatorCount + 1)) yield {
        generateComparison()
      }

    //Build aggregation
    new AggregationNode(aggregation, operators)
  }

  private def generateComparison() = {
    comparisonGenerators(Random.nextInt(comparisonGenerators.size))()
  }
}

object LinkConditionGenerator {
  def apply(instances: ReferenceInstances, components: Components) = {
    new LinkConditionGenerator((new PathPairGenerator(components).apply(instances) ++ new PatternGenerator(components).apply(instances)).toIndexedSeq, components)
  }
}