package beam.agentsim.agents.choice.mode

import beam.agentsim.agents.modalbehaviors.ModeChoiceCalculator
import beam.router.Modes
import beam.router.model.EmbodiedBeamTrip
import beam.sim.config.BeamConfig
import beam.sim.population.AttributesOfIndividual
import org.matsim.api.core.v01.population.{Activity, Person}

import scala.collection.mutable.ListBuffer

/**
  * BEAM
  */
class ModeChoiceUniformRandom(val beamConfig: BeamConfig) extends ModeChoiceCalculator {

  override def apply(
    alternatives: IndexedSeq[EmbodiedBeamTrip],
    attributesOfIndividual: AttributesOfIndividual,
    destinationActivity: Option[Activity],
    tourPurpose: String,
    person: Option[Person] = None
  ): Option[EmbodiedBeamTrip] = {
    if (alternatives.nonEmpty) {
      Some(alternatives(chooseRandomAlternativeIndex(alternatives)))
    } else {
      None
    }
  }

  override def utilityOf(
    alternative: EmbodiedBeamTrip,
    attributesOfIndividual: AttributesOfIndividual,
    destinationActivity: Option[Activity],
    tourPurpose: String
  ): Double = 0.0

  override def utilityOf(
    alternative: EmbodiedBeamTrip,
    attributesOfIndividual: AttributesOfIndividual,
    destinationActivity: Option[Activity],
    person: Person
  ): Double = 0.0

  override def utilityOf(
    person: Person,
    mode: Modes.BeamMode,
    cost: Double,
    time: Double,
    numTransfers: Int,
    transitOccupancyLevel: Double
  ): Double = 0.0

  override def computeAllDayUtility(
    trips: ListBuffer[EmbodiedBeamTrip],
    person: Person,
    attributesOfIndividual: AttributesOfIndividual
  ): Double = 0.0
}
