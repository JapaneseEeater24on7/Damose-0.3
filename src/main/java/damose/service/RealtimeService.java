package damose.service;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import com.google.transit.realtime.GtfsRealtime;

import damose.config.AppConstants;
import damose.model.ConnectionMode;

/**
 * Service for fetching and maintaining GTFS-RT feeds.
 * Downloads and keeps the latest valid GTFS-RT snapshots.
 */
public class RealtimeService {

    private static GtfsRealtime.FeedMessage latestVehiclePositions;
    private static GtfsRealtime.FeedMessage latestTripUpdates;

    private static Timer timer;
    private static ConnectionMode mode = ConnectionMode.ONLINE;
    private static Runnable onDataReceived;
    private static boolean dataReceivedOnce = false;

    private RealtimeService() {
        // Static utility class
    }

    public static void setMode(ConnectionMode newMode) {
        mode = newMode;
    }

    public static ConnectionMode getMode() {
        return mode;
    }

    public static synchronized void startPolling() {
        stopPolling();
        timer = new Timer("GTFSRealtimeUpdater", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (mode == ConnectionMode.ONLINE) {
                    fetchRealtimeFeeds();
                }
            }
        }, 0, AppConstants.RT_UPDATE_INTERVAL_MS);
    }

    public static synchronized void stopPolling() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public static void fetchRealtimeFeeds() {
        // Vehicle Positions
        try {
            GtfsRealtime.FeedMessage parsed = fetchFeedFromUrl(AppConstants.VEHICLE_POSITIONS_URL);
            if (parsed != null) {
                latestVehiclePositions = parsed;
                System.out.println("VehiclePositions updated: header.ts=" +
                        (parsed.hasHeader() && parsed.getHeader().hasTimestamp() 
                            ? parsed.getHeader().getTimestamp() : "n/a"));
            }
        } catch (Exception e) {
            System.out.println("Error fetching VehiclePositions: " + e.getMessage());
        }

        // Trip Updates
        try {
            GtfsRealtime.FeedMessage parsed = fetchFeedFromUrl(AppConstants.TRIP_UPDATES_URL);
            if (parsed != null) {
                latestTripUpdates = parsed;
                System.out.println("TripUpdates updated: header.ts=" +
                        (parsed.hasHeader() && parsed.getHeader().hasTimestamp() 
                            ? parsed.getHeader().getTimestamp() : "n/a"));
                notifyDataReceived();
            }
        } catch (Exception e) {
            System.out.println("Error fetching TripUpdates: " + e.getMessage());
        }
    }

    private static GtfsRealtime.FeedMessage fetchFeedFromUrl(String urlStr) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(AppConstants.HTTP_CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(AppConstants.HTTP_READ_TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", 
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) damose-bus-tracker/1.0");
            conn.setRequestProperty("Accept", "application/x-protobuf, application/octet-stream, */*");
            conn.setRequestProperty("Accept-Encoding", "identity");
            conn.setInstanceFollowRedirects(true);

            int code = conn.getResponseCode();
            if (code != 200) {
                System.out.println("HTTP error: " + code + " for " + urlStr);
                return null;
            }

            try (InputStream in = conn.getInputStream()) {
                byte[] data = in.readAllBytes();
                if (data == null || data.length == 0) {
                    return null;
                }
                return GtfsRealtime.FeedMessage.parseFrom(data);
            }
        } catch (Exception ex) {
            System.out.println("Error fetching/parsing from " + urlStr + ": " + ex.getMessage());
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    public static GtfsRealtime.FeedMessage getLatestVehiclePositions() {
        return latestVehiclePositions;
    }

    public static GtfsRealtime.FeedMessage getLatestTripUpdates() {
        return latestTripUpdates;
    }

    public static boolean hasRealTimeData() {
        return latestTripUpdates != null || latestVehiclePositions != null;
    }

    public static void setOnDataReceived(Runnable callback) {
        onDataReceived = callback;
    }

    private static void notifyDataReceived() {
        if (!dataReceivedOnce && hasRealTimeData()) {
            dataReceivedOnce = true;
            if (onDataReceived != null) {
                onDataReceived.run();
            }
        }
    }
}

