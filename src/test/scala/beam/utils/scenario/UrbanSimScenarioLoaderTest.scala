package beam.utils.scenario

import beam.router.gtfs.FareCalculator
import beam.sim.BeamHelper
import beam.sim.common.GeoUtilsImpl
import beam.sim.config.{BeamConfig, MatSimBeamConfigBuilder}
import beam.sim.config.BeamConfig.Beam.Exchange
import beam.utils.TestConfigUtils.testConfig
import beam.utils.scenario.urbansim.censusblock.{ScenarioAdjuster, UrbansimReaderV2}
import com.typesafe.config.{Config, ConfigValueFactory}
import org.matsim.core.scenario.{MutableScenario, ScenarioBuilder}
import org.scalatest.{FlatSpec, Matchers}

class UrbanSimScenarioLoaderTest extends FlatSpec with Matchers with BeamHelper {

  val config: Config = testConfig("test/input/detroit/detroit-test.conf").resolve()
  val beamConfig: BeamConfig = BeamConfig(config)
  val scenarioConfig: Exchange.Scenario = beamConfig.beam.exchange.scenario
  val src: String = scenarioConfig.source.toLowerCase
  val configBuilder = new MatSimBeamConfigBuilder(config)
  val matsimConfig = configBuilder.buildMatSimConf()

  it should "compare" in {
    1 should be (3 - 2)
  }

  it should "initialize" ignore {
    val beamScenario = loadScenario(beamConfig)
    val emptyScenario = ScenarioBuilder(matsimConfig, beamScenario.network).build

    val scenario = {
      val source =  {
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
