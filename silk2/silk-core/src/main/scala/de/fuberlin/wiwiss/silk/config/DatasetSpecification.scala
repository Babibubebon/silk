/* 
 * Copyright 2009-2011 Freie Universität Berlin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.config

import xml.Node
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.entity.SparqlRestriction

/**
 * Defines a dataset.
 *
 * @param sourceId The id of the source
 * @param variable Each data item will be bound to this variable.
 * @param restriction Restricts this dataset to specific resources.
 */
case class DatasetSpecification(sourceId: Identifier, variable: String, restriction: SparqlRestriction) {
  require(!variable.isEmpty, "Variable must be non-empty")

  /**
   * Serializes this Dataset Specification as XML.
   *
   * @param If true, this dataset will be serialized as a source dataset. If false it will be serialize as target dataset.
   */
  def toXML(asSource: Boolean) = {
    if (asSource) {
      <SourceDataset dataSource={sourceId} var={variable}>
        <RestrictTo>
          {restriction}
        </RestrictTo>
      </SourceDataset>
    }
    else {
      <TargetDataset dataSource={sourceId} var={variable}>
        <RestrictTo>
          {restriction}
        </RestrictTo>
      </TargetDataset>
    }
  }
}

object DatasetSpecification {
  /**
   * Creates a DatasetSpecification from XML.
   */
  def fromXML(node: Node)(implicit prefixes: Prefixes): DatasetSpecification = {
    new DatasetSpecification(
      node \ "@dataSource" text,
      node \ "@var" text,
      SparqlRestriction.fromSparql((node \ "RestrictTo").text.trim)
    )
  }

  def empty = DatasetSpecification(Identifier.random, "x", SparqlRestriction.empty)
}
