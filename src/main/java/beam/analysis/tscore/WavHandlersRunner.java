package beam.analysis.tscore;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.vehicles.VehicleReaderV1;

public class WavHandlersRunner {

    private static final Logger log = Logger.getLogger(WavHandlersRunner.class);


    public static void main(String[] args){
        String eventsFile = "output/WAV/wav_250/trial/outputEvents.xml.gz";
        String networkFile = "output/WAV/wav_250/trial/output_network.xml.gz";
        //String outputFile = "output/WAV/wav_250/route_ridership.csv";

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
        networkReader.readFile(networkFile);

        EventsManager em = EventsUtils.createEventsManager();
        WavRidehailDelayCalculator wrdc = new WavRidehailDelayCalculator(scenario);
        AllEventsCounter aec = new AllEventsCounter(scenario);
        em.addHandler(wrdc);
        em.addHandler(aec);

        new MatsimEventsReader(em).readFile(eventsFile);

       // print into log file
       // log.info("Total number of WAVs: " + wrds.get );
        aec.printEventCounts();
       log.info("All people in WAVs: " + wrdc.getPeopleInWavs());
       // log.info("Total number of WC users: " + );
       // log.info("WC users in WAVs: " + );
       log.info("total wait time for WC users: " + wrdc.getTotalWaitTimeForWcPeople());
       log.info("Number of trips of WC users: " + wrdc.getNumberOfWcTrips());
        // log.info("Average wait time for WAVs: " + + " minutes.");
       log.info("Average wait time for for WC users for WAV: " + wrdc.getAverageWcWaitTime() + " minutes.");
       log.info("Average travel time for WC users on WAV: " + wrdc.getAverageWcTravelTime() + " minutes.");

       // log.info("Total number of ridehail vehicles: " +
       log.info("total wait time for other users: " + wrdc.getTotalWaitTimeForOtherPeople());
       log.info("total other trips: " + wrdc.getNumberOfOtherTrips());
       log.info("Average wait time for Other users: " + wrdc.getAverageOtherWaitTime() + " minutes.");
       log.info("Averge travel time for other users: " + wrdc.getAverageOtherTravelTime() + " minutes.");

       log.info("Total wait time: " + wrdc.getTotalWaitTimeForAllPeople());
       log.info("Total number of ride hail trips: " + wrdc.getNumberOfTrips());
       // write into text file
    }
}
