package de.fuberlin.wiwiss.silk.util

import collection.mutable.{SynchronizedMap, WeakHashMap}

trait Observable[T] {
  private val subscribers = new WeakHashMap[T => _, Unit]() with SynchronizedMap[T => _, Unit]

  /**
   * Execute a function on every update.
   * Note that the function is stored in a weak hash map i.e. it is removed as soon as it is no longer referenced.
   *
   * @return The provided function
   */
  def onUpdate[U](f: T => U) = {
    subscribers.update(f, Unit)
    f
  }

  protected def publish(event: T) {
    for(subscriber <- subscribers.keys)
      subscriber(event)
  }

  def removeSubscriptions() {
    subscribers.clear()
  }
}