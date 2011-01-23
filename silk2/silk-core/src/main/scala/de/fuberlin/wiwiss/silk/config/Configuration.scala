package de.fuberlin.wiwiss.silk.config

import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification
import de.fuberlin.wiwiss.silk.output.Output
import de.fuberlin.wiwiss.silk.datasource.{DataSource, Source}
import xml.{Node, Elem}
import de.fuberlin.wiwiss.silk.util.ValidatingXMLReader

/**
 * A Silk linking configuration.
 * Specifies how multiple sources are interlinked by defining a link specification for each type of instance to be interlinked.
 *
 * @param prefixes The prefixes which are used throughout the configuration to shorten URIs
 * @param sources The sources which should be interlinked
 * @param linkSpecs The Silk link specifications
 * @param outputs The global output
 */
case class Configuration(prefixes : Map[String, String],
                         sources : Traversable[Source],
                         linkSpecs : Traversable[LinkSpecification],
                         outputs : Traversable[Output] = Traversable.empty)
{
    private val sourceMap = sources.map(s => (s.id, s)).toMap
    private val linkSpecMap = linkSpecs.map(s => (s.id, s)).toMap

    def source(id : String) = sourceMap(id)

    def linkSpec(id : String) = linkSpecMap(id)
}

object Configuration
{
  private val schemaLocation = "de/fuberlin/wiwiss/silk/linkspec/LinkSpecificationLanguage.xsd"

  def load =
  {
    new ValidatingXMLReader(fromXML, schemaLocation)
  }

  def fromXML(node : Node) =
  {
    val prefixes = readPrefixes(node)
    val sources = readDataSources(node)
    val linkSpecifications = readLinkSpecifications(node, prefixes)
    val outputs = (node \ "Outputs" \ "Output").map(Output.fromXML)

    Configuration(prefixes, sources, linkSpecifications, outputs)
  }

  private def readPrefixes(xml : Node) : Map[String, String] =
  {
    xml \ "Prefixes" \ "Prefix" map(prefix => (prefix \ "@id" text, prefix \ "@namespace" text)) toMap
  }

  private def readDataSources(xml : Node) : Traversable[Source] =
  {
    (xml \ "DataSources" \ "DataSource").map(ds => new Source(ds \ "@id" text, DataSource(ds \ "@type" text, readParams(ds))))
  }

  private def readParams(element : Node) : Map[String, String] =
  {
    element \ "Param" map(p => (p \ "@name" text, p \ "@value" text)) toMap
  }

  private def readLinkSpecifications(node : Node, prefixes : Map[String, String]) : Traversable[LinkSpecification] =
  {
    (node \ "Interlinks" \ "Interlink").map(p => LinkSpecification.fromXML(p, prefixes))
  }
}
