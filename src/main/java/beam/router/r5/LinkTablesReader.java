package beam.router.r5;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ParseBool;
import org.supercsv.cellprocessor.ParseDouble;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.constraint.UniqueHashCode;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.*;
import org.supercsv.prefs.CsvPreference;
import scala.tools.nsc.doc.base.comment.Cell;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class LinkTablesReader {
    private static final Logger log = Logger.getLogger(LinkTablesReader.class);
    private final Scenario scenario;
    private final CoordinateTransformation ct;
    private final Network network;
    private final NetworkFactory networkFactory;

    private final File outDir;
    private final File nodesFile;
    private final File linksFile;

    /**
     * Initialize a network reader.
     * @param scenario A MATSim scenario
     * @param nodesFile A CSV file with node IDS.
     * @param linksFile A CSV file with link attributes
     * @param outDir The path to the output MATSim network *directory*. File is `<outDir>/highway_network.xml.gz`
     */
    public LinkTablesReader (Scenario scenario, File nodesFile, File linksFile, File outDir) {
        this.scenario = scenario;
        this.network = this.scenario.getNetwork();
        this.networkFactory = network.getFactory();
        this.ct = TransformationFactory.getCoordinateTransformation("EPSG:4326",
                this.scenario.getConfig().global().getCoordinateSystem());
        this.outDir = outDir;
        this.nodesFile = nodesFile;
        this.linksFile = linksFile;
    }

    public void makeNetwork() throws IOException {
        readNodes(nodesFile);
        readLinks(linksFile);
        //addTurnaroundLinks();
        //new NetworkCleaner().run(network);
    }

    /**
     * Read a Nodes CSV file into the network.
     * @param nodesFile A CSV file with the following fields:
     *                  - id
     *                  - x (lon)
     *                  - y (lat)
     */
    private void readNodes(File nodesFile) throws IOException {
        // Start a reader and read the header row. `col` is an index between the column names and numbers
        ICsvMapReader reader = null;
        try{
            reader = new CsvMapReader(new FileReader(nodesFile), CsvPreference.STANDARD_PREFERENCE);
            final String[] header = reader.getHeader(true);
            final CellProcessor[] processors = getNodesProcessor();

            Map<String, Object> nodesMap;
            while( (nodesMap = reader.read(header, processors)) != null) {
                Id<Node> nodeId = Id.createNodeId(nodesMap.get("id").toString());
                Double lon = (Double) nodesMap.get("x");
                Double lat = (Double) nodesMap.get("y");
                Coord coordLatLon = CoordUtils.createCoord(lon, lat);
                Coord coord = ct.transform(coordLatLon);
                Node node = networkFactory.createNode(nodeId, coord);
                network.addNode(node);

            }

        }
        finally {
            if (reader != null) {
                reader.close();
            }
        }

    }

    /**
     * Build the header reader for the nodes file
     * @return cell processors for nodes table
     */
    private static CellProcessor[] getNodesProcessor() {
        return new CellProcessor[] {
                new UniqueHashCode(), // nodeid (must be unique)
                new ParseDouble(),    //x coordinate
                new ParseDouble()    //y coordinate
        };
    }


    /**
     * Read the network links information
     * @param linksFile A csv file containing the following fields:
     *                  - link_id,
     *                  - Oneway,
     *                  - Speed (free flow),
     *                  - DriveTime (minutes)
     *                  - Length_Miles (miles)
     *                  - RoadClass (text)
     *                  - AADT (count)
     *                  - start_node
     *                  - end_node
     *                  - ft (hcm definition)
     *                  - lanes
     *                  - sl (miles per hour)
     *                  - med median treatment
     *                  - terrain
     *                  - capacity (vehicles / hr)
     */
    private void readLinks(File linksFile) throws IOException {
        ICsvMapReader mapReader = null;
        try{
            mapReader = new CsvMapReader(new FileReader(linksFile), CsvPreference.STANDARD_PREFERENCE);
            final String[] header = mapReader.getHeader(true);
            final CellProcessor[] processors = getLinksProcessors();

            Map<String, Object> linkMap;
            while(( linkMap = mapReader.read(header, processors)) != null) {

                // set up link ID with from and to nodes
                Id<Node> fromNodeId = Id.createNodeId(linkMap.get("start_node").toString());
                Id<Node> toNodeId   = Id.createNodeId(linkMap.get("end_node").toString());
                Node fromNode = network.getNodes().get(fromNodeId);
                Node toNode   = network.getNodes().get(toNodeId);
                Id<Link> linkId = Id.createLinkId((String) linkMap.get("link_id"));
                Link l = networkFactory.createLink(linkId, fromNode, toNode);

                // get link attributes from csv
                Double driveTime   = (Double) linkMap.get("DriveTime");
                Double lengthMiles = (Double) linkMap.get("Length_Miles");
                Double capacity    = (Double) linkMap.get("capacity");
                Integer lanes      = (Integer) linkMap.get("lanes");
                Integer oneWay     = (Integer) linkMap.get("Oneway");

                Double length = lengthMiles * 1609.34; // convert miles to meters
                Double freeSpeed = length / (driveTime * 60); // convert meters per minute to meters per second

                // put link attributes on link
                l.setLength(length);
                l.setFreespeed(freeSpeed);
                l.setNumberOfLanes(lanes);
                l.setCapacity(capacity);
                network.addLink(l);

                // create reverse direction link if it exists
                if(oneWay != 1) {
                    Id<Link> rLinkId = Id.createLinkId(linkId.toString() + "r");
                    Link rl = networkFactory.createLink(rLinkId, toNode, fromNode);

                    rl.setLength(length);
                    rl.setFreespeed(freeSpeed);
                    rl.setNumberOfLanes(lanes);
                    rl.setCapacity(capacity);
                    network.addLink(rl);
                }

            }

        }
        finally {
            if( mapReader != null) {
                mapReader.close();
            }
        }

    }

    /**
     * Build the header reader for the links file
     * @return cell processors for links table
     */
    private static CellProcessor[] getLinksProcessors() {

        return new CellProcessor[] {
                    new UniqueHashCode(), // linkNo (must be unique)
                    new ParseInt(),      //Oneway
                    new ParseDouble(),    //DriveTime
                    new ParseDouble(),    //length
                    new NotNull(),        // RoadClass
                    new Optional(),    // AADT
                    new NotNull(), //start_node
                    new NotNull(), //end_node
                    new NotNull(),        //ft (HCM)
                    new ParseInt(),       //lanes
                    new ParseDouble(),    //sl
                    new NotNull(),        //median treatment
                    new NotNull(),        // area type
                    new NotNull(),        // terrain
                    new ParseDouble()     // capacity
            };
    }

    /**
     * Interstates at the edge of the boundary need to have additional turnaround links added or paths
     * going in that direction end up breaking.
     */
    private void addTurnaroundLinks() {

        // create map of nodes that only have one link entering or exiting.
        ArrayList<Node> inOnlyNodes = new ArrayList<>();
        ArrayList<Node> outOnlyNodes = new ArrayList<>();

        // Loop through all nodes in the network, and populate the lists we just created
        Iterator<? extends Node> iter = network.getNodes().values().iterator();
        while (iter.hasNext()) {
            Node myNode = iter.next();
            if(myNode.getOutLinks().isEmpty()) inOnlyNodes.add(myNode); // no outbound links
            if(myNode.getInLinks().isEmpty()) outOnlyNodes.add(myNode); // no inbound links
        }

        // loop through all the outOnlyNodes
        for(Node outNode : outOnlyNodes){
            // Loop through the inOnlyNodes and see if there are any outOnlyNodes within 50m. Finds the nearest outOnlyNode
            Node matchInNode = null;
            Coord outCoord = outNode.getCoord();
            Double inDistance = Double.POSITIVE_INFINITY; // starting distance is infinite
            for (Node inNode : inOnlyNodes) {
                Coord inCoord = inNode.getCoord();
                Double thisDistance = NetworkUtils.getEuclideanDistance(outCoord, inCoord);
                if(thisDistance < inDistance & thisDistance < 50){
                    matchInNode = inNode; // update the selected companion node
                    inDistance = thisDistance; // update the comparison distance
                }
            }

            // if there is a matched inOnlyNode, we will build a new link with default stupid attributes
            if(matchInNode != null){
                Id<Link> lid = Id.createLinkId(outNode.getId() + "_" + matchInNode.getId());
                Link l =  networkFactory.createLink(lid, matchInNode, outNode);
                l.setCapacity(2000);
                l.setNumberOfLanes(1);
                l.setFreespeed(20);
                network.addLink(l);
                log.trace("Added turnaround link " + lid);
            }
        }


    }



    private void writeNetwork(){
        log.info("Writing network to " + outDir);
        log.info("--- Links: " + network.getLinks().values().size());
        log.info("--- Nodes: " + network.getNodes().values().size());
        new NetworkWriter(network).write(outDir.toString() + "/highway_network.xml.gz");
    }



    public static void main(String[] args) {
        File nodesFile = new File(args[0]);
        File linksFile = new File(args[1]);
        File outDir = new File(args[2]);
        String crs = args[3];

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        scenario.getConfig().global().setCoordinateSystem(crs);
        LinkTablesReader reader = new LinkTablesReader(scenario, nodesFile, linksFile, new File("."));

        try {
            reader.makeNetwork();
            reader.writeNetwork();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
