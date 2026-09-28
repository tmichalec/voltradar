package sk.brutech.voltradar.domain.model;

/**
 * Indicates whether a charging unit / connector shares available electrical power with other connectors.
 */
public enum SharingStatus {
    /**
     * Power is dynamically or statically shared across multiple connectors on the same unit/power module.
     */
    SHARED,

    /**
     * Dedicated, independent power delivery capacity guaranteed for this connector.
     */
    INDEPENDENT,

    /**
     * Power sharing state cannot be determined from provider API or community overrides.
     */
    UNKNOWN
}
