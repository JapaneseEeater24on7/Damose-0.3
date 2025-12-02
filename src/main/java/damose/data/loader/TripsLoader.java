package damose.data.loader;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import damose.config.AppConstants;
import damose.data.model.Trip;

/**
 * Loader for GTFS trips.txt file.
 * Tolerant to malformed rows and preserves service_id exactly as in the file.
 */
public final class TripsLoader {

    private TripsLoader() {
        // Utility class
    }

    public static List<Trip> load() {
        return load(AppConstants.GTFS_TRIPS_PATH);
    }

    public static List<Trip> load(String resourcePath) {
        List<Trip> trips = new ArrayList<>();

        try (InputStream in = TripsLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                System.err.println("TripsLoader: resource not found: " + resourcePath);
                return trips;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line = br.readLine();
                if (line == null) return trips;

                // Check if first line is header
                boolean headerConsumed = line.toLowerCase().startsWith("route_id") 
                        || line.toLowerCase().startsWith("service_id") 
                        || line.toLowerCase().contains("trip_id");
                
                if (!headerConsumed) {
                    processLine(line, trips);
                }

                while ((line = br.readLine()) != null) {
                    processLine(line, trips);
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading trips.txt: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("Trips loaded: " + trips.size());
        return trips;
    }

    private static void processLine(String line, List<Trip> trips) {
        if (line == null || line.trim().isEmpty()) return;

        List<String> fields = CsvParser.parseLine(line);
        
        // trips.txt: route_id,service_id,trip_id,trip_headsign,trip_short_name,direction_id,block_id,shape_id
        if (fields.size() < 3) return;

        try {
            String routeId = safeGet(fields, 0).trim();
            String serviceId = safeGet(fields, 1).trim();
            String tripId = safeGet(fields, 2).trim();
            String tripHeadsign = safeGet(fields, 3).replace("\"", "").trim();
            String tripShortName = safeGet(fields, 4).trim();

            int directionId = 0;
            String dirField = safeGet(fields, 5).trim();
            if (!dirField.isEmpty()) {
                try {
                    directionId = Integer.parseInt(dirField);
                } catch (NumberFormatException ignored) {
                }
            }

            String shapeId = safeGet(fields, 7).trim();

            trips.add(new Trip(routeId, serviceId, tripId, tripHeadsign, tripShortName, directionId, shapeId));
        } catch (Exception ex) {
            System.err.println("TripsLoader: row ignored due to parsing error: " + ex.getMessage());
        }
    }

    private static String safeGet(List<String> list, int idx) {
        return idx < list.size() ? list.get(idx) : "";
    }
}

