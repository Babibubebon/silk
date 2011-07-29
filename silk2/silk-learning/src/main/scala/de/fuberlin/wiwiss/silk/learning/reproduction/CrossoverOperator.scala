package de.fuberlin.wiwiss.silk.learning.reproduction

import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.util.strategy.{Strategy, Factory}
import de.fuberlin.wiwiss.silk.learning.individual.LinkConditionNode

//TODO implement operators: toggle required, change strategy

/**
 * A crossover operator takes a pair of nodes and combines them into a new node.
 */
trait CrossoverOperator extends (SourceTargetPair[LinkConditionNode] => Option[LinkConditionNode]) with Strategy {
  /**
   * Applies this crossover operator to a specific pair of nodes.
   */
  def apply(nodePair: SourceTargetPair[LinkConditionNode]): Option[LinkConditionNode]

  override def toString = getClass.getSimpleName
}

object CrossoverOperator extends Factory[CrossoverOperator] {
  register(classOf[LimitCrossover])
  register(classOf[AggregationOperatorsCrossover])
  register(classOf[AggregationStrategyCrossover])
  register(classOf[OperatorCrossover])
  register(classOf[TransformationCrossover])
}
