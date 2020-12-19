# Questions and what I learned.
I am creating and analyzing a mini wc scenario. A few people have a wheelchair
and a there are a few new wav ridehail vehicles.
The purpose of this exercise is to
1) learn how to add population attributes and new vehicles,
2) understand the output to measure ridehail effectiveness
3) alter the ridehailing manager code.


## Explore
populationAttributes has an "excluded-modes" attribute.
- Could we put rideHail there for wc users?
But they still have access to ridehail with WAV accessibility...


## Input
### Population.xml
 I ran the sf-light_1k scenario with a sample wc population: successful.
 I manually added the wc variable to look like
`<attribute name="wc_var" class="java.lang.String" >TRUE</attribute>`. Maybe
it could be a boolean, but string works for now.

### rideHailFleet.csv
- I still don't know how a car becomes a rideHail vehicle

### sf-light-1k.conf
rideHail initialization can be "PROCEDURAL" or "FILE". If file then we could
use car or write new vehicle type WAV.

### VehicleType.csv
I created WAV as a new vehicle type "WAV-rh-only" and added an accessibility
attribute.
- What is the sampleProbabilityString column? all:0.6? income? Is this where I
should include accessibility?

- The `rideHailFleet` also needs to use the new vehicle type "WAV". What
about the vehicles file?

#### rideHailFleet.csv
- Where does this file come from? This seems too manual to be an input file.

This file was the key to visualizing my new vehicle type (WAV) in via. I manually
changed the first 30 to a WAV-rh-only and copied their ids to another csv to create
the group in via.



## Output
- Why does output only have iteration 0?
- Why do the output folder write their own names? I can't seem to create a custom
output folder name.

- How can I see the vehicle types used? I can't view `outputVehicles.xml` or
`output_vehicles.xml`.
However I see that `0.rideHailFleet.csv` calls some cases of "WAV-rh-only".
- Is `0.rideHailFleet.csv` an optimized version of rideHailFleet.csv? What is
the difference?
- What are the vehicle IDs on `0.physSimEvents.xml`? The vehicle ids are so confusing

#### Analysis of RideHail
I want to know information on wc people and on wavs.
I think all of this information comes from the events file `0.events.csv`.
Then I can join vehicles (`0.rideHailFleet.csv`), but I can't find how to get
population.
- Where can I get population Ids?
- Can I convert xml to csv or read xml into R tibbles?
- Should I just spend the time figuring it out in Via? R would be faster
To do the analysis in R, I want to make a persons file, a trips file, a vehicles
file (from plans), and events file (may be the same as the tris file...)





## Via
How do I get it to work in Via 20.2? Use 0.physSimEvents.xml! physSimEvents
deals more with the vehicles while the 0.events.xml comes from agentSim and
is more with agents.

- I created a wav group: successful. This group was created from a csv file of ids
copied from the `rideHailFleet_wav`. I styled the group in Via.
