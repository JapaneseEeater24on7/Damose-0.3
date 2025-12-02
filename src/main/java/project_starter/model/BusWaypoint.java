package project_starter.model;

import org.jxmapviewer.viewer.DefaultWaypoint;

/**
 * Waypoint rappresentante un bus sulla mappa.
 */
public class BusWaypoint extends DefaultWaypoint {
    private final String tripId;
    private final String tripHeadsign;
    private final String vehicleId;

    public BusWaypoint(VehiclePosition vp, String tripHeadsign) {
        super(vp.getPosition());
        this.tripId = vp.getTripId();
        this.vehicleId = vp.getVehicleId();
        this.tripHeadsign = tripHeadsign;
    }

    public String getTripId() {
        return tripId;
    }

    public String getTripHeadsign() {
        return tripHeadsign;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    @Override
    public String toString() {
        return "Bus " + vehicleId + " su linea " + tripHeadsign + " (" + tripId + ")";
    }
}

