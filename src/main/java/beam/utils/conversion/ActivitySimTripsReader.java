package beam.utils.conversion;

import com.opencsv.CSVReader;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.facilities.ActivityFacility;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ActivitySimTripsReader {
    //private static final Logger log = Logger.getLogger(ActivitySimTripsReader.class);

    Scenario scenario;
    PopulationFactory pf;
    File tripsFile;
    //TransportMode mode;

    Random r = new Random(15);
    HashMap<String, List<Id<ActivityFacility>>> tazFacilitymap = null;

    /**
     * Create an instance of ActivitySimTripsReader using an existing scenario
     * @param scenario A scenario
     * @param tripsFile File path to csv file containing trips
     */
    public ActivitySimTripsReader(Scenario scenario, File tripsFile){
        this.scenario = scenario;
        this.tripsFile = tripsFile;
        this.pf = scenario.getPopulation().getFactory();
    }

    public ActivitySimTripsReader(Scenario scenario, File tripsFile, HashMap<String, List<Id<ActivityFacility>>> tazFacilityMap) {
        this(scenario, tripsFile);
        this.tazFacilitymap = tazFacilityMap;
    }

    public void readTrips() {
        try {
            // Start a reader and read the header row. `col` is an index between the column names and numbers
            CSVReader reader = beam.utils.conversion.CSVUtils.createCSVReader(tripsFile.toString());
            String[] header = reader.readNext();
            Map<String, Integer> col = beam.utils.conversion.CSVUtils.getIndices(header,
                    new String[]{"person_id", "primary_purpose", "purpose", "destination", "origin", "depart", "trip_mode"}, // mandatory columns
                    new String[]{"household_id"} // optional columns
                    );

            // Read each line of the trips file
            String[] nextLine;
            double time;
            double depTime;
            Coord home;
            int plansize;
            PlanElement lastElement;
            Coord lastPlace;
            Coord nextCoord;
            String leg_mode;
            Leg leg;
            String purpose;
            String destId;
            Activity homeActivity2;
            ActivityFacility nextPlace;
            Activity otherActivity;
            Activity lastActivity;
            Id<Person> personId;
            Id<ActivityFacility> homeId;
            Person person;
            Plan plan;
            Activity homeActivity1;
            while((nextLine = reader.readNext()) != null) {
                // get plan for this person
                personId = Id.createPersonId(nextLine[col.get("person_id")]);

                homeId = Id.create("h" + nextLine[col.get("household_id")], ActivityFacility.class);

                person = scenario.getPopulation().getPersons().get(personId);
                if (person == null){
                    personId = Id.createPersonId("wc-" + personId);
                    person = scenario.getPopulation().getPersons().get(personId);
                }
                plan = person.getPlans().get(0);

                // Get time of departure for this trip and add randomness
                time = Double.parseDouble(nextLine[col.get("depart")]);
                depTime = time*3600 + r.nextDouble()*3600; //adds a random number within 60 min
                // Figure out how to store a value here so that the numbers don't over lap incorrectly
                // If there's an issue at the end, cut the code

                // Handle origin side
                // Is this the first trip of the day?
                if (plan.getPlanElements().isEmpty()){
                    homeActivity1 = pf.createActivityFromLinkId("Home", Id.createLinkId(homeId));
                    home = scenario.getActivityFacilities().getFacilities().get(homeId).getCoord();
                    homeActivity1.setEndTime(depTime);
                    homeActivity1.setCoord(home);
                    plan.addActivity(homeActivity1);
                } else { // if not, then there is an existing activity that we need to find. maybe?
                    // and add a departure to it!
                    // Find out how long the plan is
                    plansize = plan.getPlanElements().size();
                    // The last item of the plan is a travel leg, and we actually want the last activity
                    lastElement = plan.getPlanElements().get(plansize - 1);
                    if (lastElement instanceof Activity) {
                        lastActivity = (Activity) lastElement;
                        if (lastActivity.getFacilityId() != null) {
                            lastPlace = scenario.getActivityFacilities().getFacilities().get(lastActivity.getFacilityId()).getCoord();
                            lastActivity.setCoord(lastPlace);
                        }
                        lastActivity.setEndTime(depTime);

                    }
                }

                // Add leg to plan
                leg_mode = nextLine[col.get("trip_mode")];
                leg = pf.createLeg(leg_mode);
                leg.setMode(getLegMode(leg_mode));
                plan.addLeg(leg);

                // Handle next activity
                purpose = nextLine[col.get("purpose")];
                destId   = nextLine[col.get("destination")];

                if(purpose.equals("Home")) {
                    homeActivity2 = pf.createActivityFromLinkId("Home", Id.createLinkId(homeId));
                    home = scenario.getActivityFacilities().getFacilities().get(homeId).getCoord();
                    homeActivity2.setCoord(home);
                    plan.addActivity(homeActivity2);
                } else {
                    nextPlace = getFacilityinZone(destId);
                    otherActivity = pf.createActivityFromLinkId(purpose, Id.createLinkId(nextPlace.getId()));
                    nextCoord = scenario.getActivityFacilities().getFacilities().get(nextPlace.getId()).getCoord();
                    otherActivity.setCoord(nextCoord);

                    plan.addActivity(otherActivity);
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private String getLegMode(String leg_mode) {
        if(leg_mode.equals("BIKE")){
            return TransportMode.bike;
        } else if(leg_mode.equals("WALK")){
            return TransportMode.walk;
        } else if(leg_mode.matches("DRIVEALONEFREE|SHARED2FREE|SHARED3FREE")){
            return TransportMode.car;
        } else if(leg_mode.matches("DRIVE_COM|DRIVE_EXP|DRIVE_LOC|DRIVE_LRF|WALK_COM|WALK_EXP|WALK_LOC|WALK_LRF")){
            return TransportMode.pt;
        } else{
            return "we messed up";
        }
    }



    private ActivityFacility getFacilityinZone(String tazId) {
        List<Id<ActivityFacility>> facilityList = tazFacilitymap.get(tazId);
        Id<ActivityFacility> facilityId = facilityList.get(r.nextInt(facilityList.size()));

        return scenario.getActivityFacilities().getFacilities().get(facilityId);
    }



}
