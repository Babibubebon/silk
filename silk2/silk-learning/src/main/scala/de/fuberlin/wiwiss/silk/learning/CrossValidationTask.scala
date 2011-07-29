package de.fuberlin.wiwiss.silk.learning

import de.fuberlin.wiwiss.silk.util.task.Task
import de.fuberlin.wiwiss.silk.evaluation.ReferenceInstances
import java.util.logging.Level

/**
 * Performs multiple cross validation runs and outputs the statistics.
 */
class CrossValidationTask(instances : ReferenceInstances) extends Task[Unit] {
  /** The number of cross validation runs. */
  private val numRuns = 1

  /** The number of splits used for cross-validation. */
  private val numFolds = 4

  /** Don't log progress. */
  logLevel = Level.FINE

  /**
   * Executes all cross validation runs.
   */
  override def execute() {
    for(run <- 1 to numRuns) yield {
      val statistics = crossValidation()
      println(StatisticsLatexFormatter(statistics))
    }
  }

  /**
   * Executes one cross validation run.
   */
  private def crossValidation(): Iterable[LearningStatistics] = {
    val splits = splitReferenceInstances()

    for((split, index) <- splits.zipWithIndex) yield {
      val learningTask = new LearningTask(split.trainingSet, split.validationSet)
      executeSubTask(learningTask, index.toDouble / splits.size)
      learningTask.statistics
    }
  }

  /**
   * Splits the reference instances..
   */
  private def splitReferenceInstances(): IndexedSeq[LearningSet] = {
    //Get the positive and negative reference instances
    val posInstances = instances.positive.values
    val negInstances = instances.negative.values

    //Split the reference instances into numFolds samples
    val posSamples = posInstances.grouped((posInstances.size.toDouble / numFolds).ceil.toInt).toStream
    val negSamples = negInstances.grouped((negInstances.size.toDouble / numFolds).ceil.toInt).toStream

    //Generate numFold splits
    val posSplits = (posSamples ++ posSamples).sliding(posSamples.size).take(posSamples.size)
    val negSplits = (negSamples ++ negSamples).sliding(negSamples.size).take(negSamples.size)

    //Generate a learning set from each split
    val splits =
      for((p, n) <- posSplits zip negSplits) yield {
        LearningSet(
          trainingSet = ReferenceInstances.fromInstances(p.tail.flatten, n.tail.flatten),
          validationSet = ReferenceInstances.fromInstances(p.head, n.head)
        )
      }

    splits.toIndexedSeq
  }

  /**
   * A learning set consisting of a training set and a validation set.
   */
  private case class LearningSet(trainingSet: ReferenceInstances, validationSet: ReferenceInstances)
}