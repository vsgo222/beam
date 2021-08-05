package beam.agentsim.agents.choice.mode

import beam.agentsim.agents.choice.logit.LatentClassChoiceModel.{Mandatory, TourType}
import beam.agentsim.agents.choice.logit.MultinomialLogit.MNLSample
import beam.agentsim.agents.choice.logit.LatentClassChoiceModel
import beam.agentsim.agents.choice.mode.ModeChoiceLCCM.ModeChoiceData
import beam.agentsim.agents.modalbehaviors.ModeChoiceCalculator
import beam.router.Modes.BeamMode
import beam.router.Modes.BeamMode.{BIKE, BIKE_TRANSIT, DRIVE_TRANSIT, RIDE_HAIL, TRANSIT, WALK, WALK_TRANSIT}
import beam.router.model.EmbodiedBeamTrip
import beam.sim.config.BeamConfig
import beam.sim.{BeamServices, MapStringDouble}
import beam.sim.population.AttributesOfIndividual
import org.matsim.api.core.v01.population.Activity
import org.matsim.api.core.v01.population.Person

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import beam.agentsim.agents.choice.logit
import beam.agentsim.agents.choice.logit._
import beam.agentsim.agents.choice.mode.ModeChoiceMultinomialLogit.{ModeCostTimeTransfer, _}
import beam.agentsim.agents.modalbehaviors.ModeChoiceCalculator
import beam.agentsim.agents.modalbehaviors.ModeChoiceCalculator._
import beam.agentsim.agents.vehicles.BeamVehicle
import beam.agentsim.events.ModeChoiceOccurredEvent
import beam.router.Modes.BeamMode
import beam.router.Modes.BeamMode._
import beam.router.model.{BeamPath, EmbodiedBeamLeg, EmbodiedBeamTrip}
import beam.router.r5.BikeLanesAdjustment
import beam.router.skim.readonly.TransitCrowdingSkims
import beam.sim.BeamServices
import beam.sim.config.{BeamConfig, BeamConfigHolder}
import beam.sim.config.BeamConfig.Beam.Agentsim.Agents.ModalBehaviors
import beam.sim.population.AttributesOfIndividual
import beam.utils.logging.ExponentialLazyLogging
import org.matsim.api.core.v01.Id
import org.matsim.api.core.v01.population.{Activity, Person}
import org.matsim.core.api.experimental.events.EventsManager

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import scala.util.Random

class ModeChoiceTest(
   val beamServices: BeamServices,
   val lccm: LatentClassChoiceModel,
   beamConfigHolder: BeamConfigHolder,
   transitCrowding: TransitCrowdingSkims,
   val eventsManager: EventsManager
  ) extends ModeChoiceCalculator
        with ExponentialLazyLogging{

  override lazy val beamConfig: BeamConfig = beamServices.beamConfig
  val modalBehaviors: ModalBehaviors = beamConfig.beam.agentsim.agents.modalBehaviors

  var expectedMaximumUtility: Double = Double.NaN

  private val shouldLogDetails: Boolean = false
  private val bikeLanesAdjustment: BikeLanesAdjustment = beamServices.bikeLanesAdjustment

  override def apply(
    alternatives: IndexedSeq[EmbodiedBeamTrip],
    attributesOfIndividual: AttributesOfIndividual,
    destinationActivity: Option[Activity],
    person: Option[Person] = None,
    tourPurpose : String = "Work"
  ): Option[EmbodiedBeamTrip] = {
    choose(alternatives, attributesOfIndividual, destinationActivity, person, tourPurpose)
  }

  private def determinePlanPurpose(
    person : Person
  ) : String = {
    val activities: List[Activity] = person.getSelectedPlan.getPlanElements.asScala
      .collect{ case activity: Activity => activity }.toList
    val types: List[String] = activities.map(_.getType)
    if (types.contains("Work")) {
      "Work"
    } else("Nonwork")
  }

  private def choose(
    alternatives: IndexedSeq[EmbodiedBeamTrip],
    attributesOfIndividual: AttributesOfIndividual,
    destinationActivity: Option[Activity],
    person: Option[Person],
    purpose: String
  ): Option[EmbodiedBeamTrip] = {
    if (alternatives.isEmpty) {
      None
    } else {
      val (model, modeModel) = lccm.modeChoiceModels(Mandatory)(purpose)

      val modeCostTimeTransfers = altsToModeCostTimeTransfers(alternatives, attributesOfIndividual, destinationActivity)

      val bestInGroup = modeCostTimeTransfers groupBy (_.embodiedBeamTrip.tripClassifier) map {
        case (_, group) => findBestIn(group, model)
      }

      val inputData = bestInGroup.map { mct =>
        val theParams: Map[String, Double] =
          Map("cost" -> timeAndCost(mct))
        val transferParam: Map[String, Double] = if (mct.embodiedBeamTrip.tripClassifier.isTransit) {
          Map("transfer" -> mct.numTransfers, "transitOccupancyLevel" -> mct.transitOccupancyLevel)
        } else {
          Map()
        }
        (mct.embodiedBeamTrip, theParams ++ transferParam)
      }.toMap

      val alternativesWithUtility = model.calcAlternativesWithUtility(inputData)
      val chosenModeOpt = model.sampleAlternative(alternativesWithUtility, random)

      expectedMaximumUtility = model.getExpectedMaximumUtility(inputData).getOrElse(0)

      if (shouldLogDetails) {
        val personId = person.map(_.getId)
        val msgToLog =
          s"""|@@@[$personId]-----------------------------------------
              |@@@[$personId]Alternatives:${alternatives}
              |@@@[$personId]AttributesOfIndividual:${attributesOfIndividual}
              |@@@[$personId]DestinationActivity:${destinationActivity}
              |@@@[$personId]modeCostTimeTransfers:$modeCostTimeTransfers
              |@@@[$personId]bestInGroup:$bestInGroup
              |@@@[$personId]inputData:$inputData
              |@@@[$personId]chosenModeOpt:${chosenModeOpt}
              |@@@[$personId]expectedMaximumUtility:${expectedMaximumUtility}
              |@@@[$personId]-----------------------------------------
              |""".stripMargin
        logger.debug(msgToLog)
      }

      chosenModeOpt match {
        case Some(chosenMode) =>
          val chosenModeCostTime =
            bestInGroup.filter(_.embodiedBeamTrip == chosenMode.alternativeType)
          if (chosenModeCostTime.isEmpty || chosenModeCostTime.head.index < 0) {
            None
          } else {
            if (beamServices.beamConfig.beam.debug.writeModeChoiceAlternatives)
              createModeChoiceOccurredEvent(
                person,
                alternativesWithUtility,
                modeCostTimeTransfers,
                alternatives,
                chosenModeCostTime
              ).foreach(eventsManager.processEvent)

            Some(alternatives(chosenModeCostTime.head.index))
          }
        case None =>
          None
      }
    }
  }

  private def findBestIn(
    group: IndexedSeq[ModeCostTimeTransfer],
    model: MultinomialLogit[EmbodiedBeamTrip,String]
  ): ModeCostTimeTransfer = {
    if (group.size == 1) {
      group.head
    } else if (group.head.embodiedBeamTrip.tripClassifier.isTransit) {
      val inputData = group
        .map(
          mct => mct.embodiedBeamTrip -> attributes(timeAndCost(mct), mct.transitOccupancyLevel, mct.numTransfers)
        )
        .toMap
      val alternativesWithUtility = model.calcAlternativesWithUtility(inputData)
      val chosenModeOpt = model.sampleAlternative(alternativesWithUtility, random)
      chosenModeOpt
        .flatMap(sample => group.find(_.embodiedBeamTrip == sample.alternativeType))
        .getOrElse(group minBy timeAndCost)
    } else {
      group minBy timeAndCost
    }
  }

  private def attributes(cost: Double, transitOccupancyLevel: Double, numTransfers: Int) = {
    Map(
      "transfer"              -> numTransfers.toDouble,
      "cost"                  -> cost,
      "transitOccupancyLevel" -> transitOccupancyLevel
    )
  }

  private def createModeChoiceOccurredEvent(
     person: Option[Person],
     alternativesWithUtility: Iterable[MultinomialLogit.AlternativeWithUtility[EmbodiedBeamTrip]],
     modeCostTimeTransfers: IndexedSeq[ModeCostTimeTransfer],
     alternatives: IndexedSeq[EmbodiedBeamTrip],
     chosenModeCostTime: Iterable[ModeCostTimeTransfer]
  ): Option[ModeChoiceOccurredEvent] = {
    person match {
      case Some(p) =>
        val altUtility = alternativesWithUtility
          .map(
            au =>
              au.alternative.tripClassifier.value.toLowerCase() -> ModeChoiceOccurredEvent
                .AltUtility(au.utility, au.expUtility)
          )
          .toMap

        val altCostTimeTransfer = modeCostTimeTransfers
          .map(
            mctt =>
              mctt.embodiedBeamTrip.tripClassifier.value.toLowerCase() -> ModeChoiceOccurredEvent
                .AltCostTimeTransfer(mctt.cost, mctt.scaledTime, mctt.numTransfers)
          )
          .toMap

        val time = alternatives.headOption match {
          case Some(trip) =>
            trip.legs.headOption match {
              case Some(leg) => Some(leg.beamLeg.startTime)
              case None      => None
            }
          case None => None
        }

        if (time.nonEmpty) {
          Some(
            ModeChoiceOccurredEvent(
              time.get,
              p.getId.toString,
              alternatives,
              altCostTimeTransfer,
              altUtility,
              chosenModeCostTime.head.index
            )
          )
        } else {
          None
        }

      case _ => None
    }
  }

  private def timeAndCost(mct: ModeCostTimeTransfer): Double = {
    mct.scaledTime + mct.cost
  }

  private def getGeneralizedTimeOfTripInHours(
    embodiedBeamTrip: EmbodiedBeamTrip,
    attributesOfIndividual: Option[AttributesOfIndividual],
    destinationActivity: Option[Activity],
    adjustSpecialBikeLines: Boolean = false
  ): Double = {
    val adjustedTripDuration = if (adjustSpecialBikeLines && embodiedBeamTrip.tripClassifier == BIKE) {
      calculateBeamTripTimeInSecsWithSpecialBikeLanesAdjustment(embodiedBeamTrip, bikeLanesAdjustment)
    } else {
      embodiedBeamTrip.legs.map(_.beamLeg.duration).sum
    }
    val waitingTime: Int = embodiedBeamTrip.totalTravelTimeInSecs - adjustedTripDuration
    embodiedBeamTrip.legs.map { x: EmbodiedBeamLeg =>
      val factor = if (adjustSpecialBikeLines) {
        bikeLanesAdjustment.scaleFactor(x.beamLeg.mode, adjustSpecialBikeLines)
      } else {
        1D
      }
      getGeneralizedTimeOfLeg(x, attributesOfIndividual, destinationActivity) * factor
    }.sum + getGeneralizedTime(waitingTime, None, None)
  }

  private def altsToModeCostTimeTransfers(
    alternatives: IndexedSeq[EmbodiedBeamTrip],
    attributesOfIndividual: AttributesOfIndividual,
    destinationActivity: Option[Activity]
  ): IndexedSeq[ModeCostTimeTransfer] = {
    alternatives.zipWithIndex.map { altAndIdx =>
      val mode = altAndIdx._1.tripClassifier
      val totalCost = getNonTimeCost(altAndIdx._1, includeReplanningPenalty = true)
      val incentive: Double = beamServices.beamScenario.modeIncentives.computeIncentive(attributesOfIndividual, mode)

      val incentivizedCost = Math.max(0, totalCost.toDouble - incentive)

      if (totalCost < incentive)
        logger.warn(
          "Mode incentive is even higher then the cost, setting cost to zero. Mode: {}, Cost: {}, Incentive: {}",
          mode,
          totalCost,
          incentive
        )

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
      val scaledTime = attributesOfIndividual.getVOT(
        getGeneralizedTimeOfTripInHours(
          altAndIdx._1,
          Some(attributesOfIndividual),
          destinationActivity,
          adjustSpecialBikeLines = true
        )
      )

      val percentile =
        beamConfig.beam.agentsim.agents.modalBehaviors.mulitnomialLogit.params.transit_crowding_percentile
      val occupancyLevel: Double = transitCrowding.getTransitOccupancyLevelForPercentile(altAndIdx._1, percentile)

      ModeCostTimeTransfer(
        embodiedBeamTrip = altAndIdx._1,
        cost = incentivizedCost,
        scaledTime = scaledTime,
        numTransfers = numTransfers,
        transitOccupancyLevel = occupancyLevel,
        index = altAndIdx._2
      )
    }
  }

  override def utilityOf(
     alternative: EmbodiedBeamTrip,
     attributesOfIndividual: AttributesOfIndividual,
     destinationActivity: Option[Activity],
     tourPurpose: String
  ): Double = {
    val modeCostTimeTransfer =
      altsToModeCostTimeTransfers(IndexedSeq(alternative), attributesOfIndividual, destinationActivity).head
    utilityOf(modeCostTimeTransfer, tourPurpose)
  }

  override def utilityOf(
    alternative: EmbodiedBeamTrip,
    attributesOfIndividual: AttributesOfIndividual,
    destinationActivity: Option[Activity],
    person: Person
  ): Double = {
    val modeCostTimeTransfer =
      altsToModeCostTimeTransfers(IndexedSeq(alternative), attributesOfIndividual, destinationActivity).head
    utilityOf2(modeCostTimeTransfer, person)
  }

  private def utilityOf(
     mct: ModeCostTimeTransfer,
     tourPurpose: String
  ): Double = {
    val (model, modeModel) = lccm.modeChoiceModels(Mandatory)(tourPurpose)
    model
      .getUtilityOfAlternative(mct.embodiedBeamTrip, attributes(mct.cost, mct.transitOccupancyLevel, mct.numTransfers))
      .getOrElse(0)
  }

  private def utilityOf2(
    mct: ModeCostTimeTransfer,
    person: Person
  ): Double = {
    val planPurpose = determinePlanPurpose(person)
    val (model, modeModel) = lccm.modeChoiceModels(Mandatory)(planPurpose)
    model
      .getUtilityOfAlternative(mct.embodiedBeamTrip, attributes(mct.cost, mct.transitOccupancyLevel, mct.numTransfers))
      .getOrElse(0)
  }

  override def utilityOf(
    person: Person,
    mode: BeamMode,
    cost: Double,
    time: Double,
    numTransfers: Int = 0,
    transitOccupancyLevel: Double
  ): Double = {
    val planPurpose = determinePlanPurpose(person)
    val (model, modeModel) = lccm.modeChoiceModels(Mandatory)(planPurpose)
    modeModel.getUtilityOfAlternative(mode, attributes(cost, transitOccupancyLevel, numTransfers)).getOrElse(0)
  }

  override def computeAllDayUtility(
    trips: ListBuffer[EmbodiedBeamTrip],
    person: Person,
    attributesOfIndividual: AttributesOfIndividual
  ): Double = trips.map(utilityOf(_, attributesOfIndividual, None, person)).sum // TODO: Update with destination & origin activity

  case class ModeCostTimeTransfer(
    embodiedBeamTrip: EmbodiedBeamTrip,
    cost: Double,
    scaledTime: Double,
    numTransfers: Int,
    transitOccupancyLevel: Double,
    index: Int = -1
  )


}