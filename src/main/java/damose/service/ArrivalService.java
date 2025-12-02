package damose.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import damose.config.AppConstants;
import damose.data.mapper.StopTripMapper;
import damose.data.mapper.TripIdUtils;
import damose.data.mapper.TripMatcher;
import damose.data.model.StopTime;
import damose.data.model.Trip;
import damose.data.model.TripServiceCalendar;
import damose.data.model.TripUpdateRecord;
import damose.model.ConnectionMode;

/**
 * Service for managing real-time and static arrivals.
 * Handles matching between RT and static data, time calculations and formatting.
 */
public class ArrivalService {

    // Real-time map: normalizedTripKey -> (stopId -> arrivalEpochSeconds)
    private final Map<String, Map<String, Long>> realtimeArrivals = new HashMap<>();

    private final TripMatcher matcher;
    private final StopTripMapper stopTripMapper;
    private final TripServiceCalendar tripServiceCalendar;

    public ArrivalService(TripMatcher matcher, StopTripMapper stopTripMapper, 
                         TripServiceCalendar tripServiceCalendar) {
        this.matcher = matcher;
        this.stopTripMapper = stopTripMapper;
        this.tripServiceCalendar = tripServiceCalendar;
    }

    /**
     * Update RT arrivals map with new data from feed.
     */
    public void updateRealtimeArrivals(List<TripUpdateRecord> updates) {
        synchronized (realtimeArrivals) {
            realtimeArrivals.clear();
            for (TripUpdateRecord u : updates) {
                String rawFeedTrip = u.getTripId();
                String normalizedKey = normalizeTripKey(rawFeedTrip);
                Set<String> variants = TripIdUtils.generateVariants(normalizedKey);

                for (String key : variants) {
                    realtimeArrivals
                        .computeIfAbsent(key, k -> new HashMap<>())
                        .put(u.getStopId(), u.getArrivalEpochSeconds());
                }
            }
        }
    }

    /**
     * Compute arrivals for a specific stop.
     * @return List of formatted strings for UI
     */
    public List<String> computeArrivalsForStop(String stopId, ConnectionMode mode, long currentFeedTs) {
        List<StopTime> times = stopTripMapper.getStopTimesForStop(stopId);
        if (times == null || times.isEmpty()) {
            return List.of("Nessun arrivo imminente");
        }

        final long nowEpoch = Instant.now().getEpochSecond();
        final LocalDate feedDate = Instant.ofEpochSecond(currentFeedTs)
                .atZone(ZoneId.systemDefault()).toLocalDate();

        Map<String, RouteArrivalInfo> perRoute = new HashMap<>();

        for (StopTime st : times) {
            Trip trip = matcher.matchByTripId(st.getTripId());
            if (trip == null) continue;
            String routeId = trip.getRouteId();

            // Check active service
            String serviceId = trip.getServiceId();
            if (serviceId != null && !serviceId.isEmpty()) {
                if (!tripServiceCalendar.serviceRunsOnDate(serviceId, feedDate)) {
                    continue;
                }
            }

            // Static schedule
            LocalTime arr = st.getArrivalTime();
            if (arr == null) continue;

            long scheduledEpoch = computeScheduledEpochForFeed(st, currentFeedTs);
            if (scheduledEpoch <= 0) continue;

            long staticDiffMin = (scheduledEpoch - nowEpoch) / 60;
            if (staticDiffMin < -2 || staticDiffMin > AppConstants.STATIC_WINDOW_MIN) continue;

            // RT prediction
            Long predictedEpoch = (mode == ConnectionMode.ONLINE) 
                ? lookupRealtimeArrivalEpochStrictByStop(st, stopId) 
                : null;

            // Sanity check on RT prediction
            if (predictedEpoch != null) {
                long rtDiffMin = (predictedEpoch - nowEpoch) / 60;
                if (rtDiffMin < -2 || rtDiffMin > AppConstants.RT_WINDOW_MIN) {
                    predictedEpoch = null;
                }
            }

            RouteArrivalInfo candidate = new RouteArrivalInfo(routeId, st, scheduledEpoch, predictedEpoch);
            RouteArrivalInfo current = perRoute.get(routeId);

            if (current == null) {
                perRoute.put(routeId, candidate);
            } else {
                long curKey = current.sortKey();
                long candKey = candidate.sortKey();

                if (candKey < curKey) {
                    perRoute.put(routeId, candidate);
                } else if (candidate.predictedEpoch != null && current.predictedEpoch == null) {
                    if (candKey - curKey < 30 * 60) {
                        perRoute.put(routeId, candidate);
                    }
                }
            }
        }

        List<String> arrivi = perRoute.values().stream()
            .sorted(Comparator.comparingLong(RouteArrivalInfo::sortKey))
            .map(this::formatArrivalInfo)
            .collect(Collectors.toList());

        if (arrivi.isEmpty()) {
            arrivi = new ArrayList<>();
            arrivi.add("Nessun arrivo imminente");
        }
        return arrivi;
    }

    private String formatArrivalInfo(RouteArrivalInfo info) {
        long now = Instant.now().getEpochSecond();
        if (info.predictedEpoch != null) {
            long diffFromNowMin = Math.max(0, (info.predictedEpoch - now) / 60);
            long delayMin = (info.predictedEpoch - info.scheduledEpoch) / 60;
            String status;
            if (delayMin > 0) status = "ritardo di " + Math.abs(delayMin) + " min";
            else if (delayMin < 0) status = "anticipo di " + Math.abs(delayMin) + " min";
            else status = "in orario";

            if (diffFromNowMin <= AppConstants.IN_ARRIVO_THRESHOLD_MIN) {
                return info.routeId + " - In arrivo (" + status + ")";
            } else {
                return info.routeId + " - " + diffFromNowMin + " min (" + status + ")";
            }
        } else {
            long diffStaticMin = Math.max(0, 
                Duration.between(LocalTime.now(), info.stopTime.getArrivalTime()).toMinutes());
            return info.routeId + " - " + diffStaticMin + " min (statico)";
        }
    }

    private Long lookupRealtimeArrivalEpochStrictByStop(StopTime st, String stopId) {
        String rawStaticTrip = st.getTripId();
        String normalizedStaticKey = normalizeTripKey(rawStaticTrip);
        Set<String> staticVariants = TripIdUtils.generateVariants(normalizedStaticKey);

        synchronized (realtimeArrivals) {
            // Direct match on variants and exact stopId
            for (String v : staticVariants) {
                Map<String, Long> byStop = realtimeArrivals.get(v);
                if (byStop == null) continue;
                Long direct = byStop.get(stopId);
                if (direct != null) {
                    return direct;
                }
            }

            // Fuzzy: search RT keys that contain static variant
            for (String key : realtimeArrivals.keySet()) {
                for (String v : staticVariants) {
                    if (key == null || v == null) continue;
                    if (key.contains(v) || key.endsWith(v)) {
                        Map<String, Long> candidate = realtimeArrivals.get(key);
                        if (candidate == null) continue;
                        Long cand = candidate.get(stopId);
                        if (cand != null) {
                            return cand;
                        }
                    }
                }
            }

            return null;
        }
    }

    private String normalizeTripKey(String rawTripId) {
        if (rawTripId == null) return null;
        String simple = TripIdUtils.normalizeSimple(rawTripId);
        if (simple != null) {
            simple = simple.trim();
        }
        return simple;
    }

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

    /**
     * Info about an arrival for a specific route.
     */
    private static class RouteArrivalInfo {
        final String routeId;
        final StopTime stopTime;
        final long scheduledEpoch;
        final Long predictedEpoch;

        RouteArrivalInfo(String routeId, StopTime stopTime, long scheduledEpoch, Long predictedEpoch) {
            this.routeId = routeId;
            this.stopTime = stopTime;
            this.scheduledEpoch = scheduledEpoch;
            this.predictedEpoch = predictedEpoch;
        }

        long sortKey() {
            return predictedEpoch != null ? predictedEpoch : scheduledEpoch;
        }
    }
}

