package beam.agentsim.agents.choice.logit

sealed trait UtilityFunctionOperation {
  def apply(value: Double): Double
}

/**
  * Operation one can execute on a utility function.
  */

object UtilityFunctionOperation {

  case class Intercept(coefficient: Double) extends UtilityFunctionOperation {
    override def apply(value: Double): Double = coefficient
  }

  case class Multiplier(coefficient: Double) extends UtilityFunctionOperation {
    override def apply(value: Double): Double = coefficient * value
  }

  def apply(s: String, value: Double): UtilityFunctionOperation = {
    (s, value) match {
      case ("intercept", _)      => Intercept(value)
      case ("asc", _)            => Intercept(value)
      case ("multiplier", _)     => Multiplier(value)

      // for LCCM use
      case ("householdsize", _)  => Multiplier(value)
      case ("income", _)         => Multiplier(value)
      case ("male", _)           => Multiplier(value)
      case ("numbikes", _)       => Multiplier(value)
      case ("numcars", _)        => Multiplier(value)
      case ("surplus", _)        => Multiplier(value)
      case ("cost", _)           => Multiplier(value)
      case ("time", _)           => Multiplier(value)

      // for TPCM use
      case ("vehicleTime", _)    => Multiplier(value)
      case ("waitTime", _)       => Multiplier(value)
      case ("transfer", _)       => Multiplier(value)
      case ("shortWalkDist", _)  => Multiplier(value)
      case("longWalkDist", _)    => Multiplier(value)
      case ("shortBikeDist", _)  => Multiplier(value)
      case ("longBikeDist", _)   => Multiplier(value)
      case ("egressTime", _)     => Multiplier(value)
      case ("ascNoAuto", _)      => Intercept(value)
      case ("ascFewAuto", _)     => Intercept(value)
      case ("ascMoreAuto", _)    => Intercept(value)
      case _                     => throw new RuntimeException(s"Unknown Utility Parameter Type $s")
    }
  }
}
