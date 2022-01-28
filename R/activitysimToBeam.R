library(tidyverse)
library(magrittr)

# set wd
wd <- "test/input/csv_test/data"

# Read in csvs
persons <- read_csv(paste0(wd,"/final_persons.csv"))
hh <- read_csv(paste0(wd,"/final_households.csv"))
plans <- read_csv(paste0(wd,"/final_plans.csv"))

#rewrite plans
plans$person_id %<>% as.integer()
write_csv(plans, paste0(wd,"/final_plans.csv"), na = "")

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
