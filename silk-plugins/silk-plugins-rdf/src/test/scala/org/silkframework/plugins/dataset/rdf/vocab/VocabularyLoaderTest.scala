package org.silkframework.plugins.dataset.rdf.vocab

import com.hp.hpl.jena.query.DatasetFactory
import com.hp.hpl.jena.rdf.model.ModelFactory
import org.scalatest.{FlatSpec, ShouldMatchers}
import org.silkframework.plugins.dataset.rdf.endpoint.JenaDatasetEndpoint
import org.silkframework.rule.vocab.{GenericInfo, VocabularyClass, VocabularyProperty}

class VocabularyLoaderTest extends FlatSpec with ShouldMatchers {
  private val MOVIE = "Movie"
  private val PERSON = "Person"

  behavior of "VocabularyLoader"

  val graphUri = "urn:example"

  private val loader = load()

  val classes: Seq[VocabularyClass] = loader.retrieveClasses(graphUri).toSeq.sortBy(_.info.uri)
  val classMap: Map[String, VocabularyClass] = classes.map(c => (c.info.uri, c)).toMap

  it should "load classes" in {
    classes.size shouldBe 3
    classes(1) shouldBe VocabularyClass(GenericInfo(uri(MOVIE), Some(MOVIE), None), Seq())
    classes(2) shouldBe VocabularyClass(GenericInfo(uri(PERSON), Some(PERSON), Some("A Person")), Seq())
    classes.head shouldBe VocabularyClass(GenericInfo(uri("Employee"), Some("Angestellter"),Some("Angestellter einer Firma")), Seq(uri(PERSON)))
  }

  it should "load properties" in {
    val properties = loader.retrieveProperties(graphUri, classes).toSeq.sortBy(_.info.uri)
    properties.size shouldBe 2
    properties.head shouldBe
      VocabularyProperty(
        info = GenericInfo(uri("hasDate"), Some("release date"), None),
        domain = Some(classMap(uri(MOVIE))),
        range = None
      )
    properties(1) shouldBe
      VocabularyProperty(
        info = GenericInfo(uri("hasDirector"), Some("director"), Some("Director of a movie")),
        domain = Some(classMap(uri(MOVIE))),
        range = Some(classMap(uri(PERSON)))
      )
  }

  private def load(): VocabularyLoader = {
    // Load example into Jena model
    val stream = getClass.getClassLoader.getResourceAsStream("org/silkframework/plugins/dataset/rdf/vocab/vocabulary.ttl")
    val model = ModelFactory.createDefaultModel()
    model.read(stream, null, "TURTLE")

    // Create Jena dataset
    val dataset = DatasetFactory.createMem()
    dataset.addNamedModel(graphUri, model)

    // Create VocabularyLoader
    val endpoint = new JenaDatasetEndpoint(dataset)
    new VocabularyLoader(endpoint)
  }

  private def uri(localName: String): String = "https://silkframework.org/testOntology/" + localName

}
