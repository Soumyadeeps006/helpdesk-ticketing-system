package com.helpdesk.enums;

/**
 * TicketStatus — drives the ticket lifecycle state machine.
 *
 * Valid transitions (enforced in TicketService):
 * <pre>
 *   OPEN → IN_PROGRESS → CLOSED
 *                     ↗
 *          OPEN ──────
 * </pre>
 * Re-opening a CLOSED ticket is intentionally disallowed to preserve
 * audit integrity; a new ticket should be raised instead.
 */
public enum TicketStatus {
    /** Ticket has been created and is awaiting assignment. */
    OPEN,

    /** An IT staff member has picked up the ticket. */
    IN_PROGRESS,

    /** The issue has been resolved and the ticket is closed. */
    CLOSED;

    /**
     * Returns {@code true} if transitioning to {@code next} is a legal move.
     *
     * @param next the target status
     * @return whether the transition is permitted
     */
    public boolean canTransitionTo(TicketStatus next) {
        if (this == OPEN) return next == IN_PROGRESS;
        if (this == IN_PROGRESS) return next == CLOSED;
        return false; // CLOSED is a terminal state
    }
}
