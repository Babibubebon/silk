package controllers.transform

import org.silkframework.rule.TransformSpec
import org.silkframework.util.DPair
import org.silkframework.workspace.User
import org.silkframework.workspace.activity.transform.TransformPathsCache
import play.api.mvc.{Action, Controller}
import plugins.Context

object TransformEditor extends Controller {

  def start(project: String, task: String) = Action { request =>
    val context = Context.get[TransformSpec](project, task, request.path)
    Ok(views.html.editor.transformRules(context))
  }

  def editor(project: String, task: String, rule: String) = Action { request =>
    val context = Context.get[TransformSpec](project, task, request.path)
    context.task.data.rules.find(_.name == rule) match {
      case Some(r) => Ok(views.html.editor.transformEditor(context, r))
      case None => NotFound(s"No rule named '$rule' found!")
    }
  }

  def paths(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    val pathsCache = task.activity[TransformPathsCache].control
    val prefixes = project.config.prefixes
    val sourceName = task.data.selection.inputId.toString

    if(pathsCache.status().isRunning) {
      val loadingMsg = f"Cache loading (${pathsCache.status().progress * 100}%.1f%%)"
      ServiceUnavailable(views.html.editor.paths(DPair(sourceName, ""), DPair.fill(Seq.empty), onlySource = true, loadingMsg = loadingMsg))
    } else if(pathsCache.status().failed) {
      Ok(views.html.editor.paths(DPair(sourceName, ""), DPair.fill(Seq.empty), onlySource = true, warning = pathsCache.status().message))
    } else {
      val paths = DPair(pathsCache.value().paths.map(_.serialize(prefixes)), Seq.empty)
      Ok(views.html.editor.paths(DPair(sourceName, ""), paths, onlySource = true))
    }
  }

  def score(projectName: String, taskName: String) = Action {
    Ok
  }
}
