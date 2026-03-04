package com.helpdesk;

import static org.junit.jupiter.api.Assertions.*;

import com.helpdesk.dao.TicketDAO;
import com.helpdesk.dao.UserDAO;
import com.helpdesk.enums.Priority;
import com.helpdesk.model.AuditLog;
import com.helpdesk.model.Ticket;
import com.helpdesk.model.User;
import com.helpdesk.service.TicketService;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.*;

/**
 * Integration tests for the audit trail produced by TicketService.
 * Path: src/test/java/com/helpdesk/AuditTrailIT.java
 *
 * Core focus:
 *   - Every TicketService write produces an AuditLog row
 *   - Action strings match what TicketService.appendAudit writes:
 *     "CREATED", "STATUS_CHANGE", "PRIORITY_CHANGE"
 *   - Actor and timestamp are correctly recorded on each entry
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuditTrailIT {

    private static SessionFactory sessionFactory;

    private TicketService ticketService;
    private TicketDAO ticketDAO;
    private UserDAO userDAO;

    private User reporter;
    private User agent;

    // -------------------------------------------------------------------------
    // Test lifecycle
    // -------------------------------------------------------------------------

    @BeforeAll
    static void setUpSessionFactory() {
        sessionFactory = new Configuration()
            .configure("hibernate-test.cfg.xml")
            .buildSessionFactory();
    }

    @AfterAll
    static void tearDownSessionFactory() {
        if (sessionFactory != null) sessionFactory.close();
    }

    @BeforeEach
    void setUp() {
        ticketDAO = new TicketDAO(sessionFactory);
        userDAO = new UserDAO(sessionFactory);
        ticketService = new TicketService(ticketDAO, userDAO, sessionFactory);

        reporter = userDAO.save(
            new User(
                "carol_at",
                "carol_at@test.com",
                "pass",
                "EMPLOYEE",
                "Carol"
            )
        );
        agent = userDAO.save(
            new User("dave_at", "dave_at@test.com", "pass", "IT_STAFF", "Dave")
        );
    }

    @AfterEach
    void cleanUp() {
        sessionFactory.getCurrentSession().clear();
    }

    /** Load all AuditLog rows for a ticket, ordered by timestamp. */
    @SuppressWarnings("unchecked")
    private List<AuditLog> logsForTicket(Long ticketId) {
        return sessionFactory
            .getCurrentSession()
            .createQuery(
                "FROM AuditLog a WHERE a.ticket.id = :tid ORDER BY a.timestamp ASC"
            )
            .setParameter("tid", ticketId)
            .list();
    }

    // -------------------------------------------------------------------------
    // 5.2a — createTicket produces one "CREATED" audit entry
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("5.2a — createTicket produces one CREATED audit entry")
    void testCreateProducesAuditEntry() {
        Ticket ticket = ticketService.createTicket(
            "Audit-create test",
            "Verifies creation audit.",
            Priority.LOW,
            reporter.getId()
        );

        List<AuditLog> logs = logsForTicket(ticket.getId());
        assertEquals(
            1,
            logs.size(),
            "One audit entry expected after ticket creation"
        );
        assertEquals("CREATED", logs.get(0).getAction());
        assertEquals(reporter.getId(), logs.get(0).getActor().getId());
    }

    // -------------------------------------------------------------------------
    // 5.2b — Full lifecycle produces three STATUS_CHANGE entries (+ 1 CREATED)
    // -------------------------------------------------------------------------

    @Test
    @Order(2)
    @DisplayName("5.2b — Full lifecycle produces 3 audit entries total")
    void testFullLifecycleAuditEntries() {
        Ticket ticket = ticketService.createTicket(
            "Audit full-lifecycle",
            "End-to-end audit trail verification.",
            Priority.MEDIUM,
            reporter.getId()
        );
        Long id = ticket.getId();

        ticketService.startProgress(id, agent.getId());
        ticketService.closeTicket(id, agent.getId());

        List<AuditLog> logs = logsForTicket(id);
        assertEquals(
            3,
            logs.size(),
            "Expected 3 audit entries: CREATED, STATUS_CHANGE (x2)"
        );

        assertEquals("CREATED", logs.get(0).getAction());
        assertEquals("STATUS_CHANGE", logs.get(1).getAction());
        assertEquals("STATUS_CHANGE", logs.get(2).getAction());
    }

    // -------------------------------------------------------------------------
    // 5.2c — Actor is recorded correctly per operation
    // -------------------------------------------------------------------------

    @Test
    @Order(3)
    @DisplayName("5.2c — Correct actor is recorded on each audit entry")
    void testAuditEntriesRecordCorrectActor() {
        Ticket ticket = ticketService.createTicket(
            "Actor-audit test",
            "Verify actor is recorded per action.",
            Priority.HIGH,
            reporter.getId()
        );
        Long id = ticket.getId();

        ticketService.startProgress(id, agent.getId());

        List<AuditLog> logs = logsForTicket(id);
        assertEquals(
            reporter.getId(),
            logs.get(0).getActor().getId(),
            "Reporter should be the actor for CREATED"
        );
        assertEquals(
            agent.getId(),
            logs.get(1).getActor().getId(),
            "Agent should be the actor for STATUS_CHANGE (startProgress)"
        );
    }

    // -------------------------------------------------------------------------
    // 5.2d — Audit entries carry non-null timestamps
    // -------------------------------------------------------------------------

    @Test
    @Order(4)
    @DisplayName("5.2d — All audit entries have non-null timestamps")
    void testAuditEntriesHaveTimestamps() {
        Ticket ticket = ticketService.createTicket(
            "Timestamp-audit test",
            "Audit entries must carry timestamps.",
            Priority.CRITICAL,
            reporter.getId()
        );

        List<AuditLog> logs = logsForTicket(ticket.getId());
        assertFalse(logs.isEmpty(), "At least one audit entry expected");
        logs.forEach(log ->
            assertNotNull(
                log.getTimestamp(),
                "AuditLog.timestamp must never be null"
            )
        );
    }

    // -------------------------------------------------------------------------
    // 5.2e — Priority change produces a PRIORITY_CHANGE audit entry
    // -------------------------------------------------------------------------

    @Test
    @Order(5)
    @DisplayName("5.2e — updatePriority produces a PRIORITY_CHANGE audit entry")
    void testPriorityChangeAudit() {
        Ticket ticket = ticketService.createTicket(
            "Priority-audit test",
            "Verify priority changes are audited.",
            Priority.LOW,
            reporter.getId()
        );
        Long id = ticket.getId();

        ticketService.updatePriority(id, Priority.HIGH, reporter.getId());

        List<AuditLog> logs = logsForTicket(id);
        assertEquals(2, logs.size(), "Expected CREATED + PRIORITY_CHANGE");
        assertEquals("PRIORITY_CHANGE", logs.get(1).getAction());
        assertEquals("LOW", logs.get(1).getOldValue());
        assertEquals("HIGH", logs.get(1).getNewValue());
    }
}
