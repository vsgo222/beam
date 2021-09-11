package beam.agentsim.agents.choice.mode

import beam.router.Modes.BeamMode
import beam.router.Modes.BeamMode.{BIKE_TRANSIT, DRIVE_TRANSIT, RIDE_HAIL, TRANSIT, WALK_TRANSIT}
import beam.router.model.EmbodiedBeamTrip
import beam.sim.BeamServices

class ModeChoiceTPCMCalculator(
  beamServices: BeamServices
) {

  def getTotalCost(
    mode: BeamMode,
    altAndIdx: (EmbodiedBeamTrip, Int),
    transitFareDefaults: Seq[Double]
  ) : Double = {
    mode match {
      case TRANSIT | WALK_TRANSIT | DRIVE_TRANSIT | BIKE_TRANSIT =>
        (altAndIdx._1.costEstimate + transitFareDefaults(altAndIdx._2)) * beamServices.beamConfig.beam.agentsim.tuning.transitPrice
      case RIDE_HAIL =>
        altAndIdx._1.costEstimate * beamServices.beamConfig.beam.agentsim.tuning.rideHailPrice
      case _ =>
        altAndIdx._1.costEstimate
    }
  }





}
