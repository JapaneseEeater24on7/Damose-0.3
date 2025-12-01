package project_starter.datas;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * CalendarLoader minimale che legge solo calendar_dates.txt.
 * Formato atteso: service_id,date,exception_type
 * - exception_type 1 => add
 * - exception_type 2 => remove
 *
 * Il loader è tollerante: ignora righe malformate e campi vuoti.
 */
public final class CalendarLoader {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyyMMdd");

    private CalendarLoader() {}

    public static TripServiceCalendar loadFromCalendarDates(String calendarDatesPath) {
        TripServiceCalendar cal = new TripServiceCalendar();

        try (InputStream in = CalendarLoader.class.getResourceAsStream(calendarDatesPath)) {
            if (in == null) {
                System.out.println("calendar_dates.txt non trovato in classpath: " + calendarDatesPath);
                return cal;
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String header = br.readLine(); // salta header se presente
                String line;
                int lineNo = 1;
                while ((line = br.readLine()) != null) {
                    lineNo++;
                    line = line.trim();
                    if (line.isEmpty()) continue;

                    // split con -1 per preservare campi vuoti; rimuovi spazi attorno
                    String[] cols = line.split(",", -1);
                    if (cols.length < 2) {
                        // riga troppo corta, ignora
                        System.out.println("calendar_dates.txt: riga " + lineNo + " ignorata (colonne insufficienti): " + line);
                        continue;
                    }

                    String serviceId = cols[0].trim();
                    String dateStr = cols.length > 1 ? cols[1].trim() : "";
                    String exStr = cols.length > 2 ? cols[2].trim() : "";

                    if (serviceId.isEmpty() || dateStr.isEmpty() || exStr.isEmpty()) {
                        // alcuni file possono avere trailing comma o campi mancanti; ignora riga se mancano i campi essenziali
                        System.out.println("calendar_dates.txt: riga " + lineNo + " ignorata (campo vuoto): " + line);
                        continue;
                    }

                    try {
                        LocalDate date = LocalDate.parse(dateStr, DTF);
                        int ex = Integer.parseInt(exStr);
                        if (ex == 1) {
                            cal.addServiceDate(serviceId, date);
                        } else if (ex == 2) {
                            cal.removeServiceDate(serviceId, date);
                        } else {
                            System.out.println("calendar_dates.txt: riga " + lineNo + " exception_type non riconosciuto: " + exStr);
                        }
                    } catch (Exception e) {
                        System.out.println("calendar_dates.txt: riga " + lineNo + " parsing fallito: " + e.getMessage() + " -> " + line);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Errore leggendo calendar_dates.txt: " + e.getMessage());
        }

        System.out.println("TripServiceCalendar caricato da calendar_dates: serviceCount=" + cal.serviceCount());
        return cal;
    }
}
