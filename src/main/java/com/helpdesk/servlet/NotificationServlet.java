package com.helpdesk.servlet;

import com.helpdesk.model.Ticket;
import com.helpdesk.service.TicketService;
import java.io.IOException;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * J2EE Servlet: NotificationServlet
 *
 * Triggered whenever a ticket status changes. Demonstrates how a plain
 * HttpServlet reaches Spring beans via WebApplicationContextUtils.
 *
 * Mapped to: /internal/notify
 *
 * Expected POST params:
 *   ticketId  — the ticket that changed
 *   newStatus — the new TicketStatus value (string)
 */
@WebServlet(name = "NotificationServlet", urlPatterns = "/internal/notify")
public class NotificationServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(
        NotificationServlet.class.getName()
    );

    // Lazily resolved from Spring context — do NOT @Autowired here (not a Spring bean)
    private TicketService ticketService;

    @Override
    public void init() throws ServletException {
        super.init();
        LOG.info("NotificationServlet initialised.");
    }

    @Override
    protected void doPost(
        HttpServletRequest request,
        HttpServletResponse response
    ) throws ServletException, IOException {
        // ----------------------------------------------------------------
        // 1. Resolve Spring context — safe to call on every request
        // ----------------------------------------------------------------
        WebApplicationContext ctx =
            WebApplicationContextUtils.getRequiredWebApplicationContext(
                getServletContext()
            );
        ticketService = ctx.getBean(TicketService.class);

        // ----------------------------------------------------------------
        // 2. Read parameters
        // ----------------------------------------------------------------
        String ticketIdParam = request.getParameter("ticketId");
        String newStatus = request.getParameter("newStatus");

        if (ticketIdParam == null || newStatus == null) {
            response.sendError(
                HttpServletResponse.SC_BAD_REQUEST,
                "Missing required params: ticketId, newStatus"
            );
            return;
        }

        long ticketId;
        try {
            ticketId = Long.parseLong(ticketIdParam);
        } catch (NumberFormatException e) {
            response.sendError(
                HttpServletResponse.SC_BAD_REQUEST,
                "ticketId must be numeric."
            );
            return;
        }

        // ----------------------------------------------------------------
        // 3. Fetch ticket details (read-only — no state change here)
        // ----------------------------------------------------------------
        Ticket ticket;
        try {
            ticket = ticketService.getTicketById(ticketId);
        } catch (IllegalArgumentException e) {
            response.sendError(
                HttpServletResponse.SC_NOT_FOUND,
                e.getMessage()
            );
            return;
        }

        // ----------------------------------------------------------------
        // 4. Send notification (stubbed — replace with email/Slack/etc.)
        // ----------------------------------------------------------------
        sendNotification(ticket, newStatus);

        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write("Notification sent for ticket #" + ticketId);
    }

    @Override
    protected void doGet(
        HttpServletRequest request,
        HttpServletResponse response
    ) throws ServletException, IOException {
        response.sendError(
            HttpServletResponse.SC_METHOD_NOT_ALLOWED,
            "Use POST with params: ticketId, newStatus"
        );
    }

    // ----------------------------------------------------------------
    // Notification stub — real implementation sends email / Slack
    // ----------------------------------------------------------------
    private void sendNotification(Ticket ticket, String newStatus) {
        // Ticket stores the reporter as createdBy (see Ticket.java)
        String recipient =
            ticket.getCreatedBy() != null
                ? ticket.getCreatedBy().getEmail()
                : "unknown";

        LOG.info(
            String.format(
                "[NOTIFY] Ticket #%d ('%s') moved to %s — notifying %s",
                ticket.getId(),
                ticket.getTitle(),
                newStatus,
                recipient
            )
        );

        // TODO: replace with JavaMailSender or REST call to Slack webhook
    }
}
