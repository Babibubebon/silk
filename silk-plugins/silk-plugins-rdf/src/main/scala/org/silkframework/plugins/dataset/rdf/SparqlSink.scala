package org.silkframework.plugins.dataset.rdf

import java.util.logging.Logger

import org.silkframework.dataset.{LinkSink, EntitySink, DataSink}
import org.silkframework.dataset.rdf.SparqlEndpoint
import org.silkframework.entity.Link
import org.silkframework.util.StringUtils.DoubleLiteral

/**
 * A sink for writing to SPARQL/Update endpoints.
 */
class SparqlSink(params: SparqlParams, endpoint: SparqlEndpoint) extends EntitySink with LinkSink {

  private val log = Logger.getLogger(classOf[SparqlSink].getName)

  /**Maximum number of statements per request. */
  private val StatementsPerRequest = 200

  private val body: StringBuilder = new StringBuilder

  private var statements = 0

  private var properties = Seq[String]()

  override def open(properties: Seq[String]) {
    this.properties = properties
  }

  override def init() = {}

  override def writeLink(link: Link, predicateUri: String) {
    if(body.isEmpty) {
      beginSparul(true)
    } else if (statements + 1 > StatementsPerRequest) {
      endSparql()
      beginSparul(false)
    }

    body.append("<" + link.source + "> <" + predicateUri + "> <" + link.target + "> .\n")
    statements += 1
  }

  override def writeEntity(subject: String, values: Seq[Seq[String]]) {
    if(body.isEmpty) {
      beginSparul(true)
    } else if (statements + 1 > StatementsPerRequest) {
      endSparql()
      beginSparul(false)
    }

    for((property, valueSet) <- properties zip values; value <- valueSet) {
      writeStatement(subject, property, value)
    }
  }

  override def close() {
    if(body.nonEmpty) {
      endSparql()
    }
  }

  private def writeStatement(subject: String, property: String, value: String): Unit = {
    value match {
      // Check if value is an URI
      case v if value.startsWith("http:") || value.startsWith("https:") =>
        body.append("<" + subject + "> <" + property + "> <" + v + "> .\n")
      // Check if value is a number
      case DoubleLiteral(d) =>
        body.append("<" + subject + "> <" + property + "> \"" + d + "\"^^<http://www.w3.org/2001/XMLSchema#double> .\n")
      // Write string values
      case _ =>
        val escapedValue = value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
        body.append("<" + subject + "> <" + property + "> \"" + escapedValue + "\" .\n")
    }

    statements += 1
  }

  /**
   * Begins a new SPARQL/Update request.
   *
   * @param newGraph Create a new (empty) graph?
   */
  private def beginSparul(newGraph: Boolean) {
    body.clear()
    params.graph match {
      case None =>
        body.append("INSERT DATA { ")
      case Some(graph) =>
        //if (newGraph) {
        //  body.append("CREATE SILENT GRAPH {" + params.graph + "}")
        //}
        body.append("INSERT DATA { GRAPH <" + graph + "> { ")
    }
    statements = 0
  }

  /**
   * Ends the current SPARQL/Update request.
   */
  private def endSparql() {
    params.graph match {
      case None => body.append("}")
      case Some(g) => body.append("} }")
    }
    val query = body.toString()
    body.clear()
    if(statements > 0) { // Else this would throw an exception, because of invalid syntax
      endpoint.update(query)
    }
  }
}
