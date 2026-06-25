package com.helpdesk.service;

import com.helpdesk.config.RabbitConfig;
import com.helpdesk.dao.TicketDAO;
import com.helpdesk.dao.UserDAO;
import com.helpdesk.enums.Priority;
import com.helpdesk.enums.TicketStatus;
import com.helpdesk.model.AuditLog;
import com.helpdesk.model.Ticket;
import com.helpdesk.model.User;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * TicketService is the ONLY place allowed to mutate ticket state.
 * Controllers delegate here; DAOs are never called directly from controllers.
 */
@Service
@Transactional
public class TicketService {

    private final TicketDAO ticketDAO;
    private final UserDAO userDAO;
    private final SessionFactory sessionFactory;
    private final RabbitTemplate rabbitTemplate; // ADDED FOR RABBITMQ

    @Autowired
    public TicketService(
        TicketDAO ticketDAO,
        UserDAO userDAO,
        SessionFactory sessionFactory,
        RabbitTemplate rabbitTemplate // ADDED FOR RABBITMQ
    ) {
        this.ticketDAO = ticketDAO;
        this.userDAO = userDAO;
        this.sessionFactory = sessionFactory;
        this.rabbitTemplate = rabbitTemplate; // ADDED FOR RABBITMQ
    }

    // -----------------------------------------------------------------------
    // Read
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Ticket> getAllTickets() {
        return ticketDAO.findAll();
    }

    @Transactional(readOnly = true)
    public List<Ticket> getTicketsPage(int page, int size) {
        return ticketDAO.findPage(page, size);
    }

    @Transactional(readOnly = true)
    public long getTicketCount() {
        return ticketDAO.countAll();
    }

    @Transactional(readOnly = true)
    public Ticket getTicketById(Long id) {
        return ticketDAO
            .findById(id)
            .orElseThrow(() ->
                new IllegalArgumentException("Ticket not found: " + id)
            );
    }

    // -----------------------------------------------------------------------
    // Create
    // -----------------------------------------------------------------------

    public Ticket createTicket(
        String title,
        String description,
        Priority priority,
        Long reporterId
    ) {
        User reporter = userDAO
            .findById(reporterId)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Reporter not found: " + reporterId
                )
            );

        Ticket ticket = new Ticket(title, description, priority, reporter);
        ticket.setStatus(TicketStatus.OPEN);

        ticketDAO.save(ticket);

        // ADDED: Publish async event after DB commit
        publishTicketEvent(ticket.getId(), "CREATED");

        appendAudit(
            ticket,
            reporter,
            "CREATED",
            null,
            "Ticket created with priority " + priority
        );

        return ticket;
    }

    // -----------------------------------------------------------------------
    // State transitions — OPEN → IN_PROGRESS → CLOSED only
    // -----------------------------------------------------------------------

    public Ticket startProgress(Long ticketId, Long assigneeId) {
        Ticket ticket = getTicketById(ticketId);
        assertStatus(ticket, TicketStatus.OPEN, "start progress on");

        User assignee = userDAO
            .findById(assigneeId)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Assignee not found: " + assigneeId
                )
            );

        ticket.assignTo(assignee);

        ticketDAO.save(ticket);
        
        // ADDED: Publish async event after DB commit
        publishTicketEvent(ticket.getId(), "IN_PROGRESS");

        appendAudit(ticket, assignee, "STATUS_CHANGE", "OPEN", "IN_PROGRESS");

        return ticket;
    }

    public Ticket closeTicket(Long ticketId, Long closedByUserId) {
        Ticket ticket = getTicketById(ticketId);
        assertStatus(ticket, TicketStatus.IN_PROGRESS, "close");

        User closedBy = userDAO
            .findById(closedByUserId)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "User not found: " + closedByUserId
                )
            );

        ticket.close();

        ticketDAO.save(ticket);
        
        // ADDED: Publish async event after DB commit
        publishTicketEvent(ticket.getId(), "CLOSED");

        appendAudit(ticket, closedBy, "STATUS_CHANGE", "IN_PROGRESS", "CLOSED");

        return ticket;
    }

    // -----------------------------------------------------------------------
    // Priority update
    // -----------------------------------------------------------------------

    public Ticket updatePriority(
        Long ticketId,
        Priority newPriority,
        Long updatedByUserId
    ) {
        Ticket ticket = getTicketById(ticketId);

        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new IllegalStateException(
                "Cannot change priority of a CLOSED ticket."
            );
        }

        User updatedBy = userDAO
            .findById(updatedByUserId)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "User not found: " + updatedByUserId
                )
            );

        String oldPriority = ticket.getPriority().name();
        ticket.setPriority(newPriority);

        ticketDAO.save(ticket);
        
        // ADDED: Publish async event after DB commit
        publishTicketEvent(ticket.getId(), "PRIORITY_CHANGED");

        appendAudit(
            ticket,
            updatedBy,
            "PRIORITY_CHANGE",
            oldPriority,
            newPriority.name()
        );

        return ticket;
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private void assertStatus(
        Ticket ticket,
        TicketStatus required,
        String action
    ) {
        if (ticket.getStatus() != required) {
            throw new IllegalStateException(
                String.format(
                    "Cannot %s ticket #%d — expected status %s but was %s.",
                    action,
                    ticket.getId(),
                    required,
                    ticket.getStatus()
                )
            );
        }
    }

    private void appendAudit(
        Ticket ticket,
        User actor,
        String action,
        String oldValue,
        String newValue
    ) {
        AuditLog log = new AuditLog(ticket, actor, action, oldValue, newValue);
        Session session = sessionFactory.getCurrentSession();
        session.save(log);
    }

    // -----------------------------------------------------------------------
    // ADDED: RabbitMQ Event Publisher
    // -----------------------------------------------------------------------
    
    /**
     * Safely publishes a message to RabbitMQ ONLY after the database transaction 
     * has successfully committed. This prevents sending notifications for rolled-back transactions.
     */
    private void publishTicketEvent(Long ticketId, String eventType) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    String routingKey = "ticket.event." + eventType.toLowerCase();
                    rabbitTemplate.convertAndSend(RabbitConfig.TICKET_EXCHANGE, routingKey, ticketId);
                } catch (Exception e) {
                    // Log the error, but don't fail the main transaction if RabbitMQ is down
                    System.err.println("Failed to publish RabbitMQ event for ticket " + ticketId + ": " + e.getMessage());
                }
            }
        });
    }
}