package beam.agentsim.agents.ridehail.repositioningmanager

import java.util.concurrent.TimeUnit

import beam.agentsim.agents.ridehail.RideHailManager
import beam.agentsim.agents.vehicles.VehicleProtocol.StreetVehicle
import beam.agentsim.events.SpaceTime
import beam.router.BeamRouter.{Location, RoutingRequest, RoutingResponse}
import beam.router.Modes.BeamMode.CAR
import beam.sim.BeamServices
import org.matsim.api.core.v01.Id
import org.matsim.vehicles.Vehicle

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.reflect.ClassTag
import akka.pattern._
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging

abstract class RepositioningManager(
  private val beamServices: BeamServices,
  private val rideHailManager: RideHailManager
) {
  def repositionVehicles(tick: Int): Vector[(Id[Vehicle], Location)]
}

object RepositioningManager {

  def apply[T <: RepositioningManager](beamServices: BeamServices, rideHailManager: RideHailManager)(
    implicit ct: ClassTag[T]
  ): T = {
    val constructors = ct.runtimeClass.getDeclaredConstructors
    require(
      constructors.size == 1,
      s"Only one constructor is allowed for RepositioningManager, but $ct has ${constructors.length}"
    )
    constructors.head.newInstance(beamServices, rideHailManager).asInstanceOf[T]
  }
}

class DefaultRepositioningManager(val beamServices: BeamServices, val rideHailManager: RideHailManager)
    extends RepositioningManager(beamServices, rideHailManager) {
  override def repositionVehicles(tick: Int): Vector[(Id[Vehicle], Location)] = Vector.empty
}

class SamePlaceRepositioningManager(val beamServices: BeamServices, val rideHailManager: RideHailManager)
    extends RepositioningManager(beamServices, rideHailManager)
    with LazyLogging {
  implicit val timeout: Timeout = Timeout(50000, TimeUnit.SECONDS)

  var totalDistDifference: Double = 0.0

  override def repositionVehicles(tick: Int): Vector[(Id[Vehicle], Location)] = {
    val map = rideHailManager.vehicleManager.getIdleVehiclesAndFilterOutExluded
    val futures = map.map {
      case (vehId, rha) =>
        val rideHailVehicleAtOrigin = StreetVehicle(
          rha.vehicleId,
          rha.vehicleType.id,
          SpaceTime((rha.currentLocationUTM.loc, tick)),
          CAR,
          asDriver = false
        )
        val routingRequest = RoutingRequest(
          originUTM = rha.currentLocationUTM.loc,
          destinationUTM = rha.currentLocationUTM.loc,
          departureTime = tick,
          withTransit = false,
          streetVehicles = Vector(rideHailVehicleAtOrigin)
        )
        rideHailManager.router.ask(routingRequest).mapTo[RoutingResponse].map(r => (vehId, r))
    }
    val res = Await.result(Future.sequence(futures), timeout.duration).toMap
    val finalResult = res.map {
      case (vehId, resp) =>
        val spaces =
          resp.itineraries.flatMap(_.legs.map(_.beamLeg.travelPath.endPoint)).map(beamServices.geo.wgs2Utm(_))
        val whereToRepos =
          if (spaces.isEmpty) map(vehId).currentLocationUTM.loc
          else {
            spaces.head.loc
          }
        vehId -> whereToRepos
    }.toVector

    val dist = finalResult.map {
      case (vehId, s) =>
        beamServices.geo.distUTMInMeters(map(vehId).currentLocationUTM.loc, s)
    }.sum
    totalDistDifference += dist

    logger.info(
      s"Sum of differences in the distance for the repositioning to the same location in the same tick $tick is $dist. totalDistDifference: $totalDistDifference"
    )
    finalResult
  }
}
