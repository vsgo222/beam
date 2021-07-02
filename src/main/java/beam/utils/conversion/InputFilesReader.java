package beam.utils.conversion;


import beam.utils.matsim_conversion.MatsimConversionTool;
import beam.utils.matsim_conversion.MatsimToBeam;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.FacilitiesWriter;

import java.io.File;
import java.util.logging.Logger;

/**
 * Combines the InputFilesReader (ActivitySim -> Matsim Files) and MatSim Conversion Tool (MatSim -> Beam Files)
 */
public class InputFilesReader {
    private static final Logger log = Logger.getLogger(String.valueOf(InputFilesReader.class));
    private static MatsimConversionTool matsimConversionTool;
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
        System.out.println("Facilities Done");
        facilitiesReader.readHouseholds();
        System.out.println("Households Done");
        ActivitySimPersonsReader personsReader = new ActivitySimPersonsReader(scenario, personsFile);
        personsReader.readPersons();
        System.out.println("Persons Done");
        personsReader.readPlans();
        System.out.println("Plans Done");
        ActivitySimTripsReader tripsReader = new ActivitySimTripsReader(scenario, tripsFile,
                facilitiesReader.getTazFacilityMap());
        tripsReader.readTrips();
        System.out.println("Trips Done");


    }

/*
** To use the InputFilesReader:
*  (If you don't already have one)
* 1. Create a folder in your beam directory called conversion_input and put in your input files
* - persons.csv, trips.csv, households.csv, facility_ids.csv, hhcoord.csv
* 2.
 */
    public static void main(String[] args){
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        scenario.getConfig().global().setCoordinateSystem("EPSG:26912");
        InputFilesReader reader = new InputFilesReader(scenario);
        File outputDirectory = new File("./conversion_output/");
        if (!outputDirectory.exists()){
            outputDirectory.mkdir();
        }
        String scenarioPath = "conversion_input/";
        String configPath = scenarioPath + "CONF.conf";
        File personsFile = new File(scenarioPath + "persons.csv");
        File tripsFile = new File(scenarioPath + "trips.csv");
        File householdsFile = new File(scenarioPath + "households.csv");
        File facilitiesFile = new File(scenarioPath + "facility_ids.csv");
        File householdCoordFile = new File(scenarioPath + "hhcoord.csv");
        //Reads in all the input files
        reader.readActivitySimFiles(personsFile, tripsFile, facilitiesFile, householdsFile, householdCoordFile);
        //Creates the plans.xml with the input files
        new PopulationWriter(scenario.getPopulation()).write(scenarioPath + "plans.xml");

        //new FacilitiesWriter(scenario.getActivityFacilities()).write(scenarioPath + "facilities.xml.gz");
        //Uses the MatsimConversionTool singleton object to convert plans.xml to BEAM output
        MatsimConversionTool.readInConfig(configPath);
    }

}
