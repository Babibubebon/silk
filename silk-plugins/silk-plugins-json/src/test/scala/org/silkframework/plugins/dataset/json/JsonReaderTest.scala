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

package org.silkframework.plugins.dataset.json

import org.silkframework.entity.Path
import org.silkframework.runtime.resource.ClasspathResourceLoader


import org.scalatest.{FlatSpec, Matchers}


class JsonReaderTest extends FlatSpec with Matchers {

  private val json = {
    val resources = new ClasspathResourceLoader("org/silkframework/plugins/dataset/json")
    JsonParser.load(resources.get("example.json"))
  }

  private val persons = JsonParser.select(json, "persons" :: Nil)

  "On example.json, JsonReader" should "return 2 persons" in {
    persons.size should equal (2)
  }

  it should "return both person names" in {
    evaluate("name") should equal (Seq("John", "Max"))
  }

  it should "return all three numbers" in {
    evaluate("phoneNumbers/number") should equal (Seq("123", "456", "789"))
  }

  it should "return both home numbers" in {
    evaluate("""phoneNumbers[type = "home"]/number""") should equal (Seq("123", "789"))
  }

  private def evaluate(path: String): Seq[String] = {
    persons.flatMap(person => JsonParser.evaluate(person, Path.parse(path)))
  }
}