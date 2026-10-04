package sk.brutech.voltradar.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import sk.brutech.voltradar.domain.model.CpoProvider;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "provider_stations")
public class ProviderStationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_station_id", nullable = false, length = 128)
    private String providerStationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 32)
    private CpoProvider provider;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "longitude", nullable = false)
    private double longitude;

    @Column(name = "street")
    private String street;

    @Column(name = "city")
    private String city;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "country_code")
    private String countryCode;

    @Column(name = "raw_provider_type")
    private String rawProviderType;

    @Column(name = "raw_json_payload", columnDefinition = "TEXT")
    private String rawJsonPayload;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "charging_location_id", nullable = false)
    private ChargingLocationEntity chargingLocation;

    @OneToMany(mappedBy = "providerStation", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ConnectorEntity> connectors = new LinkedHashSet<>();

    public ProviderStationEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProviderStationId() {
        return providerStationId;
    }

    public void setProviderStationId(String providerStationId) {
        this.providerStationId = providerStationId;
    }

    public CpoProvider getProvider() {
        return provider;
    }

    public void setProvider(CpoProvider provider) {
        this.provider = provider;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getRawProviderType() {
        return rawProviderType;
    }

    public void setRawProviderType(String rawProviderType) {
        this.rawProviderType = rawProviderType;
    }

    public String getRawJsonPayload() {
        return rawJsonPayload;
    }

    public void setRawJsonPayload(String rawJsonPayload) {
        this.rawJsonPayload = rawJsonPayload;
    }

    public ChargingLocationEntity getChargingLocation() {
        return chargingLocation;
    }

    public void setChargingLocation(ChargingLocationEntity chargingLocation) {
        this.chargingLocation = chargingLocation;
    }

    public Set<ConnectorEntity> getConnectors() {
        return connectors;
    }

    public void setConnectors(Set<ConnectorEntity> connectors) {
        this.connectors = connectors;
    }

    public void addConnector(ConnectorEntity connector) {
        connectors.add(connector);
        connector.setProviderStation(this);
    }

    public void removeConnector(ConnectorEntity connector) {
        connectors.remove(connector);
        connector.setProviderStation(null);
    }
}
