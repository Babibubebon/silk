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

package de.fuberlin.wiwiss.silk.workspace.modules

import java.util.concurrent.{ScheduledFuture, TimeUnit, Executors}
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.runtime.activity.{HasValue, Activity, ActivityControl}
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.workspace.Project
import scala.reflect.ClassTag


/**
 * A task.
 *
 * @tparam DataType The data type that specifies the properties of this task.
 */
class Task[DataType](val name: Identifier, initialData: DataType,
                     plugin: ModulePlugin[DataType], project: Project) {

  private val log = Logger.getLogger(getClass.getName)

  @volatile
  private var currentData: DataType = initialData

  @volatile
  private var scheduledWriter: Option[ScheduledFuture[_]] = None

  private val activities = plugin.activities(this, project)

  private var activityControls = Map[Class[_], ActivityControl[_]]()
  for(activity <- activities)
    activityControls += ((activity.activityType, Activity(activity)))

  /**
   * Retrieves the current data of this task.
   */
  def data = currentData

  /**
   * Updates the data of this task.
   */
  def update(newData: DataType) = synchronized {
    // Update data
    currentData = newData
    // (Re)Schedule write
    for(writer <- scheduledWriter) {
      writer.cancel(false)
    }
    scheduledWriter = Some(Task.scheduledExecutor.schedule(Writer, Task.writeInterval, TimeUnit.SECONDS))
    log.info("Updated task '" + name + "'")
  }

  /**
   * Retrieves an activity by type.
   *
   * @tparam T The type of the requested activity
   * @return The activity control for the requested activity
   */
  def activity[T <: HasValue : ClassTag]: ActivityControl[T#ValueType] = {
    val requestedClass = implicitly[ClassTag[T]].runtimeClass
    activityControls.getOrElse(requestedClass, throw new NoSuchElementException(s"Task '$name' in project '${project.name}' does not contain an activity of type '${requestedClass.getName}'"))
                    .asInstanceOf[ActivityControl[T#ValueType]]
  }

  /**
   * Retrieves an activity by name.
   *
   * @param name The name of the requested activity
   * @return The activity control for the requested activity
   */
  def activity(name: String): ActivityControl[_] = {
    activityControls.values.find(_.name == name)
      .getOrElse(throw new NoSuchElementException(s"Task '$name' in project '${project.name}' does not contain an activity named '$name'"))
  }

  private object Writer extends Runnable {
    override def run(): Unit = {
      // Write task
      plugin.writeTask(name, data, project.resourceManager.child(plugin.prefix))
      log.info(s"Persisted task '$name' in project '${project.name}'")
      // Update caches
      for(activity <- activities if activity.autoRun) {
        val activityControl = activityControls(activity.activityType)
        activityControl.cancel()
        activityControl.start()
      }
    }
  }
}

object Task {

  /* Do not persist updates more frequently than this (in seconds) */
  private val writeInterval = 5

  private val scheduledExecutor = Executors.newSingleThreadScheduledExecutor()

}