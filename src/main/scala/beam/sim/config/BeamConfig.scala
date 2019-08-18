// source: src/main/resources/beam-template.conf

package beam.sim.config

case class BeamConfig(
  beam: BeamConfig.Beam,
  matsim: BeamConfig.Matsim
)

object BeamConfig {
  case class Beam(
    agentsim: BeamConfig.Beam.Agentsim,
    beamskimmer: BeamConfig.Beam.Beamskimmer,
    calibration: BeamConfig.Beam.Calibration,
    cluster: BeamConfig.Beam.Cluster,
    debug: BeamConfig.Beam.Debug,
    exchange: BeamConfig.Beam.Exchange,
    experimental: BeamConfig.Beam.Experimental,
    inputDirectory: java.lang.String,
    logger: BeamConfig.Beam.Logger,
    metrics: BeamConfig.Beam.Metrics,
    outputs: BeamConfig.Beam.Outputs,
    physsim: BeamConfig.Beam.Physsim,
    replanning: BeamConfig.Beam.Replanning,
    routing: BeamConfig.Beam.Routing,
    spatial: BeamConfig.Beam.Spatial,
    useLocalWorker: scala.Boolean,
    warmStart: BeamConfig.Beam.WarmStart
  )

  object Beam {
    case class Agentsim(
      agentSampleSizeAsFractionOfPopulation: scala.Double,
      agents: BeamConfig.Beam.Agentsim.Agents,
      endTime: java.lang.String,
      firstIteration: scala.Int,
      lastIteration: scala.Int,
      populationAdjustment: java.lang.String,
      scenarios: BeamConfig.Beam.Agentsim.Scenarios,
      scheduleMonitorTask: BeamConfig.Beam.Agentsim.ScheduleMonitorTask,
      schedulerParallelismWindow: scala.Int,
      simulationName: java.lang.String,
      startTime: java.lang.String,
      taz: BeamConfig.Beam.Agentsim.Taz,
      thresholdForMakingParkingChoiceInMeters: scala.Int,
      thresholdForWalkingInMeters: scala.Int,
      timeBinSize: scala.Int,
      toll: BeamConfig.Beam.Agentsim.Toll,
      tuning: BeamConfig.Beam.Agentsim.Tuning
    )

    object Agentsim {
      case class Agents(
        bodyType: java.lang.String,
        households: BeamConfig.Beam.Agentsim.Agents.Households,
        modalBehaviors: BeamConfig.Beam.Agentsim.Agents.ModalBehaviors,
        modeIncentive: BeamConfig.Beam.Agentsim.Agents.ModeIncentive,
        parking: BeamConfig.Beam.Agentsim.Agents.Parking,
        plans: BeamConfig.Beam.Agentsim.Agents.Plans,
        population: BeamConfig.Beam.Agentsim.Agents.Population,
        ptFare: BeamConfig.Beam.Agentsim.Agents.PtFare,
        rideHail: BeamConfig.Beam.Agentsim.Agents.RideHail,
        rideHailTransit: BeamConfig.Beam.Agentsim.Agents.RideHailTransit,
        vehicles: BeamConfig.Beam.Agentsim.Agents.Vehicles
      )

      object Agents {
        case class Households(
          inputFilePath: java.lang.String,
          inputHouseholdAttributesFilePath: java.lang.String
        )

        object Households {

          def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Agentsim.Agents.Households = {
            BeamConfig.Beam.Agentsim.Agents.Households(
              inputFilePath =
                if (c.hasPathOrNull("inputFilePath")) c.getString("inputFilePath")
                else "/test/input/beamville/households.xml.gz",
              inputHouseholdAttributesFilePath =
                if (c.hasPathOrNull("inputHouseholdAttributesFilePath")) c.getString("inputHouseholdAttributesFilePath")
                else "/test/input/beamville/householdAttributes.xml.gz"
            )
          }
        }

        case class ModalBehaviors(
          defaultValueOfTime: scala.Double,
          highTimeSensitivity: BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.HighTimeSensitivity,
          lccm: BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.Lccm,
          lowTimeSensitivity: BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.LowTimeSensitivity,
          maximumNumberOfReplanningAttempts: scala.Int,
          modeChoiceClass: java.lang.String,
          modeVotMultiplier: BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.ModeVotMultiplier,
          mulitnomialLogit: BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.MulitnomialLogit,
          overrideAutomationForVOTT: scala.Boolean,
          overrideAutomationLevel: scala.Int,
          poolingMultiplier: BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.PoolingMultiplier
        )

        object ModalBehaviors {
          case class HighTimeSensitivity(
            highCongestion: BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.HighTimeSensitivity.HighCongestion,
            lowCongestion: BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.HighTimeSensitivity.LowCongestion
          )

          object HighTimeSensitivity {
            case class HighCongestion(
              highwayFactor: BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.HighTimeSensitivity.HighCongestion.HighwayFactor,
              nonHighwayFactor: BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.HighTimeSensitivity.HighCongestion.NonHighwayFactor
            )

            object HighCongestion {
              case class HighwayFactor(
                Level3: scala.Double,
                Level4: scala.Double,
                Level5: scala.Double,
                LevelLE2: scala.Double
              )

              object HighwayFactor {

                def apply(
                  c: com.typesafe.config.Config
                ): BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.HighTimeSensitivity.HighCongestion.HighwayFactor = {
                  BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.HighTimeSensitivity.HighCongestion.HighwayFactor(
                    Level3 = if (c.hasPathOrNull("Level3")) c.getDouble("Level3") else 1.0,
                    Level4 = if (c.hasPathOrNull("Level4")) c.getDouble("Level4") else 1.0,
                    Level5 = if (c.hasPathOrNull("Level5")) c.getDouble("Level5") else 1.0,
                    LevelLE2 = if (c.hasPathOrNull("LevelLE2")) c.getDouble("LevelLE2") else 1.0
                  )
                }
              }

              case class NonHighwayFactor(
                Level3: scala.Double,
                Level4: scala.Double,
                Level5: scala.Double,
                LevelLE2: scala.Double
              )

              object NonHighwayFactor {

                def apply(
                  c: com.typesafe.config.Config
                ): BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.HighTimeSensitivity.HighCongestion.NonHighwayFactor = {
                  BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.HighTimeSensitivity.HighCongestion.NonHighwayFactor(
                    Level3 = if (c.hasPathOrNull("Level3")) c.getDouble("Level3") else 1.0,
                    Level4 = if (c.hasPathOrNull("Level4")) c.getDouble("Level4") else 1.0,
                    Level5 = if (c.hasPathOrNull("Level5")) c.getDouble("Level5") else 1.0,
                    LevelLE2 = if (c.hasPathOrNull("LevelLE2")) c.getDouble("LevelLE2") else 1.0
                  )
                }
              }

              def apply(
                c: com.typesafe.config.Config
              ): BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.HighTimeSensitivity.HighCongestion = {
                BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.HighTimeSensitivity.HighCongestion(
                  highwayFactor =
                    BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.HighTimeSensitivity.HighCongestion.HighwayFactor(
                      if (c.hasPathOrNull("highwayFactor")) c.getConfig("highwayFactor")
                      else com.typesafe.config.ConfigFactory.parseString("highwayFactor{}")
                    ),
                  nonHighwayFactor =
                    BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.HighTimeSensitivity.HighCongestion.NonHighwayFactor(
                      if (c.hasPathOrNull("nonHighwayFactor")) c.getConfig("nonHighwayFactor")
                      else com.typesafe.config.ConfigFactory.parseString("nonHighwayFactor{}")
                    )
                )
              }
            }

            case class LowCongestion(
              highwayFactor: BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.HighTimeSensitivity.LowCongestion.HighwayFactor,
              nonHighwayFactor: BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.HighTimeSensitivity.LowCongestion.NonHighwayFactor
            )

            object LowCongestion {
              case class HighwayFactor(
                Level3: scala.Double,
                Level4: scala.Double,
                Level5: scala.Double,
                LevelLE2: scala.Double
              )

              object HighwayFactor {

                def apply(
                  c: com.typesafe.config.Config
                ): BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.HighTimeSensitivity.LowCongestion.HighwayFactor = {
                  BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.HighTimeSensitivity.LowCongestion.HighwayFactor(
                    Level3 = if (c.hasPathOrNull("Level3")) c.getDouble("Level3") else 1.0,
                    Level4 = if (c.hasPathOrNull("Level4")) c.getDouble("Level4") else 1.0,
                    Level5 = if (c.hasPathOrNull("Level5")) c.getDouble("Level5") else 1.0,
                    LevelLE2 = if (c.hasPathOrNull("LevelLE2")) c.getDouble("LevelLE2") else 1.0
                  )
                }
              }

              case class NonHighwayFactor(
                Level3: scala.Double,
                Level4: scala.Double,
                Level5: scala.Double,
                LevelLE2: scala.Double
              )

              object NonHighwayFactor {

                def apply(
                  c: com.typesafe.config.Config
                ): BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.HighTimeSensitivity.LowCongestion.NonHighwayFactor = {
                  BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.HighTimeSensitivity.LowCongestion.NonHighwayFactor(
                    Level3 = if (c.hasPathOrNull("Level3")) c.getDouble("Level3") else 1.0,
                    Level4 = if (c.hasPathOrNull("Level4")) c.getDouble("Level4") else 1.0,
                    Level5 = if (c.hasPathOrNull("Level5")) c.getDouble("Level5") else 1.0,
                    LevelLE2 = if (c.hasPathOrNull("LevelLE2")) c.getDouble("LevelLE2") else 1.0
                  )
                }
              }

              def apply(
                c: com.typesafe.config.Config
              ): BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.HighTimeSensitivity.LowCongestion = {
                BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.HighTimeSensitivity.LowCongestion(
                  highwayFactor =
                    BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.HighTimeSensitivity.LowCongestion.HighwayFactor(
                      if (c.hasPathOrNull("highwayFactor")) c.getConfig("highwayFactor")
                      else com.typesafe.config.ConfigFactory.parseString("highwayFactor{}")
                    ),
                  nonHighwayFactor =
                    BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.HighTimeSensitivity.LowCongestion.NonHighwayFactor(
                      if (c.hasPathOrNull("nonHighwayFactor")) c.getConfig("nonHighwayFactor")
                      else com.typesafe.config.ConfigFactory.parseString("nonHighwayFactor{}")
                    )
                )
              }
            }

            def apply(
              c: com.typesafe.config.Config
            ): BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.HighTimeSensitivity = {
              BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.HighTimeSensitivity(
                highCongestion = BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.HighTimeSensitivity.HighCongestion(
                  if (c.hasPathOrNull("highCongestion")) c.getConfig("highCongestion")
                  else com.typesafe.config.ConfigFactory.parseString("highCongestion{}")
                ),
                lowCongestion = BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.HighTimeSensitivity.LowCongestion(
                  if (c.hasPathOrNull("lowCongestion")) c.getConfig("lowCongestion")
                  else com.typesafe.config.ConfigFactory.parseString("lowCongestion{}")
                )
              )
            }
          }

          case class Lccm(
            filePath: java.lang.String
          )

          object Lccm {

            def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.Lccm = {
              BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.Lccm(
                filePath =
                  if (c.hasPathOrNull("filePath")) c.getString("filePath") else "/test/input/beamville/lccm-long.csv"
              )
            }
          }

          case class LowTimeSensitivity(
            highCongestion: BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.LowTimeSensitivity.HighCongestion,
            lowCongestion: BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.LowTimeSensitivity.LowCongestion
          )

          object LowTimeSensitivity {
            case class HighCongestion(
              highwayFactor: BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.LowTimeSensitivity.HighCongestion.HighwayFactor,
              nonHighwayFactor: BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.LowTimeSensitivity.HighCongestion.NonHighwayFactor
            )

            object HighCongestion {
              case class HighwayFactor(
                Level3: scala.Double,
                Level4: scala.Double,
                Level5: scala.Double,
                LevelLE2: scala.Double
              )

              object HighwayFactor {

                def apply(
                  c: com.typesafe.config.Config
                ): BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.LowTimeSensitivity.HighCongestion.HighwayFactor = {
                  BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.LowTimeSensitivity.HighCongestion.HighwayFactor(
                    Level3 = if (c.hasPathOrNull("Level3")) c.getDouble("Level3") else 1.0,
                    Level4 = if (c.hasPathOrNull("Level4")) c.getDouble("Level4") else 1.0,
                    Level5 = if (c.hasPathOrNull("Level5")) c.getDouble("Level5") else 1.0,
                    LevelLE2 = if (c.hasPathOrNull("LevelLE2")) c.getDouble("LevelLE2") else 1.0
                  )
                }
              }

              case class NonHighwayFactor(
                Level3: scala.Double,
                Level4: scala.Double,
                Level5: scala.Double,
                LevelLE2: scala.Double
              )

              object NonHighwayFactor {

                def apply(
                  c: com.typesafe.config.Config
                ): BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.LowTimeSensitivity.HighCongestion.NonHighwayFactor = {
                  BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.LowTimeSensitivity.HighCongestion.NonHighwayFactor(
                    Level3 = if (c.hasPathOrNull("Level3")) c.getDouble("Level3") else 1.0,
                    Level4 = if (c.hasPathOrNull("Level4")) c.getDouble("Level4") else 1.0,
                    Level5 = if (c.hasPathOrNull("Level5")) c.getDouble("Level5") else 1.0,
                    LevelLE2 = if (c.hasPathOrNull("LevelLE2")) c.getDouble("LevelLE2") else 1.0
                  )
                }
              }

              def apply(
                c: com.typesafe.config.Config
              ): BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.LowTimeSensitivity.HighCongestion = {
                BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.LowTimeSensitivity.HighCongestion(
                  highwayFactor =
                    BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.LowTimeSensitivity.HighCongestion.HighwayFactor(
                      if (c.hasPathOrNull("highwayFactor")) c.getConfig("highwayFactor")
                      else com.typesafe.config.ConfigFactory.parseString("highwayFactor{}")
                    ),
                  nonHighwayFactor =
                    BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.LowTimeSensitivity.HighCongestion.NonHighwayFactor(
                      if (c.hasPathOrNull("nonHighwayFactor")) c.getConfig("nonHighwayFactor")
                      else com.typesafe.config.ConfigFactory.parseString("nonHighwayFactor{}")
                    )
                )
              }
            }

            case class LowCongestion(
              highwayFactor: BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.LowTimeSensitivity.LowCongestion.HighwayFactor,
              nonHighwayFactor: BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.LowTimeSensitivity.LowCongestion.NonHighwayFactor
            )

            object LowCongestion {
              case class HighwayFactor(
                Level3: scala.Double,
                Level4: scala.Double,
                Level5: scala.Double,
                LevelLE2: scala.Double
              )

              object HighwayFactor {

                def apply(
                  c: com.typesafe.config.Config
                ): BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.LowTimeSensitivity.LowCongestion.HighwayFactor = {
                  BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.LowTimeSensitivity.LowCongestion.HighwayFactor(
                    Level3 = if (c.hasPathOrNull("Level3")) c.getDouble("Level3") else 1.0,
                    Level4 = if (c.hasPathOrNull("Level4")) c.getDouble("Level4") else 1.0,
                    Level5 = if (c.hasPathOrNull("Level5")) c.getDouble("Level5") else 1.0,
                    LevelLE2 = if (c.hasPathOrNull("LevelLE2")) c.getDouble("LevelLE2") else 1.0
                  )
                }
              }

              case class NonHighwayFactor(
                Level3: scala.Double,
                Level4: scala.Double,
                Level5: scala.Double,
                LevelLE2: scala.Double
              )

              object NonHighwayFactor {

                def apply(
                  c: com.typesafe.config.Config
                ): BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.LowTimeSensitivity.LowCongestion.NonHighwayFactor = {
                  BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.LowTimeSensitivity.LowCongestion.NonHighwayFactor(
                    Level3 = if (c.hasPathOrNull("Level3")) c.getDouble("Level3") else 1.0,
                    Level4 = if (c.hasPathOrNull("Level4")) c.getDouble("Level4") else 1.0,
                    Level5 = if (c.hasPathOrNull("Level5")) c.getDouble("Level5") else 1.0,
                    LevelLE2 = if (c.hasPathOrNull("LevelLE2")) c.getDouble("LevelLE2") else 1.0
                  )
                }
              }

              def apply(
                c: com.typesafe.config.Config
              ): BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.LowTimeSensitivity.LowCongestion = {
                BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.LowTimeSensitivity.LowCongestion(
                  highwayFactor =
                    BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.LowTimeSensitivity.LowCongestion.HighwayFactor(
                      if (c.hasPathOrNull("highwayFactor")) c.getConfig("highwayFactor")
                      else com.typesafe.config.ConfigFactory.parseString("highwayFactor{}")
                    ),
                  nonHighwayFactor =
                    BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.LowTimeSensitivity.LowCongestion.NonHighwayFactor(
                      if (c.hasPathOrNull("nonHighwayFactor")) c.getConfig("nonHighwayFactor")
                      else com.typesafe.config.ConfigFactory.parseString("nonHighwayFactor{}")
                    )
                )
              }
            }

            def apply(
              c: com.typesafe.config.Config
            ): BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.LowTimeSensitivity = {
              BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.LowTimeSensitivity(
                highCongestion = BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.LowTimeSensitivity.HighCongestion(
                  if (c.hasPathOrNull("highCongestion")) c.getConfig("highCongestion")
                  else com.typesafe.config.ConfigFactory.parseString("highCongestion{}")
                ),
                lowCongestion = BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.LowTimeSensitivity.LowCongestion(
                  if (c.hasPathOrNull("lowCongestion")) c.getConfig("lowCongestion")
                  else com.typesafe.config.ConfigFactory.parseString("lowCongestion{}")
                )
              )
            }
          }

          case class ModeVotMultiplier(
            CAV: scala.Double,
            bike: scala.Double,
            drive: scala.Double,
            rideHail: scala.Double,
            rideHailPooled: scala.Double,
            rideHailTransit: scala.Double,
            transit: scala.Double,
            waiting: scala.Double,
            walk: scala.Double
          )

          object ModeVotMultiplier {

            def apply(
              c: com.typesafe.config.Config
            ): BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.ModeVotMultiplier = {
              BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.ModeVotMultiplier(
                CAV = if (c.hasPathOrNull("CAV")) c.getDouble("CAV") else 1.0,
                bike = if (c.hasPathOrNull("bike")) c.getDouble("bike") else 1.0,
                drive = if (c.hasPathOrNull("drive")) c.getDouble("drive") else 1.0,
                rideHail = if (c.hasPathOrNull("rideHail")) c.getDouble("rideHail") else 1.0,
                rideHailPooled = if (c.hasPathOrNull("rideHailPooled")) c.getDouble("rideHailPooled") else 1.0,
                rideHailTransit = if (c.hasPathOrNull("rideHailTransit")) c.getDouble("rideHailTransit") else 1.0,
                transit = if (c.hasPathOrNull("transit")) c.getDouble("transit") else 1.0,
                waiting = if (c.hasPathOrNull("waiting")) c.getDouble("waiting") else 1.0,
                walk = if (c.hasPathOrNull("walk")) c.getDouble("walk") else 1.0
              )
            }
          }

          case class MulitnomialLogit(
            params: BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.MulitnomialLogit.Params
          )

          object MulitnomialLogit {
            case class Params(
              bike_intercept: scala.Double,
              car_intercept: scala.Double,
              cav_intercept: scala.Double,
              drive_transit_intercept: scala.Double,
              ride_hail_intercept: scala.Double,
              ride_hail_pooled_intercept: scala.Double,
              ride_hail_transit_intercept: scala.Double,
              transfer: scala.Double,
              walk_intercept: scala.Double,
              walk_transit_intercept: scala.Double
            )

            object Params {

              def apply(
                c: com.typesafe.config.Config
              ): BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.MulitnomialLogit.Params = {
                BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.MulitnomialLogit.Params(
                  bike_intercept = if (c.hasPathOrNull("bike_intercept")) c.getDouble("bike_intercept") else 0.0,
                  car_intercept = if (c.hasPathOrNull("car_intercept")) c.getDouble("car_intercept") else 0.0,
                  cav_intercept = if (c.hasPathOrNull("cav_intercept")) c.getDouble("cav_intercept") else 0.0,
                  drive_transit_intercept =
                    if (c.hasPathOrNull("drive_transit_intercept")) c.getDouble("drive_transit_intercept") else 0.0,
                  ride_hail_intercept =
                    if (c.hasPathOrNull("ride_hail_intercept")) c.getDouble("ride_hail_intercept") else 0.0,
                  ride_hail_pooled_intercept =
                    if (c.hasPathOrNull("ride_hail_pooled_intercept")) c.getDouble("ride_hail_pooled_intercept")
                    else 0.0,
                  ride_hail_transit_intercept =
                    if (c.hasPathOrNull("ride_hail_transit_intercept")) c.getDouble("ride_hail_transit_intercept")
                    else 0.0,
                  transfer = if (c.hasPathOrNull("transfer")) c.getDouble("transfer") else -1.4,
                  walk_intercept = if (c.hasPathOrNull("walk_intercept")) c.getDouble("walk_intercept") else 0.0,
                  walk_transit_intercept =
                    if (c.hasPathOrNull("walk_transit_intercept")) c.getDouble("walk_transit_intercept") else 0.0
                )
              }
            }

            def apply(
              c: com.typesafe.config.Config
            ): BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.MulitnomialLogit = {
              BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.MulitnomialLogit(
                params = BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.MulitnomialLogit.Params(
                  if (c.hasPathOrNull("params")) c.getConfig("params")
                  else com.typesafe.config.ConfigFactory.parseString("params{}")
                )
              )
            }
          }

          case class PoolingMultiplier(
            Level3: scala.Double,
            Level4: scala.Double,
            Level5: scala.Double,
            LevelLE2: scala.Double
          )

          object PoolingMultiplier {

            def apply(
              c: com.typesafe.config.Config
            ): BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.PoolingMultiplier = {
              BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.PoolingMultiplier(
                Level3 = if (c.hasPathOrNull("Level3")) c.getDouble("Level3") else 1.0,
                Level4 = if (c.hasPathOrNull("Level4")) c.getDouble("Level4") else 1.0,
                Level5 = if (c.hasPathOrNull("Level5")) c.getDouble("Level5") else 1.0,
                LevelLE2 = if (c.hasPathOrNull("LevelLE2")) c.getDouble("LevelLE2") else 1.0
              )
            }
          }

          def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Agentsim.Agents.ModalBehaviors = {
            BeamConfig.Beam.Agentsim.Agents.ModalBehaviors(
              defaultValueOfTime =
                if (c.hasPathOrNull("defaultValueOfTime")) c.getDouble("defaultValueOfTime") else 8.0,
              highTimeSensitivity = BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.HighTimeSensitivity(
                if (c.hasPathOrNull("highTimeSensitivity")) c.getConfig("highTimeSensitivity")
                else com.typesafe.config.ConfigFactory.parseString("highTimeSensitivity{}")
              ),
              lccm = BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.Lccm(
                if (c.hasPathOrNull("lccm")) c.getConfig("lccm")
                else com.typesafe.config.ConfigFactory.parseString("lccm{}")
              ),
              lowTimeSensitivity = BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.LowTimeSensitivity(
                if (c.hasPathOrNull("lowTimeSensitivity")) c.getConfig("lowTimeSensitivity")
                else com.typesafe.config.ConfigFactory.parseString("lowTimeSensitivity{}")
              ),
              maximumNumberOfReplanningAttempts =
                if (c.hasPathOrNull("maximumNumberOfReplanningAttempts")) c.getInt("maximumNumberOfReplanningAttempts")
                else 3,
              modeChoiceClass =
                if (c.hasPathOrNull("modeChoiceClass")) c.getString("modeChoiceClass")
                else "ModeChoiceMultinomialLogit",
              modeVotMultiplier = BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.ModeVotMultiplier(
                if (c.hasPathOrNull("modeVotMultiplier")) c.getConfig("modeVotMultiplier")
                else com.typesafe.config.ConfigFactory.parseString("modeVotMultiplier{}")
              ),
              mulitnomialLogit = BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.MulitnomialLogit(
                if (c.hasPathOrNull("mulitnomialLogit")) c.getConfig("mulitnomialLogit")
                else com.typesafe.config.ConfigFactory.parseString("mulitnomialLogit{}")
              ),
              overrideAutomationForVOTT = c.hasPathOrNull("overrideAutomationForVOTT") && c.getBoolean(
                "overrideAutomationForVOTT"
              ),
              overrideAutomationLevel =
                if (c.hasPathOrNull("overrideAutomationLevel")) c.getInt("overrideAutomationLevel") else 1,
              poolingMultiplier = BeamConfig.Beam.Agentsim.Agents.ModalBehaviors.PoolingMultiplier(
                if (c.hasPathOrNull("poolingMultiplier")) c.getConfig("poolingMultiplier")
                else com.typesafe.config.ConfigFactory.parseString("poolingMultiplier{}")
              )
            )
          }
        }

        case class ModeIncentive(
          filePath: java.lang.String
        )

        object ModeIncentive {

          def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Agentsim.Agents.ModeIncentive = {
            BeamConfig.Beam.Agentsim.Agents.ModeIncentive(
              filePath = if (c.hasPathOrNull("filePath")) c.getString("filePath") else ""
            )
          }
        }

        case class Parking(
          bev: BeamConfig.Beam.Agentsim.Agents.Parking.Bev,
          maxSearchRadius: scala.Double,
          mulitnomialLogit: BeamConfig.Beam.Agentsim.Agents.Parking.MulitnomialLogit
        )

        object Parking {
          case class Bev(
            distance_safety_margin: scala.Double
          )

          object Bev {

            def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Agentsim.Agents.Parking.Bev = {
              BeamConfig.Beam.Agentsim.Agents.Parking.Bev(
                distance_safety_margin =
                  if (c.hasPathOrNull("distance_safety_margin")) c.getDouble("distance_safety_margin") else 35000
              )
            }
          }

          case class MulitnomialLogit(
            params: BeamConfig.Beam.Agentsim.Agents.Parking.MulitnomialLogit.Params
          )

          object MulitnomialLogit {
            case class Params(
              distance_multiplier: scala.Double,
              installed_capacity_multiplier: scala.Double,
              parking_costs_price_multiplier: scala.Double
            )

            object Params {

              def apply(
                c: com.typesafe.config.Config
              ): BeamConfig.Beam.Agentsim.Agents.Parking.MulitnomialLogit.Params = {
                BeamConfig.Beam.Agentsim.Agents.Parking.MulitnomialLogit.Params(
                  distance_multiplier =
                    if (c.hasPathOrNull("distance_multiplier")) c.getDouble("distance_multiplier") else 1.0,
                  installed_capacity_multiplier =
                    if (c.hasPathOrNull("installed_capacity_multiplier")) c.getDouble("installed_capacity_multiplier")
                    else 0.001,
                  parking_costs_price_multiplier =
                    if (c.hasPathOrNull("parking_costs_price_multiplier")) c.getDouble("parking_costs_price_multiplier")
                    else 4.5
                )
              }
            }

            def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Agentsim.Agents.Parking.MulitnomialLogit = {
              BeamConfig.Beam.Agentsim.Agents.Parking.MulitnomialLogit(
                params = BeamConfig.Beam.Agentsim.Agents.Parking.MulitnomialLogit.Params(
                  if (c.hasPathOrNull("params")) c.getConfig("params")
                  else com.typesafe.config.ConfigFactory.parseString("params{}")
                )
              )
            }
          }

          def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Agentsim.Agents.Parking = {
            BeamConfig.Beam.Agentsim.Agents.Parking(
              bev = BeamConfig.Beam.Agentsim.Agents.Parking.Bev(
                if (c.hasPathOrNull("bev")) c.getConfig("bev")
                else com.typesafe.config.ConfigFactory.parseString("bev{}")
              ),
              maxSearchRadius = if (c.hasPathOrNull("maxSearchRadius")) c.getDouble("maxSearchRadius") else 8046.72,
              mulitnomialLogit = BeamConfig.Beam.Agentsim.Agents.Parking.MulitnomialLogit(
                if (c.hasPathOrNull("mulitnomialLogit")) c.getConfig("mulitnomialLogit")
                else com.typesafe.config.ConfigFactory.parseString("mulitnomialLogit{}")
              )
            )
          }
        }

        case class Plans(
          inputPersonAttributesFilePath: java.lang.String,
          inputPlansFilePath: java.lang.String
        )

        object Plans {

          def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Agentsim.Agents.Plans = {
            BeamConfig.Beam.Agentsim.Agents.Plans(
              inputPersonAttributesFilePath =
                if (c.hasPathOrNull("inputPersonAttributesFilePath")) c.getString("inputPersonAttributesFilePath")
                else "/test/input/beamville/populationAttributes.xml.gz",
              inputPlansFilePath =
                if (c.hasPathOrNull("inputPlansFilePath")) c.getString("inputPlansFilePath")
                else "/test/input/beamville/population.xml.gz"
            )
          }
        }

        case class Population(
          useVehicleSampling: scala.Boolean
        )

        object Population {

          def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Agentsim.Agents.Population = {
            BeamConfig.Beam.Agentsim.Agents.Population(
              useVehicleSampling = c.hasPathOrNull("useVehicleSampling") && c.getBoolean("useVehicleSampling")
            )
          }
        }

        case class PtFare(
          filePath: java.lang.String
        )

        object PtFare {

          def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Agentsim.Agents.PtFare = {
            BeamConfig.Beam.Agentsim.Agents.PtFare(
              filePath = if (c.hasPathOrNull("filePath")) c.getString("filePath") else ""
            )
          }
        }

        case class RideHail(
          allocationManager: BeamConfig.Beam.Agentsim.Agents.RideHail.AllocationManager,
          cav: BeamConfig.Beam.Agentsim.Agents.RideHail.Cav,
          defaultBaseCost: scala.Double,
          defaultCostPerMile: scala.Double,
          defaultCostPerMinute: scala.Double,
          human: BeamConfig.Beam.Agentsim.Agents.RideHail.Human,
          initialization: BeamConfig.Beam.Agentsim.Agents.RideHail.Initialization,
          iterationStats: BeamConfig.Beam.Agentsim.Agents.RideHail.IterationStats,
          pooledBaseCost: scala.Double,
          pooledCostPerMile: scala.Double,
          pooledCostPerMinute: scala.Double,
          pooledToRegularRideCostRatio: scala.Double,
          rangeBufferForDispatchInMeters: scala.Int,
          refuelLocationType: java.lang.String,
          refuelThresholdInMeters: scala.Double,
          repositioningManager: BeamConfig.Beam.Agentsim.Agents.RideHail.RepositioningManager,
          rideHailManager: BeamConfig.Beam.Agentsim.Agents.RideHail.RideHailManager,
          surgePricing: BeamConfig.Beam.Agentsim.Agents.RideHail.SurgePricing
        )

        object RideHail {
          case class AllocationManager(
            alonsoMora: BeamConfig.Beam.Agentsim.Agents.RideHail.AllocationManager.AlonsoMora,
            name: java.lang.String,
            repositionLowWaitingTimes: BeamConfig.Beam.Agentsim.Agents.RideHail.AllocationManager.RepositionLowWaitingTimes,
            requestBufferTimeoutInSeconds: scala.Int
          )

          object AllocationManager {
            case class AlonsoMora(
              solutionSpaceSizePerVehicle: scala.Int,
              travelTimeDelayAsFraction: scala.Double,
              waitingTimeInSec: scala.Int
            )

            object AlonsoMora {

              def apply(
                c: com.typesafe.config.Config
              ): BeamConfig.Beam.Agentsim.Agents.RideHail.AllocationManager.AlonsoMora = {
                BeamConfig.Beam.Agentsim.Agents.RideHail.AllocationManager.AlonsoMora(
                  solutionSpaceSizePerVehicle =
                    if (c.hasPathOrNull("solutionSpaceSizePerVehicle")) c.getInt("solutionSpaceSizePerVehicle") else 5,
                  travelTimeDelayAsFraction =
                    if (c.hasPathOrNull("travelTimeDelayAsFraction")) c.getDouble("travelTimeDelayAsFraction") else 0.2,
                  waitingTimeInSec = if (c.hasPathOrNull("waitingTimeInSec")) c.getInt("waitingTimeInSec") else 360
                )
              }
            }

            case class RepositionLowWaitingTimes(
              allowIncreasingRadiusIfDemandInRadiusLow: scala.Boolean,
              demandWeight: scala.Double,
              distanceWeight: scala.Double,
              keepMaxTopNScores: scala.Int,
              minDemandPercentageInRadius: scala.Double,
              minScoreThresholdForRepositioning: scala.Double,
              minimumNumberOfIdlingVehiclesThresholdForRepositioning: scala.Int,
              percentageOfVehiclesToReposition: scala.Double,
              produceDebugImages: scala.Boolean,
              repositionCircleRadiusInMeters: scala.Double,
              repositioningMethod: java.lang.String,
              timeWindowSizeInSecForDecidingAboutRepositioning: scala.Double,
              waitingTimeWeight: scala.Double
            )

            object RepositionLowWaitingTimes {

              def apply(
                c: com.typesafe.config.Config
              ): BeamConfig.Beam.Agentsim.Agents.RideHail.AllocationManager.RepositionLowWaitingTimes = {
                BeamConfig.Beam.Agentsim.Agents.RideHail.AllocationManager.RepositionLowWaitingTimes(
                  allowIncreasingRadiusIfDemandInRadiusLow = !c.hasPathOrNull(
                    "allowIncreasingRadiusIfDemandInRadiusLow"
                  ) || c.getBoolean("allowIncreasingRadiusIfDemandInRadiusLow"),
                  demandWeight = if (c.hasPathOrNull("demandWeight")) c.getDouble("demandWeight") else 4.0,
                  distanceWeight = if (c.hasPathOrNull("distanceWeight")) c.getDouble("distanceWeight") else 0.01,
                  keepMaxTopNScores = if (c.hasPathOrNull("keepMaxTopNScores")) c.getInt("keepMaxTopNScores") else 1,
                  minDemandPercentageInRadius =
                    if (c.hasPathOrNull("minDemandPercentageInRadius")) c.getDouble("minDemandPercentageInRadius")
                    else 0.1,
                  minScoreThresholdForRepositioning =
                    if (c.hasPathOrNull("minScoreThresholdForRepositioning"))
                      c.getDouble("minScoreThresholdForRepositioning")
                    else 0.1,
                  minimumNumberOfIdlingVehiclesThresholdForRepositioning =
                    if (c.hasPathOrNull("minimumNumberOfIdlingVehiclesThresholdForRepositioning"))
                      c.getInt("minimumNumberOfIdlingVehiclesThresholdForRepositioning")
                    else 1,
                  percentageOfVehiclesToReposition =
                    if (c.hasPathOrNull("percentageOfVehiclesToReposition"))
                      c.getDouble("percentageOfVehiclesToReposition")
                    else 0.01,
                  produceDebugImages = !c.hasPathOrNull("produceDebugImages") || c.getBoolean("produceDebugImages"),
                  repositionCircleRadiusInMeters =
                    if (c.hasPathOrNull("repositionCircleRadiusInMeters")) c.getDouble("repositionCircleRadiusInMeters")
                    else 3000,
                  repositioningMethod =
                    if (c.hasPathOrNull("repositioningMethod")) c.getString("repositioningMethod") else "TOP_SCORES",
                  timeWindowSizeInSecForDecidingAboutRepositioning =
                    if (c.hasPathOrNull("timeWindowSizeInSecForDecidingAboutRepositioning"))
                      c.getDouble("timeWindowSizeInSecForDecidingAboutRepositioning")
                    else 1200,
                  waitingTimeWeight =
                    if (c.hasPathOrNull("waitingTimeWeight")) c.getDouble("waitingTimeWeight") else 4.0
                )
              }
            }

            def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Agentsim.Agents.RideHail.AllocationManager = {
              BeamConfig.Beam.Agentsim.Agents.RideHail.AllocationManager(
                alonsoMora = BeamConfig.Beam.Agentsim.Agents.RideHail.AllocationManager.AlonsoMora(
                  if (c.hasPathOrNull("alonsoMora")) c.getConfig("alonsoMora")
                  else com.typesafe.config.ConfigFactory.parseString("alonsoMora{}")
                ),
                name = if (c.hasPathOrNull("name")) c.getString("name") else "DEFAULT_MANAGER",
                repositionLowWaitingTimes =
                  BeamConfig.Beam.Agentsim.Agents.RideHail.AllocationManager.RepositionLowWaitingTimes(
                    if (c.hasPathOrNull("repositionLowWaitingTimes")) c.getConfig("repositionLowWaitingTimes")
                    else com.typesafe.config.ConfigFactory.parseString("repositionLowWaitingTimes{}")
                  ),
                requestBufferTimeoutInSeconds =
                  if (c.hasPathOrNull("requestBufferTimeoutInSeconds")) c.getInt("requestBufferTimeoutInSeconds") else 0
              )
            }
          }

          case class Cav(
            noRefuelThresholdInMeters: scala.Int,
            refuelRequiredThresholdInMeters: scala.Int,
            valueOfTime: scala.Int
          )

          object Cav {

            def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Agentsim.Agents.RideHail.Cav = {
              BeamConfig.Beam.Agentsim.Agents.RideHail.Cav(
                noRefuelThresholdInMeters =
                  if (c.hasPathOrNull("noRefuelThresholdInMeters")) c.getInt("noRefuelThresholdInMeters") else 96540,
                refuelRequiredThresholdInMeters =
                  if (c.hasPathOrNull("refuelRequiredThresholdInMeters")) c.getInt("refuelRequiredThresholdInMeters")
                  else 16090,
                valueOfTime = if (c.hasPathOrNull("valueOfTime")) c.getInt("valueOfTime") else 1
              )
            }
          }

          case class Human(
            noRefuelThresholdInMeters: scala.Int,
            refuelRequiredThresholdInMeters: scala.Int,
            valueOfTime: scala.Double
          )

          object Human {

            def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Agentsim.Agents.RideHail.Human = {
              BeamConfig.Beam.Agentsim.Agents.RideHail.Human(
                noRefuelThresholdInMeters =
                  if (c.hasPathOrNull("noRefuelThresholdInMeters")) c.getInt("noRefuelThresholdInMeters") else 128720,
                refuelRequiredThresholdInMeters =
                  if (c.hasPathOrNull("refuelRequiredThresholdInMeters")) c.getInt("refuelRequiredThresholdInMeters")
                  else 32180,
                valueOfTime = if (c.hasPathOrNull("valueOfTime")) c.getDouble("valueOfTime") else 22.9
              )
            }
          }

          case class Initialization(
            filePath: java.lang.String,
            initType: java.lang.String,
            parking: BeamConfig.Beam.Agentsim.Agents.RideHail.Initialization.Parking,
            procedural: BeamConfig.Beam.Agentsim.Agents.RideHail.Initialization.Procedural
          )

          object Initialization {
            case class Parking(
              filePath: java.lang.String
            )

            object Parking {

              def apply(
                c: com.typesafe.config.Config
              ): BeamConfig.Beam.Agentsim.Agents.RideHail.Initialization.Parking = {
                BeamConfig.Beam.Agentsim.Agents.RideHail.Initialization.Parking(
                  filePath =
                    if (c.hasPathOrNull("filePath")) c.getString("filePath")
                    else "/test/input/beamville/ride-hail-parking.csv"
                )
              }
            }

            case class Procedural(
              fractionOfInitialVehicleFleet: scala.Double,
              initialLocation: BeamConfig.Beam.Agentsim.Agents.RideHail.Initialization.Procedural.InitialLocation,
              vehicleTypeId: java.lang.String,
              vehicleTypePrefix: java.lang.String
            )

            object Procedural {
              case class InitialLocation(
                home: BeamConfig.Beam.Agentsim.Agents.RideHail.Initialization.Procedural.InitialLocation.Home,
                name: java.lang.String
              )

              object InitialLocation {
                case class Home(
                  radiusInMeters: scala.Double
                )

                object Home {

                  def apply(
                    c: com.typesafe.config.Config
                  ): BeamConfig.Beam.Agentsim.Agents.RideHail.Initialization.Procedural.InitialLocation.Home = {
                    BeamConfig.Beam.Agentsim.Agents.RideHail.Initialization.Procedural.InitialLocation.Home(
                      radiusInMeters = if (c.hasPathOrNull("radiusInMeters")) c.getDouble("radiusInMeters") else 10000
                    )
                  }
                }

                def apply(
                  c: com.typesafe.config.Config
                ): BeamConfig.Beam.Agentsim.Agents.RideHail.Initialization.Procedural.InitialLocation = {
                  BeamConfig.Beam.Agentsim.Agents.RideHail.Initialization.Procedural.InitialLocation(
                    home = BeamConfig.Beam.Agentsim.Agents.RideHail.Initialization.Procedural.InitialLocation.Home(
                      if (c.hasPathOrNull("home")) c.getConfig("home")
                      else com.typesafe.config.ConfigFactory.parseString("home{}")
                    ),
                    name = if (c.hasPathOrNull("name")) c.getString("name") else "HOME"
                  )
                }
              }

              def apply(
                c: com.typesafe.config.Config
              ): BeamConfig.Beam.Agentsim.Agents.RideHail.Initialization.Procedural = {
                BeamConfig.Beam.Agentsim.Agents.RideHail.Initialization.Procedural(
                  fractionOfInitialVehicleFleet =
                    if (c.hasPathOrNull("fractionOfInitialVehicleFleet")) c.getDouble("fractionOfInitialVehicleFleet")
                    else 0.1,
                  initialLocation = BeamConfig.Beam.Agentsim.Agents.RideHail.Initialization.Procedural.InitialLocation(
                    if (c.hasPathOrNull("initialLocation")) c.getConfig("initialLocation")
                    else com.typesafe.config.ConfigFactory.parseString("initialLocation{}")
                  ),
                  vehicleTypeId = if (c.hasPathOrNull("vehicleTypeId")) c.getString("vehicleTypeId") else "Car",
                  vehicleTypePrefix =
                    if (c.hasPathOrNull("vehicleTypePrefix")) c.getString("vehicleTypePrefix") else "RH"
                )
              }
            }

            def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Agentsim.Agents.RideHail.Initialization = {
              BeamConfig.Beam.Agentsim.Agents.RideHail.Initialization(
                filePath =
                  if (c.hasPathOrNull("filePath")) c.getString("filePath")
                  else "/test/input/beamville/ride-hail-fleet.csv",
                initType = if (c.hasPathOrNull("initType")) c.getString("initType") else "PROCEDURAL",
                parking = BeamConfig.Beam.Agentsim.Agents.RideHail.Initialization.Parking(
                  if (c.hasPathOrNull("parking")) c.getConfig("parking")
                  else com.typesafe.config.ConfigFactory.parseString("parking{}")
                ),
                procedural = BeamConfig.Beam.Agentsim.Agents.RideHail.Initialization.Procedural(
                  if (c.hasPathOrNull("procedural")) c.getConfig("procedural")
                  else com.typesafe.config.ConfigFactory.parseString("procedural{}")
                )
              )
            }
          }

          case class IterationStats(
            timeBinSizeInSec: scala.Double
          )

          object IterationStats {

            def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Agentsim.Agents.RideHail.IterationStats = {
              BeamConfig.Beam.Agentsim.Agents.RideHail.IterationStats(
                timeBinSizeInSec = if (c.hasPathOrNull("timeBinSizeInSec")) c.getDouble("timeBinSizeInSec") else 3600.0
              )
            }
          }

          case class RepositioningManager(
            demandFollowingRepositioningManager: BeamConfig.Beam.Agentsim.Agents.RideHail.RepositioningManager.DemandFollowingRepositioningManager,
            name: java.lang.String,
            timeout: scala.Int
          )

          object RepositioningManager {
            case class DemandFollowingRepositioningManager(
              fractionOfClosestClustersToConsider: scala.Double,
              numberOfClustersForDemand: scala.Int,
              sensitivityOfRepositioningToDemand: scala.Double
            )

            object DemandFollowingRepositioningManager {

              def apply(
                c: com.typesafe.config.Config
              ): BeamConfig.Beam.Agentsim.Agents.RideHail.RepositioningManager.DemandFollowingRepositioningManager = {
                BeamConfig.Beam.Agentsim.Agents.RideHail.RepositioningManager.DemandFollowingRepositioningManager(
                  fractionOfClosestClustersToConsider =
                    if (c.hasPathOrNull("fractionOfClosestClustersToConsider"))
                      c.getDouble("fractionOfClosestClustersToConsider")
                    else 0.2,
                  numberOfClustersForDemand =
                    if (c.hasPathOrNull("numberOfClustersForDemand")) c.getInt("numberOfClustersForDemand") else 30,
                  sensitivityOfRepositioningToDemand =
                    if (c.hasPathOrNull("sensitivityOfRepositioningToDemand"))
                      c.getDouble("sensitivityOfRepositioningToDemand")
                    else 1
                )
              }
            }

            def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Agentsim.Agents.RideHail.RepositioningManager = {
              BeamConfig.Beam.Agentsim.Agents.RideHail.RepositioningManager(
                demandFollowingRepositioningManager =
                  BeamConfig.Beam.Agentsim.Agents.RideHail.RepositioningManager.DemandFollowingRepositioningManager(
                    if (c.hasPathOrNull("demandFollowingRepositioningManager"))
                      c.getConfig("demandFollowingRepositioningManager")
                    else com.typesafe.config.ConfigFactory.parseString("demandFollowingRepositioningManager{}")
                  ),
                name = if (c.hasPathOrNull("name")) c.getString("name") else "DEFAULT_REPOSITIONING_MANAGER",
                timeout = if (c.hasPathOrNull("timeout")) c.getInt("timeout") else 0
              )
            }
          }

          case class RideHailManager(
            radiusInMeters: scala.Double
          )

          object RideHailManager {

            def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Agentsim.Agents.RideHail.RideHailManager = {
              BeamConfig.Beam.Agentsim.Agents.RideHail.RideHailManager(
                radiusInMeters = if (c.hasPathOrNull("radiusInMeters")) c.getDouble("radiusInMeters") else 5000
              )
            }
          }

          case class SurgePricing(
            minimumSurgeLevel: scala.Double,
            numberOfCategories: scala.Int,
            priceAdjustmentStrategy: java.lang.String,
            surgeLevelAdaptionStep: scala.Double
          )

          object SurgePricing {

            def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Agentsim.Agents.RideHail.SurgePricing = {
              BeamConfig.Beam.Agentsim.Agents.RideHail.SurgePricing(
                minimumSurgeLevel = if (c.hasPathOrNull("minimumSurgeLevel")) c.getDouble("minimumSurgeLevel") else 0.1,
                numberOfCategories = if (c.hasPathOrNull("numberOfCategories")) c.getInt("numberOfCategories") else 6,
                priceAdjustmentStrategy =
                  if (c.hasPathOrNull("priceAdjustmentStrategy")) c.getString("priceAdjustmentStrategy")
                  else "KEEP_PRICE_LEVEL_FIXED_AT_ONE",
                surgeLevelAdaptionStep =
                  if (c.hasPathOrNull("surgeLevelAdaptionStep")) c.getDouble("surgeLevelAdaptionStep") else 0.1
              )
            }
          }

          def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Agentsim.Agents.RideHail = {
            BeamConfig.Beam.Agentsim.Agents.RideHail(
              allocationManager = BeamConfig.Beam.Agentsim.Agents.RideHail.AllocationManager(
                if (c.hasPathOrNull("allocationManager")) c.getConfig("allocationManager")
                else com.typesafe.config.ConfigFactory.parseString("allocationManager{}")
              ),
              cav = BeamConfig.Beam.Agentsim.Agents.RideHail.Cav(
                if (c.hasPathOrNull("cav")) c.getConfig("cav")
                else com.typesafe.config.ConfigFactory.parseString("cav{}")
              ),
              defaultBaseCost = if (c.hasPathOrNull("defaultBaseCost")) c.getDouble("defaultBaseCost") else 1.8,
              defaultCostPerMile =
                if (c.hasPathOrNull("defaultCostPerMile")) c.getDouble("defaultCostPerMile") else 0.91,
              defaultCostPerMinute =
                if (c.hasPathOrNull("defaultCostPerMinute")) c.getDouble("defaultCostPerMinute") else 0.28,
              human = BeamConfig.Beam.Agentsim.Agents.RideHail.Human(
                if (c.hasPathOrNull("human")) c.getConfig("human")
                else com.typesafe.config.ConfigFactory.parseString("human{}")
              ),
              initialization = BeamConfig.Beam.Agentsim.Agents.RideHail.Initialization(
                if (c.hasPathOrNull("initialization")) c.getConfig("initialization")
                else com.typesafe.config.ConfigFactory.parseString("initialization{}")
              ),
              iterationStats = BeamConfig.Beam.Agentsim.Agents.RideHail.IterationStats(
                if (c.hasPathOrNull("iterationStats")) c.getConfig("iterationStats")
                else com.typesafe.config.ConfigFactory.parseString("iterationStats{}")
              ),
              pooledBaseCost = if (c.hasPathOrNull("pooledBaseCost")) c.getDouble("pooledBaseCost") else 1.89,
              pooledCostPerMile = if (c.hasPathOrNull("pooledCostPerMile")) c.getDouble("pooledCostPerMile") else 1.11,
              pooledCostPerMinute =
                if (c.hasPathOrNull("pooledCostPerMinute")) c.getDouble("pooledCostPerMinute") else 0.07,
              pooledToRegularRideCostRatio =
                if (c.hasPathOrNull("pooledToRegularRideCostRatio")) c.getDouble("pooledToRegularRideCostRatio")
                else 0.6,
              rangeBufferForDispatchInMeters =
                if (c.hasPathOrNull("rangeBufferForDispatchInMeters")) c.getInt("rangeBufferForDispatchInMeters")
                else 10000,
              refuelLocationType =
                if (c.hasPathOrNull("refuelLocationType")) c.getString("refuelLocationType") else "AtTAZCenter",
              refuelThresholdInMeters =
                if (c.hasPathOrNull("refuelThresholdInMeters")) c.getDouble("refuelThresholdInMeters") else 5000.0,
              repositioningManager = BeamConfig.Beam.Agentsim.Agents.RideHail.RepositioningManager(
                if (c.hasPathOrNull("repositioningManager")) c.getConfig("repositioningManager")
                else com.typesafe.config.ConfigFactory.parseString("repositioningManager{}")
              ),
              rideHailManager = BeamConfig.Beam.Agentsim.Agents.RideHail.RideHailManager(
                if (c.hasPathOrNull("rideHailManager")) c.getConfig("rideHailManager")
                else com.typesafe.config.ConfigFactory.parseString("rideHailManager{}")
              ),
              surgePricing = BeamConfig.Beam.Agentsim.Agents.RideHail.SurgePricing(
                if (c.hasPathOrNull("surgePricing")) c.getConfig("surgePricing")
                else com.typesafe.config.ConfigFactory.parseString("surgePricing{}")
              )
            )
          }
        }

        case class RideHailTransit(
          modesToConsider: java.lang.String
        )

        object RideHailTransit {

          def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Agentsim.Agents.RideHailTransit = {
            BeamConfig.Beam.Agentsim.Agents.RideHailTransit(
              modesToConsider = if (c.hasPathOrNull("modesToConsider")) c.getString("modesToConsider") else "MASS"
            )
          }
        }

        case class Vehicles(
          downsamplingMethod: java.lang.String,
          fractionOfInitialVehicleFleet: scala.Double,
          fuelTypesFilePath: java.lang.String,
          linkToGradePercentFilePath: java.lang.String,
          meanPrivateVehicleStartingSOC: scala.Double,
          meanRidehailVehicleStartingSOC: scala.Double,
          sharedFleets: scala.List[BeamConfig.Beam.Agentsim.Agents.Vehicles.SharedFleets$Elm],
          transitVehicleTypesByRouteFile: java.lang.String,
          vehicleAdjustmentMethod: java.lang.String,
          vehicleTypesFilePath: java.lang.String,
          vehiclesFilePath: java.lang.String
        )

        object Vehicles {
          case class SharedFleets$Elm(
            fixed_non_reserving: scala.Option[
              BeamConfig.Beam.Agentsim.Agents.Vehicles.SharedFleets$Elm.FixedNonReserving
            ],
            fixed_non_reserving_fleet_by_taz: scala.Option[
              BeamConfig.Beam.Agentsim.Agents.Vehicles.SharedFleets$Elm.FixedNonReservingFleetByTaz
            ],
            inexhaustible_reserving: scala.Option[
              BeamConfig.Beam.Agentsim.Agents.Vehicles.SharedFleets$Elm.InexhaustibleReserving
            ],
            managerType: java.lang.String,
            name: java.lang.String,
            reposition: scala.Option[BeamConfig.Beam.Agentsim.Agents.Vehicles.SharedFleets$Elm.Reposition]
          )

          object SharedFleets$Elm {
            case class FixedNonReserving(
              maxWalkingDistance: scala.Int,
              vehicleTypeId: java.lang.String
            )

            object FixedNonReserving {

              def apply(
                c: com.typesafe.config.Config
              ): BeamConfig.Beam.Agentsim.Agents.Vehicles.SharedFleets$Elm.FixedNonReserving = {
                BeamConfig.Beam.Agentsim.Agents.Vehicles.SharedFleets$Elm.FixedNonReserving(
                  maxWalkingDistance =
                    if (c.hasPathOrNull("maxWalkingDistance")) c.getInt("maxWalkingDistance") else 500,
                  vehicleTypeId = if (c.hasPathOrNull("vehicleTypeId")) c.getString("vehicleTypeId") else "sharedCar"
                )
              }
            }

            case class FixedNonReservingFleetByTaz(
              fleetSize: scala.Int,
              maxWalkingDistance: scala.Int,
              vehicleTypeId: java.lang.String,
              vehiclesSharePerTAZFromCSV: scala.Option[java.lang.String]
            )

            object FixedNonReservingFleetByTaz {

              def apply(
                c: com.typesafe.config.Config
              ): BeamConfig.Beam.Agentsim.Agents.Vehicles.SharedFleets$Elm.FixedNonReservingFleetByTaz = {
                BeamConfig.Beam.Agentsim.Agents.Vehicles.SharedFleets$Elm.FixedNonReservingFleetByTaz(
                  fleetSize = if (c.hasPathOrNull("fleetSize")) c.getInt("fleetSize") else 10,
                  maxWalkingDistance =
                    if (c.hasPathOrNull("maxWalkingDistance")) c.getInt("maxWalkingDistance") else 500,
                  vehicleTypeId = if (c.hasPathOrNull("vehicleTypeId")) c.getString("vehicleTypeId") else "sharedCar",
                  vehiclesSharePerTAZFromCSV =
                    if (c.hasPathOrNull("vehiclesSharePerTAZFromCSV")) Some(c.getString("vehiclesSharePerTAZFromCSV"))
                    else None
                )
              }
            }

            case class InexhaustibleReserving(
              vehicleTypeId: java.lang.String
            )

            object InexhaustibleReserving {

              def apply(
                c: com.typesafe.config.Config
              ): BeamConfig.Beam.Agentsim.Agents.Vehicles.SharedFleets$Elm.InexhaustibleReserving = {
                BeamConfig.Beam.Agentsim.Agents.Vehicles.SharedFleets$Elm.InexhaustibleReserving(
                  vehicleTypeId = if (c.hasPathOrNull("vehicleTypeId")) c.getString("vehicleTypeId") else "sharedCar"
                )
              }
            }

            case class Reposition(
              min_availability_undersupply_algorithm: scala.Option[
                BeamConfig.Beam.Agentsim.Agents.Vehicles.SharedFleets$Elm.Reposition.MinAvailabilityUndersupplyAlgorithm
              ],
              name: java.lang.String,
              repositionTimeBin: scala.Int,
              statTimeBin: scala.Int
            )

            object Reposition {
              case class MinAvailabilityUndersupplyAlgorithm(
                matchLimit: scala.Int
              )

              object MinAvailabilityUndersupplyAlgorithm {

                def apply(
                  c: com.typesafe.config.Config
                ): BeamConfig.Beam.Agentsim.Agents.Vehicles.SharedFleets$Elm.Reposition.MinAvailabilityUndersupplyAlgorithm = {
                  BeamConfig.Beam.Agentsim.Agents.Vehicles.SharedFleets$Elm.Reposition
                    .MinAvailabilityUndersupplyAlgorithm(
                      matchLimit = if (c.hasPathOrNull("matchLimit")) c.getInt("matchLimit") else 99999
                    )
                }
              }

              def apply(
                c: com.typesafe.config.Config
              ): BeamConfig.Beam.Agentsim.Agents.Vehicles.SharedFleets$Elm.Reposition = {
                BeamConfig.Beam.Agentsim.Agents.Vehicles.SharedFleets$Elm.Reposition(
                  min_availability_undersupply_algorithm =
                    if (c.hasPathOrNull("min-availability-undersupply-algorithm"))
                      scala.Some(
                        BeamConfig.Beam.Agentsim.Agents.Vehicles.SharedFleets$Elm.Reposition
                          .MinAvailabilityUndersupplyAlgorithm(c.getConfig("min-availability-undersupply-algorithm"))
                      )
                    else None,
                  name = if (c.hasPathOrNull("name")) c.getString("name") else "my-reposition-algorithm",
                  repositionTimeBin = if (c.hasPathOrNull("repositionTimeBin")) c.getInt("repositionTimeBin") else 3600,
                  statTimeBin = if (c.hasPathOrNull("statTimeBin")) c.getInt("statTimeBin") else 300
                )
              }
            }

            def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Agentsim.Agents.Vehicles.SharedFleets$Elm = {
              BeamConfig.Beam.Agentsim.Agents.Vehicles.SharedFleets$Elm(
                fixed_non_reserving =
                  if (c.hasPathOrNull("fixed-non-reserving"))
                    scala.Some(
                      BeamConfig.Beam.Agentsim.Agents.Vehicles.SharedFleets$Elm
                        .FixedNonReserving(c.getConfig("fixed-non-reserving"))
                    )
                  else None,
                fixed_non_reserving_fleet_by_taz =
                  if (c.hasPathOrNull("fixed-non-reserving-fleet-by-taz"))
                    scala.Some(
                      BeamConfig.Beam.Agentsim.Agents.Vehicles.SharedFleets$Elm
                        .FixedNonReservingFleetByTaz(c.getConfig("fixed-non-reserving-fleet-by-taz"))
                    )
                  else None,
                inexhaustible_reserving =
                  if (c.hasPathOrNull("inexhaustible-reserving"))
                    scala.Some(
                      BeamConfig.Beam.Agentsim.Agents.Vehicles.SharedFleets$Elm
                        .InexhaustibleReserving(c.getConfig("inexhaustible-reserving"))
                    )
                  else None,
                managerType = if (c.hasPathOrNull("managerType")) c.getString("managerType") else "fixed-non-reserving",
                name = if (c.hasPathOrNull("name")) c.getString("name") else "my-fixed-non-reserving-fleet",
                reposition =
                  if (c.hasPathOrNull("reposition"))
                    scala.Some(
                      BeamConfig.Beam.Agentsim.Agents.Vehicles.SharedFleets$Elm.Reposition(c.getConfig("reposition"))
                    )
                  else None
              )
            }
          }

          def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Agentsim.Agents.Vehicles = {
            BeamConfig.Beam.Agentsim.Agents.Vehicles(
              downsamplingMethod =
                if (c.hasPathOrNull("downsamplingMethod")) c.getString("downsamplingMethod")
                else "SECONDARY_VEHICLES_FIRST",
              fractionOfInitialVehicleFleet =
                if (c.hasPathOrNull("fractionOfInitialVehicleFleet")) c.getDouble("fractionOfInitialVehicleFleet")
                else 1.0,
              fuelTypesFilePath =
                if (c.hasPathOrNull("fuelTypesFilePath")) c.getString("fuelTypesFilePath")
                else "/test/input/beamville/beamFuelTypes.csv",
              linkToGradePercentFilePath =
                if (c.hasPathOrNull("linkToGradePercentFilePath")) c.getString("linkToGradePercentFilePath") else "",
              meanPrivateVehicleStartingSOC =
                if (c.hasPathOrNull("meanPrivateVehicleStartingSOC")) c.getDouble("meanPrivateVehicleStartingSOC")
                else 1.0,
              meanRidehailVehicleStartingSOC =
                if (c.hasPathOrNull("meanRidehailVehicleStartingSOC")) c.getDouble("meanRidehailVehicleStartingSOC")
                else 1.0,
              sharedFleets = $_LBeamConfig_Beam_Agentsim_Agents_Vehicles_SharedFleets$Elm(c.getList("sharedFleets")),
              transitVehicleTypesByRouteFile =
                if (c.hasPathOrNull("transitVehicleTypesByRouteFile")) c.getString("transitVehicleTypesByRouteFile")
                else "",
              vehicleAdjustmentMethod =
                if (c.hasPathOrNull("vehicleAdjustmentMethod")) c.getString("vehicleAdjustmentMethod") else "UNIFORM",
              vehicleTypesFilePath =
                if (c.hasPathOrNull("vehicleTypesFilePath")) c.getString("vehicleTypesFilePath")
                else "/test/input/beamville/vehicleTypes.csv",
              vehiclesFilePath =
                if (c.hasPathOrNull("vehiclesFilePath")) c.getString("vehiclesFilePath")
                else "/test/input/beamville/vehicles.csv"
            )
          }
          private def $_LBeamConfig_Beam_Agentsim_Agents_Vehicles_SharedFleets$Elm(
            cl: com.typesafe.config.ConfigList
          ): scala.List[BeamConfig.Beam.Agentsim.Agents.Vehicles.SharedFleets$Elm] = {
            import scala.collection.JavaConverters._
            cl.asScala
              .map(
                cv =>
                  BeamConfig.Beam.Agentsim.Agents.Vehicles
                    .SharedFleets$Elm(cv.asInstanceOf[com.typesafe.config.ConfigObject].toConfig)
              )
              .toList
          }
        }

        def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Agentsim.Agents = {
          BeamConfig.Beam.Agentsim.Agents(
            bodyType = if (c.hasPathOrNull("bodyType")) c.getString("bodyType") else "BODY-TYPE-DEFAULT",
            households = BeamConfig.Beam.Agentsim.Agents.Households(
              if (c.hasPathOrNull("households")) c.getConfig("households")
              else com.typesafe.config.ConfigFactory.parseString("households{}")
            ),
            modalBehaviors = BeamConfig.Beam.Agentsim.Agents.ModalBehaviors(
              if (c.hasPathOrNull("modalBehaviors")) c.getConfig("modalBehaviors")
              else com.typesafe.config.ConfigFactory.parseString("modalBehaviors{}")
            ),
            modeIncentive = BeamConfig.Beam.Agentsim.Agents.ModeIncentive(
              if (c.hasPathOrNull("modeIncentive")) c.getConfig("modeIncentive")
              else com.typesafe.config.ConfigFactory.parseString("modeIncentive{}")
            ),
            parking = BeamConfig.Beam.Agentsim.Agents.Parking(
              if (c.hasPathOrNull("parking")) c.getConfig("parking")
              else com.typesafe.config.ConfigFactory.parseString("parking{}")
            ),
            plans = BeamConfig.Beam.Agentsim.Agents.Plans(
              if (c.hasPathOrNull("plans")) c.getConfig("plans")
              else com.typesafe.config.ConfigFactory.parseString("plans{}")
            ),
            population = BeamConfig.Beam.Agentsim.Agents.Population(
              if (c.hasPathOrNull("population")) c.getConfig("population")
              else com.typesafe.config.ConfigFactory.parseString("population{}")
            ),
            ptFare = BeamConfig.Beam.Agentsim.Agents.PtFare(
              if (c.hasPathOrNull("ptFare")) c.getConfig("ptFare")
              else com.typesafe.config.ConfigFactory.parseString("ptFare{}")
            ),
            rideHail = BeamConfig.Beam.Agentsim.Agents.RideHail(
              if (c.hasPathOrNull("rideHail")) c.getConfig("rideHail")
              else com.typesafe.config.ConfigFactory.parseString("rideHail{}")
            ),
            rideHailTransit = BeamConfig.Beam.Agentsim.Agents.RideHailTransit(
              if (c.hasPathOrNull("rideHailTransit")) c.getConfig("rideHailTransit")
              else com.typesafe.config.ConfigFactory.parseString("rideHailTransit{}")
            ),
            vehicles = BeamConfig.Beam.Agentsim.Agents.Vehicles(
              if (c.hasPathOrNull("vehicles")) c.getConfig("vehicles")
              else com.typesafe.config.ConfigFactory.parseString("vehicles{}")
            )
          )
        }
      }

      case class Scenarios(
        frequencyAdjustmentFile: java.lang.String
      )

      object Scenarios {

        def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Agentsim.Scenarios = {
          BeamConfig.Beam.Agentsim.Scenarios(
            frequencyAdjustmentFile =
              if (c.hasPathOrNull("frequencyAdjustmentFile")) c.getString("frequencyAdjustmentFile")
              else "/test/input/beamville/r5/FrequencyAdjustment.csv"
          )
        }
      }

      case class ScheduleMonitorTask(
        initialDelay: scala.Int,
        interval: scala.Int
      )

      object ScheduleMonitorTask {

        def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Agentsim.ScheduleMonitorTask = {
          BeamConfig.Beam.Agentsim.ScheduleMonitorTask(
            initialDelay = if (c.hasPathOrNull("initialDelay")) c.getInt("initialDelay") else 1,
            interval = if (c.hasPathOrNull("interval")) c.getInt("interval") else 30
          )
        }
      }

      case class Taz(
        filePath: java.lang.String,
        parkingCostScalingFactor: scala.Double,
        parkingFilePath: java.lang.String,
        parkingStallCountScalingFactor: scala.Double
      )

      object Taz {

        def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Agentsim.Taz = {
          BeamConfig.Beam.Agentsim.Taz(
            filePath =
              if (c.hasPathOrNull("filePath")) c.getString("filePath") else "/test/input/beamville/taz-centers.csv",
            parkingCostScalingFactor =
              if (c.hasPathOrNull("parkingCostScalingFactor")) c.getDouble("parkingCostScalingFactor") else 1.0,
            parkingFilePath =
              if (c.hasPathOrNull("parkingFilePath")) c.getString("parkingFilePath")
              else "/test/input/beamville/taz-parking.csv",
            parkingStallCountScalingFactor =
              if (c.hasPathOrNull("parkingStallCountScalingFactor")) c.getDouble("parkingStallCountScalingFactor")
              else 1.0
          )
        }
      }

      case class Toll(
        filePath: java.lang.String
      )

      object Toll {

        def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Agentsim.Toll = {
          BeamConfig.Beam.Agentsim.Toll(
            filePath =
              if (c.hasPathOrNull("filePath")) c.getString("filePath") else "/test/input/beamville/toll-prices.csv"
          )
        }
      }

      case class Tuning(
        fuelCapacityInJoules: scala.Double,
        rideHailPrice: scala.Double,
        tollPrice: scala.Double,
        transitCapacity: scala.Option[scala.Double],
        transitPrice: scala.Double
      )

      object Tuning {

        def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Agentsim.Tuning = {
          BeamConfig.Beam.Agentsim.Tuning(
            fuelCapacityInJoules =
              if (c.hasPathOrNull("fuelCapacityInJoules")) c.getDouble("fuelCapacityInJoules") else 86400000,
            rideHailPrice = if (c.hasPathOrNull("rideHailPrice")) c.getDouble("rideHailPrice") else 1.0,
            tollPrice = if (c.hasPathOrNull("tollPrice")) c.getDouble("tollPrice") else 1.0,
            transitCapacity = if (c.hasPathOrNull("transitCapacity")) Some(c.getDouble("transitCapacity")) else None,
            transitPrice = if (c.hasPathOrNull("transitPrice")) c.getDouble("transitPrice") else 1.0
          )
        }
      }

      def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Agentsim = {
        BeamConfig.Beam.Agentsim(
          agentSampleSizeAsFractionOfPopulation =
            if (c.hasPathOrNull("agentSampleSizeAsFractionOfPopulation"))
              c.getDouble("agentSampleSizeAsFractionOfPopulation")
            else 1.0,
          agents = BeamConfig.Beam.Agentsim.Agents(
            if (c.hasPathOrNull("agents")) c.getConfig("agents")
            else com.typesafe.config.ConfigFactory.parseString("agents{}")
          ),
          endTime = if (c.hasPathOrNull("endTime")) c.getString("endTime") else "30:00:00",
          firstIteration = if (c.hasPathOrNull("firstIteration")) c.getInt("firstIteration") else 0,
          lastIteration = if (c.hasPathOrNull("lastIteration")) c.getInt("lastIteration") else 0,
          populationAdjustment =
            if (c.hasPathOrNull("populationAdjustment")) c.getString("populationAdjustment") else "DEFAULT_ADJUSTMENT",
          scenarios = BeamConfig.Beam.Agentsim.Scenarios(
            if (c.hasPathOrNull("scenarios")) c.getConfig("scenarios")
            else com.typesafe.config.ConfigFactory.parseString("scenarios{}")
          ),
          scheduleMonitorTask = BeamConfig.Beam.Agentsim.ScheduleMonitorTask(
            if (c.hasPathOrNull("scheduleMonitorTask")) c.getConfig("scheduleMonitorTask")
            else com.typesafe.config.ConfigFactory.parseString("scheduleMonitorTask{}")
          ),
          schedulerParallelismWindow =
            if (c.hasPathOrNull("schedulerParallelismWindow")) c.getInt("schedulerParallelismWindow") else 30,
          simulationName = if (c.hasPathOrNull("simulationName")) c.getString("simulationName") else "beamville",
          startTime = if (c.hasPathOrNull("startTime")) c.getString("startTime") else "00:00:00",
          taz = BeamConfig.Beam.Agentsim.Taz(
            if (c.hasPathOrNull("taz")) c.getConfig("taz") else com.typesafe.config.ConfigFactory.parseString("taz{}")
          ),
          thresholdForMakingParkingChoiceInMeters =
            if (c.hasPathOrNull("thresholdForMakingParkingChoiceInMeters"))
              c.getInt("thresholdForMakingParkingChoiceInMeters")
            else 100,
          thresholdForWalkingInMeters =
            if (c.hasPathOrNull("thresholdForWalkingInMeters")) c.getInt("thresholdForWalkingInMeters") else 100,
          timeBinSize = if (c.hasPathOrNull("timeBinSize")) c.getInt("timeBinSize") else 3600,
          toll = BeamConfig.Beam.Agentsim.Toll(
            if (c.hasPathOrNull("toll")) c.getConfig("toll")
            else com.typesafe.config.ConfigFactory.parseString("toll{}")
          ),
          tuning = BeamConfig.Beam.Agentsim.Tuning(
            if (c.hasPathOrNull("tuning")) c.getConfig("tuning")
            else com.typesafe.config.ConfigFactory.parseString("tuning{}")
          )
        )
      }
    }

    case class Beamskimmer(
      writeAllModeSkimsForPeakNonPeakPeriodsInterval: scala.Int,
      writeFullSkimsInterval: scala.Int,
      writeObservedSkimsInterval: scala.Int,
      writeObservedSkimsPlusInterval: scala.Int
    )

    object Beamskimmer {

      def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Beamskimmer = {
        BeamConfig.Beam.Beamskimmer(
          writeAllModeSkimsForPeakNonPeakPeriodsInterval =
            if (c.hasPathOrNull("writeAllModeSkimsForPeakNonPeakPeriodsInterval"))
              c.getInt("writeAllModeSkimsForPeakNonPeakPeriodsInterval")
            else 0,
          writeFullSkimsInterval =
            if (c.hasPathOrNull("writeFullSkimsInterval")) c.getInt("writeFullSkimsInterval") else 0,
          writeObservedSkimsInterval =
            if (c.hasPathOrNull("writeObservedSkimsInterval")) c.getInt("writeObservedSkimsInterval") else 0,
          writeObservedSkimsPlusInterval =
            if (c.hasPathOrNull("writeObservedSkimsPlusInterval")) c.getInt("writeObservedSkimsPlusInterval") else 0
        )
      }
    }

    case class Calibration(
      counts: BeamConfig.Beam.Calibration.Counts,
      meanToCountsWeightRatio: scala.Double,
      mode: BeamConfig.Beam.Calibration.Mode,
      objectiveFunction: java.lang.String,
      roadNetwork: BeamConfig.Beam.Calibration.RoadNetwork
    )

    object Calibration {
      case class Counts(
        averageCountsOverIterations: scala.Int,
        countsScaleFactor: scala.Int,
        inputCountsFile: java.lang.String,
        writeCountsInterval: scala.Int
      )

      object Counts {

        def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Calibration.Counts = {
          BeamConfig.Beam.Calibration.Counts(
            averageCountsOverIterations =
              if (c.hasPathOrNull("averageCountsOverIterations")) c.getInt("averageCountsOverIterations") else 1,
            countsScaleFactor = if (c.hasPathOrNull("countsScaleFactor")) c.getInt("countsScaleFactor") else 10,
            inputCountsFile =
              if (c.hasPathOrNull("inputCountsFile")) c.getString("inputCountsFile")
              else "/test/input/beamville/counts.xml",
            writeCountsInterval = if (c.hasPathOrNull("writeCountsInterval")) c.getInt("writeCountsInterval") else 1
          )
        }
      }

      case class Mode(
        benchmarkFilePath: java.lang.String
      )

      object Mode {

        def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Calibration.Mode = {
          BeamConfig.Beam.Calibration.Mode(
            benchmarkFilePath = if (c.hasPathOrNull("benchmarkFilePath")) c.getString("benchmarkFilePath") else ""
          )
        }
      }

      case class RoadNetwork(
        travelTimes: BeamConfig.Beam.Calibration.RoadNetwork.TravelTimes
      )

      object RoadNetwork {
        case class TravelTimes(
          zoneBoundariesFilePath: java.lang.String,
          zoneODTravelTimesFilePath: java.lang.String
        )

        object TravelTimes {

          def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Calibration.RoadNetwork.TravelTimes = {
            BeamConfig.Beam.Calibration.RoadNetwork.TravelTimes(
              zoneBoundariesFilePath =
                if (c.hasPathOrNull("zoneBoundariesFilePath")) c.getString("zoneBoundariesFilePath") else "",
              zoneODTravelTimesFilePath =
                if (c.hasPathOrNull("zoneODTravelTimesFilePath")) c.getString("zoneODTravelTimesFilePath") else ""
            )
          }
        }

        def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Calibration.RoadNetwork = {
          BeamConfig.Beam.Calibration.RoadNetwork(
            travelTimes = BeamConfig.Beam.Calibration.RoadNetwork.TravelTimes(
              if (c.hasPathOrNull("travelTimes")) c.getConfig("travelTimes")
              else com.typesafe.config.ConfigFactory.parseString("travelTimes{}")
            )
          )
        }
      }

      def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Calibration = {
        BeamConfig.Beam.Calibration(
          counts = BeamConfig.Beam.Calibration.Counts(
            if (c.hasPathOrNull("counts")) c.getConfig("counts")
            else com.typesafe.config.ConfigFactory.parseString("counts{}")
          ),
          meanToCountsWeightRatio =
            if (c.hasPathOrNull("meanToCountsWeightRatio")) c.getDouble("meanToCountsWeightRatio") else 0.5,
          mode = BeamConfig.Beam.Calibration.Mode(
            if (c.hasPathOrNull("mode")) c.getConfig("mode")
            else com.typesafe.config.ConfigFactory.parseString("mode{}")
          ),
          objectiveFunction =
            if (c.hasPathOrNull("objectiveFunction")) c.getString("objectiveFunction")
            else "ModeChoiceObjectiveFunction",
          roadNetwork = BeamConfig.Beam.Calibration.RoadNetwork(
            if (c.hasPathOrNull("roadNetwork")) c.getConfig("roadNetwork")
            else com.typesafe.config.ConfigFactory.parseString("roadNetwork{}")
          )
        )
      }
    }

    case class Cluster(
      clusterType: scala.Option[java.lang.String],
      enabled: scala.Boolean
    )

    object Cluster {

      def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Cluster = {
        BeamConfig.Beam.Cluster(
          clusterType = if (c.hasPathOrNull("clusterType")) Some(c.getString("clusterType")) else None,
          enabled = c.hasPathOrNull("enabled") && c.getBoolean("enabled")
        )
      }
    }

    case class Debug(
      actor: BeamConfig.Beam.Debug.Actor,
      agentTripScoresInterval: scala.Int,
      clearRoutedOutstandingWorkEnabled: scala.Boolean,
      debugActorTimerIntervalInSec: scala.Int,
      debugEnabled: scala.Boolean,
      memoryConsumptionDisplayTimeoutInSec: scala.Int,
      secondsToWaitToClearRoutedOutstandingWork: scala.Int,
      stuckAgentDetection: BeamConfig.Beam.Debug.StuckAgentDetection,
      triggerMeasurer: BeamConfig.Beam.Debug.TriggerMeasurer
    )

    object Debug {
      case class Actor(
        logDepth: scala.Int
      )

      object Actor {

        def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Debug.Actor = {
          BeamConfig.Beam.Debug.Actor(
            logDepth = if (c.hasPathOrNull("logDepth")) c.getInt("logDepth") else 0
          )
        }
      }

      case class StuckAgentDetection(
        checkIntervalMs: scala.Long,
        checkMaxNumberOfMessagesEnabled: scala.Boolean,
        defaultTimeoutMs: scala.Long,
        enabled: scala.Boolean,
        overallSimulationTimeoutMs: scala.Long,
        thresholds: scala.List[BeamConfig.Beam.Debug.StuckAgentDetection.Thresholds$Elm]
      )

      object StuckAgentDetection {
        case class Thresholds$Elm(
          actorTypeToMaxNumberOfMessages: BeamConfig.Beam.Debug.StuckAgentDetection.Thresholds$Elm.ActorTypeToMaxNumberOfMessages,
          markAsStuckAfterMs: scala.Long,
          triggerType: java.lang.String
        )

        object Thresholds$Elm {
          case class ActorTypeToMaxNumberOfMessages(
            population: scala.Option[scala.Int],
            rideHailAgent: scala.Option[scala.Int],
            rideHailManager: scala.Option[scala.Int],
            transitDriverAgent: scala.Option[scala.Int]
          )

          object ActorTypeToMaxNumberOfMessages {

            def apply(
              c: com.typesafe.config.Config
            ): BeamConfig.Beam.Debug.StuckAgentDetection.Thresholds$Elm.ActorTypeToMaxNumberOfMessages = {
              BeamConfig.Beam.Debug.StuckAgentDetection.Thresholds$Elm.ActorTypeToMaxNumberOfMessages(
                population = if (c.hasPathOrNull("population")) Some(c.getInt("population")) else None,
                rideHailAgent = if (c.hasPathOrNull("rideHailAgent")) Some(c.getInt("rideHailAgent")) else None,
                rideHailManager = if (c.hasPathOrNull("rideHailManager")) Some(c.getInt("rideHailManager")) else None,
                transitDriverAgent =
                  if (c.hasPathOrNull("transitDriverAgent")) Some(c.getInt("transitDriverAgent")) else None
              )
            }
          }

          def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Debug.StuckAgentDetection.Thresholds$Elm = {
            BeamConfig.Beam.Debug.StuckAgentDetection.Thresholds$Elm(
              actorTypeToMaxNumberOfMessages =
                BeamConfig.Beam.Debug.StuckAgentDetection.Thresholds$Elm.ActorTypeToMaxNumberOfMessages(
                  if (c.hasPathOrNull("actorTypeToMaxNumberOfMessages")) c.getConfig("actorTypeToMaxNumberOfMessages")
                  else com.typesafe.config.ConfigFactory.parseString("actorTypeToMaxNumberOfMessages{}")
                ),
              markAsStuckAfterMs =
                if (c.hasPathOrNull("markAsStuckAfterMs"))
                  c.getDuration("markAsStuckAfterMs", java.util.concurrent.TimeUnit.MILLISECONDS)
                else 20000,
              triggerType =
                if (c.hasPathOrNull("triggerType")) c.getString("triggerType")
                else "beam.agentsim.agents.PersonAgent$ActivityStartTrigger"
            )
          }
        }

        def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Debug.StuckAgentDetection = {
          BeamConfig.Beam.Debug.StuckAgentDetection(
            checkIntervalMs =
              if (c.hasPathOrNull("checkIntervalMs"))
                c.getDuration("checkIntervalMs", java.util.concurrent.TimeUnit.MILLISECONDS)
              else 200,
            checkMaxNumberOfMessagesEnabled = !c.hasPathOrNull("checkMaxNumberOfMessagesEnabled") || c.getBoolean(
              "checkMaxNumberOfMessagesEnabled"
            ),
            defaultTimeoutMs =
              if (c.hasPathOrNull("defaultTimeoutMs"))
                c.getDuration("defaultTimeoutMs", java.util.concurrent.TimeUnit.MILLISECONDS)
              else 60000,
            enabled = c.hasPathOrNull("enabled") && c.getBoolean("enabled"),
            overallSimulationTimeoutMs =
              if (c.hasPathOrNull("overallSimulationTimeoutMs"))
                c.getDuration("overallSimulationTimeoutMs", java.util.concurrent.TimeUnit.MILLISECONDS)
              else 100000,
            thresholds = $_LBeamConfig_Beam_Debug_StuckAgentDetection_Thresholds$Elm(c.getList("thresholds"))
          )
        }
        private def $_LBeamConfig_Beam_Debug_StuckAgentDetection_Thresholds$Elm(
          cl: com.typesafe.config.ConfigList
        ): scala.List[BeamConfig.Beam.Debug.StuckAgentDetection.Thresholds$Elm] = {
          import scala.collection.JavaConverters._
          cl.asScala
            .map(
              cv =>
                BeamConfig.Beam.Debug.StuckAgentDetection
                  .Thresholds$Elm(cv.asInstanceOf[com.typesafe.config.ConfigObject].toConfig)
            )
            .toList
        }
      }

      case class TriggerMeasurer(
        enabled: scala.Boolean,
        writeStuckAgentDetectionConfig: scala.Boolean
      )

      object TriggerMeasurer {

        def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Debug.TriggerMeasurer = {
          BeamConfig.Beam.Debug.TriggerMeasurer(
            enabled = c.hasPathOrNull("enabled") && c.getBoolean("enabled"),
            writeStuckAgentDetectionConfig = !c.hasPathOrNull("writeStuckAgentDetectionConfig") || c.getBoolean(
              "writeStuckAgentDetectionConfig"
            )
          )
        }
      }

      def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Debug = {
        BeamConfig.Beam.Debug(
          actor = BeamConfig.Beam.Debug.Actor(
            if (c.hasPathOrNull("actor")) c.getConfig("actor")
            else com.typesafe.config.ConfigFactory.parseString("actor{}")
          ),
          agentTripScoresInterval =
            if (c.hasPathOrNull("agentTripScoresInterval")) c.getInt("agentTripScoresInterval") else 0,
          clearRoutedOutstandingWorkEnabled = c.hasPathOrNull("clearRoutedOutstandingWorkEnabled") && c.getBoolean(
            "clearRoutedOutstandingWorkEnabled"
          ),
          debugActorTimerIntervalInSec =
            if (c.hasPathOrNull("debugActorTimerIntervalInSec")) c.getInt("debugActorTimerIntervalInSec") else 0,
          debugEnabled = c.hasPathOrNull("debugEnabled") && c.getBoolean("debugEnabled"),
          memoryConsumptionDisplayTimeoutInSec =
            if (c.hasPathOrNull("memoryConsumptionDisplayTimeoutInSec"))
              c.getInt("memoryConsumptionDisplayTimeoutInSec")
            else 0,
          secondsToWaitToClearRoutedOutstandingWork =
            if (c.hasPathOrNull("secondsToWaitToClearRoutedOutstandingWork"))
              c.getInt("secondsToWaitToClearRoutedOutstandingWork")
            else 60,
          stuckAgentDetection = BeamConfig.Beam.Debug.StuckAgentDetection(
            if (c.hasPathOrNull("stuckAgentDetection")) c.getConfig("stuckAgentDetection")
            else com.typesafe.config.ConfigFactory.parseString("stuckAgentDetection{}")
          ),
          triggerMeasurer = BeamConfig.Beam.Debug.TriggerMeasurer(
            if (c.hasPathOrNull("triggerMeasurer")) c.getConfig("triggerMeasurer")
            else com.typesafe.config.ConfigFactory.parseString("triggerMeasurer{}")
          )
        )
      }
    }

    case class Exchange(
      scenario: BeamConfig.Beam.Exchange.Scenario
    )

    object Exchange {
      case class Scenario(
        convertWgs2Utm: scala.Boolean,
        fileFormat: java.lang.String,
        folder: java.lang.String,
        source: java.lang.String
      )

      object Scenario {

        def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Exchange.Scenario = {
          BeamConfig.Beam.Exchange.Scenario(
            convertWgs2Utm = c.hasPathOrNull("convertWgs2Utm") && c.getBoolean("convertWgs2Utm"),
            fileFormat = if (c.hasPathOrNull("fileFormat")) c.getString("fileFormat") else "xml",
            folder = if (c.hasPathOrNull("folder")) c.getString("folder") else "",
            source = if (c.hasPathOrNull("source")) c.getString("source") else "Beam"
          )
        }
      }

      def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Exchange = {
        BeamConfig.Beam.Exchange(
          scenario = BeamConfig.Beam.Exchange.Scenario(
            if (c.hasPathOrNull("scenario")) c.getConfig("scenario")
            else com.typesafe.config.ConfigFactory.parseString("scenario{}")
          )
        )
      }
    }

    case class Experimental(
      optimizer: BeamConfig.Beam.Experimental.Optimizer
    )

    object Experimental {
      case class Optimizer(
        enabled: scala.Boolean
      )

      object Optimizer {

        def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Experimental.Optimizer = {
          BeamConfig.Beam.Experimental.Optimizer(
            enabled = c.hasPathOrNull("enabled") && c.getBoolean("enabled")
          )
        }
      }

      def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Experimental = {
        BeamConfig.Beam.Experimental(
          optimizer = BeamConfig.Beam.Experimental.Optimizer(
            if (c.hasPathOrNull("optimizer")) c.getConfig("optimizer")
            else com.typesafe.config.ConfigFactory.parseString("optimizer{}")
          )
        )
      }
    }

    case class Logger(
      keepConsoleAppenderOn: scala.Boolean
    )

    object Logger {

      def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Logger = {
        BeamConfig.Beam.Logger(
          keepConsoleAppenderOn = !c.hasPathOrNull("keepConsoleAppenderOn") || c.getBoolean("keepConsoleAppenderOn")
        )
      }
    }

    case class Metrics(
      level: java.lang.String
    )

    object Metrics {

      def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Metrics = {
        BeamConfig.Beam.Metrics(
          level = if (c.hasPathOrNull("level")) c.getString("level") else "verbose"
        )
      }
    }

    case class Outputs(
      addTimestampToOutputDirectory: scala.Boolean,
      baseOutputDirectory: java.lang.String,
      defaultWriteInterval: scala.Int,
      displayPerformanceTimings: scala.Boolean,
      events: BeamConfig.Beam.Outputs.Events,
      generalizedLinkStats: BeamConfig.Beam.Outputs.GeneralizedLinkStats,
      generalizedLinkStatsInterval: scala.Int,
      stats: BeamConfig.Beam.Outputs.Stats,
      writeEventsInterval: scala.Int,
      writeGraphs: scala.Boolean,
      writeLinkTraversalInterval: scala.Int,
      writePlansInterval: scala.Int
    )

    object Outputs {
      case class Events(
        eventsToWrite: java.lang.String,
        fileOutputFormats: java.lang.String
      )

      object Events {

        def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Outputs.Events = {
          BeamConfig.Beam.Outputs.Events(
            eventsToWrite =
              if (c.hasPathOrNull("eventsToWrite")) c.getString("eventsToWrite")
              else
                "ActivityEndEvent,ActivityStartEvent,PersonEntersVehicleEvent,PersonLeavesVehicleEvent,ModeChoiceEvent,PathTraversalEvent,ReserveRideHailEvent,ReplanningEvent,RefuelSessionEvent,ChargingPlugInEvent,ChargingPlugOutEvent,ParkEvent,LeavingParkingEvent",
            fileOutputFormats = if (c.hasPathOrNull("fileOutputFormats")) c.getString("fileOutputFormats") else "csv"
          )
        }
      }

      case class GeneralizedLinkStats(
        endTime: scala.Int,
        startTime: scala.Int
      )

      object GeneralizedLinkStats {

        def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Outputs.GeneralizedLinkStats = {
          BeamConfig.Beam.Outputs.GeneralizedLinkStats(
            endTime = if (c.hasPathOrNull("endTime")) c.getInt("endTime") else 32400,
            startTime = if (c.hasPathOrNull("startTime")) c.getInt("startTime") else 25200
          )
        }
      }

      case class Stats(
        binSize: scala.Int
      )

      object Stats {

        def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Outputs.Stats = {
          BeamConfig.Beam.Outputs.Stats(
            binSize = if (c.hasPathOrNull("binSize")) c.getInt("binSize") else 3600
          )
        }
      }

      def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Outputs = {
        BeamConfig.Beam.Outputs(
          addTimestampToOutputDirectory = !c.hasPathOrNull("addTimestampToOutputDirectory") || c.getBoolean(
            "addTimestampToOutputDirectory"
          ),
          baseOutputDirectory =
            if (c.hasPathOrNull("baseOutputDirectory")) c.getString("baseOutputDirectory") else "output",
          defaultWriteInterval = if (c.hasPathOrNull("defaultWriteInterval")) c.getInt("defaultWriteInterval") else 1,
          displayPerformanceTimings = c.hasPathOrNull("displayPerformanceTimings") && c.getBoolean(
            "displayPerformanceTimings"
          ),
          events = BeamConfig.Beam.Outputs.Events(
            if (c.hasPathOrNull("events")) c.getConfig("events")
            else com.typesafe.config.ConfigFactory.parseString("events{}")
          ),
          generalizedLinkStats = BeamConfig.Beam.Outputs.GeneralizedLinkStats(
            if (c.hasPathOrNull("generalizedLinkStats")) c.getConfig("generalizedLinkStats")
            else com.typesafe.config.ConfigFactory.parseString("generalizedLinkStats{}")
          ),
          generalizedLinkStatsInterval =
            if (c.hasPathOrNull("generalizedLinkStatsInterval")) c.getInt("generalizedLinkStatsInterval") else 0,
          stats = BeamConfig.Beam.Outputs.Stats(
            if (c.hasPathOrNull("stats")) c.getConfig("stats")
            else com.typesafe.config.ConfigFactory.parseString("stats{}")
          ),
          writeEventsInterval = if (c.hasPathOrNull("writeEventsInterval")) c.getInt("writeEventsInterval") else 1,
          writeGraphs = !c.hasPathOrNull("writeGraphs") || c.getBoolean("writeGraphs"),
          writeLinkTraversalInterval =
            if (c.hasPathOrNull("writeLinkTraversalInterval")) c.getInt("writeLinkTraversalInterval") else 0,
          writePlansInterval = if (c.hasPathOrNull("writePlansInterval")) c.getInt("writePlansInterval") else 0
        )
      }
    }

    case class Physsim(
      eventsForFullVersionOfVia: scala.Boolean,
      eventsSampling: scala.Double,
      flowCapacityFactor: scala.Double,
      initializeRouterWithFreeFlowTimes: scala.Boolean,
      inputNetworkFilePath: java.lang.String,
      jdeqsim: BeamConfig.Beam.Physsim.Jdeqsim,
      linkStatsBinSize: scala.Int,
      linkStatsWriteInterval: scala.Int,
      overwriteLinkParamPath: java.lang.String,
      ptSampleSize: scala.Double,
      quick_fix_minCarSpeedInMetersPerSecond: scala.Double,
      skipPhysSim: scala.Boolean,
      speedDataFilePath: java.lang.String,
      storageCapacityFactor: scala.Double,
      writeEventsInterval: scala.Int,
      writeMATSimNetwork: scala.Boolean,
      writePlansInterval: scala.Int,
      writeRouteHistoryInterval: scala.Int
    )

    object Physsim {
      case class Jdeqsim(
        agentSimPhysSimInterfaceDebugger: BeamConfig.Beam.Physsim.Jdeqsim.AgentSimPhysSimInterfaceDebugger,
        cacc: BeamConfig.Beam.Physsim.Jdeqsim.Cacc
      )

      object Jdeqsim {
        case class AgentSimPhysSimInterfaceDebugger(
          enabled: scala.Boolean
        )

        object AgentSimPhysSimInterfaceDebugger {

          def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Physsim.Jdeqsim.AgentSimPhysSimInterfaceDebugger = {
            BeamConfig.Beam.Physsim.Jdeqsim.AgentSimPhysSimInterfaceDebugger(
              enabled = c.hasPathOrNull("enabled") && c.getBoolean("enabled")
            )
          }
        }

        case class Cacc(
          capacityPlansWriteInterval: scala.Int,
          enabled: scala.Boolean,
          minRoadCapacity: scala.Int,
          minSpeedMetersPerSec: scala.Int,
          speedAdjustmentFactor: scala.Double
        )

        object Cacc {

          def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Physsim.Jdeqsim.Cacc = {
            BeamConfig.Beam.Physsim.Jdeqsim.Cacc(
              capacityPlansWriteInterval =
                if (c.hasPathOrNull("capacityPlansWriteInterval")) c.getInt("capacityPlansWriteInterval") else 0,
              enabled = c.hasPathOrNull("enabled") && c.getBoolean("enabled"),
              minRoadCapacity = if (c.hasPathOrNull("minRoadCapacity")) c.getInt("minRoadCapacity") else 2000,
              minSpeedMetersPerSec =
                if (c.hasPathOrNull("minSpeedMetersPerSec")) c.getInt("minSpeedMetersPerSec") else 20,
              speedAdjustmentFactor =
                if (c.hasPathOrNull("speedAdjustmentFactor")) c.getDouble("speedAdjustmentFactor") else 1.0
            )
          }
        }

        def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Physsim.Jdeqsim = {
          BeamConfig.Beam.Physsim.Jdeqsim(
            agentSimPhysSimInterfaceDebugger = BeamConfig.Beam.Physsim.Jdeqsim.AgentSimPhysSimInterfaceDebugger(
              if (c.hasPathOrNull("agentSimPhysSimInterfaceDebugger")) c.getConfig("agentSimPhysSimInterfaceDebugger")
              else com.typesafe.config.ConfigFactory.parseString("agentSimPhysSimInterfaceDebugger{}")
            ),
            cacc = BeamConfig.Beam.Physsim.Jdeqsim.Cacc(
              if (c.hasPathOrNull("cacc")) c.getConfig("cacc")
              else com.typesafe.config.ConfigFactory.parseString("cacc{}")
            )
          )
        }
      }

      def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Physsim = {
        BeamConfig.Beam.Physsim(
          eventsForFullVersionOfVia = !c.hasPathOrNull("eventsForFullVersionOfVia") || c.getBoolean(
            "eventsForFullVersionOfVia"
          ),
          eventsSampling = if (c.hasPathOrNull("eventsSampling")) c.getDouble("eventsSampling") else 1.0,
          flowCapacityFactor = if (c.hasPathOrNull("flowCapacityFactor")) c.getDouble("flowCapacityFactor") else 1.0,
          initializeRouterWithFreeFlowTimes = !c.hasPathOrNull("initializeRouterWithFreeFlowTimes") || c.getBoolean(
            "initializeRouterWithFreeFlowTimes"
          ),
          inputNetworkFilePath =
            if (c.hasPathOrNull("inputNetworkFilePath")) c.getString("inputNetworkFilePath")
            else "/test/input/beamville/r5/physsim-network.xml",
          jdeqsim = BeamConfig.Beam.Physsim.Jdeqsim(
            if (c.hasPathOrNull("jdeqsim")) c.getConfig("jdeqsim")
            else com.typesafe.config.ConfigFactory.parseString("jdeqsim{}")
          ),
          linkStatsBinSize = if (c.hasPathOrNull("linkStatsBinSize")) c.getInt("linkStatsBinSize") else 3600,
          linkStatsWriteInterval =
            if (c.hasPathOrNull("linkStatsWriteInterval")) c.getInt("linkStatsWriteInterval") else 0,
          overwriteLinkParamPath =
            if (c.hasPathOrNull("overwriteLinkParamPath")) c.getString("overwriteLinkParamPath") else "",
          ptSampleSize = if (c.hasPathOrNull("ptSampleSize")) c.getDouble("ptSampleSize") else 1.0,
          quick_fix_minCarSpeedInMetersPerSecond =
            if (c.hasPathOrNull("quick_fix_minCarSpeedInMetersPerSecond"))
              c.getDouble("quick_fix_minCarSpeedInMetersPerSecond")
            else 0.5,
          skipPhysSim = c.hasPathOrNull("skipPhysSim") && c.getBoolean("skipPhysSim"),
          speedDataFilePath =
            if (c.hasPathOrNull("speedDataFilePath")) c.getString("speedDataFilePath")
            else "/test/input/beamville/r5/speedData.csv",
          storageCapacityFactor =
            if (c.hasPathOrNull("storageCapacityFactor")) c.getDouble("storageCapacityFactor") else 1.0,
          writeEventsInterval = if (c.hasPathOrNull("writeEventsInterval")) c.getInt("writeEventsInterval") else 0,
          writeMATSimNetwork = !c.hasPathOrNull("writeMATSimNetwork") || c.getBoolean("writeMATSimNetwork"),
          writePlansInterval = if (c.hasPathOrNull("writePlansInterval")) c.getInt("writePlansInterval") else 0,
          writeRouteHistoryInterval =
            if (c.hasPathOrNull("writeRouteHistoryInterval")) c.getInt("writeRouteHistoryInterval") else 10
        )
      }
    }

    case class Replanning(
      ModuleProbability_1: scala.Double,
      ModuleProbability_2: scala.Double,
      ModuleProbability_3: scala.Double,
      ModuleProbability_4: scala.Int,
      Module_1: java.lang.String,
      Module_2: java.lang.String,
      Module_3: java.lang.String,
      Module_4: java.lang.String,
      cleanNonCarModesInIteration: scala.Int,
      fractionOfIterationsToDisableInnovation: scala.Double,
      maxAgentPlanMemorySize: scala.Int
    )

    object Replanning {

      def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Replanning = {
        BeamConfig.Beam.Replanning(
          ModuleProbability_1 = if (c.hasPathOrNull("ModuleProbability_1")) c.getDouble("ModuleProbability_1") else 0.8,
          ModuleProbability_2 = if (c.hasPathOrNull("ModuleProbability_2")) c.getDouble("ModuleProbability_2") else 0.1,
          ModuleProbability_3 = if (c.hasPathOrNull("ModuleProbability_3")) c.getDouble("ModuleProbability_3") else 0.1,
          ModuleProbability_4 = if (c.hasPathOrNull("ModuleProbability_4")) c.getInt("ModuleProbability_4") else 0,
          Module_1 = if (c.hasPathOrNull("Module_1")) c.getString("Module_1") else "SelectExpBeta",
          Module_2 = if (c.hasPathOrNull("Module_2")) c.getString("Module_2") else "ClearRoutes",
          Module_3 = if (c.hasPathOrNull("Module_3")) c.getString("Module_3") else "ClearModes",
          Module_4 = if (c.hasPathOrNull("Module_4")) c.getString("Module_4") else "TimeMutator",
          cleanNonCarModesInIteration =
            if (c.hasPathOrNull("cleanNonCarModesInIteration")) c.getInt("cleanNonCarModesInIteration") else 0,
          fractionOfIterationsToDisableInnovation =
            if (c.hasPathOrNull("fractionOfIterationsToDisableInnovation"))
              c.getDouble("fractionOfIterationsToDisableInnovation")
            else Double.PositiveInfinity,
          maxAgentPlanMemorySize =
            if (c.hasPathOrNull("maxAgentPlanMemorySize")) c.getInt("maxAgentPlanMemorySize") else 5
        )
      }
    }

    case class Routing(
      baseDate: java.lang.String,
      r5: BeamConfig.Beam.Routing.R5,
      startingIterationForTravelTimesMSA: scala.Int,
      transitOnStreetNetwork: scala.Boolean
    )

    object Routing {
      case class R5(
        departureWindow: scala.Double,
        directory: java.lang.String,
        mNetBuilder: BeamConfig.Beam.Routing.R5.MNetBuilder,
        numberOfSamples: scala.Int,
        osmFile: java.lang.String,
        osmMapdbFile: java.lang.String
      )

      object R5 {
        case class MNetBuilder(
          fromCRS: java.lang.String,
          toCRS: java.lang.String
        )

        object MNetBuilder {

          def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Routing.R5.MNetBuilder = {
            BeamConfig.Beam.Routing.R5.MNetBuilder(
              fromCRS = if (c.hasPathOrNull("fromCRS")) c.getString("fromCRS") else "EPSG:4326",
              toCRS = if (c.hasPathOrNull("toCRS")) c.getString("toCRS") else "EPSG:26910"
            )
          }
        }

        def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Routing.R5 = {
          BeamConfig.Beam.Routing.R5(
            departureWindow = if (c.hasPathOrNull("departureWindow")) c.getDouble("departureWindow") else 15.0,
            directory = if (c.hasPathOrNull("directory")) c.getString("directory") else "/test/input/beamville/r5",
            mNetBuilder = BeamConfig.Beam.Routing.R5.MNetBuilder(
              if (c.hasPathOrNull("mNetBuilder")) c.getConfig("mNetBuilder")
              else com.typesafe.config.ConfigFactory.parseString("mNetBuilder{}")
            ),
            numberOfSamples = if (c.hasPathOrNull("numberOfSamples")) c.getInt("numberOfSamples") else 1,
            osmFile =
              if (c.hasPathOrNull("osmFile")) c.getString("osmFile") else "/test/input/beamville/r5/beamville.osm.pbf",
            osmMapdbFile =
              if (c.hasPathOrNull("osmMapdbFile")) c.getString("osmMapdbFile") else "/test/input/beamville/r5/osm.mapdb"
          )
        }
      }

      def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Routing = {
        BeamConfig.Beam.Routing(
          baseDate = if (c.hasPathOrNull("baseDate")) c.getString("baseDate") else "2016-10-17T00:00:00-07:00",
          r5 = BeamConfig.Beam.Routing.R5(
            if (c.hasPathOrNull("r5")) c.getConfig("r5") else com.typesafe.config.ConfigFactory.parseString("r5{}")
          ),
          startingIterationForTravelTimesMSA =
            if (c.hasPathOrNull("startingIterationForTravelTimesMSA")) c.getInt("startingIterationForTravelTimesMSA")
            else 0,
          transitOnStreetNetwork = !c.hasPathOrNull("transitOnStreetNetwork") || c.getBoolean("transitOnStreetNetwork")
        )
      }
    }

    case class Spatial(
      boundingBoxBuffer: scala.Int,
      localCRS: java.lang.String
    )

    object Spatial {

      def apply(c: com.typesafe.config.Config): BeamConfig.Beam.Spatial = {
        BeamConfig.Beam.Spatial(
          boundingBoxBuffer = if (c.hasPathOrNull("boundingBoxBuffer")) c.getInt("boundingBoxBuffer") else 5000,
          localCRS = if (c.hasPathOrNull("localCRS")) c.getString("localCRS") else "epsg:32631"
        )
      }
    }

    case class WarmStart(
      enabled: scala.Boolean,
      path: java.lang.String,
      routeHistoryFileName: java.lang.String,
      routeHistoryFilePath: java.lang.String,
      skimsFileName: java.lang.String,
      skimsFilePath: java.lang.String,
      skimsPlusFileName: java.lang.String,
      skimsPlusFilePath: java.lang.String
    )

    object WarmStart {

      def apply(c: com.typesafe.config.Config): BeamConfig.Beam.WarmStart = {
        BeamConfig.Beam.WarmStart(
          enabled = c.hasPathOrNull("enabled") && c.getBoolean("enabled"),
          path = if (c.hasPathOrNull("path")) c.getString("path") else "",
          routeHistoryFileName =
            if (c.hasPathOrNull("routeHistoryFileName")) c.getString("routeHistoryFileName") else "routeHistory.csv.gz",
          routeHistoryFilePath =
            if (c.hasPathOrNull("routeHistoryFilePath")) c.getString("routeHistoryFilePath") else "",
          skimsFileName = if (c.hasPathOrNull("skimsFileName")) c.getString("skimsFileName") else "skims.csv.gz",
          skimsFilePath = if (c.hasPathOrNull("skimsFilePath")) c.getString("skimsFilePath") else "",
          skimsPlusFileName =
            if (c.hasPathOrNull("skimsPlusFileName")) c.getString("skimsPlusFileName") else "skimsPlus.csv.gz",
          skimsPlusFilePath = if (c.hasPathOrNull("skimsPlusFilePath")) c.getString("skimsPlusFilePath") else ""
        )
      }
    }

    def apply(c: com.typesafe.config.Config): BeamConfig.Beam = {
      BeamConfig.Beam(
        agentsim = BeamConfig.Beam.Agentsim(
          if (c.hasPathOrNull("agentsim")) c.getConfig("agentsim")
          else com.typesafe.config.ConfigFactory.parseString("agentsim{}")
        ),
        beamskimmer = BeamConfig.Beam.Beamskimmer(
          if (c.hasPathOrNull("beamskimmer")) c.getConfig("beamskimmer")
          else com.typesafe.config.ConfigFactory.parseString("beamskimmer{}")
        ),
        calibration = BeamConfig.Beam.Calibration(
          if (c.hasPathOrNull("calibration")) c.getConfig("calibration")
          else com.typesafe.config.ConfigFactory.parseString("calibration{}")
        ),
        cluster = BeamConfig.Beam.Cluster(
          if (c.hasPathOrNull("cluster")) c.getConfig("cluster")
          else com.typesafe.config.ConfigFactory.parseString("cluster{}")
        ),
        debug = BeamConfig.Beam.Debug(
          if (c.hasPathOrNull("debug")) c.getConfig("debug")
          else com.typesafe.config.ConfigFactory.parseString("debug{}")
        ),
        exchange = BeamConfig.Beam.Exchange(
          if (c.hasPathOrNull("exchange")) c.getConfig("exchange")
          else com.typesafe.config.ConfigFactory.parseString("exchange{}")
        ),
        experimental = BeamConfig.Beam.Experimental(
          if (c.hasPathOrNull("experimental")) c.getConfig("experimental")
          else com.typesafe.config.ConfigFactory.parseString("experimental{}")
        ),
        inputDirectory =
          if (c.hasPathOrNull("inputDirectory")) c.getString("inputDirectory") else "/test/input/beamville",
        logger = BeamConfig.Beam.Logger(
          if (c.hasPathOrNull("logger")) c.getConfig("logger")
          else com.typesafe.config.ConfigFactory.parseString("logger{}")
        ),
        metrics = BeamConfig.Beam.Metrics(
          if (c.hasPathOrNull("metrics")) c.getConfig("metrics")
          else com.typesafe.config.ConfigFactory.parseString("metrics{}")
        ),
        outputs = BeamConfig.Beam.Outputs(
          if (c.hasPathOrNull("outputs")) c.getConfig("outputs")
          else com.typesafe.config.ConfigFactory.parseString("outputs{}")
        ),
        physsim = BeamConfig.Beam.Physsim(
          if (c.hasPathOrNull("physsim")) c.getConfig("physsim")
          else com.typesafe.config.ConfigFactory.parseString("physsim{}")
        ),
        replanning = BeamConfig.Beam.Replanning(
          if (c.hasPathOrNull("replanning")) c.getConfig("replanning")
          else com.typesafe.config.ConfigFactory.parseString("replanning{}")
        ),
        routing = BeamConfig.Beam.Routing(
          if (c.hasPathOrNull("routing")) c.getConfig("routing")
          else com.typesafe.config.ConfigFactory.parseString("routing{}")
        ),
        spatial = BeamConfig.Beam.Spatial(
          if (c.hasPathOrNull("spatial")) c.getConfig("spatial")
          else com.typesafe.config.ConfigFactory.parseString("spatial{}")
        ),
        useLocalWorker = !c.hasPathOrNull("useLocalWorker") || c.getBoolean("useLocalWorker"),
        warmStart = BeamConfig.Beam.WarmStart(
          if (c.hasPathOrNull("warmStart")) c.getConfig("warmStart")
          else com.typesafe.config.ConfigFactory.parseString("warmStart{}")
        )
      )
    }
  }

  case class Matsim(
    conversion: BeamConfig.Matsim.Conversion,
    modules: BeamConfig.Matsim.Modules
  )

  object Matsim {
    case class Conversion(
      defaultHouseholdIncome: BeamConfig.Matsim.Conversion.DefaultHouseholdIncome,
      generateVehicles: scala.Boolean,
      matsimNetworkFile: java.lang.String,
      osmFile: java.lang.String,
      populationFile: java.lang.String,
      scenarioDirectory: java.lang.String,
      shapeConfig: BeamConfig.Matsim.Conversion.ShapeConfig,
      vehiclesFile: java.lang.String
    )

    object Conversion {
      case class DefaultHouseholdIncome(
        currency: java.lang.String,
        period: java.lang.String,
        value: scala.Int
      )

      object DefaultHouseholdIncome {

        def apply(c: com.typesafe.config.Config): BeamConfig.Matsim.Conversion.DefaultHouseholdIncome = {
          BeamConfig.Matsim.Conversion.DefaultHouseholdIncome(
            currency = if (c.hasPathOrNull("currency")) c.getString("currency") else "usd",
            period = if (c.hasPathOrNull("period")) c.getString("period") else "year",
            value = if (c.hasPathOrNull("value")) c.getInt("value") else 50000
          )
        }
      }

      case class ShapeConfig(
        shapeFile: java.lang.String,
        tazIdFieldName: java.lang.String
      )

      object ShapeConfig {

        def apply(c: com.typesafe.config.Config): BeamConfig.Matsim.Conversion.ShapeConfig = {
          BeamConfig.Matsim.Conversion.ShapeConfig(
            shapeFile = if (c.hasPathOrNull("shapeFile")) c.getString("shapeFile") else "tz46_d00.shp",
            tazIdFieldName = if (c.hasPathOrNull("tazIdFieldName")) c.getString("tazIdFieldName") else "TZ46_D00_I"
          )
        }
      }

      def apply(c: com.typesafe.config.Config): BeamConfig.Matsim.Conversion = {
        BeamConfig.Matsim.Conversion(
          defaultHouseholdIncome = BeamConfig.Matsim.Conversion.DefaultHouseholdIncome(
            if (c.hasPathOrNull("defaultHouseholdIncome")) c.getConfig("defaultHouseholdIncome")
            else com.typesafe.config.ConfigFactory.parseString("defaultHouseholdIncome{}")
          ),
          generateVehicles = !c.hasPathOrNull("generateVehicles") || c.getBoolean("generateVehicles"),
          matsimNetworkFile =
            if (c.hasPathOrNull("matsimNetworkFile")) c.getString("matsimNetworkFile") else "Siouxfalls_network_PT.xml",
          osmFile = if (c.hasPathOrNull("osmFile")) c.getString("osmFile") else "south-dakota-latest.osm.pbf",
          populationFile =
            if (c.hasPathOrNull("populationFile")) c.getString("populationFile") else "Siouxfalls_population.xml",
          scenarioDirectory =
            if (c.hasPathOrNull("scenarioDirectory")) c.getString("scenarioDirectory")
            else "/path/to/scenario/directory",
          shapeConfig = BeamConfig.Matsim.Conversion.ShapeConfig(
            if (c.hasPathOrNull("shapeConfig")) c.getConfig("shapeConfig")
            else com.typesafe.config.ConfigFactory.parseString("shapeConfig{}")
          ),
          vehiclesFile = if (c.hasPathOrNull("vehiclesFile")) c.getString("vehiclesFile") else "Siouxfalls_vehicles.xml"
        )
      }
    }

    case class Modules(
      changeMode: BeamConfig.Matsim.Modules.ChangeMode,
      controler: BeamConfig.Matsim.Modules.Controler,
      counts: BeamConfig.Matsim.Modules.Counts,
      global: BeamConfig.Matsim.Modules.Global,
      households: BeamConfig.Matsim.Modules.Households,
      network: BeamConfig.Matsim.Modules.Network,
      parallelEventHandling: BeamConfig.Matsim.Modules.ParallelEventHandling,
      planCalcScore: BeamConfig.Matsim.Modules.PlanCalcScore,
      plans: BeamConfig.Matsim.Modules.Plans,
      qsim: BeamConfig.Matsim.Modules.Qsim,
      strategy: BeamConfig.Matsim.Modules.Strategy,
      transit: BeamConfig.Matsim.Modules.Transit,
      vehicles: BeamConfig.Matsim.Modules.Vehicles
    )

    object Modules {
      case class ChangeMode(
        modes: java.lang.String
      )

      object ChangeMode {

        def apply(c: com.typesafe.config.Config): BeamConfig.Matsim.Modules.ChangeMode = {
          BeamConfig.Matsim.Modules.ChangeMode(
            modes = if (c.hasPathOrNull("modes")) c.getString("modes") else "car,pt"
          )
        }
      }

      case class Controler(
        eventsFileFormat: java.lang.String,
        firstIteration: scala.Int,
        lastIteration: scala.Int,
        mobsim: java.lang.String,
        outputDirectory: java.lang.String,
        overwriteFiles: java.lang.String
      )

      object Controler {

        def apply(c: com.typesafe.config.Config): BeamConfig.Matsim.Modules.Controler = {
          BeamConfig.Matsim.Modules.Controler(
            eventsFileFormat = if (c.hasPathOrNull("eventsFileFormat")) c.getString("eventsFileFormat") else "xml",
            firstIteration = if (c.hasPathOrNull("firstIteration")) c.getInt("firstIteration") else 0,
            lastIteration = if (c.hasPathOrNull("lastIteration")) c.getInt("lastIteration") else 0,
            mobsim = if (c.hasPathOrNull("mobsim")) c.getString("mobsim") else "metasim",
            outputDirectory = if (c.hasPathOrNull("outputDirectory")) c.getString("outputDirectory") else "",
            overwriteFiles =
              if (c.hasPathOrNull("overwriteFiles")) c.getString("overwriteFiles") else "overwriteExistingFiles"
          )
        }
      }

      case class Counts(
        averageCountsOverIterations: scala.Int,
        countsScaleFactor: scala.Double,
        inputCountsFile: java.lang.String,
        outputformat: java.lang.String,
        writeCountsInterval: scala.Int
      )

      object Counts {

        def apply(c: com.typesafe.config.Config): BeamConfig.Matsim.Modules.Counts = {
          BeamConfig.Matsim.Modules.Counts(
            averageCountsOverIterations =
              if (c.hasPathOrNull("averageCountsOverIterations")) c.getInt("averageCountsOverIterations") else 0,
            countsScaleFactor = if (c.hasPathOrNull("countsScaleFactor")) c.getDouble("countsScaleFactor") else 10.355,
            inputCountsFile = if (c.hasPathOrNull("inputCountsFile")) c.getString("inputCountsFile") else "",
            outputformat = if (c.hasPathOrNull("outputformat")) c.getString("outputformat") else "all",
            writeCountsInterval = if (c.hasPathOrNull("writeCountsInterval")) c.getInt("writeCountsInterval") else 0
          )
        }
      }

      case class Global(
        coordinateSystem: java.lang.String,
        randomSeed: scala.Int
      )

      object Global {

        def apply(c: com.typesafe.config.Config): BeamConfig.Matsim.Modules.Global = {
          BeamConfig.Matsim.Modules.Global(
            coordinateSystem = if (c.hasPathOrNull("coordinateSystem")) c.getString("coordinateSystem") else "Atlantis",
            randomSeed = if (c.hasPathOrNull("randomSeed")) c.getInt("randomSeed") else 4711
          )
        }
      }

      case class Households(
        inputFile: java.lang.String,
        inputHouseholdAttributesFile: java.lang.String
      )

      object Households {

        def apply(c: com.typesafe.config.Config): BeamConfig.Matsim.Modules.Households = {
          BeamConfig.Matsim.Modules.Households(
            inputFile =
              if (c.hasPathOrNull("inputFile")) c.getString("inputFile") else "/test/input/beamville/households.xml",
            inputHouseholdAttributesFile =
              if (c.hasPathOrNull("inputHouseholdAttributesFile")) c.getString("inputHouseholdAttributesFile")
              else "/test/input/beamville/householdAttributes.xml"
          )
        }
      }

      case class Network(
        inputNetworkFile: java.lang.String
      )

      object Network {

        def apply(c: com.typesafe.config.Config): BeamConfig.Matsim.Modules.Network = {
          BeamConfig.Matsim.Modules.Network(
            inputNetworkFile =
              if (c.hasPathOrNull("inputNetworkFile")) c.getString("inputNetworkFile")
              else "/test/input/beamville/physsim-network.xml"
          )
        }
      }

      case class ParallelEventHandling(
        estimatedNumberOfEvents: scala.Int,
        numberOfThreads: scala.Int,
        oneThreadPerHandler: scala.Boolean,
        synchronizeOnSimSteps: scala.Boolean
      )

      object ParallelEventHandling {

        def apply(c: com.typesafe.config.Config): BeamConfig.Matsim.Modules.ParallelEventHandling = {
          BeamConfig.Matsim.Modules.ParallelEventHandling(
            estimatedNumberOfEvents =
              if (c.hasPathOrNull("estimatedNumberOfEvents")) c.getInt("estimatedNumberOfEvents") else 1000000000,
            numberOfThreads = if (c.hasPathOrNull("numberOfThreads")) c.getInt("numberOfThreads") else 1,
            oneThreadPerHandler = c.hasPathOrNull("oneThreadPerHandler") && c.getBoolean("oneThreadPerHandler"),
            synchronizeOnSimSteps = c.hasPathOrNull("synchronizeOnSimSteps") && c.getBoolean("synchronizeOnSimSteps")
          )
        }
      }

      case class PlanCalcScore(
        BrainExpBeta: scala.Long,
        earlyDeparture: scala.Long,
        lateArrival: scala.Long,
        learningRate: scala.Long,
        parameterset: scala.List[BeamConfig.Matsim.Modules.PlanCalcScore.Parameterset$Elm],
        performing: scala.Long,
        traveling: scala.Long,
        waiting: scala.Long,
        writeExperiencedPlans: scala.Boolean
      )

      object PlanCalcScore {
        case class Parameterset$Elm(
          activityType: java.lang.String,
          priority: scala.Int,
          scoringThisActivityAtAll: scala.Boolean,
          `type`: java.lang.String,
          typicalDuration: java.lang.String,
          typicalDurationScoreComputation: java.lang.String
        )

        object Parameterset$Elm {

          def apply(c: com.typesafe.config.Config): BeamConfig.Matsim.Modules.PlanCalcScore.Parameterset$Elm = {
            BeamConfig.Matsim.Modules.PlanCalcScore.Parameterset$Elm(
              activityType = if (c.hasPathOrNull("activityType")) c.getString("activityType") else "Home",
              priority = if (c.hasPathOrNull("priority")) c.getInt("priority") else 1,
              scoringThisActivityAtAll = !c.hasPathOrNull("scoringThisActivityAtAll") || c.getBoolean(
                "scoringThisActivityAtAll"
              ),
              `type` = if (c.hasPathOrNull("type")) c.getString("type") else "activityParams",
              typicalDuration = if (c.hasPathOrNull("typicalDuration")) c.getString("typicalDuration") else "01:00:00",
              typicalDurationScoreComputation =
                if (c.hasPathOrNull("typicalDurationScoreComputation")) c.getString("typicalDurationScoreComputation")
                else "uniform"
            )
          }
        }

        def apply(c: com.typesafe.config.Config): BeamConfig.Matsim.Modules.PlanCalcScore = {
          BeamConfig.Matsim.Modules.PlanCalcScore(
            BrainExpBeta =
              if (c.hasPathOrNull("BrainExpBeta"))
                c.getDuration("BrainExpBeta", java.util.concurrent.TimeUnit.MILLISECONDS)
              else 2,
            earlyDeparture =
              if (c.hasPathOrNull("earlyDeparture"))
                c.getDuration("earlyDeparture", java.util.concurrent.TimeUnit.MILLISECONDS)
              else 0,
            lateArrival =
              if (c.hasPathOrNull("lateArrival"))
                c.getDuration("lateArrival", java.util.concurrent.TimeUnit.MILLISECONDS)
              else -18,
            learningRate =
              if (c.hasPathOrNull("learningRate"))
                c.getDuration("learningRate", java.util.concurrent.TimeUnit.MILLISECONDS)
              else 1,
            parameterset = $_LBeamConfig_Matsim_Modules_PlanCalcScore_Parameterset$Elm(c.getList("parameterset")),
            performing =
              if (c.hasPathOrNull("performing")) c.getDuration("performing", java.util.concurrent.TimeUnit.MILLISECONDS)
              else 6,
            traveling =
              if (c.hasPathOrNull("traveling")) c.getDuration("traveling", java.util.concurrent.TimeUnit.MILLISECONDS)
              else -6,
            waiting =
              if (c.hasPathOrNull("waiting")) c.getDuration("waiting", java.util.concurrent.TimeUnit.MILLISECONDS)
              else 0,
            writeExperiencedPlans = !c.hasPathOrNull("writeExperiencedPlans") || c.getBoolean("writeExperiencedPlans")
          )
        }
        private def $_LBeamConfig_Matsim_Modules_PlanCalcScore_Parameterset$Elm(
          cl: com.typesafe.config.ConfigList
        ): scala.List[BeamConfig.Matsim.Modules.PlanCalcScore.Parameterset$Elm] = {
          import scala.collection.JavaConverters._
          cl.asScala
            .map(
              cv =>
                BeamConfig.Matsim.Modules.PlanCalcScore
                  .Parameterset$Elm(cv.asInstanceOf[com.typesafe.config.ConfigObject].toConfig)
            )
            .toList
        }
      }

      case class Plans(
        inputPersonAttributesFile: java.lang.String,
        inputPlansFile: java.lang.String
      )

      object Plans {

        def apply(c: com.typesafe.config.Config): BeamConfig.Matsim.Modules.Plans = {
          BeamConfig.Matsim.Modules.Plans(
            inputPersonAttributesFile =
              if (c.hasPathOrNull("inputPersonAttributesFile")) c.getString("inputPersonAttributesFile")
              else "/test/input/beamville/populationAttributes.xml",
            inputPlansFile =
              if (c.hasPathOrNull("inputPlansFile")) c.getString("inputPlansFile")
              else "/test/input/beamville/population.xml"
          )
        }
      }

      case class Qsim(
        endTime: java.lang.String,
        snapshotperiod: java.lang.String,
        startTime: java.lang.String
      )

      object Qsim {

        def apply(c: com.typesafe.config.Config): BeamConfig.Matsim.Modules.Qsim = {
          BeamConfig.Matsim.Modules.Qsim(
            endTime = if (c.hasPathOrNull("endTime")) c.getString("endTime") else "30:00:00",
            snapshotperiod = if (c.hasPathOrNull("snapshotperiod")) c.getString("snapshotperiod") else "00:00:00",
            startTime = if (c.hasPathOrNull("startTime")) c.getString("startTime") else "00:00:00"
          )
        }
      }

      case class Strategy(
        ModuleProbability_1: scala.Int,
        ModuleProbability_2: scala.Int,
        ModuleProbability_3: scala.Int,
        ModuleProbability_4: scala.Int,
        Module_1: java.lang.String,
        Module_2: java.lang.String,
        Module_3: java.lang.String,
        Module_4: java.lang.String,
        fractionOfIterationsToDisableInnovation: scala.Int,
        maxAgentPlanMemorySize: scala.Int,
        parameterset: scala.List[BeamConfig.Matsim.Modules.Strategy.Parameterset$Elm],
        planSelectorForRemoval: java.lang.String
      )

      object Strategy {
        case class Parameterset$Elm(
          disableAfterIteration: scala.Int,
          strategyName: java.lang.String,
          `type`: java.lang.String,
          weight: scala.Int
        )

        object Parameterset$Elm {

          def apply(c: com.typesafe.config.Config): BeamConfig.Matsim.Modules.Strategy.Parameterset$Elm = {
            BeamConfig.Matsim.Modules.Strategy.Parameterset$Elm(
              disableAfterIteration =
                if (c.hasPathOrNull("disableAfterIteration")) c.getInt("disableAfterIteration") else -1,
              strategyName = if (c.hasPathOrNull("strategyName")) c.getString("strategyName") else "",
              `type` = if (c.hasPathOrNull("type")) c.getString("type") else "strategysettings",
              weight = if (c.hasPathOrNull("weight")) c.getInt("weight") else 0
            )
          }
        }

        def apply(c: com.typesafe.config.Config): BeamConfig.Matsim.Modules.Strategy = {
          BeamConfig.Matsim.Modules.Strategy(
            ModuleProbability_1 = if (c.hasPathOrNull("ModuleProbability_1")) c.getInt("ModuleProbability_1") else 0,
            ModuleProbability_2 = if (c.hasPathOrNull("ModuleProbability_2")) c.getInt("ModuleProbability_2") else 0,
            ModuleProbability_3 = if (c.hasPathOrNull("ModuleProbability_3")) c.getInt("ModuleProbability_3") else 0,
            ModuleProbability_4 = if (c.hasPathOrNull("ModuleProbability_4")) c.getInt("ModuleProbability_4") else 0,
            Module_1 = if (c.hasPathOrNull("Module_1")) c.getString("Module_1") else "",
            Module_2 = if (c.hasPathOrNull("Module_2")) c.getString("Module_2") else "",
            Module_3 = if (c.hasPathOrNull("Module_3")) c.getString("Module_3") else "",
            Module_4 = if (c.hasPathOrNull("Module_4")) c.getString("Module_4") else "",
            fractionOfIterationsToDisableInnovation =
              if (c.hasPathOrNull("fractionOfIterationsToDisableInnovation"))
                c.getInt("fractionOfIterationsToDisableInnovation")
              else 999999,
            maxAgentPlanMemorySize =
              if (c.hasPathOrNull("maxAgentPlanMemorySize")) c.getInt("maxAgentPlanMemorySize") else 5,
            parameterset = $_LBeamConfig_Matsim_Modules_Strategy_Parameterset$Elm(c.getList("parameterset")),
            planSelectorForRemoval =
              if (c.hasPathOrNull("planSelectorForRemoval")) c.getString("planSelectorForRemoval")
              else "WorstPlanForRemovalSelector"
          )
        }
        private def $_LBeamConfig_Matsim_Modules_Strategy_Parameterset$Elm(
          cl: com.typesafe.config.ConfigList
        ): scala.List[BeamConfig.Matsim.Modules.Strategy.Parameterset$Elm] = {
          import scala.collection.JavaConverters._
          cl.asScala
            .map(
              cv =>
                BeamConfig.Matsim.Modules.Strategy
                  .Parameterset$Elm(cv.asInstanceOf[com.typesafe.config.ConfigObject].toConfig)
            )
            .toList
        }
      }

      case class Transit(
        transitModes: java.lang.String,
        useTransit: scala.Boolean,
        vehiclesFile: java.lang.String
      )

      object Transit {

        def apply(c: com.typesafe.config.Config): BeamConfig.Matsim.Modules.Transit = {
          BeamConfig.Matsim.Modules.Transit(
            transitModes = if (c.hasPathOrNull("transitModes")) c.getString("transitModes") else "pt",
            useTransit = c.hasPathOrNull("useTransit") && c.getBoolean("useTransit"),
            vehiclesFile = if (c.hasPathOrNull("vehiclesFile")) c.getString("vehiclesFile") else ""
          )
        }
      }

      case class Vehicles(
        vehiclesFile: java.lang.String
      )

      object Vehicles {

        def apply(c: com.typesafe.config.Config): BeamConfig.Matsim.Modules.Vehicles = {
          BeamConfig.Matsim.Modules.Vehicles(
            vehiclesFile = if (c.hasPathOrNull("vehiclesFile")) c.getString("vehiclesFile") else ""
          )
        }
      }

      def apply(c: com.typesafe.config.Config): BeamConfig.Matsim.Modules = {
        BeamConfig.Matsim.Modules(
          changeMode = BeamConfig.Matsim.Modules.ChangeMode(
            if (c.hasPathOrNull("changeMode")) c.getConfig("changeMode")
            else com.typesafe.config.ConfigFactory.parseString("changeMode{}")
          ),
          controler = BeamConfig.Matsim.Modules.Controler(
            if (c.hasPathOrNull("controler")) c.getConfig("controler")
            else com.typesafe.config.ConfigFactory.parseString("controler{}")
          ),
          counts = BeamConfig.Matsim.Modules.Counts(
            if (c.hasPathOrNull("counts")) c.getConfig("counts")
            else com.typesafe.config.ConfigFactory.parseString("counts{}")
          ),
          global = BeamConfig.Matsim.Modules.Global(
            if (c.hasPathOrNull("global")) c.getConfig("global")
            else com.typesafe.config.ConfigFactory.parseString("global{}")
          ),
          households = BeamConfig.Matsim.Modules.Households(
            if (c.hasPathOrNull("households")) c.getConfig("households")
            else com.typesafe.config.ConfigFactory.parseString("households{}")
          ),
          network = BeamConfig.Matsim.Modules.Network(
            if (c.hasPathOrNull("network")) c.getConfig("network")
            else com.typesafe.config.ConfigFactory.parseString("network{}")
          ),
          parallelEventHandling = BeamConfig.Matsim.Modules.ParallelEventHandling(
            if (c.hasPathOrNull("parallelEventHandling")) c.getConfig("parallelEventHandling")
            else com.typesafe.config.ConfigFactory.parseString("parallelEventHandling{}")
          ),
          planCalcScore = BeamConfig.Matsim.Modules.PlanCalcScore(
            if (c.hasPathOrNull("planCalcScore")) c.getConfig("planCalcScore")
            else com.typesafe.config.ConfigFactory.parseString("planCalcScore{}")
          ),
          plans = BeamConfig.Matsim.Modules.Plans(
            if (c.hasPathOrNull("plans")) c.getConfig("plans")
            else com.typesafe.config.ConfigFactory.parseString("plans{}")
          ),
          qsim = BeamConfig.Matsim.Modules.Qsim(
            if (c.hasPathOrNull("qsim")) c.getConfig("qsim")
            else com.typesafe.config.ConfigFactory.parseString("qsim{}")
          ),
          strategy = BeamConfig.Matsim.Modules.Strategy(
            if (c.hasPathOrNull("strategy")) c.getConfig("strategy")
            else com.typesafe.config.ConfigFactory.parseString("strategy{}")
          ),
          transit = BeamConfig.Matsim.Modules.Transit(
            if (c.hasPathOrNull("transit")) c.getConfig("transit")
            else com.typesafe.config.ConfigFactory.parseString("transit{}")
          ),
          vehicles = BeamConfig.Matsim.Modules.Vehicles(
            if (c.hasPathOrNull("vehicles")) c.getConfig("vehicles")
            else com.typesafe.config.ConfigFactory.parseString("vehicles{}")
          )
        )
      }
    }

    def apply(c: com.typesafe.config.Config): BeamConfig.Matsim = {
      BeamConfig.Matsim(
        conversion = BeamConfig.Matsim.Conversion(
          if (c.hasPathOrNull("conversion")) c.getConfig("conversion")
          else com.typesafe.config.ConfigFactory.parseString("conversion{}")
        ),
        modules = BeamConfig.Matsim.Modules(
          if (c.hasPathOrNull("modules")) c.getConfig("modules")
          else com.typesafe.config.ConfigFactory.parseString("modules{}")
        )
      )
    }
  }

  def apply(c: com.typesafe.config.Config): BeamConfig = {
    BeamConfig(
      beam = BeamConfig.Beam(
        if (c.hasPathOrNull("beam")) c.getConfig("beam") else com.typesafe.config.ConfigFactory.parseString("beam{}")
      ),
      matsim = BeamConfig.Matsim(
        if (c.hasPathOrNull("matsim")) c.getConfig("matsim")
        else com.typesafe.config.ConfigFactory.parseString("matsim{}")
      )
    )
  }
}
