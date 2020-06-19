# Title     : TOD
# Objective : TODO
# Created by: haitam
# Created on: 6/19/20

library(stringr)
library(tidyverse)
library(urbnmapr)
library(gganimate)
library(magick)
library(OpenStreetMap)
library(data.table)
#library(urbnthemes)
#library(cepp)
library(ggplot2)
library(dplyr)
library(sf)
library(proj4)
setwd("/Users/haitam/workspace/scripts")
source("helpers.R")

#26910
#4326
tazsss <- readCsv("common-data/sfbay-tazs/taz-centers.csv")
tazs <- st_as_sf(tazsss, coords = c("coord-x", "coord-y"), crs = 26910)
tazs_wgs84 <- sf::st_transform(tazs, 4326)
depots <- readCsv("gemini-quarterly-deliverables-2020-04/depot_parking_power_baseline.csv")[,vehicle:="cav"]
stations_temp0 <- rbind(depots) %>%
  rowwise() %>%
  mutate(capacity=as.numeric(unlist(strsplit(unlist(strsplit(chargingType, "\\("))[2], "\\|"))[1]),
         current=unlist(strsplit(unlist(strsplit(unlist(strsplit(chargingType, "\\("))[2], "\\|"))[2], "\\)"))) %>%
  filter(!is.na(capacity)) %>%
  mutate(totCapacity = numStalls*capacity)
stations <- merge(stations_temp0, tazsss, by = "taz")
events <- readCsv("gemini-quarterly-deliverables-2020-04/baseline-exp1-2040-2.events.csv.gz")
events_charging <- events[type %in% c("RefuelSessionEvent")]
ev <- events_charging[
  ,c("vehicle", "time", "type", "parkingTaz", "chargingType", "parkingType", "locationY", "locationX",
     "primaryFuelLevel", "secondaryFuelLevel", "fuel", "duration", "vehicleType")]

ev[type=='RefuelSessionEvent',kw:=unlist(lapply(str_split(as.character(chargingType),'\\('),function(ll){ as.numeric(str_split(ll[2],'\\|')[[1]][1])}))]
ev[,depot:=(substr(vehicle,0,5)=='rideH' & substr(vehicleType,0,5)=='ev-L5')]
ev[,plug.xfc:=(kw>=250)]
#ev[substr(vehicle,0,5)=='rideH' & substr(vehicleType,0,5)=='ev-L5']
#length(unique(ev[substr(vehicleType,0,3)=='ev-' & substr(vehicle,0,5)=='rideH']$vehicle))
#length(unique(ev[grepl("ev-L1",vehicleType) & substr(vehicle,0,5)=='rideH']$vehicle))

phev <- length(unique(ev[substr(vehicleType,0,5)=='phev-']$vehicle))
bev <- length(unique(ev[substr(vehicleType,0,3)=='ev-']$vehicle))
rh <- length(unique(ev[substr(vehicleType,0,3)=='ev-' & substr(vehicle,0,5)=='rideH']$vehicle))
cavrh <- length(unique(ev[substr(vehicleType,0,5)=='ev-L5' & substr(vehicle,0,5)=='rideH']$vehicle))
nonev <- length(unique(ev[grepl('ev-', vehicleType)]$vehicle))
#(phev+bev)/(phev+bev+nonev)

bev <- length(unique(ev$vehicle)) - length(unique(ev[substr(vehicleType,0,5)=='phev-']$vehicle))

# focus in on just the important columns
time.bins <- data.table(time=seq(0,40,by=0.25)*3600,quarter.hour=seq(0,40,by=0.25))

sessions <- ev[type=='RefuelSessionEvent' & chargingType!='None' & time/3600>=4 & time/3600<=22,.(depot,plug.xfc,taz=parkingTaz,kw,x=locationX,y=locationY,duration=duration/60,start.time=time-duration)]
sessions[,row:=1:.N]
start.time.dt <- data.table(time=sessions$start.time)
sessions[,start.time.bin:=time.bins[start.time.dt,on=c(time="time"),roll='nearest']$quarter.hour]
sessions[,taz:=as.numeric(as.character(taz))]

sessions[(depot),taz:=-kmeans(sessions[(depot)][,.(x,y)],20)$cluster]

# here we expand each session into the appropriate number of 15-minute bins, so each row here is 1 15-minute slice of a session
toplot <- sessions[,.(depot,plug.xfc,taz,kw=c(rep(kw,length(seq(0,duration/60,by=.25))-1),kw*(duration/60-max(seq(0,duration/60,by=.25)))/.25),x,y,duration,hour.bin=start.time.bin + seq(0,duration/60,by=.25)),by='row']
toplot[,site.xfc:=(sum(kw)>=1000),by=c('depot','taz','hour.bin')]
toplot[,xfc:=site.xfc|plug.xfc]
toplot[,fuel:=kw*0.25/3.6e6] # the 0.25 converts avg. power in 15-minutes to kwh, then 3.6e6 converts to Joules

toplot <- toplot[,.(x=x[1],y=y[1],fuel=sum(fuel),kw=sum(kw,na.rm=T),site.xfc=site.xfc[1]),by=c('depot','taz','hour.bin','xfc')]
taz <- toplot[,.(x2=mean(x),y2=mean(y)),by='taz']
toplot <- merge(toplot,taz,by='taz')
toplot[,grp:=paste(depot,'-',taz)]

counties <- data.table(urbnmapr::counties)[county_name%in%c('Alameda County','Contra Costa County','Marin County','Napa County','Santa Clara County','San Francisco County','San Mateo County','Sonoma County','Solano County')]
setkey(toplot,xfc)
toplot[,extreme.lab:=ifelse(xfc,'>=1MW','<1MW')]

p <- ggplot() +
  geom_polygon(data = counties, mapping = aes(x = long, y = lat, group = group), fill="white", size=.2) +
  coord_map(projection = 'albers', lat0 = 39, lat1 = 45,xlim=c(-122.78,-121.86),ylim=c(37.37,38.17))+
  geom_point(dat=toplot,aes(x=x2,y=y2,size=kw,stroke=0.5,group=grp,colour= extreme.lab),alpha=.3)+
  scale_colour_manual(values=c('darkgrey','red'))+
  scale_size_continuous(range=c(0.5,35),breaks=c(500,1000,2000,4000))+
  labs(title="Hour: {round(frame_time,1)}",colour='Load Severity',size='Charging Site Power (kW)')+
  theme(panel.background = element_rect(fill = "#d4e6f2"))+
  transition_states(grp,transition_length = 1.5,state_length = 1) +
  transition_time(hour.bin)+enter_fade() + exit_fade()
anim <- animate(p,nframes=18*4*5, fps=10, renderer =gifski_renderer("gemini-quarterly-deliverables-2020-04/xfc.blue.gif"),width=700,height=500,end_pause=10)

ggplot(toplot[,.(kw=sum(kw)),by=c('xfc','hour.bin')],aes(x=hour.bin,y=kw,fill=xfc))+geom_area()+theme_bw()

toplot %>%
  group_by(extreme) %>%
  summarise(fuel = sum(fuel))

toplot %>%
  group_by(kw) %>%
  summarise(fuel = sum(fuel)) %>%
  ungroup() %>%
  ggplot(aes(as.character(kw), fuel)) +
  geom_bar(stat = "identity")

toplot %>%
  group_by(extreme.lab, hour.bin) %>%
  summarise(fuel = sum(fuel)) %>%
  ungroup() %>%
  ggplot(aes(hour.bin, fuel/1000, colour=extreme.lab)) +
  geom_line() +
  theme_classic() +
  scale_colour_manual(values=c('darkgrey','red'))+
  labs(x = "hour", y = "charging demand (MWh)", colour="load severity")

dev.off()
options(device = "RStudioGD")