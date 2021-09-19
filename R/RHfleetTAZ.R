# Create RH Fleet with TAZ IDs #################################################

if(!require(pacman)) install.packages("pacman")
pacman::p_load(pacman, tidyverse, dplyr)


## Set values ##################################################################

attributeTableDirectory <- "GIS/SLC_TAZ/attribute_tables/" #Include '/' at end
zoneCSVFile <- "SLC_South.csv"
TAZIDFile <- "SLC_South_TAZ.txt"

numVehicles <- 12
#Add support for multiple fleets?
rideHailManager <- "GlobalRHM"
vehicleType <- "micro"
shifts <- "{10:25200};{25300:80000}"

#These should be somewhere in the geofence, in whatever CRS
#the network is using (in our case right now, UTM12N)
initialX <- 419810
initialY <- 4486323

outputFile <- "test/input/slc_test/rhFleet-12-micro.csv"


## Read in the attribute table and find TAZ IDs, then write to file ############

TAZIDPath <- paste0(attributeTableDirectory, TAZIDFile)

read_csv(paste0(attributeTableDirectory, zoneCSVFile)) %>%
  select(TAZID) %>%
  pull(name = 'TAZID') %>%
  cat(file = TAZIDPath,
    sep = ",")

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
  TAZIDPath
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