package de.fuberlin.wiwiss.silk.workspace.modules.workflow

import java.util.logging.{Level, Logger}

import de.fuberlin.wiwiss.silk.runtime.task.Task
import de.fuberlin.wiwiss.silk.workspace.Project
import de.fuberlin.wiwiss.silk.workspace.modules.dataset.DatasetTask
import de.fuberlin.wiwiss.silk.workspace.modules.workflow.WorkflowTask.WorkflowOperator

class WorkflowExecutor(operators: Seq[WorkflowOperator], project: Project) {

  val log = Logger.getLogger(getClass.getName)

  def apply(): Task[Unit] = {
    new ExecutorTask
  }

  class ExecutorTask extends Task[Unit] {

    override protected def execute() = {
      val inputNames = operators.flatMap(_.inputs).toSet
      val outputNames = operators.flatMap(_.outputs).toSet

      // Determine all datasets that are filled by an operator
      var emptyDatasets = outputNames
      var pendingOperators = operators.toSet

      while (pendingOperators.nonEmpty) {
        // Execute next operator
        pendingOperators.find(!_.inputs.exists(emptyDatasets.contains)) match {
          case Some(op) =>
            executeOperator(op)
            emptyDatasets --= op.outputs
            pendingOperators -= op
          case None =>
            throw new RuntimeException("Deadlock in workflow execution")
        }
        // Update status
        val completedTasks = operators.size - pendingOperators.size
        updateStatus(s"$completedTasks / ${operators.size}", completedTasks.toDouble / operators.size)
      }
    }

    def executeOperator(operator: WorkflowOperator) = {
      log.info("Executing " + operator.task.name)

      val inputs = operator.inputs.map(id => project.task[DatasetTask](id).dataset)
      val outputs = operator.outputs.map(id => project.task[DatasetTask](id).dataset)

      val taskExecutor = project.getExecutor(operator.task)
          .getOrElse(throw new Exception("Cannot execute task " + operator.task.name))

      val job = taskExecutor(inputs, operator.task, outputs)
      job.statusLogLevel = Level.FINE
      job.progressLogLevel = Level.FINE
      job()

      log.info("Finished execution of " + operator.task.name)
    }
  }

}