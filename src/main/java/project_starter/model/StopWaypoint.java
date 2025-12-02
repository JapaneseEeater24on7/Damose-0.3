package project_starter.model;

import org.jxmapviewer.viewer.DefaultWaypoint;
import org.jxmapviewer.viewer.GeoPosition;

import project_starter.datas.Stops;

/**
 * Waypoint rappresentante una fermata sulla mappa.
 */
public class StopWaypoint extends DefaultWaypoint {

    private final Stops stop;

    public StopWaypoint(Stops stop) {
        super(new GeoPosition(stop.getStopLat(), stop.getStopLon()));
        this.stop = stop;
    }

    public Stops getStop() {
        return stop;
    }

    @Override
    public String toString() {
        return stop.getStopName() + " (" + stop.getStopId() + ")";
    }
}

