package org.silkframework.workspace.activity.linking

import org.silkframework.config.LinkSpecification
import org.silkframework.learning.LearningConfiguration
import org.silkframework.learning.active.{ActiveLearning, ActiveLearningState}
import org.silkframework.runtime.activity.Activity
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.workspace.Task
import org.silkframework.workspace.activity.TaskActivityFactory
import org.silkframework.workspace.activity.linking.LinkingTaskUtils._

@Plugin(
  id = "ActiveLearning",
  label = "Active Learning",
  categories = Array("LinkSpecification"),
  description = "Executes an active learning iteration."
)
case class ActiveLearningFactory() extends TaskActivityFactory[LinkSpecification, ActiveLearning] {

  def apply(task: Task[LinkSpecification]): Activity[ActiveLearningState] = {
    Activity.regenerating {
      // Update reference entities cache
      val entitiesCache = task.activity[ReferenceEntitiesCache].control
      entitiesCache.waitUntilFinished()
      entitiesCache.startBlocking()

      // Check if all links have been loaded
      val entitiesSize = entitiesCache.value().positiveEntities.size + entitiesCache.value().negativeEntities.size
      val refSize = task.data.referenceLinks.positive.size + task.data.referenceLinks.negative.size
      assert(entitiesSize == refSize, "Reference Entities Cache has not been loaded correctly")

      new ActiveLearning(
        config = LearningConfiguration.default,
        datasets = task.dataSources,
        linkSpec = task.data,
        paths = task.activity[LinkingPathsCache].value.map(_.paths),
        referenceEntities = task.activity[ReferenceEntitiesCache].value
      )
    }
  }

}
