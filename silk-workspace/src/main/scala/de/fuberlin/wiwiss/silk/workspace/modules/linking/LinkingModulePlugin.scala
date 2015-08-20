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

package de.fuberlin.wiwiss.silk.workspace.modules.linking

import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.config.{LinkSpecification, Prefixes, RuntimeConfig}
import de.fuberlin.wiwiss.silk.dataset.Dataset
import de.fuberlin.wiwiss.silk.entity.{EntityDescription, Link}
import de.fuberlin.wiwiss.silk.evaluation.{ReferenceEntities, ReferenceLinksReader}
import de.fuberlin.wiwiss.silk.execution.GenerateLinks
import de.fuberlin.wiwiss.silk.learning.active.{ActiveLearning, ActiveLearningState}
import de.fuberlin.wiwiss.silk.learning.{LearningActivity, LearningConfiguration, LearningInput, LearningResult}
import de.fuberlin.wiwiss.silk.runtime.resource.{ResourceLoader, ResourceManager}
import de.fuberlin.wiwiss.silk.util.XMLUtils._
import de.fuberlin.wiwiss.silk.util.{DPair, Identifier}
import de.fuberlin.wiwiss.silk.workspace.Project
import de.fuberlin.wiwiss.silk.workspace.modules.{ModulePlugin, Task, TaskActivity}
import de.fuberlin.wiwiss.silk.runtime.serialization.Serialization._

import scala.xml.XML

/**
 * The linking module which encapsulates all linking tasks.
 */
class LinkingModulePlugin extends ModulePlugin[LinkSpecification] {

  private val logger = Logger.getLogger(classOf[LinkingModulePlugin].getName)

  override def prefix = "linking"

  /**
   * Loads all tasks of this module.
   */
  def loadTasks(resources: ResourceLoader, project: Project): Map[Identifier, LinkSpecification] = {
    val tasks =
      for(name <- resources.listChildren) yield
        loadTask(resources.child(name), project)
    tasks.toMap
  }

  /**
   * Loads a specific task in this module.
   */
  private def loadTask(taskResources: ResourceLoader, project: Project) = {
    implicit val prefixes = project.config.prefixes
    implicit val resources = project.resources
    val linkSpec = fromXml[LinkSpecification](XML.load(taskResources.get("linkSpec.xml").load))
    val referenceLinks = ReferenceLinksReader.readReferenceLinks(taskResources.get("alignment.xml").load)
    (linkSpec.id, linkSpec.copy(referenceLinks = referenceLinks))
  }

  /**
   * Removes a specific task.
   */
  def removeTask(name: Identifier, resources: ResourceManager) = {
    resources.delete(name)
  }

  /**
   * Writes an updated task.
   */
  def writeTask(name: Identifier, data: LinkSpecification, resources: ResourceManager) = {
    //Don't use any prefixes
    implicit val prefixes = Prefixes.empty

    // Write resources
    val taskResources = resources.child(name)
    taskResources.put("linkSpec.xml", toXml(data).toString())
    taskResources.put("alignment.xml") { os => data.referenceLinks.toXML.write(os) }
  }

  override def activities(task: Task[LinkSpecification], project: Project): Seq[TaskActivity[_,_]] = {
    // Generate links
    def generateLinks(links: Seq[Link]) =
      GenerateLinks.fromSources(
        inputs = project.tasks[Dataset].map(_.data),
        linkSpec = task.data,
        runtimeConfig = RuntimeConfig(useFileCache = false, partitionSize = 300, generateLinksWithEntities = true)
      )
    // Supervised learning
    def learning(population: LearningResult) = {
      val input =
        LearningInput(
          trainingEntities = task.activity[ReferenceEntitiesCache].value(),
          seedLinkageRules = task.data.rule :: Nil
        )
      new LearningActivity(input, LearningConfiguration.default)
    }
    // Active learning
    def activeLearning(state: ActiveLearningState) =
      new ActiveLearning(
        config = LearningConfiguration.default,
        datasets = DPair.fromSeq(task.data.datasets.map(ds => project.tasks[Dataset].map(_.data).find(_.id == ds.datasetId).getOrElse(Dataset.empty).source)),
        linkSpec = task.data,
        paths = task.activity[PathsCache].value().map(_.paths),
        referenceEntities = task.activity[ReferenceEntitiesCache].value(),
        state = state
      )
    // Paths Cache
    def pathsCache() =
      new PathsCache(
        datasets = task.data.datasets.map(ds => project.task[Dataset](ds.datasetId).data),
        linkSpec = task.data
      )
    // ReferenceEntities Cache
    def referenceEntitiesCache() = new ReferenceEntitiesCache(task, project)

    // Create task activities
    val taskResources = project.resourceManager.child(prefix).child(task.name)
    TaskActivity(Seq[Link](), generateLinks) ::
    TaskActivity("pathsCache.xml", null: DPair[EntityDescription], pathsCache, taskResources) ::
    TaskActivity("referenceEntitiesCache.xml", ReferenceEntities.empty, referenceEntitiesCache, taskResources) ::
    TaskActivity(LearningResult(), learning) ::
    TaskActivity(ActiveLearningState.initial, activeLearning) :: Nil
  }

}