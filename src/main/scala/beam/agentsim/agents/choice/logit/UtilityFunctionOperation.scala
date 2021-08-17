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
    (s.toLowerCase, value) match {
      case ("intercept", _)      => Intercept(value)
      case ("asc", _)            => Intercept(value)
      case ("multiplier", _)     => Multiplier(value)
      case ("householdsize", _)  => Multiplier(value) // for LCCM use only
      case ("income", _)         => Multiplier(value) // for LCCM use only
      case ("male", _)           => Multiplier(value) // for LCCM use only
      case ("numbikes", _)       => Multiplier(value) // for LCCM use only
      case ("numcars", _)        => Multiplier(value) // for LCCM use only
      case ("surplus", _)        => Multiplier(value) // for LCCM use only
      case ("cost", _)           => Multiplier(value)
      case ("time", _)           => Multiplier(value)
      case _                     => throw new RuntimeException(s"Unknown Utility Parameter Type $s")
    }
  }
}
