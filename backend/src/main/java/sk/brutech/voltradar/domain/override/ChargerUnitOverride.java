package sk.brutech.voltradar.domain.override;

import sk.brutech.voltradar.domain.model.ConfidenceLevel;
import sk.brutech.voltradar.domain.model.SharingStatus;

import java.math.BigDecimal;
import java.util.List;

/**
 * Community and verified charger unit override specification from GitHub Gist.
 */
public record ChargerUnitOverride(
        String unitId,
        String label,
        ConfidenceLevel confidence,
        SharingStatus sharingStatus,
        BigDecimal totalPowerKw,
        List<String> evseIds,
        String notes
) {
    public ChargerUnitOverride {
        evseIds = evseIds != null ? List.copyOf(evseIds) : List.of();
    }
}
