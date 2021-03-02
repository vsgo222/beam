package beam.analysis.tscore;

import beam.agentsim.agents.ridehail.RideHailRequest;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.GenericEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;

public class WavRidehailDelayCalculator implements PersonEntersVehicleEventHandler, PersonDepartureEventHandler, GenericEventHandler {
    private static final Logger log = Logger.getLogger(WavRidehailDelayCalculator.class);

    Scenario sc;
    Integer peopleInVehicles = 0;
    Integer peopleInWavs = 0;
    Map<String, Double> totalWaitTimeMap = new HashMap<>();
    Map<String, Double> reserveTimeMap = new HashMap<>();
    Double totalWaitTimeForAllPeople = 0.0;
    int numberOfTrips = 0;

    public WavRidehailDelayCalculator(Scenario sc){
        this.sc = sc;
    }
    // total number of wavs
    // total number of ride hail vehicles
    // We want to know:
    // How many total people took a WAV?
    // How many wc users took a WAV?
    // Average wait time for WAVs? (total wait time of wc users / # of wc users
    // Can we get number of cancelled requests? (Total and WC)
    // Average wait time for all ride hail
    // Write this into a text file


    @Override
    public void handleEvent(PersonDepartureEvent event) {


    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        // counts the number of people in vehicles
        String thisPerson = event.getPersonId().toString();
        peopleInVehicles++;
        Id<Vehicle> thisVehicle = event.getVehicleId();

        // this is an example. Change to say sc.getWavs or something
        if(sc.getTransitVehicles().getVehicles().containsKey(thisVehicle)) {
            peopleInWavs++;
        }

        // store the time each person enters vehicle
        // use a hashmap
        if (reserveTimeMap.containsKey(thisPerson)){
            Double waitTime = event.getTime() - reserveTimeMap.get(thisPerson);
            totalWaitTimeForAllPeople += waitTime;
            //If the total waittime map already contains a waittime for a previous trip, then add them together.
            if (totalWaitTimeMap.containsKey(thisPerson)){
                Double currentTotalWaittime = totalWaitTimeMap.get(thisPerson);
                totalWaitTimeMap.replace(thisPerson, currentTotalWaittime + waitTime);
            }
            else {
                totalWaitTimeMap.put(thisPerson, waitTime);
            }
            reserveTimeMap.remove(thisPerson);
            numberOfTrips++;
        }


    }



    @Override
    public void handleEvent(GenericEvent genericEvent) {
        // how do we get the time that they request
        // for person abc get request time
        // for person abc get enterVehicle time
        // enterVehicle time - reserve time = wait time
        // store wait time in total wait time
        // average wait time = total wait time / # of wc users
        if (genericEvent.getEventType().contains("Reserve")){
            Double requestTime = genericEvent.getTime();
            reserveTimeMap.put(genericEvent.getAttributes().get("person"), requestTime);
        }

        // then we subtract from the time they enter vehicle and store that as "waiting time"
        // total waiting time (of wc only) / # of wc users

        // do the same for non wc

    }

    public int getNumberOfTrips() {
        return numberOfTrips;
    }

    public Double getTotalWaitTimeForAllPeople() {
        return totalWaitTimeForAllPeople;
    }

    public Integer getPeopleInWavs(){
        return this.peopleInWavs;
    }
}


