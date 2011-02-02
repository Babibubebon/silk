package de.fuberlin.wiwiss.silk.workbench.project

import java.util.logging.Logger
import java.io._
import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.linkspec._
import de.fuberlin.wiwiss.silk.util.Task
import de.fuberlin.wiwiss.silk.evaluation.Alignment
import de.fuberlin.wiwiss.silk.workbench.instancespec.{RelevantPropertiesCollector}
import de.fuberlin.wiwiss.silk.workbench.Constants
import de.fuberlin.wiwiss.silk.util.sparql.{InstanceRetriever, RemoteSparqlEndpoint, SparqlEndpoint}
import de.fuberlin.wiwiss.silk.instance.{Instance, InstanceSpecification}
import de.fuberlin.wiwiss.silk.config.{Prefixes, Configuration}

case class Project(desc : SourceTargetPair[Description],
                   config : Configuration,//TODO remove and hold all variables in desc
                   linkSpec : LinkSpecification,
                   alignment : Alignment,
                   cache : Cache)
{
  //TODO validate

  private val logger = Logger.getLogger(getClass.getName)

  val cacheLoader : Task[Unit] = new CacheLoader()

  //TODO catch exceptions which occur during filling the cache
  cacheLoader.runInBackground()

  private class CacheLoader() extends Task[Unit]
  {
    private val sampleCount = 10000

    private val positiveSamples = alignment.positiveLinks.take(sampleCount).toList

    private val negativeSamples = alignment.negativeLinks.take(sampleCount).toList

    taskName = "CacheLoaderTask"

    override protected def execute()
    {
      val sourceEndpoint = new RemoteSparqlEndpoint(desc.source.endpointUri, config.prefixes)
      val targetEndpoint = new RemoteSparqlEndpoint(desc.target.endpointUri, config.prefixes)

      if(cache.instanceSpecs == null)
      {
        updateStatus("Retrieving frequent property paths", 0.0)
        val sourcePaths = RelevantPropertiesCollector(sourceEndpoint, desc.source.restriction).map(_._1).toSeq
        val targetPaths = RelevantPropertiesCollector(targetEndpoint, desc.target.restriction).map(_._1).toSeq

        val sourceInstanceSpec = new InstanceSpecification(Constants.SourceVariable, desc.source.restriction, sourcePaths, config.prefixes)
        val targetInstanceSpec = new InstanceSpecification(Constants.TargetVariable, desc.target.restriction, targetPaths, config.prefixes)

        cache.instanceSpecs = new SourceTargetPair(sourceInstanceSpec, targetInstanceSpec)
      }

      if(!positiveSamples.isEmpty && (cache.positiveInstances == null || cache.negativeInstances == null))
      {
        updateStatus(0.2)

        //Create instance loading tasks
        val positiveSourceInstancesTask = new LoadingInstancesTask(sourceEndpoint, cache.instanceSpecs.source, positiveSamples.map(_.sourceUri))
        val positiveTargetInstancesTask = new LoadingInstancesTask(targetEndpoint, cache.instanceSpecs.target, positiveSamples.map(_.targetUri))

        val negativeSourceInstancesTask =  new LoadingInstancesTask(sourceEndpoint, cache.instanceSpecs.source, negativeSamples.map(_.sourceUri))
        val negativeTargetInstancesTask =  new LoadingInstancesTask(targetEndpoint, cache.instanceSpecs.target, negativeSamples.map(_.targetUri))

        //Load instances
        val positiveSourceInstances = executeSubTask(positiveSourceInstancesTask, 0.4)
        val positiveTargetInstances = executeSubTask(positiveTargetInstancesTask, 0.6)

        val negativeSourceInstances = executeSubTask(negativeSourceInstancesTask, 0.8)
        val negativeTargetInstances = executeSubTask(negativeTargetInstancesTask, 1.0)

        //Fill the cache with the loaded instances
        cache.positiveInstances = (positiveSourceInstances zip positiveTargetInstances).map(SourceTargetPair.fromPair)
        cache.negativeInstances = (negativeSourceInstances zip negativeTargetInstances).map(SourceTargetPair.fromPair)
      }
    }
  }

  /**
   * Task which loads a list of instances from an endpoint.
   */
  private class LoadingInstancesTask(endpoint : SparqlEndpoint, instanceSpec : InstanceSpecification, instanceUrls : Seq[String]) extends Task[List[Instance]]
  {
    override def execute() =
    {
      val instanceTraversable = InstanceRetriever(endpoint).retrieve(instanceSpec, instanceUrls)

      var instanceList : List[Instance] = Nil
      var instanceListSize = 0
      val instanceCount = instanceUrls.size

      updateStatus("Retrieving instances", 0.0)
      for(instance <- instanceTraversable)
      {
        instanceList ::= instance
        instanceListSize += 1
        updateStatus(instanceListSize.toDouble / instanceCount)
      }

      instanceList.reverse
    }
  }
}

object Project
{
  private val logger = Logger.getLogger(getClass.getName)

  @volatile private var project : Option[Project] = None

  def isOpen = project.isDefined

  def apply() =
  {
    project match
    {
      case Some(p) => p
      case None => throw new IllegalStateException("No project is open.")
    }
  }

  def create(description : SourceTargetPair[Description], prefixes : Prefixes)
  {
    project = Some(ProjectCreator.create(description, prefixes))
  }

  def open(inputStream : InputStream)
  {
    project = Some(ProjectReader.read(inputStream))
  }

  def save(ouputStream : OutputStream)
  {
    ProjectWriter.write(Project(), ouputStream)
  }

  def close()
  {
    project = None
  }

  def updateLinkSpec(linkSpec : LinkSpecification)
  {
    val updatedConfig = project.get.config.copy(linkSpecs = linkSpec :: Nil)
    project = Some(project.get.copy(config = updatedConfig, linkSpec = linkSpec, cache = new Cache()))
  }

  def updateAlignment(alignment : Alignment)
  {
    project = Some(project.get.copy(alignment = alignment, cache = new Cache()))
  }
}
