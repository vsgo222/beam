# Questions and Hypothesis

@Dr_Macfarlane, Chris and I looked through the RideHailManager, RideHailMatching,
and RideHailRequest classes and have some questions. We want to let you know what
we learned and what we hypothesize before our meeting at 1:00.

## What we learned
We started at the input level and see that vehicles are defined at the household
level. Vehicle types is another file that shows all attributes for vehicle types.
*We learned that ridehail is an attribute not a vehicle type*. We would like to
make sure we are correct about that.

## Our Questions
The vehicle IDs seem confusing, `sample/1k/vehicles.csv` is very different from
`rideHailFleet.csv`.
- What is the process of creating ridehailing vehicles? Do they come from
household vehicles?
- Would WAV be a vehicle type or a vehicle attribute?

## Our Hypothesis
Once we understand the nature of vehicles in beam, we can understand
ridehailing assignment.
- Population. Wheelchair could be an *attribute* or a *unique ID* (w-9879098
  for example)
- WAV will behave exactly like ridehail currently behaves. Ridehail could be
adjusted to exclude wc individuals
- Maybe the unique assignment happens in the request stage.

## Draft email
Dear Berkeley Lab guys (Zach),

We appreciate the work you have done to help us so far. (Anything else we should thank them for).

We are trying to extend the ridehail functionality and have some questions.
We understand that ridehailing is an attribute of the vehicle (`vehicleType.csv`).
We also understand that private vehicles come from the households file and
are labeled in their own `vehicles.csv`. These vehicles
are different from transit vehicles, and we want to know how to create a new type
of ride hail vehicle fleet (a fleet that serves only a unique population).

*Our questions*
How can we create a new type of ridehailing? Would that be an additional attribute
to an existing vehicle type or a new vehicle type altogether? Maybe both work, but which would work
better with assigning rides to particular agents?

While ridehail vehicles are not considered "a transit vehicle" (),
how do vehicles become "ridehailing vehicles" (`rideHailFleet.csv`)?
