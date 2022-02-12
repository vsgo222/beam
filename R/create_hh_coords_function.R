library(tidyverse)
library(magrittr)
library(sf)

create_hh_coords <- function(house, parcel, adderss, taz, crs){
  #set up files
  colnames(parcel)[1] <- "TAZ"
  
  address %<>%
    filter(PtType == "Residential")
  colnames(address)[1] <- "TAZ"
  
  taz %<>%
    arrange(TAZID) %>%
    filter(CO_FIPS > 0) %>%
    mutate(TAZ = TAZID)
  
  #update the parcel dataset to fill in any TAZs that don't have coordinate information
  parcel <- left_join(parcel,taz,by = "TAZ") %>%
    mutate(
      xcoord = ifelse(is.na(xcoord.x), xcoord.y, xcoord.x),
      ycoord = ifelse(is.na(ycoord.x), ycoord.y, ycoord.x),
      CO_FIPS = CO_FIPS.x
    ) %>%
    select(TAZ,CO_FIPS,PARCEL_ID,OWN_TYPE,xcoord,ycoord)
  
  
  
  #determine the total number of households per TAZ
  tothh <- house %>%
    arrange(TAZ) %>% mutate(tot_hh = 1) %>% select(TAZ, tot_hh)%>%
    group_by(TAZ) %>% summarize_all("sum")
  p <- data.frame(TAZ = (1:2881)) #total number of TAZ
  
  #display TAZs with no households as the value of 0
  tothh <- left_join(p, tothh, by = "TAZ") 
  tothh[is.na(tothh)] <- 0
  
  #display all TAZs, so that the loop in the next step doesn't break
  address <- left_join(p,address, by = "TAZ")
  address[is.na(address)] <- 0
  parcel <- left_join(p,parcel, by = "TAZ")
  parcel[is.na(parcel)] <- 0
  
  #create a table that has the third column to the tothh table
  col3 <- address %>%
    select(TAZ, CO_FIPS)%>%
    group_by(TAZ) %>% summarize_all("sum") %>%
    filter(CO_FIPS == 0)
  
  #add the CO_FIPS sum to the tothh table (displays a 0 where no address coordinates exist; allows the loop to get coordinates from parcel dataset instead)
  tothh <- left_join(tothh,col3, by = "TAZ") %>% select(TAZ,tot_hh,CO_FIPS)
  tothh[is.na(tothh)] <- 1
  
  
  
  #separate the address point data table into a set of vectors for each TAZ
  address <- address %>% arrange(TAZ) %>% select(TAZ, xcoord, ycoord)
  splitt <- split(address,address$TAZ)
  
  #separate the parcel data table into a set of vectors for each TAZ
  parcel <- parcel %>% arrange(TAZ) %>% select(TAZ, xcoord, ycoord)
  splitt2 <- split(parcel,parcel$TAZ)
  
  #a loop that determines the coordinates for each TAZ based on the number of households per TAZ; use of the sample function; a joint table is continually updated
  i = 1
  while (i <= 2881){
    n = tothh[i,2]
    m = tothh[i,3]
    testsam <- sample_n(splitt[[i]], n, replace = TRUE)
    testsam2 <- sample_n(splitt2[[i]], n, replace = TRUE)
    ifelse(i == 1, join <- sample_n(splitt[[1]], n, replace = TRUE), 
           ifelse(n != 0 & m == 0,join <- rbind(join, testsam2), join <- rbind(join,           testsam)))
    i = i + 1
  }
  colnames(join)[2] <- "longitude"
  colnames(join)[3] <- "latitude"
  
  
  #organize the household dataset by numbering each TAZ for each household using Index
  house2 <- house %>%
    arrange(TAZ) %>% 
    mutate(TAZ = as.character(TAZ)) %>% group_by(TAZ) %>%
    mutate(Index = 1:n(), index = as.character(Index), tazcom = paste(TAZ, index))
  
  #organize the address point dataset by numbering each TAZ for each address point using Index
  address2 <- join %>%
    mutate(TAZ = as.character(TAZ)) %>% group_by(TAZ) %>%
    mutate(Index = 1:n(), index = as.character(Index), tazcom = paste(TAZ, index)) %>%
    select(tazcom, longitude, latitude)
  
  #join the address point and house tables by the indexing of the TAZ, thus assigning coordinates to the households
  hhcoord <- left_join(house2, address2, by = "tazcom") %>%
    arrange(householdId)
  colnames(hhcoord)[2] <- "TAZ"
  
  #the coordinate table
  hhcoord %<>%
    select(householdId,TAZ,longitude,latitude) %>% 
    st_as_sf(coords = c("longitude", "latitude")) %>% 
    `st_crs<-`(4326) %>% #WGS84
    st_transform(crs) %>% #specified crs
    {mutate(.,
            x = unlist(map(.$geometry,1)),
            y = unlist(map(.$geometry,2)))} %>% 
    as_tibble() %>%
    select(-geometry, -TAZ)
  
  hhcoord
}