if(!require(pacman)) install.packages("pacman")
pacman::p_load(
  char = as.character(read.csv("R/packages.csv", header = F)[,1])
)
install_github("atchley-sha/R-packageSHA")
library(packageSHA)

###########################################################

#Set shapefile paths
shapefiles <- c(
  "GIS/Zones/SLCSouth/SLCSouth_polygon.shp",
  "GIS/Zones/WestCity/WestCity_polygon.shp",
  "GIS/Zones/WestJordan/WestJordan_polygon.shp"
)

#Set area names
areaNames <- c(
  "SLCSouth",
  "WestCity",
  "WestJordan"
)

#Set fleet sizes
sizes <- c(
  12
)

#Set shifts
shifts <- c(
  "{14400:87300}"
)

#Set vehicle types
vehicleTypes <- c(
  "micro"
)

#Set ridehail manager
rideHailManagerIds <- c(
  "GlobalRHM"
)

#Set coordinate system to convert to (epsg #)
#(4326 = WGS84, 26912 = UTM12N)
toCoord <- 26912

#Set output directory
fleetDir <- "test/input/ridehail_fleets"

###########################################################

n <- length(shapefiles)

#read shapefiles
sfs <- list()
for(i in 1:n){
  sfs[[i]] <- 
    read_sf(shapefiles[i]) %>% 
    st_transform(toCoord)
}

#convert to wkt
wkt <- vector()
for(i in 1:n){
  wkt[i] <- 
    sfs[[i]] %>% 
    sf_convert()
}

#find centroids
centroids <- tibble()
for(i in 1:n){
  centroids[i,1] <- 
    sfs[[i]] %>% 
    st_centroid() %>% 
    as_tibble() %>% 
    select(geometry) 
}
centroids %<>% 
  separate(
    col = geometry,
    into = c("initialLocationX",
             "initialLocationY"),
    sep = " "
    )
centroids$initialLocationX %<>%
  str_replace_all("c\\((\\d+\\.\\d+),", "\\1") %>% 
  as.numeric()
centroids$initialLocationY %<>%
  str_replace_all("(\\d+\\.\\d+)\\)", "\\1") %>% 
  as.numeric()

#create fleet tibble
fleet <- tibble(
  geofencePolygon = wkt,
  size = sizes,
  shifts = shifts,
  initialLocationX = centroids$initialLocationX,
  initialLocationY = centroids$initialLocationY,
  vehicleType = vehicleTypes,
  rideHailManagerId = rideHailManagerIds,
  fleetId = areaNames
) %>% 
  uncount(size) %>% 
  mutate(id = 1:n()) %>% 
  relocate(
    id,
    fleetId,
    rideHailManagerId,
    vehicleType,
    initialLocationX,
    initialLocationY,
    shifts,
    geofencePolygon
    )

#write fleet csv
fleetName <- paste0(areaNames, collapse = "_")
write_csv(fleet, paste0(fleetDir, "/", fleetName, ".csv"))
