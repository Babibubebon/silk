package controllers.workspace

import java.util.logging.LogRecord

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.dataset.{Dataset, DatasetTask}
import org.silkframework.rule.{LinkSpec, TransformSpec}
import org.silkframework.runtime.activity.Status
import org.silkframework.runtime.plugin.PluginDescription
import org.silkframework.runtime.resource.Resource
import org.silkframework.workspace.activity.workflow.Workflow
import org.silkframework.workspace.activity.{ProjectActivity, TaskActivity, WorkspaceActivity}
import org.silkframework.workspace.{Project, ProjectMarshallingTrait, ProjectTask, User}
import play.api.libs.json._

import scala.reflect.ClassTag

/**
  * Generates a JSON describing the current workspace
  */
object JsonSerializer {

  def projectsJson = {
    JsArray(
      for (project <- User().workspace.projects) yield {
        projectJson(project)
      }
    )
  }

  def projectJson(project: Project) = {
    Json.obj(
      "name" -> JsString(project.name),
      "tasks" -> Json.obj(
      "dataset" -> tasksJson[Dataset](project),
      "transform" -> tasksJson[TransformSpec](project),
      "linking" -> tasksJson[LinkSpec](project),
      "workflow" -> tasksJson[Workflow](project)
      )
    )
  }

  def tasksJson[T <: TaskSpec : ClassTag](project: Project) = JsArray(
    for (task <- project.tasks[T]) yield {
      JsString(task.id)
    }
  )

  def projectResources(project: Project) = {
    JsArray(
      project.resources.list.map(r => resourceProperties(project.resources.get(r)))
    )
  }

  def resourceProperties(resource: Resource) = {
    val sizeValue = resource.size match {
      case Some(size) => JsNumber(BigDecimal.decimal(size))
      case None => JsNull
    }

    Json.obj(
      "relativePath" -> resource.name,
      "absolutePath" -> resource.path,
      "size" -> sizeValue
    )
  }

  def projectActivities(project: Project) = JsArray(
    for (activity <- project.activities) yield {
      JsString(activity.name)
    }
  )

  def taskActivities(task: ProjectTask[_ <: TaskSpec]) = JsArray(
    for (activity <- task.activities) yield {
      JsString(activity.name)
    }
  )

  def activityConfig(config: Map[String, String]) = JsArray(
    for ((name, value) <- config.toSeq) yield
      Json.obj("name" -> name, "value" -> value)
  )

  def readActivityConfig(json: JsValue): Map[String, String] = {
    for (value <- json.as[JsArray].value) yield
      ((value \ "name").toString(), (value \ "value").toString)
  }.toMap


  def activityStatus(activity: WorkspaceActivity): JsValue = {
    activityStatus(activity.project.name, activity.taskOption.map(_.id.toString).getOrElse(""), activity.name, activity.status)
  }

  def activityStatus(project: String, task: String, activity: String, status: Status): JsValue = {
    JsObject(
      ("project" -> JsString(project)) ::
      ("task" -> JsString(task)) ::
      ("activity" -> JsString(activity)) ::
      ("statusName" -> JsString(status.name)) ::
      ("isRunning" -> JsBoolean(status.isRunning)) ::
      ("progress" -> JsNumber(status.progress * 100.0)) ::
      ("message" -> JsString(status.toString)) ::
      ("failed" -> JsBoolean(status.failed)) ::
      ("timestamp" -> JsNumber(status.timestamp)) :: Nil
    )
  }

  def taskMetadata(task: Task[TaskSpec]) = {
    JsObject(
      ("id" -> JsString(task.id)) ::
      ("referencedTasks" -> JsArray(task.referencedTasks.toSeq.map(JsString(_)))) :: Nil
    )
  }

  def logRecords(records: Seq[LogRecord]) = {
    JsArray(records.map(logRecord))
  }

  def logRecord(record: LogRecord) = {
    JsObject(
      ("activity" -> JsString(record.getLoggerName.substring(record.getLoggerName.lastIndexOf('.') + 1))) ::
      ("level" -> JsString(record.getLevel.getName)) ::
      ("message" -> JsString(record.getMessage)) ::
      ("timestamp" -> JsNumber(record.getMillis)) :: Nil
    )
  }

  def pluginConfig(pluginConfig: PluginDescription[_]) = {
    JsObject(
      ("id" -> JsString(pluginConfig.id)) ::
      ("label" -> JsString(pluginConfig.label)) ::
      ("description" -> JsString(pluginConfig.description)) :: Nil
    )
  }

  def marshaller(marshaller: ProjectMarshallingTrait) = {
    JsObject(
      ("id" -> JsString(marshaller.id)) ::
      ("label" -> JsString(marshaller.name)) :: Nil
    )
  }
}
