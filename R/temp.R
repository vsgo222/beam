rows <- eventsXML %>%
  html_elements("event")

xml <- function(col){
  rows %>% xml_attr(col)
}

events <- tibble(
  id = 1:length(rows),
  time = rows %>% xml_attr("time"),
  type = rows %>% xml_attr("type"),
  vehicle = rows %>% xml_attr("vehicle"),
  driver = rows %>% xml_attr("driver"),
  vehicleType = rows %>% xml_attr("vehicleType"),
  length = rows %>% xml_attr("length"),
  numPassengers = rows %>% xml_attr("numPassengers"),
  departureTime = rows %>% xml_attr("departureTime"),
  arrivalTime = rows %>% xml_attr("arrivalTime"),
  mode = rows %>% xml_attr("mode"),
  links = rows %>% xml_attr("links"),
  linkTravelTime = rows %>% xml_attr("linkTravelTime"),
  primaryFuelType = rows %>% xml_attr("primaryFuelType"),
  secondaryFuelType = rows %>% xml_attr("secondaryFuelType"),
  primaryFuel = rows %>% xml_attr("primaryFuel"),
  secondaryFuel = rows %>% xml_attr("secondaryFuel"),
  capactiy = rows %>% xml_attr("capacity"),
  startX = rows %>% xml_attr("startX"),
  startY = rows %>% xml_attr("startY"),
  endX = rows %>% xml_attr("endX"),
  endY = rows %>% xml_attr("endY"),
  primaryFuelLevel = rows %>% xml_attr("primaryFuelLevel"),
  secondaryFuelLevel = rows %>% xml_attr("secondaryFuelLevel"),
  seatingCapacity = rows %>% xml_attr("seatingCapacity"),
  tollPaid = rows %>% xml_attr("tollPaid"),
  fromStopIndex = rows %>% xml_attr("fromStopIndex"),
  toStopIndex = rows %>% xml_attr("toStopIndex"),
  riders = rows %>% xml_attr("riders"),
  person = rows %>% xml_attr("person"),
  currentTourMode = rows %>% xml_attr("currentTourMode"),
  expectedMaximumUtility = rows %>% xml_attr("expectedMaximumUtility"),
  location = rows %>% xml_attr("location"),
  availableAlternatives = rows %>% xml_attr("availableAlternatives"),
  personalVehicleAvailable = rows %>% xml_attr("personalVehicleAvailable"),
  tourIndex = rows %>% xml_attr("tourIndex"),
  link = rows %>% xml_attr("link"),
  facility = rows %>% xml_attr("facility"),
  actType = rows %>% xml_attr("actType"),
  legMode = rows %>% xml_attr("legMode"),
  departTime = rows %>% xml_attr("departTime"),
  score = rows %>% xml_attr("score"),
  parkingType = rows %>% xml_attr("parkingType"),
  pricingModel = rows %>% xml_attr("pricingModel"),
  chargingPointType = rows %>% xml_attr("chargingPointType"),
  parkingTaz = rows %>% xml_attr("parkingTaz"),
  cost = rows %>% xml_attr("cost"),
  locationX = rows %>% xml_attr("locationX"),
  locationY = rows %>% xml_attr("locationY"),
  reason = rows %>% xml_attr("reason")
)

test <- list(1:ncol(events))

for(i in 1:ncol(events)){
  test[[i]] <- events[,i] %>%
    unique()
}

map(test, nrow) %>% unlist()
