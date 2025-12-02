package project_starter.controller;

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

import project_starter.datas.StopTime;
import project_starter.datas.StopTripMapper;
import project_starter.datas.TripIdUtils;
import project_starter.datas.TripMatcher;
import project_starter.datas.TripServiceCalendar;
import project_starter.datas.TripUpdateRecord;
import project_starter.datas.Trips;
import project_starter.model.ConnectionMode;

/**
 * Servizio per la gestione degli arrivi in tempo reale e statici.
 * Gestisce il matching tra dati RT e statici, calcoli temporali e formattazione.
 */
public class ArrivalService {

    private static final int IN_ARRIVO_THRESHOLD_MIN = 2;
    private static final int STATIC_WINDOW_MIN = 120;
    private static final int RT_WINDOW_MIN = 90;

    // Mappa realtime: normalizedTripKey -> (stopId -> arrivalEpochSeconds)
    private final Map<String, Map<String, Long>> realtimeArrivals = new HashMap<>();

    private final TripMatcher matcher;
    private final StopTripMapper stopTripMapper;
    private final TripServiceCalendar tripServiceCalendar;

    public ArrivalService(TripMatcher matcher, StopTripMapper stopTripMapper, TripServiceCalendar tripServiceCalendar) {
        this.matcher = matcher;
        this.stopTripMapper = stopTripMapper;
        this.tripServiceCalendar = tripServiceCalendar;
    }

    /**
     * Aggiorna la mappa degli arrivi RT con i nuovi dati dal feed.
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
     * Calcola gli arrivi per una fermata specifica.
     * @return Lista di stringhe formattate per la UI
     */
    public List<String> computeArrivalsForStop(String stopId, ConnectionMode mode, long currentFeedTs) {
        List<StopTime> times = stopTripMapper.getStopTimesForStop(stopId);
        if (times == null || times.isEmpty()) {
            return List.of("Nessun arrivo imminente");
        }

        final long nowEpoch = Instant.now().getEpochSecond();
        final LocalDate feedDate = Instant.ofEpochSecond(currentFeedTs).atZone(ZoneId.systemDefault()).toLocalDate();

        Map<String, RouteArrivalInfo> perRoute = new HashMap<>();

        for (StopTime st : times) {
            Trips trip = matcher.matchByTripId(st.getTripId());
            if (trip == null) continue;
            String routeId = trip.getRouteId();

            // Verifica service attivo
            String serviceId = trip.getServiceId();
            if (serviceId != null && !serviceId.isEmpty()) {
                if (!tripServiceCalendar.serviceRunsOnDate(serviceId, feedDate)) {
                    continue;
                }
            }

            // Orario statico
            LocalTime arr = st.getArrivalTime();
            if (arr == null) continue;

            long scheduledEpoch = computeScheduledEpochForFeed(st, currentFeedTs);
            if (scheduledEpoch <= 0) continue;

            // Calcola differenza dal now per lo statico
            long staticDiffMin = (scheduledEpoch - nowEpoch) / 60;

            // Salta arrivi già passati (oltre 2 min fa) o troppo lontani nel futuro
            if (staticDiffMin < -2 || staticDiffMin > STATIC_WINDOW_MIN) continue;

            // Predizione RT ancorata alla fermata
            Long predictedEpoch = (mode == ConnectionMode.ONLINE) 
                ? lookupRealtimeArrivalEpochStrictByStop(st, stopId) 
                : null;

            // Sanity check su predizione RT
            if (predictedEpoch != null) {
                long rtDiffMin = (predictedEpoch - nowEpoch) / 60;
                if (rtDiffMin < -2 || rtDiffMin > RT_WINDOW_MIN) {
                    predictedEpoch = null;
                }
            }

            RouteArrivalInfo candidate = new RouteArrivalInfo(routeId, st, scheduledEpoch, predictedEpoch);
            RouteArrivalInfo current = perRoute.get(routeId);

            if (current == null) {
                perRoute.put(routeId, candidate);
            } else {
                // Tieni il più imminente, preferendo RT
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

        // Costruisci lista di testo per UI
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

            if (diffFromNowMin <= IN_ARRIVO_THRESHOLD_MIN) {
                return info.routeId + " - In arrivo (" + status + ")";
            } else {
                return info.routeId + " - " + diffFromNowMin + " min (" + status + ")";
            }
        } else {
            long diffStaticMin = Math.max(0, Duration.between(LocalTime.now(), info.stopTime.getArrivalTime()).toMinutes());
            return info.routeId + " - " + diffStaticMin + " min (statico)";
        }
    }

    /**
     * Lookup RT stretto per stop_id (niente fallback su stop_sequence per UI)
     */
    private Long lookupRealtimeArrivalEpochStrictByStop(StopTime st, String stopId) {
        String rawStaticTrip = st.getTripId();
        String normalizedStaticKey = normalizeTripKey(rawStaticTrip);
        Set<String> staticVariants = TripIdUtils.generateVariants(normalizedStaticKey);

        synchronized (realtimeArrivals) {
            // 1) match diretto su varianti e stopId esatto
            for (String v : staticVariants) {
                Map<String, Long> byStop = realtimeArrivals.get(v);
                if (byStop == null) continue;
                Long direct = byStop.get(stopId);
                if (direct != null) {
                    return direct;
                }
            }

            // 2) fuzzy: cerca chiavi RT che contengono la variante statica
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

    /**
     * Normalizza un tripId per uso come chiave.
     */
    private String normalizeTripKey(String rawTripId) {
        if (rawTripId == null) return null;
        String simple = TripIdUtils.normalizeSimple(rawTripId);
        if (simple != null) {
            simple = simple.trim();
        }
        return simple;
    }

    /**
     * Calcola lo scheduled epoch per uno StopTime usando il feed timestamp come riferimento.
     */
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
     * Informazioni su un arrivo per una specifica linea.
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

