library(tidyverse)
library(magrittr)

#############################################################
# set wd
wd <- "test/input/csv_test/data"

# Read in csvs
persons <- read_csv(paste0(wd,"/final_persons.csv"))
hh <- read_csv(paste0(wd,"/final_households.csv"))
plans <- read_csv(paste0(wd,"/final_plans.csv"))

parcel <- read_csv(paste0(wd,"/parcels.csv"))
address <- read_csv(paste0(wd,"/address_coordinates.csv"))
taz <- read_csv(paste0(wd,"/taz_centroids.csv"))

#add function
source("R/create_hh_coords_function.R")

###############################################################

####rewrite plans
#fix person ids
plans$person_id %<>% as.integer()

#fix modes
avail_modes <- c("bike", "walk", "car", "hov2", "hov2_teleportation",
                 "hov3", "hov3_teleportation", "drive_transit",
                 "walk_transit", "ride_hail", "ride_hail_pooled")

for(i in 1:length(plans$trip_mode)){
  if(!is.na(plans$trip_mode[i])){
    if(plans$trip_mode[i] == "BIKE"){
      plans$trip_mode[i] = "bike"
    } else if(plans$trip_mode[i] == "WALK"){
      plans$trip_mode[i] = "walk"
    } else if(plans$trip_mode[i] %in% c("DRIVEALONEFREE","DRIVEALONEPAY")){
      plans$trip_mode[i] = "car"
    } else if(plans$trip_mode[i] %in% c("SHARED2FREE","SHARED2PAY")){
      plans$trip_mode[i] = ifelse(runif(1) < 1/2, "hov2", "hov2_teleportation")
    } else if(plans$trip_mode[i] %in% c("SHARED3FREE","SHARED3PAY")){
      plans$trip_mode[i] = ifelse(runif(1) < 1/3, "hov3", "hov3_teleportation")
    } else if(plans$trip_mode[i] %in% c("DRIVE_COM","DRIVE_EXP","DRIVE_LOC",
                                        "DRIVE_LRF","DRIVE_HVY")){
      plans$trip_mode[i] = "drive_transit"
    } else if(plans$trip_mode[i] %in% c("WALK_COM","WALK_EXP","WALK_LOC",
                                        "WALK_LRF","WALK_HVY")){
      plans$trip_mode[i] = "walk_transit"
    } else if(plans$trip_mode[i] %in% c("TNC_SINGLE","TAXI")){
      plans$trip_mode[i] = "ride_hail"
    } else if(plans$trip_mode[i] == "TNC_SHARED"){
      plans$trip_mode[i] = "ride_hail_pooled"
    } else if(plans$trip_mode[i] %in% avail_modes){
      plans$trip_mode[i] = plans$trip_mode[i]
    } else{
      plans$trip_mode[i] =
        "we messed up mode conversion (check activitysim to BEAM R script)"
    }
  }
}

write_csv(plans, paste0(wd,"/final_plans.csv"), na = "")

###############################################################

#remove x,y if they exist
if("x" %in% colnames(hh)) hh %<>% select(-x)
if("y" %in% colnames(hh)) hh %<>% select(-y)

hh_coords <- create_hh_coords(hh, parcel, address, taz, crs = 26912)

#add x,y to hh file and write
hh %>%
  left_join(hh_coords, by = "household_id") %>% 
  write_csv(paste0(wd,"/final_households.csv"))

###############################################################

#create vehicles file
nveh <- sum(hh$auto_ownership)
veh_hh <- map2(hh$household_id,
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
plans$trip_mode[plans$trip_mode %in% c("hov2","hov2_teleportation")] %>% table()
plans$trip_mode[plans$trip_mode %in% c("hov3","hov3_teleportation")] %>% table()
plans$trip_mode %>% unique()
