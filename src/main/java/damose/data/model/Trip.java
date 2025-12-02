package damose.data.model;

/**
 * Represents a trip from GTFS static data.
 */
public class Trip {

    private final String routeId;
    private final String serviceId;
    private final String tripId;
    private final String tripHeadsign;
    private final String tripShortName;
    private final int directionId;
    private final String shapeId;

    public Trip(String routeId, String serviceId, String tripId,
                String tripHeadsign, String tripShortName, int directionId, String shapeId) {
        this.routeId = routeId;
        this.serviceId = serviceId;
        this.tripId = tripId;
        this.tripHeadsign = tripHeadsign;
        this.tripShortName = tripShortName;
        this.directionId = directionId;
        this.shapeId = shapeId;
    }

    // -------- Getters --------
    public String getRouteId() {
        return routeId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getTripId() {
        return tripId;
    }

    public String getTripHeadsign() {
        return tripHeadsign;
    }

    public String getTripShortName() {
        return tripShortName;
    }

    public int getDirectionId() {
        return directionId;
    }

    public String getShapeId() {
        return shapeId;
    }

    @Override
    public String toString() {
        return "Trip{routeId='" + routeId + "', tripId='" + tripId + "', headsign='" + tripHeadsign + "'}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Trip trip = (Trip) o;
        return tripId != null ? tripId.equals(trip.tripId) : trip.tripId == null;
    }

    @Override
    public int hashCode() {
        return tripId != null ? tripId.hashCode() : 0;
    }
}

