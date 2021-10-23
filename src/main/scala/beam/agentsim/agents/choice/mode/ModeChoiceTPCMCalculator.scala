package beam.agentsim.agents.choice.mode

import beam.agentsim.agents.choice.mode.ModeChoiceTPCMCalculator.locationData
import beam.agentsim.agents.vehicles.BeamVehicle
import beam.agentsim.infrastructure.taz
import beam.agentsim.infrastructure.taz.{TAZ, TAZTreeMap}
import beam.router.Modes.BeamMode
import beam.router.Modes.BeamMode.{BIKE, BIKE_TRANSIT, BUS, CABLE_CAR, CAR, CAV, DRIVE_TRANSIT, FERRY, FUNICULAR, GONDOLA, RAIL, RIDE_HAIL, RIDE_HAIL_POOLED, RIDE_HAIL_TRANSIT, SUBWAY, TRAM, TRANSIT, WALK, WALK_TRANSIT}
import beam.router.model.EmbodiedBeamTrip
import beam.sim.BeamServices
import beam.sim.config.BeamConfig
import org.matsim.api.core.v01.Id
import com.conveyal.r5.api.util.TransitModes
import org.matsim.core.utils.io.IOUtils
import org.supercsv.cellprocessor.constraint.NotNull
import org.supercsv.cellprocessor.{Optional, ParseDouble}
import org.supercsv.cellprocessor.ift.CellProcessor
import org.supercsv.io.CsvBeanReader
import org.supercsv.prefs.CsvPreference

import scala.beans.BeanProperty
import scala.collection.mutable
import scala.util.control.Breaks.{break, breakable}

class ModeChoiceTPCMCalculator(
  beamServices: BeamServices
) {

  val transitModes = Seq(BUS, FUNICULAR, GONDOLA, CABLE_CAR, FERRY, TRAM, TRANSIT, RAIL, SUBWAY)
  val massTransitModes: List[BeamMode] = List(FERRY, TRANSIT, RAIL, SUBWAY, TRAM)
  private val locData: Seq[locationData] = parseLocationData(
    beamServices.beamConfig.beam.agentsim.agents.modalBehaviors.tpcmLoc.filePath
  )

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

  def getTAZs(
    altAndIdx: (EmbodiedBeamTrip, Int),
    beamConfig: BeamConfig
  ): (String, String) = {
      val tazTreeMap: TAZTreeMap = taz.TAZTreeMap.getTazTreeMap(beamConfig.beam.agentsim.taz.filePath)
    // get origin TAZ
      val originCoord = altAndIdx._1.legs(0).beamLeg.travelPath.startPoint.loc
      val originTAZ = tazTreeMap.getTAZ(originCoord).tazId.toString
    // get destination TAZ
      val length = altAndIdx._1.legs.length
      val destCoord = altAndIdx._1.legs(length-1).beamLeg.travelPath.startPoint.loc
      val destTAZ = tazTreeMap.getTAZ(destCoord).tazId.toString
    // return both TAZ values
      (originTAZ, destTAZ) // right now all originTAZ = destTAZ. Double check this. 
  }

  def getCBD(
    destTAZ : String
  ): Double = {
    // get data pertaining to destTAZ and then get the cbd value
    val destData = locData.toArray.filter(_.tazid.equalsIgnoreCase(destTAZ))
      val destCBD = destData.head.cbd
    // return CBD value
    destCBD
  }

  def getZDIs(
    originTAZ: String,
    destTAZ: String
  ): (Double, Double) = {
    // get data pertainint to TAZ and then get data needed to compute ZDI
    val originData = locData.toArray.filter(_.tazid.equalsIgnoreCase(originTAZ))
      val (oHh, oRes, oCom, oEmp) =
        (originData.head.households, originData.head.residential, originData.head.commercial, originData.head.employment)
      val originZDI = Math.min(150, computeZDI(oHh, oRes, oCom, oEmp))
    // get data pertainint to TAZ and then get data needed to compute ZDI
    val destData = locData.toArray.filter(_.tazid.equalsIgnoreCase(destTAZ))
      val (dHh, dRes, dCom, dEmp) =
        (destData.head.households, destData.head.residential, destData.head.commercial, destData.head.employment)
      val destZDI = computeZDI(dHh, dRes, dCom, dEmp)
    // return both computed ZDIs
    (originZDI, destZDI)
  }

  private def computeZDI(hh: Double, res: Double, com: Double, emp: Double) = {
    (hh / res * emp / com) / (hh / res + emp / com)
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

  private def parseLocationData(locationDataFileName: String): Seq[locationData] = {
    val beanReader = new CsvBeanReader(
      IOUtils.getBufferedReader(locationDataFileName),
      CsvPreference.STANDARD_PREFERENCE
    )
    val firstLineCheck = true
    val header = beanReader.getHeader(firstLineCheck)
    val processors: Array[CellProcessor] = ModeChoiceTPCMCalculator.getProcessors

    val result = mutable.ArrayBuffer[locationData]()
    var row: locationData = newEmptyRow()
    while (beanReader.read[locationData](row, header, processors: _*) != null) {
      if (Option(row.cbd).isDefined && !row.cbd.isNaN)
        result += row.clone().asInstanceOf[locationData]
      row = newEmptyRow()
    }
    result
  }

  private def newEmptyRow(): locationData = new locationData()

}

object ModeChoiceTPCMCalculator {

  private def getProcessors: Array[CellProcessor] = {
    Array[CellProcessor](
    new NotNull, // tazid
    new Optional(new ParseDouble()), // households
    new Optional(new ParseDouble()), // residential
    new Optional(new ParseDouble()), // commercial
    new Optional(new ParseDouble()), // employment
    new Optional(new ParseDouble()) // cbd
    )
  }

  class locationData(
    @BeanProperty var tazid: String = "",
    @BeanProperty var households: Double = Double.NaN,
    @BeanProperty var residential: Double = Double.NaN,
    @BeanProperty var commercial: Double = Double.NaN,
    @BeanProperty var employment: Double = Double.NaN,
    @BeanProperty var cbd: Double = Double.NaN
  ) extends Cloneable {
    override def clone(): AnyRef =
      new locationData(tazid, households, residential, commercial, employment, cbd)
  }

}
