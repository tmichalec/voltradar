package sk.brutech.voltradar.domain.model;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Representation of a single station entry directly received from a CPO provider API.
 */
public record ProviderStation(
        String providerStationId,
        CpoProvider provider,
        String name,
        GeoCoordinates coordinates,
        Address address,
        String rawProviderType,
        List<Connector> connectors,
        String rawJsonPayload
) {
    public ProviderStation {
        Objects.requireNonNull(providerStationId, "providerStationId must not be null");
        Objects.requireNonNull(provider, "provider must not be null");
        Objects.requireNonNull(name, "name must not be null");
        connectors = connectors != null
                ? connectors.stream()
                        .sorted(Comparator.comparing(
                                Connector::evseId,
                                Comparator.nullsLast((a, b) -> {
                                    try {
                                        return Long.compare(Long.parseLong(a), Long.parseLong(b));
                                    } catch (NumberFormatException e) {
                                        return String.CASE_INSENSITIVE_ORDER.compare(a, b);
                                    }
                                }))
                                .thenComparing(c -> c.id() != null ? c.id().toString() : ""))
                        .toList()
                : List.of();
    }
}
