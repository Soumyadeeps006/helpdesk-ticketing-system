package com.helpdesk.dao;

import com.helpdesk.enums.Priority;
import com.helpdesk.enums.TicketStatus;
import com.helpdesk.model.Ticket;
import java.util.List;
import java.util.Optional;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Data Access Object for {@link Ticket} entities.
 * Path: src/main/java/com/helpdesk/dao/TicketDAO.java
 *
 * Uses HQL to avoid N+1 problems by JOIN FETCHing associations when needed.
 */
@Repository
public class TicketDAO {

    private final SessionFactory sessionFactory;

    @Autowired
    public TicketDAO(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    // -------------------------------------------------------------------------
    // Write operations
    // -------------------------------------------------------------------------

    @Transactional
    public Ticket save(Ticket ticket) {
        sessionFactory.getCurrentSession().saveOrUpdate(ticket);
        return ticket;
    }

    @Transactional
    public void delete(Long ticketId) {
        Ticket ticket = findById(ticketId).orElseThrow(() ->
            new IllegalArgumentException("Ticket not found: " + ticketId)
        );
        sessionFactory.getCurrentSession().delete(ticket);
    }

    // -------------------------------------------------------------------------
    // Read operations — lightweight (no associations fetched)
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Optional<Ticket> findById(Long id) {
        Ticket ticket = sessionFactory
            .getCurrentSession()
            .get(Ticket.class, id);
        return Optional.ofNullable(ticket);
    }

    /**
     * Fetches a ticket with its top-level comments and author eagerly loaded.
     * Uses JOIN FETCH to prevent N+1 on the comment list.
     */
    @Transactional(readOnly = true)
    public Optional<Ticket> findByIdWithComments(Long id) {
        return sessionFactory
            .getCurrentSession()
            .createQuery(
                "SELECT DISTINCT t FROM Ticket t " +
                    "LEFT JOIN FETCH t.comments c " +
                    "LEFT JOIN FETCH c.author " +
                    "WHERE t.id = :id",
                Ticket.class
            )
            .setParameter("id", id)
            .uniqueResultOptional();
    }

    /**
     * Fetches a ticket with its full audit trail.
     */
    @Transactional(readOnly = true)
    public Optional<Ticket> findByIdWithAuditLogs(Long id) {
        return sessionFactory
            .getCurrentSession()
            .createQuery(
                "SELECT DISTINCT t FROM Ticket t " +
                    "LEFT JOIN FETCH t.auditLogs a " +
                    "LEFT JOIN FETCH a.actor " +
                    "WHERE t.id = :id",
                Ticket.class
            )
            .setParameter("id", id)
            .uniqueResultOptional();
    }

    @Transactional(readOnly = true)
    public List<Ticket> findAll() {
        return sessionFactory
            .getCurrentSession()
            .createQuery(
                "SELECT t FROM Ticket t " +
                    "JOIN FETCH t.createdBy " +
                    "ORDER BY t.createdAt DESC",
                Ticket.class
            )
            .list();
    }

    @Transactional(readOnly = true)
    public List<Ticket> findByStatus(TicketStatus status) {
        return sessionFactory
            .getCurrentSession()
            .createQuery(
                "FROM Ticket t JOIN FETCH t.createdBy " +
                    "WHERE t.status = :status ORDER BY t.createdAt DESC",
                Ticket.class
            )
            .setParameter("status", status)
            .list();
    }

    @Transactional(readOnly = true)
    public List<Ticket> findByPriority(Priority priority) {
        return sessionFactory
            .getCurrentSession()
            .createQuery(
                "FROM Ticket t JOIN FETCH t.createdBy " +
                    "WHERE t.priority = :priority ORDER BY t.createdAt DESC",
                Ticket.class
            )
            .setParameter("priority", priority)
            .list();
    }

    @Transactional(readOnly = true)
    public List<Ticket> findByCreatedBy(Long userId) {
        return sessionFactory
            .getCurrentSession()
            .createQuery(
                "FROM Ticket t WHERE t.createdBy.id = :userId ORDER BY t.createdAt DESC",
                Ticket.class
            )
            .setParameter("userId", userId)
            .list();
    }

    @Transactional(readOnly = true)
    public List<Ticket> findByAssignedTo(Long staffId) {
        return sessionFactory
            .getCurrentSession()
            .createQuery(
                "FROM Ticket t WHERE t.assignedTo.id = :staffId ORDER BY t.priority DESC",
                Ticket.class
            )
            .setParameter("staffId", staffId)
            .list();
    }

    @Transactional(readOnly = true)
    public long countByStatus(TicketStatus status) {
        return sessionFactory
            .getCurrentSession()
            .createQuery(
                "SELECT COUNT(t) FROM Ticket t WHERE t.status = :status",
                Long.class
            )
            .setParameter("status", status)
            .uniqueResult();
    }

    /**
     * Full-text keyword search across title and description.
     */
    @Transactional(readOnly = true)
    public List<Ticket> search(String keyword) {
        String pattern = "%" + keyword.toLowerCase() + "%";
        return sessionFactory
            .getCurrentSession()
            .createQuery(
                "FROM Ticket t WHERE LOWER(t.title) LIKE :kw OR LOWER(t.description) LIKE :kw " +
                    "ORDER BY t.createdAt DESC",
                Ticket.class
            )
            .setParameter("kw", pattern)
            .list();
    }
}
