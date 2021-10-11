package beam.agentsim.agents.choice.mode

import beam.agentsim.agents.choice.logit.LatentClassChoiceModel
import beam.agentsim.agents.choice.logit.LatentClassChoiceModel.{Mandatory, TourType}
import beam.agentsim.agents.choice.logit.MultinomialLogit.MNLSample
import beam.agentsim.agents.choice.mode.ModeChoiceLCCM.ModeChoiceData
import beam.agentsim.agents.modalbehaviors.ModeChoiceCalculator
import beam.agentsim.agents.vehicles.BeamVehicle
import beam.router.Modes.BeamMode
import beam.router.Modes.BeamMode.{BIKE, BIKE_TRANSIT, DRIVE_TRANSIT, RIDE_HAIL, RIDE_HAIL_TRANSIT, TRANSIT, WALK, WALK_TRANSIT}
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
  * --cost
  * --vehicleTime
  * --waitTime
  * --egressTime
  * --transfer
  * --walkDistance
  * --bikeDistance
  *
  * The TPCM model is based on the LCCM model so it can differentiate between Mandatory and Nonmandatory trips, but for this
  * implementation only the Mandatory models is used...
  * TODO pass TourType so correct model can be applied
  */
class ModeChoiceTPCM(
  val beamServices: BeamServices,
  val lccm: LatentClassChoiceModel,
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
    val income = person.get.getAttributes.getAttribute("income")
    val autoWork = person.get.getAttributes.getAttribute("autoWorkRatio").toString
    choose(alternatives, tourPurpose, autoWork)
  }

  private def choose(
    alternatives: IndexedSeq[EmbodiedBeamTrip],
    purpose: String,
    autoWork: String
  ): Option[EmbodiedBeamTrip] = {
    if (alternatives.isEmpty) {
      None
    } else {
      val bestInGroup = altsToBestInGroup(alternatives, Mandatory)
      // Fill out the input data structures required by the MNL models
      val modeChoiceInputData = bestInGroup.map { alt =>
        val theParams = attributes(
          alt.cost,
          alt.vehicleTime,
          alt.waitTime,
          alt.egressTime,
          alt.numTransfers,
          if (alt.walkDistance <= 1.5) {alt.walkDistance} else {0.0},
          if (alt.walkDistance > 1.5)  {alt.walkDistance} else {0.0},
          if (alt.bikeDistance <= 1.5) {alt.bikeDistance} else {0.0},
          if (alt.bikeDistance > 1.5)  {alt.bikeDistance} else {0.0},
          if (autoWork.equals("no_auto"))         {1.0} else{0.0},
          if (autoWork.equals("auto_deficient"))  {1.0} else{0.0},
          if (autoWork.equals("auto_sufficient")) {1.0} else{0.0}
        )
        (alt.mode, theParams)
      }.toMap

      // Evaluate and sample from mode choice model
      val (model, modeModel) = lccm.modeChoiceTourModels(Mandatory)(purpose)
      val chosenModeOpt = modeModel.sampleAlternative(modeChoiceInputData, new Random())
      expectedMaximumUtility = modeModel.getExpectedMaximumUtility(modeChoiceInputData).getOrElse(0)

      chosenModeOpt match {
        case Some(chosenMode) =>
          val chosenAlt = bestInGroup.filter(_.mode.value.equalsIgnoreCase(chosenMode.alternativeType.value))
          if (chosenAlt.isEmpty) {
            None
          } else {
            Some(alternatives(chosenAlt.head.index))
          }
        case None =>
          None
      }
    }
  }

  private def attributes(
    cost: Double,
    vehicleTime: Double,
    waitTime: Double,
    egressTime: Double,
    numTransfers: Double,
    shortWalkDist: Double,
    longWalkDist: Double,
    shortBikeDist: Double,
    longBikeDist: Double,
    ascNoAuto: Double,
    ascAutoDeficient: Double,
    ascAutoSufficient: Double
  ) = {
    Map(
      "cost"                  -> cost,
      "vehicleTime"           -> vehicleTime,
      "waitTime"              -> waitTime,
      "egressTime"            -> egressTime,
      "transfer"              -> numTransfers.toDouble,
      "shortWalkDist"         -> shortWalkDist,
      "longWalkDist"          -> longWalkDist,
      "shortBikeDist"         -> shortBikeDist,
      "longBikeDist"          -> longBikeDist,
      "ascNoAuto"             -> ascNoAuto,
      "ascAutoDeficient"      -> ascAutoDeficient,
      "ascAutoSufficient"     -> ascAutoSufficient
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
          //val ivttMode = TPCMCalculator.getIVTTMode(mode, altAndIdx)
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
        //determine distance for walk or bike modes
          val walkDistance = TPCMCalculator.getWalkDistance(mode, altAndIdx)
          val bikeDistance = TPCMCalculator.getBikeDistance(mode, altAndIdx)
        //determine percentile, occupancy level, and embodied trip value
          val percentile = beamConfig.beam.agentsim.agents.modalBehaviors.mulitnomialLogit.params.transit_crowding_percentile
          val occupancyLevel = transitCrowding.getTransitOccupancyLevelForPercentile(altAndIdx._1, percentile)
          val embodiedBeamTrip = altAndIdx._1
        ModeChoiceData(
          embodiedBeamTrip,
          tourType,
          mode,
          vehicleTime,
          walkTime,
          waitTime,
          bikeTime,
          egressTime,
          numTransfers,
          walkDistance,
          bikeDistance,
          totalCost.toDouble,
          altAndIdx._2
        )
      }
    val groupedByMode: Map[BeamMode, Seq[ModeChoiceData]] =
      modeChoiceAlternatives.groupBy(_.mode)
    val bestInGroup = groupedByMode.map {
      case (_, alts) =>
        // Which dominates at $18/hr for total time
        alts
          .map { alt =>
            (
              (alt.vehicleTime + alt.walkTime + alt.waitTime + alt.bikeTime) / 3600 * 18 + alt.cost,
              alt
            )
          }
          .minBy(_._1)
          ._2
    }
    bestInGroup.toVector
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
    tourPurpose: String
  ): Double = {
    val mcd = altsToBestInGroup(Vector(alternative), Mandatory).head
    utilityOf(mcd, tourPurpose)
  }

  private def utilityOf(
    mcd: ModeChoiceData,
    tourPurpose: String
  ): Double = {
    val beamTrip = mcd.embodiedBeamTrip
    val theParams = attributes(
      mcd.cost,
      mcd.vehicleTime,
      mcd.waitTime,
      mcd.egressTime,
      mcd.numTransfers,
      if (mcd.walkDistance <= 1.5) {mcd.walkDistance} else {0.0},
      if (mcd.walkDistance > 1.5) {mcd.walkDistance} else {0.0},
      if (mcd.bikeDistance <= 1.5) {mcd.bikeDistance} else {0.0},
      if (mcd.bikeDistance > 1.5) {mcd.bikeDistance} else {0.0},
      0,0,0
    )
    val (model, modeModel) = lccm.modeChoiceTourModels(Mandatory)(tourPurpose)
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
      cost,
      time,//fix this
      time,//fix this
      time,//fix this
      numTransfers,
      0,0,0,0,//fix this
      0,0,0//fix this
    )
    val (model, modeModel) = lccm.modeChoiceTourModels(Mandatory)(tourPurpose)
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
      scoreList += utilityOf(trip, attributesOfIndividual, tripPurpose, tourPurpose)
    }
    scoreList.sum
  }

  case class ModeChoiceData(
    embodiedBeamTrip: EmbodiedBeamTrip,
    tourType: TourType,
    mode: BeamMode,
    vehicleTime: Double,
    walkTime: Double,
    waitTime: Double,
    bikeTime: Double,
    egressTime: Double,
    numTransfers: Double,
    walkDistance: Double,
    bikeDistance: Double,
    cost: Double,
    index: Int = -1
  )
}
