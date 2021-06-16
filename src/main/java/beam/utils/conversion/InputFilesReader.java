package beam.utils.conversion;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.File;
import java.util.logging.Logger;

public class InputFilesReader {
    private static final Logger log = Logger.getLogger(String.valueOf(InputFilesReader.class));
    private final Scenario scenario;

    public InputFilesReader(Scenario sc){
        this.scenario = sc;
    }

    /**
     * Read the ActivitySim output files into the scenario
     * @param personsFile Path to activitysim output persons file
     * @param tripsFile Path to activitysim output trips file
     */
    public void readActivitySimFiles(File personsFile, File tripsFile,
                                     File facilitiesFile, File householdsFile,
                                     File householdCoordFile){
        ActivitySimFacilitiesReader facilitiesReader = new ActivitySimFacilitiesReader(scenario, facilitiesFile,
                householdsFile, householdCoordFile);
        facilitiesReader.readFacilities();
        facilitiesReader.readHouseholds();
        ActivitySimPersonsReader personsReader = new ActivitySimPersonsReader(scenario, personsFile);
        personsReader.readPersons();
        personsReader.readPlans();
        ActivitySimTripsReader tripsReader = new ActivitySimTripsReader(scenario, tripsFile,
                facilitiesReader.getTazFacilityMap());
        tripsReader.readTrips();


    }


    public static void main(String[] args){
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        scenario.getConfig().global().setCoordinateSystem("EPSG:26912");
        InputFilesReader reader = new InputFilesReader(scenario);
        // String scenarioPath = args[0];
        String scenarioPath = "scenarios/activitysim_output/";

        //File personsFile = new File(scenarioPath + args[1]);
        File personsFile = new File(scenarioPath + "final_persons.csv");
        File tripsFile = new File(scenarioPath + "final_trips.csv");
        File householdsFile = new File(scenarioPath + "final_households.csv");
        File facilitiesFile = new File(scenarioPath + "facility_ids.csv");
        File householdCoordFile = new File(scenarioPath + "hhcoord.csv");
        reader.readActivitySimFiles(personsFile, tripsFile, facilitiesFile, householdsFile, householdCoordFile);

        new PopulationWriter(scenario.getPopulation()).write(scenarioPath + "complete_population_plans.xml");
        // new FacilitiesWriter(scenario.getActivityFacilities()).write(scenarioPath + "facilities.xml.gz");
    }

}
