package damose.data.model;

/**
 * Represents a trip update record from GTFS-RT data.
 * Contains arrival time prediction for a specific stop.
 */
public class TripUpdateRecord {

    private final String tripId;
    private final String stopId;
    private final long arrivalEpochSeconds;

    public TripUpdateRecord(String tripId, String stopId, long arrivalEpochSeconds) {
        this.tripId = tripId;
        this.stopId = stopId;
        this.arrivalEpochSeconds = arrivalEpochSeconds;
    }

    public String getTripId() {
        return tripId;
    }

    public String getStopId() {
        return stopId;
    }

    public long getArrivalEpochSeconds() {
        return arrivalEpochSeconds;
    }

    @Override
    public String toString() {
        return "TripUpdate{tripId='" + tripId + "', stopId='" + stopId + "', arrival=" + arrivalEpochSeconds + "}";
    }
}

