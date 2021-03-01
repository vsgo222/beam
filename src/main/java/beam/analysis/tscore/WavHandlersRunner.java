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
        String eventsFile = "output_events.xml.gz";
        String networkFile = "output_network.xml.gz";
        String outputFile = "route_ridership.csv";

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
        networkReader.readFile(networkFile);

        EventsManager em = EventsUtils.createEventsManager();
        WavRidehailDelayCalculator wrdc = new WavRidehailDelayCalculator(scenario);
        em.addHandler(wrdc);
        new MatsimEventsReader(em).readFile(eventsFile);


       log.info("People in Wavs: " + wrdc.getPeopleInWavs());
    }
}
