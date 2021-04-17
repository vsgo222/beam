# Creating the WC segmentation in Ride Hail modules

1. Call the method in `alonso_mora` and `async_greedy`
2. Write method in `RideHailMatching.scala`
3. Create new vehicle in `vehicleTypes.csv` and make sure is included in config.
3. Add attribute to `beamVehicleType.scala` and `beamVehicleUtils.scala`

How can we force the WAVs to prioritize wc requests? In pooling?

How can we use `FILE` initialization type (instead of `PROCEDURAL`)?
- I need to show that the first version of sf-light-1k does not access
the async_greedy when running with `FILE` type initialization. This means I go
back to the first commit. (I tried cloning the repo and running from scratch
  but am getting an error on java 1.8 or something.)
- Then write an issue on parent repo.


# Getting Beam up and running. (edits to 5k config file in sf-light)
- I needed to change the output directory in config
- I needed to adjust write plans and physSimEvents in config
- max weight time value = 0
