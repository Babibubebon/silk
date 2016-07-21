package org.silkframework.workspace

import java.util.logging.{Level, Logger}

import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Identifier

import scala.collection.immutable.TreeMap
import scala.reflect.ClassTag
import scala.util.control.NonFatal

/**
  * A module holds all tasks of a specific type.
  *
  * @param provider The workspace provider
  * @param project The project this module belongs to
  * @tparam TaskData The task type held by this module
  */
class Module[TaskData: ClassTag](private[workspace] val provider: WorkspaceProvider,
                                 private[workspace] val project: Project) {

  private val logger = Logger.getLogger(classOf[Module[_]].getName)

  /**
   * Caches all tasks of this module in memory.
   */
  @volatile
  private var cachedTasks: TreeMap[Identifier, Task[TaskData]] = null

  /**
    * Holds all issues that occured during loading.
    */
  @volatile
  private var error: Option[ValidationException] = None

  /**
    * Returns a validation exception if an error occured during task loading.
    */
  def loadingError: Option[ValidationException] = error

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
    cachedTasks.getOrElse(name, throw new TaskNotFoundException(project.name, name, taskType))
  }

  def taskOption(name: Identifier): Option[Task[TaskData]] = {
    load()
    cachedTasks.get(name)
  }

  def add(name: Identifier, taskData: TaskData) = {
    val task = new Task(name, taskData, this)
    provider.putTask(project.name, name, taskData)
    task.init()
    cachedTasks += ((name, task))
  }

  /**
   * Removes a task from this module.
   */
  def remove(taskId: Identifier) {
    provider.deleteTask(project.name, taskId)
    cachedTasks -= taskId
    logger.info(s"Removed task '$taskId' from project ${project.name}")
  }

  private def load(): Unit = synchronized {
    if(cachedTasks == null) {
      try {
        val tasks = provider.readTasks(project.name)
        cachedTasks = TreeMap()(TaskOrdering) ++ {
          for ((name, data) <- tasks) yield (name, new Task(name, data, this))
        }
      } catch {
        case NonFatal(ex) =>
          cachedTasks = TreeMap()(TaskOrdering)
          error = Some(new ValidationException(s"Error loading tasks of type $taskType", ex))
          logger.log(Level.WARNING, s"Error loading tasks of type $taskType", ex)
      }
    }
  }

  /**
   * Defines how tasks are sorted based on their identifier.
   */
  private object TaskOrdering extends Ordering[Identifier] {
    def compare(a:Identifier, b:Identifier) = a.toString.compareTo(b.toString)
  }
}
