package de.fuberlin.wiwiss.silk.learning.generation

import de.fuberlin.wiwiss.silk.evaluation.ReferenceInstances
import de.fuberlin.wiwiss.silk.instance.{Instance, Path}
import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.learning.individual.StrategyNode
import de.fuberlin.wiwiss.silk.linkspec.similarity.DistanceMeasure

/**
 * Analyses the reference instances and generates pairs of paths.
 */
object PathPairGenerator {
  def apply(instances: ReferenceInstances): Traversable[ComparisonGenerator] = {
    //Get all paths except sameAs paths
    val paths = PathsRetriever(instances)
    //Remove paths which hold the same values (e.g. rdfs:label and drugbank:drugName)
    val distinctPaths = DuplicateRemover(paths, instances)
    //Return all path pairs
    val pathPairs = PairGenerator(distinctPaths, instances)

    pathPairs.flatMap(createGenerators)
  }

  private def createGenerators(pathPair: SourceTargetPair[Path]) = {
    new ComparisonGenerator(InputGenerator.fromPathPair(pathPair), StrategyNode("levenshteinDistance", Nil, DistanceMeasure), 5.0) ::
    new ComparisonGenerator(InputGenerator.fromPathPair(pathPair), StrategyNode("jaccard", Nil, DistanceMeasure), 1.0) :: Nil
  }

  /**
   * Retrieves all paths except sameAs paths
   */
  private object PathsRetriever {
    def apply(instances: ReferenceInstances) = {
      val allPaths = instances.positive.values.head.map(_.spec.paths)
      allPaths.map(_.filterNot(_.toString.contains("sameAs")))
    }
  }

  /**
   * Removes paths which hold the same values (e.g. rdfs:label and drugbank:drugName)
   */
  private object DuplicateRemover {
    def apply(paths: SourceTargetPair[Traversable[Path]], instances: ReferenceInstances) = {
      SourceTargetPair(
        source = removeDuplicatePaths(instances.positive.values.map(_.source) ++ instances.negative.values.map(_.source), paths.source),
        target = removeDuplicatePaths(instances.positive.values.map(_.target) ++ instances.negative.values.map(_.target), paths.target)
      )
    }

    private def removeDuplicatePaths(instances: Traversable[Instance], paths: Traversable[Path]): Traversable[Path] = {
      for(path :: tail <- paths.toList.tails.toTraversable
          if !tail.exists(p => pathsMatch(instances, SourceTargetPair(path, p)))) yield path
    }

    private def pathsMatch(instances: Traversable[Instance], pathPair: SourceTargetPair[Path]): Boolean = {
      instances.forall(pathsMatch(_, pathPair))
    }

    private def pathsMatch(instance: Instance, pathPair: SourceTargetPair[Path]): Boolean = {
      val values = pathPair.map(instance.evaluate)

      val valuePairs = for(v1 <- values.source; v2 <- values.target) yield SourceTargetPair(v1, v2)

      valuePairs.exists(p => p.source.toLowerCase == p.target.toLowerCase)
    }
  }

  /**
   * Generates all pair of paths with overlapping values.
   */
  private object PairGenerator {
    def apply(paths: SourceTargetPair[Traversable[Path]], instances: ReferenceInstances) = {
      val pathPairs = for(sourcePath <- paths.source; targetPath <- paths.target) yield SourceTargetPair(sourcePath, targetPath)

      pathPairs.filter(pathValuesMatch(instances, _))
    }

    private def pathValuesMatch(instances: ReferenceInstances, pathPair: SourceTargetPair[Path]): Boolean = {
      val positiveMatches = instances.positive.values.filter(i => matches(i, pathPair)).size
      val negativeMatches = instances.negative.values.filter(i => matches(i, pathPair)).size

      positiveMatches > negativeMatches
    }

    private def matches(instancePair: SourceTargetPair[Instance], pathPair: SourceTargetPair[Path]): Boolean = {
      val sourceValues = instancePair.source.evaluate(pathPair.source)
      val targetValues = instancePair.target.evaluate(pathPair.target)

      val valuePairs = for(sourceValue <- sourceValues; targetValue <- targetValues) yield valuesMatch(sourceValue, targetValue)

      valuePairs.exists(identity)
    }

    private def valuesMatch(value1: String, value2: String): Boolean = {
      val tokens1 = value1.toLowerCase.split("\\s")
      val tokens2 = value2.toLowerCase.split("\\s")

      val valuePairs = for(t1 <- tokens1; t2 <- tokens2) yield t1 == t2

      valuePairs.exists(identity)
    }
  }
}