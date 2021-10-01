package beam.agentsim.agents.choice.mode

import beam.agentsim.agents.vehicles.BeamVehicle
import beam.router.Modes.BeamMode
import beam.router.Modes.BeamMode.{BIKE, BIKE_TRANSIT, BUS, CABLE_CAR, CAR, CAV, DRIVE_TRANSIT, FERRY, FUNICULAR, GONDOLA, RAIL, RIDE_HAIL, RIDE_HAIL_POOLED, RIDE_HAIL_TRANSIT, SUBWAY, TRAM, TRANSIT, WALK, WALK_TRANSIT}
import beam.router.model.EmbodiedBeamTrip
import beam.sim.BeamServices
import org.matsim.api.core.v01.Id
import com.conveyal.r5.api.util.TransitModes

class ModeChoiceTPCMCalculator(
  beamServices: BeamServices
) {
  val transitModes = Seq(BUS, FUNICULAR, GONDOLA, CABLE_CAR, FERRY, TRAM, TRANSIT, RAIL, SUBWAY)
  val massTransitModes: List[BeamMode] = List(FERRY, TRANSIT, RAIL, SUBWAY, TRAM)

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

  def getTotalTravelTime(
    altAndIdx: (EmbodiedBeamTrip, Int)
  ): Double = {
    val timeInSeconds = altAndIdx._1.legs.view
      .map(_.beamLeg.duration)
      .sum
    (timeInSeconds / 60)
  }

  def getWalkTime(altAndIdx: (EmbodiedBeamTrip, Int)): Double = {
    val timeInSeconds = altAndIdx._1.legs.view
      .filter(_.beamLeg.mode == WALK)
      .map(_.beamLeg.duration)
      .sum
    (timeInSeconds / 60)
  }

  def getBikeTime(altAndIdx: (EmbodiedBeamTrip, Int)): Double = {
    val timeInSeconds = altAndIdx._1.legs.view
      .filter(_.beamLeg.mode == BIKE)
      .map(_.beamLeg.duration)
      .sum
    (timeInSeconds / 60)
  }

  def getEgressTime(
    mode: BeamMode,
    altAndIdx: (EmbodiedBeamTrip, Int)
  ): Double = { // should this include BIKE_TRANSIT?
    if (mode in(DRIVE_TRANSIT,RIDE_HAIL_TRANSIT,BIKE_TRANSIT,TRANSIT,WALK_TRANSIT)) {
      val timeInSeconds = altAndIdx._1.legs.view
        .filter(_.beamLeg.mode in(CAR,WALK,BIKE))
        .map(_.beamLeg.duration)
        .sum
      (timeInSeconds / 60)
    } else {0.0}
  }

  def getVehicleTime(altAndIdx: (EmbodiedBeamTrip, Int)): Double = {
    val timeInSeconds = altAndIdx._1.legs.view
      .filter(_.beamLeg.mode != WALK)
      .filter(_.beamLeg.mode != BIKE)
      .map(_.beamLeg.duration)
      .sum
    (timeInSeconds / 60)
  }

  def getWaitTime(
    walkTime: Double,
    vehicleTime: Double,
    totalTravelTime: Double
  ): Double = {
    val timeInSeconds = totalTravelTime - walkTime - vehicleTime
    (timeInSeconds / 60)
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

  def getWalkDistance(
    mode: BeamMode,
    altAndIdx: (EmbodiedBeamTrip, Int)
  ): Double = {
    mode match {
      case WALK =>
        var distance = 0.0
        altAndIdx._1.legs.foreach{ leg =>
          distance = distance + leg.beamLeg.travelPath.distanceInM
        }
        distance * 0.000621371 //convert from meters to miles
      case _ =>
        0
    }
  }

  def getBikeDistance(
    mode: BeamMode,
    altAndIdx: (EmbodiedBeamTrip, Int)
  ): Double = {
    mode match {
      case BIKE =>
        var distance = 0.0
        altAndIdx._1.legs.foreach{ leg =>
          distance = distance + leg.beamLeg.travelPath.distanceInM
        }
        distance * 0.000621371 //convert from meters to miles
      case _ =>
        0
    }
  }

  def getIVTTMode(
    mode: BeamMode,
    altAndIdx: (EmbodiedBeamTrip, Int)
  ): String = {
    var ivttMode: BeamMode = mode
    altAndIdx._1.legs.foreach { leg =>
      var tMode: BeamMode = leg.beamLeg.mode
      if (transitModes.contains(tMode) | tMode == CAR) {
        ivttMode = tMode
      }
    }
    getIVTTModeType(ivttMode)
  }


  private def getIVTTModeType(mode:BeamMode): String ={
    mode match {
      case CAR  =>
        "car/bus"
      case RAIL | CABLE_CAR | GONDOLA | FUNICULAR =>
        "light-rail"
      case FERRY =>
        "ferry"
      case  SUBWAY | TRANSIT | TRAM=>
        "heavy-rail"
      case BUS =>
        "express-bus"
      case WALK | BIKE =>
        "none"
      case _ =>
        "something_wrong"
    }
  }



}
