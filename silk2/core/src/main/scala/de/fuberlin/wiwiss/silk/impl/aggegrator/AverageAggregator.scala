package de.fuberlin.wiwiss.silk.impl.aggegrator

import de.fuberlin.wiwiss.silk.linkspec.Aggregator

class AverageAggregator(val params: Map[String, String] = Map.empty) extends Aggregator
{
    override def evaluate(values : Traversable[(Int, Double)]) =
    {
        if(!values.isEmpty)
        {
            var sumWeights = 0
            var sumValues = 0.0

            for((weight, value) <- values)
            {
                sumWeights += weight
                sumValues += weight * value
            }

            Some(sumValues / sumWeights)
        }
        else
        {
            None
        }
    }
}