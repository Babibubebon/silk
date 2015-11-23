package de.fuberlin.wiwiss.silk.workspace.modules.workflow

import java.util.logging.Logger

import de.fuberlin.wiwiss.silk.dataset.Dataset
import de.fuberlin.wiwiss.silk.runtime.activity.{Activity, ActivityContext}
import de.fuberlin.wiwiss.silk.workspace.Project
import de.fuberlin.wiwiss.silk.workspace.modules.Task
import de.fuberlin.wiwiss.silk.workspace.modules.workflow.Workflow.WorkflowOperator

class WorkflowExecutor(task: Task[Workflow]) extends Activity[Unit] {

  val log = Logger.getLogger(getClass.getName)

  override def run(context: ActivityContext[Unit]) = {
    val operators = task.data.operators

    // Preliminary: Just execute the operators from left to right
    for((op, index) <- operators.sortBy(_.position.x).zipWithIndex) {
      context.status.update(s"${op.task} (${index + 1} / ${operators.size})", index.toDouble / operators.size)
      executeOperator(op, context)
    }
  }

  def executeOperator(operator: WorkflowOperator, context: ActivityContext[Unit]) = {
    val project = task.project
    val inputs = operator.inputs.map(id => project.task[Dataset](id).data.source)
    val outputs = operator.outputs.map(id => project.task[Dataset](id).data.sink)
    val taskData = project.anyTask(operator.task).data

    val taskExecutor = project.getExecutor(taskData)
      .getOrElse(throw new Exception("Cannot execute task " + operator.task))

    val activity = taskExecutor(inputs, taskData, outputs)
    //TODO job.statusLogLevel = Level.FINE
    //TODO job.progressLogLevel = Level.FINE
    context.executeBlocking(activity, 0.0)

    log.info("Finished execution of " + operator.task)
  }
}