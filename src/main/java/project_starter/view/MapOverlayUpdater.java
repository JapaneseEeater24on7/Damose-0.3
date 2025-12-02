package project_starter.view;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.WaypointPainter;

import project_starter.datas.Stops;
import project_starter.datas.Trips;
import project_starter.model.BusWaypoint;
import project_starter.model.StopWaypoint;
import project_starter.model.VehiclePosition;
import project_starter.render.BusWaypointRenderer;
import project_starter.render.StopWaypointRenderer;

/**
 * Classe responsabile dell'aggiornamento degli overlay sulla mappa:
 * fermate e bus in tempo reale.
 */
public class MapOverlayUpdater {

    public static void updateMap(JXMapViewer mapViewer,
                                 List<Stops> stops,
                                 List<VehiclePosition> busPositions,
                                 List<Trips> trips) {

        // Fermate
        Set<StopWaypoint> stopWaypoints = new HashSet<>();
        for (Stops s : stops) {
            stopWaypoints.add(new StopWaypoint(s));
        }

        // Bus
        Set<BusWaypoint> busWaypoints = new HashSet<>();
        for (VehiclePosition vp : busPositions) {
            Trips trip = trips.stream()
                    .filter(t -> t.getTripId().equals(vp.getTripId()))
                    .findFirst()
                    .orElse(null);
            String headsign = (trip != null) ? trip.getTripHeadsign() : vp.getTripId();
            busWaypoints.add(new BusWaypoint(vp, headsign));
        }

        // Painter fermate
        WaypointPainter<StopWaypoint> stopPainter = new WaypointPainter<>();
        stopPainter.setWaypoints(stopWaypoints);
        stopPainter.setRenderer(new StopWaypointRenderer());

        // Painter bus
        WaypointPainter<BusWaypoint> busPainter = new WaypointPainter<>();
        busPainter.setWaypoints(busWaypoints);
        busPainter.setRenderer(new BusWaypointRenderer());

        // Overlay combinato
        mapViewer.setOverlayPainter((g, map, w, h) -> {
            stopPainter.paint(g, map, w, h);
            busPainter.paint(g, map, w, h);
        });
    }
}
