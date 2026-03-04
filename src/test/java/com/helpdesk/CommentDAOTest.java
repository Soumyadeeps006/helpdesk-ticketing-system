package com.helpdesk;

import static org.junit.jupiter.api.Assertions.*;

import com.helpdesk.dao.CommentDAO;
import com.helpdesk.dao.TicketDAO;
import com.helpdesk.dao.UserDAO;
import com.helpdesk.enums.Priority;
import com.helpdesk.model.Comment;
import com.helpdesk.model.Ticket;
import com.helpdesk.model.User;
import java.util.List;
import java.util.Optional;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.*;

/**
 * Unit tests for the self-referencing Comment entity and CommentDAO.
 * Path: src/test/java/com/helpdesk/CommentDAOTest.java
 *
 * Core focus:
 *   - Persisting root comments and nested replies
 *   - {@link CommentDAO#findTreeByTicketId(Long)} assembles correct tree structure
 *   - Parent/children relationships are preserved after round-trip through H2
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CommentDAOTest {

    private static SessionFactory sessionFactory;

    private CommentDAO commentDAO;
    private TicketDAO ticketDAO;
    private UserDAO userDAO;

    private User employee;
    private User staffMember;
    private Ticket ticket;

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
        commentDAO = new CommentDAO(sessionFactory);
        ticketDAO = new TicketDAO(sessionFactory);
        userDAO = new UserDAO(sessionFactory);

        employee = userDAO.save(
            new User("cemp", "cemp@test.com", "pass", "EMPLOYEE", "Carol")
        );
        staffMember = userDAO.save(
            new User("cstaff", "cstaff@test.com", "pass", "IT_STAFF", "Dave")
        );
        ticket = ticketDAO.save(
            new Ticket(
                "Comment test ticket",
                "Used in CommentDAO tests",
                Priority.LOW,
                employee
            )
        );
    }

    @AfterEach
    void cleanUp() {
        sessionFactory.getCurrentSession().clear();
    }

    // -------------------------------------------------------------------------
    // 2.3a — Save and retrieve a root comment
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("2.3a — Save root comment and retrieve by ID")
    void testSaveRootComment() {
        Comment comment = commentDAO.save(
            new Comment("Issue confirmed.", ticket, employee)
        );

        assertNotNull(comment.getId(), "Comment ID should be generated");
        Optional<Comment> found = commentDAO.findById(comment.getId());
        assertTrue(found.isPresent());
        assertEquals("Issue confirmed.", found.get().getBody());
        assertNull(
            found.get().getParent(),
            "Root comment should have no parent"
        );
    }

    // -------------------------------------------------------------------------
    // 2.3b — Self-referencing: save a reply to a comment
    // -------------------------------------------------------------------------

    @Test
    @Order(2)
    @DisplayName("2.3b — Reply comment correctly references its parent")
    void testSaveReplyComment() {
        Comment root = commentDAO.save(
            new Comment("Root comment", ticket, employee)
        );
        Comment reply = commentDAO.save(
            new Comment("Reply to root", ticket, staffMember, root)
        );

        Optional<Comment> found = commentDAO.findById(reply.getId());
        assertTrue(found.isPresent());
        assertNotNull(found.get().getParent(), "Reply should have a parent");
        assertEquals(root.getId(), found.get().getParent().getId());
    }

    // -------------------------------------------------------------------------
    // 2.3c — Three-level nesting
    // -------------------------------------------------------------------------

    @Test
    @Order(3)
    @DisplayName("2.3c — Three-level comment nesting persists correctly")
    void testThreeLevelNesting() {
        Comment root = commentDAO.save(
            new Comment("Level 0 — root", ticket, employee)
        );
        Comment level1 = commentDAO.save(
            new Comment("Level 1 — reply", ticket, staffMember, root)
        );
        Comment level2 = commentDAO.save(
            new Comment("Level 2 — nested", ticket, employee, level1)
        );

        // Verify depth via getDepth()
        assertEquals(0, root.getDepth(), "Root depth should be 0");
        assertEquals(1, level1.getDepth(), "Level-1 depth should be 1");
        assertEquals(2, level2.getDepth(), "Level-2 depth should be 2");

        // Verify parent chain
        Optional<Comment> foundL2 = commentDAO.findById(level2.getId());
        assertTrue(foundL2.isPresent());
        assertEquals(level1.getId(), foundL2.get().getParent().getId());
        assertEquals(
            root.getId(),
            foundL2.get().getParent().getParent().getId()
        );
    }

    // -------------------------------------------------------------------------
    // 2.3d — findTreeByTicketId assembles correct root list
    // -------------------------------------------------------------------------

    @Test
    @Order(4)
    @DisplayName(
        "2.3d — findTreeByTicketId returns only root comments at top level"
    )
    void testFindTreeRootsOnly() {
        Comment r1 = commentDAO.save(new Comment("Root 1", ticket, employee));
        Comment r2 = commentDAO.save(
            new Comment("Root 2", ticket, staffMember)
        );
        commentDAO.save(new Comment("Reply to r1", ticket, staffMember, r1));
        commentDAO.save(new Comment("Reply to r2", ticket, employee, r2));

        List<Comment> tree = commentDAO.findTreeByTicketId(ticket.getId());
        assertEquals(
            2,
            tree.size(),
            "Only 2 root comments expected at top level"
        );
    }

    // -------------------------------------------------------------------------
    // 2.3e — findTreeByTicketId populates children lists
    // -------------------------------------------------------------------------

    @Test
    @Order(5)
    @DisplayName(
        "2.3e — findTreeByTicketId populates children on root comments"
    )
    void testFindTreeChildrenPopulated() {
        Comment root = commentDAO.save(
            new Comment("Root with replies", ticket, employee)
        );
        Comment reply1 = commentDAO.save(
            new Comment("Reply 1", ticket, staffMember, root)
        );
        commentDAO.save(new Comment("Reply 2", ticket, employee, root));
        // Also a nested reply — should appear under reply1, not root
        commentDAO.save(new Comment("Nested", ticket, staffMember, reply1));

        List<Comment> tree = commentDAO.findTreeByTicketId(ticket.getId());

        // Find the root in the returned tree
        Comment foundRoot = tree
            .stream()
            .filter(c -> c.getId().equals(root.getId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Root not found in tree"));

        assertEquals(
            2,
            foundRoot.getChildren().size(),
            "Root should have exactly 2 direct children"
        );

        Comment foundReply1 = foundRoot
            .getChildren()
            .stream()
            .filter(c -> c.getId().equals(reply1.getId()))
            .findFirst()
            .orElseThrow(() ->
                new AssertionError("Reply1 not found in root's children")
            );

        assertEquals(
            1,
            foundReply1.getChildren().size(),
            "Reply1 should have 1 nested child"
        );
    }

    // -------------------------------------------------------------------------
    // 2.3f — countByTicketId
    // -------------------------------------------------------------------------

    @Test
    @Order(6)
    @DisplayName("2.3f — countByTicketId counts all comments across all levels")
    void testCountByTicketId() {
        Comment root = commentDAO.save(new Comment("Root", ticket, employee));
        Comment reply = commentDAO.save(
            new Comment("Reply", ticket, staffMember, root)
        );
        commentDAO.save(new Comment("Nested", ticket, employee, reply));

        long count = commentDAO.countByTicketId(ticket.getId());
        assertEquals(
            3L,
            count,
            "Should count all 3 comments across all levels"
        );
    }

    // -------------------------------------------------------------------------
    // 2.3g — isRootComment helper
    // -------------------------------------------------------------------------

    @Test
    @Order(7)
    @DisplayName(
        "2.3g — isRootComment returns true only for comments without a parent"
    )
    void testIsRootComment() {
        Comment root = commentDAO.save(new Comment("Root", ticket, employee));
        Comment reply = commentDAO.save(
            new Comment("Reply", ticket, staffMember, root)
        );

        assertTrue(
            root.isRootComment(),
            "Root should report isRootComment = true"
        );
        assertFalse(
            reply.isRootComment(),
            "Reply should report isRootComment = false"
        );
    }

    // -------------------------------------------------------------------------
    // 2.3h — Deleting a parent cascades to children
    // -------------------------------------------------------------------------

    @Test
    @Order(8)
    @DisplayName(
        "2.3h — Deleting a parent comment cascades delete to its children"
    )
    void testCascadeDeleteOnParent() {
        Comment root = commentDAO.save(
            new Comment("Will be deleted", ticket, employee)
        );
        Comment child = commentDAO.save(
            new Comment("Child of deleted", ticket, staffMember, root)
        );

        Long rootId = root.getId();
        Long childId = child.getId();

        commentDAO.delete(rootId);

        assertFalse(
            commentDAO.findById(rootId).isPresent(),
            "Root should be deleted"
        );
        assertFalse(
            commentDAO.findById(childId).isPresent(),
            "Child should be cascade-deleted"
        );
    }
}
