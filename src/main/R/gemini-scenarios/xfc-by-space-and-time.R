# Title     : XFC Space-Time
# Objective : Plotting XFC events
# Created by: haitam
# Created on: 6/20/20

setwd("/Users/haitam/workspace/scripts/gemini-quarterly-deliverables-2020-04")
source("../helpers.R")
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
library(scales)

sessions <- readCsv("sim.xfc.events.csv")
#
#sessions[(depot),taz:=-kmeans(sessions[(depot)][,.(x,y)],20)$cluster]

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
anim <- animate(p,nframes=18*4*5, fps=10, renderer =gifski_renderer("xfc.blue.gif"),width=700,height=500,end_pause=10)

toplot[,.(kw=sum(kw)),by=c('extreme.lab','hour.bin')] %>%
  ggplot(aes(x=hour.bin,y=kw/1000,fill=extreme.lab))+
  theme_classic() +
  geom_area()+
  scale_fill_manual(breaks = c(">=1MW","<1MW"), values=c(hue_pal()(4)[1], hue_pal()(4)[3])) +
  labs(x = "hour", y = "MW", fill="load severity")

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
  ggplot(aes(hour.bin, fuel*3.6e6/1000, colour=extreme.lab)) +
  geom_line() +
  theme_classic() +
  scale_colour_manual(values=c('darkgrey','red'))+
  labs(x = "hour", y = "charging demand (MWh)", colour="load severity")

dev.off()
options(device = "RStudioGD")