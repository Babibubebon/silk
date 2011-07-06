package de.fuberlin.wiwiss.silk.linkspec.evaluation

import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.linkspec.condition.{Comparison, Aggregation, Operator, LinkCondition}
import de.fuberlin.wiwiss.silk.output.Link
import de.fuberlin.wiwiss.silk.output.Link.InputValue
import de.fuberlin.wiwiss.silk.linkspec.input.{TransformInput, PathInput, Input}
import de.fuberlin.wiwiss.silk.instance.{Path, Instance}

object DetailedEvaluator
{
  def apply(condition : LinkCondition, instances : SourceTargetPair[Instance], limit : Double = -1.0) : Option[Link] =
  {
    condition.rootOperator match
    {
      case Some(op) =>
      {
        val confidence = evaluateOperator(op, instances, limit)

        if(confidence.value.getOrElse(-1.0) >= limit)
        {
          Some(new Link(instances.source.uri, instances.target.uri, Some(confidence)))
        }
        else
        {
          None
        }
      }
      case None =>
      {
        if(limit == -1.0)
        {
          Some(new Link(instances.source.uri, instances.target.uri, Some(Link.SimpleConfidence(Some(-1.0)))))
        }
        else
        {
          None
        }
      }
    }
  }

  private def evaluateOperator(operator : Operator, instances : SourceTargetPair[Instance], threshold : Double) = operator match
  {
    case aggregation : Aggregation => evaluateAggregation(aggregation, instances, threshold)
    case comparison : Comparison => evaluateComparison(comparison, instances, threshold)
  }

  private def evaluateAggregation(aggregation : Aggregation, instances : SourceTargetPair[Instance], threshold : Double) : Link.AggregatorConfidence =
  {
    val totalWeights = aggregation.operators.map(_.weight).sum

    var isNone = false

    val operatorValues =
    {
      for(operator <- aggregation.operators) yield
      {
        val updatedThreshold = aggregation.aggregator.computeThreshold(threshold, operator.weight.toDouble / totalWeights)
        val value = evaluateOperator(operator, instances, updatedThreshold)
        if(operator.required && value.value.isEmpty) isNone = true

        value
      }
    }

    val weightedValues = aggregation.operators.map(_.weight) zip operatorValues.map(_.value.getOrElse(-1.0))

    val aggregatedValue = aggregation.aggregator.evaluate(weightedValues)

    val name = aggregation.aggregator.strategyName

    if(isNone)
      Link.AggregatorConfidence(None, name, operatorValues)
    else
      Link.AggregatorConfidence(aggregatedValue, name, operatorValues)
  }

  private def evaluateComparison(comparision : Comparison, instances : SourceTargetPair[Instance], threshold : Double) : Link.ComparisonConfidence =
  {
    val distance = comparision.apply(instances, threshold)

    val sourcePath = findPath(comparision.inputs.source)
    val targetPath = findPath(comparision.inputs.target)

    val sourceInput = InputValue(sourcePath, comparision.inputs.source(instances))
    val targetInput = InputValue(targetPath, comparision.inputs.target(instances))

    val name = comparision.metric.strategyName

    Link.ComparisonConfidence(distance, name, sourceInput, targetInput)
  }

  private def findPath(input : Input) : Path = input match
  {
    case PathInput(path) => path
    case TransformInput(inputs, _) => findPath(inputs.head)
  }
}