package de.fuberlin.wiwiss.silk.util.task

import collection.mutable.Publisher
import java.util.logging.{Logger, Level}

class HasStatus extends Publisher[Status] {
  /**
   * The level at which status changes should be logged.
   */
  var logLevel = Level.INFO

  /**
   * The logger used to log status changes.
   */
  private val logger = Logger.getLogger(getClass.getName)

  /**
   * Holds the current status.
   */
  private var currentStatus: Status = Idle()

  /**
   * The current status of this task.
   */
  def status = currentStatus

  /**
   * True, if the task is running at the moment; False, otherwise.
   */
  def isRunning = currentStatus match {
    case _: Idle => false
    case _: Started => true
    case _: Running => true
    case _: Finished => false
  }

  /**
   * Updates the status.
   *
   * @param status The new status
   */
  protected def updateStatus(status: Status) {
    if (logger.isLoggable(logLevel)) {
      logger.log(logLevel, status.toString)
    }
    currentStatus = status
    publish(status)
  }

  /**
   * Updates the status message.
   *
   * @param status The new status message
   */
  protected def updateStatus(message: String) {
    updateStatus(Running(message, currentStatus.progress))
  }

  /**
   * Updates the progress.
   *
   * @param progress The progress of the computation (A value between 0.0 and 1.0 inclusive).
   */
  protected def updateStatus(progress: Double) {
    updateStatus(Running(currentStatus.message, progress))
  }

  /**
   * Updates the status.
   *
   * @param message The new status message
   * @param progress The progress of the computation (A value between 0.0 and 1.0 inclusive).
   */
  protected def updateStatus(message: String, progress: Double) {
    updateStatus(Running(message, progress))
  }
}