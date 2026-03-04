package com.helpdesk.enums;

/**
 * Priority — indicates the urgency of a help-desk ticket.
 *
 * Priorities map to Bootstrap badge colours in the JSP layer:
 * <ul>
 *   <li>LOW      → badge-secondary (grey)</li>
 *   <li>MEDIUM   → badge-info (blue)</li>
 *   <li>HIGH     → badge-warning (orange)</li>
 *   <li>CRITICAL → badge-danger (red)</li>
 * </ul>
 */
public enum Priority {
    LOW(1, "badge-secondary"),
    MEDIUM(2, "badge-info"),
    HIGH(3, "badge-warning"),
    CRITICAL(4, "badge-danger");

    /** Numeric weight — higher = more urgent (useful for sorting). */
    private final int weight;

    /** Bootstrap CSS class for JSP badge rendering. */
    private final String badgeCss;

    Priority(int weight, String badgeCss) {
        this.weight = weight;
        this.badgeCss = badgeCss;
    }

    public int getWeight() {
        return weight;
    }

    public String getBadgeCss() {
        return badgeCss;
    }
}
