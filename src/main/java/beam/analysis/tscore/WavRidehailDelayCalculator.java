package beam.analysis.tscore;

import beam.agentsim.agents.ridehail.RideHailRequest;
import beam.sim.RideHailFleetInitializer;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;

public class WavRidehailDelayCalculator implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler,
        GenericEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler {
    private static final Logger log = Logger.getLogger(WavRidehailDelayCalculator.class);
    private final Map<String, RideHailFleetInitializer.RideHailAgentInputData> rhm;

    Scenario sc;
    Integer peopleInVehicles = 0;
    Integer totalRideHailCount =0;
    Integer wcPeopleInWavs = 0;
    Integer otherPeopleInWavs = 0;
    Integer rideHailCount = 0;
    Integer wrongPlace = 0;
    Map<String, Double> totalWaitTimeWcMap = new HashMap<>();
    Map<String, Double> totalWaitTimeOtherInWavMap = new HashMap<>();
    Map<String, Double> totalWaitTimeOtherMap = new HashMap<>();
    Map<String, Double> reserveTimeMap = new HashMap<>();

    Double totalWaitTimeForAllPeople = 0.0;
    Double totalWaitTimeForWcPeople = 0.0;
    // non wc users will be "other"
    Double totalWaitTimeForOtherPeople = 0.0;
    Double totalWaitTimeForOtherPeopleInWavs = 0.0;
    int numberOfTrips = 0;
    int numberOfWcTrips = 0;
    int numberOfOtherinWavTrips = 0;
    int numberOfOtherTrips = 0;

    int wavCount = 0;
    int requestCount = 0;

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
        if(event.getTime() != 0.0) {
            // counts the number of people in vehicles
            String thisPerson = event.getPersonId().toString();
            peopleInVehicles++;
            String thisVehicle = event.getVehicleId().toString();
            // if rideHail, get the vehicle type
            if (thisVehicle.contains("rideHail")) {
                totalRideHailCount++;
                String thisVehicleType = rhm.get(thisVehicle).vehicleType();

                if (thisVehicleType.equals("WAV")) {
                    wavCount++;
                    // wc users in wavs
                    if (thisPerson.contains("wc")) {
                        // count number of users
                        wcPeopleInWavs++;

                        // calculate wait time
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
                    } else { // person does not have wheelchair but getting a wav
                        otherPeopleInWavs++;

                        // calculate wait time of other users in wav
                        if (reserveTimeMap.containsKey(thisPerson)) {
                            Double waitTime = event.getTime() - reserveTimeMap.get(thisPerson);
                            totalWaitTimeForOtherPeopleInWavs += waitTime;
                            //add to total wait time also
                            totalWaitTimeForAllPeople += waitTime;
                            //If the total waittime map already contains a waittime for a previous trip, then add them together.
                            if (totalWaitTimeOtherInWavMap.containsKey(thisPerson)) {
                                Double currentTotalWaittime = totalWaitTimeOtherInWavMap.get(thisPerson);
                                totalWaitTimeOtherInWavMap.replace(thisPerson, currentTotalWaittime + waitTime);
                            } else {
                                totalWaitTimeOtherInWavMap.put(thisPerson, waitTime);
                            }
                            reserveTimeMap.remove(thisPerson);
                            numberOfTrips++;
                            // I think some people are getting into wavs before reserving them...
                            numberOfOtherinWavTrips++;
                        }
                    }
                } else { // vehicle is not a wav
                    rideHailCount++;
                    // verify that no wc users get into a ride hail
                    if (thisPerson.contains("wc")) {
                        wrongPlace++;
                    }

                    // calculate wait time
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

            // WAV utilization stats
            // count wav
            // count time used (by hour of day)
            // count time empty (by hour of day)
        }


    }

    @Override
    public void handleEvent(PersonLeavesVehicleEvent event) {

    }



    @Override
    public void handleEvent(GenericEvent genericEvent) {
        String thisPerson = genericEvent.getAttributes().get("person");

        if (genericEvent.getEventType().contains("Reserve")){
            if (genericEvent.getAttributes().get("person").contains("wc")) {
                requestCount++;
            }
            Double requestTime = genericEvent.getTime();
            reserveTimeMap.put(thisPerson, requestTime);
        }

    }

    // calculate travel times
    //

    @Override
    public void handleEvent(PersonDepartureEvent personDepartureEvent) {
        String thisPerson = personDepartureEvent.getPersonId().toString();

        if(personDepartureEvent.getLegMode().contains("ride_hail")) {
            this.departureTimes.put(thisPerson, personDepartureEvent.getTime());
        }
    }

    @Override
    public void handleEvent(PersonArrivalEvent personArrivalEvent) {
        String thisPerson = personArrivalEvent.getPersonId().toString();

        if(personArrivalEvent.getLegMode().contains("ride_hail")) { // the problem is that ride_hail_transit has big travel times
            double departureTime = this.departureTimes.get(thisPerson);
            double travelTime = personArrivalEvent.getTime() - departureTime;


            // could add if WAV here to distinguish travel times for non wc in wavs...
            // for now they are counted in non wc group
            // WELL, personArrival events don't have vehicle IDs... just leg modes
            if (thisPerson.contains("wc")) {
                this.travelTimeWcSum += travelTime;
                this.travelTimeWcCount++;
            } else {
                this.travelTimeOtherSum += travelTime;
                this.travelTimeOtherCount++;
            }
        }
    }

    //total person enters vehicle count
    public int getTotalPersonEntersVehicle(){
        return peopleInVehicles;
    }

    //get total ride hail count (person enter ridehail vehicle)
    public int getTotalRideHailCount(){
        return totalRideHailCount;
    }

    //total wait time count
    public Double getTotalWaitTimeForAllPeople() {
        return totalWaitTimeForAllPeople;
    }

    // total trips count
    public int getNumberOfTrips() {
        return numberOfTrips;
    }



    // total number of wavs
    public int getTotalWavCount() {
        return wavCount;
    }

    //total number of wc people getting into Wavs (person enters vehicles could be repeats)
    public int getWcPeopleInWavs(){
        return this.wcPeopleInWavs;
    }

    // wait time for wc users
    public Double getTotalWaitTimeForWcPeople() {
        return totalWaitTimeForWcPeople;
    }

    // total wc trips count
    public int getNumberOfWcTrips() {
        return numberOfWcTrips;
    }

    // number of requests
    public int getRequestCount() {
        return requestCount;
    }

    // compute average wait time for wheelchair users
    public double getAverageWcWaitTime() {
        // also convert to minutes
        return (totalWaitTimeForWcPeople / numberOfWcTrips) / 60 ;
    }

    // total others in wavs count
    public int getOtherPeopleInWavs(){
        return otherPeopleInWavs;
    }

    // wait time for others in wavs
    public double getTotalWaitTimeforOtherInWavs(){
        return totalWaitTimeForOtherPeopleInWavs;
    }

    // total trips for others in wavs
    public int getTotalTripsOthersInWavs(){
        return numberOfOtherinWavTrips;
    }

    // calculate average wait time for others in wavs
    public double getAverageOtherInWavWaitTime(){
        return (totalWaitTimeForOtherPeopleInWavs / numberOfOtherinWavTrips) / 60;
    }

    // compute percent of wc users in wavs
    public double getPercentWcInWavs(){
        double wcCount = wcPeopleInWavs;
        double wavCounter = wavCount;
        return wcCount*100/wavCounter;
    }

    // total ride hail count (person enters vehicles)
    public Integer getRideHailCount() {
        // Total of other people enter a ride hail
        return rideHailCount;
    }

    // an wc in cars?? This should be zero
    public Integer getWrongPlace() {
        // verify that no wc user gets into a ride hail vehicle. should be 0
        return wrongPlace;
    }

    // total wait time for others in car rh
    public Double getTotalWaitTimeForOtherPeople() {
        return totalWaitTimeForOtherPeople;
    }

    // total number of trips for other users
    public int getNumberOfOtherTrips() {
        return numberOfOtherTrips;
    }

    // compute wait time for other users
    public double getAverageOtherWaitTime() {
        // also convert to minutes
        return (totalWaitTimeForOtherPeople / numberOfOtherTrips) / 60;
    }


    // travel time for wc users
    public double getAverageWcTravelTime() {
        return (travelTimeWcSum / travelTimeWcCount) / 60;
    }

    // travel time for other users
    public double getAverageOtherTravelTime() {
        return (travelTimeOtherSum / travelTimeOtherCount) / 60;
    }


}
