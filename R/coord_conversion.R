# convert the taz centers coordinates to epsg:26912
library(tidyverse)
library(sf)

taz <- read_csv("test/input_new/small_mix/taz-centers.csv") %>% 
  mutate(x = `coord-x`, y = `coord-y`)

sf <- taz %>%
  rowwise() %>%
  mutate(geometry = st_geometry(st_point(c(x,y)))) %>%
  select(-x, -y) %>%
  ungroup()

st_as_sf(taz, coords = c("x","y")) 



finally <- st_as_sf(sf %>% as.data.frame(), sf_column_name = "geometry") %>% 
  st_set_crs(4326) %>% 
  st_transform(crs=26912) %>%
  st_coordinates() %>%
  as_tibble() %>% 
  bind_cols(taz) %>%
  transmute(
    taz,
    `coord-x` = X,
    `coord-y` = Y
  )

write_csv(finally, "test/input_new/small_mix/taz-centers_converted.csv")