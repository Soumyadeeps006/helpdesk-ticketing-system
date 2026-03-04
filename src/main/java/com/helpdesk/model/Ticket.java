package com.helpdesk.model;

import com.helpdesk.enums.Priority;
import com.helpdesk.enums.TicketStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Core domain entity representing a support ticket.
 * Path: src/main/java/com/helpdesk/model/Ticket.java
 *
 * Status lifecycle:  OPEN → IN_PROGRESS → CLOSED
 */
@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TicketStatus status = TicketStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 10)
    private Priority priority = Priority.MEDIUM;

    // The employee who raised the ticket
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    // The IT staff member assigned to resolve the ticket (nullable until assigned)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @OneToMany(
        mappedBy = "ticket",
        fetch = FetchType.LAZY,
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @OrderBy("createdAt ASC")
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(
        mappedBy = "ticket",
        fetch = FetchType.LAZY,
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @OrderBy("timestamp ASC")
    private List<AuditLog> auditLogs = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public Ticket() {}

    public Ticket(
        String title,
        String description,
        Priority priority,
        User createdBy
    ) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.createdBy = createdBy;
        this.status = TicketStatus.OPEN;
    }

    // -------------------------------------------------------------------------
    // Business / state-transition methods
    // -------------------------------------------------------------------------

    /**
     * Transitions OPEN → IN_PROGRESS and assigns a staff member.
     * Throws IllegalStateException if current status is not OPEN.
     */
    public void assignTo(User staffMember) {
        if (this.status != TicketStatus.OPEN) {
            throw new IllegalStateException(
                "Ticket can only be assigned from OPEN state. Current: " +
                    this.status
            );
        }
        this.assignedTo = staffMember;
        this.status = TicketStatus.IN_PROGRESS;
    }

    /**
     * Transitions IN_PROGRESS → CLOSED and records resolution timestamp.
     * Throws IllegalStateException if current status is not IN_PROGRESS.
     */
    public void close() {
        if (this.status != TicketStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                "Ticket can only be closed from IN_PROGRESS state. Current: " +
                    this.status
            );
        }
        this.status = TicketStatus.CLOSED;
        this.resolvedAt = LocalDateTime.now();
    }

    /**
     * Convenience method to re-open a CLOSED ticket back to OPEN.
     */
    public void reopen() {
        if (this.status != TicketStatus.CLOSED) {
            throw new IllegalStateException(
                "Only CLOSED tickets can be re-opened. Current: " + this.status
            );
        }
        this.status = TicketStatus.OPEN;
        this.assignedTo = null;
        this.resolvedAt = null;
    }

    public void addComment(Comment comment) {
        comments.add(comment);
        comment.setTicket(this);
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String t) {
        this.title = t;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String d) {
        this.description = d;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus s) {
        this.status = s;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority p) {
        this.priority = p;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User u) {
        this.createdBy = u;
    }

    public User getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(User u) {
        this.assignedTo = u;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public List<AuditLog> getAuditLogs() {
        return auditLogs;
    }

    @Override
    public String toString() {
        return (
            "Ticket{id=" +
            id +
            ", title='" +
            title +
            "', status=" +
            status +
            ", priority=" +
            priority +
            "}"
        );
    }
}
