package plugins

import de.fuberlin.wiwiss.silk.workspace.modules.ModuleTask
import de.fuberlin.wiwiss.silk.workspace.modules.linking.LinkingTask
import plugins.WorkbenchPlugin.{Tab, TaskActions}

/**
 * The linking Workbench plugin.
 */
case class LinkingPlugin() extends WorkbenchPlugin {

  override def tasks = {
    Seq(LinkingTaskActions)
  }

  override def tabs(context: Context[ModuleTask]) = {
    var tabs = List[Tab]()
    if(context.task.isInstanceOf[LinkingTask]) {
      val p = context.project.name
      val t = context.task.name
      if (config.workbench.tabs.editor)
        tabs ::= Tab("Editor", s"linking/$p/$t/editor")
      if (config.workbench.tabs.generateLinks)
        tabs ::= Tab("Generate Links", s"linking/$p/$t/generateLinks")
      if (config.workbench.tabs.learn)
        tabs ::= Tab("Learn", s"linking/$p/$t/learnStart")
      if (config.workbench.tabs.referenceLinks)
        tabs ::= Tab("Reference Links", s"linking/$p/$t/referenceLinks")
      if (config.workbench.tabs.status)
        tabs ::= Tab("Status", s"linking/$p/$t/status")
    }
    tabs.reverse
  }

  object LinkingTaskActions extends TaskActions[LinkingTask] {

    /** The name of the task type */
    override def name: String = "Linking Task"

    /** Path to the task icon */
    override def icon: String = "workspace/img/arrow-join.png"

    /** The path to the dialog for creating a new task. */
    override def createDialog(project: String) =
      Some(s"workspace/dialogs/newLinkingTask/$project")

    /** The path to the dialog for editing an existing task. */
    override def editDialog(project: String, task: String) =
      Some(s"workspace/dialogs/editLinkingTask/$project/$task")

    /** The path to redirect to when the task is opened. */
    override def open(project: String, task: String) =
      Some(s"linking/$project/$task/editor")

    /** The path to delete the task by sending a DELETE HTTP request. */
    override def delete(project: String, task: String) =
      Some(s"linking/tasks/$project/$task")

    /** Retrieves a list of properties as key-value pairs for this task to be displayed to the user. */
    override def properties(task: ModuleTask): Seq[(String, String)] = {
      val linkingTask = task.asInstanceOf[LinkingTask]
      Seq(
        ("Source", linkingTask.linkSpec.datasets.source.sourceId.toString),
        ("Target", linkingTask.linkSpec.datasets.target.sourceId.toString),
        ("Source dataset", linkingTask.linkSpec.datasets.source.restriction.toString),
        ("Target dataset", linkingTask.linkSpec.datasets.target.restriction.toString)
      )
    }
  }

}
