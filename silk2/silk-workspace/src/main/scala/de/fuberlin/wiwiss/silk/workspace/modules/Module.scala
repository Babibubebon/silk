package de.fuberlin.wiwiss.silk.workspace.modules

import java.util.logging.{Level, Logger}

import de.fuberlin.wiwiss.silk.runtime.resource.ResourceManager
import de.fuberlin.wiwiss.silk.util.{Identifier, Timer}
import de.fuberlin.wiwiss.silk.workspace.Project

import scala.reflect.ClassTag

class Module[TaskData: ClassTag](provider: ModulePlugin[TaskData], resourceMgr: ResourceManager, project: Project) {

  private val logger = Logger.getLogger(classOf[Module[_]].getName)

  /**
   * Caches all tasks of this module in memory.
   */
  @volatile
  private var cachedTasks : Map[Identifier, Task[TaskData]] = null

  // Start a background writing thread
  //WriteThread.start()

//  def loadTasks() = {
//    if(cachedTasks == null) {
//      cachedTasks = {
//        val tasks = provider.loadTasks(resourceMgr, project)
//        tasks.map(task => (task.name, task)).toMap
//      }
//    }
//  }

  def hasTaskType[T : ClassTag]: Boolean = {
    implicitly[ClassTag[T]].runtimeClass == implicitly[ClassTag[TaskData]].runtimeClass
  }

  def taskType: String = {
    implicitly[ClassTag[TaskData]].runtimeClass.getName
  }

  /**
   * Retrieves all tasks in this module.
   */
  def tasks: Seq[Task[TaskData]] = synchronized {
    if(cachedTasks == null) {
      val loadedTasks = provider.loadTasks(resourceMgr, project)
      cachedTasks = loadedTasks.map(task => (task.name, task)).toMap
    }
    cachedTasks.values.toSeq
  }

  /**
   * Retrieves a task by name.
   *
   * @throws java.util.NoSuchElementException If no task with the given name has been found
   */
  def task(name: Identifier): Task[TaskData] = {
    cachedTasks.getOrElse(name, throw new NoSuchElementException(s"Task '$name' not found in ${project.name}"))
  }

  def taskOption(name: Identifier): Option[Task[TaskData]] = {
    cachedTasks.get(name)
  }

  def add(name: Identifier, taskData: TaskData) = {
    provider.createTask(name, taskData, project)
  }

  /**
   * Removes a task from this module.
   */
  def remove(taskId: Identifier) {
    provider.removeTask(taskId, resourceMgr)
    cachedTasks -= taskId
    logger.info("Removed task '" + taskId + "'")
  }

  /**
   * Persists a task.
   */
//  private def write() {
//    val tasksToWrite = updatedTasks.values.toList
//    updatedTasks --= tasksToWrite.map(_.name)
//
//    for(task <- tasksToWrite) Timer("Writing task " + task.name + " to disk") {
//      provider.writeTask(task, resourceMgr)
//    }
//  }

//  private object WriteThread extends Thread {
//    override def run() {
//      while(true) {
//        val time = System.currentTimeMillis - lastUpdateTime
//
//        if(updatedTasks.isEmpty) {
//          Thread.sleep(writeInterval)
//        }
//        else if(time >= writeInterval) {
//          try {
//            Module.this.write()
//          }
//          catch {
//            case ex : Exception => logger.log(Level.WARNING, "Error writing tasks", ex)
//          }
//        }
//        else {
//          Thread.sleep(writeInterval - time)
//        }
//      }
//    }
//  }
}