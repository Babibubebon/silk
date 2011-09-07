package de.fuberlin.wiwiss.silk.learning

import de.fuberlin.wiwiss.silk.evaluation.ReferenceInstances
import de.fuberlin.wiwiss.silk.linkspec.LinkageRule

/**
 * The input of the learning algorithm.
 *
 * @param trainingInstances Reference instances used for training
 * @param validationInstances Reference instances used for validation
 * @param seedLinkageRules Existing linkage rules which are used to seed the population.
 */
case class LearningInput(trainingInstances: ReferenceInstances = ReferenceInstances.empty,
                         validationInstances: ReferenceInstances = ReferenceInstances.empty,
                         seedLinkageRules: Traversable[LinkageRule] = Traversable.empty)

object LearningInput {
  def empty = LearningInput()
}