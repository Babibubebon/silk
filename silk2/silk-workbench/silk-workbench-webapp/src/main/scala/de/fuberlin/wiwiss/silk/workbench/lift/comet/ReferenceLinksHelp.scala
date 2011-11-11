package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.workbench.workspace.User
import xml.{NodeSeq, Text}

class ReferenceLinksHelp extends LinksHelp {

  /**
   * Re-renders the widget if the current linking task (and with it the reference links) has been changed.
   */
  private val taskListener = User().onUpdate {
    case _: User.CurrentTaskChanged => reRender()
    case _ =>
  }

  override def overview = {
    <div>
      The reference links of this linking task.
      Positive reference links represent definitive matches, while negative reference links represent definitive non-matches.
      The reference links are used for evaluating the quality of the current linkage rule as well as for learning new linkage rules.
    </div>
  }

  override def actions = {
    val links = User().linkingTask.referenceLinks
    if(links.isEmpty) {
      Text("This linking task does not contain any reference links yet.") ++
      howToAddReferenceLinks
    } else if(links.positive.isEmpty) {
      Text("This linking task does not contain any positive reference links yet.") ++
      howToAddReferenceLinks
    } else if(links.negative.isEmpty) {
      Text("This linking task does not contain any negative reference links yet.") ++
      howToAddReferenceLinks
    } else {
      NodeSeq.Empty
    }
  }
}