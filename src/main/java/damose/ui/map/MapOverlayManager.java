package damose.ui.map;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.SwingUtilities;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.WaypointPainter;

import damose.data.model.Stop;
import damose.data.model.Trip;
import damose.data.model.VehiclePosition;
import damose.model.BusWaypoint;
import damose.model.StopWaypoint;
import damose.ui.render.BusWaypointRenderer;
import damose.ui.render.RoutePainter;
import damose.ui.render.StopWaypointRenderer;

/**
 * Manages map overlays including stops, buses, and routes.
 * Optimized to prevent lag and glitches.
 */
public class MapOverlayManager {

    private static WaypointPainter<StopWaypoint> stopPainter;
    private static WaypointPainter<BusWaypoint> busPainter;
    private static final RoutePainter routePainter = new RoutePainter();

    private static Set<String> currentStopIds = new HashSet<>();
    private static Set<String> currentBusIds = new HashSet<>();

    private static final List<Stop> routeStops = new ArrayList<>();
    private static final List<Stop> visibleStops = new ArrayList<>();

    private static final Object lock = new Object();
    private static boolean initialized = false;
    private static JXMapViewer currentMap = null;

    private MapOverlayManager() {
        // Utility class
    }

    private static void initPainters(JXMapViewer mapViewer) {
        if (initialized && currentMap == mapViewer) return;

        stopPainter = new WaypointPainter<>();
        stopPainter.setRenderer(new StopWaypointRenderer());
        stopPainter.setWaypoints(new HashSet<>());

        busPainter = new WaypointPainter<>();
        busPainter.setRenderer(new BusWaypointRenderer());
        busPainter.setWaypoints(new HashSet<>());

        mapViewer.setOverlayPainter((g, map, w, h) -> {
            synchronized (lock) {
                if (routePainter.hasRoute()) {
                    routePainter.paint(g, map, w, h);
                }
                stopPainter.paint(g, map, w, h);
                busPainter.paint(g, map, w, h);
            }
        });

        initialized = true;
        currentMap = mapViewer;
    }

    public static void updateMap(JXMapViewer mapViewer,
                                 List<Stop> allStops,
                                 List<VehiclePosition> busPositions,
                                 List<Trip> trips) {

        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> updateMap(mapViewer, allStops, busPositions, trips));
            return;
        }

        initPainters(mapViewer);
        boolean needsRepaint = false;

        synchronized (lock) {
            Set<String> showIds = new HashSet<>();
            for (Stop s : visibleStops) showIds.add(s.getStopId());
            for (Stop s : routeStops) showIds.add(s.getStopId());

            if (!showIds.equals(currentStopIds)) {
                currentStopIds = new HashSet<>(showIds);

                Set<StopWaypoint> newStopWaypoints = new HashSet<>();
                for (Stop s : visibleStops) newStopWaypoints.add(new StopWaypoint(s));
                for (Stop s : routeStops) newStopWaypoints.add(new StopWaypoint(s));

                stopPainter.setWaypoints(newStopWaypoints);
                needsRepaint = true;
            }

            Set<String> newBusIds = new HashSet<>();
            for (VehiclePosition vp : busPositions) {
                if (vp.getVehicleId() != null) newBusIds.add(vp.getVehicleId());
            }

            if (!newBusIds.equals(currentBusIds)) {
                currentBusIds = newBusIds;

                Set<BusWaypoint> newBusWaypoints = new HashSet<>();
                for (VehiclePosition vp : busPositions) {
                    Trip trip = findTrip(trips, vp.getTripId());
                    String headsign = (trip != null) ? trip.getTripHeadsign() : vp.getTripId();
                    newBusWaypoints.add(new BusWaypoint(vp, headsign));
                }

                busPainter.setWaypoints(newBusWaypoints);
                needsRepaint = true;
            }
        }

        if (needsRepaint) {
            mapViewer.repaint();
        }
    }

    private static Trip findTrip(List<Trip> trips, String tripId) {
        if (tripId == null || trips == null) return null;
        for (Trip t : trips) {
            if (t.getTripId().equals(tripId)) return t;
        }
        return null;
    }

    public static void setVisibleStops(List<Stop> stops) {
        synchronized (lock) {
            visibleStops.clear();
            if (stops != null) visibleStops.addAll(stops);
            currentStopIds.clear();
        }
    }

    public static void clearVisibleStops() {
        synchronized (lock) {
            visibleStops.clear();
            currentStopIds.clear();
        }
    }

    public static void setRoute(List<Stop> stops) {
        synchronized (lock) {
            routeStops.clear();
            currentStopIds.clear();

            if (stops == null || stops.size() < 2) {
                routePainter.clearRoute();
                return;
            }

            routeStops.addAll(stops);

            List<GeoPosition> positions = new ArrayList<>();
            for (Stop stop : stops) {
                positions.add(new GeoPosition(stop.getStopLat(), stop.getStopLon()));
            }
            routePainter.setRoute(positions);
        }

        if (currentMap != null) {
            currentMap.repaint();
        }
    }

    public static void clearRoute() {
        synchronized (lock) {
            routePainter.clearRoute();
            routeStops.clear();
            currentStopIds.clear();
        }
    }

    public static boolean hasActiveRoute() {
        synchronized (lock) {
            return routePainter.hasRoute();
        }
    }

    public static void clearAll() {
        clearRoute();
        clearVisibleStops();
    }
}

