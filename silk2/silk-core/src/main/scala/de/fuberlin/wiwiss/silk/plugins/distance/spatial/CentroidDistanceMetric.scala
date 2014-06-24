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

package de.fuberlin.wiwiss.silk.plugins.distance.spatial

import de.fuberlin.wiwiss.silk.entity.Index
import de.fuberlin.wiwiss.silk.linkagerule.similarity.SimpleDistanceMeasure
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin
import de.fuberlin.wiwiss.silk.util.spatial.SpatialExtensionsUtils.{ evaluateDistance, indexGeometriesByCentre }
import de.fuberlin.wiwiss.silk.util.spatial.Constants._

/**
 * Computes the distance between the centroids of two geometries in meters (It assumes that geometries are expressed in WKT and WGS 84 (latitude-longitude)).
 * @author Panayiotis Smeros (Department of Informatics & Telecommunications, National & Kapodistrian University of Athens)
 */
@Plugin(
  id = "CentroidDistanceMetric",
  categories = Array("Spatial"),
  label = "Centroid distance",
  description = "Computes the distance between the centroids of two geometries in meters. Author: Panayiotis Smeros (Department of Informatics & Telecommunications, National & Kapodistrian University of Athens)")
case class CentroidDistanceMetric() extends SimpleDistanceMeasure {

  override def evaluate(str1: String, str2: String, limit: Double): Double = {
    evaluateDistance(str1, str2, limit, CENTROID_DISTANCE)
  }

  override def indexValue(str: String, distance: Double): Index = {
    indexGeometriesByCentre(str, distance)
  }
}