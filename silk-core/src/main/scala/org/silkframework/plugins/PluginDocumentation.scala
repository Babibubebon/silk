package org.silkframework.plugins

import java.io.{OutputStreamWriter, FileOutputStream}
import org.silkframework.dataset.{DatasetPlugin, Dataset}
import org.silkframework.rule.input.Transformer
import org.silkframework.rule.similarity.{Aggregator, Aggregation, DistanceMeasure}
import org.silkframework.runtime.plugin._
import org.silkframework.util.Table

import scala.reflect.ClassTag

/**
 * Generates markdown documentation for all registered plugins.
 */
object PluginDocumentation {

  // Currently we add category descriptions manually here
  val categoryDescriptions: Map[String, String] =
    Map(
      "Characterbased" -> "Character-based distance measures compare strings on the character level. They are well suited for\nhandling typographical errors.",
      "Tokenbased" -> "While character-based distance measures work well for typographical errors, there are a number of tasks where token-base distance measures are better suited:\n- Strings where parts are reordered e.g. &ldquo;John Doe&rdquo; and &ldquo;Doe, John&rdquo;\n- Texts consisting of multiple words"
    )

  def apply(): String = {
    implicit val sb = new StringBuilder

    sb ++= "# Plugin Reference\n\n"

    printPlugins[DatasetPlugin](
      title = "Dataset Plugins",
      description = "The following dataset plugins are available:"
    )

    printPlugins[DistanceMeasure](
      title = "Similarity Measures",
      description = "The following similarity measures are available:"
    )

    printPlugins[Transformer](
      title = "Transformations",
      description = "The following transform and normalization functions are available:"
    )

    printPlugins[Aggregator](
      title = "Aggregations",
      description = "The following aggregation functions are available:"
    )

    sb.toString
  }

  def printPlugins[T: ClassTag](title: String, description: String)(implicit sb: StringBuilder) = {
    sb ++= "## " + title + "\n\n"
    sb ++= description + "\n\n"
    val categories = PluginRegistry.availablePlugins[T].flatMap(_.categories).filter(_ != "Recommended").distinct.sorted
    for(category <- categories) {
      if(categories.size > 1)
        sb ++= "### " + category + "\n\n"
      for(categoryDescription <- categoryDescriptions.get(category)) {
        sb ++= categoryDescription + "\n\n"
      }
      sb ++= pluginTable[T](title, category).toMarkdown
      sb ++= "\n"
    }
  }

  def pluginTable[T: ClassTag](title: String, category: String) = {
    val plugins = PluginRegistry.availablePlugins[T].filter(_.categories.contains(category)).sortBy(_.id.toString)
    Table(
      name = title,
      header = Seq("Function and parameters", "Name", "Description"),
      rows = plugins.map(formatFunction),
      values = for(plugin <- plugins) yield Seq(plugin.label, plugin.description)
    )
  }

  def formatFunction(plugin: PluginDescription[_]): String = {
    plugin.id.toString + plugin.parameters.map(formatParameter).mkString("(", ", ", ")")
  }

  def formatParameter(parameter: Parameter): String = {
    val signature = parameter.name + ": " + parameter.dataType.toString
    parameter.defaultValue match {
      case Some(default) => s"[$signature = '$default']"
      case None => signature
    }
  }
}
