

asim_table <- function(asim_tour_coeffs,purpose){
  asim1<- asim_tour_coeffs %>% select(Variable, {{purpose}}) %>% 
    slice(1:9,14:19) %>%
    mutate (
      term = case_when(grepl("ivt",Variable) ~ "ivtt", TRUE ~ Variable)
    ) %>%
    select(term,{{purpose}}) %>% 
    group_by(term) %>%
    summarize(estimate = {{purpose}}, ave:= mean({{purpose}}), std.error = sd({{purpose}})) %>%
    mutate(
      estimate := case_when(term == "ivtt" ~ ave,TRUE ~ estimate)
    ) %>% 
    distinct() %>% 
    select(-ave)
}

utah_table <- function(utah_coeff,purp,ovtt_shortwalk,ovtt_longwalk,divider){
  utah_coeff %>% filter(purpose == purp) %>%
    mutate(
      estimate = value/divider,
      term = case_when( 
        variable == "cost_coef" ~ list("cost"),
        grepl("ivt",variable) ~ list("ivtt"),
        grepl("walk_coef_1",variable) ~ list(ovtt_shortwalk),
        grepl("walk_coef_gt_1",variable) ~ list(ovtt_longwalk))
    ) %>%
    unnest(term) %>%
    filter(term != list("none")) %>%
    select(term,estimate)
}

wfrc_table <- function(wfrc_coeff,purp,ovt_long,divider){
  wfrc_coeff %>% filter(purpose == purp) %>%
    mutate(
      estimate = value/divider,
      term = case_when( 
        grepl("ivt_coef",variable) ~ list("ivtt"),
        grepl("initwait",variable)~list("wait_time_under_10_min"),
        grepl("xferwait",variable)~list("transfer_time"),
        grepl("drive_coef",variable)~list("egress_time"),
        grepl("walk_coef_1",variable) ~ list("walk_short_dist"),
        grepl("walk_coef_gt_1",variable) ~ list(wfrc_ovt_long),
        grepl("transfers_coef_drive",variable) ~ list("transfer_number_drive_transit"),
        grepl("transfers_coef_walk",variable)~ list("transfer_number_walk_transit"),
        grepl("cost",variable) ~ list("cost"),
        TRUE ~ list("none"))
    ) %>%
    unnest(term) %>%
    filter(term != list("none")) %>%
    group_by(term) %>%
    summarize(std.error = sd(estimate),estimate = mean(estimate)) %>%
    select(term,estimate,std.error)
}

nchrp_table <- function(nchrp_coeff,purp,nchrp_ovt_long,divider){
  nchrp_coeff %>% filter(purpose == purp) %>%
    mutate(
      estimate = value/divider,
      term = case_when( 
        grepl("ivt",variable) ~ list("ivtt"),
        grepl("initwait",variable)~list("wait_time_under_10_min"),
        grepl("xferwait",variable)~list("transfer_time"),
        grepl("ovt",variable)~list(nchrp_ovt_long),
        grepl("cost",variable) ~ list("cost"),
        TRUE ~ list("none"))
    ) %>%
    unnest(term) %>%
    filter(term != list("none")) %>%
    group_by(term) %>%
    summarize(std.error = sd(estimate),estimate = mean(estimate)) %>%
    select(term,estimate,std.error)
}

wgrapher <- function(purp,asim,utah,wfrc,nchrp){
  list(
  "ActivitySim" = asim,
  "Utah Statewide" = utah,
  "WFRC 2019" = wfrc,
  "NCHRP Report716" = nchrp
) %>%
  bind_rows(.id = "model") %>%
  dwplot() + 
  ggtitle(paste0(purp," Utility Parameter Values")) +
  xlab("Coefficient Value") + ylab("Path Variables in BEAM") + 
  coord_flip() + ggpubr::rotate_x_text() 
}

neg_log_trans = function() trans_new("neg_log", function(x) log(abs(x)), function(x) log(abs(x)))

loggrapher <- function(purp,asim,utah,wfrc,nchrp){ 
  list(
    "ActivitySim" = asim,
    "Utah Statewide" = utah,
    "WFRC 2019" = wfrc,
    "NCHRP Report716" = nchrp
  ) %>%
  bind_rows(.id = "model") %>%
  dwplot(dot_args = list(size=2)) + 
  ggtitle(paste0(purp," Utility Parameter Values")) +
  xlab("Abs(Coefficient Value)") + ylab("Path Variables in BEAM") +
  coord_trans(x="neg_log") + 
  theme_bw() +
  annotation_logticks(scaled = FALSE, alpha = .3,short = unit(15000,"mm"), colour = "gray")
}
#coord_flip() + ggpubr::rotate_x_text() +
#scale_x_continuous(trans = weird, breaks = breaks_log(8)(c(0.1,1))) +
#scale_x_log10(breaks = breaks_log(5)) +


ivtt_ratio_grapher <- function(purp,asim,utah,wfrc,nchrp){
  list(
    "ActivitySim" = asim,
    "Utah Statewide" = utah,
    "WFRC 2019" = wfrc,
    "NCHRP Report716" = nchrp
  ) %>%
  bind_rows(.id = "model") %>%
  dwplot(dot_args = list(size=2)) + 
  ggtitle(paste0(purp," Utility Parameter Values")) +
  xlab("(Coeff Value) / IVTT") + ylab("Path Variables in BEAM") +
  scale_x_continuous(trans = "log10", labels = function(x) format(x, scientific = FALSE)) +
   theme_bw()
}
  


weird <- scales::trans_new("neg_log2",
                           transform=function(x) log(abs(x)),
                           inverse=function(x) exp(abs(x))
                           #breaks = breaks_log())
)