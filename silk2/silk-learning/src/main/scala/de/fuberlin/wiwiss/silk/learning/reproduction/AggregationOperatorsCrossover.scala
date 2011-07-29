package de.fuberlin.wiwiss.silk.learning.reproduction

import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.learning.individual.AggregationNode
import de.fuberlin.wiwiss.silk.learning.reproduction.Utils.crossoverNodes

/**
 * A crossover operator which combines the operators of two aggregations.
 */
case class AggregationOperatorsCrossover() extends NodePairCrossoverOperator[AggregationNode] {
  override protected def crossover(nodes: SourceTargetPair[AggregationNode]) = {
    nodes.source.copy(operators = crossoverNodes(nodes.source.operators, nodes.target.operators))
  }
}