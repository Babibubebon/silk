package de.fuberlin.wiwiss.silk.instance

import de.fuberlin.wiwiss.silk.linkspec._
import input.{TransformInput, PathInput, Input}
import de.fuberlin.wiwiss.silk.config.Configuration
import de.fuberlin.wiwiss.silk.util.SourceTargetPair

@serializable
class InstanceSpecification(val variable : String, val restrictions : String, val paths : Traversable[Path], val prefixes : Map[String, String])
{
    override def toString = "InstanceSpecification(variable='" + variable + "' restrictions='" + restrictions + "' paths=" + paths + ")"
}

object InstanceSpecification
{
    def empty = new InstanceSpecification("a", "", Traversable.empty, Map.empty)

    def retrieve(config : Configuration, linkSpec : LinkSpecification) : SourceTargetPair[InstanceSpecification] =
    {
        val sourceVar = linkSpec.sourceDataset.variable
        val targetVar = linkSpec.targetDataset.variable

        val sourceRestriction = linkSpec.sourceDataset.restriction
        val targetRestriction = linkSpec.targetDataset.restriction

        val sourcePaths = collectPaths(sourceVar)(linkSpec.condition.rootAggregation)
        val targetPaths = collectPaths(targetVar)(linkSpec.condition.rootAggregation)

        val sourceInstanceSpec = new InstanceSpecification(sourceVar, sourceRestriction, sourcePaths, config.prefixes)
        val targetInstanceSpec = new InstanceSpecification(targetVar, targetRestriction, targetPaths, config.prefixes)

        SourceTargetPair(sourceInstanceSpec, targetInstanceSpec)
    }

    private def collectPaths(variable : String)(operator : Operator) : Set[Path] = operator match
    {
        case aggregation : Aggregation => aggregation.operators.flatMap(collectPaths(variable)).toSet
        case comparison : Comparison => comparison.inputs.flatMap(collectPathsFromInput(variable)).toSet
    }

    private def collectPathsFromInput(variable : String)(param : Input) : Set[Path] = param match
    {
        case p : PathInput if p.path.variable == variable => Set(p.path)
        case p : TransformInput => p.inputs.flatMap(collectPathsFromInput(variable)).toSet
        case _ => Set()
    }
}
