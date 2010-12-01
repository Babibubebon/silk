package de.fuberlin.wiwiss.silk.impl.metric

import de.fuberlin.wiwiss.silk.linkspec.Metric
import de.fuberlin.wiwiss.silk.util.StringUtils._

class LevenshteinMetric(val params : Map[String, String] = Map.empty) extends Metric
{
  private val minChar = readOptionalParam("minChar").getOrElse("0").head
  private val maxChar = readOptionalParam("maxChar").getOrElse("Z").head
  private val maxDistance = readOptionalIntParam("maxDistance")
  private val q = readOptionalIntParam("q").getOrElse(1)

  override def evaluate(str1 : String, str2 : String) =
  {
    val maxDistance = math.min(str1.length, str2.length)

    if(math.abs(str1.length - str2.length) >= maxDistance)
    {
      0.0
    }
    else
    {
      val levenshteinDistance = evaluateDistance(str1, str2)

      math.max(0.0, 1.0 - levenshteinDistance.toDouble / maxDistance.toDouble)
    }
  }

  //TODO compute qGrams lazy
  override def index(str : String) : Set[Seq[Int]] =
  {
    val k = maxDistance match
    {
      case Some(dist) => dist
      case None => str.length
    }

    val qGrams = str.qGrams(q).take(k * q + 1)

    qGrams.map(indexQGram).toSet
  }

  private def indexQGram(qGram : String) =
  {
    Seq(qGram.foldLeft(0)((index, char) => index * (maxChar - minChar) + char - minChar))
  }

  override val blockCounts : Seq[Int] =
  {
    Seq(BigInt(maxChar - minChar).pow(q).toInt)
  }

  def evaluateDistance(str1 : String, str2 : String): Int =
  {
    val lenStr1 = str1.length
    val lenStr2 = str2.length

    val d: Array[Array[Int]] = Array.ofDim(lenStr1 + 1, lenStr2 + 1)

    for (i <- 0 to lenStr1) d(i)(0) = i
    for (j <- 0 to lenStr2) d(0)(j) = j

    for (i <- 1 to lenStr1; val j <- 1 to lenStr2) {
      val cost = if (str1(i - 1) == str2(j - 1)) 0 else 1

      d(i)(j) = math.min(
        d(i - 1)(j) + 1, // deletion
        math.min(d(i)(j - 1) + 1, // insertion
          d(i - 1)(j - 1) + cost) // substitution
      )
    }
    return d(lenStr1)(lenStr2)
  }

  //from [Gonzalo Navarro - A guided tour to approximate string matching]
  def evaluateDistance2(p : String, t : String) : Int =
  {
    var min = Integer.MAX_VALUE

    //Previous column in the matrix
    var pc = new Array[Int](p.length + 1)

    //Current column in the matrix
    var c = new Array[Int](p.length + 1)

    //Substring matching: The empty pattern matches with zero errors at any text position
    //        for (int i = 0; i <= p.length(); i++)
    //            c[i] = i;

    //Build the matrix
    for(j <- 1 to t.length())
    {
      //Swap the current and the previous column
      val temp = pc;
      pc = c;
      c = temp;

      //Compute current column
      for(i <- 1 to p.length())
      {
        if (p.charAt(i - 1) == t.charAt(j - 1))
          c(i) = pc(i - 1)
        else
          c(i) = 1 + min3(c(i - 1), pc(i), pc(i - 1))
      }

      if (c(p.length()) < min)
        min = c(p.length())
    }

    return min;
  }

  private def min3(x : Int, y : Int, z : Int) = math.min(x, math.min(y, z))
}
