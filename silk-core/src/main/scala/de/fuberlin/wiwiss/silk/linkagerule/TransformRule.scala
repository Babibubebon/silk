package de.fuberlin.wiwiss.silk.linkagerule

import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.entity.{Entity, Path}
import de.fuberlin.wiwiss.silk.linkagerule.input.{Input, PathInput, TransformInput}
import de.fuberlin.wiwiss.silk.plugins.transformer.combine.ConcatTransformer
import de.fuberlin.wiwiss.silk.plugins.transformer.value.ConstantTransformer
import de.fuberlin.wiwiss.silk.runtime.resource.ResourceLoader
import de.fuberlin.wiwiss.silk.util._

import scala.xml.Node

/**
 * A transform rule.
 */
sealed trait TransformRule {

  def name: Identifier

  def operator: Option[Input]

  def target: Option[Uri]

  /**
   * Generates the transformed values.
   *
   * @param entity The source entity.
   *
   * @return The transformed values.
   */
  def apply(entity: Entity): Set[String] = {
    operator match {
      case Some(op) => op(DPair.fill(entity))
      case None => Set.empty
    }
  }

  /**
   * Collects all paths in this rule.
   */
  def paths: Set[Path] = {
    def collectPaths(param: Input): Set[Path] = param match {
      case p: PathInput if p.path.operators.isEmpty => Set()
      case p: PathInput => Set(p.path)
      case p: TransformInput => p.inputs.flatMap(collectPaths).toSet
    }

    operator match {
      case Some(op) => collectPaths(op)
      case None => Set[Path]()
    }
  }

  /**
   * Serializes this transform rule as XML.
   */
  def toXML(implicit prefixes: Prefixes = Prefixes.empty) = {
    <TransformRule name={name} targetProperty={target.map(_.uri).getOrElse("")}>
      {operator.toList.map(_.toXML)}
    </TransformRule>
  }
}

case class DirectMapping(name: Identifier = "transformation", sourcePath: Path = Path("a", Nil), targetProperty: Uri = "http://www.w3.org/2000/01/rdf-schema#label") extends TransformRule {

  override val operator = Some(PathInput(path = sourcePath))

  override val target = Some(targetProperty)
}

case class UriMapping(name: Identifier = "transformation", pattern: String = "http://example.org/{ID}") extends TransformRule {

  override val operator = {
    val inputs =
      for ((str, i) <- pattern.split("[\\{\\}]").toList.zipWithIndex) yield {
        if (i % 2 == 0)
          TransformInput(transformer = ConstantTransformer(str))
        else
          PathInput(path = Path.parse(str))
      }
    Some(TransformInput(transformer = ConcatTransformer(""), inputs = inputs))
  }

  override val target = None
}

case class TypeMapping(name: Identifier, typeUri: Uri) extends TransformRule {

  override val operator = Some(TransformInput(transformer = ConstantTransformer(typeUri.uri)))

  override val target = Some(Uri("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"))
}

case class ComplexMapping(name: Identifier, operator: Option[Input] = None, target: Option[Uri] = None) extends TransformRule

/**
 * Creates new transform rules.
 */
object TransformRule {

  def load(resourceLoader: ResourceLoader)(implicit prefixes: Prefixes) = {
    new ValidatingXMLReader(node => fromXML(node, resourceLoader)(prefixes), "de/fuberlin/wiwiss/silk/LinkSpecificationLanguage.xsd")
  }

  /**
   * Reads a transform rule from xml.
   */
  def fromXML(node: Node, resourceLoader: ResourceLoader)(implicit prefixes: Prefixes) = {
    val target = (node \ "@targetProperty").text
    val complex =
      ComplexMapping(
        name = (node \ "@name").text,
        operator = Input.fromXML(node.child, resourceLoader).headOption,
        target = if(target.isEmpty) None else Some(prefixes.resolve(target))
      )
    simplify(complex)
  }

  /**
   * Tries to express a complex mapping as a basic mapping, such as a direct mapping.
   */
  def simplify(complexMapping: ComplexMapping): TransformRule = complexMapping match {
    // Direct Mapping
    case ComplexMapping(id, Some(PathInput(_, path)), Some(target)) =>
      DirectMapping(id, path, target)
    // URI Mapping
//    case ComplexMapping(id, Some(TransformInput(_, ConcatTransformer(""), inputs)), None) if isPattern(inputs) =>
//      UriMapping(id, buildPattern(inputs))
    // Type Mapping
//    case ComplexMapping(id, Some(TransformInput(_, ConstantTransformer(typeUri), Nil)), Some(Uri("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"))) =>
//      TypeMapping(id, typeUri)
    // Complex Mapping
    case _ => complexMapping
  }

  private def isPattern(inputs: Seq[Input]) = {
    inputs.forall{
      case PathInput(id, path) => true
      case TransformInput(id, ConstantTransformer(constant), Nil) => true
      case _ => false
    }
  }

  private def buildPattern(inputs: Seq[Input]) = {
    inputs.map {
      case PathInput(id, path) => "{" + path.serializeSimplified() + "}"
      case TransformInput(id, ConstantTransformer(constant), Nil) => constant
    }.mkString("")
  }
}
