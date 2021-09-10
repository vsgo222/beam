package beam.analysis.tscore;

import org.apache.log4j.Logger;
import probability_monad.Distribution;

import java.util.HashMap;
import java.util.List;

public class WavUtilizationMap {
    private static final Logger log = Logger.getLogger(WavHandlersRunner.class);

    HashMap<String, double[][]> hourTracker = new HashMap<>();
    HashMap<String, double[][]> entryTimes = new HashMap<>();
    HashMap<String, double[]> emptyTracker = new HashMap<>();
    double[][] defaultArray = putInHours();

    public void addVehicle(String id){
        if (!hourTracker.containsKey(id)) {
            hourTracker.put(id, putInHours());
            entryTimes.put(id, putInHours());
        }
    }
    /**
     (String id, double time)
     1. Find the hour time/3600
     2. Round down
     3. Put the time in the map, value of the vehicle at the correct hour
     4. Once the person leaves, then we access that same value
     5. Subtract time difference
     6. Replace entry time with difference
     */
    public void personEntersVehicle(String id, double time){
        int hour = (int)Math.floor(time / 3600);
        double[][] vehicleData = entryTimes.get(id);
        if (vehicleData[1][hour] == 0) {
            vehicleData[1][hour] = time;
        }
    }
    public void personLeavesVehicle(String id, double time){
        if (entryTimes.containsKey(id)){
            int originalHour = (int)Math.floor(time / 3600);
            int hour = originalHour;
            double[][] vehicleData = entryTimes.get(id);
            double[][] useTime = hourTracker.get(id);
            while (vehicleData[1][hour] == 0){ // Not in the same hour
                hour = hour - 1;
            }
            double entryTime = vehicleData[1][hour];
            double travelTime = time - entryTime;
            if (hour == originalHour){ //All in the same hour
                useTime[1][hour] += travelTime;
                vehicleData[1][hour] = 0;
            }
            else { //Goes across hours
                // Take the time and subtract the original hour * 3600 and add it into the useTime
                // Take the travel time and subtract the original hour time from it add it into the useTime for the other hour.
                double lastHourTime = time - (3600 * originalHour);
                double firstHourTime = travelTime - lastHourTime;
                useTime[1][originalHour] += lastHourTime;
                useTime[1][hour] += firstHourTime;
                vehicleData[1][hour] = 0;
            }
        }
    }
    public HashMap<String, String> getAverageTimes(){
        getEmptyTimes();
        HashMap<String, String> averages = new HashMap<>();
        for (String id: emptyTracker.keySet()){
            averages.put(id, String.valueOf(getAverageTime(id)));
        }
        return averages;
    }
    private double getAverageTime(String id){ // Goes through an entry of the map (a vehicle) and gets average empty time
        double average = 0.0;
        for(int i = 0; i < 29; i++){
            average += emptyTracker.get(id)[i];
        }
        return average / 29;
    }
    public HashMap<String, double[]> getEmptyTimes(){
        for(String id: hourTracker.keySet()){
            emptyTracker.put(id, getEmptyTime(id));
        }
        return emptyTracker;
    }
    private double[] getEmptyTime(String id){
        double[] data = hourTracker.get(id)[1];
        double[] reversed = new double[data.length];
        for (int i = 0; i < data.length; i++){
            reversed[i] = 3600 - data[i];
        }
        return reversed;
    }
    private double[][] putInHours(){
        double[][] unfinishedArray = new double[2][30];
        for (int i = 0; i < 30; i++){
            unfinishedArray[0][i] = i;
            unfinishedArray[1][i] = 0;
        }
        return unfinishedArray;
    }
}