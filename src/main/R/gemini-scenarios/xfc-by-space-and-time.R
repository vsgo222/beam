setwd("/Users/haitam/workspace/scripts/gemini-quarterly-deliverables-2020-04")
source("../helpers.R")
source("../colors.R")
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
library(RColorBrewer)


sessions <- readCsv("sim.xfc.events.csv")
#
sessions[(depot),taz:=-kmeans(sessions[(depot)][,.(x,y)],20)$cluster]

# here we expand each session into the appropriate number of 15-minute bins, so each row here is 1 15-minute slice of a session
toplot <- sessions[,.(chargingType,depot,plug.xfc,taz,kw=c(rep(kw,length(seq(0,duration/60,by=.25))-1),kw*(duration/60-max(seq(0,duration/60,by=.25)))/.25),x,y,duration,hour.bin=start.time.bin + seq(0,duration/60,by=.25)),by='row']
toplot[,site.xfc:=(sum(kw)>=1000),by=c('depot','taz','hour.bin')]
toplot[,xfc:=site.xfc|plug.xfc]
toplot[,fuel:=kw*0.25/3.6e6] # the 0.25 converts avg. power in 15-minutes to kwh, then 3.6e6 converts to Joules

toplot <- toplot[,.(x=x[1],y=y[1],fuel=sum(fuel),kw=sum(kw,na.rm=T),site.xfc=site.xfc[1]),by=c('depot','taz','hour.bin','xfc','chargingType')]
taz <- toplot[,.(x2=mean(x),y2=mean(y)),by='taz']
toplot <- merge(toplot,taz,by='taz')
toplot[,grp:=paste(depot,'-',taz)]

toplot$chargingType2 <- "NO_TAG"
toplot[chargingType=="evi_public_dcfast(150.0|DC)"]$chargingType2 <- "DCFC"
toplot[chargingType=="evi_public_level2(7.2|AC)"]$chargingType2 <- "Public-L2"
toplot[chargingType=="evi_work_level2(7.2|AC)"]$chargingType2 <- "Work-L2"
toplot[chargingType=="fcs_fast(150.0|DC)"]$chargingType2 <- "DCFC"
toplot[chargingType=="fcs_fast(250.0|DC)"]$chargingType2 <- "DCFC"
toplot[chargingType=="homelevel1(1.8|AC)"]$chargingType2 <- "Home-L1"
toplot[chargingType=="homelevel2(7.2|AC)"]$chargingType2 <- "Home-L2"
toplot[,site:=ifelse(depot == T,'depot','public')]
toplot[site=='public'][,.(fuelShare=sum(fuel)/sum(toplot$fuel)),by=.(chargingType2)][order(chargingType2)]
#toplot[,.(fuelShare=sum(fuel)/sum(toplot$fuel)),by=.(chargingType)][order(chargingType)]

######
counties <- data.table(urbnmapr::counties)[county_name%in%c('Alameda County','Contra Costa County','Marin County','Napa County','Santa Clara County','San Francisco County','San Mateo County','Sonoma County','Solano County')]
setkey(toplot,xfc)
toplot[,extreme.lab:=ifelse(kw >= 1000,'>=1MW','<1MW')]
toplot[kw >= 5000]$extreme.lab <- ">=5MW"

######



toplot.colors <- c("#66CCFF", "#669900", "#660099", "#FFCC33", "#CC3300", "#0066CC")
names(toplot.colors) <- c("DCFC", "Public-L2", "Work-L2", "Work-L1", "Home-L2", "Home-L1")
toplot[,hour.bin2:=hour.bin%%25]
toplot[site=='public'][,.(kw=sum(kw)),by=c('chargingType2','hour.bin2')] %>%
  ggplot(aes(x=hour.bin2,y=kw/1e6,fill=factor(chargingType2, levels = names(toplot.colors))))+
  theme_classic() +
  geom_area(colour="black", size=0.3) +
  scale_fill_manual(values = toplot.colors, name = "") +
  labs(x = "hour", y = "GW", fill="load severity")


toplot[,forColor:=paste(toplot$site,extreme.lab, sep=" ")]
toplot[,.(kw=sum(kw)),by=c('forColor','site','hour.bin2')] %>%
  ggplot(aes(x=hour.bin2,y=kw/1e6,fill=forColor))+
  theme_classic() +
  geom_area() +
  scale_fill_manual(values = c(brewer.pal(2, "Reds"), brewer.pal(5, "Blues")), name = "") +
  labs(x = "hour", y = "GW", fill="load severity") +
  facet_wrap(~site)


####
p <- ggplot() +
  geom_polygon(data = counties, mapping = aes(x = long, y = lat, group = group), fill="white", size=.2) +
  coord_map(projection = 'albers', lat0 = 39, lat1 = 45,xlim=c(-122.78,-121.86),ylim=c(37.37,38.17))+
  geom_point(dat=toplot[hour.bin>=5&hour.bin<=22],aes(x=x2,y=y2,size=kw,stroke=0.5,group=grp,colour= extreme.lab),alpha=.3)+
  scale_colour_manual(values=c('darkgrey','orange','red'))+
  scale_size_continuous(range=c(0.5,35),breaks=c(500,1000,2000,4000))+
  labs(title="Hour: {round(frame_time,1)}",colour='Load Severity',size='Charging Site Power (kW)')+
  theme(panel.background = element_rect(fill = "#d4e6f2"))+
  transition_states(grp,transition_length = 1.5,state_length = 1) +
  transition_time(hour.bin2)+enter_fade() + exit_fade()
anim <- animate(p,nframes=18*4*5, fps=10, renderer =gifski_renderer("xfc2.blue.gif"),width=700,height=500,end_pause=10)

dev.off()
options(device = "RStudioGD")