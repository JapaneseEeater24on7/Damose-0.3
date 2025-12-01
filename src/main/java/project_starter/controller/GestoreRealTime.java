package project_starter.controller;

import com.google.transit.realtime.GtfsRealtime;
import project_starter.ConnectionMode;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

/**
 * GestoreRealTime: scarica e mantiene l'ultimo snapshot GTFS-RT valido.
 */
public class GestoreRealTime {

    public static final String VEHICLE_POSITIONS_URL =
            "https://romamobilita.it/sites/default/files/rome_rtgtfs_vehicle_positions_feed.pb";
    public static final String TRIP_UPDATES_URL =
            "https://romamobilita.it/sites/default/files/rome_rtgtfs_trip_updates_feed.pb";

    private static GtfsRealtime.FeedMessage latestVehiclePositions;
    private static GtfsRealtime.FeedMessage latestTripUpdates;

    private static Timer timer;
    private static ConnectionMode mode = ConnectionMode.ONLINE;

    public static void setMode(ConnectionMode newMode) {
        mode = newMode;
    }

    public static ConnectionMode getMode() {
        return mode;
    }

    public static synchronized void startAggiornamento() {
        stopAggiornamento();
        timer = new Timer("GTFSRealtimeUpdater", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (mode == ConnectionMode.ONLINE) {
                    fetchRealtimeFeeds();
                }
            }
        }, 0, 30_000);
    }

    public static synchronized void stopAggiornamento() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public static void fetchRealtimeFeeds() {
        // VehiclePositions
        try {
            GtfsRealtime.FeedMessage parsed = fetchFeedFromUrl(VEHICLE_POSITIONS_URL);
            if (parsed != null) {
                latestVehiclePositions = parsed;
                System.out.println("VehiclePositions aggiornato: header.ts=" +
                        (parsed.hasHeader() && parsed.getHeader().hasTimestamp() ? parsed.getHeader().getTimestamp() : "n/a"));
            } else {
                System.out.println("VehiclePositions: parsing fallito o feed vuoto; mantengo snapshot precedente");
            }
        } catch (Exception e) {
            System.out.println("Errore fetch VehiclePositions: " + e.getClass().getSimpleName() + " " + e.getMessage());
        }

        // TripUpdates
        try {
            GtfsRealtime.FeedMessage parsed = fetchFeedFromUrl(TRIP_UPDATES_URL);
            if (parsed != null) {
                latestTripUpdates = parsed;
                System.out.println("TripUpdates aggiornato: header.ts=" +
                        (parsed.hasHeader() && parsed.getHeader().hasTimestamp() ? parsed.getHeader().getTimestamp() : "n/a"));
            } else {
                System.out.println("TripUpdates: parsing fallito o feed vuoto; mantengo snapshot precedente");
            }
        } catch (Exception e) {
            System.out.println("Errore fetch TripUpdates: " + e.getClass().getSimpleName() + " " + e.getMessage());
        }
    }

    private static GtfsRealtime.FeedMessage fetchFeedFromUrl(String urlStr) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(15_000);
            conn.setRequestProperty("User-Agent", "realtime-bus-tracker/1.0");
            conn.setRequestProperty("Accept", "*/*");
            conn.setInstanceFollowRedirects(true);

            int code = conn.getResponseCode();
            String contentType = conn.getContentType();
            System.out.println("GET " + urlStr + " -> " + code + " content-type=" + contentType);

            if (code != 200) {
                System.out.println("HTTP non OK: " + code + " per " + urlStr);
                return null;
            }

            try (InputStream in = conn.getInputStream()) {
                byte[] data = in.readAllBytes();
                if (data == null || data.length == 0) {
                    System.out.println("Feed vuoto da " + urlStr);
                    return null;
                }
                GtfsRealtime.FeedMessage feed = GtfsRealtime.FeedMessage.parseFrom(data);
                System.out.println("Parsed feed entities: " + feed.getEntityCount());
                int printed = 0;
                for (GtfsRealtime.FeedEntity e : feed.getEntityList()) {
                    if (e.hasTripUpdate()) {
                        GtfsRealtime.TripUpdate tu = e.getTripUpdate();
                        String tid = tu.hasTrip() ? tu.getTrip().getTripId() : "n/a";
                        System.out.println("  sample TU tripId: " + tid);
                        if (++printed >= 20) break;
                    } else if (e.hasVehicle()) {
                        GtfsRealtime.VehiclePosition vp = e.getVehicle();
                        String tid = vp.hasTrip() ? vp.getTrip().getTripId() : "n/a";
                        System.out.println("  sample VP tripId: " + tid);
                        if (++printed >= 20) break;
                    }
                }
                return feed;
            }
        } catch (Exception ex) {
            System.out.println("Errore durante fetch/parsing da " + urlStr + ": " + ex.getClass().getSimpleName() + " " + ex.getMessage());
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
}
