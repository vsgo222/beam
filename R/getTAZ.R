library(tidyverse)
library(magrittr)
library(sf)

##################################

#Read in plans and TAZ
plans <- read_csv("data/wfrc_100k/plans.csv")
taz <- read_sf("GIS/SLC_TAZ/shapefiles/TAZ/TAZ_ALL.shp")

##################################

#select needed TAZ columns and convert to UTM12N
taz %<>%
  select(TAZID, geometry) %>% 
  arrange(TAZID) %>% 
  st_transform(26912) %>% 
  `rownames<-`(taz$TAZID)

#convert plans to sf object
planss <- plans %>% 
  filter(!is.na(activityLocationX)) %>% 
  st_as_sf(coords = c("activityLocationX", "activityLocationY")) %>% 
  `st_crs<-`(26912)

#intersect coords with TAZ
tazz <- planss %>% 
  st_intersects(taz)

#join intersection to planss
planss <- tibble(planss, TAZ = as.integer(tazz)) %>% 
  select(personId, planElementIndex, planIndex, TAZ)

#combine with original plans
tazPlans <- left_join(plans, planss, by = c("personId", "planIndex", "planElementIndex"))
