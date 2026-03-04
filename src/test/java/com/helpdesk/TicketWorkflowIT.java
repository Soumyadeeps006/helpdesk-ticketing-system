package com.helpdesk;

import static org.junit.jupiter.api.Assertions.*;

import com.helpdesk.dao.TicketDAO;
import com.helpdesk.dao.UserDAO;
import com.helpdesk.enums.Priority;
import com.helpdesk.enums.TicketStatus;
import com.helpdesk.model.Ticket;
import com.helpdesk.model.User;
import com.helpdesk.service.TicketService;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.*;

/**
 * Integration tests for the full ticket lifecycle.
 * Path: src/test/java/com/helpdesk/TicketWorkflowIT.java
 *
 * Core focus:
 *   - End-to-end happy path: OPEN → IN_PROGRESS → CLOSED
 *   - State-guard enforcement (illegal transitions throw IllegalStateException)
 *   - TicketDAO.findById reflects each status after every service call
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TicketWorkflowIT {

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
                "alice_wf",
                "alice_wf@test.com",
                "pass",
                "EMPLOYEE",
                "Alice"
            )
        );
        agent = userDAO.save(
            new User("bob_wf", "bob_wf@test.com", "pass", "IT_STAFF", "Bob")
        );
    }

    @AfterEach
    void cleanUp() {
        sessionFactory.getCurrentSession().clear();
    }

    // -------------------------------------------------------------------------
    // 5.1a — Newly created ticket has status OPEN
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("5.1a — Newly created ticket has status OPEN")
    void testCreateTicketIsOpen() {
        Ticket ticket = ticketService.createTicket(
            "Printer not working",
            "Office printer on floor 3 is jammed.",
            Priority.HIGH,
            reporter.getId()
        );

        assertNotNull(
            ticket.getId(),
            "Ticket ID should be generated after save"
        );
        assertEquals(
            TicketStatus.OPEN,
            ticket.getStatus(),
            "Newly created ticket must have status OPEN"
        );
    }

    // -------------------------------------------------------------------------
    // 5.1b — startProgress assigns agent and moves ticket to IN_PROGRESS
    // -------------------------------------------------------------------------

    @Test
    @Order(2)
    @DisplayName("5.1b — startProgress sets assignedTo and status IN_PROGRESS")
    void testStartProgress() {
        Ticket ticket = ticketService.createTicket(
            "VPN not connecting",
            "User cannot reach internal network via VPN.",
            Priority.CRITICAL,
            reporter.getId()
        );
        Long id = ticket.getId();

        ticketService.startProgress(id, agent.getId());

        Ticket inProgress = ticketDAO.findById(id).get();
        assertEquals(
            TicketStatus.IN_PROGRESS,
            inProgress.getStatus(),
            "Ticket should be IN_PROGRESS after startProgress"
        );
        assertNotNull(
            inProgress.getAssignedTo(),
            "assignedTo must be set after startProgress"
        );
        assertEquals(
            agent.getId(),
            inProgress.getAssignedTo().getId(),
            "Assignee on the persisted ticket should match the assigned agent"
        );
    }

    // -------------------------------------------------------------------------
    // 5.1c — Full happy path ends in CLOSED with resolvedAt set
    // -------------------------------------------------------------------------

    @Test
    @Order(3)
    @DisplayName(
        "5.1c — Full lifecycle OPEN → IN_PROGRESS → CLOSED persists correctly"
    )
    void testFullLifecycle() {
        Ticket ticket = ticketService.createTicket(
            "Monitor flickering",
            "Second monitor flickers when connected via HDMI.",
            Priority.MEDIUM,
            reporter.getId()
        );
        Long id = ticket.getId();

        ticketService.startProgress(id, agent.getId());
        ticketService.closeTicket(id, agent.getId());

        Ticket closed = ticketDAO.findById(id).get();
        assertEquals(
            TicketStatus.CLOSED,
            closed.getStatus(),
            "Ticket should be CLOSED after closeTicket"
        );
        assertNotNull(
            closed.getResolvedAt(),
            "resolvedAt must be set when a ticket is closed"
        );
    }

    // -------------------------------------------------------------------------
    // 5.1d — startProgress on a non-OPEN ticket throws IllegalStateException
    // -------------------------------------------------------------------------

    @Test
    @Order(4)
    @DisplayName(
        "5.1d — startProgress on IN_PROGRESS ticket throws IllegalStateException"
    )
    void testStartProgressOnAlreadyProgressedTicketThrows() {
        Ticket ticket = ticketService.createTicket(
            "Already-in-progress ticket",
            "Cannot call startProgress twice.",
            Priority.LOW,
            reporter.getId()
        );
        Long id = ticket.getId();

        ticketService.startProgress(id, agent.getId());

        assertThrows(
            IllegalStateException.class,
            () -> ticketService.startProgress(id, agent.getId()),
            "Calling startProgress on an IN_PROGRESS ticket should throw IllegalStateException"
        );
    }

    // -------------------------------------------------------------------------
    // 5.1e — closeTicket on an OPEN ticket throws IllegalStateException
    // -------------------------------------------------------------------------

    @Test
    @Order(5)
    @DisplayName(
        "5.1e — closeTicket on OPEN ticket throws IllegalStateException"
    )
    void testCloseFromOpenThrows() {
        Ticket ticket = ticketService.createTicket(
            "Direct-close attempt",
            "Cannot jump from OPEN straight to CLOSED.",
            Priority.MEDIUM,
            reporter.getId()
        );
        Long id = ticket.getId();

        assertThrows(
            IllegalStateException.class,
            () -> ticketService.closeTicket(id, reporter.getId()),
            "Closing an OPEN ticket (skipping IN_PROGRESS) should throw IllegalStateException"
        );
    }
}
