package beam.agentsim.agents.choice.mode

import beam.agentsim.agents.choice.logit.MultinomialLogit.MNLSample
import beam.agentsim.agents.choice.mode.ModeChoiceTPCMCalculator.locationData
import beam.agentsim.agents.vehicles.BeamVehicle
import beam.agentsim.infrastructure.taz
import beam.agentsim.infrastructure.taz.{TAZ, TAZTreeMap}
import beam.router.Modes.BeamMode
import beam.router.Modes.BeamMode.{BIKE, BIKE_TRANSIT, BUS, CABLE_CAR, CAR, CAV, DRIVE_TRANSIT, FERRY, FUNICULAR, GONDOLA, HOV2_TELEPORTATION, HOV3_TELEPORTATION, RAIL, RIDE_HAIL, RIDE_HAIL_POOLED, RIDE_HAIL_TRANSIT, SUBWAY, TRAM, TRANSIT, WALK, WALK_TRANSIT}
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
import org.matsim.core.utils.geometry.CoordinateTransformation
import org.matsim.core.utils.geometry.transformations.TransformationFactory

import scala.beans.BeanProperty
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks.{break, breakable}

class ModeChoiceTPCMCalculator(
  beamServices: BeamServices
) {

  val transitModes = Seq(BUS, FUNICULAR, GONDOLA, CABLE_CAR, FERRY, TRAM, TRANSIT, RAIL, SUBWAY)
  val massTransitModes: List[BeamMode] = List(FERRY, TRANSIT, RAIL, SUBWAY, TRAM)
  private val locData: Seq[locationData] = parseLocationData(
    beamServices.beamConfig.beam.agentsim.agents.modalBehaviors.tpcmLoc.filePath
  )
  val ct: CoordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "epsg:26912")

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
      case HOV2_TELEPORTATION | HOV3_TELEPORTATION =>
        if(altAndIdx._1.costEstimate == 0.0){ 1.50 }  // TODO teleportation trips that develop from walk trips don't have a cost estimate, so we use a default value
        else{ altAndIdx._1.costEstimate }
      case _ =>
        altAndIdx._1.costEstimate
    }
  }

  // not currently being used because I don't think it includes all travel time
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

  // egress time overlaps with vehicle time, but I think that is okay
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

  // wait time may be incorrect, but it is the best we got
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

  def getWalkToTransitDistance(
    mode: BeamMode,
    altAndIdx: (EmbodiedBeamTrip, Int)
  ): ListBuffer[Double] = {
    mode match {
      case WALK_TRANSIT =>
        val distance = new ListBuffer[Double]
        val walkLegs = altAndIdx._1.legs.filter(_.beamLeg.mode == WALK)
        walkLegs.foreach{ leg =>
          val dis = leg.beamLeg.travelPath.distanceInM
          val disfloor = (math floor dis * 1000) / 1000
          distance += disfloor
        }
        distance
      case _ =>
        ListBuffer(0.0)
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

  // used to determine the amount of distance below 15 miles to a transit location if driving there
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

  // used to determine the distance from the origin location to transit
  def getOriginTransitProximity(
    mode: BeamMode,
    altAndIdx: (EmbodiedBeamTrip, Int)
  ): Double = {
    var proximity = 0.0
      // only applicable to walk transit
    if (mode == WALK_TRANSIT) {
      breakable{ for (n <- 0 to (altAndIdx._1.legs.length - 1)){
        // add distance of all legs between origin and first transit spot
        proximity = proximity + altAndIdx._1.legs(n).beamLeg.travelPath.distanceInM
        if(altAndIdx._1.legs(n+1).beamLeg.mode.value != "walk") break } }
      proximity * 0.000621371 //convert from meters to miles
    } else {0.0}
  }

  // used to determine the distance from the last transit stop to the destination
  def getDestTransitProximity(
    mode: BeamMode,
    altAndIdx: (EmbodiedBeamTrip, Int)
  ): Double = {
    var proximity = 0.0
    mode match {
        // only do stuff if a transit leg
      case TRANSIT | WALK_TRANSIT | DRIVE_TRANSIT | RIDE_HAIL_TRANSIT | BIKE_TRANSIT =>
        breakable{ for (n <- 0 to (altAndIdx._1.legs.length - 1)){
          // add distance of all legs between destination and last transit spot
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
      val originCoord = ct.transform(altAndIdx._1.legs(0).beamLeg.travelPath.startPoint.loc) // convert coord to EPSG 26912
      val originTAZ = tazTreeMap.getTAZ(originCoord).tazId.toString
    // get destination TAZ
      val length = altAndIdx._1.legs.length
      val destCoord = ct.transform(altAndIdx._1.legs(length-1).beamLeg.travelPath.endPoint.loc) // convert coord to EPSG 26912
      val destTAZ = tazTreeMap.getTAZ(destCoord).tazId.toString
    // return both TAZ values
      (originTAZ, destTAZ)
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
    // get data pertaining to TAZ and then get data needed to compute ZDI
    val originData = locData.toArray.filter(_.tazid.equalsIgnoreCase(originTAZ))
      val (oHh, oRes, oCom, oEmp) =
        (originData.head.tothh, originData.head.resacre, originData.head.ciacre, originData.head.totemp)
      val originZDI = Math.min(150, computeZDI(oHh, oRes, oCom, oEmp))
    // get data pertaining to TAZ and then get data needed to compute ZDI
    val destData = locData.toArray.filter(_.tazid.equalsIgnoreCase(destTAZ))
      val (dHh, dRes, dCom, dEmp) =
        (destData.head.tothh, destData.head.resacre, destData.head.ciacre, destData.head.totemp)
      val destZDI = computeZDI(dHh, dRes, dCom, dEmp)
    // return both computed ZDIs
    (originZDI, destZDI)
  }

  // used to determine the zonal density index of a taz
  private def computeZDI(hh: Double, res: Double, com: Double, emp: Double) = {
    if (res == 0 | com == 0 | (hh + emp) == 0 ){ 0.0 }
    else{ (hh / res * emp / com) / (hh / res + emp / com) }
  }

  def getListOfAttrValues(
    modeChoiceInputData: Vector[(BeamMode, Map[String, Double])],
    chosenModeOpt: Option[MNLSample[BeamMode]]
  ): String = {
    val altParamValues = modeChoiceInputData.filter(_._1.value == chosenModeOpt.get.alternativeType.value).head._2
    val altparamvalues = new ListBuffer[Any]
    altParamValues.foreach{ variable => altparamvalues += variable }
    val availableAlts3 = Some(altparamvalues.mkString(":"))
    availableAlts3.get
  }

  // used to determine the transit mode
  // currently not in use because ivtt dependent on mode type is more complex than needed
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

  // this is used to parse through the taz location csv input file
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
    new Optional(new ParseDouble()), // tothh
    new Optional(new ParseDouble()), // resacre
    new Optional(new ParseDouble()), // ciacre
    new Optional(new ParseDouble()), // totemp
    new Optional(new ParseDouble()) // cbd
    )
  }

  class locationData(
    @BeanProperty var tazid: String = "",
    @BeanProperty var tothh: Double = Double.NaN,
    @BeanProperty var resacre: Double = Double.NaN,
    @BeanProperty var ciacre: Double = Double.NaN,
    @BeanProperty var totemp: Double = Double.NaN,
    @BeanProperty var cbd: Double = Double.NaN
  ) extends Cloneable {
    override def clone(): AnyRef =
      new locationData(tazid, tothh, resacre, ciacre, totemp, cbd)
  }

}
