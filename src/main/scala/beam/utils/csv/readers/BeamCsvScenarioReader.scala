package beam.utils.csv.readers

import java.util.{Map => JavaMap}

import beam.utils.csv.writers.ScenarioCsvWriter.ArrayItemSeparator
import beam.utils.logging.ExponentialLazyLogging
import beam.utils.scenario.matsim.BeamScenarioReader
import beam.utils.{FileUtils, ProfilingUtils}
import beam.utils.scenario._
import org.apache.commons.lang3.math.NumberUtils
import org.supercsv.io.CsvMapReader
import org.supercsv.prefs.CsvPreference

import scala.reflect.ClassTag
import scala.util.Try

object BeamCsvScenarioReader extends BeamScenarioReader with ExponentialLazyLogging {
  override def inputType: InputType = InputType.CSV

  override def readPersonsFile(path: String): Array[PersonInfo] = {
    readAs[PersonInfo](path, "readPersonsFile", toPersonInfo)
  }

  override def readPlansFile(path: String): Array[PlanElement] = {
    readAs[PlanElement](path, "readPlansFile", toPlanInfo)
  }

  override def readHouseholdsFile(householdsPath: String, vehicles: Iterable[VehicleInfo]): Array[HouseholdInfo] = {
    val householdToNumberOfCars = vehicles.groupBy(_.householdId).map { case (householdId, listOfCars) =>
      (householdId, listOfCars.size)
    }
    readAs[HouseholdInfo](householdsPath, "readHouseholdsFile", toHouseholdInfo(householdToNumberOfCars))
  }

  private[readers] def readAs[T](path: String, what: String, mapper: JavaMap[String, String] => T)(implicit
    ct: ClassTag[T]
  ): Array[T] = {
    ProfilingUtils.timed(what, x => logger.info(x)) {
      FileUtils.using(new CsvMapReader(FileUtils.readerFromFile(path), CsvPreference.STANDARD_PREFERENCE)) { csvRdr =>
        val header = csvRdr.getHeader(true)
        Iterator.continually(csvRdr.read(header: _*)).takeWhile(_ != null).map(mapper).toArray
      }
    }
  }

  private[readers] def toHouseholdInfo(
    householdIdToVehiclesSize: Map[String, Int]
  )(rec: JavaMap[String, String]): HouseholdInfo = {
    val householdId = getIfNotNull(rec, "household_id")
    val cars = householdIdToVehiclesSize.get(householdId) match {
      case Some(total) => total
      case None        =>
        // The dots here are due to an idiosyncrasy with how ExponentialLazyLogger works....
        // it has to track how many times a log message has been requested and it uses the first
        // 20 characters as a unique key... if message is fewer than 20 characters the intended
        // behavior fails.
        logger.warn(s"HouseholdId has no cars associated........Id [$householdId]")
        0
    }
    HouseholdInfo(
      householdId = HouseholdId(householdId),
      cars = cars,
      income = getIfNotNull(rec, "income").toDouble,
      locationX = getIfNotNull(rec, "x").toDouble,
      locationY = getIfNotNull(rec, "y").toDouble
    )
  }

  private[readers] def toPlanInfo(rec: java.util.Map[String, String]): PlanElement = {
    val personId = getIfNotNull(rec, "person_id")
    val planIndex = getIfNotNull(rec, "planIndex", default = "0").toInt
    val planElementType = getIfNotNull(rec, "ActivityElement")
    val planElementIndex = getIfNotNull(rec, "PlanElementIndex").toInt
    val activityType = Option(rec.get("ActivityType"))
    val linkIds =
      Option(rec.get("legRouteLinks")).map(_.split(ArrayItemSeparator).map(_.trim)).getOrElse(Array.empty[String])
    PlanElement(
      personId = PersonId(personId),
      planIndex = planIndex,
      planScore = getIfNotNull(rec, "planScore", "0").toDouble,
      planSelected = getIfNotNull(rec, "planSelected", "true").toBoolean,
      planElementType = PlanElement.PlanElementType(planElementType),
      planElementIndex = planElementIndex,
      activityType = activityType,
      activityLocationX = Option(rec.get("x")).map(_.toDouble),
      activityLocationY = Option(rec.get("y")).map(_.toDouble),
      activityEndTime = Option(rec.get("departure_time")).map(_.toDouble),
      legMode = Option(rec.get("trip_mode")),
      legDepartureTime = Option(rec.get("legDepartureTime")),
      legTravelTime = Option(rec.get("legTravelTime")),
      legRouteType = Option(rec.get("legRouteType")),
      legRouteStartLink = Option(rec.get("legRouteStartLink")),
      legRouteEndLink = Option(rec.get("legRouteEndLink")),
      legRouteTravelTime = Option(rec.get("legRouteTravelTime")).map(_.toDouble),
      legRouteDistance = Option(rec.get("legRouteDistance")).map(_.toDouble),
      legRouteLinks = linkIds,
      geoId = Option(rec.get("geoId"))
    )
  }

  private def toPersonInfo(rec: JavaMap[String, String]): PersonInfo = {
    val personId = getIfNotNull(rec, "person_id")
    val householdId = getIfNotNull(rec, "household_id")
    val age = getIfNotNull(rec, "age").toInt
    val isFemale = getIfNotNull(rec, "female", "false").toBoolean
    val rank = getIfNotNull(rec, "householdRank", "1").toInt
    val excludedModes = Try(getIfNotNull(rec, "excludedModes")).getOrElse("").split(",")
    val valueOfTime = NumberUtils.toDouble(Try(getIfNotNull(rec, "value_of_time", "0")).getOrElse("0"), 0d)
    PersonInfo(
      personId = PersonId(personId),
      householdId = HouseholdId(householdId),
      rank = rank,
      age = age,
      excludedModes = excludedModes,
      isFemale = isFemale,
      valueOfTime = valueOfTime
    )
  }

  private def toVehicle(rec: JavaMap[String, String]): VehicleInfo = {
    VehicleInfo(
      vehicleId = getIfNotNull(rec, "vehicleId"),
      vehicleTypeId = getIfNotNull(rec, "vehicleTypeId"),
      householdId = getIfNotNull(rec, "householdId")
    )
  }

  private def getIfNotNull(rec: JavaMap[String, String], column: String, default: String = null): String = {
    val v = rec.getOrDefault(column, default)
    assert(v != null, s"Value in column '$column' is null")
    v
  }

  override def readVehiclesFile(vehiclesFilePath: String): Iterable[VehicleInfo] = {
    readAs[VehicleInfo](vehiclesFilePath, "vehiclesFile", toVehicle)
  }
}
