package de.fuberlin.wiwiss.silk.plugins.transformer

import de.fuberlin.wiwiss.silk.linkspec.input.SimpleTransformer
import de.fuberlin.wiwiss.silk.util.plugin.Plugin

@Plugin(id = "stripPostfix", label = "Strip postfix", description = "Strips a postfix of a string.")
class StripPostfixTransformer(postfix: String) extends SimpleTransformer {
  override def evaluate(value: String): String = {
    value.stripSuffix(postfix)
  }
}