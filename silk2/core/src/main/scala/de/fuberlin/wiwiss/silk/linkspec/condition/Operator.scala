package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.instance.Instance

trait Operator
{
  val required : Boolean

  val weight : Int

  def apply(sourceInstance : Instance, targetInstance : Instance) : Traversable[Double]

  def index(instance : Instance) : Set[Seq[Int]]

  val blockCounts : Seq[Int]
}
