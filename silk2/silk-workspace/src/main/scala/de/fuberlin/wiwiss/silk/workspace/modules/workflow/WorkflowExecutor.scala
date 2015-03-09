package de.fuberlin.wiwiss.silk.workspace.modules.workflow

import java.util.logging.{Level, Logger}

import de.fuberlin.wiwiss.silk.dataset.Dataset
import de.fuberlin.wiwiss.silk.runtime.activity.{ActivityContext, Activity}
import de.fuberlin.wiwiss.silk.workspace.Project
import de.fuberlin.wiwiss.silk.workspace.modules.workflow.Workflow.WorkflowOperator

class WorkflowExecutor(operators: Seq[WorkflowOperator], project: Project) extends Activity[Unit] {

  val log = Logger.getLogger(getClass.getName)

  override def run(context: ActivityContext[Unit]) = {
    val inputNames = operators.flatMap(_.inputs).toSet
    val outputNames = operators.flatMap(_.outputs).toSet

    // Determine all datasets that are filled by an operator
    var emptyDatasets = outputNames
    var pendingOperators = operators.toSet

    while (pendingOperators.nonEmpty) {
      // Execute next operator
      pendingOperators.find(!_.inputs.exists(emptyDatasets.contains)) match {
        case Some(op) =>
          executeOperator(op, context)
          emptyDatasets --= op.outputs
          pendingOperators -= op
        case None =>
          throw new RuntimeException("Deadlock in workflow execution")
      }
      // Update status
      val completedTasks = operators.size - pendingOperators.size
      context.status.update(s"$completedTasks / ${operators.size}", completedTasks.toDouble / operators.size)
    }
  }

  def executeOperator(operator: WorkflowOperator, context: ActivityContext[Unit]) = {
    log.info("Executing " + operator.task)

    val inputs = operator.inputs.map(id => project.task[Dataset](id).data.source)
    val outputs = operator.outputs.map(id => project.task[Dataset](id).data.sink)
    val taskData = project.anyTask(operator.task).data

    val taskExecutor = project.getExecutor(taskData)
      .getOrElse(throw new Exception("Cannot execute task " + operator.task))

    val activity = taskExecutor(inputs, taskData, outputs)
    //TODO job.statusLogLevel = Level.FINE
    //TODO job.progressLogLevel = Level.FINE
    context.executeBackground(activity, 0.0)

    log.info("Finished execution of " + operator.task)
  }
}