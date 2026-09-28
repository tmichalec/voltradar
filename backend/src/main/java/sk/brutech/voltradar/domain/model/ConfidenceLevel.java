package sk.brutech.voltradar.domain.model;

/**
 * Trust and verification level of charging unit layout and power sharing topology.
 */
public enum ConfidenceLevel {
    /**
     * Confirmed by user observation or verified GitHub Gist community overrides.
     */
    CONFIRMED,

    /**
     * Inferred automatically via heuristics (e.g. EVSE ID patterns, physical distance clustering).
     */
    INFERRED,

    /**
     * Raw unverified state directly from provider API without topology deduction.
     */
    UNKNOWN
}
