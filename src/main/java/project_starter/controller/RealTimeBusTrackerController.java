package project_starter.controller;

import java.awt.geom.Point2D;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;

import org.jxmapviewer.viewer.GeoPosition;

import com.google.transit.realtime.GtfsRealtime;

import project_starter.datas.CalendarLoader;
import project_starter.datas.StopTime;
import project_starter.datas.StopTimesLoader;
import project_starter.datas.StopTripMapper;
import project_starter.datas.Stops;
import project_starter.datas.StopsLoader;
import project_starter.datas.TripMatcher;
import project_starter.datas.TripServiceCalendar;
import project_starter.datas.TripUpdateRecord;
import project_starter.datas.Trips;
import project_starter.datas.TripsLoader;
import project_starter.model.ConnectionMode;
import project_starter.model.GTFSFetcher;
import project_starter.model.StaticSimulator;
import project_starter.model.VehiclePosition;
import project_starter.view.MapOverlayUpdater;
import project_starter.view.RealTimeBusTrackerView;
import project_starter.view.SimpleDocumentListener;

/**
 * Controller principale dell'applicazione Rome Bus Tracker.
 * Coordina la view, i dati statici GTFS e gli aggiornamenti real-time.
 */
public class RealTimeBusTrackerController {

    private List<Stops> fermate;
    private List<Trips> trips;
    private TripMatcher matcher;
    private StopTripMapper stopTripMapper;
    private ConnectionMode mode = ConnectionMode.ONLINE;
    private RealTimeBusTrackerView view;

    private ArrivalService arrivalService;
    private Timer realtimeTimer;
    private long currentFeedTs = Instant.now().getEpochSecond();

    // ============================
    // Avvio applicazione
    // ============================
    public void start() {
        System.out.println("Avvio applicazione...");

        // Carico statici
        fermate = StopsLoader.load("/gtfs_static/stops.txt");
        trips = TripsLoader.load("/gtfs_static/trips.txt");
        System.out.println("Stops caricati: " + (fermate == null ? 0 : fermate.size()));
        System.out.println("Trips caricati: " + (trips == null ? 0 : trips.size()));

        matcher = new TripMatcher(trips);

        List<StopTime> stopTimes = StopTimesLoader.load("/gtfs_static/stop_times.txt");
        stopTripMapper = new StopTripMapper(stopTimes, matcher);

        // Carica calendar_dates.txt
        TripServiceCalendar tripServiceCalendar;
        try {
            tripServiceCalendar = CalendarLoader.loadFromCalendarDates("/gtfs_static/calendar_dates.txt");
        } catch (Exception e) {
            System.out.println("Impossibile caricare calendar_dates: " + e.getMessage());
            tripServiceCalendar = new TripServiceCalendar();
        }

        // Inizializza ArrivalService
        arrivalService = new ArrivalService(matcher, stopTripMapper, tripServiceCalendar);

        // Inizializza view
        view = new RealTimeBusTrackerView();
        view.init();
        view.setAllStops(fermate);

        setupSearchPanel();
        setupStopClickListener();

        view.addWaypointClickListener();
        MapOverlayUpdater.updateMap(view.getMapViewer(), fermate, Collections.emptyList(), trips);

        // Avvio GestoreRealTime
        GestoreRealTime.setMode(mode);
        GestoreRealTime.startAggiornamento();

        startRealtimeUpdates();

        System.out.println("Applicazione avviata correttamente");
    }

    // ============================
    // Setup listeners
    // ============================
    private void setupStopClickListener() {
        view.setStopClickListener(stop -> {
            if (stop == null) return;
            handleStopSelection(stop);
        });
    }

    private void setupSearchPanel() {
        view.getSearchButton().addActionListener(e -> view.toggleSidePanel());

        view.getSearchField().getDocument().addDocumentListener(new SimpleDocumentListener(() -> {
            String query = view.getSearchField().getText().trim().toLowerCase();
            filterStopsAndLines(query);
        }));

        view.getStopList().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Stops stop = view.getStopList().getSelectedValue();
                if (stop != null) {
                    handleStopSelection(stop);
                }
            }
        });
    }

    private void handleStopSelection(Stops stop) {
        if (!stop.isFakeLine()) {
            centerOnStop(stop);
            showFloatingArrivals(stop);
        }
    }

    // ============================
    // Filtro ricerca
    // ============================
    private void filterStopsAndLines(String query) {
        view.clearStopList();
        if (query.isEmpty()) return;

        if (view.isStopsMode()) {
            fermate.stream()
                .filter(s -> s.getStopName().toLowerCase().contains(query)
                          || s.getStopId().toLowerCase().contains(query))
                .limit(100)
                .forEach(view::addStopToList);
        } else if (view.isLinesMode()) {
            List<Trips> matchingTrips = trips.stream()
                .filter(t -> t.getRouteId().toLowerCase().contains(query))
                .collect(Collectors.toList());

            matchingTrips.stream()
                .map(t -> t.getRouteId() + " - " + t.getTripHeadsign())
                .distinct()
                .forEach(lineName -> {
                    Stops cleanLine = new Stops(
                        "fake-" + lineName.replace(" ", ""),
                        "",
                        lineName,
                        0.0,
                        0.0
                    );
                    cleanLine.markAsFakeLine();
                    view.addStopToList(cleanLine);
                });
        }
    }

    // ============================
    // Navigazione mappa
    // ============================
    private void centerOnStop(Stops stop) {
        if (stop.getStopLat() == 0.0 && stop.getStopLon() == 0.0) return;
        GeoPosition pos = new GeoPosition(stop.getStopLat(), stop.getStopLon());
        view.getMapViewer().setAddressLocation(pos);
        view.getMapViewer().setZoom(1);
    }

    // ============================
    // Mostra arrivi (delegato ad ArrivalService)
    // ============================
    private void showFloatingArrivals(Stops stop) {
        List<String> arrivi = arrivalService.computeArrivalsForStop(stop.getStopId(), mode, currentFeedTs);
        showPanel(stop, arrivi);
    }

    private void showPanel(Stops stop, List<String> arrivi) {
        GeoPosition anchorGeo = new GeoPosition(stop.getStopLat(), stop.getStopLon());
        Point2D p2d = view.getMapViewer().convertGeoPositionToPoint(anchorGeo);
        SwingUtilities.invokeLater(() -> view.showFloatingPanel(stop.getStopName(), arrivi, p2d, anchorGeo));
    }

    // ============================
    // Realtime updates
    // ============================
    private void startRealtimeUpdates() {
        if (realtimeTimer != null) {
            realtimeTimer.cancel();
        }
        realtimeTimer = new Timer("realtime-updates", true);

        realtimeTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                GtfsRealtime.FeedMessage tuFeed = GestoreRealTime.getLatestTripUpdates();
                GtfsRealtime.FeedMessage vpFeed = GestoreRealTime.getLatestVehiclePositions();

                // Aggiorna timestamp feed
                try {
                    long tsTU = (tuFeed != null && tuFeed.hasHeader() && tuFeed.getHeader().hasTimestamp())
                                ? tuFeed.getHeader().getTimestamp()
                                : Instant.now().getEpochSecond();
                    currentFeedTs = tsTU;
                } catch (Exception ignored) {}

                // Aggiorna dati RT
                if (mode == ConnectionMode.ONLINE) {
                    try {
                        List<TripUpdateRecord> updates = GTFSFetcher.parseTripUpdates(tuFeed, stopTripMapper, currentFeedTs);
                        arrivalService.updateRealtimeArrivals(updates);
                    } catch (Exception ex) {
                        System.out.println("Errore parsing TripUpdates RT: " + ex.getMessage());
                    }
                }

                // Vehicle positions / overlay
                List<VehiclePosition> computedPositions;
                try {
                    computedPositions = (mode == ConnectionMode.ONLINE)
                        ? GTFSFetcher.parseVehiclePositions(vpFeed)
                        : StaticSimulator.simulateAllTrips();
                } catch (Exception e) {
                    mode = ConnectionMode.OFFLINE;
                    computedPositions = StaticSimulator.simulateAllTrips();
                }

                final List<VehiclePosition> busPositions = computedPositions;
                SwingUtilities.invokeLater(() -> MapOverlayUpdater.updateMap(view.getMapViewer(), fermate, busPositions, trips));
            }
        }, 0, 15_000);
    }
}
