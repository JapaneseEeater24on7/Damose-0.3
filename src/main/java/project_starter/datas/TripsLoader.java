package project_starter.datas;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Loader per il file trips.txt (GTFS statico).
 * - tollerante a righe malformate
 * - preserva il campo service_id esattamente come nel file
 * - ignora righe con colonne insufficienti
 */
public class TripsLoader {

    public static List<Trips> load(String resourcePath) {
        List<Trips> trips = new ArrayList<>();

        try (InputStream in = TripsLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                System.err.println("TripsLoader: risorsa non trovata: " + resourcePath);
                return trips;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line = br.readLine(); // prova a leggere header (se presente)
                if (line == null) return trips;

                // Se la prima riga non sembra header (es. inizia con digit e non con "route_id"),
                // la trattiamo come riga dati: la rimetto in elaborazione.
                boolean headerConsumed = false;
                if (line.toLowerCase().startsWith("route_id") || line.toLowerCase().startsWith("service_id") || line.toLowerCase().contains("trip_id")) {
                    headerConsumed = true;
                } else {
                    // prima riga non header: processala come dato
                    processLine(line, trips);
                }

                while ((line = br.readLine()) != null) {
                    processLine(line, trips);
                }
            }
        } catch (Exception e) {
            System.err.println("Errore nel caricamento di trips.txt: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("TripsLoader: caricati " + trips.size() + " trips da " + resourcePath);
        return trips;
    }

    private static void processLine(String line, List<Trips> trips) {
        if (line == null) return;
        line = line.trim();
        if (line.isEmpty()) return;

        List<String> fields = parseCsv(line);
        // trips.txt standard: route_id,service_id,trip_id,trip_headsign,trip_short_name,direction_id,block_id,shape_id,...
        if (fields.size() < 3) {
            // riga troppo corta, ignora
            return;
        }

        try {
            String routeId       = safeGet(fields, 0).trim();
            String serviceId     = safeGet(fields, 1).trim(); // preserva esattamente come nel file
            String tripId        = safeGet(fields, 2).trim();
            String tripHeadsign  = safeGet(fields, 3).replace("\"", "").trim();
            String tripShortName = safeGet(fields, 4).trim();

            int directionId = 0;
            String dirField = safeGet(fields, 5).trim();
            if (!dirField.isEmpty()) {
                try { directionId = Integer.parseInt(dirField); } catch (NumberFormatException ignored) {}
            }

            String shapeId = safeGet(fields, 7).trim();

            trips.add(new Trips(routeId, serviceId, tripId, tripHeadsign, tripShortName, directionId, shapeId));
        } catch (Exception ex) {
            // ignora riga malformata ma non interrompere il caricamento
            System.err.println("TripsLoader: riga ignorata per parsing error: " + ex.getMessage());
        }
    }

    private static String safeGet(List<String> list, int idx) {
        return idx < list.size() ? list.get(idx) : "";
    }

    /**
     * Parser CSV semplice che gestisce campi con virgolette.
     */
    private static List<String> parseCsv(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                // supporta escaped quotes "" all'interno di un campo
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++; // salta il secondo quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result;
    }
}
