package de.fuberlin.wiwiss.silk.entity

import java.io.{DataInputStream, DataOutputStream}

/**
 * Efficient index.
 */
//TODO Test if we need a bitset anymore (due to optimizations in the cache we have fewer indices per entity now)
final class Index private(private val bitset: Array[Long]) {
  /**
   * Checks if this index matches another index.
   */
  def matches(other: Index) = {
    var found = false
    var i = 0
    while (!found && i < Index.Size) {
      found = (bitset(i) & other.bitset(i)) != 0
      i += 1
    }

    found
  }

  def serialize(stream: DataOutputStream) {
    bitset.foreach(stream.writeLong)
  }
}

object Index {
  /**
   * Size of the index i.e. the number of long integers used to represent the bit set.
   */
  private val Size = 100

  def build(index: Set[Int]) = {
    val array = new Array[Long](Size)

    for (i <- index) {
      val ci = i % (Size * 64)

      array(ci >> 6) |= (1L << ci)
    }

    new Index(array)
  }

  def deserialize(stream: DataInputStream) = {
    new Index(Array.fill(Size)(stream.readLong))
  }
}