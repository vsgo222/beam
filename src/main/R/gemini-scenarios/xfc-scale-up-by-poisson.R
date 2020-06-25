setwd("/Users/haitam/workspace/scripts/gemini-quarterly-deliverables-2020-04")
source("../helpers.R")
library(dplyr)
library(ggplot2)
library(sf)
library(proj4)
library(stringr)
library(data.table)

expansion.factor <- (7.75/0.315) * 27.0 / 21.3
events.file <- "full2.0.events.csv.gz"

#
time.bins <- data.table(time=seq(0,61,by=0.25)*3600,quarter.hour=seq(0,61,by=0.25))
#
nextTimePoisson <- function(rate) {
  return(-log(1.0 - runif(1)) / rate)
}
#
scaleUP <- function(DT, t, factor) {
  nb <- nrow(DT)
  nb.scaled <- nb*factor
  rate <- nb.scaled/0.25
  DT.temp1 <- data.table(start.time2=round(t+cumsum(unlist(lapply(rep(rate, nb.scaled), nextTimePoisson)))*3600))[order(start.time2),]
  DT.temp2 <- DT[sample(.N,nrow(DT.temp1),replace=T)][order(start.time)]
  DT.temp1[,row2:=1:.N]
  DT.temp2[,row2:=1:.N]
  return(DT.temp1[DT.temp2, on="row2"])
}

## parking
# parking <- readCsv("gemini_taz_parking_plugs_1.125_power_150.csv")
# tazs <- st_as_sf(readCsv("../common-data/sfbay-tazs/taz-centers.csv"), coords = c("coord-x", "coord-y"), crs = 26910)
# tazs_wgs84 <- sf::st_transform(tazs, 4326)
# tazs_wgs84 <- as.data.table(cbind(tazs_wgs84, st_coordinates(st_centroid(tazs_wgs84))))
# parking.tazs <- parking[tazs_wgs84, on = "taz"]
#sum.stalls <- sum(parking[chargingType!="NoCharger"]$numStalls)
#parking[chargingType!="NoCharger",.(stalls=sum(numStalls)/sum.stalls,feeInCents=median(feeInCents)),by=.(chargingType,parkingType)]
##
events <- readCsv(events.file)
#test <- ev[chargingType=="evi_public_dcfast(150.0|DC)"&start.time>=30600&start.time<=34200]
#####
ev1 <- events[type %in% c("RefuelSessionEvent")][
  ,c("vehicle", "time", "type", "parkingTaz", "chargingType", "parkingType", "locationY", "locationX", "duration", "vehicleType")][
  order(time),`:=`(IDX = 1:.N),by=vehicle]
ev2 <- events[type %in% c("ChargingPlugInEvent")][,c("vehicle", "time")][order(time),`:=`(IDX = 1:.N),by=vehicle]
setnames(ev2, "time", "start.time")
ev <- ev1[ev2, on=c("vehicle", "IDX")]
ev[,kw:=unlist(lapply(str_split(as.character(chargingType),'\\('),function(ll){ as.numeric(str_split(ll[2],'\\|')[[1]][1])}))]
ev[,depot:=(substr(vehicle,0,5)=='rideH' & substr(vehicleType,0,5)=='ev-L5')]
ev[,plug.xfc:=(kw>=250)]

sessions <- ev[chargingType!='None' & time/3600>=4,.(start.time,depot,plug.xfc,taz=parkingTaz,kw,x=locationX,y=locationY,duration=duration/60,chargingType)]
sessions[,row:=1:.N]
start.time.dt <- data.table(time=sessions$start.time)
sessions[,start.time.bin:=time.bins[start.time.dt,on=c(time="time"),roll='nearest']$quarter.hour]
sessions[,taz:=as.numeric(as.character(taz))]
# write.csv(
#   sessions,
#   file = "sim.xfc.events.csv",
#   row.names=FALSE,
#   quote=FALSE,
#   na="0")


sim.events <- data.table()
for (bin in seq(min(sessions$start.time.bin),max(sessions$start.time.bin),by=0.25))
  {
  sessions.bin <- sessions[start.time.bin == bin]
  sim.events <- rbind(sim.events, scaleUP(sessions.bin, bin*3600, expansion.factor))
}
sim.events$start.time <- sim.events$start.time2
sim.events$row <- paste(sim.events$row2,sim.events$row,sep="-")
sim.events <- sim.events[,-c("row2","start.time2")]
write.csv(
  sim.events,
  file = "sim.xfc.events.csv",
  row.names=FALSE,
  quote=FALSE,
  na="0")



