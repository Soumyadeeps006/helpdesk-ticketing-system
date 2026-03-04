package com.helpdesk.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Self-referencing entity that supports nested (threaded) comment replies.
 * Path: src/main/java/com/helpdesk/model/Comment.java
 *
 * Tree structure:
 *   Comment (root, parent = null)
 *     └── Comment (reply, parent = root)
 *           └── Comment (nested reply, parent = reply)
 *
 * Hibernate strategy: @ManyToOne on parent + @OneToMany on children.
 * Lazy loading with @BatchSize(16) avoids N+1 on comment trees.
 */
@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    // The ticket this comment belongs to
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    // Author of the comment
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    // ---- Self-referencing relationship ----

    /** Null for root-level comments; points to the parent reply otherwise. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    /** Direct child replies of this comment. */
    @OneToMany(
        mappedBy = "parent",
        fetch = FetchType.LAZY,
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @OrderBy("createdAt ASC")
    @BatchSize(size = 16)
    private List<Comment> children = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "edited", nullable = false)
    private boolean edited = false;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public Comment() {}

    /** Creates a root-level (top) comment on a ticket. */
    public Comment(String body, Ticket ticket, User author) {
        this.body = body;
        this.ticket = ticket;
        this.author = author;
        this.parent = null;
    }

    /** Creates a reply to an existing comment. */
    public Comment(String body, Ticket ticket, User author, Comment parent) {
        this.body = body;
        this.ticket = ticket;
        this.author = author;
        this.parent = parent;
    }

    // -------------------------------------------------------------------------
    // Business methods
    // -------------------------------------------------------------------------

    public boolean isRootComment() {
        return this.parent == null;
    }

    public int getDepth() {
        int depth = 0;
        Comment current = this.parent;
        while (current != null) {
            depth++;
            current = current.getParent();
        }
        return depth;
    }

    public void addReply(Comment reply) {
        reply.setParent(this);
        reply.setTicket(this.ticket);
        this.children.add(reply);
    }

    public void markEdited() {
        this.edited = true;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String b) {
        this.body = b;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket t) {
        this.ticket = t;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User a) {
        this.author = a;
    }

    public Comment getParent() {
        return parent;
    }

    public void setParent(Comment p) {
        this.parent = p;
    }

    public List<Comment> getChildren() {
        return children;
    }

    public void setChildren(List<Comment> c) {
        this.children = c;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isEdited() {
        return edited;
    }

    @Override
    public String toString() {
        return (
            "Comment{id=" +
            id +
            ", author=" +
            (author != null ? author.getUsername() : "?") +
            ", parent=" +
            (parent != null ? parent.getId() : "null") +
            "}"
        );
    }
}
