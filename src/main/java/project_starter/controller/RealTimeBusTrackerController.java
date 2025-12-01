package project_starter.controller;

import java.awt.geom.Point2D;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;

import org.jxmapviewer.viewer.GeoPosition;

import project_starter.ConnectionMode;
import project_starter.SimpleDocumentListener;
import project_starter.StaticSimulator;
import project_starter.datas.CalendarLoader;
import project_starter.datas.StopTime;
import project_starter.datas.StopTimesLoader;
import project_starter.datas.StopTripMapper;
import project_starter.datas.Stops;
import project_starter.datas.StopsLoader;
import project_starter.datas.TripUpdateRecord;
import project_starter.datas.Trips;
import project_starter.datas.TripsLoader;
import project_starter.datas.TripMatcher;
import project_starter.datas.TripIdUtils;
import project_starter.model.GTFSFetcher;
import project_starter.model.VehiclePosition;
import project_starter.view.MapOverlayUpdater;
import project_starter.view.RealTimeBusTrackerView;

/**
 * Controller che mostra per ogni fermata la prossima corsa per linea (routeId),
 * risolvendo il routeId solo tramite TripMatcher.matchByTripId (nessuna estrazione numerica).
 *
 * Se non è possibile risolvere un feedTripId in un Trips (e quindi in un routeId),
 * la predizione realtime per quel feedTripId viene ignorata nella lista per linea.
 */
public class RealTimeBusTrackerController {

    private static final int IN_ARRIVO_THRESHOLD_MIN = 2;
    private static final int MAX_RT_RESULTS = 12; // massimo predizioni RT da mostrare per fermata
    private static final int MAX_PREDICTION_DISTANCE_MIN = 6 * 60; // 6 ore

    private List<Stops> fermate;
    private List<Trips> trips;
    private TripMatcher matcher;
    private StopTripMapper stopTripMapper;
    private ConnectionMode mode = ConnectionMode.ONLINE;
    private RealTimeBusTrackerView view;

    // snapshot realtime: feedTripId -> (stopId -> arrivalEpochSeconds)
    private final Map<String, Map<String, Long>> realtimeArrivals = new HashMap<>();
    // cache feedTripId -> routeId (null se non risolto)
    private final Map<String, String> feedTripToRoute = new HashMap<>();

    private Timer realtimeTimer;
    private long currentFeedTs = Instant.now().getEpochSecond();
    private project_starter.datas.TripServiceCalendar tripServiceCalendar = new project_starter.datas.TripServiceCalendar();

    public void start() {
        System.out.println("Avvio applicazione...");

        fermate = StopsLoader.load("/gtfs_static/stops.txt");
        trips = TripsLoader.load("/gtfs_static/trips.txt");

        matcher = new TripMatcher(trips);

        List<StopTime> stopTimes = StopTimesLoader.load("/gtfs_static/stop_times.txt");
        stopTripMapper = new StopTripMapper(stopTimes, matcher);

        try {
            this.tripServiceCalendar = CalendarLoader.loadFromCalendarDates("/gtfs_static/calendar_dates.txt");
        } catch (Exception e) {
            System.out.println("Impossibile caricare calendar_dates: " + e.getMessage());
            this.tripServiceCalendar = new project_starter.datas.TripServiceCalendar();
        }

        view = new RealTimeBusTrackerView();
        view.init();
        view.setAllStops(fermate);

        setupSearchPanel();

        view.setStopClickListener(stop -> {
            if (stop == null) return;
            view.setStopInfo(stop);

            if (!stop.isFakeLine()) {
                List<Trips> linee = stopTripMapper.getTripsForStop(stop.getStopId());
                if (linee != null && !linee.isEmpty()) {
                    String infoLinee = linee.stream()
                        .map(t -> t.getRouteId() + " - " + t.getTripHeadsign())
                        .distinct()
                        .collect(Collectors.joining(", "));
                    view.showLinesInfo("Linee che passano: " + infoLinee);
                } else {
                    view.showLinesInfo("Nessuna linea trovata");
                }

                centerOnStop(stop);
                showFloatingArrivals(stop);
            } else {
                view.showLinesInfo("Linea selezionata: " + stop.getStopName());
            }
        });

        view.addWaypointClickListener();

        MapOverlayUpdater.updateMap(view.getMapViewer(), fermate, Collections.emptyList(), trips);

        GestoreRealTime.setMode(mode);
        GestoreRealTime.startAggiornamento();

        startRealtimeUpdates();

        System.out.println("Applicazione avviata correttamente");
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
                if (stop == null) return;

                view.setStopInfo(stop);

                if (!stop.isFakeLine()) {
                    List<Trips> linee = stopTripMapper.getTripsForStop(stop.getStopId());
                    if (linee != null && !linee.isEmpty()) {
                        String infoLinee = linee.stream()
                            .map(t -> t.getRouteId() + " - " + t.getTripHeadsign())
                            .distinct()
                            .collect(Collectors.joining(", "));
                        view.showLinesInfo("Linee che passano: " + infoLinee);
                    } else {
                        view.showLinesInfo("Nessuna linea trovata");
                    }

                    centerOnStop(stop);
                    showFloatingArrivals(stop);
                } else {
                    view.showLinesInfo("Linea selezionata: " + stop.getStopName());
                }
            }
        });
    }

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

    private void centerOnStop(Stops stop) {
        if (stop.getStopLat() == 0.0 && stop.getStopLon() == 0.0) return;
        GeoPosition pos = new GeoPosition(stop.getStopLat(), stop.getStopLon());
        view.getMapViewer().setAddressLocation(pos);
        view.getMapViewer().setZoom(1);
    }

    // ============================
    // Mostra arrivi realtime per la fermata; statici come fallback
    // ============================
    private void showFloatingArrivals(Stops stop) {
        // 1) raccogli RT per la stop, aggregando per route (routeId ottenuto solo da Trips)
        Map<String, Long> rtByRoute = getRealtimeArrivalsByRouteForStop(stop.getStopId(), MAX_RT_RESULTS);
        List<String> displayLines = new ArrayList<>();

        if (!rtByRoute.isEmpty()) {
            for (Map.Entry<String, Long> e : rtByRoute.entrySet()) {
                String route = e.getKey();
                long epoch = e.getValue();
                displayLines.add(formatLineText(route, epoch));
            }
            displayLines.add("--- statici (fallback) ---");
        }

        // 2) fallback statici (solo se non ci sono RT)
        if (rtByRoute.isEmpty()) {
            List<StopTime> times = stopTripMapper.getStopTimesForStop(stop.getStopId());
            if (times != null && !times.isEmpty()) {
                Map<String, StopTime> primiArrivi = new HashMap<>();
                for (StopTime st : times) {
                    Trips trip = matcher.matchByTripId(st.getTripId());
                    if (trip == null) continue;
                    String routeId = trip.getRouteId();

                    LocalTime arr = st.getArrivalTime();
                    if (arr == null) continue;
                    long diff = Duration.between(LocalTime.now(), arr).toMinutes();
                    if (diff < 0 || diff > 60) continue;
                    StopTime esistente = primiArrivi.get(routeId);
                    if (esistente == null || arr.isBefore(esistente.getArrivalTime())) {
                        primiArrivi.put(routeId, st);
                    }
                }

                List<String> staticLines = primiArrivi.entrySet().stream()
                    .map(entry -> {
                        StopTime st = entry.getValue();
                        Trips trip = matcher.matchByTripId(st.getTripId());
                        if (trip == null) return null;
                        return buildArrivalText(trip, st, null); // predictedEpoch null -> statico
                    })
                    .filter(Objects::nonNull)
                    .sorted()
                    .collect(Collectors.toList());
                displayLines.addAll(staticLines);
            }
        }

        if (displayLines.isEmpty()) displayLines.add("Nessun arrivo imminente");

        showPanel(stop, displayLines);
    }

    /**
     * Aggrega le predizioni realtime per stopId raggruppando per route (routeId).
     * Usa solo routeId risolti tramite TripMatcher.matchByTripId; se non si risolve,
     * la predizione viene ignorata (non si mostrano numeri a caso).
     */
    private Map<String, Long> getRealtimeArrivalsByRouteForStop(String stopId, int maxResults) {
        Map<String, Long> routeToEpoch = new HashMap<>();
        synchronized (realtimeArrivals) {
            for (Map.Entry<String, Map<String, Long>> e : realtimeArrivals.entrySet()) {
                String feedTrip = e.getKey();
                Map<String, Long> byStop = e.getValue();
                if (byStop == null) continue;
                Long epoch = byStop.get(stopId);
                if (epoch == null || epoch <= 0) continue;

                // sanity: scarta predizioni troppo lontane rispetto al now
                long now = Instant.now().getEpochSecond();
                long deltaMin = Math.abs((epoch - now) / 60);
                if (deltaMin > MAX_PREDICTION_DISTANCE_MIN) continue;

                // risolvi route per questo feedTrip (cache in feedTripToRoute)
                String route = feedTripToRoute.get(feedTrip);
                if (!feedTripToRoute.containsKey(feedTrip)) {
                    route = resolveRouteForFeedTripStrict(feedTrip);
                    // cache: può essere null (non risolto)
                    feedTripToRoute.put(feedTrip, route);
                } else {
                    route = feedTripToRoute.get(feedTrip);
                }

                // se non abbiamo route (non risolto), ignoriamo questa predizione
                if (route == null) continue;

                // aggrega: tieni il più vicino per route
                Long existing = routeToEpoch.get(route);
                if (existing == null || epoch < existing) {
                    routeToEpoch.put(route, epoch);
                }
            }
        }

        // ordina per epoch e limita
        return routeToEpoch.entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .limit(maxResults)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (a,b)->a,
                LinkedHashMap::new
            ));
    }

    // formatta la riga mostrata: "88 - 10 min" o "88 - In arrivo"
    private String formatLineText(String route, long predictedEpoch) {
        long now = Instant.now().getEpochSecond();
        long diffMin = Math.max(0, (predictedEpoch - now) / 60);
        String timeStr = Instant.ofEpochSecond(predictedEpoch).atZone(ZoneId.systemDefault()).toLocalTime().toString();
        if (diffMin <= IN_ARRIVO_THRESHOLD_MIN) {
            return route + " - In arrivo (" + timeStr + ")";
        } else {
            return route + " - " + diffMin + " min (" + timeStr + ")";
        }
    }

    /**
     * Risoluzione stretta: prova solo match esatto e match sulla versione "normalized feed"
     * (rimuove solo prefisso tipo "0#"). Non usa estrazioni numeriche.
     * Restituisce null se non è possibile ottenere un routeId valido.
     */
    private String resolveRouteForFeedTripStrict(String feedTripId) {
        if (feedTripId == null) return null;

        // 1) match esatto con Trips
        Trips t = matcher.matchByTripId(feedTripId);
        if (t != null && t.getRouteId() != null && !t.getRouteId().isEmpty()) {
            return t.getRouteId();
        }

        // 2) match con versione normalizzata (rimuove solo prefisso tipo "0#")
        String normalized = TripIdUtils.normalizeFeedTripId(feedTripId);
        if (normalized != null && !normalized.equals(feedTripId)) {
            t = matcher.matchByTripId(normalized);
            if (t != null && t.getRouteId() != null && !t.getRouteId().isEmpty()) {
                return t.getRouteId();
            }
        }

        // non risolto: ritorna null (ignoriamo predizione per visualizzazione per linea)
        return null;
    }

    private String buildArrivalText(Trips trip, StopTime st, Long predictedEpoch) {
        LocalTime scheduled = st.getArrivalTime();
        if (scheduled == null) return trip.getRouteId() + " - orario non disponibile";

        long scheduledEpoch = computeScheduledEpochForFeed(st, currentFeedTs);
        long nowEpoch = Instant.now().getEpochSecond();

        if (predictedEpoch != null) {
            long deltaMinFromNow = (predictedEpoch - nowEpoch) / 60;
            if (Math.abs(deltaMinFromNow) > MAX_PREDICTION_DISTANCE_MIN) {
                predictedEpoch = null;
            }
            long deltaMinSched = Math.abs((predictedEpoch - scheduledEpoch) / 60);
            if (deltaMinSched > MAX_PREDICTION_DISTANCE_MIN) {
                predictedEpoch = null;
            }
        }

        if (mode == ConnectionMode.ONLINE && predictedEpoch != null) {
            long diffFromNowMin = Math.max(0, (predictedEpoch - nowEpoch) / 60);
            long delayMin = (predictedEpoch - scheduledEpoch) / 60;

            String status;
            if (delayMin > 0) status = "ritardo di " + Math.abs(delayMin) + " min";
            else if (delayMin < 0) status = "anticipo di " + Math.abs(delayMin) + " min";
            else status = "in orario";

            if (diffFromNowMin <= IN_ARRIVO_THRESHOLD_MIN) {
                return trip.getRouteId() + " - In arrivo (" + status + ")";
            } else {
                return trip.getRouteId() + " - " + diffFromNowMin + " min (" + status + ")";
            }
        } else {
            long diffStaticMin = Math.max(0, Duration.between(LocalTime.now(), scheduled).toMinutes());
            return trip.getRouteId() + " - " + diffStaticMin + " min (statico)";
        }
    }

    private void showPanel(Stops stop, List<String> arrivi) {
        GeoPosition anchorGeo = new GeoPosition(stop.getStopLat(), stop.getStopLon());
        Point2D p2d = view.getMapViewer().convertGeoPositionToPoint(anchorGeo);
        SwingUtilities.invokeLater(() -> view.showFloatingPanel(stop.getStopName(), arrivi, p2d, anchorGeo));
    }

    // ============================
    // Realtime updates (popola realtimeArrivals e feedTripToRoute)
    // ============================
    private void startRealtimeUpdates() {
        if (realtimeTimer != null) {
            realtimeTimer.cancel();
        }
        realtimeTimer = new Timer("realtime-updates", true);

        realtimeTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                var tuFeed = GestoreRealTime.getLatestTripUpdates();
                var vpFeed = GestoreRealTime.getLatestVehiclePositions();

                try {
                    long feedTs = (tuFeed != null && tuFeed.hasHeader() && tuFeed.getHeader().hasTimestamp())
                                  ? tuFeed.getHeader().getTimestamp()
                                  : Instant.now().getEpochSecond();
                    currentFeedTs = feedTs;
                } catch (Exception ignored) {}

                if (mode == ConnectionMode.ONLINE) {
                    try {
                        List<TripUpdateRecord> updates = GTFSFetcher.parseTripUpdates(tuFeed, stopTripMapper, currentFeedTs);
                        // log essenziale: numero di TU ricevuti in questo ciclo
                        System.out.println("TripUpdates ricevuti: " + updates.size());

                        synchronized (realtimeArrivals) {
                            realtimeArrivals.clear();
                            for (TripUpdateRecord u : updates) {
                                String feedTrip = u.getTripId();
                                realtimeArrivals
                                    .computeIfAbsent(feedTrip, k -> new HashMap<>())
                                    .put(u.getStopId(), u.getArrivalEpochSeconds());

                                // cache route: risolvi strettamente e memorizza (null se non risolto)
                                if (!feedTripToRoute.containsKey(feedTrip)) {
                                    String route = resolveRouteForFeedTripStrict(feedTrip);
                                    feedTripToRoute.put(feedTrip, route);
                                }
                            }
                        }
                    } catch (Exception ex) {
                        System.out.println("Impossibile parsare TripUpdates realtime: " + ex.getMessage());
                    }
                }

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

    // compute scheduled epoch (feedDate-1, feedDate, feedDate+1) best match
    private long computeScheduledEpochForFeed(StopTime st, long feedEpochSeconds) {
        LocalTime scheduled = st.getArrivalTime();
        if (scheduled == null) return -1;
        ZoneId zone = ZoneId.systemDefault();
        Instant feedInstant = Instant.ofEpochSecond(feedEpochSeconds);
        LocalDate feedDate = feedInstant.atZone(zone).toLocalDate();

        long best = -1;
        long bestDiff = Long.MAX_VALUE;
        for (int delta = -1; delta <= 1; delta++) {
            LocalDate candidateDate = feedDate.plusDays(delta);
            Instant candInstant = scheduled.atDate(candidateDate).atZone(zone).toInstant();
            long diff = Math.abs(candInstant.getEpochSecond() - feedEpochSeconds);
            if (diff < bestDiff) {
                bestDiff = diff;
                best = candInstant.getEpochSecond();
            }
        }
        return best;
    }
}
