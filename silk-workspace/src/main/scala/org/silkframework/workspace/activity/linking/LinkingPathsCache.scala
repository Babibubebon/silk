package org.silkframework.workspace.activity.linking

import org.silkframework.config.{DatasetSelection, LinkSpecification, TransformSpecification}
import org.silkframework.dataset.Dataset
import org.silkframework.entity.{Path, EntitySchema}
import org.silkframework.runtime.activity.{Activity, ActivityContext}
import org.silkframework.util.DPair
import org.silkframework.workspace.Task

/**
 * Holds the most frequent paths.
 */
class LinkingPathsCache(task: Task[LinkSpecification]) extends Activity[DPair[EntitySchema]] {

  private def linkSpec = task.data

  override def name = s"Paths cache ${linkSpec.id}"

  override def initialValue = Some(DPair.fill(EntitySchema.empty))

  /**
   * Loads the most frequent property paths.
   */
  override def run(context: ActivityContext[DPair[EntitySchema]]): Unit = {
    context.status.update("Retrieving frequent property paths", 0.0)

    //Create an entity description from the link specification
    val currentEntityDescs = linkSpec.entityDescriptions

    //Check if the restriction has been changed
    val update =
      (context.value().source.paths.isEmpty && context.value().target.paths.isEmpty) ||
      (currentEntityDescs.source.typeUri != context.value().source.typeUri &&
       currentEntityDescs.target.typeUri != context.value().target.typeUri)

    // Update paths
    if (update) {
      val updatedSchemata =
        for((dataSelection, entitySchema) <- linkSpec.dataSelections zip currentEntityDescs) yield {
          val paths = retrievePaths(dataSelection)
          entitySchema.copy(paths = entitySchema.paths ++ paths.distinct)
        }
      context.value.update(updatedSchemata)
    }
  }

  private def retrievePaths(datasetSelection: DatasetSelection) = {
    task.project.taskOption[TransformSpecification](datasetSelection.datasetId) match {
      case Some(transformTask) =>
        transformTask.data.rules.flatMap(_.target).map(Path(_)).distinct.toIndexedSeq
      case None =>
        // Retrieve the data source
        val source = task.project.task[Dataset](datasetSelection.datasetId).data.source
        //Retrieve most frequent paths
        source.retrievePaths(datasetSelection.typeUri, 1, Some(50))
    }
  }
}
