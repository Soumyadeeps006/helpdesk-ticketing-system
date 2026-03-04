package com.helpdesk.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Represents a system user — either an employee or IT staff member.
 * Path: src/main/java/com/helpdesk/model/User.java
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "role", nullable = false, length = 20)
    private String role; // "EMPLOYEE" or "IT_STAFF"

    @Column(name = "full_name", length = 100)
    private String fullName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(
        mappedBy = "createdBy",
        fetch = FetchType.LAZY,
        cascade = CascadeType.ALL
    )
    private List<Ticket> tickets = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public User() {}

    public User(
        String username,
        String email,
        String rawPassword,
        String role,
        String fullName
    ) {
        this.username = username;
        this.email = email;
        this.passwordHash = BCrypt.hashpw(rawPassword, BCrypt.gensalt());
        this.role = role;
        this.fullName = fullName;
    }

    // -------------------------------------------------------------------------
    // Business methods
    // -------------------------------------------------------------------------

    /**
     * Verifies a raw password against the stored BCrypt hash.
     */
    public boolean checkPassword(String rawPassword) {
        return BCrypt.checkpw(rawPassword, this.passwordHash);
    }

    /**
     * Updates the password; re-hashes the new raw value.
     */
    public void changePassword(String newRawPassword) {
        this.passwordHash = BCrypt.hashpw(newRawPassword, BCrypt.gensalt());
    }

    public boolean isItStaff() {
        return "IT_STAFF".equalsIgnoreCase(this.role);
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String u) {
        this.username = u;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String e) {
        this.email = e;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String r) {
        this.role = r;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String n) {
        this.fullName = n;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<Ticket> getTickets() {
        return tickets;
    }

    public void setTickets(List<Ticket> t) {
        this.tickets = t;
    }

    @Override
    public String toString() {
        return (
            "User{id=" +
            id +
            ", username='" +
            username +
            "', role='" +
            role +
            "'}"
        );
    }
}
