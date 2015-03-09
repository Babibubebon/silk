package models

import java.util.logging.Level
import de.fuberlin.wiwiss.silk.runtime.oldtask._
import de.fuberlin.wiwiss.silk.runtime.activity.{ValueHolder, ActivityControl, Status}

abstract class TaskStatusListener(task: HasStatus) extends Listener[TaskStatus] {
  task.onUpdate(Listener)

  private object Listener extends (TaskStatus => Unit) {
    def apply(status: TaskStatus) {
      update(status)
    }
  }
}

abstract class TaskDataListener[T](userData: TaskData[T]) extends Listener[T] {
  userData.onUpdate(Listener)

  private object Listener extends (T => Unit) {
    def apply(value: T) {
      update(value)
    }
  }
}

/**
 * Listens to the current status of the current users task.
 */
abstract class CurrentStatusListener(taskHolder: TaskData[ActivityControl[_]]) extends Listener[Status] {

  //Deactivate logging
  //TODO statusLogLevel = Level.FINEST
  // progressLogLevel = Level.FINEST

  val statusHolder = new ValueHolder[Status](Status.Idle)

  //Listen to changes of the current task
  taskHolder.onUpdate(Listener)

  //Set current task
  @volatile protected var task = taskHolder()

  //Listen to changes of the status of the current task.
  task.status.onUpdate(StatusListener)
  statusHolder.update(task.status())

  private object Listener extends (ActivityControl[_] => Unit) {
    def apply(newActivity: ActivityControl[_]) {
      task = newActivity
      task.status.onUpdate(StatusListener)
    }
  }

  private object StatusListener extends (Status => Unit) {
    def apply(status: Status) {
      update(status)
      statusHolder.update(status)
    }
  }
}

/**
 * Listens to the current status of the current users task.
 */
abstract class CurrentTaskStatusListener[TaskType <: HasStatus](taskHolder: TaskData[TaskType]) extends Listener[TaskStatus] with HasStatus {

  //Deactivate logging
  statusLogLevel = Level.FINEST
  progressLogLevel = Level.FINEST

  //Listen to changes of the current task
  taskHolder.onUpdate(Listener)

  //Set current task
  @volatile protected var task = taskHolder()

  //Listen to changes of the status of the current task.
  task.onUpdate(StatusListener)
  updateStatus(task.status)

  private object Listener extends (TaskType => Unit) {
    def apply(newTask: TaskType) {
      task = newTask
      task.onUpdate(StatusListener)
    }
  }

  private object StatusListener extends (TaskStatus => Unit) {
    def apply(status: TaskStatus) {
      update(status)
      updateStatus(status)
    }
  }
}