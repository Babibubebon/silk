/* 
 * Copyright 2011 Freie Universität Berlin, MediaEvent Services GmbH & Co. KG
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

package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import xml.NodeSeq
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import de.fuberlin.wiwiss.silk.workbench.Constants
import de.fuberlin.wiwiss.silk.evaluation.ReferenceLinks
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.SHtml
import net.liftweb.util.Helpers._
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.linking.{Cache, LinkingTask}
import net.liftweb.http.js.JsCmds.OnLoad
import net.liftweb.common.Empty
import de.fuberlin.wiwiss.silk.entity.SparqlRestriction
import de.fuberlin.wiwiss.silk.config.{LinkFilter, DatasetSpecification, LinkSpecification}
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule

/**
 * A dialog to create new linking tasks.
 */
class CreateLinkingTaskDialog {

  def render(xhtml: NodeSeq): NodeSeq = {
    var name = ""
    var sourceId = ""
    var targetId = ""
    var sourceRestriction = ""
    var targetRestriction = ""
    var linkType = "http://www.w3.org/2002/07/owl#sameAs"

    def submit() = {
      try {
        implicit val prefixes = User().project.config.prefixes

        val linkSpec =
          LinkSpecification(
            id = name,
            linkType = linkType,
            datasets = DPair(DatasetSpecification(sourceId, Constants.SourceVariable, SparqlRestriction.fromSparql(sourceRestriction)),
                             DatasetSpecification(targetId, Constants.TargetVariable, SparqlRestriction.fromSparql(targetRestriction))),
            rule = LinkageRule(None),
            filter = LinkFilter(),
            outputs = Nil
          )

        val linkingTask = LinkingTask(User().project, linkSpec, ReferenceLinks())

        User().project.linkingModule.update(linkingTask)

        CreateLinkingTaskDialog.closeCmd & Workspace.updateCmd
      } catch {
        case ex: Exception => Workspace.hideLoadingDialogCmd & JsRaw("alert('" + ex.getMessage.encJs + "');").cmd
      }
    }

    SHtml.ajaxForm(
      bind("entry", xhtml,
        "name" -> SHtml.text(name, name = _, "id" -> "linkName", "size" -> "60", "title" -> "Linking task name"),
        "sourceId" -> SHtml.untrustedSelect(Nil, Empty, sourceId = _, "id" -> "selectSourceId", "title" -> "Source dataset"),
        "sourceRestriction" -> SHtml.text(sourceRestriction, sourceRestriction = _, "id" -> "sourceRes", "size" -> "60", "title" -> "Restrict source dataset using SPARQL clauses"),
        "targetId" -> SHtml.untrustedSelect(Nil, Empty, targetId = _, "id" -> "selectTargetId", "title" -> "Target dataset"),
        "targetRestriction" -> SHtml.text(targetRestriction, targetRestriction = _, "id" -> "targetRes", "size" -> "60", "title" -> "Restrict target dataset using SPARQL clauses"),
        "linkType" -> SHtml.text(linkType, linkType = _, "id" -> "linkType", "size" -> "60", "title" -> "Type of the generated link"),
        "submit" -> SHtml.ajaxSubmit("Create", submit)))
  }
}

object CreateLinkingTaskDialog {
  def initCmd = OnLoad(JsRaw("$('#createLinkingTaskDialog').dialog({ autoOpen: false, width: 700, modal: true })").cmd)

  def openCmd = {
    val sourceOptions = for(task <- User().project.sourceModule.tasks) yield <option value={task.name}>{task.name}</option>

    //Clear name
    JsRaw("$('#linkName').val('');").cmd &
    //Update source options
    JsRaw("$('#selectSourceId').children().remove();").cmd &
    JsRaw("$('#selectSourceId').append('" + sourceOptions.mkString + "');").cmd &
    JsRaw("$('#selectTargetId').children().remove();").cmd &
    JsRaw("$('#selectTargetId').append('" + sourceOptions.mkString + "');").cmd &
    //Clear restrictions
    JsRaw("$('#sourceRes').val('?a rdf:type myprefix:myclass');").cmd &
    JsRaw("$('#targetRes').val('?b rdf:type myprefix:myclass');").cmd &
    //Reset link type
    JsRaw("$('#linkType').val('http://www.w3.org/2002/07/owl#sameAs');").cmd &
    //Open dialog
    JsRaw("$('#createLinkingTaskDialog').dialog('open');").cmd
  }

  def closeCmd = JsRaw("$('#createLinkingTaskDialog').dialog('close');").cmd
}