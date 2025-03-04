package beam.agentsim.agents.vehicles

import akka.actor.ActorRef
import beam.agentsim.agents.PersonAgent
import beam.agentsim.agents.vehicles.BeamVehicle.{BeamVehicleState, FuelConsumed}
import beam.agentsim.agents.vehicles.ConsumptionRateFilterStore.{Primary, Secondary}
import beam.agentsim.agents.vehicles.EnergyEconomyAttributes.Powertrain
import beam.agentsim.agents.vehicles.FuelType.{Electricity, Gasoline}
import beam.agentsim.agents.vehicles.VehicleCategory._
import beam.agentsim.agents.vehicles.VehicleProtocol.StreetVehicle
import beam.agentsim.events.SpaceTime
import beam.agentsim.infrastructure.ParkingStall
import beam.agentsim.infrastructure.charging.ChargingPointType
import beam.agentsim.infrastructure.parking.ParkingZone
import beam.api.agentsim.agents.vehicles.BeamVehicleAfterUseFuelHook
import beam.router.Modes
import beam.router.Modes.BeamMode.{BIKE, CAR, CAV, WALK}
import beam.router.model.BeamLeg
import beam.sim.BeamScenario
import beam.sim.common.GeoUtils.TurningDirection
import beam.utils.NetworkHelper
import beam.utils.ReadWriteLockUtil._
import beam.utils.logging.ExponentialLazyLogging
import org.matsim.api.core.v01.Id
import org.matsim.api.core.v01.network.Link
import org.matsim.core.api.experimental.events.EventsManager
import org.matsim.vehicles.Vehicle

import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}
import java.util.concurrent.locks.ReentrantReadWriteLock
import scala.util.Random

/**
  * A [[BeamVehicle]] is a state container __administered__ by a driver ([[PersonAgent]]
  * implementing [[beam.agentsim.agents.modalbehaviors.DrivesVehicle]]). The passengers in the [[BeamVehicle]]
  * are also [[BeamVehicle]]s, however, others are possible). The
  * reference to a parent [[BeamVehicle]] is maintained in its carrier. All other information is
  * managed either through the MATSim [[Vehicle]] interface or within several other classes.
  *
  * @author saf
  * @since Beam 2.0.0
  */
// XXXX: This is a class and MUST NOT be a case class because it contains mutable state.
// If we need immutable state, we will need to operate on this through lenses.

// TODO: safety for
class BeamVehicle(
  val id: Id[BeamVehicle],
  val powerTrain: Powertrain,
  val beamVehicleType: BeamVehicleType,
  val vehicleManagerId: AtomicReference[Id[VehicleManager]] = new AtomicReference(VehicleManager.AnyManager.managerId),
  val randomSeed: Int = 0
) extends ExponentialLazyLogging {
  private val manager: AtomicReference[Option[ActorRef]] = new AtomicReference(None)
  def setManager(value: Option[ActorRef]): Unit = this.manager.set(value)
  def getManager: Option[ActorRef] = this.manager.get

  val rand: Random = new Random(randomSeed)

  @volatile
  var spaceTime: SpaceTime = _

  private val fuelRWLock = new ReentrantReadWriteLock()

  private var primaryFuelLevelInJoulesInternal = beamVehicleType.primaryFuelCapacityInJoule
  def primaryFuelLevelInJoules: Double = fuelRWLock.read { primaryFuelLevelInJoulesInternal }
  private var secondaryFuelLevelInJoulesInternal = beamVehicleType.secondaryFuelCapacityInJoule.getOrElse(0.0)
  def secondaryFuelLevelInJoules: Double = fuelRWLock.read { secondaryFuelLevelInJoulesInternal }

  private val mustBeDrivenHomeInternal: AtomicBoolean = new AtomicBoolean(false)
  def isMustBeDrivenHome: Boolean = mustBeDrivenHomeInternal.get()
  def setMustBeDrivenHome(value: Boolean): Unit = mustBeDrivenHomeInternal.set(value)

  /**
    * The [[PersonAgent]] who is currently driving the vehicle (or None ==> it is idle).
    * Effectively, this is the main controller of the vehicle in space and time in the scenario environment;
    * whereas, the manager is ultimately responsible for assignment and (for now) ownership
    * of the vehicle as a physical property.
    */
  private val driver: AtomicReference[Option[ActorRef]] = new AtomicReference(None)
  def getDriver: Option[ActorRef] = driver.get()

  private val stallRWLock = new ReentrantReadWriteLock()

  private var reservedStallInternal: Option[ParkingStall] = None
  def reservedStall: Option[ParkingStall] = stallRWLock.read { reservedStallInternal }

  private var stallInternal: Option[ParkingStall] = None
  def stall: Option[ParkingStall] = stallRWLock.read { stallInternal }

  private var lastUsedStallInternal: Option[ParkingStall] = None
  def lastUsedStall: Option[ParkingStall] = stallRWLock.read { lastUsedStallInternal }

  private val chargerRWLock = new ReentrantReadWriteLock()

  private var connectedToCharger: Boolean = false

  private var chargerConnectedTick: Option[Int] = None
  private var chargerConnectedPrimaryFuel: Option[Double] = None

  private var waitingToChargeInternal: Boolean = false
  private var waitingToChargeTick: Option[Int] = None

  /**
    * Called by the driver.
    */
  def unsetDriver(): Unit = {
    driver.set(None)
  }

  /**
    * Only permitted if no driver is currently set. Driver has full autonomy in vehicle, so only
    * a call of [[unsetDriver]] will remove the driver.
    *
    * @param newDriver incoming driver
    */
  def becomeDriver(newDriver: ActorRef): Unit = {
    if (!driver.compareAndSet(None, Some(newDriver))) {
      // This is _always_ a programming error.
      // A BeamVehicle is only a data structure, not an Actor.
      // It must be ensured externally, by other means, that only one agent can access
      // it at any time, e.g. by using a ResourceManager etc.
      // Also, this exception is only a "best effort" error detection.
      // Technically, it can also happen that it is _not_ thrown in the failure case,
      // as this method is not synchronized.
      // Don't try to catch this exception.
      throw new RuntimeException("Trying to set a driver where there already is one.")
    }
  }

  def setReservedParkingStall(newStall: Option[ParkingStall]): Unit = {
    stallRWLock.write {
      reservedStallInternal = newStall
    }
  }

  def useParkingStall(newStall: ParkingStall): Unit = {
    stallRWLock.write {
      stallInternal = Some(newStall)
      lastUsedStallInternal = Some(newStall)
    }
  }

  def unsetParkingStall(): Unit = {
    stallRWLock.write {
      stallInternal = None
    }
  }

  def waitingToCharge(startTick: Int): Unit = {
    if (beamVehicleType.primaryFuelType == Electricity || beamVehicleType.secondaryFuelType.contains(Electricity)) {
      chargerRWLock.write {
        waitingToChargeInternal = true
        waitingToChargeTick = Some(startTick)
        connectedToCharger = false
        chargerConnectedTick = None
        chargerConnectedPrimaryFuel = None
      }
    } else {
      logger.warn(
        "Trying to connect a non BEV/PHEV to a electricity charging station. " +
        "This will cause an explosion. Ignoring!"
      )
    }
  }

  /**
    * @param startTick
    */
  def connectToChargingPoint(startTick: Int): Unit = {
    if (beamVehicleType.primaryFuelType == Electricity || beamVehicleType.secondaryFuelType.contains(Electricity)) {
      chargerRWLock.write {
        connectedToCharger = true
        chargerConnectedTick = Some(startTick)
        chargerConnectedPrimaryFuel = Some(primaryFuelLevelInJoules)
        waitingToChargeInternal = false
        waitingToChargeTick = None
      }
    } else
      logger.warn(
        "Trying to connect a non BEV/PHEV to a electricity charging station. " +
        "This will cause an explosion. Ignoring!"
      )
  }

  def disconnectFromChargingPoint(): Unit = {
    chargerRWLock.write {
      connectedToCharger = false
      chargerConnectedTick = None
      chargerConnectedPrimaryFuel = None
      waitingToChargeInternal = false
      waitingToChargeTick = None
    }
  }

  def isConnectedToChargingPoint(): Boolean = {
    chargerRWLock.read {
      connectedToCharger
    }
  }

  def getChargerConnectedTick(): Int = {
    chargerRWLock.read {
      chargerConnectedTick.getOrElse(0)
    }
  }

  def getChargerConnectedPrimaryFuel(): Double = {
    chargerRWLock.read {
      chargerConnectedPrimaryFuel.getOrElse(0L)
    }
  }

  /**
    * useFuel
    *
    * This method estimates energy consumed for [beamLeg] using data in [beamServices]. It accommodates a secondary
    * powertrain and tracks the fuel consumed by each powertrain in cascading order (i.e. primary first until tank is
    * empty and then secondary).
    *
    * IMPORTANT -- This method does nothing to stop a vehicle from moving further than the fuel on-board would allow.
    * When more energy is consumed than the fuel level allows, a warning is logged and the fuel level goes negative.
    * We choose to allow for negative fuel level because this can convey useful information to the user, namely, the
    * amount of increased fuel capacity that would be needed to avoid running out.
    *
    * When fuel level goes negative, it is assumed to happen on the primary power train, not the secondary.
    *
    * It is up to the manager / driver of this vehicle to decide how to react if fuel level becomes negative.
    */
  def useFuel(
    beamLeg: BeamLeg,
    beamScenario: BeamScenario,
    networkHelper: NetworkHelper,
    eventsManager: EventsManager,
    eventBuilder: ActorRef,
    beamVehicleAfterUseFuelHook: Option[BeamVehicleAfterUseFuelHook]
  ): FuelConsumed = {
    val fuelConsumptionDataWithOnlyLength_Id_And_Type =
      !beamScenario.vehicleEnergy.vehicleEnergyMappingExistsFor(beamVehicleType)
    val fuelConsumptionData =
      BeamVehicle.collectFuelConsumptionData(
        beamLeg,
        beamVehicleType,
        networkHelper,
        fuelConsumptionDataWithOnlyLength_Id_And_Type
      )

    val primaryEnergyForFullLeg =
      beamScenario.vehicleEnergy.getFuelConsumptionEnergyInJoulesUsing(
        fuelConsumptionData,
        fallBack = powerTrain.getRateInJoulesPerMeter,
        Primary
      )
    var primaryEnergyConsumed = primaryEnergyForFullLeg
    var secondaryEnergyConsumed = 0.0
    fuelRWLock.write {
      if (primaryFuelLevelInJoulesInternal < primaryEnergyForFullLeg) {
        if (secondaryFuelLevelInJoulesInternal > 0.0) {
          // Use secondary fuel if possible
          val secondaryEnergyForFullLeg =
            beamScenario.vehicleEnergy.getFuelConsumptionEnergyInJoulesUsing(
              fuelConsumptionData,
              fallBack = powerTrain.getRateInJoulesPerMeter,
              Secondary
            )
          secondaryEnergyConsumed =
            secondaryEnergyForFullLeg * (primaryEnergyForFullLeg - primaryFuelLevelInJoulesInternal) / primaryEnergyConsumed
          if (secondaryFuelLevelInJoulesInternal < secondaryEnergyConsumed) {
            logger.warn(
              "Vehicle does not have sufficient fuel to make trip (in both primary and secondary fuel tanks), allowing trip to happen and setting fuel level negative: vehicle {} trip distance {} m",
              id,
              beamLeg.travelPath.distanceInM
            )
            primaryEnergyConsumed =
              primaryEnergyForFullLeg - secondaryFuelLevelInJoulesInternal / secondaryEnergyConsumed
            secondaryEnergyConsumed = secondaryFuelLevelInJoulesInternal
          } else {
            primaryEnergyConsumed = primaryFuelLevelInJoulesInternal
          }
        } else {
          logger.warn(
            "Vehicle does not have sufficient fuel to make trip, allowing trip to happen and setting fuel level negative: vehicle {} trip distance {} m and remainingRange {} m",
            id,
            beamLeg.travelPath.distanceInM,
            getState.remainingPrimaryRangeInM
          )

        }
      }
      primaryFuelLevelInJoulesInternal = primaryFuelLevelInJoulesInternal - primaryEnergyConsumed
      secondaryFuelLevelInJoulesInternal = secondaryFuelLevelInJoulesInternal - secondaryEnergyConsumed
    }

    beamVehicleAfterUseFuelHook.foreach(
      _.execute(beamLeg, beamScenario, networkHelper, eventsManager, eventBuilder, this)
    )

    FuelConsumed(
      primaryEnergyConsumed,
      secondaryEnergyConsumed /*, fuelConsumptionData, primaryLoggingData, secondaryLoggingData*/
    )
  }

  def isRidehailVehicle = id.toString.startsWith("rideHailVehicle")

  def addFuel(fuelInJoules: Double): Unit = {
    fuelRWLock.write {
      primaryFuelLevelInJoulesInternal = primaryFuelLevelInJoulesInternal + fuelInJoules
    }
  }

  /**
    * Estimates the duration and energy that will be required to refuel this BeamVehicle using the [[ParkingStall]]
    * passed in as an argument.
    *
    * @param parkingStall
    * @param sessionDurationLimit the maximum allowable charging duration to be considered.
    * @return tuple with (refuelingDuration, refuelingEnergy)
    */
  def refuelingSessionDurationAndEnergyInJoulesForStall(
    parkingStall: Option[ParkingStall],
    sessionDurationLimit: Option[Int],
    stateOfChargeLimit: Option[Double],
    chargingPowerLimit: Option[Double]
  ): (Int, Double) = {
    parkingStall match {
      case Some(theStall) =>
        theStall.chargingPointType match {
          case Some(chargingPoint) =>
            ChargingPointType.calculateChargingSessionLengthAndEnergyInJoule(
              chargingPoint,
              primaryFuelLevelInJoules,
              beamVehicleType.primaryFuelCapacityInJoule,
              1e6,
              1e6,
              sessionDurationLimit,
              stateOfChargeLimit,
              chargingPowerLimit
            )
          case None =>
            (0, 0.0)
        }
      case None =>
        (0, 0.0) // if we are not parked, no refueling can occur
    }
  }

  /**
    * Estimates the duration and energy that will be required to refuel this BeamVehicle using the [[ParkingStall]] at
    * which this vehicle is currently parked.
    *
    * @param sessionDurationLimit the maximum allowable charging duration to be considered.
    * @return tuple with (refuelingDuration, refuelingEnergy)
    */
  def refuelingSessionDurationAndEnergyInJoules(
    sessionDurationLimit: Option[Int],
    stateOfChargeLimit: Option[Double],
    chargingPowerLimit: Option[Double]
  ): (Int, Double) = {
    refuelingSessionDurationAndEnergyInJoulesForStall(
      stall,
      sessionDurationLimit,
      stateOfChargeLimit,
      chargingPowerLimit
    )
  }

  def getState: BeamVehicleState = {
    val primaryFuelLevel = primaryFuelLevelInJoules
    val (primaryRange, secondaryRange) = getRemainingRange
    BeamVehicleState(
      primaryFuelLevel,
      beamVehicleType.secondaryFuelCapacityInJoule,
      primaryRange,
      secondaryRange,
      driver.get(),
      stall
    )
  }

  def getTotalRemainingRange: Double = {
    primaryFuelLevelInJoules / powerTrain.estimateConsumptionInJoules(1) + beamVehicleType.secondaryFuelCapacityInJoule
      .map(_ => secondaryFuelLevelInJoules / beamVehicleType.secondaryFuelConsumptionInJoulePerMeter.get)
      .getOrElse(0.0)
  }

  def getRemainingRange: (Double, Option[Double]) = {
    (
      primaryFuelLevelInJoules / powerTrain.estimateConsumptionInJoules(1),
      beamVehicleType.secondaryFuelCapacityInJoule.map(_ =>
        secondaryFuelLevelInJoules / beamVehicleType.secondaryFuelConsumptionInJoulePerMeter.get
      )
    )
  }

  def toStreetVehicle: StreetVehicle = {
    val mode = beamVehicleType.vehicleCategory match {
      case Bike =>
        BIKE
      case Car | LightDutyTruck | HeavyDutyTruck if isCAV =>
        CAV
      case Car | LightDutyTruck | HeavyDutyTruck =>
        CAR
      case Body =>
        WALK
    }
    val needsToCalculateCost = beamVehicleType.vehicleCategory == Car || isSharedVehicle
    StreetVehicle(id, beamVehicleType.id, spaceTime, mode, asDriver = true, needsToCalculateCost = needsToCalculateCost)
  }

  def isSharedVehicle: Boolean = beamVehicleType.id.toString.startsWith("sharedVehicle")

  def isCAV: Boolean = beamVehicleType.automationLevel >= 4

  def isBEV: Boolean =
    beamVehicleType.primaryFuelType == Electricity && beamVehicleType.secondaryFuelType.isEmpty

  def isPHEV: Boolean =
    beamVehicleType.primaryFuelType == Electricity && beamVehicleType.secondaryFuelType.contains(Gasoline)

  /**
    * Initialize the vehicle's fuel levels to a given state of charge (between 0.0 and 1.0).
    *
    * For non-electric vehicles, initialSoc is ignored. For hybrids, secondaryFuelLevelInJoules is set to
    * secondaryFuelCapacityInJoule.
    *
    * @param initialSoc Initial state of charge.
    */
  def initializeFuelLevels(initialSoc: Double): Unit = {
    val primaryFuelLevelInJoules = if (beamVehicleType.primaryFuelType == FuelType.Electricity) {
      if (initialSoc < 0) {
        logger.error(f"initialSoc less than 0, setting to 0, vehicle: $id, initialSoc: $initialSoc")
        0
      } else if (initialSoc > 1) {
        logger.error(f"initialSoc greater than 1, setting to 1, vehicle: $id, initialSoc: $initialSoc")
        beamVehicleType.primaryFuelCapacityInJoule
      } else {
        beamVehicleType.primaryFuelCapacityInJoule * initialSoc
      }
    } else {
      if (initialSoc != 1) {
        logger.warn(
          f"vehicle is not electric, requested initial SoC will be ignored and set to 1, vehicle: $id, initialSoc: $initialSoc"
        )
      }
      beamVehicleType.primaryFuelCapacityInJoule
    }

    fuelRWLock.write {
      primaryFuelLevelInJoulesInternal = primaryFuelLevelInJoules
      secondaryFuelLevelInJoulesInternal = beamVehicleType.secondaryFuelCapacityInJoule.getOrElse(0.0)
    }
  }

  /**
    * Initialize the vehicle's fuel levels to a uniformly distributed state of charge with given mean.
    *
    * @param meanSoc Mean state of charge
    */
  def initializeFuelLevelsFromUniformDistribution(meanSoc: Double): Unit = {
    val initialSoc = BeamVehicle.randomSocFromUniformDistribution(rand, beamVehicleType, meanSoc)
    initializeFuelLevels(initialSoc)
  }

  def isRefuelNeeded(
    refuelRequiredThresholdInMeters: Double = 32200.0,
    noRefuelThresholdInMeters: Double = 161000.0
  ): Boolean = {
    /*
      if below a threshold (like 20 miles of remaining range) then we definitely go to charge.
      If range is above that, we do a random draw with a probability that increases the closer we get to 20 miles.
      So 21 miles my by 90%, 30 miles might be 75%, 40 miles 50%, etc. We can keep the relationship simple.
      Maybe we give a threshold and then the slope of a linear relationship between miles and prob.
      E.g. P(charge) = 1 - (rangeLeft - 20)*slopeParam….
      where any range that yields a negative probability would just be truncated to 0
     */
    val remainingRangeInMeters = getState.remainingPrimaryRangeInM + getState.remainingSecondaryRangeInM.getOrElse(0.0)
    if (remainingRangeInMeters < refuelRequiredThresholdInMeters) {
      logger.debug(
        "Refueling since range of {} m is less than {} for {}",
        remainingRangeInMeters,
        refuelRequiredThresholdInMeters,
        toString
      )
      true
    } else if (remainingRangeInMeters > noRefuelThresholdInMeters) {
      logger.debug(
        "No refueling since range of {} m is greater than {} for {}",
        remainingRangeInMeters,
        noRefuelThresholdInMeters,
        toString
      )
      false
    } else {
      val probabilityOfRefuel =
        1.0 - (remainingRangeInMeters - refuelRequiredThresholdInMeters) / (noRefuelThresholdInMeters - refuelRequiredThresholdInMeters)
      val refuelNeeded = rand.nextDouble() < probabilityOfRefuel
      if (refuelNeeded) {
        logger.debug("Refueling because random draw exceeded probability to refuel of {}", probabilityOfRefuel)
      } else {
        logger.debug(
          "Not refueling because random draw did not exceed probability to refuel of {}",
          probabilityOfRefuel
        )
      }
      refuelNeeded
    }
  }

  override def toString = s"$id (${beamVehicleType.id},${getTotalRemainingRange / 1000.0}km)"

  def resetState(): Unit = {
    setManager(None)
    spaceTime = null

    fuelRWLock.write {
      primaryFuelLevelInJoulesInternal = 0.0
      secondaryFuelLevelInJoulesInternal = 0.0
    }

    mustBeDrivenHomeInternal.set(false)
    unsetDriver()

    stallRWLock.write {
      reservedStallInternal = None
      stallInternal = None
      lastUsedStallInternal = None
    }

    chargerRWLock.write {
      connectedToCharger = false
      chargerConnectedTick = None
    }
  }
}

object BeamVehicle {

  case class FuelConsumed(
    primaryFuel: Double,
    secondaryFuel: Double /*, fuelConsumptionData: IndexedSeq[FuelConsumptionData],
                          primaryLoggingData: IndexedSeq[LoggingData],
                          secondaryLoggingData: IndexedSeq[LoggingData]*/
  )

  def noSpecialChars(theString: String): String =
    theString.replaceAll("[\\\\|\\\\^]+", ":")

  def createId[A](id: Id[A], prefix: Option[String] = None): Id[BeamVehicle] = {
    createId(id.toString, prefix)
  }

  def createId[A](id: String, prefix: Option[String]): Id[BeamVehicle] = {
    Id.create(s"${prefix.map(_ + "-").getOrElse("")}$id", classOf[BeamVehicle])
  }

  case class BeamVehicleState(
    primaryFuelLevel: Double,
    secondaryFuelLevel: Option[Double],
    remainingPrimaryRangeInM: Double,
    remainingSecondaryRangeInM: Option[Double],
    driver: Option[ActorRef],
    stall: Option[ParkingStall]
  ) {

    def totalRemainingRange: Double = {
      remainingPrimaryRangeInM + remainingSecondaryRangeInM.getOrElse(0.0)
    }
  }

  case class FuelConsumptionData(
    linkId: Int,
    vehicleType: BeamVehicleType,
    linkNumberOfLanes: Option[Int],
    linkCapacity: Option[Double] = None,
    linkLength: Option[Double],
    averageSpeed: Option[Double],
    freeFlowSpeed: Option[Double],
    linkArrivalTime: Option[Long] = None,
    turnAtLinkEnd: Option[TurningDirection] = None,
    numberOfStops: Option[Int] = None
  )

  /**
    * Organizes the fuel consumption data table
    *
    * @param beamLeg Instance of beam leg
    * @param networkHelper the transport network instance
    * @return list of fuel consumption objects generated
    */
  def collectFuelConsumptionData(
    beamLeg: BeamLeg,
    theVehicleType: BeamVehicleType,
    networkHelper: NetworkHelper,
    fuelConsumptionDataWithOnlyLength_Id_And_Type: Boolean = false
  ): IndexedSeq[FuelConsumptionData] = {
    //TODO: This method is becoming a little clunky. If it has to grow again then maybe refactor/break it out
    if (beamLeg.mode.isTransit & !Modes.isOnStreetTransit(beamLeg.mode)) {
      Vector.empty
    } else if (fuelConsumptionDataWithOnlyLength_Id_And_Type) {
      beamLeg.travelPath.linkIds
        .drop(1)
        .map(id =>
          FuelConsumptionData(
            linkId = id,
            vehicleType = theVehicleType,
            linkNumberOfLanes = None,
            linkCapacity = None,
            linkLength = networkHelper.getLink(id).map(_.getLength),
            averageSpeed = None,
            freeFlowSpeed = None,
            linkArrivalTime = None,
            turnAtLinkEnd = None,
            numberOfStops = None
          )
        )
    } else {
      val linkIds = beamLeg.travelPath.linkIds.drop(1)
      val linkTravelTimes: IndexedSeq[Double] = beamLeg.travelPath.linkTravelTime.drop(1)
      // generate the link arrival times for each link ,by adding cumulative travel times of previous links
//      val linkArrivalTimes = linkTravelTimes.scan(beamLeg.startTime)((enterTime,duration) => enterTime + duration).dropRight(1)
//      val nextLinkIds = linkIds.takeRight(linkIds.size - 1)
      linkIds.zipWithIndex.map { case (id, idx) =>
        val travelTime = linkTravelTimes(idx)
        val currentLink: Option[Link] = networkHelper.getLink(id)
        val averageSpeed =
          try {
            if (travelTime > 0) currentLink.map(_.getLength).getOrElse(0.0) / travelTime else 0
          } catch {
            case _: Exception => 0.0
          }
        FuelConsumptionData(
          linkId = id,
          vehicleType = theVehicleType,
          linkNumberOfLanes = currentLink.map(_.getNumberOfLanes().toInt),
          linkCapacity = None, //currentLink.map(_.getCapacity),
          linkLength = currentLink.map(_.getLength),
          averageSpeed = Some(averageSpeed),
          freeFlowSpeed = None,
          linkArrivalTime = None, //Some(arrivalTime),
          turnAtLinkEnd = None, //Some(turnAtLinkEnd),
          numberOfStops = None //Some(numStops)
        )
      }
    }
  }

  def randomSocFromUniformDistribution(rand: Random, beamVehicleType: BeamVehicleType, meanSoc: Double): Double = {
    beamVehicleType.primaryFuelType match {
      case Electricity =>
        val meanSOC = math.max(math.min(meanSoc, 1.0), 0.5)
        val minimumSOC = 2.0 * meanSOC - 1
        minimumSOC + (1.0 - minimumSOC) * rand.nextDouble()
      case _ => 1.0
    }
  }
}
