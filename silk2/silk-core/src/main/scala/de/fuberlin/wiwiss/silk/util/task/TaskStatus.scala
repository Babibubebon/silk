package de.fuberlin.wiwiss.silk.util.task

/**
 * A status message
 */
sealed trait TaskStatus {
  /**
   * The current status message.
   */
  def message: String

  /**
   * The progress of the computation.
   * Will be 0.0 when the task has been started and 1.0 when it has finished execution.
   */
  def progress: Double = 0.0

  /**
   * True, if the task is running at the moment; False, otherwise.
   */
  def isRunning: Boolean = false

  /**
   * True, if the task has failed.
   */
  def failed: Boolean = false

  /**
   * The complete status message including the progress.
   */
  override def toString = message + " (" + "%3.1f".format(progress * 100.0) + "%)"
}

/**
 * Status which indicates that the task has not been started yet.
 */
case class TaskIdle() extends TaskStatus {
  def message = "Idle"
}

/**
 * Status which indicates that the task has been started.
 */
case class TaskStarted(name: String) extends TaskStatus {
  override def message = name + " started"
  override def isRunning = true
}

/**
 * Running status
 *
 * @param message The status message
 * @param progress The progress of the computation (A value between 0.0 and 1.0 inclusive).
 */
case class TaskRunning(message: String, override val progress: Double) extends TaskStatus {
  override def isRunning = true
}

case class TaskCanceling(name: String, override val progress: Double) extends TaskStatus {
  override def message = "Stopping " + name
  override def isRunning = true
}

/**
 * Status which indicates that the task has finished execution.
 *
 * @param name The name of the task.
 * @param success True, if the computation finished successfully. False, otherwise.
 * @param time The time in milliseconds needed to execute the task.
 * @param exception The exception, if the task failed.
 */
case class TaskFinished(name: String, success: Boolean, time: Long, exception: Option[Exception] = None) extends TaskStatus {
  override def message = exception match {
    case None => name + " finished in " + formattedTime
    case Some(ex) => name + " failed after " + formattedTime + ": " + ex.getMessage
  }

  private def formattedTime = {
    if (time < 1000)
      time + "ms"
    else
      (time / 1000) + "s"
  }

  override def progress = 1.0

  override def failed = !success
}