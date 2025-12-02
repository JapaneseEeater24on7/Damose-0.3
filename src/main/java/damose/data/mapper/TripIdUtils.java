package damose.data.mapper;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Utility for normalizing tripId and generating variants for matching
 * between GTFS static and GTFS-RT data.
 */
public final class TripIdUtils {

    private TripIdUtils() {
        // Utility class
    }

    /**
     * Normalizes a tripId to a simple, consistent form:
     * - trim
     * - remove known prefixes (0#, agency:, trip:)
     * - remove non-significant characters
     * - keep letters, numbers, '-' and '_'
     * - convert to lowercase
     */
    public static String normalizeSimple(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;

        // Remove common prefixes: digit(s)# pattern (e.g., "0#", "1#", "12#")
        s = s.replaceFirst("^\\d+#", "");

        String lower = s.toLowerCase();
        if (lower.startsWith("agency:")) {
            s = s.substring("agency:".length());
        } else if (lower.startsWith("trip:")) {
            s = s.substring("trip:".length());
        } else {
            // Remove generic prefixes up to first ':' if short
            int colon = s.indexOf(':');
            if (colon > 0 && colon < 6) {
                s = s.substring(colon + 1);
            }
        }

        s = s.trim();
        s = s.replaceAll("[^A-Za-z0-9_\\-\\.]", "");
        s = s.replaceAll("^[\\-_.]+", "");
        s = s.replaceAll("[\\-_.]+$", "");
        s = s.replaceAll("([\\-_.])0+$", "");
        s = s.toLowerCase();

        return s.isEmpty() ? null : s;
    }

    /**
     * Generates a set of useful variants for matching between RT and static feeds.
     */
    public static Set<String> generateVariants(String rawTripId) {
        Set<String> out = new HashSet<>();
        if (rawTripId == null) return out;

        String norm = normalizeSimple(rawTripId);

        if (norm == null) {
            String fallback = rawTripId.trim().toLowerCase();
            if (!fallback.isEmpty()) {
                out.add(fallback);
            }
            return out;
        }

        out.add(norm);

        // Variant: remove separators
        String noSep = norm.replaceAll("[-_\\.]", "");
        if (!noSep.isEmpty()) out.add(noSep);

        // Variant: replace '-' with '_'
        if (norm.contains("-")) {
            out.add(norm.replace('-', '_'));
        }

        // Variant: replace '_' with '-'
        if (norm.contains("_")) {
            out.add(norm.replace('_', '-'));
        }

        // Variant: handle dots
        if (norm.contains(".")) {
            out.add(norm.replace('.', '-'));
            out.add(norm.replace('.', '_'));
            out.add(norm.replace(".", ""));
        }

        out.removeIf(Objects::isNull);
        out.removeIf(String::isEmpty);

        return out;
    }

    /**
     * Normalizes and returns empty string if null (useful for map keys).
     */
    public static String normalizeOrEmpty(String raw) {
        String n = normalizeSimple(raw);
        return n == null ? "" : n;
    }
}

