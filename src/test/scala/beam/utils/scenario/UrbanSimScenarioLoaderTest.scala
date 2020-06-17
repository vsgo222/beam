package beam.utils.scenario

import beam.sim.BeamHelper
import beam.sim.common.GeoUtilsImpl
import beam.sim.config.{BeamConfig, MatSimBeamConfigBuilder}
import beam.sim.config.BeamConfig.Beam.Exchange
import beam.utils.TestConfigUtils.testConfig
import beam.utils.scenario.urbansim.censusblock.{ScenarioAdjuster, UrbansimReaderV2}
import com.typesafe.config.{Config, ConfigValueFactory}
import org.matsim.core.scenario.{MutableScenario, ScenarioBuilder}
import org.scalatest.{FlatSpec, Matchers}
import org.matsim.api.core.v01.{Coord, Id, Scenario}
import org.matsim.core.scenario.MutableScenario
import org.matsim.core.utils.geometry.geotools.MGC
import org.matsim.core.utils.gis.{PointFeatureFactory, ShapeFileWriter}
import org.opengis.feature.simple.SimpleFeature

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class UrbanSimScenarioLoaderTest extends FlatSpec with Matchers with BeamHelper {

  def createShapeFile(coords: Traversable[Coord], shapeFileOutputPath: String, crs: String): Unit = {
    val features = ArrayBuffer[SimpleFeature]()

    val pointf: PointFeatureFactory = new PointFeatureFactory.Builder()
      .setCrs(MGC.getCRS(crs))
      .setName("nodes")
      .create()

    coords.foreach { wsgCoord =>
      val coord = new com.vividsolutions.jts.geom.Coordinate(wsgCoord.getX, wsgCoord.getY)
      val feature = pointf.createPoint(coord)
      features += feature
    }

    ShapeFileWriter.writeGeometries(features.asJava, shapeFileOutputPath)
    logger.error(s"Shape file $shapeFileOutputPath was written.")
  }

  it should "initialize" in {
    val config: Config = testConfig("test/input/detroit/detroit-200k.conf").resolve()
    val beamConfig: BeamConfig = BeamConfig(config)
    val scenarioConfig: Exchange.Scenario = beamConfig.beam.exchange.scenario
    val src: String = scenarioConfig.source.toLowerCase
    val configBuilder = new MatSimBeamConfigBuilder(config)
    val matsimConfig = configBuilder.buildMatSimConf()
    val beamScenario = loadScenario(beamConfig)
    val emptyScenario = ScenarioBuilder(matsimConfig, beamScenario.network).build

    val source = {
      val pathToHouseholds = s"${beamConfig.beam.exchange.scenario.folder}/households.csv.gz"
      val pathToPersonFile = s"${beamConfig.beam.exchange.scenario.folder}/persons.csv.gz"
      val pathToPlans = s"${beamConfig.beam.exchange.scenario.folder}/plans.csv.gz"
      val pathToTrips = s"${beamConfig.beam.exchange.scenario.folder}/trips.csv.gz"
      val pathToBlocks = s"${beamConfig.beam.exchange.scenario.folder}/blocks.csv.gz"
      new UrbansimReaderV2(
        inputPersonPath = pathToPersonFile,
        inputPlanPath = pathToPlans,
        inputHouseholdPath = pathToHouseholds,
        inputTripsPath = pathToTrips,
        inputBlockPath = pathToBlocks
      )
    }

    val plansCoords = mutable.ListBuffer.empty[Coord]
    source.getPlans.foreach { plan =>
      (plan.activityLocationX, plan.activityLocationY) match {
        case (Some(x), Some(y)) => plansCoords += new Coord(x, y)
        case _                  =>
      }
    }

    createShapeFile(plansCoords, "coords-all-plans.shp", "EPSG:4326")

    val hhCoords = mutable.ListBuffer.empty[Coord]
    source.getHousehold.foreach { hh =>
      hhCoords += new Coord(hh.locationX, hh.locationY)
    }

    createShapeFile(hhCoords, "coords-all-hh.shp", "EPSG:4326")

    val scenario = {
      val scenario =
        new UrbanSimScenarioLoader(emptyScenario, beamScenario, source, new GeoUtilsImpl(beamConfig)).loadScenario()
      if (src == "urbansim_v2") {
        new ScenarioAdjuster(
          beamConfig.beam.urbansim,
          scenario.getPopulation,
          beamConfig.matsim.modules.global.randomSeed
        ).adjust()
      }
      scenario
    }.asInstanceOf[MutableScenario]

    scenario shouldBe a[MutableScenario]
  }
}
