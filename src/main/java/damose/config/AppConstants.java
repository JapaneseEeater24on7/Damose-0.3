package damose.config;

import java.awt.Color;
import java.awt.Font;

/**
 * Centralized application constants.
 * All colors, fonts, URLs, and configuration values should be defined here.
 */
public final class AppConstants {

    private AppConstants() {
        // Prevent instantiation
    }

    // ==================== API URLs ====================
    public static final String VEHICLE_POSITIONS_URL =
            "https://romamobilita.it/sites/default/files/rome_rtgtfs_vehicle_positions_feed.pb";
    public static final String TRIP_UPDATES_URL =
            "https://romamobilita.it/sites/default/files/rome_rtgtfs_trip_updates_feed.pb";

    // ==================== GTFS Static Paths ====================
    public static final String GTFS_STOPS_PATH = "/gtfs_static/stops.txt";
    public static final String GTFS_TRIPS_PATH = "/gtfs_static/trips.txt";
    public static final String GTFS_STOP_TIMES_PATH = "/gtfs_static/stop_times.txt";
    public static final String GTFS_CALENDAR_DATES_PATH = "/gtfs_static/calendar_dates.txt";

    // ==================== Timing ====================
    public static final int RT_TIMEOUT_SECONDS = 30;
    public static final int RT_UPDATE_INTERVAL_MS = 30_000;
    public static final int HTTP_CONNECT_TIMEOUT_MS = 30_000;
    public static final int HTTP_READ_TIMEOUT_MS = 60_000;

    // ==================== Arrival Service ====================
    public static final int IN_ARRIVO_THRESHOLD_MIN = 2;
    public static final int STATIC_WINDOW_MIN = 120;
    public static final int RT_WINDOW_MIN = 90;

    // ==================== UI - Midnight Dark Palette ====================
    public static final Color BG_DARK = new Color(17, 17, 21);
    public static final Color BG_MEDIUM = new Color(24, 24, 28);
    public static final Color BG_LIGHT = new Color(32, 32, 38);
    public static final Color BG_FIELD = new Color(38, 38, 44);
    public static final Color BORDER_COLOR = new Color(45, 45, 52);
    public static final Color TEXT_PRIMARY = new Color(229, 229, 234);
    public static final Color TEXT_SECONDARY = new Color(142, 142, 147);
    public static final Color TEXT_MUTED = new Color(100, 100, 110);
    public static final Color ACCENT = new Color(88, 166, 255);
    public static final Color ACCENT_HOVER = new Color(110, 180, 255);
    public static final Color ERROR_COLOR = new Color(255, 99, 99);
    public static final Color SUCCESS_COLOR = new Color(99, 210, 99);
    public static final Color WARNING_COLOR = new Color(245, 180, 70);

    // ==================== UI - Overlay Colors ====================
    public static final Color LIST_BG = new Color(32, 32, 38);
    public static final Color LIST_HOVER = new Color(45, 45, 52);
    public static final Color PROGRESS_BG = new Color(42, 42, 52);

    // ==================== UI - Route Colors ====================
    public static final Color ROUTE_COLOR = new Color(220, 50, 50, 220);
    public static final Color ROUTE_OUTLINE_COLOR = new Color(120, 20, 20, 255);

    // ==================== UI - Floating Panel ====================
    public static final Color PANEL_BG = new Color(35, 35, 35);
    public static final Color PANEL_BORDER = new Color(60, 60, 60);

    // ==================== UI - Fonts ====================
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_HINT = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_BUTTON = new Font("Segoe UI", Font.BOLD, 14);

    // ==================== UI - Panel Dimensions ====================
    public static final int FLOATING_PANEL_WIDTH = 360;
    public static final int FLOATING_ROW_HEIGHT = 32;
    public static final int FLOATING_HEADER_HEIGHT = 50;

    // ==================== Map ====================
    public static final double ROME_LAT = 41.9028;
    public static final double ROME_LON = 12.4964;
    public static final int DEFAULT_ZOOM = 7;
    public static final int CLICK_PROXIMITY_THRESHOLD = 20;

    // ==================== Database ====================
    public static final String DB_URL = "jdbc:sqlite:bustracker.db";
}

