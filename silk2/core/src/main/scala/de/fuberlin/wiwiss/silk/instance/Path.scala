package de.fuberlin.wiwiss.silk.instance

import collection.mutable.{SynchronizedMap, WeakHashMap}
import java.util.concurrent.atomic.AtomicInteger

/**
 * Represents an RDF path.
 */
case class Path(variable : String, operators : List[PathOperator], id : Int = Path.currentId.getAndIncrement())
{
  /**
   * Serializes this path using the Silk RDF path language.
   */
  override def toString = "?" + variable + operators.mkString

  /**
   * Tests if this path equals another path
   */
  override def equals(other : Any) = other.isInstanceOf[Path] && toString == other.toString

  override def hashCode = toString.hashCode
}

object Path
{
  private val pathCache = new WeakHashMap[String, Path]() with SynchronizedMap[String, Path]

  private val currentId = new AtomicInteger(0)

  /**
   * Parses a path string.
   * May return a cached copy.
   */
  def parse(pathStr : String) =
  {
    //Try to retrieve a cached copy. If not found, parse the path
    pathCache.getOrElseUpdate(pathStr, PathParser.parse(pathStr))
  }
}
