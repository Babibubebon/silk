package controllers.linking

import controllers.core.{Stream, Widgets}
import models.linking.EvalLink.{Correct, Generated, Incorrect, Unknown}
import models.linking.{EvalLink, LinkSorter}
import org.silkframework.dataset.{Dataset, DatasetTask}
import org.silkframework.rule.execution.{GenerateLinks => GenerateLinksActivity}
import org.silkframework.rule.LinkSpec
import org.silkframework.rule.evaluation.DetailedEvaluator
import org.silkframework.workspace.User
import play.api.mvc.{Action, Controller}
import plugins.Context

object GenerateLinks extends Controller {

  def generateLinks(project: String, task: String) = Action { request =>
    val context = Context.get[LinkSpec](project, task, request.path)
    Ok(views.html.generateLinks.generateLinks(context))
  }

  def generateLinksDialog(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val outputs = project.tasks[Dataset].map(_.id.toString())

    Ok(views.html.generateLinks.generateLinksDialog(projectName, taskName, outputs))
  }

  def links(projectName: String, taskName: String, sorting: String, filter: String, page: Int) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpec](taskName)
    val linkSorter = LinkSorter.fromId(sorting)
    val linking = task.activity[GenerateLinksActivity].value
    val schemata = task.data.entityDescriptions

    // We only show links if entities have been attached to them. We check this by looking at the first link.
    val showLinks = {
      linking.links.headOption.flatMap(_.entities) match {
        case Some(entities) =>
          // Check if the entities got all paths that are used in the linkage rule
          schemata.source.paths.forall(entities.source.desc.paths.contains) &&
          schemata.target.paths.forall(entities.target.desc.paths.contains)
        case None => false
      }
    }

    if(showLinks) {
      val referenceLinks = task.data.referenceLinks
      def links =
        for (link <- linking.links.view;
             detailedLink <- DetailedEvaluator(task.data.rule, link.entities.get)) yield {
          if (referenceLinks.positive.contains(link))
            new EvalLink(detailedLink, Correct, Generated)
          else if (referenceLinks.negative.contains(link))
            new EvalLink(detailedLink, Incorrect, Generated)
          else
            new EvalLink(detailedLink, Unknown, Generated)
        }
      Ok(views.html.widgets.linksTable(project, task, links, Some(linking.statistics), linkSorter, filter, page, showStatus = false, showDetails = true, showEntities = false, rateButtons = true))
    } else {
      // Show an empty links table
      Ok(views.html.widgets.linksTable(project, task, Seq[EvalLink](), Some(linking.statistics), linkSorter, filter, page, showStatus = false, showDetails = true, showEntities = false, rateButtons = true))
    }
  }

  def linksStream(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpec](taskName)
    val stream = Stream.activityValue(task.activity[GenerateLinksActivity].control)
    Ok.chunked(Widgets.autoReload("updateLinks", stream))
  }

  def statusStream(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpec](taskName)
    val stream = Stream.status(task.activity[GenerateLinksActivity].control.status)
    Ok.chunked(Widgets.statusStream(stream))
  }

}
