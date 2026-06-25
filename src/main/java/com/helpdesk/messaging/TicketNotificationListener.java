package com.helpdesk.messaging;

import com.helpdesk.config.RabbitConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class TicketNotificationListener {

    /**
     * Listens for ticket events published by TicketService.
     * In a real application, you would fetch the ticket details here 
     * and trigger an email, Slack message, or push notification.
     */
    @RabbitListener(queues = RabbitConfig.TICKET_EVENT_QUEUE)
    public void handleTicketEvent(Long ticketId) {
        System.out.println("=========================================");
        System.out.println("Received ticket event for Ticket ID: " + ticketId);
        System.out.println("Triggering background notification logic...");
        System.out.println("=========================================");
        
        // Example: emailService.sendTicketUpdateNotification(ticketId);
    }
}