package damose.data.model;

/**
 * Represents a bus stop from GTFS static data.
 */
public class Stop {

    private final String stopId;
    private final String stopCode;
    private final String stopName;
    private final double stopLat;
    private final double stopLon;

    // Flag to distinguish real stops from fake line entries (used in search)
    private boolean isFakeLine = false;

    public Stop(String stopId, String stopCode, String stopName, double stopLat, double stopLon) {
        this.stopId = stopId;
        this.stopCode = stopCode;
        this.stopName = stopName;
        this.stopLat = stopLat;
        this.stopLon = stopLon;
    }

    // -------- Getters --------
    public String getStopId() {
        return stopId;
    }

    public String getStopCode() {
        return stopCode;
    }

    public String getStopName() {
        return stopName;
    }

    public double getStopLat() {
        return stopLat;
    }

    public double getStopLon() {
        return stopLon;
    }

    // -------- Fake Line Support --------
    public void markAsFakeLine() {
        this.isFakeLine = true;
    }

    public boolean isFakeLine() {
        return isFakeLine;
    }

    @Override
    public String toString() {
        return isFakeLine ? stopName : stopId + " - " + stopName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Stop stop = (Stop) o;
        return stopId != null ? stopId.equals(stop.stopId) : stop.stopId == null;
    }

    @Override
    public int hashCode() {
        return stopId != null ? stopId.hashCode() : 0;
    }
}

