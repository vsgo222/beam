package beam.utils;


import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
/*
Command Line Arguments:
File path to the outputEvents.xml file.

Make sure to unzip the gzip file
*/
public class XMLKiller {
    private static String generateFileName(){
        String filename = "Key_Data_";
        LocalDate today = LocalDate.now();
        LocalTime time = LocalTime.now();
        filename = filename.concat(today.toString());
        filename = filename.concat(time.toString());
        filename = filename.replace(':', '-');
        filename = filename.replace('.', '-');
        filename = filename.concat(".csv");
        return filename;
    }
    private static String parseXMLLine(String XML){
        String CSVLine = "";
        Boolean broken = false;
        String person = "";
        String vehicle = "";
        if (XML.contains("person")){
            int leftIndex = XML.indexOf("person") + 8;
            int rightIndex = XML.indexOf("\"",leftIndex);
            person = XML.substring(leftIndex, rightIndex);
            CSVLine = CSVLine.concat(person + ",");
        }
        else {
            CSVLine = CSVLine.concat(",");
        }
        if (XML.contains("vehicle")){
            int leftIndex = XML.indexOf("vehicle") + 9;
            int rightIndex = XML.indexOf("\"",leftIndex);
            vehicle = XML.substring(leftIndex, rightIndex);
            CSVLine = CSVLine.concat(vehicle + ",");
        }
        else {
            CSVLine = CSVLine.concat(",");
        }
        if (XML.contains("vehicleType")){
            int leftIndex = XML.indexOf("vehicleType") + 13;
            int rightIndex = XML.indexOf("\"", leftIndex);
            String vehicleType = XML.substring(leftIndex, rightIndex);
            CSVLine = CSVLine.concat(vehicleType + ",");
        }
        else {
            CSVLine = CSVLine.concat(",");
        }
        if (XML.contains("type")){
            int leftIndex = XML.indexOf("type") + 6;
            int rightIndex = XML.indexOf("\"", leftIndex);
            String type = XML.substring(leftIndex, rightIndex);
            CSVLine = CSVLine.concat(type + ",");
        }
        else {
            CSVLine = CSVLine.concat(",");
        }
        if (XML.contains("mode")){
            int leftIndex = XML.indexOf("mode") + 6;
            int rightIndex = XML.indexOf("\"",leftIndex);
            String mode = XML.substring(leftIndex, rightIndex);
            CSVLine = CSVLine.concat(mode + ",");
        }
        else {
            CSVLine = CSVLine.concat(",");
        }
        if (XML.contains("time")){
            int leftIndex = XML.indexOf("time") + 6;
            int rightIndex = XML.indexOf("\"", leftIndex);
            String time = XML.substring(leftIndex, rightIndex);
            CSVLine = CSVLine.concat(time + ",");
        }
        else {
            CSVLine = CSVLine.concat(",");
        }
        if (XML.contains("startX")){
            int leftIndex = XML.indexOf("startX") + 8;
            int rightIndex = XML.indexOf("\"", leftIndex);
            String startingX = XML.substring(leftIndex, rightIndex);

            leftIndex = XML.indexOf("startY") + 8;
            rightIndex = XML.indexOf("\"", leftIndex);
            String startingY = XML.substring(leftIndex, rightIndex);

            CSVLine = CSVLine.concat(startingX + " | " + startingY + ",");

            leftIndex = XML.indexOf("endX") + 6;
            rightIndex = XML.indexOf("\"", leftIndex);
            String endingX = XML.substring(leftIndex, rightIndex);

            leftIndex = XML.indexOf("endY") + 6;
            rightIndex = XML.indexOf("\"", leftIndex);
            String endingY = XML.substring(leftIndex, rightIndex);

            CSVLine = CSVLine.concat(endingX + " | " + endingY + ",");
        }
        else {
            CSVLine = CSVLine.concat(",,");
        }
        if (person.contains("wc-") && !(vehicle.contains("rideHailVehicle") || vehicle.contains("dummy")|| vehicle.contains("SF"))){
            if (!vehicle.contains("wc-") && !vehicle.isEmpty()){
                broken = true;
            }
        }
        if (broken){
            CSVLine = CSVLine.concat("Yes");
        }
        else {
            CSVLine = CSVLine.concat("No");
        }

        return CSVLine + "\n";
    }
    /*
    TODO: Read in VehicleType as well.
     */
    private static void readInFile(String XMLName){
        BufferedReader reader;
        BufferedWriter writer;
        File XMLFile = new File(XMLName);
        try {
            FileWriter fStream = new FileWriter(generateFileName(), true);
            fStream.append("PersonID");
            fStream.append(",");
            fStream.append("VehicleID");
            fStream.append(",");
            fStream.append("VehicleType");
            fStream.append(",");
            fStream.append("EventType");
            fStream.append(",");
            fStream.append("Mode");
            fStream.append(",");
            fStream.append("Time");
            fStream.append(",");
            fStream.append("Starting Coordinates");
            fStream.append(",");
            fStream.append("Ending Coordinates");
            fStream.append(",");
            fStream.append("Check this out");
            fStream.append("\n");

            writer = new BufferedWriter(fStream);

            reader = new BufferedReader(new FileReader(XMLFile));
            String nextLine = " ";
            reader.readLine();
            reader.readLine();
            /*
            This is where the big boy stuff happens.
             */
            while (nextLine != null){
                writer.write(parseXMLLine(nextLine));
                nextLine = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] argv) {
        readInFile(argv[0]);

    }

}
