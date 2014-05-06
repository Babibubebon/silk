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

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.plugins.Plugins
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

/**
 * Tests the Intersects Metric.
 * @author Panayiotis Smeros (Department of Informatics & Telecommunications, National & Kapodistrian University of Athens)
 */

@RunWith(classOf[JUnitRunner])
class IntersectsMetricTest extends FlatSpec with ShouldMatchers {
  Plugins.register()

  val metric = new IntersectsMetric()

  //Intersects evaluation.
  "IntersectsMetric test 1" should "return '0.0'" in {
    metric.evaluate("POLYGON ((0 0, 0 3, 3 3, 3 0, 0 0))", "POLYGON ((1 1, 1 2, 2 2, 2 1, 1 1))", 0.0) should equal(0.0)
  }

}
