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
  * --vehicleTime
  * --walkTime
  * --bikeTime
  * --waitTime
  * --transfer
  * --transitOccupancy
  * --cost
  *
  * The TPCM models are structured to differentiate between Mandatory and Nonmandatory trips, but for this preliminary
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

  override def apply(
    alternatives: IndexedSeq[EmbodiedBeamTrip],
    attributesOfIndividual: AttributesOfIndividual,
    destinationActivity: Option[Activity],
    person: Option[Person] = None,
    tourPurpose : String = "Work"
  ): Option[EmbodiedBeamTrip] = {
    choose(alternatives, tourPurpose)
  }

  private def choose(
    alternatives: IndexedSeq[EmbodiedBeamTrip],
    purpose: String
  ): Option[EmbodiedBeamTrip] = {
    if (alternatives.isEmpty) {
      None
    } else {
      val bestInGroup = altsToBestInGroup(alternatives, Mandatory)
      // Fill out the input data structures required by the MNL models
      val modeChoiceInputData = bestInGroup.map { alt =>
        val theParams = attributes(
          alt.cost,
          alt.walkTime + alt.bikeTime + alt.vehicleTime + alt.waitTime,
          alt.numTransfers,
          alt.occupancyLevel
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

  private def attributes(cost: Double, time: Double, numTransfers: Double, transitOccupancy: Double) = {
    Map(
      "cost"                  -> cost,
      "time"                  -> time,
      "transfer"              -> numTransfers.toDouble,
      "transitOccupancy"      -> transitOccupancy
    )
  }

  def altsToBestInGroup(
    alternatives: IndexedSeq[EmbodiedBeamTrip],
    tourType: TourType
  ): Vector[ModeChoiceData] = {
    val transitFareDefaults: Seq[Double] =
      TransitFareDefaults.estimateTransitFares(alternatives)
    val modeChoiceAlternatives: Seq[ModeChoiceData] =
      alternatives.zipWithIndex.map { altAndIdx =>
        val mode = altAndIdx._1.tripClassifier
        val totalCost = mode match {
          case TRANSIT | WALK_TRANSIT | DRIVE_TRANSIT | BIKE_TRANSIT =>
            (altAndIdx._1.costEstimate + transitFareDefaults(altAndIdx._2)) * beamServices.beamConfig.beam.agentsim.tuning.transitPrice
          case RIDE_HAIL =>
            altAndIdx._1.costEstimate * beamServices.beamConfig.beam.agentsim.tuning.rideHailPrice
          case _ =>
            altAndIdx._1.costEstimate
        }
        //TODO verify wait time is correct, look at transit and ride_hail in particular
        val walkTime = altAndIdx._1.legs.view
          .filter(_.beamLeg.mode == WALK)
          .map(_.beamLeg.duration)
          .sum
        val bikeTime = altAndIdx._1.legs.view
          .filter(_.beamLeg.mode == BIKE)
          .map(_.beamLeg.duration)
          .sum
        val vehicleTime = altAndIdx._1.legs.view
          .filter(_.beamLeg.mode != WALK)
          .filter(_.beamLeg.mode != BIKE)
          .map(_.beamLeg.duration)
          .sum
        val waitTime = altAndIdx._1.totalTravelTimeInSecs - walkTime - vehicleTime
        //TODO verify number of transfers is correct
        val numTransfers = mode match {
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
        assert(numTransfers >= 0)
        val percentile =
          beamConfig.beam.agentsim.agents.modalBehaviors.mulitnomialLogit.params.transit_crowding_percentile
        val occupancyLevel =
          transitCrowding.getTransitOccupancyLevelForPercentile(altAndIdx._1, percentile)
        val embodiedBeamTrip = altAndIdx._1
        ModeChoiceData(
          embodiedBeamTrip,
          mode,
          tourType,
          vehicleTime,
          walkTime,
          waitTime,
          bikeTime,
          numTransfers,
          occupancyLevel,
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
      mcd.walkTime + mcd.bikeTime + mcd.vehicleTime + mcd.waitTime,
      mcd.numTransfers,
      mcd.occupancyLevel
    )
    val (model, modeModel) = lccm.modeChoiceTourModels(Mandatory)(tourPurpose)
    model.getUtilityOfAlternative(beamTrip, theParams).getOrElse(0)
  }

  def utilityOf(
    tourPurpose: String = "Work",
    mode: BeamMode,
    cost: Double,
    time: Double,
    numTransfers: Int = 0,
    transitOccupancyLevel: Double
  ): Double = {
    val theParams = attributes(
      cost,
      time,
      numTransfers,
      transitOccupancyLevel
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
        .toString
      scoreList += utilityOf(trip, attributesOfIndividual, tripPurpose, tourPurpose)
    }
    scoreList.sum
  }

  case class ModeChoiceData(
    embodiedBeamTrip: EmbodiedBeamTrip,
    mode: BeamMode,
    tourType: TourType,
    vehicleTime: Double,
    walkTime: Double,
    waitTime: Double,
    bikeTime: Double,
    numTransfers: Double,
    occupancyLevel: Double,
    cost: Double,
    index: Int = -1
  )
}
