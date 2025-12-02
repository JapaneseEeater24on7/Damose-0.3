package damose.data.loader;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import damose.config.AppConstants;
import damose.data.model.TripServiceCalendar;

/**
 * Loader for GTFS calendar_dates.txt file.
 * Format: service_id,date,exception_type
 * - exception_type 1 => add service
 * - exception_type 2 => remove service
 */
public final class CalendarLoader {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private CalendarLoader() {
        // Utility class
    }

    public static TripServiceCalendar load() {
        return loadFromCalendarDates(AppConstants.GTFS_CALENDAR_DATES_PATH);
    }

    public static TripServiceCalendar loadFromCalendarDates(String calendarDatesPath) {
        TripServiceCalendar calendar = new TripServiceCalendar();

        try (InputStream in = CalendarLoader.class.getResourceAsStream(calendarDatesPath)) {
            if (in == null) {
                System.out.println("calendar_dates.txt not found: " + calendarDatesPath);
                return calendar;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                br.readLine(); // Skip header

                String line;
                int lineNo = 1;
                while ((line = br.readLine()) != null) {
                    lineNo++;
                    line = line.trim();
                    if (line.isEmpty()) continue;

                    String[] cols = line.split(",", -1);
                    if (cols.length < 2) {
                        System.out.println("calendar_dates.txt: line " + lineNo + " ignored (insufficient columns)");
                        continue;
                    }

                    String serviceId = cols[0].trim();
                    String dateStr = cols.length > 1 ? cols[1].trim() : "";
                    String exStr = cols.length > 2 ? cols[2].trim() : "";

                    if (serviceId.isEmpty() || dateStr.isEmpty() || exStr.isEmpty()) {
                        continue;
                    }

                    try {
                        LocalDate date = LocalDate.parse(dateStr, DATE_FORMAT);
                        int exceptionType = Integer.parseInt(exStr);

                        if (exceptionType == 1) {
                            calendar.addServiceDate(serviceId, date);
                        } else if (exceptionType == 2) {
                            calendar.removeServiceDate(serviceId, date);
                        }
                    } catch (Exception e) {
                        System.out.println("calendar_dates.txt: line " + lineNo + " parsing failed: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error reading calendar_dates.txt: " + e.getMessage());
        }

        System.out.println("TripServiceCalendar loaded: serviceCount=" + calendar.serviceCount());
        return calendar;
    }
}

