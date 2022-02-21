package beam.agentsim.agents.choice.mode

import beam.agentsim.agents.choice.logit.LatentClassChoiceModel
import beam.agentsim.agents.choice.logit.LatentClassChoiceModel.{Mandatory, TourType}
import beam.agentsim.agents.choice.logit.MultinomialLogit.MNLSample
import beam.agentsim.agents.choice.mode.ModeChoiceLCCM.ModeChoiceData
import beam.agentsim.agents.modalbehaviors.ModeChoiceCalculator
import beam.agentsim.agents.vehicles.BeamVehicle
import beam.agentsim.infrastructure.taz
import beam.agentsim.infrastructure.taz.TAZTreeMap
import beam.router.Modes
import beam.router.Modes.BeamMode
import beam.router.Modes.BeamMode.{BIKE, BIKE_TRANSIT, CAR, DRIVE_TRANSIT, HOV2, HOV3, RIDE_HAIL, RIDE_HAIL_TRANSIT, TRANSIT, WALK, WALK_TRANSIT}
import beam.router.model.EmbodiedBeamTrip
import beam.router.skim.readonly.TransitCrowdingSkims
import beam.sim.config.BeamConfig
import beam.sim.population.AttributesOfIndividual
import beam.sim.{BeamServices, MapStringDouble}
import org.matsim.api.core.v01.Id
import org.matsim.api.core.v01.population.{Activity, Person}

import java.util.Random
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

/**
  * ModeChoiceTPCM
  *
  * Data used by mode choice model:
  *  Path Variables:
      *  transfer
      *  vehicleTime, egressTime, waitTime
      *  originCloseToTransit, originFarFromTransit, destCloseToTransit, destFarFromTransit
      *  shortWalkDist, longWalkDist, shortBikeDist, longBikeDist, shortDrive
  *  Person Variables:
      *  cost
      *  age1619, age010, age16P
      *  hhSize1, hhSize2
      *  ascNoAuto, ascAutoDeficient, ascAutoSufficient
  *  Location Variables:
      *  destZDI, originZDI
      *  CBD
  *
  * The TPCM model is based on the LCCM model, but instead of differentiating between Mandatory and Nonmandatory trips, it
  * differentiates between tour purpose and type of variable (Path, Person, Location).
  *
  * TODO pass person attributes and tour purpose so correct model can be applied
  */
class ModeChoiceTPCM(
  val beamServices: BeamServices,
  val lccm: LatentClassChoiceModel,
  val varType: String,
  transitCrowding: TransitCrowdingSkims,
) extends ModeChoiceCalculator {

  override lazy val beamConfig: BeamConfig = beamServices.beamConfig
  var expectedMaximumUtility: Double = Double.NaN
  val TPCMCalculator = new ModeChoiceTPCMCalculator(beamServices)

  override def apply(
    alternatives: IndexedSeq[EmbodiedBeamTrip],
    attributesOfIndividual: AttributesOfIndividual,
    destinationActivity: Option[Activity],
    person: Option[Person] = None,
    tourPurpose : String
  ): Option[EmbodiedBeamTrip] = {
    // get person attributes from the attributesOfIndividual and person object to determine utility coefficients later on
      val age = attributesOfIndividual.age.get.asInstanceOf[Double]
      val hhSize = attributesOfIndividual.householdAttributes.householdSize.asInstanceOf[Double]
      val vot = attributesOfIndividual.valueOfTime
      val autoWork = person.get.getAttributes.getAttribute("autoWorkRatio").toString
    // pass tour alternatives, tour purpose, and person attributes to choice model
    choose(alternatives, tourPurpose, autoWork, age, hhSize, vot)
  }

  private def choose(
    alternatives: IndexedSeq[EmbodiedBeamTrip],
    purpose: String,
    autoWork: String,
    age: Double,
    hhSize: Double,
    vot: Double
  ): Option[EmbodiedBeamTrip] = {
    if (alternatives.isEmpty) {
      None
    } else {
      val bestInGroup = altsToBestInGroup(alternatives, Mandatory)
      // Fill out the input data structures required by the MNL models
      val modeChoiceInputData = bestInGroup.map { alt =>
        val theParams = attributes(
          alt.vehicleTime,
          alt.waitTime,
          alt.egressTime,
          if (alt.originTransitProximity <= .5) {1.0} else {0.0},
          if (alt.originTransitProximity > .5)  {1.0} else {0.0},
          if (alt.destTransitProximity <= .5)   {1.0} else {0.0},
          if (alt.destTransitProximity > .5)    {1.0} else {0.0},
          alt.numTransfers,
          if (alt.walkDistance <= 1.5) {alt.walkDistance} else {0.0},
          if (alt.walkDistance > 1.5)  {alt.walkDistance} else {0.0},
          if (alt.bikeDistance <= 1.5) {alt.bikeDistance} else {0.0},
          if (alt.bikeDistance > 1.5)  {alt.bikeDistance} else {0.0},
          if (alt.cost == 0.0 || vot == 0.0) {0.0} else {alt.cost * 60 / vot}, // the cost utility values in the csv are actually vehicleTime values, and this conversion creates the cost coefficient
          alt.destZDI,
          alt.originZDI,
          if (age >= 16.0 & age <= 19.0)  {age} else {0.0}, // TODO: is it age, or 1.0? (is this value a coeff or const?)
          if (age <= 10.0)                {age} else {0.0},
          if (age >= 16.0)                {age} else {0.0},
          if (hhSize == 1.0)              {hhSize} else {0.0}, // TODO: is it hhSize, or 1.0? (is this value a coeff or const?)
          if (hhSize == 2.0)              {hhSize} else {0.0},
          alt.dtDistance,
          alt.destCBD,
          if (autoWork.equals("no_auto"))         {1.0} else {0.0},
          if (autoWork.equals("auto_deficient"))  {1.0} else {0.0},
          if (autoWork.equals("auto_sufficient")) {1.0} else {0.0},
          alt.walkTime
        )
        (alt.mode, theParams)
      }// TODO: instead of .toMap, we keep it in a Vector to prevent HOV2 and HOV3 options from grouping together into CAR


      // create a vector of extra data that can be outputted for tracing and calibration
      val extraData = bestInGroup.map{alt =>
        val theParams = List(
          "(walkTransitDistances," + alt.walkToTransitDistance + ")", // in meters
          "(cost," + alt.cost.toString + ")", // in dollars
          "(numberOfTransfers," + alt.numTransfers + ")"
        )
        (alt.mode, theParams.mkString(":"))
      }

      // Evaluate and sample from mode choice model
      val (model, modeModel) = lccm.modeChoiceTourModels(varType)(purpose)
      val chosenModeOpt = modeModel.sampleAlternative(modeChoiceInputData, new Random())
      expectedMaximumUtility = modeModel.getExpectedMaximumUtility(modeChoiceInputData).getOrElse(0)

      chosenModeOpt match {
        case Some(chosenMode) =>
          val chosenAlt = bestInGroup.filter(_.mode.value.equalsIgnoreCase(chosenMode.alternativeType.value))
          if (chosenAlt.isEmpty) {
            None
          } else {
            alternatives(chosenAlt.head.index).calculatedUtiilty = chosenModeOpt.get.utility
            alternatives(chosenAlt.head.index).attributeValues = TPCMCalculator.getListOfAttrValues(modeChoiceInputData,chosenModeOpt)
            alternatives(chosenAlt.head.index).extraData = extraData.filter(alt => alt._1.value == chosenAlt.head.mode.value).head._2
            Some(alternatives(chosenAlt.head.index))
          }
        case None =>
          None
      }
    }
  }

  // defines all attributes used by the Tour Purpose Mode Choice Model
  private def attributes(
    vehicleTime: Double,
    waitTime: Double,
    egressTime: Double,
    originCloseToTransit: Double,
    originFarFromTransit: Double,
    destCloseToTransit: Double,
    destFarFromTransit: Double,
    transfer: Double,
    shortWalkDist: Double,
    longWalkDist: Double,
    shortBikeDist: Double,
    longBikeDist: Double,
    cost: Double,
    destZDI: Double,
    originZDI: Double,
    age1619: Double,
    age010: Double,
    age16P: Double,
    hhSize1: Double,
    hhSize2: Double,
    shortDrive: Double,
    CBD: Double,
    ascNoAuto: Double,
    ascAutoDeficient: Double,
    ascAutoSufficient: Double,
    walkTime: Double
  ) = {
    Map(
      "vehicleTime"           -> vehicleTime,
      "waitTime"              -> waitTime,
      "egressTime"            -> egressTime,
      "originCloseToTransit"  -> originCloseToTransit,
      "originFarFromTransit"  -> originFarFromTransit,
      "destCloseToTransit"    -> destCloseToTransit,
      "destFarFromTransit"    -> destFarFromTransit,
      "transfer"              -> transfer,
      "shortWalkDist"         -> shortWalkDist,
      "longWalkDist"          -> longWalkDist,
      "shortBikeDist"         -> shortBikeDist,
      "longBikeDist"          -> longBikeDist,
      "cost"                  -> cost,
      "destZDI"               -> destZDI,
      "originZDI"             -> originZDI,
      "age1619"               -> age1619,
      "age010"                -> age010,
      "age16P"                -> age16P,
      "hhSize1"               -> hhSize1,
      "hhSize2"               -> hhSize2,
      "shortDrive"            -> shortDrive,
      "CBD"                   -> CBD,
      "ascNoAuto"             -> ascNoAuto,
      "ascAutoDeficient"      -> ascAutoDeficient,
      "ascAutoSufficient"     -> ascAutoSufficient,
      "walkTime"              -> walkTime
    )
  }

  def altsToBestInGroup(
    alternatives: IndexedSeq[EmbodiedBeamTrip],
    tourType: TourType
  ): Vector[ModeChoiceData] = {
    //check to see if this is always 0
    val transitFareDefaults: Seq[Double] =
      TransitFareDefaults.estimateTransitFares(alternatives)
    val modeChoiceAlternatives: Seq[ModeChoiceData] =
      alternatives.zipWithIndex.map { altAndIdx =>
        //get mode and total cost of current trip
          val mode = altAndIdx._1.tripClassifier
          val totalCost = TPCMCalculator.getTotalCost(mode, altAndIdx, transitFareDefaults)
        //TODO verify wait time is correct, look at transit and ride_hail in particular (is totalTravelTime correct?)
        // total travel time does not equal sum of all leg times (because it includes wait time?)
          val totalTravelTime = altAndIdx._1.totalTravelTimeInSecs / 60
          val egressTime = TPCMCalculator.getEgressTime(mode, altAndIdx)
          val vehicleTime = TPCMCalculator.getVehicleTime(altAndIdx) //can ivtt overlap with egress time?
          val walkTime = TPCMCalculator.getWalkTime(altAndIdx)
          val bikeTime = TPCMCalculator.getBikeTime(altAndIdx)
          val waitTime = TPCMCalculator.getWaitTime(walkTime, vehicleTime, totalTravelTime)
        //TODO verify number of transfers is correct
          val numTransfers = TPCMCalculator.getNumTransfers(mode, altAndIdx)
          assert(numTransfers >= 0)
        //determine distance for walk or bike modes; also drive transit distance value
          val walkDistance = TPCMCalculator.getWalkDistance(mode, altAndIdx)
          val walkToTransitDistance = TPCMCalculator.getWalkToTransitDistance(mode,altAndIdx).mkString(":")
          val bikeDistance = TPCMCalculator.getBikeDistance(mode, altAndIdx)
          val dtDistance = TPCMCalculator.getDriveTransitDistance(mode, altAndIdx)
        //determine proximity to transit
          val originTransitProximity = TPCMCalculator.getOriginTransitProximity(mode, altAndIdx)
          val destTransitProximity = TPCMCalculator.getDestTransitProximity(mode, altAndIdx)
        //determine origin and destination tazs, also whether it is a cbd
          val (originTAZ, destTAZ) = TPCMCalculator.getTAZs(altAndIdx, beamConfig)
          val (originZDI, destZDI) = TPCMCalculator.getZDIs(originTAZ, destTAZ)
          val destCBD = TPCMCalculator.getCBD(destTAZ)
        //determine percentile, occupancy level, and embodied trip value
          val percentile = beamConfig.beam.agentsim.agents.modalBehaviors.mulitnomialLogit.params.transit_crowding_percentile
          val occupancyLevel = transitCrowding.getTransitOccupancyLevelForPercentile(altAndIdx._1, percentile)
          val embodiedBeamTrip = altAndIdx._1
        ModeChoiceData(
          embodiedBeamTrip,
          tourType,
          mode,
          vehicleTime,
          waitTime,
          egressTime,
          originTransitProximity,
          destTransitProximity,
          numTransfers,
          walkDistance,
          walkTime,
          bikeDistance,
          bikeTime,
          dtDistance,
          destZDI,
          originZDI,
          destCBD,
          totalCost,
          altAndIdx._2,
          walkToTransitDistance
        )
      }
    modeChoiceAlternatives.toVector
  }

  override def utilityOf(
    alternative: EmbodiedBeamTrip,
    attributesOfIndividual: AttributesOfIndividual,
    destinationActivity: Option[Activity],
    person: Person
  ): Double = 0.0

  override def utilityOf(
    alternative: EmbodiedBeamTrip,
    attributesOfIndividual: AttributesOfIndividual,
    destinationActivity: Option[Activity],
    tourPurpose: String,
    person: Person
  ): Double = {
    val mcd = altsToBestInGroup(Vector(alternative), Mandatory).head
    utilityOf(mcd, tourPurpose, person, attributesOfIndividual)
  }

  private def utilityOf(
    mcd: ModeChoiceData,
    tourPurpose: String,
    person: Person,
    attributesOfIndividual: AttributesOfIndividual
  ): Double = {
    val beamTrip = mcd.embodiedBeamTrip
    val autoWork = person.getAttributes.getAttribute("autoWorkRatio").toString
    val vot = attributesOfIndividual.valueOfTime
    val age = attributesOfIndividual.age.get.asInstanceOf[Double]
    val hhSize = attributesOfIndividual.householdAttributes.householdSize.asInstanceOf[Double]
    val theParams = attributes(
      mcd.vehicleTime,
      mcd.waitTime,
      mcd.egressTime,
      if (mcd.originTransitProximity <= .5) {1.0} else {0.0},
      if (mcd.originTransitProximity > .5)  {1.0} else {0.0},
      if (mcd.destTransitProximity <= .5)   {1.0} else {0.0},
      if (mcd.destTransitProximity > .5)    {1.0} else {0.0},
      mcd.numTransfers,
      if (mcd.walkDistance <= 1.5) {mcd.walkDistance} else {0.0},
      if (mcd.walkDistance > 1.5) {mcd.walkDistance} else {0.0},
      if (mcd.bikeDistance <= 1.5) {mcd.bikeDistance} else {0.0},
      if (mcd.bikeDistance > 1.5) {mcd.bikeDistance} else {0.0},
      if (mcd.cost == 0.0 || vot == 0.0) {0.0} else {mcd.cost * 60 / vot},
      mcd.destZDI,
      mcd.originZDI,
      if (age >= 16.0 & age <= 19.0)  {age} else {0.0},
      if (age <= 10.0)                {age} else {0.0},
      if (age >= 16.0)                {age} else {0.0},
      if (hhSize == 1.0)              {hhSize} else {0.0},
      if (hhSize == 2.0)              {hhSize} else {0.0},
      mcd.dtDistance,
      mcd.destCBD,
      if (autoWork.equals("no_auto"))         {1.0} else {0.0},
      if (autoWork.equals("auto_deficient"))  {1.0} else {0.0},
      if (autoWork.equals("auto_sufficient")) {1.0} else {0.0},
      mcd.walkTime
    )
    val (model, modeModel) = lccm.modeChoiceTourModels(varType)(tourPurpose)
    model.getUtilityOfAlternative(beamTrip, theParams).getOrElse(0)
  }

  //fix this if you want to. ChangeModeForTour.scala is the only one that uses it though, and idk if that class is even called.
  def utilityOf(
    tourPurpose: String,
    mode: BeamMode,
    cost: Double,
    time: Double,
    numTransfers: Int = 0,
    transitOccupancyLevel: Double
  ): Double = {
    val theParams = attributes(
      time,//fix vehicle time
      time,//fix wait time
      time,//fix egress time
      0,0,0,0,//fix proximity to transit
      numTransfers,
      0,0,0,0, //fix walk-bike-dist
      cost,
      0,0, // fix ZTI & ZDI
      0,0,0, // fix age
      0,0, // fix hhSize
      0,0, // fix drive-dist and CBD
      0,0,0,//fix ascAuto
      time //fix walk time
    )
    val (model, modeModel) = lccm.modeChoiceTourModels(varType)(tourPurpose)
    modeModel.getUtilityOfAlternative(mode, theParams).getOrElse(0)
  }

  override def computeAllDayUtility(
    trips: ListBuffer[EmbodiedBeamTrip],
    person: Person,
    attributesOfIndividual: AttributesOfIndividual
  ): Double = {
    val scoreList = new ListBuffer[Double]
    trips.zipWithIndex map { tripWithIndex =>


      val (trip, tripIndex) = tripWithIndex
      val totalTravelTimeIn30mins = trip.totalTravelTimeInSecs/1800
      val tripPurpose = person.getSelectedPlan.getPlanElements.asScala
        .filter(_.isInstanceOf[Activity])
        .map(_.asInstanceOf[Activity])
        .lift(tripIndex + 1)
      val tourPurpose = person.getSelectedPlan.getPlanElements.asScala
        .filter(_.isInstanceOf[Activity])
        .map(_.asInstanceOf[Activity])
        .lift(tripIndex).get
        .getAttributes.getAttribute("primary_purpose")
        .toString.toLowerCase

      scoreList += utilityOf(trip, attributesOfIndividual, tripPurpose, tourPurpose, person) - totalTravelTimeIn30mins

    }
    scoreList.sum
  }


  case class ModeChoiceData(
    embodiedBeamTrip: EmbodiedBeamTrip,
    tourType: TourType,
    mode: BeamMode,
    vehicleTime: Double,
    waitTime: Double,
    egressTime: Double,
    originTransitProximity: Double,
    destTransitProximity: Double,
    numTransfers: Double,
    walkDistance: Double,
    walkTime: Double,
    bikeDistance: Double,
    bikeTime: Double,
    dtDistance: Double,
    destZDI: Double,
    originZDI: Double,
    destCBD: Double,
    cost: Double,
    index: Int = -1,
    walkToTransitDistance: String
  )
}
