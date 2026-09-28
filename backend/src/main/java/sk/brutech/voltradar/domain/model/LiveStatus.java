package sk.brutech.voltradar.domain.model;

/**
 * Operational live status of a physical charging connector / EVSE.
 */
public enum LiveStatus {
    AVAILABLE,
    OCCUPIED,
    OUT_OF_ORDER,
    UNKNOWN
}
