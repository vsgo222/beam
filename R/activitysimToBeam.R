library(tidyverse)
library(magrittr)

#############################################################
# set wd
wd <- "test/input/csv_test/data"

# Read in csvs
persons <- read_csv(paste0(wd,"/final_persons.csv"))
hh <- read_csv(paste0(wd,"/final_households.csv"))
plans <- read_csv(paste0(wd,"/final_plans.csv"))

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
      plans$trip_mode[i] = ifelse(runif(1) < 0.5, "hov2", "hov2_teleportation")
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
#get person home x,y
person_home <- plans %>% 
  filter(ActivityType == "Home", PlanElementIndex == 1) %>% 
  select(person_id, x, y)

#get hh x,y
hh_home <- persons %>%
  left_join(person_home, by = "person_id") %>% 
  filter(PNUM == 1) %>% 
  select(household_id, x, y)

#add x,y to hh file and write
hh %>%
  left_join(hh_home, by = "household_id") %>% 
  write_csv(paste0(wd,"/final_households.csv"), na = "0")

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
