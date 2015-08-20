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

package de.fuberlin.wiwiss.silk.linkagerule.input

import de.fuberlin.wiwiss.silk.entity.Entity
import de.fuberlin.wiwiss.silk.runtime.serialization.{Serialization, XmlFormat}
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.config.Prefixes
import scala.xml.Node
import de.fuberlin.wiwiss.silk.linkagerule.Operator
import de.fuberlin.wiwiss.silk.runtime.resource.ResourceLoader

/**
 * An input that retrieves a set of values.
 */
trait Input extends Operator {
  /**
   * Retrieves the values of this input for a given entity.
   *
   * @param entities The pair of entities.
   * @return The values.
   */
  def apply(entities: DPair[Entity]): Set[String]
}

object Input {

  /**
   * XML serialization format.
   */
  implicit object InputFormat extends XmlFormat[Input] {

    import Serialization._

    def read(node: Node)(implicit prefixes: Prefixes, resourceLoader: ResourceLoader): Input = {
      node match {
        case node @ <Input/> => fromXml[PathInput](node)
        case node @ <TransformInput>{_*}</TransformInput> => fromXml[TransformInput](node)
      }
    }

    def write(value: Input)(implicit prefixes: Prefixes): Node = {
      value match {
        case path: PathInput => toXml(path)
        case transform: TransformInput => toXml(transform)
      }
    }
  }
}