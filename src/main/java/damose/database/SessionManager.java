package damose.database;

/**
 * Manages the current user session.
 */
public final class SessionManager {

    private static User currentUser = null;

    private SessionManager() {
        // Utility class
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static void logout() {
        currentUser = null;
    }

    public static int getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : -1;
    }
}

