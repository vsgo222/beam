package beam.analysis.tscore;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;

public class WavRidehailDelayCalculator implements PersonEntersVehicleEventHandler, PersonDepartureEventHandler {
    private static final Logger log = Logger.getLogger(WavRidehailDelayCalculator.class);

    Scenario sc;
    Integer peopleInVehicles = 0;
    Integer peopleInWavs = 0;

    public WavRidehailDelayCalculator(Scenario sc){
        this.sc = sc;
    }


    @Override
    public void handleEvent(PersonDepartureEvent event) {


    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        Id<Person> thisPerson = event.getPersonId();
        peopleInVehicles++;
        Id<Vehicle> thisVehicle = event.getVehicleId();

        if(sc.getTransitVehicles().getVehicles().containsKey(thisVehicle)) {
            peopleInWavs++;
        }

    }


    public Integer getPeopleInWavs(){
        return this.peopleInWavs;
    }
}
