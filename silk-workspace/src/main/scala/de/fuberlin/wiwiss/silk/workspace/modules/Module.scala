package de.fuberlin.wiwiss.silk.workspace.modules

import java.util.logging.Logger

import de.fuberlin.wiwiss.silk.runtime.resource.ResourceManager
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.workspace.Project

import scala.collection.immutable.TreeMap
import scala.reflect.ClassTag

class Module[TaskData: ClassTag](plugin: ModulePlugin[TaskData], resourceMgr: ResourceManager, project: Project) {

  private val logger = Logger.getLogger(classOf[Module[_]].getName)

  /**
   * Caches all tasks of this module in memory.
   */
  @volatile
  private var cachedTasks: TreeMap[Identifier, Task[TaskData]] = null

  def hasTaskType[T : ClassTag]: Boolean = {
    implicitly[ClassTag[T]].runtimeClass == implicitly[ClassTag[TaskData]].runtimeClass
  }

  def taskType: String = {
    implicitly[ClassTag[TaskData]].runtimeClass.getName
  }

  /**
   * Retrieves all tasks in this module.
   */
  def tasks: Seq[Task[TaskData]] = {
    load()
    cachedTasks.values.toSeq
  }

  /**
   * Retrieves a task by name.
   *
   * @throws java.util.NoSuchElementException If no task with the given name has been found
   */
  def task(name: Identifier): Task[TaskData] = {
    load()
    cachedTasks.getOrElse(name, throw new NoSuchElementException(s"Task '$name' not found in ${project.name}"))
  }

  def taskOption(name: Identifier): Option[Task[TaskData]] = {
    load()
    cachedTasks.get(name)
  }

  def add(name: Identifier, taskData: TaskData) = {
    val task = new Task(name, taskData, plugin, project)
    plugin.writeTask(taskData, resourceMgr)
    cachedTasks += ((name, task))
  }

  /**
   * Removes a task from this module.
   */
  def remove(taskId: Identifier) {
    plugin.removeTask(taskId, resourceMgr)
    cachedTasks -= taskId
    logger.info(s"Removed task '$taskId' from project ${project.name}")
  }

  private def load(): Unit = synchronized {
    if(cachedTasks == null) {
      val loadedTasks = plugin.loadTasks(resourceMgr, project.resources)
      cachedTasks = TreeMap()(TaskOrdering) ++ { for((name, data) <- loadedTasks) yield (name, new Task(name, data, plugin, project)) }
    }
  }

  /**
   * Defines how tasks are sorted based on their identifier.
   */
  private object TaskOrdering extends Ordering[Identifier] {
    def compare(a:Identifier, b:Identifier) = a.toString.compareTo(b.toString)
  }
}