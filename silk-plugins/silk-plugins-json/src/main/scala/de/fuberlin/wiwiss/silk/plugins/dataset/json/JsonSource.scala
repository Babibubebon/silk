package de.fuberlin.wiwiss.silk.plugins.dataset.json

import java.net.URLEncoder
import java.util.logging.{Level, Logger}
import de.fuberlin.wiwiss.silk.dataset.DataSource
import de.fuberlin.wiwiss.silk.entity._
import de.fuberlin.wiwiss.silk.runtime.resource.Resource
import play.api.libs.json._

/**
 * A data source that retrieves all entities from an JSON file.
 *
 * @param file JSON resource
 * @param basePath The path to the elements to be read, starting from the root element, e.g., '/Persons/Person'.
 *                 If left empty, all direct children of the root element will be read.
 * @param uriPattern A URI pattern, e.g., http://namespace.org/{ID}, where {path} may contain relative paths to elements
 */
class JsonSource(file: Resource, basePath: String, uriPattern: String) extends DataSource {

  private val logger = Logger.getLogger(getClass.getName)

  private val uriRegex = "\\{([^\\}]+)\\}".r

  override def retrievePaths(restriction: SparqlRestriction, depth: Int, limit: Option[Int]): Traversable[(Path, Double)] = {
    val json = Json.parse(file.loadAsString)
    val selectedElements = select(json, basePath.stripPrefix("/").split('/'))
    for (element <- selectedElements.headOption.toSeq; // At the moment, we only retrieve the path from the first found element
         path <- collectPaths(element)) yield {
      (Path(restriction.variable, path.toList), 1.0)
    }
  }

  override def retrieve(entityDesc: EntityDescription, entities: Seq[String] = Seq.empty): Traversable[Entity] = {
    logger.log(Level.FINE, "Retrieving data from JSON.")
    val json = Json.parse(file.loadAsString)
    val selectedElements = select(json, basePath.stripPrefix("/").split('/'))
    new Entities(selectedElements, entityDesc)

  }

  /**
   * Collects all paths from an json node
   * @param json The xml node to search paths in
   * @param path Path prefix to be prepended to all found paths
   * @return Sequence of all found paths
   */
  def collectPaths(json: JsValue, path: Seq[PathOperator] = Nil): Seq[Seq[PathOperator]] = {
    json match {
      case obj: JsObject =>
        obj.keys.toSeq.flatMap(key => collectPaths(obj.value(key), path :+ ForwardOperator(key)))
      case array: JsArray if array.value.nonEmpty =>
        collectPaths(array.value.head, path)
      case _ => if(path.nonEmpty) Seq(path) else Seq()
    }
  }

  /**
   * Selects all elements in a JSON node matching a path.
   */
  private def select(json: JsValue, path: Seq[String]): Seq[JsValue] = {
    json match {
      case obj: JsObject if path.nonEmpty =>
        obj.value.get(path.head).toSeq.flatMap(value => select(value, path.tail))
      case array: JsArray if array.value.nonEmpty =>
        array.value.flatMap(value => select(value, path))
      case _ =>
        Seq(json)
    }
  }

  private class Entities(elements: Seq[JsValue], entityDesc: EntityDescription) extends Traversable[Entity] {
    def foreach[U](f: Entity => U) {
      // Enumerate entities
      for ((node, index) <- elements.zipWithIndex) {
        val uri =
          if (uriPattern.isEmpty)
            index.toString
          else
            uriRegex.replaceAllIn(uriPattern, m => {
              val path = Path.parse(m.group(1)).operators
              val string = evaluate(node, path).mkString
              URLEncoder.encode(string, "UTF8")
            })

        val values = for (path <- entityDesc.paths) yield evaluateSilkPath(node, path)
        f(new Entity(uri, values, entityDesc))
      }
    }

    private def evaluateSilkPath(json: JsValue, path: Path) = {
      evaluate(json, path.operators).toSet // TODO toSet can be removed as soon as the Entity class uses Seq instead of Set for storing values
    }

    private def evaluate(json: JsValue, path: Seq[PathOperator]): Seq[String] = {
      path match {
        case ForwardOperator(prop) :: tail =>
          json match {
            case obj: JsObject =>
              obj.value.get(prop.uri).toSeq.flatMap(value => evaluate(value, tail))
            case array: JsArray if array.value.nonEmpty =>
              array.value.flatMap(value => evaluate(value, path))
            case _ =>
              Nil
          }
        case (p @ PropertyFilter(prop, op, value)) :: tail =>
          json match {
            case obj: JsObject if p.evaluate(toString(obj.value(prop.uri))) =>
              evaluate(obj, tail)
            case _ =>
              Nil
          }
        case Nil =>
          Seq(toString(json))
        case _ =>
          throw new IllegalArgumentException("For JSON only forward and filter operators are supported in paths.")
      }
    }

    private def toString(json: JsValue): String = {
      json match {
        case JsBoolean(value) => value.toString
        case JsNumber(value) => value.toString
        case JsString(value) => value.toString
        case _ => json.toString()
      }
    }
  }

}
