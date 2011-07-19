package de.fuberlin.wiwiss.silk.workbench.workspace.modules.linking

import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.ModuleTask
import de.fuberlin.wiwiss.silk.evaluation.Alignment
import de.fuberlin.wiwiss.silk.workbench.workspace.Project

/**
 * A linking task which interlinks two datasets.
 */
class LinkingTask private(val linkSpec: LinkSpecification, val alignment: Alignment, val cache: Cache) extends ModuleTask {
  val name = linkSpec.id

  def updateLinkSpec(linkSpec: LinkSpecification, project: Project) = {
    new LinkingTask(linkSpec, alignment, cache.update(project, linkSpec, alignment))
  }

  def updateAlignment(alignment: Alignment, project: Project) = {
    new LinkingTask(linkSpec, alignment, cache.update(project, linkSpec, alignment))
  }
}

object LinkingTask {
  def apply(linkSpec: LinkSpecification, alignment: Alignment = Alignment(), cache: Cache = new Cache()) = {
    new LinkingTask(linkSpec, alignment, cache)
  }
}