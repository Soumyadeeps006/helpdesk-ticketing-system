package com.helpdesk.controller;

import com.helpdesk.enums.Priority;
import com.helpdesk.model.Ticket;
import com.helpdesk.service.TicketService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles all /tickets/** routes.
 *
 * Rule: controllers are thin. ALL business logic stays in TicketService.
 * Never call ticket.setStatus() / ticket.assignTo() from here.
 */
@Controller
@RequestMapping("/tickets")
public class TicketController {

    private final TicketService ticketService;

    @Autowired
    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    // ------------------------------------------------------------------
    // GET /tickets  → list all tickets
    // ------------------------------------------------------------------
    @GetMapping
    public ModelAndView listTickets() {
        List<Ticket> tickets = ticketService.getAllTickets();
        ModelAndView mav = new ModelAndView("ticket-list");
        mav.addObject("tickets", tickets);
        return mav;
    }

    // ------------------------------------------------------------------
    // GET /tickets/{id}  → single ticket detail
    // ------------------------------------------------------------------
    @GetMapping("/{id}")
    public ModelAndView viewTicket(@PathVariable Long id, Model model) {
        Ticket ticket = ticketService.getTicketById(id);
        ModelAndView mav = new ModelAndView("ticket-detail");
        mav.addObject("ticket", ticket);
        return mav;
    }

    // ------------------------------------------------------------------
    // GET /tickets/new  → show creation form
    // ------------------------------------------------------------------
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("priorities", Priority.values());
        return "create-ticket";
    }

    // ------------------------------------------------------------------
    // POST /tickets  → create new ticket
    // ------------------------------------------------------------------
    @PostMapping
    public String createTicket(
        @RequestParam String title,
        @RequestParam String description,
        @RequestParam Priority priority,
        @RequestParam Long reporterId, // in real app: from session
        RedirectAttributes redirectAttributes
    ) {
        try {
            Ticket ticket = ticketService.createTicket(
                title,
                description,
                priority,
                reporterId
            );
            redirectAttributes.addFlashAttribute(
                "success",
                "Ticket #" + ticket.getId() + " created."
            );
            return "redirect:/tickets/" + ticket.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/tickets/new";
        }
    }

    // ------------------------------------------------------------------
    // PUT /tickets/{id}/status  → state transitions (start / close)
    // ------------------------------------------------------------------
    @PostMapping("/{id}/status") // HTML forms don't support PUT; use POST + _method override or a dedicated path
    public String updateStatus(
        @PathVariable Long id,
        @RequestParam String action, // "start" | "close"
        @RequestParam Long userId,
        RedirectAttributes redirectAttributes
    ) {
        try {
            switch (action) {
                case "start":
                    ticketService.startProgress(id, userId);
                    redirectAttributes.addFlashAttribute(
                        "success",
                        "Ticket moved to IN_PROGRESS."
                    );
                    break;
                case "close":
                    ticketService.closeTicket(id, userId);
                    redirectAttributes.addFlashAttribute(
                        "success",
                        "Ticket CLOSED."
                    );
                    break;
                default:
                    redirectAttributes.addFlashAttribute(
                        "error",
                        "Unknown action: " + action
                    );
            }
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tickets/" + id;
    }

    // ------------------------------------------------------------------
    // PUT /tickets/{id}/priority  → priority assignment (Task 3.6)
    // ------------------------------------------------------------------
    @PostMapping("/{id}/priority")
    public String updatePriority(
        @PathVariable Long id,
        @RequestParam Priority priority,
        @RequestParam Long userId,
        RedirectAttributes redirectAttributes
    ) {
        try {
            ticketService.updatePriority(id, priority, userId);
            redirectAttributes.addFlashAttribute(
                "success",
                "Priority updated to " + priority + "."
            );
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tickets/" + id;
    }
}
