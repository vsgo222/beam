package beam.agentsim.agents

import akka.actor.FSM.Failure
import akka.actor.{ActorRef, FSM, Props, Stash, Status}
import beam.agentsim.Resource._
import beam.agentsim.agents.BeamAgent._
import beam.agentsim.agents.PersonAgent._
import beam.agentsim.agents.household.HouseholdActor.ReleaseVehicle
import beam.agentsim.agents.household.HouseholdCAVDriverAgent
import beam.agentsim.agents.modalbehaviors.ChoosesMode.ChoosesModeData
import beam.agentsim.agents.modalbehaviors.DrivesVehicle._
import beam.agentsim.agents.modalbehaviors.{ChoosesMode, DrivesVehicle, ModeChoiceCalculator}
import beam.agentsim.agents.parking.ChoosesParking
import beam.agentsim.agents.parking.ChoosesParking.{ChoosingParkingSpot, ReleasingParkingSpot}
import beam.agentsim.agents.planning.{BeamPlan, Tour, Trip}
import beam.agentsim.agents.ridehail.RideHailManager.TravelProposal
import beam.agentsim.agents.ridehail._
import beam.agentsim.agents.vehicles.BeamVehicle.FuelConsumed
import beam.agentsim.agents.vehicles.EnergyEconomyAttributes.Powertrain
import beam.agentsim.agents.vehicles.VehicleCategory.Bike
import beam.agentsim.agents.vehicles._
import beam.agentsim.events._
import beam.agentsim.events.resources.{ReservationError, ReservationErrorCode}
import beam.agentsim.infrastructure.ChargingNetworkManager.{
  EndingRefuelSession,
  StartingRefuelSession,
  UnhandledVehicle,
  WaitingToCharge
}
import beam.agentsim.infrastructure.parking.ParkingMNL
import beam.agentsim.infrastructure.{ParkingInquiryResponse, ParkingStall}
import beam.agentsim.scheduler.BeamAgentScheduler.{CompletionNotice, IllegalTriggerGoToError, ScheduleTrigger}
import beam.agentsim.scheduler.Trigger.TriggerWithId
import beam.agentsim.scheduler.{BeamAgentSchedulerTimer, Trigger}
import beam.router.Modes.BeamMode
import beam.router.Modes.BeamMode.{CAR, CAV, RIDE_HAIL, RIDE_HAIL_POOLED, RIDE_HAIL_TRANSIT, WALK, WALK_TRANSIT}
import beam.router.RouteHistory
import beam.router.model.{EmbodiedBeamLeg, EmbodiedBeamTrip}
import beam.router.osm.TollCalculator
import beam.router.skim.ActivitySimSkimmerEvent
import beam.router.skim.event.{DriveTimeSkimmerEvent, ODSkimmerEvent}
import beam.sim.common.GeoUtils
import beam.sim.config.BeamConfig.Beam.Debug
import beam.sim.population.AttributesOfIndividual
import beam.sim.{BeamScenario, BeamServices, Geofence}
import beam.utils.NetworkHelper
import beam.utils.logging.ExponentialLazyLogging
import com.conveyal.r5.transit.TransportNetwork
import com.vividsolutions.jts.geom.Envelope
import org.matsim.api.core.v01.Id
import org.matsim.api.core.v01.events._
import org.matsim.api.core.v01.population._
import org.matsim.core.api.experimental.events.{EventsManager, TeleportationArrivalEvent}
import org.matsim.core.utils.misc.Time

import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec
import scala.concurrent.duration._

/**
  */
object PersonAgent {

  type VehicleStack = Vector[Id[BeamVehicle]]

  def props(
    scheduler: ActorRef,
    services: BeamServices,
    beamScenario: BeamScenario,
    modeChoiceCalculator: ModeChoiceCalculator,
    transportNetwork: TransportNetwork,
    tollCalculator: TollCalculator,
    router: ActorRef,
    rideHailManager: ActorRef,
    parkingManager: ActorRef,
    chargingNetworkManager: ActorRef,
    eventsManager: EventsManager,
    personId: Id[PersonAgent],
    householdRef: ActorRef,
    plan: Plan,
    fleetManagers: Seq[ActorRef],
    sharedVehicleFleets: Seq[ActorRef],
    possibleSharedVehicleTypes: Set[BeamVehicleType],
    routeHistory: RouteHistory,
    boundingBox: Envelope
  ): Props = {
    Props(
      new PersonAgent(
        scheduler,
        services,
        beamScenario,
        modeChoiceCalculator,
        transportNetwork,
        router,
        rideHailManager,
        eventsManager,
        personId,
        plan,
        parkingManager,
        chargingNetworkManager,
        tollCalculator,
        householdRef,
        fleetManagers,
        sharedVehicleFleets,
        possibleSharedVehicleTypes,
        routeHistory,
        boundingBox
      )
    )
  }

  sealed trait Traveling extends BeamAgentState

  trait PersonData extends DrivingData

  trait DrivingData {
    def currentVehicle: VehicleStack

    def passengerSchedule: PassengerSchedule

    def currentLegPassengerScheduleIndex: Int

    def withPassengerSchedule(newPassengerSchedule: PassengerSchedule): DrivingData

    def withCurrentLegPassengerScheduleIndex(currentLegPassengerScheduleIndex: Int): DrivingData

    def hasParkingBehaviors: Boolean

    def geofence: Option[Geofence]

    def legStartsAt: Option[Int]
  }

  case class LiterallyDrivingData(delegate: DrivingData, legEndsAt: Double, legStartsAt: Option[Int])
      extends DrivingData { // sorry
    def currentVehicle: VehicleStack = delegate.currentVehicle

    def passengerSchedule: PassengerSchedule = delegate.passengerSchedule

    def currentLegPassengerScheduleIndex: Int =
      delegate.currentLegPassengerScheduleIndex

    def withPassengerSchedule(newPassengerSchedule: PassengerSchedule): DrivingData =
      LiterallyDrivingData(delegate.withPassengerSchedule(newPassengerSchedule), legEndsAt, legStartsAt)

    def withCurrentLegPassengerScheduleIndex(currentLegPassengerScheduleIndex: Int): DrivingData =
      LiterallyDrivingData(
        delegate.withCurrentLegPassengerScheduleIndex(currentLegPassengerScheduleIndex),
        legEndsAt,
        legStartsAt
      )

    override def hasParkingBehaviors: Boolean = false

    override def geofence: Option[Geofence] = delegate.geofence
  }

  case class BasePersonData(
    currentActivityIndex: Int = 0,
    currentTrip: Option[EmbodiedBeamTrip] = None,
    restOfCurrentTrip: List[EmbodiedBeamLeg] = List(),
    currentVehicle: VehicleStack = Vector(),
    currentTourMode: Option[BeamMode] = None,
    currentTourPersonalVehicle: Option[Id[BeamVehicle]] = None,
    passengerSchedule: PassengerSchedule = PassengerSchedule(),
    currentLegPassengerScheduleIndex: Int = 0,
    hasDeparted: Boolean = false,
    currentTripCosts: Double = 0.0,
    numberOfReplanningAttempts: Int = 0,
    lastUsedParkingStall: Option[ParkingStall] = None
  ) extends PersonData {

    override def withPassengerSchedule(newPassengerSchedule: PassengerSchedule): DrivingData =
      copy(passengerSchedule = newPassengerSchedule)

    override def withCurrentLegPassengerScheduleIndex(
      newLegPassengerScheduleIndex: Int
    ): DrivingData = copy(currentLegPassengerScheduleIndex = newLegPassengerScheduleIndex)

    override def hasParkingBehaviors: Boolean = true

    override def geofence: Option[Geofence] = None
    override def legStartsAt: Option[Int] = None
  }

  case class ActivityStartTrigger(tick: Int) extends Trigger

  case class ActivityEndTrigger(tick: Int) extends Trigger

  case class PersonDepartureTrigger(tick: Int) extends Trigger

  case object PerformingActivity extends BeamAgentState

  case object ChoosingMode extends Traveling

  case object WaitingForDeparture extends Traveling

  case object WaitingForReservationConfirmation extends Traveling

  case object Waiting extends Traveling

  case object ProcessingNextLegOrStartActivity extends Traveling

  case object TryingToBoardVehicle extends Traveling

  case object WaitingToDrive extends Traveling

  case object WaitingToDriveInterrupted extends Traveling

  case object PassengerScheduleEmpty extends Traveling

  case object PassengerScheduleEmptyInterrupted extends Traveling

  case object ReadyToChooseParking extends Traveling

  case object Moving extends Traveling

  case object Driving extends Traveling

  case object DrivingInterrupted extends Traveling

  def correctTripEndTime(
    trip: EmbodiedBeamTrip,
    endTime: Int,
    bodyVehicleId: Id[BeamVehicle],
    bodyVehicleTypeId: Id[BeamVehicleType]
  ): EmbodiedBeamTrip = {
    if (trip.tripClassifier != WALK && trip.tripClassifier != WALK_TRANSIT) {
      trip.copy(
        legs = trip.legs
          .dropRight(1) :+ EmbodiedBeamLeg
          .dummyLegAt(
            endTime - trip.legs.last.beamLeg.duration,
            bodyVehicleId,
            isLastLeg = true,
            trip.legs.dropRight(1).last.beamLeg.travelPath.endPoint.loc,
            WALK,
            bodyVehicleTypeId,
            asDriver = true,
            trip.legs.last.beamLeg.duration
          )
      )
    } else {
      trip
    }
  }
}

class PersonAgent(
  val scheduler: ActorRef,
  val beamServices: BeamServices,
  val beamScenario: BeamScenario,
  val modeChoiceCalculator: ModeChoiceCalculator,
  val transportNetwork: TransportNetwork,
  val router: ActorRef,
  val rideHailManager: ActorRef,
  val eventsManager: EventsManager,
  override val id: Id[PersonAgent],
  val matsimPlan: Plan,
  val parkingManager: ActorRef,
  val chargingNetworkManager: ActorRef,
  val tollCalculator: TollCalculator,
  val householdRef: ActorRef,
  val fleetManagers: Seq[ActorRef] = Vector(),
  val sharedVehicleFleets: Seq[ActorRef] = Vector(),
  val possibleSharedVehicleTypes: Set[BeamVehicleType] = Set.empty,
  val routeHistory: RouteHistory,
  val boundingBox: Envelope
) extends DrivesVehicle[PersonData]
    with ChoosesMode
    with ChoosesParking
    with Stash
    with ExponentialLazyLogging {

  override val eventBuilderActor: ActorRef = beamServices.eventBuilderActor
  implicit val debug: Debug = beamServices.beamConfig.beam.debug

  val networkHelper: NetworkHelper = beamServices.networkHelper
  val geo: GeoUtils = beamServices.geo

  val bodyType: BeamVehicleType = beamScenario.vehicleTypes(
    Id.create(beamScenario.beamConfig.beam.agentsim.agents.bodyType, classOf[BeamVehicleType])
  )

  val body: BeamVehicle = new BeamVehicle(
    BeamVehicle.createId(id, Some("body")),
    new Powertrain(bodyType.primaryFuelConsumptionInJoulePerMeter),
    bodyType,
    vehicleManagerId = new AtomicReference(VehicleManager.NoManager.managerId)
  )
  body.setManager(Some(self))
  beamVehicles.put(body.id, ActualVehicle(body))

  val vehicleFleets: Seq[ActorRef] = fleetManagers ++ sharedVehicleFleets

  val attributes: AttributesOfIndividual =
    matsimPlan.getPerson.getCustomAttributes
      .get("beam-attributes")
      .asInstanceOf[AttributesOfIndividual]

  val _experiencedBeamPlan: BeamPlan = BeamPlan(matsimPlan)

  var totFuelConsumed: FuelConsumed = FuelConsumed(0.0, 0.0)
  var curFuelConsumed: FuelConsumed = FuelConsumed(0.0, 0.0)

  def updateFuelConsumed(fuelOption: Option[FuelConsumed]): Unit = {
    val newFuelConsumed = fuelOption.getOrElse(FuelConsumed(0.0, 0.0))
    curFuelConsumed = FuelConsumed(
      curFuelConsumed.primaryFuel + newFuelConsumed.primaryFuel,
      curFuelConsumed.secondaryFuel + newFuelConsumed.secondaryFuel
    )
    totFuelConsumed = FuelConsumed(
      totFuelConsumed.primaryFuel + curFuelConsumed.primaryFuel,
      totFuelConsumed.secondaryFuel + curFuelConsumed.secondaryFuel
    )
  }

  def resetFuelConsumed(): Unit = curFuelConsumed = FuelConsumed(0.0, 0.0)

  override def logDepth: Int = beamScenario.beamConfig.beam.debug.actor.logDepth

  val lastTickOfSimulation: Int = Time
    .parseTime(beamScenario.beamConfig.beam.agentsim.endTime)
    .toInt - beamServices.beamConfig.beam.agentsim.schedulerParallelismWindow

  /**
    * identifies agents with remaining range which is smaller than their remaining tour
    *
    * @param personData current state data cast as a [[BasePersonData]]
    * @return true if they have enough fuel, or fuel type is not exhaustible
    */
  def calculateRemainingTripData(personData: BasePersonData): Option[ParkingMNL.RemainingTripData] = {

    val refuelNeeded: Boolean =
      currentBeamVehicle.isRefuelNeeded(
        beamScenario.beamConfig.beam.agentsim.agents.rideHail.human.refuelRequiredThresholdInMeters,
        beamScenario.beamConfig.beam.agentsim.agents.rideHail.human.noRefuelThresholdInMeters
      )

    if (refuelNeeded) {

      val primaryFuelLevelInJoules: Double = beamScenario
        .privateVehicles(personData.currentVehicle.head)
        .primaryFuelLevelInJoules

      val primaryFuelConsumptionInJoulePerMeter: Double =
        currentBeamVehicle.beamVehicleType.primaryFuelConsumptionInJoulePerMeter

      val remainingTourDist: Double = nextActivity(personData) match {
        case Some(nextAct) =>
          // in the case that we are headed "home", we need to motivate charging.
          // in order to induce range anxiety, we need to have agents consider
          // their tomorrow activities. the agent's first leg of the day
          // is used here to add distance to a "ghost activity" tomorrow morning
          // which is used in place of our real remaining tour distance of 0.0
          // which should help encourage residential end-of-day charging
          val tomorrowFirstLegDistance =
            if (nextAct.getType.toLowerCase == "home") {
              findFirstCarLegOfTrip(personData) match {
                case Some(carLeg) =>
                  carLeg.beamLeg.travelPath.distanceInM
                case None =>
                  0.0
              }
            } else 0.0

          val tripIndexOfElement = currentTour(personData)
            .tripIndexOfElement(nextAct)
            .getOrElse(throw new IllegalArgumentException(s"Element [$nextAct] not found"))
          val nextActIdx = tripIndexOfElement - 1
          currentTour(personData).trips
            .slice(nextActIdx, currentTour(personData).trips.length)
            .sliding(2, 1)
            .toList
            .foldLeft(tomorrowFirstLegDistance) { (sum, pair) =>
              sum + Math
                .ceil(
                  beamServices.skims.od_skimmer
                    .getTimeDistanceAndCost(
                      pair.head.activity.getCoord,
                      pair.last.activity.getCoord,
                      0,
                      CAR,
                      currentBeamVehicle.beamVehicleType.id,
                      currentBeamVehicle.beamVehicleType,
                      beamServices.beamScenario.fuelTypePrices(currentBeamVehicle.beamVehicleType.primaryFuelType)
                    )
                    .distance
                )
            }

        case None =>
          0.0
      }

      Some(
        ParkingMNL.RemainingTripData(
          primaryFuelLevelInJoules,
          primaryFuelConsumptionInJoulePerMeter,
          remainingTourDist,
          beamScenario.beamConfig.beam.agentsim.agents.parking.rangeAnxietyBuffer
        )
      )

    } else {
      None
    }
  }

  startWith(Uninitialized, BasePersonData())

  def scaleTimeByValueOfTime(timeInSeconds: Double): Double = {
    attributes.unitConversionVOTT(
      timeInSeconds
    ) // TODO: ZN, right now not mode specific. modal factors reside in ModeChoiceMultinomialLogit. Move somewhere else?
  }

  def currentTour(data: BasePersonData): Tour = {
    stateName match {
      case PerformingActivity =>
        _experiencedBeamPlan.getTourContaining(currentActivity(data))
      case _ =>
        _experiencedBeamPlan.getTourContaining(nextActivity(data).get)
    }
  }

  def currentTourType(data: BasePersonData) : String = {
    val tour = currentTour(data)
    val tourTypes = tour.trips.map(_.activity.getType)

    if ( tourTypes.contains("Work") || tourTypes.contains("work") ) {"Work"}
    else if ( tourTypes.contains("Univ") ) {"univ"}
    else if ( tourTypes.contains("School") ) {"school"}
    else if ( tourTypes.contains("Escort") ) {"escort"}
    else if ( tourTypes.contains("Shopping") ) {"shopping"}
    else if ( tourTypes.contains("Eatout") ) {"eatout"}
    else if ( tourTypes.contains("Social") ) {"social"}
    else if ( tourTypes.contains("Othmaint") || tourTypes.contains("Other") ) {"othmaint"}
    else if ( tourTypes.contains("Othdiscr") ) {"othdiscr"}
    else if ( tourTypes.contains("Atwork") ) {"atwork"}
    else {"Nonwork"}
  }

  def currentActivity(data: BasePersonData): Activity =
    _experiencedBeamPlan.activities(data.currentActivityIndex)

  def currentTrip(data: BasePersonData): Trip =
    _experiencedBeamPlan.trips(data.currentActivityIndex)

  def nextActivity(data: BasePersonData): Option[Activity] = {
    val ind = data.currentActivityIndex + 1
    if (ind < 0 || ind >= _experiencedBeamPlan.activities.length) {
      None
    } else {
      Some(_experiencedBeamPlan.activities(ind))
    }
  }

  def getNumOfActivities()= {
    _experiencedBeamPlan.activities.length
  }

  def findFirstCarLegOfTrip(data: BasePersonData): Option[EmbodiedBeamLeg] = {
    @tailrec
    def _find(remaining: IndexedSeq[EmbodiedBeamLeg]): Option[EmbodiedBeamLeg] = {
      if (remaining.isEmpty) None
      else if (remaining.head.beamLeg.mode == CAR) Some { remaining.head }
      else _find(remaining.tail)
    }
    for {
      trip <- data.currentTrip
      leg  <- _find(trip.legs)
    } yield {
      leg
    }
  }

  when(Uninitialized) { case Event(TriggerWithId(InitializeTrigger(_), triggerId), _) =>
    goto(Initialized) replying CompletionNotice(
      triggerId,
      Vector(ScheduleTrigger(ActivityStartTrigger(0), self))
    )
  }

  when(Initialized) { case Event(TriggerWithId(ActivityStartTrigger(tick), triggerId), data: BasePersonData) =>
    logDebug(s"starting at ${currentActivity(data).getType} @ $tick")
    goto(PerformingActivity) replying CompletionNotice(
      triggerId,
      Vector(ScheduleTrigger(ActivityEndTrigger(currentActivity(data).getEndTime.toInt), self))
    )
  }

  when(PerformingActivity) { case Event(TriggerWithId(ActivityEndTrigger(tick), triggerId), data: BasePersonData) =>
    nextActivity(data) match {
      case None =>
        logger.warn(s"didn't get nextActivity, PersonAgent:$id")
        stay replying CompletionNotice(triggerId)
      case Some(nextAct) =>
        logDebug(s"wants to go to ${nextAct.getType} @ $tick")
        holdTickAndTriggerId(tick, triggerId)
        val modeOfNextLeg = _experiencedBeamPlan.getPlanElements
          .get(_experiencedBeamPlan.getPlanElements.indexOf(nextAct) - 1) match {
          case leg: Leg =>
            BeamMode.fromString(leg.getMode)
          case _ => None
        }
        goto(ChoosingMode) using ChoosesModeData(
          personData = data.copy(
            // If the mode of the next leg is defined and is CAV, use it, otherwise,
            // If we don't have a current tour mode (i.e. are not on a tour aka at home),
            // use the mode of the next leg as the new tour mode.
            currentTourMode = modeOfNextLeg match {
              case Some(CAV) =>
                Some(CAV)
              case _ =>
                data.currentTourMode.orElse(modeOfNextLeg)
            },
            numberOfReplanningAttempts = 0
          ),
          SpaceTime(currentActivity(data).getCoord, _currentTick.get)
        )
    }
  }

  when(WaitingForDeparture) {

    /**
      * Callback from [[ChoosesMode]]
      */
    case Event(
          TriggerWithId(PersonDepartureTrigger(tick), triggerId),
          data @ BasePersonData(_, Some(currentTrip), _, _, _, _, _, _, false, _, _, _)
        ) =>
      // We end our activity when we actually leave, not when we decide to leave, i.e. when we look for a bus or
      // hail a ride. We stay at the party until our Uber is there.
      eventsManager.processEvent(
        new ActivityEndEvent(
          tick,
          id,
          currentActivity(data).getLinkId,
          currentActivity(data).getFacilityId,
          currentActivity(data).getType
        )
      )
      assert(currentActivity(data).getLinkId != null)
      eventsManager.processEvent(
        new PersonDepartureEvent(
          tick,
          id,
          currentActivity(data).getLinkId,
          currentTrip.tripClassifier.value
        )
      )
      holdTickAndTriggerId(tick, triggerId)
      goto(ProcessingNextLegOrStartActivity) using data.copy(hasDeparted = true)

    case Event(
          TriggerWithId(PersonDepartureTrigger(tick), triggerId),
          BasePersonData(_, _, restOfCurrentTrip, _, _, _, _, _, true, _, _, _)
        ) =>
      // We're coming back from replanning, i.e. we are already on the trip, so we don't throw a departure event
      logDebug(s"replanned to leg ${restOfCurrentTrip.head}")
      holdTickAndTriggerId(tick, triggerId)
      goto(ProcessingNextLegOrStartActivity)
  }

  def activityOrMessage(ind: Int, msg: String): Either[String, Activity] = {
    if (ind < 0 || ind >= _experiencedBeamPlan.activities.length) Left(msg)
    else Right(_experiencedBeamPlan.activities(ind))
  }

  def handleFailedRideHailReservation(
    error: ReservationError,
    response: RideHailResponse,
    data: BasePersonData
  ): State = {
    logDebug(s"replanning because ${error.errorCode}")
    val tick = _currentTick.getOrElse(response.request.departAt)
    val replanningReason = getReplanningReasonFrom(data, error.errorCode.entryName)
    eventsManager.processEvent(
      new RideHailReservationConfirmationEvent(
        tick,
        Id.createPersonId(id),
        RideHailReservationConfirmationEvent.typeWhenPooledIs(response.request.asPooled),
        Some(error.errorCode),
        response.request.requestTime.getOrElse(response.request.departAt),
        response.request.departAt,
        response.request.quotedWaitTime,
        beamServices.geo.utm2Wgs(response.request.pickUpLocationUTM),
        beamServices.geo.utm2Wgs(response.request.destinationUTM),
        None,
        response.directTripTravelProposal.map(_.travelDistanceForCustomer(bodyVehiclePersonId)),
        response.directTripTravelProposal.map(proposal =>
          proposal.travelTimeForCustomer(bodyVehiclePersonId) + proposal.timeToCustomer(bodyVehiclePersonId)
        )
      )
    )
    eventsManager.processEvent(new ReplanningEvent(tick, Id.createPersonId(id), replanningReason))
    goto(ChoosingMode) using ChoosesModeData(
      data.copy(currentTourMode = None, numberOfReplanningAttempts = data.numberOfReplanningAttempts + 1),
      currentLocation = SpaceTime(
        beamServices.geo.wgs2Utm(data.restOfCurrentTrip.head.beamLeg.travelPath.startPoint).loc,
        tick
      ),
      isWithinTripReplanning = true,
      excludeModes = if (data.numberOfReplanningAttempts > 0) { Vector(RIDE_HAIL, RIDE_HAIL_POOLED, RIDE_HAIL_TRANSIT) }
      else {
        Vector()
      }
    )
  }

  when(WaitingForReservationConfirmation) {
    // TRANSIT SUCCESS
    case Event(ReservationResponse(Right(response), _), data: BasePersonData) =>
      handleSuccessfulReservation(response.triggersToSchedule, data)
    // TRANSIT FAILURE
    case Event(
          ReservationResponse(Left(firstErrorResponse), _),
          data @ BasePersonData(_, _, nextLeg :: _, _, _, _, _, _, _, _, _, _)
        ) =>
      logDebug(s"replanning because ${firstErrorResponse.errorCode}")

      val replanningReason = getReplanningReasonFrom(data, firstErrorResponse.errorCode.entryName)
      eventsManager.processEvent(
        new ReplanningEvent(_currentTick.get, Id.createPersonId(id), replanningReason)
      )
      goto(ChoosingMode) using ChoosesModeData(
        data.copy(numberOfReplanningAttempts = data.numberOfReplanningAttempts + 1),
        currentLocation =
          SpaceTime(beamServices.geo.wgs2Utm(nextLeg.beamLeg.travelPath.startPoint).loc, _currentTick.get),
        isWithinTripReplanning = true
      )
    // RIDE HAIL DELAY
    case Event(DelayedRideHailResponse, data: BasePersonData) =>
      // this means ride hail manager is taking time to assign and we should complete our
      // current trigger and wait to be re-triggered by the manager
      val (_, triggerId) = releaseTickAndTriggerId()
      scheduler ! CompletionNotice(triggerId, Vector())
      stay() using data
    // RIDE HAIL DELAY FAILURE
    // we use trigger for this to get triggerId back into hands of the person
    case Event(
          TriggerWithId(RideHailResponseTrigger(tick, response @ RideHailResponse(_, _, Some(error), _, _)), triggerId),
          data: BasePersonData
        ) =>
      holdTickAndTriggerId(tick, triggerId)
      handleFailedRideHailReservation(error, response, data)
    // RIDE HAIL SUCCESS
    // no trigger needed here since we're going to Waiting anyway without any other actions needed
    case Event(
          RideHailResponse(req, travelProposal, None, triggersToSchedule, directTripTravelProposal),
          data: BasePersonData
        ) =>
      eventsManager.processEvent(
        new RideHailReservationConfirmationEvent(
          _currentTick.getOrElse(req.departAt).toDouble,
          Id.createPersonId(id),
          RideHailReservationConfirmationEvent.typeWhenPooledIs(req.asPooled),
          None,
          req.requestTime.getOrElse(req.departAt),
          req.departAt,
          req.quotedWaitTime,
          beamServices.geo.utm2Wgs(req.pickUpLocationUTM),
          beamServices.geo.utm2Wgs(req.destinationUTM),
          travelProposal.flatMap(
            _.passengerSchedule.legsWithPassenger(bodyVehiclePersonId).headOption.map(_.startTime)
          ),
          directTripTravelProposal.map(_.travelDistanceForCustomer(bodyVehiclePersonId)),
          directTripTravelProposal.map(_.travelTimeForCustomer(bodyVehiclePersonId))
        )
      )
      handleSuccessfulReservation(triggersToSchedule, data, travelProposal)
    // RIDE HAIL FAILURE
    case Event(
          response @ RideHailResponse(_, _, Some(error), _, _),
          data @ BasePersonData(_, _, _, _, _, _, _, _, _, _, _, _)
        ) =>
      handleFailedRideHailReservation(error, response, data)
  }

  when(Waiting) {
    /*
     * Learn as passenger that it is time to board the vehicle
     */
    case Event(
          TriggerWithId(BoardVehicleTrigger(tick, vehicleToEnter), triggerId),
          data @ BasePersonData(_, _, currentLeg :: _, currentVehicle, _, _, _, _, _, _, _, _)
        ) =>
      logDebug(s"PersonEntersVehicle: $vehicleToEnter @ $tick")
      eventsManager.processEvent(new PersonEntersVehicleEvent(tick, id, vehicleToEnter))

      if (currentLeg.cost > 0.0) {
        currentLeg.beamLeg.travelPath.transitStops.foreach { transitStopInfo =>
          // If it doesn't have transitStopInfo, it is not a transit but a ridehailing trip
          eventsManager.processEvent(new AgencyRevenueEvent(tick, transitStopInfo.agencyId, currentLeg.cost))
        }
        eventsManager.processEvent(
          new PersonCostEvent(
            tick,
            id,
            data.currentTrip.get.tripClassifier.value,
            0.0, // incentive applies to a whole trip and is accounted for at Arrival
            0.0, // only drivers pay tolls, if a toll is in the fare it's still a fare
            currentLeg.cost
          )
        )
      }

      goto(Moving) replying CompletionNotice(triggerId) using data.copy(
        currentVehicle = vehicleToEnter +: currentVehicle
      )
  }

  when(Moving) {
    /*
     * Learn as passenger that it is time to alight the vehicle
     */
    case Event(
          TriggerWithId(AlightVehicleTrigger(tick, vehicleToExit, energyConsumedOption), triggerId),
          data @ BasePersonData(_, _, _ :: restOfCurrentTrip, currentVehicle, _, _, _, _, _, _, _, _)
        ) if vehicleToExit.equals(currentVehicle.head) =>
      updateFuelConsumed(energyConsumedOption)
      logDebug(s"PersonLeavesVehicle: $vehicleToExit @ $tick")
      eventsManager.processEvent(new PersonLeavesVehicleEvent(tick, id, vehicleToExit))
      holdTickAndTriggerId(tick, triggerId)
      goto(ProcessingNextLegOrStartActivity) using data.copy(
        restOfCurrentTrip = restOfCurrentTrip.dropWhile(leg => leg.beamVehicleId == vehicleToExit),
        currentVehicle = currentVehicle.tail
      )
  }

  // Callback from DrivesVehicle. Analogous to AlightVehicleTrigger, but when driving ourselves.
  when(PassengerScheduleEmpty) {
    case Event(PassengerScheduleEmptyMessage(_, toll, triggerId, energyConsumedOption), data: BasePersonData) =>
      updateFuelConsumed(energyConsumedOption)
      val netTripCosts = data.currentTripCosts // This includes tolls because it comes from leg.cost
      if (toll > 0.0 || netTripCosts > 0.0)
        eventsManager.processEvent(
          new PersonCostEvent(
            _currentTick.get,
            matsimPlan.getPerson.getId,
            data.restOfCurrentTrip.head.beamLeg.mode.value,
            0.0,
            toll,
            netTripCosts // Again, includes tolls but "net" here means actual money paid by the person
          )
        )
      val dataForNextLegOrActivity = if (data.restOfCurrentTrip.head.unbecomeDriverOnCompletion) {
        data.copy(
          restOfCurrentTrip = data.restOfCurrentTrip.tail,
          currentVehicle = if (data.currentVehicle.size > 1) data.currentVehicle.tail else Vector(),
          currentTripCosts = 0.0
        )
      } else {
        data.copy(
          restOfCurrentTrip = data.restOfCurrentTrip.tail,
          currentVehicle = Vector(body.id),
          currentTripCosts = 0.0
        )
      }
      if (data.restOfCurrentTrip.head.unbecomeDriverOnCompletion) {
        val vehicleToExit = data.currentVehicle.head
        currentBeamVehicle.unsetDriver()
        nextNotifyVehicleResourceIdle.foreach(currentBeamVehicle.getManager.get ! _)
        eventsManager.processEvent(
          new PersonLeavesVehicleEvent(_currentTick.get, Id.createPersonId(id), vehicleToExit)
        )
        if (currentBeamVehicle != body) {
          if (currentBeamVehicle.beamVehicleType.vehicleCategory != Bike) {
            if (currentBeamVehicle.stall.isEmpty) logWarn("Expected currentBeamVehicle.stall to be defined.")
          }
          if (!currentBeamVehicle.isMustBeDrivenHome) {
            // Is a shared vehicle. Give it up.
            currentBeamVehicle.getManager.get ! ReleaseVehicle(currentBeamVehicle, triggerId)
            beamVehicles -= data.currentVehicle.head
          }
        }
      }
      goto(ProcessingNextLegOrStartActivity) using dataForNextLegOrActivity

  }

  when(ReadyToChooseParking, stateTimeout = Duration.Zero) {
    case Event(
          StateTimeout,
          data @ BasePersonData(_, _, completedLeg :: theRestOfCurrentTrip, _, _, _, _, _, _, _, currentCost, _)
        ) =>
      log.debug("ReadyToChooseParking, restoftrip: {}", theRestOfCurrentTrip.toString())
      goto(ChoosingParkingSpot) using data.copy(
        restOfCurrentTrip = theRestOfCurrentTrip,
        currentTripCosts = currentCost + completedLeg.cost
      )
  }

  onTransition { case _ -> _ =>
    unstashAll()
  }

  when(TryingToBoardVehicle) {
    case Event(Boarded(vehicle, _), _: BasePersonData) =>
      beamVehicles.put(vehicle.id, ActualVehicle(vehicle))
      potentiallyChargingBeamVehicles.remove(vehicle.id)
      goto(ProcessingNextLegOrStartActivity)
    case Event(NotAvailable(_), basePersonData: BasePersonData) =>
      log.debug("{} replanning because vehicle not available when trying to board")
      val replanningReason = getReplanningReasonFrom(basePersonData, ReservationErrorCode.ResourceUnavailable.entryName)
      eventsManager.processEvent(
        new ReplanningEvent(_currentTick.get, Id.createPersonId(id), replanningReason)
      )
      goto(ChoosingMode) using ChoosesModeData(
        basePersonData.copy(
          currentTourMode = None, // Have to give up my mode as well, perhaps there's no option left for driving.
          currentTourPersonalVehicle = None,
          numberOfReplanningAttempts = basePersonData.numberOfReplanningAttempts + 1
        ),
        SpaceTime(
          beamServices.geo.wgs2Utm(basePersonData.restOfCurrentTrip.head.beamLeg.travelPath.startPoint).loc,
          _currentTick.get
        )
      )
  }

  when(ProcessingNextLegOrStartActivity, stateTimeout = Duration.Zero) {
    case Event(
          StateTimeout,
          data @ BasePersonData(
            _,
            _,
            nextLeg :: restOfCurrentTrip,
            currentVehicle,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _
          )
        ) if nextLeg.asDriver =>
      // Declaring a function here because this case is already so convoluted that I require a return
      // statement from within.
      // TODO: Refactor.
      def nextState: FSM.State[BeamAgentState, PersonData] = {
        val currentVehicleForNextState =
          if (currentVehicle.isEmpty || currentVehicle.head != nextLeg.beamVehicleId) {
            beamVehicles(nextLeg.beamVehicleId) match {
              case t @ Token(_, manager, _) =>
                manager ! TryToBoardVehicle(t, self, getCurrentTriggerIdOrGenerate)
                return goto(TryingToBoardVehicle)
              case _: ActualVehicle =>
              // That's fine, continue
            }
            eventsManager.processEvent(
              new PersonEntersVehicleEvent(
                _currentTick.get,
                Id.createPersonId(id),
                nextLeg.beamVehicleId
              )
            )
            nextLeg.beamVehicleId +: currentVehicle
          } else {
            currentVehicle
          }
        val legsToInclude = nextLeg +: restOfCurrentTrip.takeWhile(_.beamVehicleId == nextLeg.beamVehicleId)
        val newPassengerSchedule = PassengerSchedule().addLegs(legsToInclude.map(_.beamLeg))

        // Really? Also in the ReleasingParkingSpot case? How can it be that only one case releases the trigger,
        // but both of them send a CompletionNotice?
        if (nextLeg.beamLeg.endTime > lastTickOfSimulation) {
          scheduler ! CompletionNotice(
            _currentTriggerId.get,
            Vector()
          )
        } else {
          scheduler ! CompletionNotice(
            _currentTriggerId.get,
            Vector(ScheduleTrigger(StartLegTrigger(_currentTick.get, nextLeg.beamLeg), self))
          )
        }

        val stateToGo =
          if (
            nextLeg.beamLeg.mode == CAR
            || beamVehicles(nextLeg.beamVehicleId)
              .asInstanceOf[ActualVehicle]
              .vehicle
              .isSharedVehicle
          ) {
            log.debug(
              "ProcessingNextLegOrStartActivity, going to ReleasingParkingSpot with legsToInclude: {}",
              legsToInclude
            )
            ReleasingParkingSpot
          } else {
            releaseTickAndTriggerId()
            WaitingToDrive
          }
        goto(stateToGo) using data.copy(
          passengerSchedule = newPassengerSchedule,
          currentLegPassengerScheduleIndex = 0,
          currentVehicle = currentVehicleForNextState
        )
      }
      nextState

    // TRANSIT but too late
    case Event(StateTimeout, data @ BasePersonData(_, _, nextLeg :: _, _, _, _, _, _, _, _, _, _))
        if nextLeg.beamLeg.mode.isTransit && nextLeg.beamLeg.startTime < _currentTick.get =>
      // We've missed the bus. This occurs when something takes longer than planned (based on the
      // initial inquiry). So we replan but change tour mode to WALK_TRANSIT since we've already done our non-transit
      // portion.
      log.debug("Missed transit pickup, late by {} sec", _currentTick.get - nextLeg.beamLeg.startTime)

      val replanningReason = getReplanningReasonFrom(data, ReservationErrorCode.MissedTransitPickup.entryName)
      eventsManager.processEvent(
        new ReplanningEvent(_currentTick.get, Id.createPersonId(id), replanningReason)
      )
      goto(ChoosingMode) using ChoosesModeData(
        personData = data
          .copy(currentTourMode = Some(WALK_TRANSIT), numberOfReplanningAttempts = data.numberOfReplanningAttempts + 1),
        currentLocation =
          SpaceTime(beamServices.geo.wgs2Utm(nextLeg.beamLeg.travelPath.startPoint).loc, _currentTick.get),
        isWithinTripReplanning = true
      )
    // TRANSIT
    case Event(StateTimeout, BasePersonData(_, _, nextLeg :: _, _, _, _, _, _, _, _, _, _))
        if nextLeg.beamLeg.mode.isTransit =>
      val resRequest = TransitReservationRequest(
        nextLeg.beamLeg.travelPath.transitStops.get.fromIdx,
        nextLeg.beamLeg.travelPath.transitStops.get.toIdx,
        PersonIdWithActorRef(id, self),
        getCurrentTriggerIdOrGenerate
      )
      TransitDriverAgent.selectByVehicleId(nextLeg.beamVehicleId) ! resRequest
      goto(WaitingForReservationConfirmation)
    // RIDE_HAIL
    case Event(StateTimeout, BasePersonData(_, _, nextLeg :: tailOfCurrentTrip, _, _, _, _, _, _, _, _, _))
        if nextLeg.isRideHail =>
      val legSegment = nextLeg :: tailOfCurrentTrip.takeWhile(leg => leg.beamVehicleId == nextLeg.beamVehicleId)

      rideHailManager ! RideHailRequest(
        ReserveRide,
        PersonIdWithActorRef(id, self),
        beamServices.geo.wgs2Utm(nextLeg.beamLeg.travelPath.startPoint.loc),
        _currentTick.get,
        beamServices.geo.wgs2Utm(legSegment.last.beamLeg.travelPath.endPoint.loc),
        nextLeg.isPooledTrip,
        requestTime = _currentTick,
        quotedWaitTime = Some(nextLeg.beamLeg.startTime - _currentTick.get),
        triggerId = getCurrentTriggerIdOrGenerate
      )

      eventsManager.processEvent(
        new ReserveRideHailEvent(
          _currentTick.get.toDouble,
          id,
          _currentTick.get,
          nextLeg.beamLeg.travelPath.startPoint.loc,
          legSegment.last.beamLeg.travelPath.endPoint.loc
        )
      )
      goto(WaitingForReservationConfirmation)
    // CAV but too late
    // TODO: Refactor so it uses literally the same code block as transit
    case Event(StateTimeout, data @ BasePersonData(_, _, nextLeg :: _, _, _, _, _, _, _, _, _, _))
        if nextLeg.beamLeg.startTime < _currentTick.get =>
      // We've missed the CAV. This occurs when something takes longer than planned (based on the
      // initial inquiry). So we replan but change tour mode to WALK_TRANSIT since we've already done our non-transit
      // portion.
      log.warning("Missed CAV pickup, late by {} sec", _currentTick.get - nextLeg.beamLeg.startTime)

      val replanningReason = getReplanningReasonFrom(data, ReservationErrorCode.MissedTransitPickup.entryName)
      eventsManager.processEvent(
        new ReplanningEvent(_currentTick.get, Id.createPersonId(id), replanningReason)
      )
      goto(ChoosingMode) using ChoosesModeData(
        personData = data
          .copy(currentTourMode = Some(WALK_TRANSIT), numberOfReplanningAttempts = data.numberOfReplanningAttempts + 1),
        currentLocation =
          SpaceTime(beamServices.geo.wgs2Utm(nextLeg.beamLeg.travelPath.startPoint).loc, _currentTick.get),
        isWithinTripReplanning = true
      )
    // CAV
    // TODO: Refactor so it uses literally the same code block as transit
    case Event(StateTimeout, BasePersonData(_, _, nextLeg :: tailOfCurrentTrip, _, _, _, _, _, _, _, _, _)) =>
      val legSegment = nextLeg :: tailOfCurrentTrip.takeWhile(leg => leg.beamVehicleId == nextLeg.beamVehicleId)
      val resRequest = ReservationRequest(
        legSegment.head.beamLeg,
        legSegment.last.beamLeg,
        PersonIdWithActorRef(id, self),
        getCurrentTriggerIdOrGenerate
      )
      context.actorSelection(
        householdRef.path.child(HouseholdCAVDriverAgent.idFromVehicleId(nextLeg.beamVehicleId).toString)
      ) ! resRequest
      goto(WaitingForReservationConfirmation)

    case Event(
          StateTimeout,
          data @ BasePersonData(
            currentActivityIndex,
            Some(currentTrip),
            _,
            _,
            currentTourMode,
            currentTourPersonalVehicle,
            _,
            _,
            _,
            _,
            _,
            _
          )
        ) =>
      nextActivity(data) match {
        case Some(activity) =>
          val (tick, triggerId) = releaseTickAndTriggerId()
          val endTime =
            if (
              activity.getEndTime >= tick && Math
                .abs(activity.getEndTime) < Double.PositiveInfinity
            ) {
              activity.getEndTime
            } else if (activity.getEndTime >= 0.0 && activity.getEndTime < tick) {
              tick
            } else {
              //           logWarn(s"Activity endTime is negative or infinite ${activity}, assuming duration of 10
              // minutes.")
              //TODO consider ending the day here to match MATSim convention for start/end activity
              tick + 60 * 10
            }
          val newEndTime = if (lastTickOfSimulation >= tick) {
            Math.min(lastTickOfSimulation, endTime)
          } else {
            endTime
          }
          // Report travelled distance for inclusion in experienced plans.
          // We currently get large unaccountable differences in round trips, e.g. work -> home may
          // be twice as long as home -> work. Probably due to long links, and the location of the activity
          // on the link being undefined.
          eventsManager.processEvent(
            new TeleportationArrivalEvent(
              tick,
              id,
              currentTrip.legs.map(l => l.beamLeg.travelPath.distanceInM).sum
            )
          )
          assert(activity.getLinkId != null)
          eventsManager.processEvent(
            new PersonArrivalEvent(tick, id, activity.getLinkId, currentTrip.tripClassifier.value)
          )
          val incentive = beamScenario.modeIncentives.computeIncentive(attributes, currentTrip.tripClassifier)
          if (incentive > 0.0)
            eventsManager.processEvent(
              new PersonCostEvent(
                tick,
                id,
                currentTrip.tripClassifier.value,
                math.min(incentive, currentTrip.costEstimate),
                0.0,
                0.0 // the cost as paid by person has already been accounted for, this event is just about the incentive
              )
            )
          val correctedTrip = correctTripEndTime(data.currentTrip.get, tick, body.id, body.beamVehicleType.id)
          val generalizedTime =
            modeChoiceCalculator.getGeneralizedTimeOfTrip(correctedTrip, Some(attributes), nextActivity(data))
          val generalizedCost = modeChoiceCalculator.getNonTimeCost(correctedTrip) + attributes
            .getVOT(generalizedTime)
          // Correct the trip to deal with ride hail / disruptions and then register to skimmer
          val (odSkimmerEvent, origCoord, destCoord) = ODSkimmerEvent.forTaz(
            tick,
            beamServices,
            correctedTrip,
            generalizedTime,
            generalizedCost,
            curFuelConsumed.primaryFuel + curFuelConsumed.secondaryFuel
          )
          eventsManager.processEvent(odSkimmerEvent)
          if (beamServices.beamConfig.beam.exchange.output.activitySimSkimsEnabled) {
            val (origin, destination) = beamScenario.exchangeGeoMap match {
              case Some(geoMap) =>
                val origGeo = geoMap.getTAZ(origCoord)
                val destGeo = geoMap.getTAZ(destCoord)
                (origGeo.tazId.toString, destGeo.tazId.toString)
              case None =>
                (odSkimmerEvent.origin, odSkimmerEvent.destination)
            }
            val asSkimmerEvent = ActivitySimSkimmerEvent(
              origin,
              destination,
              odSkimmerEvent.eventTime,
              odSkimmerEvent.trip,
              odSkimmerEvent.generalizedTimeInHours,
              odSkimmerEvent.generalizedCost,
              odSkimmerEvent.energyConsumption,
              beamServices.beamConfig.beam.router.skim.activity_sim_skimmer.name
            )
            eventsManager.processEvent(asSkimmerEvent)
          }

          correctedTrip.legs.filter(x => x.beamLeg.mode == BeamMode.CAR || x.beamLeg.mode == BeamMode.CAV).foreach {
            carLeg =>
              eventsManager.processEvent(DriveTimeSkimmerEvent(tick, beamServices, carLeg))
          }

          resetFuelConsumed()

          eventsManager.processEvent(
            new ActivityStartEvent(
              tick,
              id,
              activity.getLinkId,
              activity.getFacilityId,
              activity.getType
            )
          )
          scheduler ! CompletionNotice(
            triggerId,
            Vector(ScheduleTrigger(ActivityEndTrigger(newEndTime.toInt), self))
          )
          goto(PerformingActivity) using data.copy(
            currentActivityIndex = currentActivityIndex + 1,
            currentTrip = None,
            restOfCurrentTrip = List(),
            currentTourPersonalVehicle = currentTourPersonalVehicle match {
              case Some(personalVehId) =>
                val personalVeh = beamVehicles(personalVehId).asInstanceOf[ActualVehicle].vehicle
                if (activity.getType.equals("Home")) {
                  potentiallyChargingBeamVehicles.put(personalVeh.id, beamVehicles(personalVeh.id))
                  beamVehicles -= personalVeh.id
                  personalVeh.getManager.get ! ReleaseVehicle(personalVeh, triggerId)
                  None
                } else {
                  currentTourPersonalVehicle
                }
              case None =>
                None
            },
            currentTourMode = if (activity.getType.equals("Home")) None else currentTourMode,
            hasDeparted = false
          )
        case None =>
          logDebug("PersonAgent nextActivity returned None")
          val (_, triggerId) = releaseTickAndTriggerId()
          scheduler ! CompletionNotice(triggerId)
          stop
      }
  }

  def getReplanningReasonFrom(data: BasePersonData, prefix: String): String = {
    data.currentTourMode
      .collect { case mode =>
        s"$prefix $mode"
      }
      .getOrElse(prefix)
  }

  def handleSuccessfulReservation(
    triggersToSchedule: Vector[ScheduleTrigger],
    data: BasePersonData,
    travelProposal: Option[TravelProposal] = None
  ): FSM.State[BeamAgentState, PersonData] = {
    if (_currentTriggerId.isDefined) {
      val (tick, triggerId) = releaseTickAndTriggerId()
      log.debug("releasing tick {} and scheduling triggers from reservation responses: {}", tick, triggersToSchedule)
      scheduler ! CompletionNotice(triggerId, triggersToSchedule)
    } else {
      // if _currentTriggerId is empty, this means we have received the reservation response from a batch
      // vehicle allocation process. It's ok, the trigger is with the ride hail manager.
    }
    val newData = travelProposal match {
      case Some(newTrip) =>
        data.copy(
          restOfCurrentTrip = data.restOfCurrentTrip
            .takeWhile(_.isRideHail)
            .map(_.copy(beamVehicleId = newTrip.rideHailAgentLocation.vehicleId)) ++ data.restOfCurrentTrip.dropWhile(
            _.isRideHail
          )
        )
      case None =>
        data
    }
    goto(Waiting) using newData

  }

  def handleBoardOrAlightOutOfPlace: State = {
    stash
    stay
  }

  val myUnhandled: StateFunction = {
    case Event(BeamAgentSchedulerTimer, _) =>
      // Put a breakpoint here to see an internal state of the actor
      log.debug(s"Received message from ${sender()}")
      stay
    case Event(IllegalTriggerGoToError(reason), _) =>
      stop(Failure(reason))
    case Event(Status.Failure(reason), _) =>
      stop(Failure(reason))
    case Event(StateTimeout, _) =>
      log.error("Events leading up to this point:\n\t" + getLog.mkString("\n\t"))
      stop(
        Failure(
          "Timeout - this probably means this agent was not getting a reply it was expecting."
        )
      )
    case Event(Finish, _) =>
      if (stateName == Moving) {
        log.warning(s"$id is still travelling at end of simulation.")
        log.warning(s"$id events leading up to this point:\n\t${getLog.mkString("\n\t")}")
      } else if (stateName == PerformingActivity) {
        logger.debug(s"$id is performing Activity at end of simulation")
        logger.warn("Performing Activity at end of simulation")
      } else {
        logger.warn(s"$id has received Finish while in state: $stateName, personId: $id")
      }
      stop
    case Event(TriggerWithId(_: BoardVehicleTrigger, _), _: ChoosesModeData) =>
      handleBoardOrAlightOutOfPlace
    case Event(TriggerWithId(_: AlightVehicleTrigger, _), _: ChoosesModeData) =>
      handleBoardOrAlightOutOfPlace
    case Event(
          TriggerWithId(BoardVehicleTrigger(_, vehicleId), triggerId),
          BasePersonData(_, _, _, currentVehicle, _, _, _, _, _, _, _, _)
        ) if currentVehicle.nonEmpty && currentVehicle.head.equals(vehicleId) =>
      log.debug("Person {} in state {} received Board for vehicle that he is already on, ignoring...", id, stateName)
      stay() replying CompletionNotice(triggerId, Vector())
    case Event(TriggerWithId(_: BoardVehicleTrigger, _), _: BasePersonData) =>
      handleBoardOrAlightOutOfPlace
    case Event(TriggerWithId(_: AlightVehicleTrigger, _), _: BasePersonData) =>
      handleBoardOrAlightOutOfPlace
    case Event(_: NotifyVehicleIdle, _) =>
      stay()
    case Event(TriggerWithId(_: RideHailResponseTrigger, triggerId), _) =>
      stay() replying CompletionNotice(triggerId)
    case ev @ Event(RideHailResponse(_, _, _, _, _), _) =>
      stop(Failure(s"Unexpected RideHailResponse from ${sender()}: $ev"))
    case Event(ParkingInquiryResponse(_, _, _), _) =>
      stop(Failure("Unexpected ParkingInquiryResponse"))
    case ev @ Event(StartingRefuelSession(_, _), _) =>
      log.debug("myUnhandled.StartingRefuelSession: {}", ev)
      stay()
    case ev @ Event(UnhandledVehicle(_, _, _), _) =>
      log.debug("myUnhandled.UnhandledVehicle: {}", ev)
      stay()
    case ev @ Event(WaitingToCharge(_, _, _), _) =>
      log.debug("myUnhandled.WaitingInLine: {}", ev)
      stay()
    case ev @ Event(EndingRefuelSession(_, _, _), _) =>
      log.debug("myUnhandled.EndingRefuelSession: {}", ev)
      stay()
    case Event(e, s) =>
      log.warning("received unhandled request {} in state {}/{}", e, stateName, s)
      stay()
  }

  whenUnhandled(drivingBehavior.orElse(myUnhandled))

  override def logPrefix(): String = s"PersonAgent:$id "
}
