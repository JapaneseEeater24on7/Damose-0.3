package damose.data.loader;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import damose.config.AppConstants;
import damose.data.model.Stop;

/**
 * Loader for GTFS stops.txt file.
 */
public final class StopsLoader {

    private StopsLoader() {
        // Utility class
    }

    public static List<Stop> load() {
        return load(AppConstants.GTFS_STOPS_PATH);
    }

    public static List<Stop> load(String resourcePath) {
        List<Stop> stops = new ArrayList<>();

        try (InputStream in = StopsLoader.class.getResourceAsStream(resourcePath);
             BufferedReader br = new BufferedReader(new InputStreamReader(in))) {

            // Skip header
            br.readLine();

            String line;
            while ((line = br.readLine()) != null) {
                List<String> fields = CsvParser.parseLine(line);

                if (fields.size() < 6) continue;

                String stopId = fields.get(0).trim();
                String stopCode = fields.get(1).trim();
                String stopName = fields.get(2).trim();
                String latStr = fields.get(4).trim();
                String lonStr = fields.get(5).trim();

                if (latStr.isEmpty() || lonStr.isEmpty()) continue;

                double lat = Double.parseDouble(latStr);
                double lon = Double.parseDouble(lonStr);

                stops.add(new Stop(stopId, stopCode, stopName, lat, lon));
            }

        } catch (Exception e) {
            System.err.println("Error loading stops: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("Stops loaded: " + stops.size());
        return stops;
    }
}

