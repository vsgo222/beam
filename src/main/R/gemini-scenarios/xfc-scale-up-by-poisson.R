# Title     : XFC Scale up
# Objective : Scaling up XFC events using Poisson process
# Created by: haitam
# Created on: 6/20/20

setwd("/Users/haitam/workspace/scripts/gemini-quarterly-deliverables-2020-04")
source("../helpers.R")
library(dplyr)
library(ggplot2)
library(sf)
library(proj4)
library(stringr)
library(data.table)

expansion.factor <- (7.75/0.315) * 27.0 / 21.3
events.file <- "baseline-exp1-2040-2.events.csv.gz"
#
time.bins <- data.table(time=seq(0,40,by=0.25)*3600,quarter.hour=seq(0,40,by=0.25))
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

##
events <- readCsv(events.file)
ev <- events[type %in% c("RefuelSessionEvent")][
  ,c("vehicle", "time", "type", "parkingTaz", "chargingType", "parkingType", "locationY", "locationX",
     "primaryFuelLevel", "secondaryFuelLevel", "fuel", "duration", "vehicleType")]
ev[,kw:=unlist(lapply(str_split(as.character(chargingType),'\\('),function(ll){ as.numeric(str_split(ll[2],'\\|')[[1]][1])}))]
ev[,depot:=(substr(vehicle,0,5)=='rideH' & substr(vehicleType,0,5)=='ev-L5')]
ev[,plug.xfc:=(kw>=250)]

sessions <- ev[chargingType!='None' & time/3600>=4 & time/3600<=22,.(depot,plug.xfc,taz=parkingTaz,kw,x=locationX,y=locationY,duration=duration/60,start.time=time-duration)]
sessions[,row:=1:.N]
start.time.dt <- data.table(time=sessions$start.time)
sessions[,start.time.bin:=time.bins[start.time.dt,on=c(time="time"),roll='nearest']$quarter.hour]
sessions[,taz:=as.numeric(as.character(taz))]

sim.events <- data.table()
for (bin in seq(min(sessions$start.time.bin),max(sessions$start.time.bin),by=0.25))
  {
  sessions.bin <- sessions[start.time.bin == bin]
  sim.events <- rbind(sim.events, scaleUP(sessions.bin, bin*3600, expansion.factor))
}
sim.events$start.time <- sim.events$start.time2
sim.events$row <- paste(sim.events$row2,sim.events$row,sep="-")
sim.events <- sim.events[,-c("row2","start.time2")]

#sim.events <- sessions
#
write.csv(
  sim.events,
  file = "sim.xfc.events.csv",
  row.names=FALSE,
  quote=FALSE,
  na="0")



