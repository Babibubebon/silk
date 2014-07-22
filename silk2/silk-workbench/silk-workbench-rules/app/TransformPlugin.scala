import de.fuberlin.wiwiss.silk.workspace.modules.ModuleTask
import de.fuberlin.wiwiss.silk.workspace.modules.transform.TransformTask
import plugins.WorkbenchPlugin.{Tab, TaskActions}
import plugins.{Context, WorkbenchPlugin}

case class TransformPlugin() extends WorkbenchPlugin {

  override def routes = Map("rules" -> rules.Routes, "transform" -> transform.Routes)

  override def tasks = {
    Seq(TransformTaskActions)
  }

  override def tabs(context: Context[ModuleTask]) = {
    var tabs = List[Tab]()
    if(context.task.isInstanceOf[TransformTask]) {
      val p = context.project.name
      val t = context.task.name
      tabs ::= Tab("Editor", s"transform/$p/$t/editor")
      tabs ::= Tab("Evaluate", s"transform/$p/$t/evaluate")
      tabs ::= Tab("Execute", s"transform/$p/$t/execute")
    }
    tabs.reverse
  }

  object TransformTaskActions extends TaskActions[TransformTask] {

    /** The name of the task type */
    override def name: String = "Transform Task"

    /** Path to the task icon */
    override def icon: String = "img/arrow-skip.png"

    /** The path to the dialog for creating a new task. */
    override def createDialog(project: String) =
      Some(s"transform/dialogs/newTransformTask/$project")

    /** The path to the dialog for editing an existing task. */
    override def editDialog(project: String, task: String) =
      Some(s"transform/dialogs/editTransformTask/$project/$task")

    /** The path to redirect to when the task is opened. */
    override def open(project: String, task: String) =
      Some(s"transform/$project/$task/editor")

    /** The path to delete the task by sending a DELETE HTTP request. */
    override def delete(project: String, task: String) =
      Some(s"transform/tasks/$project/$task")

    /** Retrieves a list of properties as key-value pairs for this task to be displayed to the user. */
    override def properties(task: ModuleTask): Seq[(String, String)] = {
      val transformTask = task.asInstanceOf[TransformTask]
      Seq(
        ("Source", transformTask.dataSelection.datasetId.toString),
        ("Dataset", transformTask.dataSelection.restriction.toString)
      )
    }
  }
}
