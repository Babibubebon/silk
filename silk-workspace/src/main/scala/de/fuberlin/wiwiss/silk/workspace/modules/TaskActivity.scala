package de.fuberlin.wiwiss.silk.workspace.modules

import java.util.logging.{Logger, Level}
import de.fuberlin.wiwiss.silk.runtime.activity.{ActivityContext, Activity}
import de.fuberlin.wiwiss.silk.runtime.resource.ResourceManager
import de.fuberlin.wiwiss.silk.runtime.serialization.{XmlFormat, Serialization}
import de.fuberlin.wiwiss.silk.runtime.serialization.Serialization._
import de.fuberlin.wiwiss.silk.util.XMLUtils._
import scala.xml.{XML, Node}

trait TaskActivity[T] extends Activity[T] {

  def activityType: Class[_]

}

object TaskActivity {

  /**
   * Creates a task activity that runs in the background and does not hold a value
   *
   * @param create Given the previous value, a function to create a new activity that updates the value.
   * @return The task activity.
   */
  def apply(create: => Activity[Unit]): TaskActivity[Unit] = new BackgroundActivity[Unit](Unit, (Unit) => create)

  /**
   * Creates a task activity that updates a value.
   *
   * @param initialValue The initial value.
   * @param create Given the previous value, a function to create a new activity that updates the value.
   * @tparam T The type of the value.
   * @return The task activity.
   */
  def apply[T](initialValue: T, create: T => Activity[T]): TaskActivity[T] = new BackgroundActivity[T](initialValue, create)

  /**
   * Creates a task activity that updates a value that is cached on the filesystem.
   *
   * @param resourceName The resource name used for persisting the value.
   * @param initialValue The initial value.
   * @param create Given the previous value, a function to create a new activity that updates the value.
   * @param resourceMgr The resource manager used for persisting the value.
   * @tparam T The type of the value.
   * @return The task activity.
   */
  def apply[T](resourceName: String, initialValue: T, create: () => Activity[T], resourceMgr: ResourceManager)(implicit xmlFormat: XmlFormat[T]) : TaskActivity[T] = new CachedActivity[T](resourceName, initialValue, create, resourceMgr)

  /**
   * A task activity that executes in the background.
   */
  private class BackgroundActivity[T](override val initialValue: T, create: T => Activity[T]) extends TaskActivity[T] {

    override val activityType = create(initialValue).getClass

    override def name = create(initialValue).name

    /**
     * Executes this activity.
     *
     * @param context Holds the context in which the activity is executed.
     */
    override def run(context: ActivityContext[T]): Unit = {
      val child = context.child(create(context.value()), 1.0)
      child.value.onUpdate(context.value.update)
      child.startBlocking()
    }
  }

  /**
   * A task activity that executes in the background and caches its value.
   */
  private class CachedActivity[T](resourceName: String, defaultValue: T, create: () => Activity[T], resourceMgr: ResourceManager)(implicit xmlFormat: XmlFormat[T]) extends TaskActivity[T] {

    private val log = Logger.getLogger(classOf[CachedActivity[_]].getName)

    override val activityType = create().getClass

    override def name = create().name

    override def initialValue: T = readValue()

    override def run(context: ActivityContext[T]): Unit = {
      // Create a new child activity
      val child = context.child(create(), 1.0)
      // Update value of this task when child value changes
      var updated = false
      child.value.onUpdate{ v =>
        context.value.update(v)
        updated = true
      }
      // Execute activity
      val result = child.startBlocking(Some(context.value()))
      // Persist value
      if(updated)
        writeValue(result)
    }

    private def readValue(): T = {
      try {
        val xml = XML.load(resourceMgr.get(resourceName).load)
        val value = fromXml[T](xml)
        log.info(s"Cache read from $resourceName")
        value
      } catch {
        case ex: Exception =>
          log.log(Level.WARNING, s"Could not load cache from $resourceName", ex)
          defaultValue
      }
    }

    private def writeValue(value: T): Unit = {
      try {
        resourceMgr.put(resourceName)(toXml[T](value).write)
        log.info(s"Cache written to $resourceName.")
      } catch {
        case ex: Exception =>
          log.log(Level.WARNING, s"Could not write cache to $resourceName", ex)
      }
    }
  }

}