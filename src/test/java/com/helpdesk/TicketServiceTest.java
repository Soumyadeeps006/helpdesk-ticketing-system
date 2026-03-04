package com.helpdesk;

import static org.junit.jupiter.api.Assertions.*;

import com.helpdesk.dao.TicketDAO;
import com.helpdesk.dao.UserDAO;
import com.helpdesk.enums.Priority;
import com.helpdesk.enums.TicketStatus;
import com.helpdesk.model.Ticket;
import com.helpdesk.model.User;
import java.util.List;
import java.util.Optional;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Unit tests for Ticket persistence and state-transition logic.
 * Path: src/test/java/com/helpdesk/TicketServiceTest.java
 *
 * Uses an in-memory H2 database — no external dependencies required.
 * The SessionFactory is rebuilt once per test class (expensive); each test
 * runs in its own transaction that is rolled back on teardown.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TicketServiceTest {

    private static SessionFactory sessionFactory;
    private TicketDAO ticketDAO;
    private UserDAO userDAO;
    private User employee;
    private User staffMember;

    // -------------------------------------------------------------------------
    // Test lifecycle
    // -------------------------------------------------------------------------

    @BeforeAll
    static void setUpSessionFactory() {
        sessionFactory = new Configuration()
            .configure("hibernate-test.cfg.xml") // points to H2 in-memory DB
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

        // Seed users for each test
        employee = userDAO.save(
            new User("emp1", "emp1@test.com", "pass", "EMPLOYEE", "Alice")
        );
        staffMember = userDAO.save(
            new User("staff1", "staff@test.com", "pass", "IT_STAFF", "Bob")
        );
    }

    @AfterEach
    void cleanUp() {
        // Flush & clear — H2 is recreated with create-drop, so full clean happens at class level
        sessionFactory.getCurrentSession().clear();
    }

    // -------------------------------------------------------------------------
    // 2.2a — Save and retrieve a ticket
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("2.2a — Save ticket and retrieve by ID")
    void testSaveAndFindById() {
        Ticket ticket = new Ticket(
            "Keyboard broken",
            "Keys 1-9 unresponsive",
            Priority.HIGH,
            employee
        );
        Ticket saved = ticketDAO.save(ticket);

        assertNotNull(
            saved.getId(),
            "Ticket ID should be generated after save"
        );

        Optional<Ticket> found = ticketDAO.findById(saved.getId());
        assertTrue(found.isPresent(), "Ticket should be found by ID");
        assertEquals("Keyboard broken", found.get().getTitle());
        assertEquals(TicketStatus.OPEN, found.get().getStatus());
        assertEquals(Priority.HIGH, found.get().getPriority());
    }

    // -------------------------------------------------------------------------
    // 2.2b — Default status is OPEN
    // -------------------------------------------------------------------------

    @Test
    @Order(2)
    @DisplayName("2.2b — New ticket defaults to OPEN status")
    void testDefaultStatus() {
        Ticket ticket = ticketDAO.save(
            new Ticket(
                "Network issue",
                "Cannot connect to VPN",
                Priority.MEDIUM,
                employee
            )
        );
        assertEquals(TicketStatus.OPEN, ticket.getStatus());
    }

    // -------------------------------------------------------------------------
    // 2.2c — State transition: OPEN → IN_PROGRESS → CLOSED
    // -------------------------------------------------------------------------

    @Test
    @Order(3)
    @DisplayName("2.2c — State transition OPEN → IN_PROGRESS → CLOSED succeeds")
    void testHappyPathStateTransition() {
        Ticket ticket = ticketDAO.save(
            new Ticket(
                "Monitor flicker",
                "Screen flickers on startup",
                Priority.LOW,
                employee
            )
        );

        // OPEN → IN_PROGRESS
        ticket.assignTo(staffMember);
        ticketDAO.save(ticket);
        assertEquals(TicketStatus.IN_PROGRESS, ticket.getStatus());
        assertEquals(staffMember.getId(), ticket.getAssignedTo().getId());

        // IN_PROGRESS → CLOSED
        ticket.close();
        ticketDAO.save(ticket);
        assertEquals(TicketStatus.CLOSED, ticket.getStatus());
        assertNotNull(
            ticket.getResolvedAt(),
            "resolvedAt must be set on close"
        );
    }

    // -------------------------------------------------------------------------
    // 2.2d — Invalid transitions throw IllegalStateException
    // -------------------------------------------------------------------------

    @Test
    @Order(4)
    @DisplayName("2.2d — Cannot close an OPEN ticket directly")
    void testDirectCloseFromOpenThrows() {
        Ticket ticket = ticketDAO.save(
            new Ticket("Printer jam", "Paper stuck", Priority.LOW, employee)
        );
        assertThrows(
            IllegalStateException.class,
            ticket::close,
            "Closing from OPEN should throw IllegalStateException"
        );
    }

    @Test
    @Order(5)
    @DisplayName("2.2d — Cannot assign a ticket that is already IN_PROGRESS")
    void testDoubleAssignThrows() {
        Ticket ticket = ticketDAO.save(
            new Ticket(
                "Email down",
                "Outlook not loading",
                Priority.HIGH,
                employee
            )
        );
        ticket.assignTo(staffMember);
        assertThrows(
            IllegalStateException.class,
            () -> ticket.assignTo(staffMember),
            "Second assignTo should throw IllegalStateException"
        );
    }

    // -------------------------------------------------------------------------
    // 2.2e — findByStatus
    // -------------------------------------------------------------------------

    @Test
    @Order(6)
    @DisplayName("2.2e — findByStatus returns only matching tickets")
    void testFindByStatus() {
        ticketDAO.save(new Ticket("T-OPEN-1", "desc", Priority.LOW, employee));
        ticketDAO.save(
            new Ticket("T-OPEN-2", "desc", Priority.MEDIUM, employee)
        );

        Ticket inProgress = ticketDAO.save(
            new Ticket("T-PROG-1", "desc", Priority.HIGH, employee)
        );
        inProgress.assignTo(staffMember);
        ticketDAO.save(inProgress);

        List<Ticket> openTickets = ticketDAO.findByStatus(TicketStatus.OPEN);
        assertTrue(
            openTickets.size() >= 2,
            "Should find at least 2 OPEN tickets"
        );
        openTickets.forEach(t ->
            assertEquals(
                TicketStatus.OPEN,
                t.getStatus(),
                "All returned tickets must be OPEN"
            )
        );
    }

    // -------------------------------------------------------------------------
    // 2.2f — Parameterized: every Priority value persists correctly
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @EnumSource(Priority.class)
    @Order(7)
    @DisplayName(
        "2.2f — All Priority enum values persist and retrieve correctly"
    )
    void testAllPrioritiesPersist(Priority priority) {
        Ticket ticket = ticketDAO.save(
            new Ticket(
                "Priority test",
                "Testing " + priority,
                priority,
                employee
            )
        );
        Optional<Ticket> found = ticketDAO.findById(ticket.getId());
        assertTrue(found.isPresent());
        assertEquals(priority, found.get().getPriority());
    }

    // -------------------------------------------------------------------------
    // 2.2g — Search by keyword
    // -------------------------------------------------------------------------

    @Test
    @Order(8)
    @DisplayName("2.2g — Keyword search finds tickets by title and description")
    void testKeywordSearch() {
        ticketDAO.save(
            new Ticket(
                "Outlook crash",
                "Outlook keeps freezing",
                Priority.HIGH,
                employee
            )
        );
        ticketDAO.save(
            new Ticket(
                "Teams issue",
                "Teams audio not working",
                Priority.MEDIUM,
                employee
            )
        );
        ticketDAO.save(
            new Ticket(
                "Laptop restart",
                "Laptop restarts randomly",
                Priority.LOW,
                employee
            )
        );

        List<Ticket> results = ticketDAO.search("outlook");
        assertFalse(
            results.isEmpty(),
            "Search for 'outlook' should return results"
        );
        assertTrue(
            results
                .stream()
                .anyMatch(t -> t.getTitle().toLowerCase().contains("outlook"))
        );
    }

    // -------------------------------------------------------------------------
    // 2.2h — Delete ticket
    // -------------------------------------------------------------------------

    @Test
    @Order(9)
    @DisplayName("2.2h — Deleted ticket is no longer retrievable")
    void testDelete() {
        Ticket ticket = ticketDAO.save(
            new Ticket(
                "To be deleted",
                "Will be removed",
                Priority.LOW,
                employee
            )
        );
        Long id = ticket.getId();

        ticketDAO.delete(id);
        Optional<Ticket> found = ticketDAO.findById(id);
        assertFalse(found.isPresent(), "Deleted ticket should not be found");
    }
}
