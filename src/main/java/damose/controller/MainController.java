package damose.controller;

import java.awt.geom.Point2D;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;

import org.jxmapviewer.viewer.GeoPosition;

import com.google.transit.realtime.GtfsRealtime;

import damose.data.loader.CalendarLoader;
import damose.data.loader.StopTimesLoader;
import damose.data.loader.StopsLoader;
import damose.data.loader.TripsLoader;
import damose.data.mapper.StopTripMapper;
import damose.data.mapper.TripMatcher;
import damose.data.model.Stop;
import damose.data.model.StopTime;
import damose.data.model.Trip;
import damose.data.model.TripServiceCalendar;
import damose.data.model.TripUpdateRecord;
import damose.data.model.VehiclePosition;
import damose.model.ConnectionMode;
import damose.service.ArrivalService;
import damose.service.GtfsParser;
import damose.service.RealtimeService;
import damose.service.RouteService;
import damose.service.StaticSimulator;
import damose.ui.MainView;
import damose.ui.map.MapOverlayManager;

/**
 * Main application controller.
 * Coordinates view, static GTFS data, and real-time updates.
 */
public class MainController {

    private List<Stop> stops;
    private List<Trip> trips;
    private List<StopTime> stopTimes;
    private TripMatcher matcher;
    private StopTripMapper stopTripMapper;
    private RouteService routeService;
    private ConnectionMode mode = ConnectionMode.ONLINE;
    private MainView view;

    private ArrivalService arrivalService;
    private Timer realtimeTimer;
    private long currentFeedTs = Instant.now().getEpochSecond();

    public void start() {
        System.out.println("Starting application...");

        // Load static data
        stops = StopsLoader.load();
        trips = TripsLoader.load();
        stopTimes = StopTimesLoader.load();

        System.out.println("Stops loaded: " + (stops == null ? 0 : stops.size()));
        System.out.println("Trips loaded: " + (trips == null ? 0 : trips.size()));

        matcher = new TripMatcher(trips);
        stopTripMapper = new StopTripMapper(stopTimes, matcher);
        routeService = new RouteService(trips, stopTimes, stops);

        // Load calendar_dates.txt
        TripServiceCalendar tripServiceCalendar;
        try {
            tripServiceCalendar = CalendarLoader.load();
        } catch (Exception e) {
            System.out.println("Could not load calendar_dates: " + e.getMessage());
            tripServiceCalendar = new TripServiceCalendar();
        }

        // Initialize ArrivalService
        arrivalService = new ArrivalService(matcher, stopTripMapper, tripServiceCalendar);

        // Initialize view
        view = new MainView();
        view.init();
        view.setAllStops(stops);

        setupSearchPanel();
        setupStopClickListener();

        view.addWaypointClickListener();
        MapOverlayManager.updateMap(view.getMapViewer(), Collections.emptyList(), Collections.emptyList(), trips);

        // Start RealtimeService
        RealtimeService.setMode(mode);
        RealtimeService.startPolling();

        startRealtimeUpdates();

        System.out.println("Application started successfully");
    }

    private void setupStopClickListener() {
        view.setStopClickListener(stop -> {
            if (stop == null) return;
            handleStopSelection(stop);
        });
    }

    private void setupSearchPanel() {
        // Prepare lines list
        List<Stop> linesList = trips.stream()
            .map(t -> t.getRouteId() + " - " + t.getTripHeadsign())
            .distinct()
            .map(lineName -> {
                Stop fakeLine = new Stop(
                    "fake-" + lineName.replace(" ", ""),
                    "",
                    lineName,
                    0.0,
                    0.0
                );
                fakeLine.markAsFakeLine();
                return fakeLine;
            })
            .collect(Collectors.toList());

        view.setSearchData(stops, linesList);
        view.getSearchButton().addActionListener(e -> view.showSearchOverlay());

        view.setOnSearchSelect(stop -> {
            if (stop != null) {
                handleStopSelection(stop);
            }
        });
    }

    private void handleStopSelection(Stop stop) {
        if (stop.isFakeLine()) {
            handleLineSelection(stop);
        } else {
            MapOverlayManager.clearRoute();
            MapOverlayManager.setVisibleStops(Collections.singletonList(stop));
            centerOnStop(stop);
            showFloatingArrivals(stop);
            refreshMapOverlay();
        }
    }

    private void handleLineSelection(Stop fakeLine) {
        String lineName = fakeLine.getStopName();
        String[] parts = lineName.split(" - ", 2);
        String routeId = parts[0].trim();
        String headsign = parts.length > 1 ? parts[1].trim() : null;

        System.out.println("Line selected: " + routeId + " -> " + headsign);

        List<Stop> routeStops = routeService.getStopsForRouteAndHeadsign(routeId, headsign);

        if (routeStops.isEmpty()) {
            routeStops = routeService.getStopsForRoute(routeId);
        }

        if (routeStops.isEmpty()) {
            System.out.println("No stops found for line " + routeId);
            return;
        }

        System.out.println("Stops found for " + routeId + ": " + routeStops.size());

        MapOverlayManager.setRoute(routeStops);
        refreshMapOverlay();
        fitMapToRoute(routeStops);
        view.hideFloatingPanel();
    }

    private void centerOnStop(Stop stop) {
        if (stop.getStopLat() == 0.0 && stop.getStopLon() == 0.0) return;
        GeoPosition pos = new GeoPosition(stop.getStopLat(), stop.getStopLon());
        view.getMapViewer().setAddressLocation(pos);
        view.getMapViewer().setZoom(1);
    }

    private void fitMapToRoute(List<Stop> routeStops) {
        if (routeStops == null || routeStops.isEmpty()) return;

        double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;

        for (Stop s : routeStops) {
            minLat = Math.min(minLat, s.getStopLat());
            maxLat = Math.max(maxLat, s.getStopLat());
            minLon = Math.min(minLon, s.getStopLon());
            maxLon = Math.max(maxLon, s.getStopLon());
        }

        int middleIndex = routeStops.size() / 2;
        Stop middleStop = routeStops.get(middleIndex);
        view.getMapViewer().setAddressLocation(new GeoPosition(middleStop.getStopLat(), middleStop.getStopLon()));

        double latDiff = maxLat - minLat;
        double lonDiff = maxLon - minLon;
        double maxDiff = Math.max(latDiff, lonDiff);

        int zoom;
        if (maxDiff > 0.3) zoom = 8;
        else if (maxDiff > 0.15) zoom = 7;
        else if (maxDiff > 0.08) zoom = 6;
        else if (maxDiff > 0.04) zoom = 5;
        else if (maxDiff > 0.02) zoom = 4;
        else zoom = 3;

        view.getMapViewer().setZoom(zoom);
    }

    private void showFloatingArrivals(Stop stop) {
        List<String> arrivi = arrivalService.computeArrivalsForStop(stop.getStopId(), mode, currentFeedTs);
        showPanel(stop, arrivi);
    }

    private void showPanel(Stop stop, List<String> arrivi) {
        GeoPosition anchorGeo = new GeoPosition(stop.getStopLat(), stop.getStopLon());
        Point2D p2d = view.getMapViewer().convertGeoPositionToPoint(anchorGeo);
        SwingUtilities.invokeLater(() -> view.showFloatingPanel(stop.getStopName(), arrivi, p2d, anchorGeo));
    }

    private void refreshMapOverlay() {
        GtfsRealtime.FeedMessage vpFeed = RealtimeService.getLatestVehiclePositions();
        List<VehiclePosition> positions;
        try {
            positions = (mode == ConnectionMode.ONLINE)
                ? GtfsParser.parseVehiclePositions(vpFeed)
                : StaticSimulator.simulateAllTrips();
        } catch (Exception e) {
            positions = Collections.emptyList();
        }

        final List<VehiclePosition> busPositions = positions;
        SwingUtilities.invokeLater(() -> MapOverlayManager.updateMap(
                view.getMapViewer(), Collections.emptyList(), busPositions, trips));
    }

    private void startRealtimeUpdates() {
        if (realtimeTimer != null) {
            realtimeTimer.cancel();
        }
        realtimeTimer = new Timer("realtime-updates", true);

        realtimeTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                GtfsRealtime.FeedMessage tuFeed = RealtimeService.getLatestTripUpdates();
                GtfsRealtime.FeedMessage vpFeed = RealtimeService.getLatestVehiclePositions();

                try {
                    long tsTU = (tuFeed != null && tuFeed.hasHeader() && tuFeed.getHeader().hasTimestamp())
                                ? tuFeed.getHeader().getTimestamp()
                                : Instant.now().getEpochSecond();
                    currentFeedTs = tsTU;
                } catch (Exception ignored) {}

                if (mode == ConnectionMode.ONLINE) {
                    try {
                        List<TripUpdateRecord> updates = GtfsParser.parseTripUpdates(tuFeed, stopTripMapper, currentFeedTs);
                        arrivalService.updateRealtimeArrivals(updates);
                    } catch (Exception ex) {
                        System.out.println("Error parsing TripUpdates RT: " + ex.getMessage());
                    }
                }

                List<VehiclePosition> computedPositions;
                try {
                    computedPositions = (mode == ConnectionMode.ONLINE)
                        ? GtfsParser.parseVehiclePositions(vpFeed)
                        : StaticSimulator.simulateAllTrips();
                } catch (Exception e) {
                    mode = ConnectionMode.OFFLINE;
                    computedPositions = StaticSimulator.simulateAllTrips();
                }

                final List<VehiclePosition> busPositions = computedPositions;
                SwingUtilities.invokeLater(() -> MapOverlayManager.updateMap(
                        view.getMapViewer(), Collections.emptyList(), busPositions, trips));
            }
        }, 0, 30_000);
    }
}

