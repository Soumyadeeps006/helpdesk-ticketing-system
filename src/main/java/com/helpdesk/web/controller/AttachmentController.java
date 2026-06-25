package com.helpdesk.web.controller;

import com.helpdesk.model.Attachment;
import com.helpdesk.model.Ticket;
import com.helpdesk.service.LocalStorageService;
import com.helpdesk.service.TicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tickets/{ticketId}/attachments")
public class AttachmentController {

    private final LocalStorageService localStorageService;
    private final TicketService ticketService;

    public AttachmentController(LocalStorageService localStorageService, TicketService ticketService) {
        this.localStorageService = localStorageService;
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<Attachment> uploadAttachment(
            @PathVariable Long ticketId,
            @RequestParam("file") MultipartFile file) {
        
        // Fetch the ticket to ensure it exists before attaching a file
        Ticket ticket = ticketService.getTicketById(ticketId);
        
        // Store the file and save metadata to the database
        Attachment savedAttachment = localStorageService.store(file, ticket);
        
        return ResponseEntity.ok(savedAttachment);
    }
}