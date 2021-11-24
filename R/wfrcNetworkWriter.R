if(!require(pacman)) install.packages("pacman")
pacman::p_load(
  char = as.character(read.csv("R/packages.csv", header = F)[,1])
)
install_github("atchley-sha/R-packageSHA")
library(packageSHA)


network <- xml_new_root(
  xml_dtd("network",
          system_id = "http://www.matsim.org/files/dtd/network_v2.dtd")
  )

network <- xml_new_document()
xml_add_child(network, "network", id = "test")


as.character(network)  
  
  
write_xml(network,"test.xml")

