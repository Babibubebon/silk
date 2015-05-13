package de.fuberlin.wiwiss.silk.workspace.modules.linking

import de.fuberlin.wiwiss.silk.config.LinkSpecification
import de.fuberlin.wiwiss.silk.dataset.{Dataset, DataSink, DataSource}
import de.fuberlin.wiwiss.silk.execution.GenerateLinks
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.workspace.modules.TaskExecutor

class LinkingTaskExecutor extends TaskExecutor[LinkSpecification] {

  def apply(inputs: Seq[DataSource], linkSpec: LinkSpecification, outputs: Seq[Dataset]) = {
    require(inputs.size == 1 || inputs.size == 2, "Linking tasks expect one or two input datasets.")

    val inputPair = if(inputs.size == 1) DPair.fill(inputs.head) else DPair.fromSeq(inputs)
    new GenerateLinks(inputPair, linkSpec.copy(outputs = outputs))
  }
}
