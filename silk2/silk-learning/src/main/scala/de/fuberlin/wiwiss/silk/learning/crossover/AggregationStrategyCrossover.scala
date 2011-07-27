package de.fuberlin.wiwiss.silk.learning.crossover

import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.learning.individual.AggregationNode

/**
 * A crossover operator which interchanges the aggregation strategies.
 */
case class AggregationStrategyCrossover() extends NodePairCrossoverOperator[AggregationNode] {
  override protected def crossover(nodes: SourceTargetPair[AggregationNode]) = {
    nodes.source.copy(aggregation = nodes.target.aggregation)
  }
}
