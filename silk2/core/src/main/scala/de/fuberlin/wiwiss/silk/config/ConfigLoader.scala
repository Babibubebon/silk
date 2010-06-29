package de.fuberlin.wiwiss.silk.config

import javax.xml.validation.SchemaFactory
import de.fuberlin.wiwiss.silk.datasource.DataSource
import xml._
import java.io.{File}
import org.xml.sax.{SAXException}
import javax.xml.transform.stream.StreamSource
import de.fuberlin.wiwiss.silk.output.Output
import de.fuberlin.wiwiss.silk.linkspec._
import input.{Input, TransformInput, Transformer, PathInput}
import de.fuberlin.wiwiss.silk.instance.Path

object ConfigLoader
{
    def load(file : File) : Configuration =
    {
        validateXML(file)
        val xml = XML.loadFile(file)

        val prefixes = loadPrefixes(xml)
        val dataSources = loadDataSources(xml)
        val linkSpecifications = loadLinkSpecifications(xml, prefixes, dataSources)
        val outputs = loadOutputs(xml \ "Outputs" \ "Output")

        new Configuration(prefixes, dataSources, linkSpecifications, outputs)
    }

    private def validateXML(file : File) : Unit =
    {
        try {
            val factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema")
            val stream = ClassLoader.getSystemClassLoader.getResourceAsStream("de/fuberlin/wiwiss/silk/linkspec/LinkSpecificationLanguage.xsd")
            val schema = factory.newSchema(new StreamSource(stream))
            val validator = schema.newValidator()
            validator.validate(new StreamSource(file))
        } catch {
            case ex : SAXException => throw new ValidationException("Invalid XML. Details: " + ex.getMessage, ex)
        }

    }

    private def loadPrefixes(xml : Elem) : Map[String, String] =
    {
        xml \ "Prefixes" \ "Prefix" map(prefix => (prefix \ "@id" text, prefix \ "@namespace" text)) toMap
    }

    private def loadDataSources(xml : Elem) : Map[String, DataSource] =
    {
        (xml \ "DataSources" \ "DataSource")
            .map(ds => (ds \ "@id" text, DataSource(ds \ "@type" text, loadParams(ds)))).toMap
    }

    private def loadParams(element : Node) : Map[String, String] =
    {
        element \ "Param" map(p => (p \ "@name" text, p \ "@value" text)) toMap
    }

    private def loadLinkSpecifications(node : Node, prefixes : Map[String, String], dataSources : Map[String, DataSource]) : Map[String, LinkSpecification] =
    {
        node \ "Interlinks" \ "Interlink" map(p => (p \ "@id" text, loadLinkSpecification(p, prefixes, dataSources))) toMap
    }

    private def loadLinkSpecification(node : Node, prefixes : Map[String, String], dataSources : Map[String, DataSource]) : LinkSpecification =
    {
        //We cache all paths, so multiple equivalent paths will share the same path object
        var pathCache = collection.mutable.Map[String, Path]()

        new LinkSpecification(
            resolveQualifiedName(node \ "LinkType" text, prefixes),
            loadDatasetSpecification(node \ "SourceDataset", dataSources),
            loadDatasetSpecification(node \ "TargetDataset", dataSources),
            (node \ "Blocking").headOption.map(blockingNode => loadBlocking(blockingNode, pathCache)),
            new LinkCondition(loadAggregation(node \ "LinkCondition" \ "Aggregate" head, pathCache)),
            loadLinkFilter(node \ "Filter" head),
            loadOutputs(node \ "Outputs" \ "Output")
        )
    }

    private def loadDatasetSpecification(node : NodeSeq, dataSources : Map[String, DataSource]) : DatasetSpecification =
    {
        val datasourceName = node \ "@dataSource" text
        
        new DatasetSpecification(
            dataSources.get(datasourceName).getOrElse(throw new ValidationException("Datasource " + datasourceName + " not defined.")),
            node \ "@var" text,
            (node \ "RestrictTo").text.trim 
            )
    }

    private def loadOperators(nodes : Seq[Node], pathCache : collection.mutable.Map[String, Path]) : Traversable[Operator] =
    {
        nodes.collect
        {
            case node @ <Aggregate>{_*}</Aggregate> => loadAggregation(node, pathCache)
            case node @ <Compare>{_*}</Compare> => loadComparison(node, pathCache)
        }
    }

    private def loadAggregation(node : Node, pathCache : collection.mutable.Map[String, Path]) : Aggregation =
    {
        val weightStr = node \ "@weight" text

        val aggregator = Aggregator(node \ "@type" text, loadParams(node))

        new Aggregation(
            if(weightStr.isEmpty) 1 else weightStr.toInt,
            loadOperators(node.child, pathCache),
            aggregator
        )
    }

    private def loadComparison(node : Node, pathCache : collection.mutable.Map[String, Path]) : Comparison =
    {
        val weightStr = node \ "@weight" text
        val metric = Metric(node \ "@metric" text, loadParams(node))

        new Comparison(
            if(weightStr.isEmpty) 1 else weightStr.toInt,
            loadInputs(node.child, pathCache),
            metric
        )
    }

    private def loadBlocking(node : Node, pathCache : collection.mutable.Map[String, Path]) : Blocking =
    {
        new Blocking(
            loadInputs(node.child, pathCache).asInstanceOf[Traversable[PathInput]],
            BlockingFunction(node \ "@function" text, loadParams(node)),
            (node \ "@blocks").text.toInt,
            (node \ "@overlap").headOption.map(_.text.toDouble).getOrElse(0.0)
        )
    }

    private def loadInputs(nodes : Seq[Node], pathCache : collection.mutable.Map[String, Path]) : Seq[Input] =
    {
        nodes.collect {
            case p @ <Input/> =>
            {
                //Use a cached path if available
                val pathStr = p \ "@path" text
                val path = pathCache.get(pathStr) match
                {
                    case Some(path) => path
                    case None =>
                    {
                        val path = Path.parse(pathStr)
                        pathCache.update(pathStr, path)
                        path
                    }
                }
                new PathInput(path)
            }
            case p @ <TransformInput>{_*}</TransformInput> =>
            {
                val transformer = Transformer(p \ "@function" text, loadParams(p))
                new TransformInput(loadInputs(p.child, pathCache), transformer)
            }
        }
    }

    private def loadLinkFilter(node : Node) =
    {
        val limitStr = (node \ "@limit").text
        new LinkFilter((node \ "@threshold").text.toDouble, if(limitStr.isEmpty) None else Some(limitStr.toInt))
    }

    private def loadOutputs(nodes : NodeSeq) : Traversable[Output] =
    {
        nodes.map(loadOutput)
    }

    private def loadOutput(node : Node) : Output =
    {
        Output(node \ "@type" text, loadParams(node))
    }

    private def resolveQualifiedName(name : String, prefixes : Map[String, String]) =
    {
        if(name.startsWith("<") && name.endsWith(">"))
        {
            name.substring(1, name.length - 1)
        }
        else
        {
            name.split(":", 2) match
            {
                case Array(prefix, suffix) => prefixes.get(prefix) match
                {
                    case Some(resolvedPrefix) => resolvedPrefix + suffix
                    case None => throw new IllegalArgumentException("Unknown prefix: " + prefix)
                }
                case _ => throw new IllegalArgumentException("No prefix found in " + name)
            }
        }
    }
}
