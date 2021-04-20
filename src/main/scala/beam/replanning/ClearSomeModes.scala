package beam.replanning

import javax.inject.Inject
import org.matsim.api.core.v01.population.{HasPlansAndId, Leg, Person, Plan}
import org.matsim.core.config.Config
import org.slf4j.LoggerFactory

class ClearSomeModes @Inject()(config: Config) extends PlansStrategyAdopter {

  private val log = LoggerFactory.getLogger(classOf[ClearSomeModes])

  override def run(person: HasPlansAndId[Plan, Person]): Unit = {
    if (person.getId.toString.contains("wc")) {
      log.debug("Before Replanning ClearModes: Person-" + person.getId + " - " + person.getPlans.size())
      ReplanningUtil.makeExperiencedMobSimCompatible(person)
      ReplanningUtil.copyRandomPlanAndSelectForMutation(person.getSelectedPlan.getPerson)

      person.getSelectedPlan.getPlanElements.forEach {
        case leg: Leg =>
          leg.setMode("")
        case _ =>
      }
    }
    log.debug("After Replanning ClearModes: Person-" + person.getId + " - " + person.getPlans.size())
  }
}
