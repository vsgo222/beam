# Create RH Fleet with TAZ IDs #################################################

if(!require(pacman)) install.packages("pacman")
pacman::p_load(pacman, tidyverse, dplyr)


## Set values ##################################################################

zoneCSVFile <- "GIS/SLC_TAZ/attribute_tables/SLC_South.csv"
TAZIDFileDir <- "S:/Documents/scenarios/ridehail_fleets/geofencing"
TAZRelativeDir <- "scenarios/ridehail_fleets/geofencing"
rideHailOutDirectory <- "S:/Documents/scenarios/ridehail_fleets"

zoneName <- "SLC_South"

numVehicles <- 12
#Add support for multiple fleets?
rideHailManager <- "GlobalRHM"
vehicleType <- "micro"
shifts <- "{10:25200};{25300:80000}"

#These should be somewhere in the geofence, in whatever CRS
#the network is using (in our case right now, UTM12N)
initialX <- 419810
initialY <- 4486323

outputFile <- paste0(
  rideHailOutDirectory,
  "/", zoneName, "_TAZ",
  "-", numVehicles,
  "-", vehicleType,
  ".csv"
)


## Read in the attribute table and find TAZ IDs, then write to file ############

read_csv(zoneCSVFile) %>%
  select(TAZID) %>%
  pull(name = 'TAZID') %>%
  cat(file = paste0(TAZIDFileDir, "/", zoneName, "_TAZ"),
    sep = "\n")

## Create a dataframe and write RH Fleet data to it ############################

RHFleetIDs <- paste0("rideHailVehicle-1-1-", c(1:numVehicles))
geofenceX <- ""
geofenceY <- ""
geofenceRadius <- ""

RHFleet <- data.frame(
  RHFleetIDs,
  rideHailManager,
  vehicleType,
  initialX,
  initialY,
  shifts,
  geofenceX,
  geofenceY,
  geofenceRadius,
  paste0(TAZRelativeDir, "/", zoneName, "_TAZ")
)

colnames(RHFleet) <- c(
  "id",
  "rideHailManagerId",
  "vehicleType",
  "initialLocationX",
  "initialLocationY",
  "shifts",
  "geofenceX",
  "geofenceY",
  "geofenceRadius",
  "geofenceTAZFile"
)


## Write RH Fleet dataframe to .csv file #######################################

RHFleet %>%
  write_csv(file = outputFile)