package com.helpdesk.controller;

import com.helpdesk.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles comment and nested-reply creation for a ticket.
 * Routes under /tickets/{ticketId}/comments.
 */
@Controller
@RequestMapping("/tickets/{ticketId}/comments")
public class CommentController {

    private final CommentService commentService;

    @Autowired
    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    // ------------------------------------------------------------------
    // POST /tickets/{ticketId}/comments  → add a top-level comment
    // ------------------------------------------------------------------
    @PostMapping
    public String addComment(
        @PathVariable Long ticketId,
        @RequestParam Long authorId, // in real app: from session
        @RequestParam String body,
        RedirectAttributes redirectAttributes
    ) {
        try {
            commentService.addComment(ticketId, authorId, body);
            redirectAttributes.addFlashAttribute("success", "Comment added.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tickets/" + ticketId;
    }

    // ------------------------------------------------------------------
    // POST /tickets/{ticketId}/comments/{parentId}/reply  → nested reply
    // ------------------------------------------------------------------
    @PostMapping("/{parentId}/reply")
    public String addReply(
        @PathVariable Long ticketId,
        @PathVariable Long parentId,
        @RequestParam Long authorId,
        @RequestParam String body,
        RedirectAttributes redirectAttributes
    ) {
        try {
            commentService.addReply(ticketId, parentId, authorId, body);
            redirectAttributes.addFlashAttribute("success", "Reply added.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tickets/" + ticketId;
    }
}
