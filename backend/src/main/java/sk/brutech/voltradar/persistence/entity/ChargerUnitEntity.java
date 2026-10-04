package sk.brutech.voltradar.persistence.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import sk.brutech.voltradar.domain.model.ConfidenceLevel;
import sk.brutech.voltradar.domain.model.SharingStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "charger_units")
public class ChargerUnitEntity {

    @Id
    @Column(name = "id", nullable = false, length = 128)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "charging_location_id", nullable = false)
    private ChargingLocationEntity chargingLocation;

    @Column(name = "label")
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence", nullable = false, length = 32)
    private ConfidenceLevel confidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "sharing_status", nullable = false, length = 32)
    private SharingStatus sharingStatus;

    @Column(name = "total_power_kw", precision = 8, scale = 2)
    private BigDecimal totalPowerKw;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "charger_unit_evse_ids", joinColumns = @JoinColumn(name = "charger_unit_id"))
    @Column(name = "evse_id", length = 128)
    private List<String> evseIds = new ArrayList<>();

    @Column(name = "verified_by")
    private String verifiedBy;

    @Column(name = "notes")
    private String notes;

    public ChargerUnitEntity() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ChargingLocationEntity getChargingLocation() {
        return chargingLocation;
    }

    public void setChargingLocation(ChargingLocationEntity chargingLocation) {
        this.chargingLocation = chargingLocation;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public ConfidenceLevel getConfidence() {
        return confidence;
    }

    public void setConfidence(ConfidenceLevel confidence) {
        this.confidence = confidence;
    }

    public SharingStatus getSharingStatus() {
        return sharingStatus;
    }

    public void setSharingStatus(SharingStatus sharingStatus) {
        this.sharingStatus = sharingStatus;
    }

    public BigDecimal getTotalPowerKw() {
        return totalPowerKw;
    }

    public void setTotalPowerKw(BigDecimal totalPowerKw) {
        this.totalPowerKw = totalPowerKw;
    }

    public List<String> getEvseIds() {
        return evseIds;
    }

    public void setEvseIds(List<String> evseIds) {
        this.evseIds = evseIds;
    }

    public String getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(String verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
