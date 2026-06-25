package com.helpdesk.web.controller;

import com.helpdesk.enums.Priority;
import com.helpdesk.model.Ticket;
import com.helpdesk.service.TicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public ResponseEntity<List<Ticket>> getAllTickets() {
        return ResponseEntity.ok(ticketService.getAllTickets());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ticket> getTicketById(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getTicketById(id));
    }

    @PostMapping
    public ResponseEntity<Ticket> createTicket(
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam Priority priority,
            @RequestParam Long reporterId) {
        
        Ticket ticket = ticketService.createTicket(title, description, priority, reporterId);
        return ResponseEntity.ok(ticket);
    }

    @PutMapping("/{id}/start")
    public ResponseEntity<Ticket> startProgress(
            @PathVariable Long id,
            @RequestParam Long assigneeId) {
        
        Ticket ticket = ticketService.startProgress(id, assigneeId);
        return ResponseEntity.ok(ticket);
    }

    @PutMapping("/{id}/close")
    public ResponseEntity<Ticket> closeTicket(
            @PathVariable Long id,
            @RequestParam Long closedByUserId) {
        
        Ticket ticket = ticketService.closeTicket(id, closedByUserId);
        return ResponseEntity.ok(ticket);
    }
    
    @PutMapping("/{id}/priority")
    public ResponseEntity<Ticket> updatePriority(
            @PathVariable Long id,
            @RequestParam Priority newPriority,
            @RequestParam Long updatedByUserId) {
            
        Ticket ticket = ticketService.updatePriority(id, newPriority, updatedByUserId);
        return ResponseEntity.ok(ticket);
    }
}