package beam.analysis.tscore;

import java.util.HashMap;

public class WavUtilizationMap {
    HashMap<String, double[][]> hourTracker = new HashMap<>();
    HashMap<String, double[][]> entryTimes = new HashMap<>();
    double[][] defaultArray = putInHours();

    public void addVehicle(String id){
        hourTracker.put(id, putInHours());
        entryTimes.put(id, putInHours());
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

            }
        }
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
