package damose.model;

import org.jxmapviewer.viewer.DefaultWaypoint;
import org.jxmapviewer.viewer.GeoPosition;

import damose.data.model.Stop;

/**
 * Waypoint representing a stop on the map.
 */
public class StopWaypoint extends DefaultWaypoint {

    private final Stop stop;

    public StopWaypoint(Stop stop) {
        super(new GeoPosition(stop.getStopLat(), stop.getStopLon()));
        this.stop = stop;
    }

    public Stop getStop() {
        return stop;
    }

    @Override
    public String toString() {
        return stop.getStopName() + " (" + stop.getStopId() + ")";
    }
}

