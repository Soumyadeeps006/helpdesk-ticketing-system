package com.helpdesk.dao;

import com.helpdesk.model.Comment;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Data Access Object for {@link Comment} entities, including recursive tree fetch.
 * Path: src/main/java/com/helpdesk/dao/CommentDAO.java
 *
 * Key design:
 * - {@link #findRootsByTicketId(Long)}  → fetches only top-level comments (parent IS NULL)
 * - {@link #findTreeByTicketId(Long)}   → fetches all comments flat, then assembles tree in-memory
 *
 * The flat-then-assemble approach avoids recursive SQL and is safe with Hibernate's
 * @BatchSize(16) on Comment.children (set in Day 1 config).
 */
@Repository
public class CommentDAO {

    private final SessionFactory sessionFactory;

    @Autowired
    public CommentDAO(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    // -------------------------------------------------------------------------
    // Write operations
    // -------------------------------------------------------------------------

    @Transactional
    public Comment save(Comment comment) {
        sessionFactory.getCurrentSession().saveOrUpdate(comment);
        return comment;
    }

    @Transactional
    public void delete(Long commentId) {
        Comment c = findById(commentId).orElseThrow(() ->
            new IllegalArgumentException("Comment not found: " + commentId)
        );
        sessionFactory.getCurrentSession().delete(c);
    }

    // -------------------------------------------------------------------------
    // Read operations
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Optional<Comment> findById(Long id) {
        Comment c = sessionFactory.getCurrentSession().get(Comment.class, id);
        return Optional.ofNullable(c);
    }

    /**
     * Returns only the root (top-level) comments for a ticket, ordered oldest-first.
     * Children are loaded lazily via @BatchSize on demand.
     */
    @Transactional(readOnly = true)
    public List<Comment> findRootsByTicketId(Long ticketId) {
        return sessionFactory
            .getCurrentSession()
            .createQuery(
                "SELECT c FROM Comment c " +
                    "JOIN FETCH c.author " +
                    "WHERE c.ticket.id = :ticketId AND c.parent IS NULL " +
                    "ORDER BY c.createdAt ASC",
                Comment.class
            )
            .setParameter("ticketId", ticketId)
            .list();
    }

    /**
     * Fetches ALL comments for a ticket in a single query, then assembles them
     * into a proper parent→children tree structure in memory.
     *
     * Returns only the root nodes; each root's {@code children} list is populated
     * recursively so callers can traverse the full tree.
     *
     * @param ticketId the ticket whose comments are fetched
     * @return list of root {@link Comment} objects with children populated
     */
    @Transactional(readOnly = true)
    public List<Comment> findTreeByTicketId(Long ticketId) {
        // 1. Fetch all comments for this ticket flat (single SELECT)
        List<Comment> allComments = sessionFactory
            .getCurrentSession()
            .createQuery(
                "SELECT c FROM Comment c " +
                    "JOIN FETCH c.author " +
                    "WHERE c.ticket.id = :ticketId " +
                    "ORDER BY c.createdAt ASC",
                Comment.class
            )
            .setParameter("ticketId", ticketId)
            .list();

        // 2. Index by id for O(1) parent lookup
        Map<Long, Comment> byId = allComments
            .stream()
            .collect(Collectors.toMap(Comment::getId, c -> c));

        // 3. Clear Hibernate-populated children to avoid duplicates, then rebuild
        allComments.forEach(c -> c.getChildren().clear());

        // 4. Assemble tree: wire each child into its parent's children list
        List<Comment> roots = new ArrayList<>();
        for (Comment c : allComments) {
            if (c.getParent() == null) {
                roots.add(c);
            } else {
                Comment parent = byId.get(c.getParent().getId());
                if (parent != null) {
                    parent.getChildren().add(c);
                }
                // orphaned comment (parent deleted) — skip silently
            }
        }

        return roots;
    }

    /**
     * Returns the total number of comments (all depths) on a ticket.
     */
    @Transactional(readOnly = true)
    public long countByTicketId(Long ticketId) {
        return sessionFactory
            .getCurrentSession()
            .createQuery(
                "SELECT COUNT(c) FROM Comment c WHERE c.ticket.id = :ticketId",
                Long.class
            )
            .setParameter("ticketId", ticketId)
            .uniqueResult();
    }

    /**
     * Returns all direct replies to a given parent comment.
     */
    @Transactional(readOnly = true)
    public List<Comment> findDirectReplies(Long parentCommentId) {
        return sessionFactory
            .getCurrentSession()
            .createQuery(
                "SELECT c FROM Comment c JOIN FETCH c.author " +
                    "WHERE c.parent.id = :parentId ORDER BY c.createdAt ASC",
                Comment.class
            )
            .setParameter("parentId", parentCommentId)
            .list();
    }

    /**
     * Returns all comments authored by a specific user across all tickets.
     */
    @Transactional(readOnly = true)
    public List<Comment> findByAuthorId(Long authorId) {
        return sessionFactory
            .getCurrentSession()
            .createQuery(
                "FROM Comment c WHERE c.author.id = :authorId ORDER BY c.createdAt DESC",
                Comment.class
            )
            .setParameter("authorId", authorId)
            .list();
    }
}
