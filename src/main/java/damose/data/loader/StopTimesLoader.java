package damose.data.loader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import damose.config.AppConstants;
import damose.data.model.StopTime;

/**
 * Loader for GTFS stop_times.txt file.
 */
public final class StopTimesLoader {

    private static List<StopTime> allStopTimes = new ArrayList<>();
    private static Map<String, List<StopTime>> stopTimesByStop = new HashMap<>();

    private StopTimesLoader() {
        // Utility class
    }

    public static List<StopTime> load() {
        return load(AppConstants.GTFS_STOP_TIMES_PATH);
    }

    public static List<StopTime> load(String resourcePath) {
        allStopTimes.clear();
        stopTimesByStop.clear();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(StopTimesLoader.class.getResourceAsStream(resourcePath)))) {

            String line;
            boolean firstLine = true;

            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                String[] parts = line.split(",");
                if (parts.length < 10) continue;

                String tripId = parts[0];
                LocalTime arrival = parseTime(parts[1]);
                LocalTime departure = parseTime(parts[2]);
                String stopId = parts[3];
                int stopSequence = parseInt(parts[4]);
                String stopHeadsign = parts[5];
                int pickupType = parseInt(parts[6]);
                int dropOffType = parseInt(parts[7]);
                double shapeDistTraveled = parseDouble(parts[8]);
                int timepoint = parseInt(parts[9]);

                StopTime st = new StopTime(tripId, arrival, departure, stopId,
                        stopSequence, stopHeadsign, pickupType, dropOffType,
                        shapeDistTraveled, timepoint);

                allStopTimes.add(st);
                stopTimesByStop.computeIfAbsent(stopId, k -> new ArrayList<>()).add(st);
            }

            System.out.println("StopTimes loaded: " + allStopTimes.size());

        } catch (IOException e) {
            System.err.println("Error loading stop_times: " + e.getMessage());
            e.printStackTrace();
        }

        return allStopTimes;
    }

    public static List<StopTime> getAllStopTimes() {
        return allStopTimes;
    }

    public static List<StopTime> getStopTimesForStop(String stopId) {
        return stopTimesByStop.getOrDefault(stopId, new ArrayList<>());
    }

    private static LocalTime parseTime(String s) {
        try {
            if (s == null || s.isEmpty()) return null;

            String[] parts = s.split(":");
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            int sec = Integer.parseInt(parts[2]);

            // GTFS allows hours > 24, normalize
            h = h % 24;

            return LocalTime.of(h, m, sec);
        } catch (Exception e) {
            return null;
        }
    }

    private static int parseInt(String s) {
        try {
            return (s == null || s.isEmpty()) ? 0 : Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private static double parseDouble(String s) {
        try {
            return (s == null || s.isEmpty()) ? 0.0 : Double.parseDouble(s);
        } catch (Exception e) {
            return 0.0;
        }
    }
}

