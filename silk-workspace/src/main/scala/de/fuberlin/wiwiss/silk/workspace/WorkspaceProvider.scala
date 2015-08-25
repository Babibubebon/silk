package de.fuberlin.wiwiss.silk.workspace

import de.fuberlin.wiwiss.silk.util.Identifier
import scala.reflect.ClassTag

trait WorkspaceProvider {

  def readProjects(): Seq[ProjectConfig]

  def putProject(project: ProjectConfig): Unit

  def deleteProject(name: Identifier): Unit

  def readTasks[T: ClassTag](project: Identifier): Seq[(Identifier, T)]

  def putTask[T: ClassTag](project: Identifier, data: T): Unit

  def deleteTask[T: ClassTag](project: Identifier, task: Identifier): Unit
}