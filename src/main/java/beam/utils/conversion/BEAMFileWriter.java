package beam.utils.conversion;

import java.io.File;

/**
 * This Class is for converting Activity Sim Files into BEAM input files.
 * This will assist with Microtransit Research using BEAM.
 * @author Munseok Kim
 */
public class BEAMFileWriter {
    File persons;
    File households;
    File trips;
    File hhcoord;
    File facility_ids;

    /**
     *
     * @param persons persons.csv file, will be unique to each scenario
     * @param households households.csv file, will be unique to each scenario
     * @param trips trips.csv file, will be unique to each scenario
     * @param hhcoord hhcoord.csv file, same for all scenarios (in our scope)
     * @param facility_ids facility_ids file, same for all scenarios (in our scope)
     */
    public BEAMFileWriter(File persons, File households, File trips, File hhcoord, File facility_ids) {
        this.persons = persons;
        this.households = households;
        this.trips = trips;
        this.hhcoord = hhcoord;
        this.facility_ids = facility_ids;
    }
}
