package damose.data.loader;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple CSV parser that handles quoted fields.
 */
public final class CsvParser {

    private CsvParser() {
        // Utility class
    }

    /**
     * Parse a CSV line handling quoted fields.
     */
    public static List<String> parseLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                // Support escaped quotes "" inside a field
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
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

