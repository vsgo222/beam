package beam.agentsim.agents.choice.logit

import beam.agentsim.agents.choice.logit.LatentClassChoiceModel.{LccmData, Mandatory, NonMandatory, TourType}
import beam.router.Modes.BeamMode
import beam.router.model.EmbodiedBeamTrip
import beam.sim.BeamServices
import org.matsim.core.utils.io.IOUtils
import org.supercsv.cellprocessor.constraint.NotNull
import org.supercsv.cellprocessor.ift.CellProcessor
import org.supercsv.cellprocessor.{Optional, ParseDouble}
import org.supercsv.io.CsvBeanReader
import org.supercsv.prefs.CsvPreference

import scala.beans.BeanProperty
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class LatentClassChoiceModel(val beamServices: BeamServices) {

  private val lccmData: Seq[LccmData] = parseModeChoiceParams(
    beamServices.beamConfig.beam.agentsim.agents.modalBehaviors.lccm.filePath
  )

  val classMembershipModelMaps: Map[TourType, Map[String, Map[String, UtilityFunctionOperation]]] =
    LatentClassChoiceModel.extractClassMembershipModels(lccmData)

  val classMembershipModels: Map[TourType, MultinomialLogit[String, String]] = classMembershipModelMaps.mapValues {
    modelMap =>
      MultinomialLogit(modelMap)
  }

  val modeChoiceModels
    : Map[TourType, Map[String, (MultinomialLogit[EmbodiedBeamTrip, String], MultinomialLogit[BeamMode, String])]] = {
    LatentClassChoiceModel.extractModeChoiceModels(lccmData)
  }

  val modeChoiceTourModels
  : Map[TourType, Map[String, (MultinomialLogit[EmbodiedBeamTrip, String], MultinomialLogit[BeamMode, String])]] = {
    LatentClassChoiceModel.extractModeChoiceTourModels(lccmData)
  }

  private def parseModeChoiceParams(lccmParamsFileName: String): Seq[LccmData] = {
    val beanReader = new CsvBeanReader(
      IOUtils.getBufferedReader(lccmParamsFileName),
      CsvPreference.STANDARD_PREFERENCE
    )
    val firstLineCheck = true
    val header = beanReader.getHeader(firstLineCheck)
    val processors: Array[CellProcessor] = LatentClassChoiceModel.getProcessors

    val result = mutable.ArrayBuffer[LccmData]()
    var row: LccmData = newEmptyRow()
    while (beanReader.read[LccmData](row, header, processors: _*) != null) {
      if (Option(row.value).isDefined && !row.value.isNaN)
        result += row.clone().asInstanceOf[LccmData]
      row = newEmptyRow()
    }
    result
  }

  private def newEmptyRow(): LccmData = new LccmData()

}

object LatentClassChoiceModel {

  private def getProcessors: Array[CellProcessor] = {
    Array[CellProcessor](
      new NotNull, // model
      new NotNull, // tourType
      new NotNull, // variable
      new NotNull, // alternative
      new NotNull, // units
      new Optional, // latentClass
      new Optional(new ParseDouble()) // value
    )
  }

  class LccmData(
    @BeanProperty var model: String = "",
    @BeanProperty var tourType: String = "",
    @BeanProperty var variable: String = "",
    @BeanProperty var alternative: String = "",
    @BeanProperty var units: String = "",
    @BeanProperty var latentClass: String = "",
    @BeanProperty var value: Double = Double.NaN
  ) extends Cloneable {
    override def clone(): AnyRef =
      new LccmData(model, tourType, variable, alternative, units, latentClass, value)
  }

  private def extractClassMembershipModels(
    lccmData: Seq[LccmData]
  ): Map[TourType, Map[String, Map[String, UtilityFunctionOperation]]] = {
    val classMemData = lccmData.filter(_.model == "classMembership")
    Vector[TourType](Mandatory, NonMandatory).map { theTourType =>
      val theData = classMemData.filter(_.tourType.equalsIgnoreCase(theTourType.toString))
      val utilityFunctions: Iterable[(String, Map[String, UtilityFunctionOperation])] = for {
        data          <- theData
        //alternativeId <- data.alternative
      } yield {
        (data.alternative, Map(data.variable -> UtilityFunctionOperation(data.variable, data.value)))
      }
      theTourType -> utilityFunctions.toMap
    }.toMap
  }

  sealed trait TourType

  case object Mandatory extends TourType

  case object NonMandatory extends TourType

  //method where common utility values can be specified
  def getCommonUtility: Map[String, UtilityFunctionOperation] = {
    Map(
      "cost" -> UtilityFunctionOperation("multiplier", 0),
      "time" -> UtilityFunctionOperation("multiplier", 0),
      "transfer" -> UtilityFunctionOperation("multiplier", 0)
    )
  }

  /*
     * We use presence of ASC to indicate whether an alternative should be added to the MNL model. So even if an alternative is a base alternative,
     * it should be given an ASC with value of 0.0 in order to be added to the choice set.
     */
  def extractModeChoiceModels(
    lccmData: Seq[LccmData]
  ): Map[TourType, Map[String, (MultinomialLogit[EmbodiedBeamTrip, String], MultinomialLogit[BeamMode, String])]] = {
    val uniqueClasses = lccmData.map(_.latentClass).distinct
    val uniqueAlts = ArrayBuffer("bike", "car", "drive_transit", "ride_hail", "walk", "walk_transit")
    val modeChoiceData = lccmData.filter(_.model == "modeChoice")
    Vector[TourType](Mandatory, NonMandatory).map { theTourType: TourType =>
      val theTourTypeData = modeChoiceData.filter(_.tourType.equalsIgnoreCase(theTourType.toString))
      theTourType -> uniqueClasses.map { theLatentClass =>
        val theData = theTourTypeData.filter(_.latentClass.equalsIgnoreCase(theLatentClass))
        val utilityFunctions: Iterable[(String, Map[String, UtilityFunctionOperation])] = for {
          data          <- theData
        } yield {
          (data.alternative, Map(data.variable -> UtilityFunctionOperation(data.variable, data.value)))
        }
          //group together all utility parameter values for each alternative
          var utilMap = Map[String, Map[String, UtilityFunctionOperation]]()
          utilMap -> uniqueAlts.map{ theAlternative =>
            val theAltData = utilityFunctions.toArray.filter(_._1.equalsIgnoreCase(theAlternative))
            var altFunction: Map[String,UtilityFunctionOperation] = Map()
            for { data    <- theAltData } {
              var (mode, param, value) = (data._1, data._2.head._1, data._2.head._2)
              altFunction += (param -> value)
              utilMap += (mode -> altFunction)
            }
            theAlternative -> utilMap
          }
        val utilityFunctionMap = utilMap
        val commonUtility = getCommonUtility
        theLatentClass -> (
          new MultinomialLogit[EmbodiedBeamTrip, String](trip => utilityFunctionMap.get(trip.tripClassifier.value), commonUtility),
          new MultinomialLogit[BeamMode, String](mode => utilityFunctionMap.get(mode.value), commonUtility)
        )
      }.toMap
    }.toMap
  }

  def extractModeChoiceTourModels(
    lccmData: Seq[LccmData]
  ): Map[TourType, Map[String, (MultinomialLogit[EmbodiedBeamTrip, String], MultinomialLogit[BeamMode, String])]] = {
    val uniqueClasses = lccmData.map(_.latentClass).distinct
    val uniqueAlts = lccmData.map(_.alternative).distinct
    val modeChoiceData = lccmData.filter(_.model == "modeChoice")
    Vector[TourType](Mandatory, NonMandatory).map { theTourType: TourType =>
      val theTourTypeData = modeChoiceData.filter(_.tourType.equalsIgnoreCase(theTourType.toString))
      theTourType -> uniqueClasses.map { theTourPurpose =>
        val theData = theTourTypeData.filter(_.latentClass.equalsIgnoreCase(theTourPurpose))

        val utilityFunctions: Iterable[(String, Map[String, UtilityFunctionOperation])] = for {
          data          <- theData
        } yield {
          (data.alternative, Map(data.variable -> UtilityFunctionOperation(data.variable, data.value)))
        }
          //group together all utility parameter values for each alternative
          var utilMap = Map[String, Map[String, UtilityFunctionOperation]]()
          utilMap -> uniqueAlts.map{ theAlternative =>
            val theAltData = utilityFunctions.toArray.filter(_._1.equalsIgnoreCase(theAlternative))
            var altFunction: Map[String,UtilityFunctionOperation] = Map()
            for { data    <- theAltData } {
              var (mode, param, value) = (data._1, data._2.head._1, data._2.head._2)
              altFunction += (param -> value)
              utilMap += (mode -> altFunction)
            }
            theAlternative -> utilMap
          }
        val utilityFunctionMap = utilMap
        val commonUtility = getCommonUtility
        theTourPurpose -> (
          new MultinomialLogit[EmbodiedBeamTrip, String](trip => utilityFunctionMap.get(trip.tripClassifier.value), commonUtility),
          new MultinomialLogit[BeamMode, String](mode => utilityFunctionMap.get(mode.value), commonUtility)
        )
      }.toMap
    }.toMap
  }

}
