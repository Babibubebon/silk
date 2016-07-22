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

package org.silkframework.workspace

import java.util.logging.{Level, Logger}

import org.silkframework.config._
import org.silkframework.dataset.{Dataset, DatasetPlugin}
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Identifier
import org.silkframework.workspace.activity.linking.LinkingTaskExecutor
import org.silkframework.workspace.activity.transform._
import org.silkframework.workspace.activity.workflow.Workflow
import org.silkframework.workspace.activity.{ProjectActivity, ProjectActivityFactory, TaskExecutor}

import scala.reflect.ClassTag
import scala.util.control.NonFatal

/**
 * A project.
 */
class Project(initialConfig: ProjectConfig = ProjectConfig(), provider: WorkspaceProvider) {

  private implicit val logger = Logger.getLogger(classOf[Project].getName)

  val resources: ResourceManager = provider.projectResources(initialConfig.id)

  val cacheResources: ResourceManager = provider.projectCache(initialConfig.id)

  @volatile
  private var cachedConfig: ProjectConfig = initialConfig

  @volatile
  private var modules = Seq[Module[_ <: TaskSpecification]]()

  @volatile
  private var executors = Map[String, TaskExecutor[_]]()

  /**
    * Holds all issues that occurred during loading project activities.
    */
  @volatile
  private var activityLoadingErrors: Seq[ValidationException] = Seq.empty

  // Register all default modules
  registerModule[DatasetPlugin]()
  registerModule[TransformSpecification]()
  registerModule[LinkSpecification]()
  registerModule[Workflow]()
  registerModule[CustomTaskPlugin]()

  registerExecutor(new LinkingTaskExecutor())
  registerExecutor(new TransformTaskExecutor())

  // Initialize Tasks
  allTasks.foreach(_.init())

  /**
    * The name of this project.
    */
  def name = cachedConfig.id

  /**
    * Retrieves all errors that occured during loading this project.
    */
  def loadingErrors: Seq[ValidationException] = modules.flatMap(_.loadingError) ++ activityLoadingErrors

  private val projectActivities = {
    val factories = PluginRegistry.availablePlugins[ProjectActivityFactory[_]].toList
    var activities = List[ProjectActivity]()
    for(factory <- factories) {
      try {
        activities ::= new ProjectActivity(this, factory()(config.prefixes, resources))
      } catch {
        case NonFatal(ex) =>
          val errorMsg = s"Could not load project activity '$factory' in project '${initialConfig.id}'."
          activityLoadingErrors :+= new ValidationException(errorMsg + "Details: " + ex.getMessage, ex)
          logger.log(Level.WARNING, errorMsg, ex)
      }
    }
    activities.reverse
  }

  /**
    * Available activities for this project.
    */
  def activities: Seq[ProjectActivity] = {
    projectActivities
  }

  /**
    * Retrieves an activity by name.
    *
    * @param activityName The name of the requested activity
    * @return The activity control for the requested activity
    */
  def activity(activityName: String) = {
    projectActivities.find(_.name == activityName)
      .getOrElse(throw new NoSuchElementException(s"Project '$name' does not contain an activity named '$activityName'. " +
        s"Available activities: ${activities.map(_.name).mkString(", ")}"))
  }

  /**
   * Reads the project configuration.
   */
  def config: ProjectConfig = cachedConfig

  /**
   * Writes the updated project configuration.
   */
  def config_=(project : ProjectConfig) {
    provider.putProject(project)
    cachedConfig = project
  }

  /**
   * Retrieves all tasks in this project.
   */
  def allTasks: Seq[ProjectTask[_ <: TaskSpecification]] = {
    for(module <- modules; task <- module.tasks) yield task.asInstanceOf[ProjectTask[_ <: TaskSpecification]]
  }

  /**
   * Retrieves all tasks of a specific type.
   */
  def tasks[T <: TaskSpecification : ClassTag]: Seq[ProjectTask[T]] = {
    module[T].tasks
  }

  /**
   * Retrieves a task of a specific type by name.
   *
   * @param taskName The name of the task
   * @tparam T The task type
   * @throws java.util.NoSuchElementException If no task with the given name has been found
   */
  def task[T <: TaskSpecification : ClassTag](taskName: Identifier): ProjectTask[T] = {
    module[T].task(taskName)
  }

  def taskOption[T <: TaskSpecification : ClassTag](taskName: Identifier): Option[ProjectTask[T]] = {
    module[T].taskOption(taskName)
  }

  /**
   * Retrieves a task of any type by name.
   *
   * @param taskName The name of the task
   * @throws java.util.NoSuchElementException If no task with the given name has been found
   */
  def anyTask(taskName: Identifier): ProjectTask[_ <: TaskSpecification] = {
    modules.flatMap(_.taskOption(taskName).asInstanceOf[Option[ProjectTask[_ <: TaskSpecification]]]).headOption
           .getOrElse(throw new NoSuchElementException(s"No task '$taskName' found in project '$name'"))
  }

  /**
    * Adds a new task to this project.
    *
    * @param name The name of the task. Must be unique for all tasks in this project.
    * @param taskData The task data.
    * @tparam T The task type.
    */
  def addTask[T <: TaskSpecification : ClassTag](name: Identifier, taskData: T) = {
    require(!allTasks.exists(_.id == name), s"Task name '$name' is not unique as there is already a task in project '${this.name}' with this name.")
    module[T].add(name, taskData)
  }

  /**
    * Updates a task.
    * If no task with the given name exists, a new task is created in the project.
    *
    * @param name The name of the task.
    * @param taskData The task data.
    * @tparam T The task type.
    */
  def updateTask[T <: TaskSpecification : ClassTag](name: Identifier, taskData: T) = {
    module[T].taskOption(name) match {
      case Some(task) => task.update(taskData)
      case None => addTask[T](name, taskData)
    }
  }

  /**
   * Removes a task.
   *
   * @param taskName The name of the task
   * @tparam T The task type
   */
  def removeTask[T <: TaskSpecification : ClassTag](taskName: Identifier): Unit = {
    module[T].remove(taskName)
  }

  /**
   * Retrieves an executor for a specific task.
   */
  def getExecutor[T](taskData: T): Option[TaskExecutor[T]] = {
    executors.get(taskData.getClass.getName).map(_.asInstanceOf[TaskExecutor[T]])
  }

  /**
   * Retrieves a module for a specific task type.
   *
   * @tparam T The task type
   * @throws java.util.NoSuchElementException If no module for the given task type has been registered
   */
  private def module[T <: TaskSpecification : ClassTag]: Module[T] = {
    modules.find(_.hasTaskType[T]) match {
      case Some(m) => m.asInstanceOf[Module[T]]
      case None =>
        val className = implicitly[ClassTag[T]].runtimeClass.getName
        throw new NoSuchElementException(s"No module for task type $className has been registered. ${modules.size} Registered task types: ${modules.map(_.taskType).mkString(";")}")
    }
  }

  /**
   * Registers a new module from a module provider.
   */
  def registerModule[T <: TaskSpecification : ClassTag]() = {
    modules = modules :+ new Module[T](provider, this)
  }

  /**
   * Registers a new executor for a specific task type.
   */
  def registerExecutor[T : ClassTag](executor: TaskExecutor[T]) = {
    val taskClassName = implicitly[ClassTag[T]].runtimeClass.getName
    executors = executors.updated(taskClassName, executor)
  }
}