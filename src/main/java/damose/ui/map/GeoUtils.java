package damose.ui.map;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;

import damose.config.AppConstants;
import damose.data.model.Stop;

/**
 * Utility for geographic calculations and map interactions.
 */
public final class GeoUtils {

    private GeoUtils() {
        // Utility class
    }

    /**
     * Calculate distance between two coordinates using Haversine formula.
     * @return Distance in kilometers
     */
    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.sin(dLon/2) * Math.sin(dLon/2) *
                   Math.cos(lat1) * Math.cos(lat2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Check if a click is close to a stop on the map.
     */
    public static boolean isClickCloseToStop(JXMapViewer mapViewer, Stop stop, int clickX, int clickY) {
        var point = mapViewer.getTileFactory().geoToPixel(
                new GeoPosition(stop.getStopLat(), stop.getStopLon()),
                mapViewer.getZoom()
        );

        double dx = clickX - point.getX();
        double dy = clickY - point.getY();
        double dist = Math.sqrt(dx * dx + dy * dy);

        return dist < AppConstants.CLICK_PROXIMITY_THRESHOLD;
    }
}

