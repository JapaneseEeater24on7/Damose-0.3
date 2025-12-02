package project_starter.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jxmapviewer.viewer.GeoPosition;

import project_starter.datas.StopTime;
import project_starter.datas.StopTimesLoader;
import project_starter.datas.Stops;
import project_starter.datas.StopsLoader;
import project_starter.datas.Trips;
import project_starter.datas.TripsLoader;

/**
 * Simulatore statico per modalità offline.
 * Genera posizioni bus fittizie basate sui dati GTFS statici.
 */
public class StaticSimulator {

    private StaticSimulator() {}

    /**
     * Simula tutti i bus presenti nei file statici GTFS.
     */
    public static List<VehiclePosition> simulateAllTrips() {
        List<VehiclePosition> buses = new ArrayList<>();

        List<Trips> trips = TripsLoader.load("/gtfs_static/trips.txt");
        List<StopTime> stopTimes = StopTimesLoader.load("/gtfs_static/stop_times.txt");
        List<Stops> stops = StopsLoader.load("/gtfs_static/stops.txt");

        for (Trips trip : trips) {
            String tripId = trip.getTripId();

            List<StopTime> tripStops = stopTimes.stream()
                    .filter(st -> st.getTripId().equals(tripId))
                    .sorted((a, b) -> Integer.compare(a.getStopSequence(), b.getStopSequence()))
                    .collect(Collectors.toList());

            for (StopTime st : tripStops) {
                Stops stop = stops.stream()
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

