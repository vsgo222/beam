if(!require(pacman)) install.packages("pacman")
pacman::p_load(pacman, tidyverse, xml2, magrittr, rvest)

################################################################################

#inputEventsXML is BEAM run output_events.xml
# inputEventsXML = "X:\\RA_Microtransit\\MERGE\\output\\slc_test\\slc_test__2021-09-25_12-54-55_zrf"
# rhFleetFile = "X:\\RA_Microtransit\\MERGE\\test\\input\\slc_test\\rhFlees-12-micro.csv"

inputEventsXML = "from_BYUsc/output_events.xml"
rhFleetFile = "from_BYUsc/rhFlees-12-micro.csv"

################################################################################

####To unzip, not working yet####################################
#
# eventsFile <- paste0(inputDirectory,
#                      "\\output_events.xml.gz")
# copyDir <- paste0(inputDirectory,
#                   "\\R\\")
# copyDir %>%
#   {
#     if(!dir.exists(.)) dir.create(.)
#   }
# 
# file.copy(eventsFile, copyDir)
# 
# eventsFile <- paste0(inputDirectory,
#                      "\\R\\output_events.xml.gz")
# newName <- paste0(inputDirectory,
#                   "\\R\\output_events.gzip")
# 
# file.rename(eventsFile, newName)
# 
# untar(
#   newName
# )
##################################################################

eventsXML <- read_xml(inputEventsXML)

##### Convert eventsXML to tibble ####

#get list of event tags ####
rows <- eventsXML %>%
  html_elements("event")

#write the tibble ####
events <- tibble(
  event_id = 1:length(rows),
  time = rows %>% xml_attr("time") %>% as.numeric(),
  type = rows %>% xml_attr("type"),
  vehicle = rows %>% xml_attr("vehicle"),
  driver = rows %>% xml_attr("driver"),
  vehicleType = rows %>% xml_attr("vehicleType"),
  length = rows %>% xml_attr("length") %>% as.numeric(),
  numPassengers = rows %>% xml_attr("numPassengers") %>% as.numeric(),
  departureTime = rows %>% xml_attr("departureTime") %>% as.numeric(),
  arrivalTime = rows %>% xml_attr("arrivalTime") %>% as.numeric(),
  mode = rows %>% xml_attr("mode"),
  links = rows %>% xml_attr("links"),
  linkTravelTime = rows %>% xml_attr("linkTravelTime"),
  primaryFuelType = rows %>% xml_attr("primaryFuelType"),
  secondaryFuelType = rows %>% xml_attr("secondaryFuelType"),
  primaryFuel = rows %>% xml_attr("primaryFuel") %>% as.numeric(),
  secondaryFuel = rows %>% xml_attr("secondaryFuel") %>% as.numeric(),
  capacity = rows %>% xml_attr("capacity") %>% as.numeric(),
  startX = rows %>% xml_attr("startX") %>% as.numeric(),
  startY = rows %>% xml_attr("startY") %>% as.numeric(),
  endX = rows %>% xml_attr("endX") %>% as.numeric(),
  endY = rows %>% xml_attr("endY") %>% as.numeric(),
  primaryFuelLevel = rows %>% xml_attr("primaryFuelLevel") %>% as.numeric(),
  secondaryFuelLevel = rows %>% xml_attr("secondaryFuelLevel") %>% as.numeric(),
  seatingCapacity = rows %>% xml_attr("seatingCapacity") %>% as.numeric(),
  tollPaid = rows %>% xml_attr("tollPaid") %>% as.numeric(),
  fromStopIndex = rows %>% xml_attr("fromStopIndex") %>% as.numeric(),
  toStopIndex = rows %>% xml_attr("toStopIndex") %>% as.numeric(),
  riders = rows %>% xml_attr("riders"),
  person = rows %>% xml_attr("person"),
  currentTourMode = rows %>% xml_attr("currentTourMode"),
  expectedMaximumUtility = rows %>% xml_attr("expectedMaximumUtility"),
  location = rows %>% xml_attr("location") %>% as.numeric(),
  availableAlternatives = rows %>% xml_attr("availableAlternatives"),
  personalVehicleAvailable = rows %>% xml_attr("personalVehicleAvailable"),
  tourIndex = rows %>% xml_attr("tourIndex") %>% as.numeric(),
  link = rows %>% xml_attr("link") %>% as.numeric(),
  facility = rows %>% xml_attr("facility"),
  actType = rows %>% xml_attr("actType"),
  legMode = rows %>% xml_attr("legMode"),
  departTime = rows %>% xml_attr("departTime") %>% as.numeric(),
  score = rows %>% xml_attr("score"),
  parkingType = rows %>% xml_attr("parkingType"),
  pricingModel = rows %>% xml_attr("pricingModel"),
  chargingPointType = rows %>% xml_attr("chargingPointType"),
  parkingTaz = rows %>% xml_attr("parkingTaz"),
  cost = rows %>% xml_attr("cost") %>% as.numeric(),
  locationX = rows %>% xml_attr("locationX") %>% as.numeric(),
  locationY = rows %>% xml_attr("locationY") %>% as.numeric(),
  reason = rows %>% xml_attr("reason")
)
events %<>% relocate(person, .after = type)


######### MESSING AROUND ############

UTAOD <- read_csv("from_BYUsc/UTAODpilotinfo.csv")

fullEvents <- events

types <- events$type %>% unique()

events <- fullEvents %>%
  select(c(1,2,3,4,5,7,8,9,10,11,12,14,19,26,30,39,41,50))

countEvents <- events %>%
  group_by(type) %>%
  summarize(n = n())

modechoice <- filter(events, type == "ModeChoice") %>%
  group_by(mode) %>%
  summarize(n = n())

events %>%
  filter(type == "PathTraversal") %>%
  mutate(travelTime = arrivalTime - departureTime,
         speed = length/travelTime) %>%
  group_by(vehicleType) %>%
  summarize(n = n(),
            avgTravelTime = mean(travelTime, na.rm = T)/60)

events %>%
  filter(type == "PathTraversal") %>%
  mutate(travelTime = arrivalTime - departureTime,
         speed = length/travelTime) %>%
  group_by(mode) %>%
  summarize(n = n(),
            avgTravelTime = mean(travelTime))

events %>%
  filter(type == "PersonEntersVehicle") %>%
  group_by(vehicleType) %>%
  summarise(n = n())

events %>%
  filter(grepl("rideHailVehicle", vehicle)) %>%
  select(vehicle) %>%
  unique()
