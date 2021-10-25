if(!require(pacman)) install.packages("pacman")
pacman::p_load(pacman, tidyverse, xml2, magrittr, rvest)

################################################################################

eventsCSV = "from_BYUsc/39.events.csv"
rhFleetFile = "from_BYUsc/rhFleet-12-micro.csv"

##for testing##
personRH <- 1067049  #normal ridehail
personRH2 <- 730554   #rh2
personReplan <- 1060618  #rh replanning

personModefail <- 1023248

# personX <- 1095346
# personY <- 1195571
# personZ <- 1251294

################################################################################

coltypes <- paste0("cdcdccdddd",
                   "cddddddddc",
                   "dcccdddddc",
                   "cdcccdldcc",
                   "ccccddcddc",
                   "cccc"
                   )

fullEvents <- read_csv(eventsCSV, col_types = coltypes)
eventCols <- c("person",
               "time",
               "type",
               "mode",
               "legMode",
               "vehicleType",
               "vehicle",
               "arrivalTime",
               "departureTime",
               "departTime",
               "length",
               "numPassengers",
               "actType",
               "personalVehicleAvailable"
               )
fullEvents %<>% relocate(eventCols)

events <- fullEvents %>% select(eventCols)
events %<>% mutate(
  travelTime = arrivalTime - departureTime,
  avgSpeed = length / travelTime
)

types <- unique(events$type)

UTAOD <- read_csv("from_BYUsc/UTAODpilotinfo.csv")
rhFleet <- read_csv(rhFleetFile)

rhHours <- 80000/3600 #add code to read from the shifts
rhNum <- nrow(rhFleet)

############# MESSING AROUND ###################################################

countEvents <- events %>%
  group_by(type) %>%
  summarize(n = n())

modechoice <- events %>%
  filter(type == "ModeChoice") %>%
  group_by(mode) %>%
  summarize(n = n())

legMode <- events %>%
  filter(!is.na(legMode),
         type == "arrival") %>%
  group_by(legMode) %>%
  summarise(n = n())

modeComparison <- left_join(modechoice,
          legMode,
          by = c("mode" = "legMode")
          ) %>%
  `colnames<-`(c("mode", "modechoice", "legmode")) %>%
  mutate(
    replans = modechoice - legmode,
    replan_pct = replans / modechoice
  )


events %>%
  filter(type %in% c("ModeChoice",
                     "Replanning",
                     "arrival"
                     )
         ) %>%
  arrange(person,
          time
          ) %>%
  filter(
    !((type == "ModeChoice" & lead(type) == "arrival") |
      (type == "arrival" & lag(type) == "ModeChoice"))
  ) %>%
  filter(
    !((type == "ModeChoice" & lead(type) == "Replanning") |
        (type == "Replanning" & lag(type) == "ModeChoice"))
  )


################################################

pathmode <- events %>%
  filter(type == "PathTraversal") %>%
  group_by(mode) %>%
  summarise(n = n())

pathvehicle <- events %>%
  filter(type == "PathTraversal") %>%
  group_by(vehicleType) %>%
  summarise(n = n())

###############################################

totRiders <- events %>%
  filter(vehicleType == "micro",
         type == "PathTraversal"
         ) %>%
  "$"(numPassengers) %>%
  sum()

totTrips <- events %>%
  filter(type == "PersonEntersVehicle",
         grepl("rideHail", vehicle),
         !grepl("rideHail", person)
         ) %>%
  nrow()

utilization <- totRiders / rhHours / rhNum

events %>%
  filter(!is.na(travelTime)) %>%
  group_by(vehicleType) %>%
  summarise(mean = mean(travelTime)/60,
            median = median(travelTime)/60)

events %>%
  filter(!is.na(travelTime)) %>%
  group_by(vehicleType) %>%
  filter(travelTime > 60) %>%
  summarise(mean = mean(travelTime)/60,
            median = median(travelTime)/60)

events %>%
  filter(!is.na(travelTime)) %>%
  group_by(vehicleType) %>%
  filter(travelTime > 60, travelTime < 20000) %>%
  summarise(mean = mean(travelTime)/60,
            median = median(travelTime)/60)

events %>%
  filter(!is.na(travelTime)) %>%
  mutate(travelTime = travelTime / 60) %>% 
  group_by(vehicleType) %>%
  boxplot(travelTime ~ vehicleType,
          data = .,
          horizontal = T
  )

events %>%
  filter(!is.na(travelTime)) %>%
  mutate(travelTime = travelTime / 60) %>% 
  group_by(vehicleType) %>%
  filter(travelTime > 60) %>%
  boxplot()

events %>%
  filter(!is.na(travelTime)) %>%
  mutate(travelTime = travelTime / 60) %>% 
  group_by(vehicleType) %>%
  filter(travelTime > 60, travelTime < 20000) %>%
  boxplot()

times <- events$travelTime %>%
  discard(is.na)
times[times>60][times<20000]

####################################################

rhTimes <- events %>%
  arrange(person, time) %>%
  mutate(
    rhReserveTime = ifelse(
      type == "ReserveRideHail" & person == lead(person),
      lead(time) - time,
      NA
      ),
    rhReserveOutcome = ifelse(
      type == "ReserveRideHail" & person == lead(person),
      lead(type),
      NA
      )
  ) %>%
  filter(!is.na(rhReserveTime))

rhTimes %>%
  group_by(rhReserveOutcome) %>%
  summarise(
    mean = mean(rhReserveTime) / 60,
    median = median(rhReserveTime) /60,
    min = min(rhReserveTime) / 60,
    max = max(rhReserveTime) / 60
    )

#####################################################

events %>%
  filter(type == "PathTraversal", mode == "bus") %>%
  select(travelTime) %>%
  mutate(travelTime = travelTime / 60) %>%
  boxplot()
