package beam.utils.plan.sampling

import java.util
import scala.collection.{JavaConverters, mutable}
import beam.router.Modes.BeamMode
import beam.router.Modes.BeamMode.{BIKE, CAR, DRIVE_TRANSIT, HOV2, HOV3, WALK, WALK_TRANSIT}
import beam.sim.{BeamScenario, BeamServices}
import beam.sim.population.{AttributesOfIndividual, PopulationAdjustment}
import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.lang3.StringUtils.isBlank
import org.matsim.api.core.v01.population.{Person, Plan, Population}
import org.matsim.core.population.algorithms.PermissibleModesCalculator
import org.matsim.core.utils.io.IOUtils
import org.matsim.households.Household
import org.supercsv.cellprocessor.constraint.NotNull
import org.supercsv.cellprocessor.{Optional, ParseDouble}
import org.supercsv.cellprocessor.ift.CellProcessor
import org.supercsv.io.CsvBeanReader
import org.supercsv.prefs.CsvPreference

import scala.beans.BeanProperty


/**
  * Several utility/convenience methods for mode availability. Note that the MATSim convention
  * is to call these permissible modes. BEAM uses available modes. The semantics are identical.
  */
object AvailableModeUtils extends LazyLogging {

  object AllowAllModes extends PermissibleModesCalculator {

    override def getPermissibleModes(plan: Plan): util.Collection[String] = {
      JavaConverters.asJavaCollection(BeamMode.allModes.map(_.toString))
    }
  }

  def availableModeParser(availableModes: String): Seq[BeamMode] = {
    availableModes.split(",").toSeq map BeamMode.withValue
  }

  /**
    * Gets the excluded modes set for the given person in the population
    *
    * @param population population from the scenario
    * @param personId the respective person's id
    * @return List of excluded mode string
    */
  def getExcludedModesForPerson(population: Population, personId: String): Array[String] = {
    val maybeExcludedModes = Option(
      population.getPersonAttributes.getAttribute(personId, PopulationAdjustment.EXCLUDED_MODES)
    )
    maybeExcludedModes.map(_.toString)
    maybeExcludedModes match {
      case Some(modes: Array[String]) => modes.filterNot(isBlank)
      case Some(modes: Iterable[_])   => modes.flatMap(_.toString.split(",")).filterNot(isBlank).toArray
      case Some(modes)                => modes.toString.split(",").filterNot(isBlank)
      case _                          => Array.empty[String]
    }
  }

  /**
    * Gets the excluded modes set for the given person
    *
    * @param person the respective person
    * @return
    */
  def availableModesForPerson(person: Person): Seq[BeamMode] = {
    getPersonCustomAttributes(person).map(_.availableModes).getOrElse(Seq.empty)
  }

  def availableModesFromTourMode(setTourMode: BeamMode, beamServices: BeamServices): Seq[BeamMode] = {
    val tourModeList: Seq[tourModeData] = parseTourModeData(beamServices.beamConfig.beam.agentsim.agents.modalBehaviors.tourMode.filePath)
    getAvailableTripModesByTourMode(setTourMode, tourModeList)
  }

  /**
    * Sets the available modes for the given person in the population
    *
    * @param population population from the scenario
    * @param person the respective person
    * @param permissibleModes List of permissible modes for the person
    */
  def setAvailableModesForPerson(person: Person, population: Population, permissibleModes: Seq[String]): Unit = {
    getPersonCustomAttributes(person) match {
      case Some(attributesOfIndividual) =>
        setModesForPerson(person, population, permissibleModes, attributesOfIndividual)
      case _ =>
        logger.warn(s"Not found attributes of the individual: [$person]")
    }
  }

  private def setModesForPerson(
    person: Person,
    population: Population,
    permissibleModes: Seq[String],
    attributesOfIndividual: AttributesOfIndividual
  ): Unit = {
    val availableModes = getAvailableModesOfPerson(person, population, permissibleModes)
    try {
      val attributesUpdated =
        attributesOfIndividual.copy(availableModes = availableModes.map(f => BeamMode.withValue(f.toLowerCase)))
      person.getCustomAttributes.put(PopulationAdjustment.BEAM_ATTRIBUTES, attributesUpdated)
    } catch {
      case e: Exception =>
        logger.error("Error while converting available mode string to respective Beam Mode Enums : " + e.getMessage, e)
    }
  }

  private def getAvailableModesOfPerson(
    person: Person,
    population: Population,
    permissibleModes: Seq[String]
  ): Seq[String] = {
    val excludedModes = getExcludedModesForPerson(population, person.getId.toString)
    permissibleModes.filterNot(am => excludedModes.exists(em => em.equalsIgnoreCase(am)))
  }

  def setAvailableModesForPerson_v2(
    beamScenario: BeamScenario,
    person: Person,
    household: Household,
    population: Population,
    permissibleModes: Seq[String]
  ): Unit = {
    val attributesOfIndividual = getOrElseUpdateAttributesOfIndividual(beamScenario, person, household, population)
    setModesForPerson(person, population, permissibleModes, attributesOfIndividual)
  }

  private def getOrElseUpdateAttributesOfIndividual(
    beamScenario: BeamScenario,
    person: Person,
    household: Household,
    population: Population
  ): AttributesOfIndividual = {
    getPersonCustomAttributes(person).getOrElse {
      val attributes: AttributesOfIndividual =
        PopulationAdjustment.createAttributesOfIndividual(beamScenario, population, person, household)
      person.getCustomAttributes.put(PopulationAdjustment.BEAM_ATTRIBUTES, attributes)
      attributes
    }
  }

  private def getPersonCustomAttributes(person: Person): Option[AttributesOfIndividual] = {
    val attributes = person.getCustomAttributes
      .get(PopulationAdjustment.BEAM_ATTRIBUTES)
    Option(attributes.asInstanceOf[AttributesOfIndividual])
  }

  /**
    * Replaces the available modes given with the existing available modes for the given person
    *
    * @param person the respective person
    * @param newAvailableModes List of new available modes to replace
    */
  def replaceAvailableModesForPerson(person: Person, newAvailableModes: Seq[String]): Unit = {
    val maybeIndividual = getPersonCustomAttributes(person)
    if (maybeIndividual.isDefined) {
      val attributesOfIndividual = maybeIndividual.get
      val modes: Seq[BeamMode] = newAvailableModes.flatMap(f => selectBeamMode(f.toLowerCase))
      person.getCustomAttributes.put(
        PopulationAdjustment.BEAM_ATTRIBUTES,
        attributesOfIndividual.copy(availableModes = modes)
      )
    }
  }

  private def selectBeamMode(mode: String): Option[BeamMode] = {
    BeamMode.withValueOpt(mode) match {
      case result @ Some(_) => result
      case None =>
        logger.error(s"Error while converting available mode string [$mode] to respective Beam Mode Enums")
        None
    }
  }

  def getAvailableTripModesByTourMode(
    setTourMode: BeamMode,
    tourModeList:Seq[tourModeData]
  ): Seq[BeamMode] = {
    val tripModeList = tourModeList.toArray.filter(_.tourMode.equalsIgnoreCase(setTourMode.toString))
    val tripModes = tripModeList.head.tripModes
    availableModeParser(tripModes)
  }

  // this is used to parse through the taz location csv input file
  private def parseTourModeData(tourModeDataFileName: String): Seq[tourModeData] = {
    val beanReader = new CsvBeanReader(
      IOUtils.getBufferedReader(tourModeDataFileName),
      CsvPreference.STANDARD_PREFERENCE
    )
    val firstLineCheck = true
    val header = beanReader.getHeader(firstLineCheck)
    val processors: Array[CellProcessor] = AvailableModeUtils.getProcessors

    val result = mutable.ArrayBuffer[tourModeData]()
    var row: tourModeData = newEmptyRow()
    while (beanReader.read[tourModeData](row, header, processors: _*) != null) {
      if (Option(row.tourMode).isDefined)
        result += row.clone().asInstanceOf[tourModeData]
      row = newEmptyRow()
    }
    result
  }

  private def newEmptyRow(): tourModeData = new tourModeData()

  private def getProcessors: Array[CellProcessor] = {
    Array[CellProcessor](
      new NotNull, // TourMode
      new NotNull // TripModes
    )
  }

  class tourModeData(
    @BeanProperty var tourMode: String = "",
    @BeanProperty var tripModes: String = ""
  ) extends Cloneable {
    override def clone(): AnyRef =
      new tourModeData(tourMode, tripModes)
  }

}
