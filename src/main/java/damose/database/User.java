package damose.database;

import java.time.LocalDateTime;

/**
 * Model representing a user.
 */
public class User {

    private int id;
    private String username;
    private String email;
    private LocalDateTime createdAt;

    public User(int id, String username, String email, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "'}";
    }
}

