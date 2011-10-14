package de.fuberlin.wiwiss.silk.learning.generation

import de.fuberlin.wiwiss.silk.util.DPair
import util.Random
import de.fuberlin.wiwiss.silk.linkagerule.similarity.DistanceMeasure
import de.fuberlin.wiwiss.silk.learning.individual._

class ComparisonGenerator(inputGenerators: DPair[InputGenerator], measure: FunctionNode[DistanceMeasure], maxThreshold: Double) {

  def apply() = {
    ComparisonNode(
      inputs = inputGenerators.map(_.apply()),
      threshold =  Random.nextDouble() * maxThreshold,
      weight = 1 + Random.nextInt(20),
      metric = measure
    )
  }
}