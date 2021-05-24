i <- sum(vector) + 1
j <- length(vector) + 1

while(i < nrow(DBT)){
  x <- 1
  while(DBT[i,"person_id"] == DBT[i+1, "person_id"]){
    x+1 -> x
    i+1 -> i
  }
  vector[[j]] <- x
  i+1 -> i
  j+1 -> j
}