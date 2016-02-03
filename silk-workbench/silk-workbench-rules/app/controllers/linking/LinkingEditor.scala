package controllers.linking

import org.silkframework.config.LinkSpecification
import org.silkframework.entity.EntitySchema
import org.silkframework.entity.rdf.SparqlEntitySchema
import org.silkframework.workspace.activity.linking.{ReferenceEntitiesCache, LinkingPathsCache}
import play.api.mvc.Controller
import play.api.mvc.Action
import org.silkframework.workspace.User
import org.silkframework.util.DPair
import org.silkframework.evaluation.LinkageRuleEvaluator
import plugins.Context

object LinkingEditor extends Controller {

  def editor(project: String, task: String) = Action { request =>
    val context = Context.get[LinkSpecification](project, task, request.path)
    Ok(views.html.editor.linkingEditor(context))
  }

  def paths(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    val pathsCache = task.activity[LinkingPathsCache].control
    val prefixes = project.config.prefixes

    if(pathsCache.status().isRunning) {
      val loadingMsg = f"Cache loading (${pathsCache.status().progress * 100}%.1f%%)"
      ServiceUnavailable(views.html.editor.paths(DPair.fill(Seq.empty), onlySource = false, loadingMsg = loadingMsg))
    } else if(pathsCache.status().failed) {
      Ok(views.html.editor.paths(DPair.fill(Seq.empty), onlySource = false, warning = pathsCache.status().message + " Try reloading the paths."))
    } else {
      val entityDescs = Option(pathsCache.value()).getOrElse(DPair.fill(EntitySchema.empty))
      val paths = entityDescs.map(_.paths.map(_.serialize(prefixes)))
      Ok(views.html.editor.paths(paths, onlySource = false))
    }
  }

  def score(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpecification](taskName)
    val entitiesCache = task.activity[ReferenceEntitiesCache].control

    // If the entity cache is still loading
    if(entitiesCache.status().isRunning) {
      ServiceUnavailable(f"Cache loading (${entitiesCache.status().progress * 100}%.1f%%)")
    // If the cache loading failed
    } else if(entitiesCache.status().failed) {
      Ok(views.html.editor.score(
        info = "No score available",
        error = "No score available as loading the entities that are referenced by the reference links failed. " +
                "Reason: " + entitiesCache.status().message))
    // If there are no reference links
    } else if (entitiesCache.value().positive.isEmpty || entitiesCache.value().negative.isEmpty) {
      Ok(views.html.editor.score(
        info = "No score available",
        error = "No score available as this project does not define any reference links."))
    // If everything needed for computing a score is available
    } else {
      val result = LinkageRuleEvaluator(task.data.rule, entitiesCache.value())
      val score = f"Precision: ${result.precision}%.2f | Recall: ${result.recall}%.2f | F-measure: ${result.fMeasure}%.2f"
      Ok(views.html.editor.score(score))
    }
  }

}
