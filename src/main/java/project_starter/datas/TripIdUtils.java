package project_starter.datas;

import java.util.HashSet;
import java.util.Set;

/**
 * Utility per normalizzazione e generazione di varianti di trip_id.
 *
 * Nota: questa versione rimuove SOLO il prefisso numerico seguito da '#'
 * (es. "0#4930-8" -> "4930-8") e NON rimuove i suffissi come "-8".
 * I raw tripId statici non vengono modificati.
 */
public final class TripIdUtils {

    private TripIdUtils() {}

    /**
     * Normalizzazione "semplice" usata per confronti tra statici (preserva la forma originale).
     * Mantiene il valore così com'è, ma protegge da null.
     */
    public static String normalizeSimple(String tripId) {
        if (tripId == null) return null;
        return tripId.trim();
    }

    /**
     * Normalizza un tripId proveniente dal feed realtime rimuovendo SOLO
     * il prefisso numerico seguito da '#' (es. "0#12345-6" -> "12345-6").
     *
     * Questa funzione NON rimuove suffissi come "-8".
     */
    public static String normalizeFeedTripId(String feedTripId) {
        if (feedTripId == null) return null;
        String s = feedTripId.trim();
        // rimuove prefisso tipo "0#" o "12#"
        s = s.replaceFirst("^\\d+#", "");
        return s;
    }

    /**
     * Genera un set di varianti utili per il matching.
     * - include il raw feedTripId (se non null)
     * - include la versione normalizzata feed (prefisso rimosso)
     * - include la versione normalizeSimple (raw trimmed) per i statici
     *
     * L'idea è fornire chiavi multiple senza alterare i trip_id statici.
     */
    public static Set<String> generateVariants(String feedTripId) {
        Set<String> out = new HashSet<>();
        if (feedTripId == null) return out;

        String raw = feedTripId.trim();
        out.add(raw);

        String normalizedFeed = normalizeFeedTripId(raw);
        if (normalizedFeed != null && !normalizedFeed.equals(raw)) {
            out.add(normalizedFeed);
        }

        String simple = normalizeSimple(raw);
        if (simple != null && !simple.equals(raw)) {
            out.add(simple);
        }

        return out;
    }
}
