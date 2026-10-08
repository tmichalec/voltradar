package sk.brutech.voltradar.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import sk.brutech.voltradar.domain.model.ConfidenceLevel;
import sk.brutech.voltradar.domain.model.CurrentType;
import sk.brutech.voltradar.domain.model.LiveStatus;
import sk.brutech.voltradar.domain.model.NormalizedConnectorType;
import sk.brutech.voltradar.domain.model.SharingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "connectors")
public class ConnectorEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_station_id", nullable = false)
    private ProviderStationEntity providerStation;

    @Column(name = "evse_id", nullable = false, length = 128)
    private String evseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "connector_type", nullable = false, length = 32)
    private NormalizedConnectorType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_type", nullable = false, length = 16)
    private CurrentType currentType;

    @Column(name = "max_power_kw", precision = 8, scale = 2, nullable = false)
    private BigDecimal maxPowerKw;

    @Column(name = "public_price_per_kwh", precision = 8, scale = 4)
    private BigDecimal publicPricePerKwh;

    @Enumerated(EnumType.STRING)
    @Column(name = "power_sharing_status", nullable = false, length = 32)
    private SharingStatus powerSharingStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "power_sharing_confidence", nullable = false, length = 32)
    private ConfidenceLevel powerSharingConfidence;

    @Column(name = "advertised_power_kw", precision = 8, scale = 2)
    private BigDecimal advertisedPowerKw;

    @Column(name = "total_stand_power_kw", precision = 8, scale = 2)
    private BigDecimal totalStandPowerKw;

    @Column(name = "effective_available_power_kw", precision = 8, scale = 2)
    private BigDecimal effectiveAvailablePowerKw;

    @Column(name = "active_sessions_on_stand")
    private int activeSessionsOnStand;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_known_status", nullable = false, length = 32)
    private LiveStatus lastKnownStatus;

    @Column(name = "last_status_update")
    private Instant lastStatusUpdate;

    @Column(name = "free_parking_minutes")
    private Integer freeParkingMinutes;

    public ConnectorEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ProviderStationEntity getProviderStation() {
        return providerStation;
    }

    public void setProviderStation(ProviderStationEntity providerStation) {
        this.providerStation = providerStation;
    }

    public String getEvseId() {
        return evseId;
    }

    public void setEvseId(String evseId) {
        this.evseId = evseId;
    }

    public NormalizedConnectorType getType() {
        return type;
    }

    public void setType(NormalizedConnectorType type) {
        this.type = type;
    }

    public CurrentType getCurrentType() {
        return currentType;
    }

    public void setCurrentType(CurrentType currentType) {
        this.currentType = currentType;
    }

    public BigDecimal getMaxPowerKw() {
        return maxPowerKw;
    }

    public void setMaxPowerKw(BigDecimal maxPowerKw) {
        this.maxPowerKw = maxPowerKw;
    }

    public BigDecimal getPublicPricePerKwh() {
        return publicPricePerKwh;
    }

    public void setPublicPricePerKwh(BigDecimal publicPricePerKwh) {
        this.publicPricePerKwh = publicPricePerKwh;
    }

    public SharingStatus getPowerSharingStatus() {
        return powerSharingStatus;
    }

    public void setPowerSharingStatus(SharingStatus powerSharingStatus) {
        this.powerSharingStatus = powerSharingStatus;
    }

    public ConfidenceLevel getPowerSharingConfidence() {
        return powerSharingConfidence;
    }

    public void setPowerSharingConfidence(ConfidenceLevel powerSharingConfidence) {
        this.powerSharingConfidence = powerSharingConfidence;
    }

    public BigDecimal getAdvertisedPowerKw() {
        return advertisedPowerKw;
    }

    public void setAdvertisedPowerKw(BigDecimal advertisedPowerKw) {
        this.advertisedPowerKw = advertisedPowerKw;
    }

    public BigDecimal getTotalStandPowerKw() {
        return totalStandPowerKw;
    }

    public void setTotalStandPowerKw(BigDecimal totalStandPowerKw) {
        this.totalStandPowerKw = totalStandPowerKw;
    }

    public BigDecimal getEffectiveAvailablePowerKw() {
        return effectiveAvailablePowerKw;
    }

    public void setEffectiveAvailablePowerKw(BigDecimal effectiveAvailablePowerKw) {
        this.effectiveAvailablePowerKw = effectiveAvailablePowerKw;
    }

    public int getActiveSessionsOnStand() {
        return activeSessionsOnStand;
    }

    public void setActiveSessionsOnStand(int activeSessionsOnStand) {
        this.activeSessionsOnStand = activeSessionsOnStand;
    }

    public LiveStatus getLastKnownStatus() {
        return lastKnownStatus;
    }

    public void setLastKnownStatus(LiveStatus lastKnownStatus) {
        this.lastKnownStatus = lastKnownStatus;
    }

    public Instant getLastStatusUpdate() {
        return lastStatusUpdate;
    }

    public void setLastStatusUpdate(Instant lastStatusUpdate) {
        this.lastStatusUpdate = lastStatusUpdate;
    }

    public Integer getFreeParkingMinutes() {
        return freeParkingMinutes;
    }

    public void setFreeParkingMinutes(Integer freeParkingMinutes) {
        this.freeParkingMinutes = freeParkingMinutes;
    }
}
