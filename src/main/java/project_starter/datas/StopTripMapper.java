package project_starter.datas;

import java.util.*;
import java.util.stream.Collectors;

/**
 * StopTripMapper patched to index trip sequence mappings for multiple tripId variants.
 * Uses TripIdUtils located in project_starter.datas.
 */
public class StopTripMapper {
    private final Map<String, List<StopTime>> stopToTrips = new HashMap<>();
    private final Map<String, Map<Integer, String>> tripSeqToStop = new HashMap<>();
    private final Set<String> knownStopIds = new HashSet<>();
    private final TripMatcher matcher;

    public StopTripMapper(List<StopTime> stopTimes, TripMatcher matcher) {
        this.matcher = matcher;
        for (StopTime st : stopTimes) {
            String stopId = st.getStopId();
            String rawTripId = st.getTripId();
            String normTripId = normalizeTripId(rawTripId);
            int seq = st.getStopSequence();

            if (stopId != null) knownStopIds.add(stopId);

            stopToTrips.computeIfAbsent(stopId, k -> new ArrayList<>()).add(st);

            // index mapping for the normalized trip id
            tripSeqToStop
                    .computeIfAbsent(normTripId, k -> new HashMap<>())
                    .put(seq, stopId);

            // also index for all useful variants so lookups succeed regardless of feed format
            Set<String> variants = TripIdUtils.generateVariants(rawTripId);
            for (String v : variants) {
                String vNorm = normalizeTripId(v);
                tripSeqToStop
                        .computeIfAbsent(vNorm, k -> new HashMap<>())
                        .put(seq, stopId);
            }
        }
        for (List<StopTime> list : stopToTrips.values()) {
            list.sort(Comparator.comparing(StopTime::getArrivalTime, Comparator.nullsLast(Comparator.naturalOrder())));
        }
        System.out.println("StopTripMapper initialized: stopToTrips=" + stopToTrips.size() + " tripSeqToStop=" + tripSeqToStop.size());
    }

    public List<Trips> getTripsForStop(String stopId) {
        List<StopTime> times = stopToTrips.getOrDefault(stopId, Collections.emptyList());
        if (times.isEmpty() || matcher == null) return Collections.emptyList();

        List<Trips> result = new ArrayList<>(times.size());
        for (StopTime st : times) {
            Trips trip = matcher.matchByTripId(st.getTripId());
            if (trip != null) result.add(trip);
        }
        return result.stream().distinct().collect(Collectors.toList());
    }

    public boolean isKnownStopId(String stopId) {
        return stopId != null && knownStopIds.contains(stopId);
    }

    public String getStopIdByTripAndSequence(String tripId, int sequence) {
        if (tripId == null) return null;

        // try direct normalized lookup
        String norm = normalizeTripId(tripId);
        Map<Integer, String> seqMap = tripSeqToStop.get(norm);
        if (seqMap != null) {
            String s = seqMap.get(sequence);
            if (s != null) {
                System.out.println("Found stop by direct normalized tripId=" + norm + " seq=" + sequence + " -> " + s);
                return s;
            }
        }

        // try all variants
        Set<String> variants = TripIdUtils.generateVariants(tripId);
        for (String v : variants) {
            String vNorm = normalizeTripId(v);
            Map<Integer, String> m = tripSeqToStop.get(vNorm);
            if (m != null) {
                String s = m.get(sequence);
                if (s != null) {
                    System.out.println("Found stop by variant tripId=" + v + " normalized=" + vNorm + " seq=" + sequence + " -> " + s);
                    return s;
                }
            }
        }

        // not found
        return null;
    }

    public List<StopTime> getStopTimesForStop(String stopId) {
        return stopToTrips.getOrDefault(stopId, Collections.emptyList());
    }

    private String normalizeTripId(String id) {
        if (id == null) return null;
        return id.replaceAll("^[0#]+", "").trim();
    }
}
