library(tidyverse)
library(xml2)

pop <- read_xml("population.xml")
xml_find_all(pop, "person") %>% length