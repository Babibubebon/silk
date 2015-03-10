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

package de.fuberlin.wiwiss.silk.workspace.modules.dataset

import java.util.logging.{Logger, Level}
import de.fuberlin.wiwiss.silk.config.LinkSpecification
import de.fuberlin.wiwiss.silk.dataset.Dataset
import de.fuberlin.wiwiss.silk.runtime.resource.{ResourceLoader, ResourceManager}
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.workspace.Project
import de.fuberlin.wiwiss.silk.workspace.modules.workflow.Workflow
import de.fuberlin.wiwiss.silk.workspace.modules.{Task, ModulePlugin}
import de.fuberlin.wiwiss.silk.workspace.modules.linking.LinkingCaches
import de.fuberlin.wiwiss.silk.util.XMLUtils._
import scala.xml.XML

/**
 * The source module which encapsulates all data sources.
 */
class DatasetModulePlugin extends ModulePlugin[Dataset] {

  private val logger = Logger.getLogger(classOf[DatasetModulePlugin].getName)

  override def prefix = "dataset"

  def createTask(name: Identifier, taskData: Dataset, project: Project): Task[Dataset] = {
    new Task(name, taskData, Seq(new TypesCache()), this, project)
  }

  /**
   * Loads all tasks of this module.
   */
  override def loadTasks(resources: ResourceLoader, project: Project): Seq[Task[Dataset]] = {
    // Read dataset tasks
    val names = resources.list.filter(_.endsWith(".xml")).filter(!_.contains("cache"))
    val tasks = for (name <- names) yield {
      loadTask(name, resources, project)
    }

    if (tasks.isEmpty) {
      // Also read dataset tasks from the old source folder
      val oldResources = resources.parent.get.child("source")
      val oldNames = oldResources.list.filter(_.endsWith(".xml")).filter(!_.contains("cache"))
      for (name <- oldNames) yield {
        loadTask(name, oldResources, project)
      }
    } else {
      tasks
    }
  }

  private def loadTask(name: String, resources: ResourceLoader, project: Project) = {
    // Load the data set
    val dataset = Dataset.load(project.resources)(resources.get(name).load)

    // Load the cache
    val cache = new TypesCache()
    try {
      cache.loadFromXML(XML.load(resources.get(dataset.id + "_cache.xml").load))
    } catch {
      case ex : Exception =>
        logger.log(Level.WARNING, "Cache corrupted. Rebuilding Cache.", ex)
        new LinkingCaches()
    }

    new Task(dataset.id, dataset, Seq(cache), this, project)
  }

  /**
   * Writes an updated task.
   */
  override def writeTask(task: Task[Dataset], resources: ResourceManager): Unit = {
    resources.put(task.name + ".xml"){ os => task.data.toXML.write(os) }
    resources.put(task.name + "_cache.xml") { os => task.caches.head.toXML.write(os) }
  }

  /**
   * Removes a specific task.
   */
  override def removeTask(taskId: Identifier, resources: ResourceManager): Unit = {
    resources.delete(taskId + ".xml")
    resources.delete(taskId + "_cache.xml")
  }
}