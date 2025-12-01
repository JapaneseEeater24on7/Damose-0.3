package project_starter.model;

import com.google.transit.realtime.GtfsRealtime;
import org.jxmapviewer.viewer.GeoPosition;
import project_starter.datas.StopTripMapper;
import project_starter.datas.TripIdUtils;
import project_starter.datas.TripUpdateRecord;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * GTFSFetcher aggiornato:
 * - parsing robusto da raw bytes o da FeedMessage
 * - nessun log riga-per-riga
 * - normalizeEpoch gestisce millisecondi vs secondi
 * - mappe stop_sequence -> stopId tramite StopTripMapper quando necessario
 */
public class GTFSFetcher {

    private GTFSFetcher() {}

    private static long normalizeEpoch(long raw) {
        if (raw <= 0) return -1;

        // Se sembra millisecondi (>= 10^12) converti in secondi
        if (raw >= 1_000_000_000_000L) {
            return raw / 1000L;
        }

        // Se sembra già in secondi (>= 10^9) usalo così com'è
        if (raw >= 1_000_000_000L) {
            return raw;
        }

        // valori non plausibili
        return -1;
    }

    /**
     * Parsea TripUpdates da un array di byte (raw protobuf) e restituisce una lista di TripUpdateRecord.
     * Questo metodo è utile quando il downloader legge i raw bytes (eventualmente decompressi).
     */
    public static List<TripUpdateRecord> parseTripUpdatesFromBytes(byte[] rawBytes, StopTripMapper stopTripMapper, Long feedHeaderTs) {
        if (rawBytes == null || rawBytes.length == 0) return new ArrayList<>();
        try (InputStream in = new ByteArrayInputStream(rawBytes)) {
            GtfsRealtime.FeedMessage feed = GtfsRealtime.FeedMessage.parseFrom(in);
            return parseTripUpdates(feed, stopTripMapper, feedHeaderTs);
        } catch (Exception e) {
            // errore di parsing: ritorna lista vuota (log minimo)
            System.out.println("GTFSFetcher: errore parsing TU feed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Parsea TripUpdates da un FeedMessage già costruito.
     *
     * @param feed           FeedMessage GTFS-RT (può essere null)
     * @param stopTripMapper mapper per risolvere stopId da stop_sequence
     * @param feedHeaderTs   timestamp dell'header del feed (epoch seconds) usato come riferimento
     * @return lista di TripUpdateRecord (rawTripId, stopId, arrivalEpochSeconds)
     */
    public static List<TripUpdateRecord> parseTripUpdates(GtfsRealtime.FeedMessage feed, StopTripMapper stopTripMapper, Long feedHeaderTs) {
        List<TripUpdateRecord> updates = new ArrayList<>();
        if (feed == null) return updates;

        try {
            for (GtfsRealtime.FeedEntity entity : feed.getEntityList()) {
                if (!entity.hasTripUpdate()) continue;

                GtfsRealtime.TripUpdate tu = entity.getTripUpdate();
                String rawTripId = tu.hasTrip() ? tu.getTrip().getTripId() : null;

                for (GtfsRealtime.TripUpdate.StopTimeUpdate stu : tu.getStopTimeUpdateList()) {
                    // skip schedule relationships that are not useful
                    if (stu.hasScheduleRelationship()) {
                        GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship rel = stu.getScheduleRelationship();
                        if (rel == GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SKIPPED
                                || rel == GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.NO_DATA) {
                            continue;
                        }
                    }

                    String stopId = stu.hasStopId() ? stu.getStopId().trim() : null;

                    // se stopId non è noto, prova a mappare tramite stop_sequence
                    if ((stopId == null || !stopTripMapper.isKnownStopId(stopId)) && stu.hasStopSequence()) {
                        int seq = stu.getStopSequence();
                        String mappedStop = stopTripMapper.getStopIdByTripAndSequence(rawTripId, seq);
                        if (mappedStop != null) {
                            stopId = mappedStop;
                        }
                    }

                    long rawTime = -1;
                    if (stu.hasArrival() && stu.getArrival().hasTime()) {
                        rawTime = stu.getArrival().getTime();
                    } else if (stu.hasDeparture() && stu.getDeparture().hasTime()) {
                        rawTime = stu.getDeparture().getTime();
                    }

                    long arrivalEpoch = normalizeEpoch(rawTime);
                    if (stopId != null && arrivalEpoch > 0) {
                        updates.add(new TripUpdateRecord(rawTripId, stopId, arrivalEpoch));
                    }
                }
            }
        } catch (Exception e) {
            // parsing error: ritorna quello che è stato raccolto finora
            System.out.println("GTFSFetcher: eccezione durante parseTripUpdates: " + e.getMessage());
        }
        return updates;
    }

    /**
     * Parsea VehiclePositions da un FeedMessage GTFS-RT.
     * Restituisce una lista di VehiclePosition (tripId, vehicleId, GeoPosition, stopSequence).
     */
    public static List<project_starter.model.VehiclePosition> parseVehiclePositions(GtfsRealtime.FeedMessage feed) {
        List<project_starter.model.VehiclePosition> positions = new ArrayList<>();
        if (feed == null) return positions;

        try {
            for (GtfsRealtime.FeedEntity entity : feed.getEntityList()) {
                if (!entity.hasVehicle()) continue;

                GtfsRealtime.VehiclePosition vehicle = entity.getVehicle();

                String tripId = vehicle.hasTrip() ? vehicle.getTrip().getTripId() : null;
                String vehicleId = vehicle.hasVehicle() ? vehicle.getVehicle().getId() : null;

                double lat = vehicle.hasPosition() ? vehicle.getPosition().getLatitude() : 0.0;
                double lon = vehicle.hasPosition() ? vehicle.getPosition().getLongitude() : 0.0;
                int stopSeq = vehicle.hasCurrentStopSequence() ? vehicle.getCurrentStopSequence() : -1;

                // correzione microgradi se necessario
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

                positions.add(new project_starter.model.VehiclePosition(
                        tripId,
                        vehicleId,
                        new GeoPosition(lat, lon),
                        stopSeq
                ));
            }
        } catch (Exception e) {
            System.out.println("GTFSFetcher: eccezione durante parseVehiclePositions: " + e.getMessage());
        }
        return positions;
    }
}
