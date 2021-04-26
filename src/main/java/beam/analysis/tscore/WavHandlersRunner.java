package beam.analysis.tscore;

import beam.sim.RideHailFleetInitializer;
import beam.sim.RideHailFleetInitializer.RideHailAgentInputData;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import scala.collection.Iterator;
import scala.collection.immutable.List;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class WavHandlersRunner {

    private static final Logger log = Logger.getLogger(WavHandlersRunner.class);


    public static void main(String[] args){
        String path = "output/small_mix/";
        String eventsFile = path + "outputEvents.xml.gz";
        String networkFile = path + "outputNetwork.xml.gz";
        //String outputFile = "output/WAV/wav_250/route_ridership.csv";
        String ridehailFleetFile = "test/input_new/small_mix/fleet_16.csv";

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
        networkReader.readFile(networkFile);

        Map<String, RideHailAgentInputData> rhm = createRideHailVehicleMap(ridehailFleetFile);

        EventsManager em = EventsUtils.createEventsManager();
        WavRidehailDelayCalculator wrdc = new WavRidehailDelayCalculator(scenario, rhm);
        AllEventsCounter aec = new AllEventsCounter(scenario);
        em.addHandler(wrdc);
        em.addHandler(aec);

        new MatsimEventsReader(em).readFile(eventsFile);

       // print into log file
        aec.printEventCounts();

        log.info("Total number of people entering a vehicle: " + wrdc.getTotalPersonEntersVehicle());
        log.info("Number of people entering a 'ride hail' vehicle: " + wrdc.getTotalRideHailCount());
        log.info("Total wait time: " + wrdc.getTotalWaitTimeForAllPeople() + " seconds.");
        log.info("Total trips count: " + wrdc.getNumberOfTrips());

        log.info("------ WAV ridership information ---------");
        log.info("Number of all people entering WAVs: " + wrdc.getTotalWavCount());
        log.info("Total number of wc users in Wavs: " + wrdc.getWcPeopleInWavs());
        log.info("total wait time for WC users: " + wrdc.getTotalWaitTimeForWcPeople());
        log.info("Number of trips of WC users: " + wrdc.getNumberOfWcTrips());
        log.info("Average wait time for for WC users for WAV: " + wrdc.getAverageWcWaitTime() + " minutes.");
        log.info("Number of other people entering a WAV: " + wrdc.getOtherPeopleInWavs());
        log.info("Total wait time for others in wavs: " + wrdc.getTotalWaitTimeforOtherInWavs());
        log.info("Total trips for others in wavs: " + wrdc.getTotalTripsOthersInWavs());
        log.info("Average wait time for others in wavs: " + wrdc.getAverageOtherInWavWaitTime() + " minutes.");
        log.info("Percent of Wc people using WAVs: " + wrdc.getPercentWcInWavs() + " %");

        log.info("------- General non-wav ride hail information -----------");
        log.info("Total number of other people enter a ride hail: " + wrdc.getRideHailCount());
        log.info("Number of wc incorrectly placed into RH: " + wrdc.getWrongPlace());
        log.info("total wait time for other users: " + wrdc.getTotalWaitTimeForOtherPeople());
        log.info("Total count of other (non-wav) trips: " + wrdc.getNumberOfOtherTrips());
        log.info("Average wait time for Other users: " + wrdc.getAverageOtherWaitTime() + " minutes.");

        log.info("-------- General Travel Time information -------------");
        log.info("Average travel time for WC users on WAV: " + wrdc.getAverageWcTravelTime() + " minutes.");
        log.info("Averge travel time for other users in ride_hail: " + wrdc.getAverageOtherTravelTime() + " minutes.");

        log.info("----- General WAV utilization statistics ----------");

       // write into text file
    }


    public static Map<String, RideHailAgentInputData> createRideHailVehicleMap(String ridehailFile){
        List<RideHailAgentInputData> rideHailList;
        rideHailList = RideHailFleetInitializer.readFleetFromCSV(ridehailFile);

        Map<String, RideHailAgentInputData> rhMap = new LinkedHashMap<>();

        Iterator<RideHailAgentInputData> agentListIterator = rideHailList.iterator();
        RideHailAgentInputData placeholder;
        while(agentListIterator.hasNext()){ ;
            placeholder = agentListIterator.next();
            rhMap.put(placeholder.id(), placeholder);
        }

        return rhMap;

    }
}
