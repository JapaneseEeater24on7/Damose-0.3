package damose.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jxmapviewer.viewer.GeoPosition;

import damose.data.loader.StopTimesLoader;
import damose.data.loader.StopsLoader;
import damose.data.loader.TripsLoader;
import damose.data.model.Stop;
import damose.data.model.StopTime;
import damose.data.model.Trip;
import damose.data.model.VehiclePosition;

/**
 * Static simulator for offline mode.
 * Generates simulated bus positions based on static GTFS data.
 */
public final class StaticSimulator {

    private StaticSimulator() {
        // Utility class
    }

    /**
     * Simulate all buses from static GTFS files.
     */
    public static List<VehiclePosition> simulateAllTrips() {
        List<VehiclePosition> buses = new ArrayList<>();

        List<Trip> trips = TripsLoader.load();
        List<StopTime> stopTimes = StopTimesLoader.load();
        List<Stop> stops = StopsLoader.load();

        for (Trip trip : trips) {
            String tripId = trip.getTripId();

            List<StopTime> tripStops = stopTimes.stream()
                    .filter(st -> st.getTripId().equals(tripId))
                    .sorted((a, b) -> Integer.compare(a.getStopSequence(), b.getStopSequence()))
                    .collect(Collectors.toList());

            for (StopTime st : tripStops) {
                Stop stop = stops.stream()
                        .filter(s -> s.getStopId().equals(st.getStopId()))
                        .findFirst()
                        .orElse(null);

                if (stop != null) {
                    GeoPosition pos = new GeoPosition(stop.getStopLat(), stop.getStopLon());
                    buses.add(new VehiclePosition(
                            tripId,
                            "SIM-" + tripId,
                            pos,
                            st.getStopSequence()
                    ));
                }
            }
        }

        return buses;
    }
}

