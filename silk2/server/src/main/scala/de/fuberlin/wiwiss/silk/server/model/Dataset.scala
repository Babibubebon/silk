package de.fuberlin.wiwiss.silk.server.model

import de.fuberlin.wiwiss.silk.config.Configuration
import de.fuberlin.wiwiss.silk.output.Output
import de.fuberlin.wiwiss.silk.{Matcher, Loader}
import de.fuberlin.wiwiss.silk.impl.writer.MemoryWriter
import de.fuberlin.wiwiss.silk.datasource.DataSource
import de.fuberlin.wiwiss.silk.instance.{InstanceSpecification, MemoryInstanceCache}
import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification

/**
 * Holds the dataset of a link specification.
 */
class Dataset(val name : String, config : Configuration, linkSpec : LinkSpecification, writeUnmatchedInstances : Boolean)
{
    private val sourceCache = new MemoryInstanceCache()
    private val targetCache = new MemoryInstanceCache()
    new Loader(config, linkSpec).writeCaches(sourceCache, targetCache)

    private val (sourceInstanceSpec, targetInstanceSpec) = InstanceSpecification.retrieve(linkSpec)
    /**
     * Matches a set of instances with all instances in this dataset.
     */
    def apply(instanceSource : DataSource) : MatchResult =
    {
        val sourceLinks = generateSourceLinks(instanceSource)
        val targetLinks = generateTargetLinks(instanceSource)

        MatchResult(
            links = sourceLinks.links ++ targetLinks.links,
            linkType = linkSpec.linkType,
            unmatchedInstances = sourceLinks.unmatchedInstances ++ targetLinks.unmatchedInstances
        )
    }

    /**
     * Generates all links where the provided instances are link source.
     */
    private def generateSourceLinks(instanceSource : DataSource) =
    {
        val instanceCache = new MemoryInstanceCache()
        val writer = new MemoryWriter()
        val matcher = new Matcher(config.copy(outputs = Nil), linkSpec.copy(outputs = new Output(writer) :: Nil))

        val instances = instanceSource.retrieve(sourceInstanceSpec, config.prefixes).toList
        instanceCache.write(instances)
        if(instanceCache.instanceCount > 0)
        {
            matcher.execute(instanceCache, targetCache)
        }

        val matchedInstances = writer.links.map(_.sourceUri).toSet
        val unmatchedInstances = instances.filterNot(instance => matchedInstances.contains(instance.uri))

        if(writeUnmatchedInstances)
        {
            sourceCache.write(unmatchedInstances, linkSpec.blocking)
        }

        MatchResult(writer.links, linkSpec.linkType, unmatchedInstances.map(_.uri).toSet)
    }

    /**
     * Generates all links where the provided instances are link target.
     */
    private def generateTargetLinks(instanceSource : DataSource) =
    {
        val instanceCache = new MemoryInstanceCache()
        val writer = new MemoryWriter()
        val matcher = new Matcher(config.copy(outputs = Nil), linkSpec.copy(outputs = new Output(writer) :: Nil))

        val instances = instanceSource.retrieve(targetInstanceSpec, config.prefixes).toList
        instanceCache.write(instances)
        if(instanceCache.instanceCount > 0)
        {
            matcher.execute(sourceCache, instanceCache)
        }

        val matchedInstances = writer.links.map(_.targetUri).toSet
        val unmatchedInstances = instances.filterNot(instance => matchedInstances.contains(instance.uri))

        if(writeUnmatchedInstances)
        {
            targetCache.write(unmatchedInstances, linkSpec.blocking)
        }

        MatchResult(writer.links, linkSpec.linkType, unmatchedInstances.map(_.uri).toSet)
    }

    def sourceInstanceCount = sourceCache.instanceCount

    def targetInstanceCount = targetCache.instanceCount
}