package beam.analysis.tscore;

import beam.agentsim.agents.ridehail.RideHailRequest;
import beam.sim.RideHailFleetInitializer;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.GenericEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;

public class WavRidehailDelayCalculator implements PersonEntersVehicleEventHandler,
        GenericEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler {
    private static final Logger log = Logger.getLogger(WavRidehailDelayCalculator.class);
    private final Map<String, RideHailFleetInitializer.RideHailAgentInputData> rhm;

    Scenario sc;
    Integer peopleInVehicles = 0;
    Integer peopleInWavs = 0;
    Map<String, Double> totalWaitTimeWcMap = new HashMap<>();
    Map<String, Double> totalWaitTimeOtherMap = new HashMap<>();
    Map<String, Double> reserveTimeMap = new HashMap<>();

    Double totalWaitTimeForAllPeople = 0.0;
    Double totalWaitTimeForWcPeople = 0.0;
    // non wc users will be "other"
    Double totalWaitTimeForOtherPeople = 0.0;
    int numberOfTrips = 0;
    int numberOfWcTrips = 0;
    int numberOfOtherTrips = 0;

    // calculating travel times
    Map<String, Double> departureTimes = new HashMap<>();
    Double travelTimeWcSum = 0.0;
    Double travelTimeOtherSum = 0.0;
    int travelTimeWcCount = 0;
    int travelTimeOtherCount = 0;

    public WavRidehailDelayCalculator(Scenario sc, Map<String, RideHailFleetInitializer.RideHailAgentInputData> rhm){
        this.sc = sc;
        this.rhm = rhm;
    }


    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        // counts the number of people in vehicles
        String thisPerson = event.getPersonId().toString();
        peopleInVehicles++;
        Id<Vehicle> thisVehicle = event.getVehicleId();
        String thisVehicleType = rhm.get(thisVehicle).vehicleType();

        // this is an example. Change to say sc.getWavs or something
        if(thisVehicleType.equals("WAV")) {
            peopleInWavs++;
        }

        // store the time each person enters vehicle
        // use a hashmap
        // if person has wheelchair
        if (thisPerson.contains("wc")) {
            if (reserveTimeMap.containsKey(thisPerson)) {
                Double waitTime = event.getTime() - reserveTimeMap.get(thisPerson);
                totalWaitTimeForWcPeople += waitTime;
                //add to total wait time also
                totalWaitTimeForAllPeople += waitTime;
                //If the total waittime map already contains a waittime for a previous trip, then add them together.
                if (totalWaitTimeWcMap.containsKey(thisPerson)) {
                    Double currentTotalWaittime = totalWaitTimeWcMap.get(thisPerson);
                    totalWaitTimeWcMap.replace(thisPerson, currentTotalWaittime + waitTime);
                } else {
                    totalWaitTimeWcMap.put(thisPerson, waitTime);
                }
                reserveTimeMap.remove(thisPerson);
                numberOfTrips++;
                numberOfWcTrips++;
            }
        } else { // if they don't have a wheelchair
            if (reserveTimeMap.containsKey(thisPerson)) {
                Double waitTime = event.getTime() - reserveTimeMap.get(thisPerson);
                totalWaitTimeForOtherPeople += waitTime;
                //add to total wait time also
                totalWaitTimeForAllPeople += waitTime;
                //If the total waittime map already contains a waittime for a previous trip, then add them together.
                if (totalWaitTimeOtherMap.containsKey(thisPerson)) {
                    Double currentTotalWaittime = totalWaitTimeOtherMap.get(thisPerson);
                    totalWaitTimeOtherMap.replace(thisPerson, currentTotalWaittime + waitTime);
                } else {
                    totalWaitTimeOtherMap.put(thisPerson, waitTime);
                }
                reserveTimeMap.remove(thisPerson);
                numberOfTrips++;
                numberOfOtherTrips++;
            }

        }


    }



    @Override
    public void handleEvent(GenericEvent genericEvent) {
        String thisPerson = genericEvent.getAttributes().get("person");

        if (genericEvent.getEventType().contains("Reserve")){
            Double requestTime = genericEvent.getTime();
            reserveTimeMap.put(thisPerson, requestTime);
        }

    }

    @Override
    public void handleEvent(PersonDepartureEvent personDepartureEvent) {
        String thisPerson = personDepartureEvent.getPersonId().toString();

        this.departureTimes.put(thisPerson, personDepartureEvent.getTime());
    }

    @Override
    public void handleEvent(PersonArrivalEvent personArrivalEvent) {
        String thisPerson = personArrivalEvent.getPersonId().toString();

        double departureTime = this.departureTimes.get(thisPerson);
        double travelTime = personArrivalEvent.getTime() - departureTime;

        if (thisPerson.contains("wc")) {
            this.travelTimeWcSum += travelTime;
            this.travelTimeWcCount++;
        } else {
            this.travelTimeOtherSum += travelTime;
            this.travelTimeOtherCount++;
        }
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

    public Double getTotalWaitTimeForWcPeople() {
        return totalWaitTimeForWcPeople;
    }

    public int getNumberOfWcTrips() {
        return numberOfWcTrips;
    }

    public double getAverageWcWaitTime() {
        // also convert to minutes
        return (totalWaitTimeForWcPeople / numberOfWcTrips) / 60 ;
    }

    public Double getTotalWaitTimeForOtherPeople() {
        return totalWaitTimeForOtherPeople;
    }

    public int getNumberOfOtherTrips() {
        return numberOfOtherTrips;
    }

    public double getAverageOtherWaitTime() {
        // also convert to minutes
        return (totalWaitTimeForOtherPeople / numberOfOtherTrips) / 60;
    }

    public double getAverageWcTravelTime() {
        return (travelTimeWcSum / travelTimeWcCount) / 60;
    }

    public double getAverageOtherTravelTime() {
        return (travelTimeOtherSum / travelTimeOtherCount) / 60;
    }



}
