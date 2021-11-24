if(!require(pacman)) install.packages("pacman")
pacman::p_load(
  char = as.character(read.csv("R/packages.csv", header = F)[,1])
)
install_github("atchley-sha/R-packageSHA")
library(packageSHA)



networkFile <- "test/input/slc_test_network/wfrc_network.xml"

network <- read_file(networkFile)

network %<>% str_replace_all("(id=\"\\d+)_(\\d+\")", "\\10\\2")

network %>% 
  write_file(networkFile)