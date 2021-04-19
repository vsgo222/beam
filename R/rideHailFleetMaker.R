# This R code with convert a geojson file of polygons into a 
# a csv table of wkt values

# The following site can be usefull for understanding wkt R stuff
#https://cran.r-project.org/web/packages/wellknown/wellknown.pdf
# ------------------------------------------------------------------- #

library(tidyverse)
library(sf)
library(wellknown)
library(data.table)

#creating a geojson file in QGIS
#-	Layer/Create Layer/New Temporary Scratch Layer
#-	Select Geometry Type – Polygon
#-	Select Coordinate System
#-	Click Add Polygon, Create the Polygon, Make Attributes for it
  #-	Open Attribute Table, Add New Fields (area_id,fleet_id,FLEETSIZE,xcoord,ycoord,vehicleType,rideHailManagerId)
  #-	To create numerous fleets in the same area id, copy and paste the row using the copy and paste commands. 

#-	Repeat for as many polygons as wanted
#-	Right Click/Export/Save Feature as/GeoJSON





#read in geojson file
geojson_file = "R/fleet_areas2.geojson"

#tabulate the geojson file
fleet_org <- st_read(geojson_file)%>%
  select(area_id,fleet_id,FLEETSIZE,xcoord,ycoord,vehicleType,rideHailManagerId,geometry)%>%
  mutate(num = 1:n())

#create wkt text relating to the geometry of the geojson
wkt <- sf_convert(fleet_org) %>% as_tibble() %>% mutate(num = 1:n())

#a table including the wkt polygon
fleet_wkt <- left_join(fleet_org,wkt,by="num")

#create the ridehailfleet table with the number of rows corresponding to fleetsize and then rename columns 
rhfleet <- as.data.frame(lapply(fleet_wkt, rep, fleet_wkt$FLEETSIZE)) %>% as_tibble() %>%
  group_by(area_id,fleet_id) %>%
  mutate(number = 1:n(), 
    id = paste("rideHailVehicle", area_id,fleet_id, number,sep="-"),
    initialLocationX = xcoord, 
    initialLocationY = ycoord,
    shifts = "{10:25200};{25300:80000}",
    geofenceX = NA, geofenceY = NA, geofenceRadius = NA,
    geofencePolygon = value) %>%
  ungroup() %>%
  select(area_id,fleet_id,id,rideHailManagerId,vehicleType,initialLocationX,initialLocationY,
         shifts,geofenceX,geofenceY,geofenceRadius,geofencePolygon)

#organize ridehailfleet table by area id and fleet id
rhfleet_order<- rhfleet[with(rhfleet, order(area_id, fleet_id)), ] %>%
  select(id,rideHailManagerId,vehicleType,initialLocationX,initialLocationY,
         shifts,geofenceX,geofenceY,geofenceRadius,geofencePolygon)

#write the rideHailFleet.csv
write_csv(rhfleet_order,"R/rideHailFleetPolygonsFULL.csv")








# if the geojson is composed of polygon(s) for the SAME vehicle, use option 1
# if the geojson is composed of multiple polygons each for DIFFERENT vehicles, use option 2

#OPTION1: create a single wkt value to store the selected polygon(s) from the geojson 
#if (is.na(taz_wkt[2])) {
#  wkt = polygon(taz, fmt = 16, third = "z")
#} else {
#  wkt = multipolygon(taz, fmt = 16, third = "z")
#}

#OPTION2: put the wkt values in an array to create multiple wkt values to store the selected polygons from the geojson
#wkt <- c()
#for (i in taz_wkt)
#  wkt[i] = i

#Both Options: create a table of the wkt values and export to a csv
#table2 = data.table(wkt)
#table <- tibble::rowid_to_column(table, "IDw")
#write_csv(table,"project_tasks/geofencing/geofences_wkt.csv")


