package beam.agentsim.agents.choice.mode

import beam.agentsim.agents.vehicles.BeamVehicle
import beam.router.Modes.BeamMode
import beam.router.Modes.BeamMode.{BIKE, BIKE_TRANSIT, BUS, CABLE_CAR, CAR, CAV, DRIVE_TRANSIT, FERRY, FUNICULAR, GONDOLA, RAIL, RIDE_HAIL, RIDE_HAIL_POOLED, RIDE_HAIL_TRANSIT, SUBWAY, TRAM, TRANSIT, WALK, WALK_TRANSIT}
import beam.router.model.EmbodiedBeamTrip
import beam.sim.BeamServices
import org.matsim.api.core.v01.Id
import com.conveyal.r5.api.util.TransitModes

import scala.util.control.Breaks.{break, breakable}

class ModeChoiceTPCMCalculator(
  beamServices: BeamServices
) {

  val transitModes = Seq(BUS, FUNICULAR, GONDOLA, CABLE_CAR, FERRY, TRAM, TRANSIT, RAIL, SUBWAY)
  val massTransitModes: List[BeamMode] = List(FERRY, TRANSIT, RAIL, SUBWAY, TRAM)
  val timeCoefs = Map("atwork"-> -0.0655, "univ" -> -0.0775, "othdiscr" -> -0.0605, "eatout" -> -0.0605, "escort" -> -0.0605,
    "othmaint" -> -0.0605, "school" -> -0.0775, "shopping" -> -0.0605, "social" -> -0.0605, "work" -> -0.0465)

  def getTotalCost(
    mode: BeamMode,
    altAndIdx: (EmbodiedBeamTrip, Int),
    transitFareDefaults: Seq[Double]
  ) : Double = {
    mode match {
      case TRANSIT | WALK_TRANSIT | DRIVE_TRANSIT | BIKE_TRANSIT =>
        (altAndIdx._1.costEstimate + transitFareDefaults(altAndIdx._2)) * beamServices.beamConfig.beam.agentsim.tuning.transitPrice
      case RIDE_HAIL | RIDE_HAIL_TRANSIT | RIDE_HAIL_POOLED =>
        altAndIdx._1.costEstimate * beamServices.beamConfig.beam.agentsim.tuning.rideHailPrice
      case _ =>
        altAndIdx._1.costEstimate
    }
  }

  def getCostCoef(vot: Double,
    purpose: String
  ): Double = {
    timeCoefs.get(purpose).get * 60 / vot // get the cost coeff using the time coeff and multiplying be the VOT
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
    totalTravelTime - walkTime - vehicleTime
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

  def getDriveTransitDistance(
    mode: BeamMode,
    altAndIdx: (EmbodiedBeamTrip, Int)
  ): Double = {
    mode match {
      case DRIVE_TRANSIT | RIDE_HAIL_TRANSIT =>
        var distance = 0.0
        altAndIdx._1.legs.foreach { leg =>
          distance = distance + leg.beamLeg.travelPath.distanceInM
        }
        distance = distance * 0.000621371 //convert from meters to miles
        val distUnder15 = if (distance < 15.0) {15.0 - distance} else {0.0}
        distUnder15
      case _ =>
        0
    }
  }

  def getOriginTransitProximity(
    mode: BeamMode,
    altAndIdx: (EmbodiedBeamTrip, Int)
  ): Double = {
    var proximity = 0.0
    if (mode == WALK_TRANSIT) {
      //easier way --> val proximity = altAndIdx._1.legs(0).beamLeg.travelPath.distanceInM
      breakable{ for (n <- 0 to (altAndIdx._1.legs.length - 1)){
        proximity = proximity + altAndIdx._1.legs(n).beamLeg.travelPath.distanceInM
        if(altAndIdx._1.legs(n+1).beamLeg.mode.value != "walk") break } }
      proximity * 0.000621371 //convert from meters to miles
    } else {0.0}
  }

  def getDestTransitProximity(
    mode: BeamMode,
    altAndIdx: (EmbodiedBeamTrip, Int)
  ): Double = {
    var proximity = 0.0
    mode match {
      case TRANSIT | WALK_TRANSIT | DRIVE_TRANSIT | RIDE_HAIL_TRANSIT | BIKE_TRANSIT =>
        breakable{ for (n <- 0 to (altAndIdx._1.legs.length - 1)){
          proximity = proximity + altAndIdx._1.legs.reverse(n).beamLeg.travelPath.distanceInM
          if(transitModes.contains(altAndIdx._1.legs.reverse(n+1).beamLeg.mode)) break } }
          proximity * 0.000621371 //convert from meters to miles
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
