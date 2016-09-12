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

package org.silkframework.runtime.plugin

import scala.language.existentials

case class Parameter(name: String,
                     dataType: ParameterType[_],
                     description: String = "No description",
                     defaultValue: Option[AnyRef] = None,
                     exampleValue: Option[AnyRef] = None) {

  /**
   * Retrieves the current value of this parameter.
   */
  def apply(obj: AnyRef): AnyRef = {
    obj.getClass.getMethod(name).invoke(obj)
  }

  /**
    * Retrieves the current value of this parameter as string.
    */
  def stringValue(obj: AnyRef): String = {
    dataType.asInstanceOf[ParameterType[AnyRef]].toString(apply(obj))
  }
}
