package beam.agentsim.events;

import beam.router.model.EmbodiedBeamTrip;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.HasPersonId;
import java.util.Map;

/**
 * BEAM
 */
public class TripArrivalEvent extends Event implements HasPersonId{
    public final static String EVENT_TYPE = "TripArrivalEvent";
    public final static String ATTRIBUTE_MODE = "mode";
    public final static String ATTRIBUTE_CURRENT_TOUR_MODE = "currentTourMode";
    public final static String ATTRIBUTE_PERSON_ID = "person";
    public final static String ATTRIBUTE_INCOME = "income";
    public final static String ATTRIBUTE_VEH_OWNERSHIP = "vehicleOwnership";
    public final static String ATTRIBUTE_TRIP_LENGTH = "length";
    public final static String ATTRIBUTE_TOUR_INDEX = "tourIndex";
    public final static String ATTRIBUTE_ACTIVITY_INDEX = "activityIndex";
    public final static String ATTRIBUTE_TOUR_PURPOSE = "tourPurpose";
    public final static String ATTRIBUTE_ACT_TYPE = "actType";
    public final static String ATTRIBUTE_LOCATION_X = "locationX";
    public final static String ATTRIBUTE_LOCATION_Y = "locationY";
    public final EmbodiedBeamTrip chosenTrip;
    public final Id<Person> personId;
    public final String mode;
    public final String currentTourMode;
    public final Double income;
    public final String vehOwnership;
    public final Double length;
    public final Integer tourIndex;
    public final Integer activityIndex;
    public final String tourPurpose;
    public final String actType;
    public final Double locationX;
    public final Double locationY;

    public TripArrivalEvent(double time,Id<Person> personId, String chosenMode, String currentTourMode, Double income, String vehOwnership,
                           Double length, Integer tourIndex, Integer activityIndex, String tourPurpose, String actType,
                           Double locationX, Double locationY, EmbodiedBeamTrip chosenTrip) {
        super(time);
        this.personId = personId;
        this.mode = chosenMode;
        this.currentTourMode = currentTourMode;
        this.income = income;
        this.vehOwnership = vehOwnership;
        this.length = length;
        this.tourIndex = tourIndex;
        this.activityIndex = activityIndex;
        this.tourPurpose = tourPurpose;
        this.actType = actType;
        this.locationX = locationX;
        this.locationY = locationY;
        this.chosenTrip = chosenTrip;
    }

    public static TripArrivalEvent apply(Event event) {
        if (!(event instanceof TripArrivalEvent) && EVENT_TYPE.equalsIgnoreCase(event.getEventType())) {
            Map<String, String> attr = event.getAttributes();
            return new TripArrivalEvent(event.getTime(),
                    Id.createPersonId(attr.get(ATTRIBUTE_PERSON_ID)),
                    attr.get(ATTRIBUTE_MODE),
                    attr.get(ATTRIBUTE_CURRENT_TOUR_MODE),
                    Double.parseDouble(attr.get(ATTRIBUTE_INCOME)),
                    attr.get(ATTRIBUTE_VEH_OWNERSHIP),
                    Double.parseDouble(attr.get(ATTRIBUTE_TRIP_LENGTH)),
                    Integer.parseInt(attr.get(ATTRIBUTE_TOUR_INDEX)),
                    Integer.parseInt(attr.get(ATTRIBUTE_ACTIVITY_INDEX)),
                    attr.get(ATTRIBUTE_TOUR_PURPOSE),
                    attr.get(ATTRIBUTE_ACT_TYPE),
                    Double.parseDouble(attr.get(ATTRIBUTE_LOCATION_X)),
                    Double.parseDouble(attr.get(ATTRIBUTE_LOCATION_Y)),
                    null
            );
        }
        return (TripArrivalEvent) event;
    }

    @Override
    public Map<String, String> getAttributes() {
        Map<String, String> attr = super.getAttributes();
        attr.put(ATTRIBUTE_PERSON_ID, personId.toString());
        attr.put(ATTRIBUTE_MODE, mode);
        attr.put(ATTRIBUTE_CURRENT_TOUR_MODE, currentTourMode);
        attr.put(ATTRIBUTE_INCOME, income.toString());
        attr.put(ATTRIBUTE_VEH_OWNERSHIP, vehOwnership);
        attr.put(ATTRIBUTE_TRIP_LENGTH, length.toString());
        attr.put(ATTRIBUTE_TOUR_INDEX, tourIndex.toString());
        attr.put(ATTRIBUTE_ACTIVITY_INDEX, activityIndex.toString());
        attr.put(ATTRIBUTE_TOUR_PURPOSE, tourPurpose);
        attr.put(ATTRIBUTE_ACT_TYPE, actType);
        attr.put(ATTRIBUTE_LOCATION_X, locationX.toString());
        attr.put(ATTRIBUTE_LOCATION_Y, locationY.toString());
        return attr;
    }

    public Map<String, String> getVerboseAttributes() {
        Map<String, String> attr = getAttributes();
        return attr;
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }

    @Override
    public Id<Person> getPersonId() {
        return personId;
    }

    public String getMode() { return mode; }

}
