library(tidyverse)
library(magrittr)
library(sf)

#############################################################
# set wd
wd <- "/Users/haydenatchley/Documents/beam/data/wfrc_100k"
dd <- "/Users/haydenatchley/Documents/beam/data"

wd <- "X:/RA_Microtransit/BEAM/data/wfrc_100k"
dd <- "X:/RA_Microtransit/BEAM/data"

# Read in csvs
persons <- read_csv(paste0(wd,"/final_persons.csv"))
hh <- read_csv(paste0(wd,"/final_households.csv"))
plans <- read_csv(paste0(wd,"/final_plans.csv"))

parcel <- read_csv(paste0(dd,"/parcels.csv"))
address <- read_csv(paste0(dd,"/address_coordinates.csv"))
taz <- read_csv(paste0(dd,"/taz_centroids.csv"))

#add function
source("R/create_hh_coords_function.R")

#housing type
hType <- "House"

#plan index
planIndex = 0

###############################################################

###fix colnames
persons %<>% 
  rename(personId = person_id,
         householdId = household_id,
         isFemale = female,
         valueOfTime = value_of_time)
hh %<>% 
  rename(householdId = household_id,
         incomeValue = income,
         locationX = x,
         locationY = y)
plans %<>% 
  rename(personId = person_id,
         planElementType = ActivityElement,
         planElementIndex = PlanElementIndex,
         activityType = ActivityType,
         activityLocationX = x,
         activityLocationY = y,
         activityEndTime = departure_time,
         legMode = trip_mode)

persons %<>% 
  select(personId, householdId, age, sex, isFemale, valueOfTime)
hh %<>%
  select(householdId, TAZ, incomeValue, hhsize, auto_ownership, num_workers, locationX, locationY) %>% 
  mutate(num_workers = ifelse(num_workers == -8, 0, num_workers),
         autoWorkRatio =
           case_when(auto_ownership == 0 ~ "no_auto",
                     auto_ownership / num_workers < 1 ~ "auto_deficient",
                     auto_ownership / num_workers >= 1 ~ "auto_sufficient",
                     T ~ "we messed up, check asim to beam script"))
plans %<>%
  select(personId, legMode, planIndex, planElementIndex, planElementType, activityType, activityLocationX, activityLocationY, activityEndTime) %>% 
  left_join(select(persons, personId, householdId), by = "personId")

persons %<>%
  left_join(hh, by = "householdId")

persons %>% 
  write_csv(paste0(wd,"/final_persons.csv"))

####rewrite plans
#fix person ids
plans$personId %<>% as.integer()

plans %<>% mutate(planIndex = 0)

#fix modes
avail_modes <- c("bike", "walk", "car", "hov2", "hov2_teleportation",
                 "hov3", "hov3_teleportation", "drive_transit",
                 "walk_transit", "ride_hail", "ride_hail_pooled")

if(!all(plans$legMode[!is.na(plans$legMode)] %in% avail_modes)){
  for(i in 1:length(plans$legMode)){
    if(!is.na(plans$legMode[i])){
      if(plans$legMode[i] == "BIKE"){
        plans$legMode[i] = "bike"
      } else if(plans$legMode[i] == "WALK"){
        plans$legMode[i] = "walk"
      } else if(plans$legMode[i] %in% c("DRIVEALONEFREE","DRIVEALONEPAY")){
        plans$legMode[i] = "car"
      } else if(plans$legMode[i] %in% c("SHARED2FREE","SHARED2PAY")){
        plans$legMode[i] = ifelse(runif(1) < 1/2, "hov2", "hov2_teleportation")
      } else if(plans$legMode[i] %in% c("SHARED3FREE","SHARED3PAY")){
        plans$legMode[i] = ifelse(runif(1) < 1/3, "hov3", "hov3_teleportation")
      } else if(plans$legMode[i] %in% c("DRIVE_COM","DRIVE_EXP","DRIVE_LOC",
                                          "DRIVE_LRF","DRIVE_HVY")){
        plans$legMode[i] = "drive_transit"
      } else if(plans$legMode[i] %in% c("WALK_COM","WALK_EXP","WALK_LOC",
                                          "WALK_LRF","WALK_HVY")){
        plans$legMode[i] = "walk_transit"
      } else if(plans$legMode[i] %in% c("TNC_SINGLE","TAXI")){
        plans$legMode[i] = "ride_hail"
      } else if(plans$legMode[i] == "TNC_SHARED"){
        plans$legMode[i] = "ride_hail_pooled"
      } else if(plans$legMode[i] %in% avail_modes){
        plans$legMode[i] = plans$legMode[i]
      } else{
        plans$legMode[i] =
          "we messed up mode conversion (check activitysim to BEAM R script)"
      }
    }
  }
}

write_csv(plans, paste0(wd,"/final_plans.csv"), na = "")

###############################################################

#remove x,y if they exist
if("locationX" %in% colnames(hh)) hh %<>% select(-locationX)
if("locationY" %in% colnames(hh)) hh %<>% select(-locationY)

hh_coords <- create_hh_coords(hh, parcel, address, taz, crs = 26912)

#add x,y to hh file and write
hh %<>%
  left_join(hh_coords, by = "householdId") %>% 
  rename(locationX = x,
         locationY = y)
hh %>% 
  write_csv(paste0(wd,"/final_households.csv"))

#write hhattr file
hhattr <- hh %>% 
  select(householdId, locationX, locationY) %>% 
  mutate(housingType = hType)
write_csv(hhattr, paste0(wd,"/household_attributes.csv"))

###############################################################

#create vehicles file
nveh <- sum(hh$auto_ownership)
veh_hh <- map2(hh$householdId,
               hh$auto_ownership,
               function(.x,.y) rep.int(.x, .y)
               ) %>%
  unlist() %>%
  sort()

tibble(
  vehicleId = 1:nveh,
  vehicleTypeId = "CAR",
  householdId = veh_hh
) %>% 
  write_csv(paste0(wd,"/vehicles.csv"))

##############################################################

#sanity check
plans$legMode[plans$legMode %in% c("hov2","hov2_teleportation")] %>%
  table() %>% 
  as_tibble_row() %>% 
  mutate(pct = hov2/(hov2+hov2_teleportation))
plans$legMode[plans$legMode %in% c("hov3","hov3_teleportation")] %>%
  table() %>% 
  as_tibble_row() %>% 
  mutate(pct = hov3/(hov3+hov3_teleportation))
plans$legMode %>% unique()
hh$autoWorkRatio %>% unique()
