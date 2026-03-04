package com.helpdesk;

import static org.junit.jupiter.api.Assertions.*;

import com.helpdesk.dao.CommentDAO;
import com.helpdesk.dao.TicketDAO;
import com.helpdesk.dao.UserDAO;
import com.helpdesk.enums.Priority;
import com.helpdesk.model.Comment;
import com.helpdesk.model.Ticket;
import com.helpdesk.model.User;
import com.helpdesk.service.CommentService;
import com.helpdesk.service.TicketService;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.*;

/**
 * Integration tests for the self-referencing comment tree.
 * Path: src/test/java/com/helpdesk/CommentTreeIT.java
 *
 * Core focus:
 *   - CommentDAO.findTreeByTicketId (JOIN FETCH) returns correct root list
 *   - Parent/child relationships are intact after a round-trip through H2
 *   - Deep nesting (grandchild) chains correctly
 *   - Tree is scoped to the requested ticket only
 *
 * CommentService API used here:
 *   addComment(Long ticketId, Long authorId, String body)          — root comment
 *   addReply(Long ticketId, Long parentId, Long authorId, String)  — nested reply
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CommentTreeIT {

    private static SessionFactory sessionFactory;

    private CommentService commentService;
    private CommentDAO commentDAO;
    private TicketDAO ticketDAO;
    private UserDAO userDAO;

    private User alice;
    private User bob;
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
        // CommentService(CommentDAO, TicketDAO, UserDAO) — no SessionFactory param
        commentService = new CommentService(commentDAO, ticketDAO, userDAO);

        TicketService ticketService = new TicketService(
            ticketDAO,
            userDAO,
            sessionFactory
        );

        alice = userDAO.save(
            new User(
                "alice_ct",
                "alice_ct@test.com",
                "pass",
                "EMPLOYEE",
                "Alice"
            )
        );
        bob = userDAO.save(
            new User("bob_ct", "bob_ct@test.com", "pass", "IT_STAFF", "Bob")
        );
        ticket = ticketService.createTicket(
            "Comment tree ticket",
            "Ticket used for comment tree integration tests.",
            Priority.LOW,
            alice.getId()
        );
    }

    @AfterEach
    void cleanUp() {
        sessionFactory.getCurrentSession().clear();
    }

    // -------------------------------------------------------------------------
    // 5.3a — Root comment has no parent
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("5.3a — Root comment persisted with no parent")
    void testRootCommentHasNoParent() {
        commentService.addComment(
            ticket.getId(),
            alice.getId(),
            "This is the root comment."
        );

        List<Comment> tree = commentDAO.findTreeByTicketId(ticket.getId());
        assertEquals(1, tree.size(), "Exactly one root comment expected");

        Comment root = tree.get(0);
        assertEquals("This is the root comment.", root.getBody());
        assertNull(root.getParent(), "Root comment must have no parent");
        assertEquals(alice.getId(), root.getAuthor().getId());
    }

    // -------------------------------------------------------------------------
    // 5.3b — Two replies are attached to their parent via addReply
    // -------------------------------------------------------------------------

    @Test
    @Order(2)
    @DisplayName("5.3b — Two replies are correctly attached to their parent")
    void testTwoRepliesAttachedToParent() {
        Comment root = commentService.addComment(
            ticket.getId(),
            alice.getId(),
            "Root: issue description."
        );
        commentService.addReply(
            ticket.getId(),
            root.getId(),
            bob.getId(),
            "Reply 1: I can reproduce this."
        );
        commentService.addReply(
            ticket.getId(),
            root.getId(),
            alice.getId(),
            "Reply 2: Here is a workaround."
        );

        List<Comment> tree = commentDAO.findTreeByTicketId(ticket.getId());
        Comment fetchedRoot = tree
            .stream()
            .filter(c -> c.getParent() == null)
            .findFirst()
            .orElseThrow(() ->
                new AssertionError("Root comment not found in tree")
            );

        assertEquals("Root: issue description.", fetchedRoot.getBody());
        assertEquals(
            2,
            fetchedRoot.getChildren().size(),
            "Root should have exactly 2 direct children"
        );

        List<String> replyBodies = fetchedRoot
            .getChildren()
            .stream()
            .map(Comment::getBody)
            .toList();
        assertTrue(replyBodies.contains("Reply 1: I can reproduce this."));
        assertTrue(replyBodies.contains("Reply 2: Here is a workaround."));
    }

    // -------------------------------------------------------------------------
    // 5.3c — Three-level (grandchild) chain
    // -------------------------------------------------------------------------

    @Test
    @Order(3)
    @DisplayName(
        "5.3c — Three-level nesting chains root → child → grandchild correctly"
    )
    void testThreeLevelNestingChain() {
        Comment root = commentService.addComment(
            ticket.getId(),
            alice.getId(),
            "Root comment."
        );
        Comment child = commentService.addReply(
            ticket.getId(),
            root.getId(),
            bob.getId(),
            "Child reply."
        );
        commentService.addReply(
            ticket.getId(),
            child.getId(),
            alice.getId(),
            "Grandchild reply."
        );

        List<Comment> tree = commentDAO.findTreeByTicketId(ticket.getId());
        Comment fetchedRoot = tree
            .stream()
            .filter(c -> c.getParent() == null)
            .findFirst()
            .orElseThrow();

        assertEquals(
            1,
            fetchedRoot.getChildren().size(),
            "Root should have exactly one direct child"
        );

        Comment fetchedChild = fetchedRoot.getChildren().get(0);
        assertEquals("Child reply.", fetchedChild.getBody());
        assertEquals(
            1,
            fetchedChild.getChildren().size(),
            "Child should have exactly one grandchild"
        );

        Comment fetchedGrandchild = fetchedChild.getChildren().get(0);
        assertEquals("Grandchild reply.", fetchedGrandchild.getBody());
        assertTrue(
            fetchedGrandchild.getChildren().isEmpty(),
            "Grandchild should have no further replies"
        );
    }

    // -------------------------------------------------------------------------
    // 5.3d — findTreeByTicketId is scoped to the requested ticket
    // -------------------------------------------------------------------------

    @Test
    @Order(4)
    @DisplayName(
        "5.3d — findTreeByTicketId returns only comments for the requested ticket"
    )
    void testTreeScopedToTicket() {
        TicketService ticketService = new TicketService(
            ticketDAO,
            userDAO,
            sessionFactory
        );
        Ticket otherTicket = ticketService.createTicket(
            "Other ticket",
            "Another ticket.",
            Priority.LOW,
            bob.getId()
        );

        commentService.addComment(
            ticket.getId(),
            alice.getId(),
            "Comment on ticket A."
        );
        commentService.addComment(
            otherTicket.getId(),
            bob.getId(),
            "Comment on ticket B."
        );

        List<Comment> treeA = commentDAO.findTreeByTicketId(ticket.getId());
        List<Comment> treeB = commentDAO.findTreeByTicketId(
            otherTicket.getId()
        );

        assertEquals(1, treeA.size(), "Ticket A should have exactly 1 comment");
        assertEquals(1, treeB.size(), "Ticket B should have exactly 1 comment");
        assertEquals("Comment on ticket A.", treeA.get(0).getBody());
        assertEquals("Comment on ticket B.", treeB.get(0).getBody());
    }
}
