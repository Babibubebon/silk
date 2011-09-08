package de.fuberlin.wiwiss.silk.util.task

import de.fuberlin.wiwiss.silk.util.task.Task._
import java.util.logging.Level
import java.util.concurrent.{TimeUnit, ThreadPoolExecutor, Callable, Executors}
import de.fuberlin.wiwiss.silk.util.StringUtils._

/**
 * A task which computes a result.
 * While executing the status of the execution can be queried.
 */
trait Task[+T] extends HasStatus with (() => T) {

  var taskName = getClass.getSimpleName.undoCamelCase

  /**
   * Executes this task and returns the result.
   */
  override final def apply(): T = synchronized {
    val startTime = System.currentTimeMillis
    updateStatus(TaskStarted(taskName))

    try {
      val result = execute()
      updateStatus(TaskFinished(taskName, true, System.currentTimeMillis - startTime))
      result
    } catch {
      case ex: Exception => {
        logger.log(Level.WARNING, taskName + "failed", ex)
        updateStatus(TaskFinished(taskName, false, System.currentTimeMillis - startTime, Some(ex)))
        throw ex
      }
    }
  }

  /**
   * Executes this task in a background thread
   */
  def runInBackground(): Future[T] = {
    Task.backgroundExecutor.submit(toCallable(this))
  }

  /**
   * Requests to stop the execution of this task.
   * There is no guarantee that the task will stop immediately.
   * Subclasses need to override stopExecution() to allow cancellation.
   */
  def cancel() {
    if(status.isRunning) {
      updateStatus(TaskCanceling(taskName, status.progress))
      stopExecution()
    }
  }

  /**
   *  Must be overridden in subclasses to do the actual computation.
   */
  protected def execute(): T

  /**
   *  Can be overridden in subclasses to allow cancellation of the task.
   */
  protected def stopExecution() { }

  protected def executeSubTask[R](subTask: Task[R], finalProgress: Double = 1.0): R = {
    require(finalProgress >= status.progress, "finalProgress >= progress")

    //Disable logging of the subtask as this task will do the logging
    val subTaskLogLevel = subTask.statusLogLevel
    subTask.statusLogLevel = Level.FINEST
    subTask.progressLogLevel = Level.FINEST

    //Subscribe to status changes of the sub task
    val listener = new (TaskStatus => Unit) {
      val initialProgress = status.progress

      def apply(status: TaskStatus) {
        status match {
          case TaskRunning(msg, taskProgress) => {
            updateStatus(msg, initialProgress + taskProgress * (finalProgress - initialProgress))
          }
          case TaskFinished(_, success, _, _) if success == true => {
            updateStatus(finalProgress)
          }
          case _ =>
        }
      }
    }

    //Start sub task
    try {
      subTask.onUpdate(listener)
      subTask()
    } finally {
      subTask.statusLogLevel = subTaskLogLevel
    }
  }
}

object Task {
  /**
   * Converts a task to a Java Runnable
   */
  implicit def toRunnable[T](task: Task[T]) = new Runnable {
    override def run() = task.apply()
  }

  /**
   * Converts a task to a Java Callable
   */
  implicit def toCallable[T](task: Task[T]) = new Callable[T] {
    override def call() = task.apply()
  }

  /**
   * The executor service used to execute link specs in the background.
   */
  private val backgroundExecutor = {
    val executor = Executors.newCachedThreadPool()

    //Reducing the keep-alive time of the executor, so it won't prevent the JVM from shutting down to long
    executor.asInstanceOf[ThreadPoolExecutor].setKeepAliveTime(2, TimeUnit.SECONDS)

    executor
  }
}
