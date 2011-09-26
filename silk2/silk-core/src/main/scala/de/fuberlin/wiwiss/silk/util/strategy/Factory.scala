package de.fuberlin.wiwiss.silk.util.strategy

import org.clapper.classutil.ClassFinder
import java.io.File
import java.net.{URL, URLClassLoader}

/**
 * An abstract Factory.
 */
class Factory[T <: Strategy : Manifest] extends ((String, Map[String, String]) => T) {
  private var strategies = Map[String, StrategyDescription[T]]()

  override def apply(id: String, params: Map[String, String] = Map.empty): T = {
    val strategy = {
      strategies.get(id) match {
        case Some(s) => s(params)
        case None => throw new NoSuchElementException("No strategy called '" + id + "' found.")
      }
    }

    strategy
  }

  def unapply(t: T): Option[(String, Map[String, String])] = {
    Some(t.id, t.parameters)
  }

  def strategy(id: String) = strategies(id)

  def availableStrategies: Traversable[StrategyDescription[T]] = strategies.values

  def register(implementationClass: Class[_ <: T]) {
    val strategyDefinition = StrategyDescription(implementationClass)

    strategies += ((strategyDefinition.id, strategyDefinition))
  }

   def registerClasspath() {
     val classFinder = ClassFinder()
     val classes = classFinder.getClasses()

     val pluginClassNames = ClassFinder.concreteSubclasses(manifest[T].erasure.getName, classes).map(_.name)
     val pluginClasses = pluginClassNames.map(Class.forName)

     for(pluginClass <- pluginClasses)
       register(pluginClass.asInstanceOf[Class[T]])
   }

   def registerJars(jarDir: String) {
    val jarFiles = Option(new File(jarDir).listFiles())
                   .getOrElse(throw new Exception("Directory " + jarDir + " does not exist"))
                   .filter(_.getName.endsWith(".jar"))

    val pluginInterface = ClassFinder().getClasses().find(_.name == manifest[T].erasure.getName).get
    val classes = ClassFinder(jarFiles).getClasses ++ Iterator(pluginInterface)
    val pluginClassNames = ClassFinder.concreteSubclasses(manifest[T].erasure.getName, classes).map(_.name)

    val classLoader = URLClassLoader.newInstance(jarFiles.map(file => new URL("jar:file:" + file.getName + "!/")))
    val pluginClasses = pluginClassNames.map(classLoader.loadClass)

    for(pluginClass <- pluginClasses)
      register(pluginClass.asInstanceOf[Class[T]])
   }
}
