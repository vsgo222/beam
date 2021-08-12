Network Conversion from MATSim to BEAM
===========================================

NOTE: This should be done *before* running the MATSimConversionTool (though if the Tool has already been run, it can simply be run again after the network conversion).

Input network
----------------

The network initially needs to be in MATSim format, which is a format similar to the following:

	<network name="example network">
		<nodes>
			<node id="1" x="0.0" y="0.0" />
			<node id="2" x="1000.0" y="0.0" />
			<node id="3" x="1000.0" y="1000.0" />
			<node id="4" x="0.0" y="1000.0" />
		</nodes>
		<links>
			<link id="1" from="1" to="2" length="1000" capacity="3600" freespeed="27.78" permlanes="2" modes="car" type="major-arterial" />
			<link id="1" from="2" to="3" length="1000" capacity="3600" freespeed="27.78" permlanes="2" modes="car" type="major-arterial" />
			<link id="1" from="3" to="4" length="1000" capacity="3600" freespeed="27.78" permlanes="2" modes="car" type="major-arterial" />
			<link id="1" from="4" to="1" length="1000" capacity="3600" freespeed="27.78" permlanes="2" modes="car" type="major-arterial" />
		</links>
	</network>

This format follows the network_v2.dtd, and needs this DOCTYPE declaration:
`<!DOCTYPE network SYSTEM "http://www.matsim.org/files/dtd/network_v2.dtd">`

Regarding the allowed `type` values in the link attributes:
There is a default list (from OpenStreetMap I believe) that can be found in the code, but it's possible to use a .json file with custom values as well.
That file should follow the same format as the default (which is written explicitly in the code, I'm not sure exactly where but it's not too hard to find by tracing through the conversion script below and finding where it uses the `type` value).

Converting to PBF
--------------------

Use src/test/scala/beam/router/PhyssimXmlToOsmConverter.scala to convert the network to `.pbf` format.
This class takes 3 CLI arguments: `--sourceFile`, `--targetFile`, and `--outputType`.
`sourceFile` is the path to the MATSim network to be converted (the .xml file), `targetFile` is the path to the output file (no extension needed), and `outputType` is either 'osm' or 'pbf'.
It seems it needs to be 'pbf' to work properly with BEAM.

Once the .pbf network is created, put it in the `r5/` folder, and delete all other files in this folder except `SLC.zip` or the equivalent GTFS file (and the newly added network). Also delete the `physsim-network.xml` file in the root scenario directory.
Change the `.conf` file for the scenario at `beam.routing.r5.osmFile` to point to the new network.
Also change the matsim conversion network near the top of the `.conf` file to point to the input .xml network.
Then run the MATSimConversionTool and BEAM as you normally would.

BEAM should create a network from this new file and use that for its simulation.
