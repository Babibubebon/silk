package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.util.task._
import de.fuberlin.wiwiss.silk.output.Link
import net.liftweb.http.SHtml
import xml.NodeSeq
import de.fuberlin.wiwiss.silk.workbench.evaluation.EvalLink.{Correct, Incorrect, Unknown, Generated}
import net.liftweb.http.js.JsCmds._
import de.fuberlin.wiwiss.silk.workbench.workspace.{CurrentTaskStatusListener, User}
import de.fuberlin.wiwiss.silk.workbench.evaluation.{CurrentGenerateLinksTask, EvalLink}
import de.fuberlin.wiwiss.silk.GenerateLinksTask

class GeneratedLinks extends Links with RateLinkButtons {

  /**Minimum time in milliseconds between two successive updates*/
  private val minUpdatePeriod = 3000L

  /**The time of the last update */
  private var lastUpdateTime = 0L

  override protected val showStatus = false

  private var generateLinksTask = CurrentGenerateLinksTask()

  private val currentGenerateLinksTaskListener = (task: GenerateLinksTask) => { generateLinksTask = task }

  CurrentGenerateLinksTask.onUpdate(currentGenerateLinksTaskListener)

  private val generatedLinkListener = new CurrentTaskStatusListener(CurrentGenerateLinksTask) {
    override def onUpdate(status: TaskStatus) {
      status match {
        case _: TaskStarted => {}
        case _: TaskRunning if System.currentTimeMillis - lastUpdateTime > minUpdatePeriod => {
          partialUpdate(updateLinksCmd)
          lastUpdateTime = System.currentTimeMillis
        }
        case _: TaskFinished => {
          val cmd = {
            val warnings = CurrentGenerateLinksTask().warnings
            if (warnings.isEmpty) {
              updateLinksCmd
            }
            else {
              updateLinksCmd & Alert("Warnings have been raised during execution:\n- " + warnings.map(_.getMessage).mkString("\n- "))
            }
          }

          partialUpdate(cmd)
        }
        case _ =>
      }
    }
  }

  override protected def links: Seq[EvalLink] = {
    def alignment = linkingTask.alignment

    for (link <- generateLinksTask.links.view) yield {
      if (alignment.positive.contains(link)) {
        new EvalLink(link, Correct, Generated)
      } else if (alignment.negative.contains(link)) {
        new EvalLink(link, Incorrect, Generated)
      } else {
        new EvalLink(link, Unknown, Generated)
      }
    }
  }

  override protected def renderStatus(link: EvalLink): NodeSeq = {
    link.correct match {
      case Correct => <div>correct</div>
      case Incorrect => <div>wrong</div>
      case Unknown => <div>unknown</div>
    }
  }
}
