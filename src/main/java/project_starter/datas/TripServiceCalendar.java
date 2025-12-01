package project_starter.datas;

import java.time.LocalDate;
import java.util.*;

/**
 * Mantiene le date attive per ogni service_id.
 */
public class TripServiceCalendar {
    private final Map<String, Set<LocalDate>> serviceDates = new HashMap<>();

    public void addServiceDate(String serviceId, LocalDate date) {
        serviceDates.computeIfAbsent(serviceId, k -> new HashSet<>()).add(date);
    }

    public void removeServiceDate(String serviceId, LocalDate date) {
        Set<LocalDate> s = serviceDates.get(serviceId);
        if (s != null) {
            s.remove(date);
            if (s.isEmpty()) serviceDates.remove(serviceId);
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
