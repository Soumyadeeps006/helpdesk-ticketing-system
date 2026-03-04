package com.helpdesk.model;

import java.time.LocalDateTime;
import javax.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Immutable audit record capturing every state change on a Ticket.
 * Path: src/main/java/com/helpdesk/model/AuditLog.java
 *
 * Fields: actor (who), action (what), oldValue → newValue (change), ticket (which).
 * Records are never updated or deleted — only inserted.
 */
@Entity
@Table(
    name = "audit_log",
    indexes = {
        @Index(name = "idx_audit_ticket", columnList = "ticket_id"),
        @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    }
)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The ticket this log entry belongs to
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    // Who performed the action
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    /**
     * Human-readable action label.
     * Examples: "STATUS_CHANGED", "PRIORITY_CHANGED", "TICKET_CREATED",
     *           "COMMENT_ADDED", "TICKET_ASSIGNED", "TICKET_CLOSED"
     */
    @Column(name = "action", nullable = false, length = 50)
    private String action;

    /** Previous value (null for creation events). */
    @Column(name = "old_value", length = 255)
    private String oldValue;

    /** New value after the change. */
    @Column(name = "new_value", length = 255)
    private String newValue;

    /** Optional free-text note to add context. */
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @CreationTimestamp
    @Column(name = "timestamp", updatable = false, nullable = false)
    private LocalDateTime timestamp;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public AuditLog() {}

    public AuditLog(
        Ticket ticket,
        User actor,
        String action,
        String oldValue,
        String newValue
    ) {
        this.ticket = ticket;
        this.actor = actor;
        this.action = action;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public AuditLog(
        Ticket ticket,
        User actor,
        String action,
        String oldValue,
        String newValue,
        String note
    ) {
        this(ticket, actor, action, oldValue, newValue);
        this.note = note;
    }

    // -------------------------------------------------------------------------
    // Factory helpers
    // -------------------------------------------------------------------------

    public static AuditLog statusChange(
        Ticket ticket,
        User actor,
        String oldStatus,
        String newStatus
    ) {
        return new AuditLog(
            ticket,
            actor,
            "STATUS_CHANGED",
            oldStatus,
            newStatus
        );
    }

    public static AuditLog priorityChange(
        Ticket ticket,
        User actor,
        String oldPriority,
        String newPriority
    ) {
        return new AuditLog(
            ticket,
            actor,
            "PRIORITY_CHANGED",
            oldPriority,
            newPriority
        );
    }

    public static AuditLog ticketCreated(Ticket ticket, User actor) {
        return new AuditLog(ticket, actor, "TICKET_CREATED", null, "OPEN");
    }

    public static AuditLog commentAdded(
        Ticket ticket,
        User actor,
        Long commentId
    ) {
        return new AuditLog(
            ticket,
            actor,
            "COMMENT_ADDED",
            null,
            String.valueOf(commentId)
        );
    }

    // -------------------------------------------------------------------------
    // Getters (no setters — audit logs are immutable after creation)
    // -------------------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public User getActor() {
        return actor;
    }

    public String getAction() {
        return action;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public String getNote() {
        return note;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return (
            "AuditLog{id=" +
            id +
            ", action='" +
            action +
            "', " +
            oldValue +
            " → " +
            newValue +
            ", at=" +
            timestamp +
            "}"
        );
    }
}
