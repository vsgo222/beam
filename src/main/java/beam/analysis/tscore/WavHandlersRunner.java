package beam.analysis.tscore;

import beam.sim.RideHailFleetInitializer;
import beam.sim.RideHailFleetInitializer.RideHailAgentInputData;
import com.google.devtools.common.options.Option;
import com.google.devtools.common.options.OptionsBase;
import com.google.devtools.common.options.OptionsParser;
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

import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class WavHandlersRunner {

    private static final Logger log = Logger.getLogger(WavHandlersRunner.class);


    public static void main(String[] args) throws IOException {
        /**
         * This handler reads the output_events and output_network files and provides data re: ridehail.
         * It uses a few CLI arguments:
         * --inputDir [i]: Directory of input files (BEAM output files)
         * --rideHailFleetFile [r]: Path of ridehail fleet file (used as an input for BEAM)
         * --outputFile [o]: Name of output file (not required, there is a default)
         */
        OptionsParser parser = OptionsParser.newOptionsParser(CLIOptions.class);
        parser.parseAndExitUponError(args);
        CLIOptions options = parser.getOptions(CLIOptions.class);
        if (options.inputDir.isEmpty() || options.rideHailFleetFile.isEmpty()) {
            printUsage(parser);
            throw new FileNotFoundException("Input Directory and Ridehail Fleet File are required");
        }

        String eventsFile = options.inputDir + "/output_events.xml.gz";
        String networkFile = options.inputDir + "/output_network.xml.gz";
        String outputFile = options.inputDir + "/" + options.outputFile;
        String ridehailFleetFile = options.rideHailFleetFile;

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
        HashMap<String, String> check = wrdc.getEmptyTime();
        int nonNull = 0;
        log.info("Average time for WAV's sitting empty: ");
        for (Map.Entry<String, String> id: check.entrySet()){
            if (id.getKey() != null)
                nonNull++;
            log.info(id.getKey() + "'s average time sitting empty: " + id.getValue());
        }
        log.info("Ratio of WAV's being used: " + ((double)nonNull/wrdc.getTotalWavCount()));
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
//        File fout = new File(outputFile);
//        FileOutputStream fos = new FileOutputStream(fout);
//        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(fos));
        
        BufferedWriter out = new BufferedWriter(
                new OutputStreamWriter(
                        new FileOutputStream(
                                new File(
                                        outputFile
                                )
                        )
                )
        );

        writeLine(out, "WAV Stats");
        writeLine(out, "=============================================");
        writeLine(out,"");

        aec.writeEventCounts(out);
//        HashMap<String, String> check = wrdc.getEmptyTime();
//        int nonNull = 0;
        writeLine(out, "Average time for WAV's sitting empty: ");
        for (Map.Entry<String, String> id: check.entrySet()){
            if (id.getKey() != null)
                nonNull++;
            writeLine(out, id.getKey() + "'s average time sitting empty: " + id.getValue());
        }
        writeLine(out, "Ratio of WAV's being used: " + ((double)nonNull/wrdc.getTotalWavCount()));
        writeLine(out, "Total number of people entering a vehicle: " + wrdc.getTotalPersonEntersVehicle());
        writeLine(out, "Number of people entering a 'ride hail' vehicle: " + wrdc.getTotalRideHailCount());
        writeLine(out, "Total wait time: " + wrdc.getTotalWaitTimeForAllPeople() + " seconds.");
        writeLine(out, "Total trips count: " + wrdc.getNumberOfTrips());

        writeLine(out, "------ WAV ridership information ---------");
        writeLine(out, "Number of all people entering WAVs: " + wrdc.getTotalWavCount());
        writeLine(out, "Total number of wc users in Wavs: " + wrdc.getWcPeopleInWavs());
        writeLine(out, "total wait time for WC users: " + wrdc.getTotalWaitTimeForWcPeople());
        writeLine(out, "Number of trips of WC users: " + wrdc.getNumberOfWcTrips());
        writeLine(out, "Average wait time for for WC users for WAV: " + wrdc.getAverageWcWaitTime() + " minutes.");
        writeLine(out, "Number of other people entering a WAV: " + wrdc.getOtherPeopleInWavs());
        writeLine(out, "Total wait time for others in wavs: " + wrdc.getTotalWaitTimeforOtherInWavs());
        writeLine(out, "Total trips for others in wavs: " + wrdc.getTotalTripsOthersInWavs());
        writeLine(out, "Average wait time for others in wavs: " + wrdc.getAverageOtherInWavWaitTime() + " minutes.");
        writeLine(out, "Percent of Wc people using WAVs: " + wrdc.getPercentWcInWavs() + " %");

        writeLine(out, "------- General non-wav ride hail information -----------");
        writeLine(out, "Total number of other people enter a ride hail: " + wrdc.getRideHailCount());
        writeLine(out, "Number of wc incorrectly placed into RH: " + wrdc.getWrongPlace());
        writeLine(out, "total wait time for other users: " + wrdc.getTotalWaitTimeForOtherPeople());
        writeLine(out, "Total count of other (non-wav) trips: " + wrdc.getNumberOfOtherTrips());
        writeLine(out, "Average wait time for Other users: " + wrdc.getAverageOtherWaitTime() + " minutes.");

        writeLine(out, "-------- General Travel Time information -------------");
        writeLine(out, "Average travel time for WC users on WAV: " + wrdc.getAverageWcTravelTime() + " minutes.");
        writeLine(out, "Averge travel time for other users in ride_hail: " + wrdc.getAverageOtherTravelTime() + " minutes.");

        writeLine(out, "----- General WAV utilization statistics ----------");


        out.close();

    }

    private static void printUsage(OptionsParser parser) {
        System.out.println("Usage: java -jar server.jar OPTIONS");
        System.out.println(parser.describeOptions(Collections.<String, String>emptyMap(),
                OptionsParser.HelpVerbosity.LONG));
    }

    private static void writeLine(BufferedWriter bw, String line) throws IOException {
        bw.write(line);
        bw.newLine();
    }


    public static Map<String, RideHailAgentInputData> createRideHailVehicleMap(String ridehailFile){
        List<RideHailAgentInputData> rideHailList;
        rideHailList = RideHailFleetInitializer.readFleetFromCSV(ridehailFile);

        Map<String, RideHailAgentInputData> rhMap = new LinkedHashMap<>();

        Iterator<RideHailAgentInputData> agentListIterator = rideHailList.iterator();
        RideHailAgentInputData placeholder;
        while(agentListIterator.hasNext()){
            placeholder = agentListIterator.next();
            rhMap.put(placeholder.id(), placeholder);
        }

        return rhMap;

    }
}