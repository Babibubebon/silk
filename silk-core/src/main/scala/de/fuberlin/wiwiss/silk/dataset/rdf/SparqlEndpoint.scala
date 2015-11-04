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

package de.fuberlin.wiwiss.silk.dataset.rdf

/**
 * Represents a SPARQL endpoint and provides an interface to execute queries on it.
 */
trait SparqlEndpoint {

  /**
    * Executes a select query.
    * If the query does not contain a offset or limit, automatic paging is done by issuing multiple queries with a sliding offset.
    *
    */
  def select(query: String, limit: Int = Integer.MAX_VALUE): SparqlResults

  /**
    * Executes an update query.
    */
  def update(query: String): Unit = {
    throw new UnsupportedOperationException(s"Endpoint type $getClass does not support issuing SPARQL/Update queries")
  }
}
