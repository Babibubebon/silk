package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.workbench.lift.util.ProgressWidget
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.linking.LinkingTask
import de.fuberlin.wiwiss.silk.util.Observable
import de.fuberlin.wiwiss.silk.util.task.{Task, TaskStatus}
import de.fuberlin.wiwiss.silk.workbench.workspace.User.{Message, CurrentTaskChanged}

/**
 * Shows the progress of the cache loader task.
 */
class CacheLoadingProgress() extends ProgressWidget(new CacheStatus, hide = true)

/**
 * Listens to cache status changes of the current linking task.
 */
private class CacheStatus extends Observable[TaskStatus] {
  /** Listen to the cache status of the current task. */
  if(User().linkingTaskOpen) User().linkingTask.cache.onUpdate(StatusListener)

  /** Listen to changes of the current task. */
  User().onUpdate(CurrentTaskListener)

  override def onUpdate[U](f: TaskStatus => U) = {
    if(User().linkingTaskOpen) f(User().linkingTask.cache.status)
    super.onUpdate(f)
  }

  private object CurrentTaskListener extends (Message => Unit) {
    def apply(event: Message) {
      event match {
        case CurrentTaskChanged(_, _, task: LinkingTask) => task.cache.onUpdate(StatusListener)
        case _ =>
      }
    }
  }

  private object StatusListener extends (TaskStatus => Unit) {
    def apply(status: TaskStatus) {
      publish(status)
    }
  }
}