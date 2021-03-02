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
        em.addHandler(wrdc);
        new MatsimEventsReader(em).readFile(eventsFile);

       // print into log file
       log.info("People in Wavs: " + wrdc.getPeopleInWavs());
       log.info("Total wait time: " + wrdc.getTotalWaitTimeForAllPeople());
       log.info("Total number of trips: " + wrdc.getNumberOfTrips());
       // write into text file
    }
}
