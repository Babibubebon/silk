//package de.fuberlin.wiwiss.silk.workbench.lift.comet
//
//import net.liftweb.util.Helpers
//import net.liftweb.http.{SHtml, CometActor}
//import de.fuberlin.wiwiss.silk.util.XMLUtils._
//import net.liftweb.http.js.JsCmds.SetHtml
//import collection.mutable.{Publisher, Subscriber}
//import de.fuberlin.wiwiss.silk.workbench.learning.{Individual, LearningServer, PopulationUpdated}
//import de.fuberlin.wiwiss.silk.linkspec.condition.{Aggregation, LinkCondition}
//import de.fuberlin.wiwiss.silk.workbench.workspace.User
//import xml.{NodeBuffer, Text, NodeSeq}
//
//class Population extends CometActor with Subscriber[PopulationUpdated, Publisher[PopulationUpdated]]
//{
//  private var individuals : Seq[Individual] = Seq.empty
//
//  private var individualCount = 0
//
//  LearningServer.subscribe(this)
//
//  private val listId = "individuals"
//
//  private val individualId = "individual"
//
//  override def notify(pub : Publisher[PopulationUpdated], event : PopulationUpdated)
//  {
//    val sortedIndividuals = LearningServer.population.individuals.toSeq.sortBy(-_.fitness)
//    individuals = sortedIndividuals
//    individualCount = sortedIndividuals.size
//    partialUpdate(SetHtml(listId, displayList))
//  }
//
//  override def render =
//  {
//    bind("chat", defaultXml,
//      "list" -> <div id={listId} style="float: left; height: 600px; min-width: 100; overflow: auto;">{displayList}</div>,
//      "individual" -> <div id={individualId} style="float: left; height: 600px; min-width: 100px; overflow: auto;"></div>)
//  }
//
//  private def displayList =
//  {
//    def line(individual : Individual) =
//    {
//      val link = SHtml.a(showCondition(individual) _, Text("Condition(fitness=" + individual.fitness + ")"))
//
//      <div>{link}</div>
//    }
//
//    val nodes = individuals.flatMap(line)
//
//    NodeSeq.fromSeq(nodes)
//  }
//
//  private def showCondition(individual : Individual)() =
//  {
//    SetHtml(individualId, individualToHtml(individual))
//  }
//
//  private def individualToHtml(individual : Individual) : NodeSeq =
//  {
//    val nodes = new NodeBuffer()
//
//    //Format fitness
//    nodes += <div>{"Fitness: " + individual.fitness}</div>
//
//    //Format the condition
//    val linkCondition = individual.node.build
//    implicit val prefixes = User().project.config.prefixes
//
//    nodes += <pre><tt>{linkCondition.toXML.toFormattedString}</tt></pre>
//
//    //Format the base operator
//    for(Individual.Base(operator, baseIndividual) <- individual.base)
//    {
//      nodes += <div>{"Operator: " + operator}</div>
//      nodes ++= individualToHtml(baseIndividual)
//    }
//
//    //Return nodes
//    nodes
//  }
//}
