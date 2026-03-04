package com.helpdesk.servlet;

import com.helpdesk.model.AuditLog;
import com.helpdesk.model.Ticket;
import com.helpdesk.model.User;
import java.io.IOException;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * J2EE Servlet: AuditServlet
 *
 * Provides a low-level audit-write endpoint kept outside the Spring MVC
 * DispatcherServlet, demonstrating how both servlet types coexist in web.xml.
 *
 * The servlet obtains Hibernate's SessionFactory directly from the Spring
 * WebApplicationContext using WebApplicationContextUtils.
 *
 * Mapped to: /internal/audit
 *
 * Expected POST params:
 *   ticketId  — ID of the ticket being audited
 *   actorId   — ID of the user who performed the action
 *   action    — short action label (e.g. "STATUS_CHANGE")
 *   newValue  — new value after the change (optional)
 *   oldValue  — previous value before the change (optional)
 */
@WebServlet(name = "AuditServlet", urlPatterns = "/internal/audit")
public class AuditServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(
        AuditServlet.class.getName()
    );

    @Override
    public void init() throws ServletException {
        super.init();
        LOG.info("AuditServlet initialised.");
    }

    @Override
    protected void doPost(
        HttpServletRequest request,
        HttpServletResponse response
    ) throws ServletException, IOException {
        // ----------------------------------------------------------------
        // 1. Resolve Spring WebApplicationContext to reach Hibernate
        // ----------------------------------------------------------------
        WebApplicationContext ctx =
            WebApplicationContextUtils.getRequiredWebApplicationContext(
                getServletContext()
            );
        SessionFactory sessionFactory = ctx.getBean(SessionFactory.class);

        // ----------------------------------------------------------------
        // 2. Parse & validate params
        // ----------------------------------------------------------------
        String ticketIdParam = request.getParameter("ticketId");
        String actorIdParam = request.getParameter("actorId");
        String action = request.getParameter("action");
        String oldValue = request.getParameter("oldValue"); // optional
        String newValue = request.getParameter("newValue"); // optional

        if (ticketIdParam == null || actorIdParam == null || action == null) {
            response.sendError(
                HttpServletResponse.SC_BAD_REQUEST,
                "Required params: ticketId, actorId, action"
            );
            return;
        }

        long ticketId, actorId;
        try {
            ticketId = Long.parseLong(ticketIdParam);
            actorId = Long.parseLong(actorIdParam);
        } catch (NumberFormatException e) {
            response.sendError(
                HttpServletResponse.SC_BAD_REQUEST,
                "ticketId and actorId must be numeric."
            );
            return;
        }

        // ----------------------------------------------------------------
        // 3. Persist AuditLog in its own transaction
        //    (Spring @Transactional does NOT wrap plain servlets)
        // ----------------------------------------------------------------
        Session session = sessionFactory.openSession();
        try {
            session.beginTransaction();

            // Load proxy references — avoids full entity fetch
            Ticket ticket = session.load(Ticket.class, ticketId);
            User actor = session.load(User.class, actorId);

            // AuditLog is immutable — no setters, use constructor directly
            AuditLog log = new AuditLog(
                ticket,
                actor,
                action,
                oldValue,
                newValue
            );

            session.save(log);
            session.getTransaction().commit();

            LOG.info(
                String.format(
                    "[AUDIT] %s on ticket #%d by user #%d",
                    action,
                    ticketId,
                    actorId
                )
            );

            response.setStatus(HttpServletResponse.SC_CREATED);
            response.getWriter().write("Audit log created.");
        } catch (Exception e) {
            if (session.getTransaction() != null) {
                session.getTransaction().rollback();
            }
            LOG.severe("AuditServlet failed: " + e.getMessage());
            response.sendError(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Audit write failed."
            );
        } finally {
            session.close();
        }
    }

    @Override
    protected void doGet(
        HttpServletRequest request,
        HttpServletResponse response
    ) throws ServletException, IOException {
        response.sendError(
            HttpServletResponse.SC_METHOD_NOT_ALLOWED,
            "Use POST with params: ticketId, actorId, action, oldValue, newValue"
        );
    }
}
