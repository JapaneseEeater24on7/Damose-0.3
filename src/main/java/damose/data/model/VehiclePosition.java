package damose.data.model;

import org.jxmapviewer.viewer.GeoPosition;

/**
 * Represents a real-time vehicle position from GTFS-RT data.
 */
public class VehiclePosition {

    private final String tripId;
    private final String vehicleId;
    private final GeoPosition position;
    private final int stopSequence;

    public VehiclePosition(String tripId, String vehicleId, GeoPosition position, int stopSequence) {
        this.tripId = tripId;
        this.vehicleId = vehicleId;
        this.position = position;
        this.stopSequence = stopSequence;
    }

    public String getTripId() {
        return tripId;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public GeoPosition getPosition() {
        return position;
    }

    public int getStopSequence() {
        return stopSequence;
    }

    @Override
    public String toString() {
        return "Bus " + vehicleId + " trip=" + tripId +
               " pos=" + position.getLatitude() + "," + position.getLongitude() +
               " stopSeq=" + stopSequence;
    }
}

