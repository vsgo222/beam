package beam.agentsim.agents.choice.mode

import beam.agentsim.agents.vehicles.BeamVehicle
import beam.router.Modes.BeamMode
import beam.router.Modes.BeamMode.{BIKE, BIKE_TRANSIT, DRIVE_TRANSIT, RIDE_HAIL, RIDE_HAIL_TRANSIT, TRANSIT, WALK, WALK_TRANSIT}
import beam.router.model.EmbodiedBeamTrip
import beam.sim.BeamServices
import org.matsim.api.core.v01.Id

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

  def getWalkTime(altAndIdx: (EmbodiedBeamTrip, Int)): Double = {
    altAndIdx._1.legs.view
      .filter(_.beamLeg.mode == WALK)
      .map(_.beamLeg.duration)
      .sum
  }

  def getBikeTime(altAndIdx: (EmbodiedBeamTrip, Int)): Double = {
    altAndIdx._1.legs.view
      .filter(_.beamLeg.mode == BIKE)
      .map(_.beamLeg.duration)
      .sum
  }

  def getVehicleTime(altAndIdx: (EmbodiedBeamTrip, Int)): Double = {
    altAndIdx._1.legs.view
      .filter(_.beamLeg.mode != WALK)
      .filter(_.beamLeg.mode != BIKE)
      .map(_.beamLeg.duration)
      .sum
  }

  def getWaitTime(
    altAndIdx: (EmbodiedBeamTrip, Int),
    walkTime: Double,
    vehicleTime: Double
  ): Double = {
    altAndIdx._1.totalTravelTimeInSecs - walkTime - vehicleTime
  }

  def getNumTransfers(
    mode: BeamMode,
    altAndIdx: (EmbodiedBeamTrip, Int)
  ): Int = {
    mode match {
      case TRANSIT | WALK_TRANSIT | DRIVE_TRANSIT | RIDE_HAIL_TRANSIT | BIKE_TRANSIT =>
        var nVeh = -1
        var vehId = Id.create("dummy", classOf[BeamVehicle])
        altAndIdx._1.legs.foreach { leg =>
          if (leg.beamLeg.mode.isTransit && leg.beamVehicleId != vehId) {
            vehId = leg.beamVehicleId
            nVeh = nVeh + 1
          }
        }
        nVeh
      case _ =>
        0
    }
  }












}
