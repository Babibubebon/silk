package de.fuberlin.wiwiss.silk.util

import collection.mutable.WeakHashMap

trait Observable[T] {
  private val subscribers = WeakHashMap[T => Unit, Unit]()

  /**
   * Execute a function on every update.
   * Note that the function is stored in a weak hash map i.e. it is removed as soon as it is no longer referenced.
   */
  def onUpdate(f: T => Unit) {
    subscribers.update(f, Unit)
  }

  protected def publish(event: T) {
    for(subscriber <- subscribers.keys)
      subscriber(event)
  }
}