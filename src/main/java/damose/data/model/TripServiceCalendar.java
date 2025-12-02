package damose.data.model;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Maintains the active dates for each service_id from GTFS calendar data.
 */
public class TripServiceCalendar {

    private final Map<String, Set<LocalDate>> serviceDates = new HashMap<>();

    public void addServiceDate(String serviceId, LocalDate date) {
        serviceDates.computeIfAbsent(serviceId, k -> new HashSet<>()).add(date);
    }

    public void removeServiceDate(String serviceId, LocalDate date) {
        Set<LocalDate> dates = serviceDates.get(serviceId);
        if (dates != null) {
            dates.remove(date);
            if (dates.isEmpty()) {
                serviceDates.remove(serviceId);
            }
        }
    }

    public boolean serviceRunsOnDate(String serviceId, LocalDate date) {
        Set<LocalDate> dates = serviceDates.get(serviceId);
        return dates != null && dates.contains(date);
    }

    public int serviceCount() {
        return serviceDates.size();
    }
}

