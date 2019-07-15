package beam.sim.vehiclesharing
import beam.agentsim.agents.vehicles.{BeamVehicle, BeamVehicleType}
import beam.agentsim.events.SpaceTime
import beam.agentsim.infrastructure.taz.{TAZ, TAZTreeMap}
import beam.router.BeamSkimmer
import beam.router.Modes.BeamMode
import beam.sim.BeamServices
import org.matsim.api.core.v01.Id

import scala.collection.mutable

case class AvailabilityBasedRepositioning(
  repositionTimeBin: Int,
  statTimeBin: Int,
  matchLimit: Int,
  vehicleManager: Id[VehicleManager],
  beamServices: BeamServices,
  beamSkimmer: BeamSkimmer
) extends RepositionAlgorithm {

  case class RepositioningRequest(taz: TAZ, availability: Int)
  val minAvailabilityMap = mutable.HashMap.empty[(Int, Id[TAZ]), Int]
  val notAvailableVehicle = mutable.HashMap.empty[(Int, Id[TAZ]), Int]
  val ordering = Ordering.by[RepositioningRequest, Int](_.availability)

  val F = mutable.HashMap.empty[Id[TAZ], Int]

  beamServices.beamScenario.tazTreeMap.getTAZs.foreach { taz =>
    (0 to 108000 / repositionTimeBin).foreach { i =>
      val time = i * repositionTimeBin
      val availVal = getCollectedDataFromPreviousSimulation(time, taz.tazId, RepositionManager.availability)
      val availValMin = availVal.drop(1).foldLeft(availVal.headOption.getOrElse(0.0).toInt) { (minV, cur) =>
        Math.min(minV, cur.toInt)
      }
      minAvailabilityMap.put((i, taz.tazId), availValMin)
      val notAvailableVal = getCollectedDataFromPreviousSimulation(time, taz.tazId, RepositionManager.notAvailable).sum.toInt
      notAvailableVehicle.put((i, taz.tazId), notAvailableVal)
    }
  }

  def getCollectedDataFromPreviousSimulation(time: Int, idTAZ: Id[TAZ], label: String) = {
    val fromBin = time / statTimeBin
    val untilBin = (time + repositionTimeBin) / statTimeBin
    beamSkimmer.getPreviousSkimPlusValues(fromBin, untilBin, idTAZ, vehicleManager, label)
  }

  override def getVehiclesForReposition(
    now: Int,
    timeBin: Int,
    availableFleet: List[BeamVehicle]
  ): List[(BeamVehicle, SpaceTime, Id[TAZ], SpaceTime, Id[TAZ])] = {
    val oversuppliedTAZ = mutable.TreeSet.empty[RepositioningRequest](ordering)
    val undersuppliedTAZ = mutable.TreeSet.empty[RepositioningRequest](ordering.reverse)
    val nowRepBin = now / timeBin
    val futureRepBin = nowRepBin + 1
    beamServices.beamScenario.tazTreeMap.getTAZs.foreach {
      taz =>
        val availValMin1 = minAvailabilityMap((nowRepBin, taz.tazId))
        val notAvailVal1 = notAvailableVehicle((nowRepBin, taz.tazId))
        val av1 = availValMin1 - notAvailVal1 + F.getOrElse(taz.tazId, 0)
        if(av1 > 0)
          oversuppliedTAZ.add(RepositioningRequest(taz, av1))

        val availValMin2 = minAvailabilityMap((futureRepBin, taz.tazId))
        val notAvailVal2 = notAvailableVehicle((futureRepBin, taz.tazId))
        val av2 = availValMin2 - notAvailVal2 + F.getOrElse(taz.tazId, 0)
        if(av2 < 0)
          undersuppliedTAZ.add(RepositioningRequest(taz, av2))
    }
    val topOversuppliedTAZ = oversuppliedTAZ.take(matchLimit)
    val topUndersuppliedTAZ = undersuppliedTAZ.take(matchLimit)
    val ODs = new mutable.ListBuffer[(RepositioningRequest, RepositioningRequest, Int, Int)]
    while (topOversuppliedTAZ.nonEmpty && topUndersuppliedTAZ.nonEmpty) {
      val org = topOversuppliedTAZ.head
      var destTimeOpt: Option[(RepositioningRequest, Int)] = None
      topUndersuppliedTAZ.foreach { dst =>
        val skim = beamSkimmer.getTimeDistanceAndCost(
          org.taz.coord,
          dst.taz.coord,
          now,
          BeamMode.CAR,
          Id.create( // FIXME Vehicle type borrowed from ridehail -- pass the vehicle type of the car sharing fleet instead
            beamServices.beamConfig.beam.agentsim.agents.rideHail.initialization.procedural.vehicleTypeId,
            classOf[BeamVehicleType]
          )
        )
        if (destTimeOpt.isEmpty || (destTimeOpt.isDefined && skim.time < destTimeOpt.get._2)) {
          destTimeOpt = Some((dst, skim.time))
        }
      }
      destTimeOpt foreach {
        case (dst, tt) =>
          val fleetSize = Math.min(org.availability, Math.abs(dst.availability))
          F.put(org.taz.tazId, F.getOrElse(org.taz.tazId, 0) - fleetSize)
          topOversuppliedTAZ.remove(org)
          if (F(org.taz.tazId) > 0) {
            topOversuppliedTAZ.add(org.copy(availability = F(org.taz.tazId)))
          }
          F.put(dst.taz.tazId, F.getOrElse(dst.taz.tazId, 0) + fleetSize)
          topUndersuppliedTAZ.remove(dst)
          if (F(dst.taz.tazId) < 0) {
            topUndersuppliedTAZ.add(dst.copy(availability = F(dst.taz.tazId)))
          }
          ODs.append((org, dst, tt, fleetSize))
      }
    }

    val vehiclesForReposition = mutable.ListBuffer.empty[(BeamVehicle, SpaceTime, Id[TAZ], SpaceTime, Id[TAZ])]
    var fleetTemp = availableFleet
    ODs.foreach {
      case (org, dst, tt, fleetSizeToReposition) =>
        val arrivalTime = now + tt
        val vehiclesForRepositionTemp =
          mutable.ListBuffer.empty[(BeamVehicle, SpaceTime, Id[TAZ], SpaceTime, Id[TAZ])]
        fleetTemp
          .filter(
            v =>
              org.taz == beamServices.beamScenario.tazTreeMap
                .getTAZ(v.spaceTime.loc.getX, v.spaceTime.loc.getY)
          )
          .take(fleetSizeToReposition)
          .map(
            (
              _,
              SpaceTime(org.taz.coord, now),
              org.taz.tazId,
              SpaceTime(dst.taz.coord, arrivalTime),
              dst.taz.tazId
            )
          )
          .foreach(vehiclesForRepositionTemp.append(_))
        fleetTemp = fleetTemp.filter(x => !vehiclesForRepositionTemp.exists(_._1 == x))
        vehiclesForReposition.appendAll(vehiclesForRepositionTemp)
    }
    vehiclesForReposition.toList
  }
}
