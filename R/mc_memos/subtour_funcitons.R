
get_start_and_end_subs <- function(asim_final_trips) %>% 
  group_by(person_id, tour_id) %>%
  mutate(act = paste0(purpose,destination)) %>%
  mutate(sub1 = duplicated(act)) %>%
  ungroup() %>% 
  
  arrange(person_id,desc(trip_id)) %>% 
  group_by(person_id,tour_id) %>%
  mutate(sub2 = duplicated(act)) %>%
  # we only want the longest subtour (no imbedded subtours)
  mutate(sub1 = ifelse(duplicated(sub1),FALSE,sub1)) %>% 
  ungroup() %>%
  
  arrange(person_id,trip_id) %>% 
  group_by(person_id,tour_id) %>%
  # we only want the longest subtour (no imbedded subtours)
  mutate(sub2 = ifelse(duplicated(sub2),FALSE,sub2)) %>% 
  ungroup() %>%
  # define the start and end of a subtour
  mutate(sub = ifelse(sub1 == TRUE, "endsub", ifelse(sub2 == TRUE, "startsub", "none"))) %>%
  arrange(person_id,tour_id,trip_id) %>%
  
  mutate(sub = ifelse(sub == "startsub" & lead(sub) == "endsub", "none", ifelse(sub== "endsub" & lag(sub) == "startsub", "none", sub))) %>%
  filter(person_id %in% c(61,133,1284, 6738, 28819))

