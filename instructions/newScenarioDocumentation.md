<!--
This document will describe how to create the salt lake scenario in Beam.
--->

# First steps: ActivitySim output
ActivitySim writes a persons, houshoulds, and trips file. We also need the
hhcoord file and the facility id table. These are the inputs into the
plans maker (wav_microtransit_scenario repository)

Verify that the trips file is arranged by person_id and `depart` (departure time).
Then run the `InputFilesReader.java` class.

This will write your *plans.xml.*

# Create a New Scenario: Salt Lake City

## Conversion from MATSim to BEAM.

Instructions are found in the [BEAM documentation](https://beam.readthedocs.io/en/latest/users.html#converting-a-matsim-scenario-to-run-with-beam).

Input files needed:
(Conversion input)
- network.xml
- utah-latest.osm.pbf
- tz49_d00_shp.zip
- population.xml (*plans.xml*)
(r5 folder)
- SLC.zip

We used a wc plans population
from our PlansMaker class in Java. Chris made the network. And the OSM file
came from ... The TAZ file is also necessary (contrary to the instructions) and
can be [downloaded](https://www.census.gov/geographies/mapping-files/2000/geo/carto-boundary-file.html)

The SLC zip file (contains a bunch of text files) can be found [here](https://github.com/tscore-utc/scenarios/tree/main/slc-tscore/input/r5).
This file goes (unextracted) into the R5 folder and set as the dummyFtfsPath
in `MatsimConversionTool`.

Add the following to the Beam config file

```
matsim.conversion {
  scenarioDirectory = "test/input/salt_lake"
  populationFile = "final_population_saturated_wc.xml"
  matsimNetworkFile = "highway_network.xml.gz"
  generateVehicles = true
  # vehiclesFile = "Siouxfalls_vehicles.xml"
  defaultHouseholdIncome {
    currency = "usd"
    period = "year"
    value = 50000
  }
  osmFile = "utah-latest.osm.pbf"
   shapeConfig {
     shapeFile = "tz49_d00.shp"
     tazIdFieldName = "TZ49_D00_I"
  }
}
```
Edit configurations to match the config file path and run. Should take about
30 seconds to run.

The output files (which become the input into the Beam run):
- beamFuelTypes.csv
- households.xml.gz
- householdAttributes.xml
- population.xml.gz
- populationAttributes.xml
- taz-centers.csv
- vehicles.csv
- vehicleTypes.csv (but it needs to be edited manually)


### Osmosis
This process will improve the osm.pbf file... I think.

After downloading from wikipedia, open a command line window.
Go to your beam working directory.
Start with the file path to the osmosis file.
```
C:\Users\nlant.EB232-21\osmosis-0.48.3\bin\osmosis.bat
```

Then using the output from the conversion run with `MatsimConversionTool` combine
with the script above.
```
C:\Users\nlant.EB232-21\osmosis-0.48.3\bin\osmosis.bat --read-pbf file=test/input/tiny/conversion-input/sflight_muni.osm.pbf --bounding-box top=37.96318345943006 left=-122.65944836338394 bottom=37.52697151338729 right=-122.13675232013519 completeWays=yes completeRelations=yes clipIncompleteEntities=true --write-pbf file=test/input/tiny/r5/sf-light-tiny.osm.pbf
```

#### In the case of cleaning an Osmosis

```
file\to\path>
osmfilter new-utah.osm --keep="highway=primary =secondary">utah_highway_filter_v1.osm
osmfilter new-utah.osm --keep="highway=primary =secondary" >utah_highway_filter_v2.osm.pbf
osmfilter utah.o5m --keep="highway=primary =secondary" >utah_highway_filter_v3.osm.pbf
osmfilter new-utah.osm --keep="highway=" >utah_highway_filter_v4.osm.pbf
osmfilter utah.o5m --keep="highway=motorway =trunk =primary =secondary =tertiary =unclassified =residential =motorway_link =trunk_link =primary_link =secondary_link =tertiary_link" >utah_highway_filter_v5.osm

at this point the filter is working... but it is missing something. For example
these do not run through osmosis (fixing bounding box is later and unessential step)
What is inside the utah-bouding-box?

osmosis --read-xml city.osm --tf accept-ways highway=* --used-node --write-xml highways.osm
```


## Running Beam

Update the config file. This will include some files to be copied from the
sf-light or the siouxfalls scenarios including `benchmark.csv` and `ptFares.csv`,
but these can be commented out.

Go to `vehicleTypes.csv` and in the vehicle category column put something like
"Car" in for each row. I also had to add CAR and Car and Body-Type-Default as
other types. It worked on the tiny scenario when I used the sf-light-vehicleTypes


Find and replace age from string to integer. (Fixed in the matsim plans maker)

Change the time zone

Maybe bounding box is too small for activities. This happens in the osmosis process
