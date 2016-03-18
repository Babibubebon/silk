package org.silkframework.workspace.activity.transform

import org.silkframework.config.TransformSpecification
import org.silkframework.entity.EntitySchema
import org.silkframework.runtime.activity.{Activity, ActivityContext}
import org.silkframework.workspace.Task
import org.silkframework.workspace.activity.transform.TransformTaskUtils._

/**
 * Holds the most frequent paths.
 */
class TransformPathsCache(task: Task[TransformSpecification]) extends Activity[EntitySchema] {

  override def name = s"Paths cache ${task.name}"

  override def initialValue = Some(EntitySchema.empty)

  /**
   * Loads the most frequent paths.
   */
  override def run(context: ActivityContext[EntitySchema]) = {
    val transform = task.data

    //Create an entity description from the transformation task
    val currentEntityDesc = transform.entitySchema

    //Check if paths have not been loaded yet or if the restriction has been changed
    if (context.value().paths.isEmpty || currentEntityDesc.typeUri != context.value().typeUri) {
      // Retrieve the data sources
      val source = task.dataSource
      //Retrieve most frequent paths
      context.status.update("Retrieving frequent paths", 0.0)
      val paths = source.retrievePaths(transform.selection.typeUri, 1)
      //Add the frequent paths to the entity description
      context.value() = currentEntityDesc.copy(paths = (currentEntityDesc.paths ++ paths).distinct)
    }
  }
}