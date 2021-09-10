package beam.analysis.tscore;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.events.handler.GenericEventHandler;

import java.util.HashMap;
import java.util.Map;

public class AllEventsCounter implements GenericEventHandler {
    private static final Logger log = Logger.getLogger(AllEventsCounter.class);

    Map<String, Integer> eventCounterMap = new HashMap<>();

    public AllEventsCounter(Scenario scenario) {
    }

    @Override
    public void handleEvent(GenericEvent event) {
        String type = event.getEventType();

        eventCounterMap.merge(type, 1, Integer::sum);

    }

    public void printEventCounts(){
        for(String eventType:eventCounterMap.keySet()){
            log.info("Event type: " + eventType);
            log.info("Number of events: " + eventCounterMap.get(eventType));
        }
    }
}