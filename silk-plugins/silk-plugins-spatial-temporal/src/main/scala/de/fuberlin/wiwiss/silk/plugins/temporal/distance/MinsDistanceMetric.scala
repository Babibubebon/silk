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

package de.fuberlin.wiwiss.silk.plugins.temporal.distance

import de.fuberlin.wiwiss.silk.entity.Index
import de.fuberlin.wiwiss.silk.rule.similarity.SimpleDistanceMeasure
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin
import de.fuberlin.wiwiss.silk.plugins.temporal.utils._



/**
 * Computes the distance in minutes between two time periods or instants (It assumes that the times are expressed in the "yyyy-MM-DD'T'hh:mm:ss" format).
 * @author Panayiotis Smeros <psmeros@di.uoa.gr> (National and Kapodistrian University of Athens)
 */
@Plugin(
  id = "MinsDistanceMetric",
  categories = Array("Temporal"),
  label = "Mins distance",
  description = "Computes the distance in minutes between two time periods or instants.")
case class MinsDistanceMetric() extends SimpleDistanceMeasure {

  override def evaluate(str1: String, str2: String, limit: Double): Double = {
    Utils.evaluateDistance(str1, str2, limit, Constants.MINS_DISTANCE)
  }

  override def indexValue(str: String, distance: Double): Index = {
    Utils.indexTimes(str, distance*Constants.MILLISECS_PER_MIN)
  }
}