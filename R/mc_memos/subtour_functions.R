

# not in function
'%!in%' <- function(x,y)!('%in%'(x,y))

#---------------------------------------------------------------------------#
# This function will find and assign the trips that act as the beginning
# or end of a subtour. It filters out "subtours" that are two trips back
# back. It will only get subtours for specific purpose.
get_purpose_subtours <- function(asim_final_trips, purp){
  
  asim_subtable <- asim_final_trips %>%
    
    # find duplicated purposes in the same taz
    group_by(person_id, tour_id) %>%
    mutate(act = paste0(purpose,destination)) %>%
    mutate(sub1 = ifelse(purpose == purp, duplicated(act), FALSE)) %>%
    ungroup() %>% 
  
    # flip table and find the original purposes where dublicates come from
    arrange(person_id,desc(trip_id)) %>% 
    group_by(person_id,tour_id) %>%
    mutate(sub2 = ifelse(purpose == purp, duplicated(act), FALSE)) %>%
    # filter out embedded subtours
    mutate(sub1 = ifelse(duplicated(sub1),FALSE,sub1)) %>% 
    ungroup() %>%
  
    # classify beginning and end of subtours
    arrange(person_id,trip_id) %>% 
    group_by(person_id,tour_id) %>%
    # filter out embedded subtours
    mutate(sub2 = ifelse(duplicated(sub2),FALSE,sub2)) %>% 
    ungroup() %>%
    # define the start and end of a subtour
    mutate(sub = ifelse(sub1 == TRUE, "endsub", ifelse(sub2 == TRUE, "startsub", "none"))) %>%
    arrange(person_id,tour_id,trip_id) %>%
  
    # delete "subtours" that are simply just two of the same purposes in a row
    mutate(sub = ifelse(sub == "startsub" & lead(sub) == "endsub", "none", ifelse(sub== "endsub" & lag(sub) == "startsub", "none", sub))) %>%
    filter(person_id %in% c(61,133,1284, 6738, 28819))
  
  # fill in the gaps between the beginning and end of each subtour
  for(i in 1:length(asim_subtable$sub)){
    asim_subtable$sub[i] <- 
      ifelse(i == 0, asim_subtable$sub[i], 
             ifelse(asim_subtable$sub[i-1] %in% c("startsub","midsub") & 
                      asim_subtable$sub[i] %!in% c("startsub","endsub"), 
                    "midsub", 
                    asim_subtable$sub[i]
             )
      )  
  }
  
  # techincally the beginsub is the trip before the beginning of a subtour
  asim_subtable %>%
    select(-sub1, -sub2) %>%
    mutate(purpy = ifelse(sub %in% c("endsub", "midsub"),
                        purp, "none"))

}  



