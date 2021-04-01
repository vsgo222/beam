# convert the taz centers coordinates to epsg:26912
library(tidyverse)
library(sf)

taz <- read_csv("test/input_new/small_mix/taz-centers.csv") %>% 
  transmute(taz, x = `coord-x`, y = `coord-y`)

sf <- taz %>%
  rowwise() %>%
  mutate(geometry = st_geometry(st_point(c(x,y)))) %>%
  select(-x, -y) %>%
  ungroup()

st_as_sf(taz, coords = c("x","y")) 

st_as_sf(sf %>% as.data.frame(), sf_column_name = "geometry")

st_transform(sf$geometry, crs = st_crs("26912"))
sf %>% st_transform(geometry, crs = 26912)

sf %>% st_set_crs(4326) %>% st_transform(crs=26912)

sf %>% st_point()
%>% st_sfc(crs = 4326)




st_as_sf(sf %>% as.data.frame(), sf_column_name = "geometry") %>% 
  st_set_crs(4326) %>% 
  st_transform(crs=26912) %>%
  as_tibble()