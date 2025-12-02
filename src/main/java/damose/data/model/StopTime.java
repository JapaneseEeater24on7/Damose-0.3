package damose.data.model;

import java.time.LocalTime;

/**
 * Represents a stop time from GTFS static data.
 * Links a trip to a stop at a specific time.
 */
public class StopTime {

    private final String tripId;
    private final LocalTime arrivalTime;
    private final LocalTime departureTime;
    private final String stopId;
    private final int stopSequence;
    private final String stopHeadsign;
    private final int pickupType;
    private final int dropOffType;
    private final double shapeDistTraveled;
    private final int timepoint;

    public StopTime(String tripId, LocalTime arrivalTime, LocalTime departureTime,
                    String stopId, int stopSequence, String stopHeadsign,
                    int pickupType, int dropOffType, double shapeDistTraveled, int timepoint) {
        this.tripId = tripId;
        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
        this.stopId = stopId;
        this.stopSequence = stopSequence;
        this.stopHeadsign = stopHeadsign;
        this.pickupType = pickupType;
        this.dropOffType = dropOffType;
        this.shapeDistTraveled = shapeDistTraveled;
        this.timepoint = timepoint;
    }

    // -------- Getters --------
    public String getTripId() {
        return tripId;
    }

    public LocalTime getArrivalTime() {
        return arrivalTime;
    }

    public LocalTime getDepartureTime() {
        return departureTime;
    }

    public String getStopId() {
        return stopId;
    }

    public int getStopSequence() {
        return stopSequence;
    }

    public String getStopHeadsign() {
        return stopHeadsign;
    }

    public int getPickupType() {
        return pickupType;
    }

    public int getDropOffType() {
        return dropOffType;
    }

    public double getShapeDistTraveled() {
        return shapeDistTraveled;
    }

    public int getTimepoint() {
        return timepoint;
    }

    @Override
    public String toString() {
        return tripId + " @ " + stopId + " → " + arrivalTime;
    }
}

