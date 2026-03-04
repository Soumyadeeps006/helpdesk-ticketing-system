package com.helpdesk.service;

import com.helpdesk.dao.CommentDAO;
import com.helpdesk.dao.TicketDAO;
import com.helpdesk.dao.UserDAO;
import com.helpdesk.model.Comment;
import com.helpdesk.model.Ticket;
import com.helpdesk.model.User;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CommentService {

    private final CommentDAO commentDAO;
    private final TicketDAO ticketDAO;
    private final UserDAO userDAO;

    @Autowired
    public CommentService(
        CommentDAO commentDAO,
        TicketDAO ticketDAO,
        UserDAO userDAO
    ) {
        this.commentDAO = commentDAO;
        this.ticketDAO = ticketDAO;
        this.userDAO = userDAO;
    }

    // -----------------------------------------------------------------------
    // Read
    // -----------------------------------------------------------------------

    /**
     * Returns the full comment tree for a ticket (root comments + nested replies),
     * loaded via the JOIN FETCH HQL in CommentDAO.
     */
    @Transactional(readOnly = true)
    public List<Comment> getCommentTree(Long ticketId) {
        return commentDAO.findTreeByTicketId(ticketId);
    }

    // -----------------------------------------------------------------------
    // Create top-level comment
    // -----------------------------------------------------------------------

    public Comment addComment(Long ticketId, Long authorId, String body) {
        Ticket ticket = ticketDAO
            .findById(ticketId)
            .orElseThrow(() ->
                new IllegalArgumentException("Ticket not found: " + ticketId)
            );

        User author = userDAO
            .findById(authorId)
            .orElseThrow(() ->
                new IllegalArgumentException("User not found: " + authorId)
            );

        // Comment(body, ticket, author) constructor — createdAt set by @CreationTimestamp
        Comment comment = new Comment(body, ticket, author);
        commentDAO.save(comment);
        return comment;
    }

    // -----------------------------------------------------------------------
    // Create nested reply
    // -----------------------------------------------------------------------

    public Comment addReply(
        Long ticketId,
        Long parentCommentId,
        Long authorId,
        String body
    ) {
        Ticket ticket = ticketDAO
            .findById(ticketId)
            .orElseThrow(() ->
                new IllegalArgumentException("Ticket not found: " + ticketId)
            );

        Comment parent = commentDAO
            .findById(parentCommentId)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Parent comment not found: " + parentCommentId
                )
            );

        if (!parent.getTicket().getId().equals(ticketId)) {
            throw new IllegalArgumentException(
                "Parent comment does not belong to ticket " + ticketId
            );
        }

        User author = userDAO
            .findById(authorId)
            .orElseThrow(() ->
                new IllegalArgumentException("User not found: " + authorId)
            );

        // Comment(body, ticket, author, parent) constructor — createdAt set by @CreationTimestamp
        Comment reply = new Comment(body, ticket, author, parent);
        commentDAO.save(reply);
        return reply;
    }
}
