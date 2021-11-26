if(!require(pacman)) install.packages("pacman")
pacman::p_load(
  char = as.character(read.csv("R/packages.csv", header = F)[,1])
)
install_github("atchley-sha/R-packageSHA")
library(packageSHA)

###################################

#Set paths
shapefiles <- c(
  "GIS/Zones/SLCSouth/SLCSouth_polygon.shp",
  "GIS/Zones/WestCity/WestCity_polygon.shp"
)

#Set fleet sizes
sizes <- c(
  12,
  12
)

#Set shifts
shifts <- c(
  "{10:25200};{25300:80000}"
)

#Set vehicle types
vehicleTypes <- c(
  "micro"
)

#Set ridehail manager
rideHailManagerIds <- c(
  "GlobalRHM"
)

#Set coordinate system to convert to
#(4326 = WGS84, 26912 = UTM12N)
toCoord <- "epsg:26912"

####################################

#read shapefiles
sfs <- list()
n <- length(shapefiles)

for(i in 1:n){
  sfs[[i]] <- 
    read_sf(shapefiles[i])
}

