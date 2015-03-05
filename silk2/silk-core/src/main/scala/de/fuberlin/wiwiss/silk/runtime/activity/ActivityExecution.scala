package de.fuberlin.wiwiss.silk.runtime.activity

import java.util.logging.{Logger, Level}

import de.fuberlin.wiwiss.silk.runtime.oldtask.{TaskFinished, TaskStarted, TaskCanceling}

import scala.concurrent.ExecutionContext

private class ActivityExecution[T](activity: Activity[T], parent: Option[ActivityContext[_]] = None, progressContribution: Double = 0.0) extends Runnable with ActivityControl[T] with ActivityContext[T] {

  /**
   * The logger used to log status changes.
   */
  private val logger = Logger.getLogger(getClass.getName)

  /**
   * Holds the current value.
   */
  override val value = new ValueHolder[T]()

  /**
   * Retrieves the logger to be used by the activity.
   */
  override val log = Logger.getLogger(getClass.getName)

  /**
   * Holds the current status.
   */
  override val status = new StatusHolder(log, parent.map(_.status), progressContribution)

  // TODO synchronize
  private var childControls: Seq[ActivityControl[_]] = Seq.empty

  override def run(): Unit = synchronized {
    val startTime = System.currentTimeMillis
    status.update(Status.Started(activity.taskName))

    try {
      activity.run(this)
      status.update(Status.Finished(activity.taskName, success = true, System.currentTimeMillis - startTime))
    } catch {
      case ex: Throwable =>
        logger.log(Level.WARNING, activity.taskName + " failed", ex)
        status.update(Status.Finished(activity.taskName, success = false, System.currentTimeMillis - startTime, Some(ex)))
        throw ex
    }
  }
  
  override def children(): Seq[ActivityControl[_]] = {
    removeDoneChildren()
    childControls
  }

  override def cancel() = {
    if(status().isRunning && !status().isInstanceOf[TaskCanceling]) {
      status.update(Status.Canceling(activity.taskName, status().progress))
      childControls.foreach(_.cancel())
      activity.cancelExecution()
    }
  }
  
  override def executeBlocking[R](activity: Activity[R], progressContribution: Double = 0.0, onUpdate: R => Unit): R = {
    val execution = new ActivityExecution(activity, Some(this), progressContribution)
    execution.value.onUpdate(onUpdate)
    execution.run()
    execution.value()
  }

  override def executeBackground[R](activity: Activity[R], progressContribution: Double = 0.0): ActivityControl[R] = {
    val execution = new ActivityExecution(activity, Some(this), progressContribution)
    addChild(execution)
    ExecutionContext.global.execute(execution)
    execution
  }

  private def addChild(control: ActivityControl[_]): Unit = {
    childControls = childControls :+ control
  }

  private def removeDoneChildren(): Unit = {
    childControls = childControls.filter(_.status().isRunning)
  }
}