package de.fuberlin.wiwiss.silk.util.sparql

import de.fuberlin.wiwiss.silk.instance.{Path, InstanceSpecification, Instance}

/**
 * Retrieves instances from a SPARQL endpoint.
 */
class InstanceRetriever(endpoint : SparqlEndpoint, pageSize : Int = 1000, graphUri : Option[String] = None)
{
    private val varPrefix = "v"

    /**
     * Retrieves instances with a given instance specification.
     *
     * @param instanceSpec The instance specification
     * @param instances The URIs of the instances to be retrieved. If empty, all instances will be retrieved.
     * @return The retrieved instances
     */
    def retrieve(instanceSpec : InstanceSpecification, instances : Seq[String]) : Traversable[Instance] =
    {
        if(instances.isEmpty)
        {
            retrieveAll(instanceSpec)
        }
        else
        {
            retrieveList(instances, instanceSpec)
        }
    }

    /**
     * Retrieves all instances with a given instance specification.
     *
     * @param instanceSpec The instance specification
     * @return The retrieved instances
     */
    def retrieveAll(instanceSpec : InstanceSpecification) : Traversable[Instance] =
    {
        //Prefixes
        var sparql = instanceSpec.prefixes.map{case (prefix, uri) => "PREFIX " + prefix + ": <" + uri + ">\n"}.mkString

        //Select
        sparql += "SELECT DISTINCT "
        sparql += "?" + instanceSpec.variable + " "
        sparql += instanceSpec.paths.map("?" + varPrefix + _.id).mkString(" ") + "\n"

        //Graph
        for(graph <- graphUri) sparql += "FROM <" + graph + ">\n"

        //Body
        sparql += "WHERE {\n"
        if(instanceSpec.restrictions.isEmpty && instanceSpec.paths.isEmpty)
        {
            sparql += "?" + instanceSpec.variable + " ?" + varPrefix + "_p ?" + varPrefix + "_o "
        }
        else
        {
            sparql += instanceSpec.restrictions + "\n"
            sparql += SparqlPathBuilder(instanceSpec.paths, "?" + instanceSpec.variable, "?" + varPrefix)
        }
        sparql += "}"

        val sparqlResults = endpoint.query(sparql)

        new InstanceTraversable(sparqlResults, instanceSpec, None)
    }

    /**
     * Retrieves a list of instances.
     *
     * @param instanceUris The URIs of the instances
     * @param instanceSpec The instance specification
     * @return A sequence of the retrieved instances. If a instance is not in the store, it wont be included in the returned sequence.
     */
    def retrieveList(instanceUris : Seq[String], instanceSpec : InstanceSpecification) : Seq[Instance] =
    {
        instanceUris.view.flatMap(instanceUri => retrieveInstance(instanceUri, instanceSpec))
    }

    /**
     * Retrieves a single instance.
     *
     * @param instanceUri The URI of the instance
     * @param instanceSpec The instance specification
     * @return Some(instance), if a instance with the given uri is in the Store
     *         None, if no instance with the given uri is in the Store
     */
    def retrieveInstance(instanceUri : String, instanceSpec : InstanceSpecification) : Option[Instance] =
    {
        //Query 5 paths at once and combine the result into one
        val pathGroups = instanceSpec.paths.toList.grouped(5).toList

        val sparqlResults = pathGroups.flatMap(pathGroup => retrievePaths(instanceUri, pathGroup, instanceSpec.prefixes))

        new InstanceTraversable(sparqlResults, instanceSpec, Some(instanceUri)).headOption
    }

    private def retrievePaths(instanceUri : String, paths : Traversable[Path], prefixes : Map[String, String] = Map.empty) =
    {
        //Prefixes
        var sparql = prefixes.map{case (prefix, uri) => "PREFIX " + prefix + ": <" + uri + ">\n"}.mkString

        //Select
        sparql += "SELECT DISTINCT "
        sparql += paths.map("?" + varPrefix + _.id).mkString(" ") + "\n"

        //Graph
        for(graph <- graphUri) sparql += "FROM <" + graph + ">\n"

        //Body
        sparql += "WHERE {\n"
        sparql += SparqlPathBuilder(paths, "<" + instanceUri + ">", "?" + varPrefix)
        sparql += "}"

        endpoint.query(sparql)
    }

    /**
     * Wraps a Traversable of SPARQL results and retrieves instances from them.
     */
    private class InstanceTraversable(sparqlResults : Traversable[Map[String, Node]], instanceSpec : InstanceSpecification, subject : Option[String]) extends Traversable[Instance]
    {
        override def foreach[U](f : Instance => U) : Unit =
        {
            //Remember current subject
            var curSubject : Option[String] = subject

            //Collect values of the current subject
            val values = collection.mutable.HashMap[Int, Set[String]]()

            for(result <- sparqlResults)
            {
                //If the subject is unknown, find binding for subject variable
                if(subject.isEmpty)
                {
                    //Check if we are still reading values for the current subject
                    val resultSubject = result.get(instanceSpec.variable) match
                    {
                        case Some(Resource(value)) => Some(value)
                        case _ => None
                    }

                    if(resultSubject != curSubject)
                    {
                        for(curSubjectUri <- curSubject)
                        {
                            f(new Instance(instanceSpec.variable, curSubjectUri, values.toMap))
                        }

                        curSubject = resultSubject
                        values.clear()
                    }
                }

                //Find results for values for the current subject
                if(curSubject.isDefined)
                {
                    for((variable, node) <- result if variable.startsWith(varPrefix))
                    {
                        val id = variable.substring(varPrefix.length).toInt

                        val oldVarValues = values.get(id).getOrElse(Set())

                        values(id) = oldVarValues + node.value
                    }
                }
            }

            for(curSubjectUri <- curSubject)
            {
                f(new Instance(instanceSpec.variable, curSubjectUri, values.toMap))
            }
        }
    }
}