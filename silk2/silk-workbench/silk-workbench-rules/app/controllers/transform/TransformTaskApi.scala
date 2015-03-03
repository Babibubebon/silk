package controllers.transform

import java.util.logging.{Level, Logger}
import de.fuberlin.wiwiss.silk.config.DatasetSelection
import de.fuberlin.wiwiss.silk.entity.{ForwardOperator, SparqlRestriction}
import de.fuberlin.wiwiss.silk.execution.{ExecuteTransform}
import de.fuberlin.wiwiss.silk.linkagerule.TransformRule
import de.fuberlin.wiwiss.silk.runtime.task.Executor
import de.fuberlin.wiwiss.silk.util.{CollectLogs, ValidationException}
import de.fuberlin.wiwiss.silk.workspace.modules.dataset.DatasetTask
import de.fuberlin.wiwiss.silk.workspace.modules.transform.TransformTask
import de.fuberlin.wiwiss.silk.workspace.{Constants, User}
import models.transform.CurrentExecuteTransformTask
import play.api.libs.json.{JsArray, JsObject, JsString}
import play.api.mvc.{Action, Controller}

object TransformTaskApi extends Controller {

  private val log = Logger.getLogger(getClass.getName)

  def putTransformTask(project: String, task: String) = Action { implicit request => {
    val values = request.body.asFormUrlEncoded.getOrElse(Map.empty).mapValues(_.mkString)

    val proj = User().workspace.project(project)
    implicit val prefixes = proj.config.prefixes

    val dataset = DatasetSelection(values("source"), Constants.SourceVariable, SparqlRestriction.fromSparql(Constants.SourceVariable, values("restriction")))

    proj.tasks[TransformTask].find(_.name == task) match {
      //Update existing task
      case Some(oldTask) => {
        val updatedTransformTask = oldTask.updateDataset(dataset, proj)
        proj.updateTask(updatedTransformTask)
      }
      //Create new task with a single rule
      case None => {
        val transformTask = TransformTask(proj, task, dataset, Seq.empty)
        proj.updateTask(transformTask)
      }
    }
    Ok
  }}

  def deleteTransformTask(project: String, task: String) = Action {
    User().workspace.project(project).removeTask[TransformTask](task)
    Ok
  }

  def getRules(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[TransformTask](taskName)
    implicit val prefixes = project.config.prefixes

    Ok(<TransformRules>{ task.rules.map(_.toXML) }</TransformRules>)
  }

  def putRules(projectName: String, taskName: String) = Action { request => {
    val project = User().workspace.project(projectName)
    val task = project.task[TransformTask](taskName)
    implicit val prefixes = project.config.prefixes

    request.body.asXml match {
      case Some(xml) =>
        try {
          //Parse transformation rules
          val updatedRules = (xml \ "TransformRule").map(TransformRule.load(project.resources)(prefixes))
          //Update transformation task
          val updatedTask = task.updateRules(updatedRules, project)
          project.updateTask(updatedTask)
          Ok
        } catch {
          case ex: ValidationException =>
            BadRequest(ex.toString)
          case ex: Exception =>
            InternalServerError("Error in back end: " + ex.getMessage)
        }
      case None =>
        BadRequest("Expecting text/xml request body")
    }
  }}

  def getRule(projectName: String, taskName: String, rule: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[TransformTask](taskName)
    implicit val prefixes = project.config.prefixes

    task.rules.find(_.name == rule) match {
      case Some(r) => Ok(r.toXML)
      case None => NotFound(s"No rule named '$rule' found!")
    }
  }

  def putRule(projectName: String, taskName: String, ruleIndex: Int) = Action { request => {
    val project = User().workspace.project(projectName)
    val task = project.task[TransformTask](taskName)
    implicit val prefixes = project.config.prefixes

    request.body.asXml match {
      case Some(xml) =>
        try {
          //Collect warnings while parsing transformation rule
          val warnings = CollectLogs(Level.WARNING, "de.fuberlin.wiwiss.silk.linkagerule") {
            //Load transformation rule
            val updatedRule = TransformRule.load(project.resources)(prefixes)(xml.head)
            val updatedRules = task.rules.updated(ruleIndex, updatedRule)
            //Update transformation task
            val updatedTask = task.updateRules(updatedRules, project)
            project.updateTask(updatedTask)
          }
          // Return warnings
          Ok(statusJson(warnings = warnings.map(_.getMessage)))
        } catch {
          case ex: ValidationException =>
            log.log(Level.INFO, "Invalid transformation rule")
            BadRequest(statusJson(errors = ex.errors))
          case ex: Exception =>
            log.log(Level.INFO, "Failed to save transformation rule", ex)
            InternalServerError(statusJson(errors =ValidationException.ValidationError("Error in back end: " + ex.getMessage) :: Nil))
        }
      case None =>
        BadRequest("Expecting text/xml request body")
    }
  }}

  private def statusJson(errors: Seq[ValidationException.ValidationError] = Nil, warnings: Seq[String] = Nil, infos: Seq[String] = Nil) = {
    /**Generates a Json expression from an error */
    def errorToJsExp(error: ValidationException.ValidationError) = JsObject(("message", JsString(error.toString)) :: ("id", JsString(error.id.map(_.toString).getOrElse(""))) :: Nil)

    JsObject(
      ("error", JsArray(errors.map(errorToJsExp))) ::
          ("warning", JsArray(warnings.map(JsString))) ::
          ("info", JsArray(infos.map(JsString))) :: Nil
    )
  }

  def reloadTransformCache(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[TransformTask](taskName)
    task.cache.clear()
    task.cache.load(project, task)
    Ok
  }

  def executeTransformTask(projectName: String, taskName: String) = Action { request =>
    val project = User().workspace.project(projectName)
    val task = project.task[TransformTask](taskName)

    // Retrieve parameters
    val params = request.body.asFormUrlEncoded.getOrElse(Map.empty)
    val outputNames = params.get("outputs[]").toSeq.flatten
    val outputs = outputNames.map(project.task[DatasetTask](_).dataset)

    // Create execution task
    val executeTransformTask =
      new ExecuteTransform(
        input = project.task[DatasetTask](task.dataSelection.datasetId).dataset.source,
        selection = task.dataSelection,
        rules = task.rules,
        outputs = outputs.map(_.sink)
      )

    // Start task in the background
    CurrentExecuteTransformTask() = Executor().execute(executeTransformTask)

    Ok
  }

  /**
   * Given a search term, returns all possible completions for source property paths.
   */
  def sourcePathCompletions(projectName: String, taskName: String, term: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[TransformTask](taskName)
    var completions = Seq[String]()

    // Add known paths
    if(task.cache.value != null) {
      val knownPaths = task.cache.value.paths
      completions ++= knownPaths.map(_.serializeSimplified(project.config.prefixes)).sorted
    }

    // Add known prefixes last
    val prefixCompletions = project.config.prefixes.prefixMap.keys.map(_ + ":")
    completions ++= prefixCompletions

    // Filter all completions that match the search term
    val matches = completions.filter(_.contains(term))

    // Convert to JSON and return
    Ok(JsArray(matches.map(JsString)))
  }

  def targetPathCompletions(projectName: String, taskName: String, term: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[TransformTask](taskName)

    // Collect known prefixes
    val prefixCompletions = project.config.prefixes.prefixMap.keys

    // Filter all completions that match the search term
    val matches = prefixCompletions.filter(_.contains(term)).toSeq.sorted.map(_ + ":")

    // Convert to JSON and return
    Ok(JsArray(matches.map(JsString)))
  }
}
