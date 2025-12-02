package damose.database;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Service for user management (registration, login, etc.).
 */
public final class UserService {

    private UserService() {
        // Utility class
    }

    /**
     * Register a new user.
     * @return true if registration succeeded, false if username exists
     */
    public static boolean register(String username, String password, String email) {
        String hashedPassword = hashPassword(password);

        String sql = "INSERT INTO users (username, password_hash, email) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username.toLowerCase().trim());
            stmt.setString(2, hashedPassword);
            stmt.setString(3, email != null ? email.trim() : null);

            stmt.executeUpdate();
            System.out.println("User registered: " + username);
            return true;

        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                System.out.println("Username already exists: " + username);
            } else {
                System.err.println("Registration error: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Login a user.
     * @return User object if credentials are valid, null otherwise
     */
    public static User login(String username, String password) {
        String hashedPassword = hashPassword(password);

        String sql = "SELECT id, username, email, created_at FROM users WHERE username = ? AND password_hash = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username.toLowerCase().trim());
            stmt.setString(2, hashedPassword);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                User user = new User(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("email"),
                    LocalDateTime.now()
                );
                System.out.println("Login successful: " + username);
                return user;
            }

        } catch (SQLException e) {
            System.err.println("Login error: " + e.getMessage());
        }

        System.out.println("Login failed for: " + username);
        return null;
    }

    /**
     * Check if username exists.
     */
    public static boolean usernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username.toLowerCase().trim());
            ResultSet rs = stmt.executeQuery();

            return rs.next() && rs.getInt(1) > 0;

        } catch (SQLException e) {
            System.err.println("Error checking username: " + e.getMessage());
            return false;
        }
    }

    /**
     * Change user password.
     */
    public static boolean changePassword(int userId, String oldPassword, String newPassword) {
        String oldHash = hashPassword(oldPassword);
        String newHash = hashPassword(newPassword);

        String sql = "UPDATE users SET password_hash = ? WHERE id = ? AND password_hash = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newHash);
            stmt.setInt(2, userId);
            stmt.setString(3, oldHash);

            int updated = stmt.executeUpdate();
            return updated > 0;

        } catch (SQLException e) {
            System.err.println("Error changing password: " + e.getMessage());
            return false;
        }
    }

    /**
     * Hash password using SHA-256.
     */
    private static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}

