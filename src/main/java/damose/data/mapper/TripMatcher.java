package damose.data.mapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import damose.data.model.Trip;
import damose.data.model.VehiclePosition;

/**
 * Matches real-time vehicle positions to static trip data.
 */
public class TripMatcher {

    private final Map<String, Trip> tripsById;

    public TripMatcher(List<Trip> trips) {
        this.tripsById = trips.stream()
                .collect(Collectors.toMap(Trip::getTripId, t -> t, (a, b) -> a));
    }

    /**
     * Match a real-time bus to its static trip.
     */
    public Trip match(VehiclePosition vp) {
        return tripsById.get(vp.getTripId());
    }

    /**
     * Get a Trip by tripId (used for StopTimes).
     */
    public Trip matchByTripId(String tripId) {
        return tripsById.get(tripId);
    }

    /**
     * Search trips by route_id, headsign, or short_name.
     */
    public List<Trip> searchByRouteOrHeadsign(String query) {
        String q = query.toLowerCase();
        return tripsById.values().stream()
                .filter(t -> t.getRouteId().toLowerCase().contains(q)
                          || t.getTripHeadsign().toLowerCase().contains(q)
                          || (t.getTripShortName() != null && t.getTripShortName().toLowerCase().contains(q)))
                .collect(Collectors.toList());
    }
}

