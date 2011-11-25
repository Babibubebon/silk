/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.learning.active

import de.fuberlin.wiwiss.silk.util.task.ValueTask
import de.fuberlin.wiwiss.silk.learning.individual.Population
import linkselector.{WeightedLinkageRule, EntropySelector}
import de.fuberlin.wiwiss.silk.entity.Link
import de.fuberlin.wiwiss.silk.evaluation.ReferenceEntities

private class SampleFromPopulationTask(population: Population, unlabeledLinks: Seq[Link], referenceEntities: ReferenceEntities) extends ValueTask[Seq[Link]](Seq.empty) {

  /**
   * Weight the linkage rules.
   * Better linkage rules will have a bigger weight in the information gain computation.
   */
  private val weightedRules = {
    val bestFitness = population.individuals.map(_.fitness).max
    val topIndividuals = population.individuals.toSeq.filter(_.fitness >= bestFitness * 0.5)
    for(individual <- topIndividuals) yield {
      new WeightedLinkageRule(individual)
    }
  }

  override protected def execute(): Seq[Link] = {
    val selector = new EntropySelector(weightedRules, unlabeledLinks)
    //val selector = new UncertaintySelector(weightedRules, unlabeledLinks, referenceEntities)
    //val selector = new KullbackLeiblerDivergenceSelector(weightedRules, unlabeledLinks, referenceEntities)
    selector()
  }

}
