/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.execution

import java.io.File
import java.util.logging.LogRecord
import de.fuberlin.wiwiss.silk.cache.{FileEntityCache, MemoryEntityCache}
import de.fuberlin.wiwiss.silk.config.{LinkSpecification, RuntimeConfig}
import de.fuberlin.wiwiss.silk.dataset.{Dataset,DataSink, DataSource}
import de.fuberlin.wiwiss.silk.entity.{Entity, Link}
import de.fuberlin.wiwiss.silk.runtime.task.{TaskContext, ValueHolder, Task}
import de.fuberlin.wiwiss.silk.util.FileUtils._
import de.fuberlin.wiwiss.silk.util.{CollectLogs, DPair}

/**
 * Main task to generate links.
 */
class GenerateLinksTask(inputs: DPair[DataSource],
                        linkSpec: LinkSpecification,
                        outputs: Seq[DataSink] = Seq.empty,
                        runtimeConfig: RuntimeConfig = RuntimeConfig()) extends Task {

  /** The task used for loading the entities into the cache */
  @volatile private var loadTask: LoadTask = null

  /** The task used for matching the entities */
  @volatile private var matchTask: MatchTask = null

  /** The warnings which occurred during execution */
  @volatile private var warningLog: Seq[LogRecord] = Seq.empty

  /** The entity descriptions which define which entities are retrieved by this task */
  def entityDescs = linkSpec.entityDescriptions

  /** The links which have been generated so far by this task */
  val links = new ValueHolder[Seq[Link]](Seq.empty)

  /**
   * All warnings which have been generated during executing.
   */
  def warnings = warningLog

  /**
   * Stops the tasks and removes all generated links.
   */
  def clear() {
    cancelExecution()
    links.update(Seq.empty)
  }

  override def execute(context: TaskContext) = {
    //TODO statusLogLevel = runtimeConfig.logLevel
    //TODO progressLogLevel = runtimeConfig.logLevel

    links.update(Seq.empty)

    warningLog = CollectLogs() {
      //Entity caches
      val caches = createCaches()

      //Create tasks
      loadTask = new LoadTask(inputs, caches)
      matchTask = new MatchTask(linkSpec.rule, caches, runtimeConfig)

      //Load entities
      if (runtimeConfig.reloadCache) {
        //TODO loadTask.statusLogLevel = statusLogLevel
        //TODO loadTask.progressLogLevel = progressLogLevel
        loadTask.runInBackground()
      }

      //Execute matching
      matchTask.links.onUpdate(links.update)
      context.executeBlocking(matchTask, 0.95)

      //Filter links
      val filterTask = new FilterTask(matchTask.links(), linkSpec.rule.filter)
      context.executeBlocking(filterTask)
      links.update(filterTask.filteredLinks)

      //Output links
      val outputTask = new OutputTask(filterTask.filteredLinks, linkSpec.rule.linkType, outputs)
      context.executeBlocking(outputTask)
    }
  }

  private def createCaches() = {
    val indexFunction = (entity: Entity) => runtimeConfig.executionMethod.indexEntity(entity, linkSpec.rule)

    if (runtimeConfig.useFileCache) {
      val cacheDir = new File(runtimeConfig.homeDir + "/entityCache/" + linkSpec.id)

      DPair(
        source = new FileEntityCache(entityDescs.source, indexFunction, cacheDir + "/source/", runtimeConfig),
        target = new FileEntityCache(entityDescs.target, indexFunction, cacheDir + "/target/", runtimeConfig)
      )
    } else {
      DPair(
        source = new MemoryEntityCache(entityDescs.source, indexFunction, runtimeConfig),
        target = new MemoryEntityCache(entityDescs.target, indexFunction, runtimeConfig)
      )
    }
  }
}

object GenerateLinksTask {

  def fromSources(inputs: Traversable[Dataset],
                  linkSpec: LinkSpecification,
                  outputs: Seq[Dataset] = Seq.empty,
                  runtimeConfig: RuntimeConfig = RuntimeConfig()) = {
    val sourcePair = DPair.fromSeq(linkSpec.datasets.map(_.datasetId).map(id => inputs.find(_.id == id).get.source))
    val sinks = outputs.map(_.sink)
    new GenerateLinksTask(sourcePair, linkSpec, sinks, runtimeConfig)
  }

  def empty = new GenerateLinksTask(DPair.empty, LinkSpecification())
}