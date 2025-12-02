package project_starter.datas;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Utility per normalizzare tripId e generare varianti usate per il matching
 * tra GTFS statico e GTFS-RT.
 *
 * Principi:
 * - rimuove prefissi comuni (es. "0#", "agency:", "trip:")
 * - pulisce spazi e caratteri non significativi
 * - mantiene separatori utili (-, _) ma produce anche varianti senza separatori
 * - restituisce sempre stringhe in forma "semplice" coerente per l'uso come chiave
 */
public final class TripIdUtils {

    private TripIdUtils() { /* utility */ }

    /**
     * Normalizza un tripId in una forma "semplice" e coerente:
     * - trim
     * - rimuove prefissi noti (0#, agency:, trip:)
     * - rimuove caratteri non significativi all'inizio/fine
     * - mantiene lettere, numeri, '-' e '_' (altri caratteri vengono rimossi)
     * - converte in lower-case
     *
     * Esempi:
     *  "0#4930-11" -> "4930-11"
     *  "agency:4930_11" -> "4930_11"
     */
    public static String normalizeSimple(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;

        // Rimuovi prefissi comuni: digit(s)# pattern (es. "0#", "1#", "12#")
        // Questo è il formato usato da molti feed GTFS italiani
        s = s.replaceFirst("^\\d+#", "");
        
        String lower = s.toLowerCase();
        if (lower.startsWith("agency:")) {
            s = s.substring("agency:".length());
        } else if (lower.startsWith("trip:")) {
            s = s.substring("trip:".length());
        } else {
            // rimuovi eventuali prefissi generici fino al primo ':' (es. "X:123")
            int colon = s.indexOf(':');
            if (colon > 0 && colon < 6) { // solo se il prefisso è breve (evita rimuovere parti valide)
                s = s.substring(colon + 1);
            }
        }

        // Trim again after prefix removal
        s = s.trim();

        // Keep only letters, digits, dash and underscore and dot (dot sometimes used)
        // Replace other characters with empty string
        s = s.replaceAll("[^A-Za-z0-9_\\-\\.]", "");

        // Remove repeated separators at edges
        s = s.replaceAll("^[\\-_.]+", "");
        s = s.replaceAll("[\\-_.]+$", "");

        // Remove trailing numeric padding like "-0", "_0", ":0" repeated (common noise)
        s = s.replaceAll("([\\-_.])0+$", "");

        // Lowercase for stable matching
        s = s.toLowerCase();

        return s.isEmpty() ? null : s;
    }

    /**
     * Genera un insieme di varianti utili per il matching tra feed RT e statici.
     * Le varianti includono:
     * - la forma normalizzata
     * - la forma senza separatori (- e _ rimossi)
     * - la forma con '_' al posto di '-' (se presente)
     * - la forma con '-' al posto di '_' (se presente)
     *
     * L'obiettivo è coprire le differenze più comuni nei tripId tra dataset.
     */
    public static Set<String> generateVariants(String rawTripId) {
        Set<String> out = new HashSet<>();
        if (rawTripId == null) return out;

        String norm = normalizeSimple(rawTripId);
        
        // Fallback: se la normalizzazione restituisce null (es. "0#" -> ""),
        // usa il raw tripId (trim + lowercase) come variante di fallback
        // per evitare di perdere silenziosamente dati realtime
        if (norm == null) {
            String fallback = rawTripId.trim().toLowerCase();
            if (!fallback.isEmpty()) {
                out.add(fallback);
            }
            return out;
        }

        out.add(norm);

        // variant: remove separators
        String noSep = norm.replaceAll("[-_\\.]", "");
        if (!noSep.isEmpty()) out.add(noSep);

        // variant: replace '-' with '_'
        if (norm.contains("-")) {
            out.add(norm.replace('-', '_'));
        }

        // variant: replace '_' with '-'
        if (norm.contains("_")) {
            out.add(norm.replace('_', '-'));
        }

        // variant: if dot present, also try replacing dot with dash/underscore and removing
        if (norm.contains(".")) {
            out.add(norm.replace('.', '-'));
            out.add(norm.replace('.', '_'));
            out.add(norm.replace(".", ""));
        }

        // keep only non-null, non-empty values
        out.removeIf(Objects::isNull);
        out.removeIf(String::isEmpty);

        return out;
    }

    /**
     * Utility helper: normalizza e, se null, ritorna stringa vuota (utile per map keys).
     */
    public static String normalizeOrEmpty(String raw) {
        String n = normalizeSimple(raw);
        return n == null ? "" : n;
    }
}
