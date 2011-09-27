package de.fuberlin.wiwiss.silk.evaluation

import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.entity.Entity
import de.fuberlin.wiwiss.silk.output.Link

case class ReferenceEntities(positive: Map[Link, DPair[Entity]] = Map.empty,
                              negative: Map[Link, DPair[Entity]] = Map.empty) {
  def withPositive(entityPair: DPair[Entity]) = {
    copy(positive = positive + (new Link(entityPair.source.uri, entityPair.target.uri) -> entityPair))
  }

  def withNegative(entityPair: DPair[Entity]) = {
    copy(negative = negative + (new Link(entityPair.source.uri, entityPair.target.uri) -> entityPair))
  }
}

object ReferenceEntities {
  def empty = ReferenceEntities(Map.empty, Map.empty)

  def fromEntities(positiveEntities: Traversable[DPair[Entity]], negativeEntities: Traversable[DPair[Entity]]) = {
    ReferenceEntities(
      positive = positiveEntities.map(i => (new Link(i.source.uri, i.target.uri), i)).toMap,
      negative = negativeEntities.map(i => (new Link(i.source.uri, i.target.uri), i)).toMap
    )
  }
}