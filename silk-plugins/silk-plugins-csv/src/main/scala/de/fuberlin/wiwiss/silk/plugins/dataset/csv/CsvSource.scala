package de.fuberlin.wiwiss.silk.plugins.dataset.csv

import java.io.{BufferedReader, InputStreamReader}
import java.net.URLEncoder
import java.util.logging.{Level, Logger}
import java.util.regex.Pattern

import de.fuberlin.wiwiss.silk.dataset.DataSource
import de.fuberlin.wiwiss.silk.entity._
import de.fuberlin.wiwiss.silk.runtime.resource.Resource
import de.fuberlin.wiwiss.silk.util.Uri

import scala.io.{Codec, Source}

class CsvSource(file: Resource,
                settings: CsvSettings,
                properties: String = "",
                prefix: String = "",
                uri: String = "",
                regexFilter: String = "",
                codec: Codec = Codec.UTF8,
                skipLinesBeginning: Int = 0,
                ignoreBadLines: Boolean = false ) extends DataSource {

  private val logger = Logger.getLogger(getClass.getName)

  private lazy val propertyList: Seq[String] = {
    val parser = new CsvParser(Seq.empty, settings)
    if (!properties.trim.isEmpty)
      parser.parseLine(properties)
    else {
      val source = getBufferedReaderForCsvFile()
      initBufferedReader(source)
      val firstLine = source.readLine()
      source.close()
      parser.parseLine(firstLine).map(s => URLEncoder.encode(s, "UTF8"))
    }
  }

  override def retrieveSparqlPaths(restriction: SparqlRestriction, depth: Int, limit: Option[Int]): Traversable[(Path, Double)] = {
    for (property <- propertyList) yield {
      (Path(restriction.variable, ForwardOperator(prefix + property) :: Nil), 1.0)
    }
  }

  override def retrieveSparqlEntities(entityDesc: EntityDescription, entities: Seq[String] = Seq.empty): Traversable[Entity] = {

    logger.log(Level.FINE, "Retrieving data from CSV.")

    // Retrieve the indices of the request paths
    val indices =
      for (path <- entityDesc.paths) yield {
        val property = path.operators.head.asInstanceOf[ForwardOperator].property.uri.stripPrefix(prefix)
        val propertyIndex = propertyList.indexOf(property)
        if (propertyIndex == -1)
          throw new Exception("Property " + path.toString + " not found in CSV")
        propertyIndex
      }

    // Return new Traversable that generates an entity for each line
    new Traversable[Entity] {
      def foreach[U](f: Entity => U) {

        val reader = getBufferedReaderForCsvFile
        val parser = new CsvParser(Seq.empty, settings) // Here we could only load the required indices as a performance improvement

        // Compile the line regex.
        val regex: Pattern = if (!regexFilter.isEmpty) regexFilter.r.pattern else null

        try {
          // Iterate through all lines of the source file. If a *regexFilter* has been set, then use it to filter
          // the rows.
          initBufferedReader(reader)
          var line = reader.readLine()
          var index = 0
          while(line != null) {
            if(!(properties.trim.isEmpty && 0 == index) && (regexFilter.isEmpty || regex.matcher(line).matches())) {
              logger.log(Level.FINER, s"Retrieving data from CSV [ line number :: ${index + 1} ].")

              //Split the line into values
              val allValues = parser.parseLine(line)
              if(allValues != null) {
                if(propertyList.size <= allValues.size) {

                  //Extract requested values
                  val values = indices.map(allValues(_))

                  // The default URI pattern is to use the prefix and the line number.
                  // However the user can specify a different URI pattern (in the *uri* property), which is then used to
                  // build the entity URI. An example of such pattern is 'urn:zyx:{id}' where *id* is a name of a property
                  // as defined in the *properties* field.
                  val entityURI = if (uri.isEmpty)
                    prefix + (index + 1)
                  else
                    "\\{([^\\}]+)\\}".r.replaceAllIn(uri, m => {
                      val propName = m.group(1)

                      assert(propertyList.contains(propName))
                      val value = allValues(propertyList.indexOf(propName))
                      URLEncoder.encode(value, "UTF-8")
                    })

                  //Build entity
                  if (entities.isEmpty || entities.contains(entityURI)) {
                    val entityValues = settings.arraySeparator match {
                      case None =>
                        values.map(v => if (v != null) Set(v) else Set.empty[String])
                      case Some(c) =>
                        values.map(v => if (v != null) v.split(c.toString, -1).toSet else Set.empty[String])
                    }

                    f(new Entity(
                      uri = entityURI,
                      values = entityValues,
                      desc = entityDesc
                    ))
                  }
                } else {
                  // Bad line
                  if(! ignoreBadLines) {
                    assert(propertyList.size <= allValues.size, s"Invalid line ${index + 1}: '$line' in resource '${file.name}' with ${allValues.size} elements. Expected number of elements ${propertyList.size}.")
                  }
                }
              }
            }
            index += 1
            line = reader.readLine()
          }
        } finally {
          reader.close()
        }
      }
    }
  }

  // Skip lines that are not part of the CSV file, headers may be included
  private def initBufferedReader(reader: BufferedReader): Unit = {
    for (i <- 1 to skipLinesBeginning)
      reader.readLine() // Skip line
  }

  private def getBufferedReaderForCsvFile(): BufferedReader = {
    val inputStream = file.load
    new BufferedReader(new InputStreamReader(inputStream, codec.decoder))
  }

  override def retrieveTypes(limit: Option[Int] = None): Traversable[(String, Double)] = {
    Seq((classUri, 1.0))
  }

  override def retrievePaths(t: Uri, depth: Int = 1, limit: Option[Int] = None): IndexedSeq[Path] = {
    if(classUri == t.uri) {
      val reader = getBufferedReaderForCsvFile
      val props = for (property <- propertyList) yield {
        Path(prefix + property)
      }
      props.toIndexedSeq
    } else {
      IndexedSeq.empty[Path]
    }
  }

  override def retrieve(entitySchema: EntitySchema, limitOpt: Option[Int] = None): Traversable[Entity] = {
    if(entitySchema.filter.operator.isDefined) {
      ??? // TODO: Implement Restriction handling!
    }
    val entities = retrieveSparqlEntities(EntityDescription(paths = entitySchema.paths))
    limitOpt match {
      case Some(limit) =>
        entities.take(limit)
      case None =>
        entities
    }
  }

  private def classUri = prefix + "CsvTable"
}
