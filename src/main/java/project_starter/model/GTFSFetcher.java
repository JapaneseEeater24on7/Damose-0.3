package project_starter.model;

import java.util.ArrayList;
import java.util.List;

import org.jxmapviewer.viewer.GeoPosition;

import com.google.transit.realtime.GtfsRealtime;

import project_starter.datas.StopTripMapper;
import project_starter.datas.TripIdUtils;
import project_starter.datas.TripUpdateRecord;

/**
 * GTFSFetcher: parsing di feed GTFS-RT (TripUpdates e VehiclePositions).
 */
public class GTFSFetcher {

    private GTFSFetcher() {}

    // Normalizza epoch seconds: accetta secondi, converte millisecondi, scarta valori non plausibili
    private static long normalizeEpoch(long raw) {
        if (raw <= 0) return -1;

        // Millisecondi -> secondi
        if (raw >= 1_000_000_000_000L) {
            return raw / 1000L;
        }

        // Secondi plausibili
        if (raw >= 1_000_000_000L) {
            return raw;
        }

        // Troppo piccolo: ignora
        return -1;
    }

    /**
     * Parsea TripUpdates dal feed e restituisce una lista di TripUpdateRecord.
     */
    public static List<TripUpdateRecord> parseTripUpdates(GtfsRealtime.FeedMessage feed,
                                                          StopTripMapper stopTripMapper,
                                                          Long feedHeaderTs) {
        List<TripUpdateRecord> updates = new ArrayList<>();
        if (feed == null) return updates;

        for (GtfsRealtime.FeedEntity entity : feed.getEntityList()) {
            if (!entity.hasTripUpdate()) continue;

            GtfsRealtime.TripUpdate tu = entity.getTripUpdate();
            String rawTripId = (tu.hasTrip() && tu.getTrip().hasTripId()) ? tu.getTrip().getTripId() : null;
            String simple = TripIdUtils.normalizeSimple(rawTripId);

            for (GtfsRealtime.TripUpdate.StopTimeUpdate stu : tu.getStopTimeUpdateList()) {
                // Filtra relazioni inutili
                if (stu.hasScheduleRelationship()) {
                    GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship rel = stu.getScheduleRelationship();
                    if (rel == GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SKIPPED
                        || rel == GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.NO_DATA) {
                        continue;
                    }
                }

                // Estrarre stopId o mappare da stop_sequence
                String stopId = (stu.hasStopId() ? stu.getStopId().trim() : null);
                boolean hadStopId = stopId != null && !stopId.isBlank();
                if (!hadStopId && stu.hasStopSequence() && rawTripId != null) {
                    int seq = stu.getStopSequence();
                    String mapped = stopTripMapper.getStopIdByTripAndSequence(rawTripId, seq);
                    if (mapped == null || mapped.isBlank()) {
                        mapped = stopTripMapper.getStopIdByTripAndSequence(simple, seq);
                    }
                    if (mapped != null && !mapped.isBlank()) {
                        stopId = mapped;
                    }
                }

                // Tempo: preferisci ARRIVAL, fallback DEPARTURE
                long rawTime = -1;
                if (stu.hasArrival() && stu.getArrival().hasTime()) {
                    rawTime = stu.getArrival().getTime();
                } else if (stu.hasDeparture() && stu.getDeparture().hasTime()) {
                    rawTime = stu.getDeparture().getTime();
                }

                long arrivalEpoch = normalizeEpoch(rawTime);
                if (stopId != null && !stopId.isBlank() && arrivalEpoch > 0) {
                    updates.add(new TripUpdateRecord(rawTripId, stopId, arrivalEpoch));
                }
            }
        }

        return updates;
    }

    /**
     * Parsea VehiclePositions: usato per overlay mappa.
     */
    public static List<VehiclePosition> parseVehiclePositions(GtfsRealtime.FeedMessage feed) {
        List<VehiclePosition> positions = new ArrayList<>();
        if (feed == null) return positions;

        for (GtfsRealtime.FeedEntity entity : feed.getEntityList()) {
            if (!entity.hasVehicle()) continue;

            GtfsRealtime.VehiclePosition vehicle = entity.getVehicle();

            String tripId = (vehicle.hasTrip() && vehicle.getTrip().hasTripId()) ? vehicle.getTrip().getTripId() : null;
            String vehicleId = (vehicle.hasVehicle() && vehicle.getVehicle().hasId()) ? vehicle.getVehicle().getId() : null;

            double lat = vehicle.hasPosition() ? vehicle.getPosition().getLatitude() : 0.0;
            double lon = vehicle.hasPosition() ? vehicle.getPosition().getLongitude() : 0.0;
            int stopSeq = vehicle.hasCurrentStopSequence() ? vehicle.getCurrentStopSequence() : -1;

            // Correzione microgradi se necessario
            if (Math.abs(lat) > 90 || Math.abs(lon) > 180) {
                double latC = lat / 1_000_000.0;
                double lonC = lon / 1_000_000.0;
                if (Math.abs(latC) <= 90 && Math.abs(lonC) <= 180) {
                    lat = latC;
                    lon = lonC;
                }
            }

            if (Math.abs(lat) > 90 || Math.abs(lon) > 180) {
                continue;
            }

            positions.add(new VehiclePosition(
                tripId,
                vehicleId,
                new GeoPosition(lat, lon),
                stopSeq
            ));
        }

        return positions;
    }
}
