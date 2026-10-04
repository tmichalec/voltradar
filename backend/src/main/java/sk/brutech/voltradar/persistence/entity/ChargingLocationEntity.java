package sk.brutech.voltradar.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "charging_locations")
public class ChargingLocationEntity {

    @Id
    @Column(name = "id", nullable = false, length = 128)
    private String id;

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

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "chargingLocation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProviderStationEntity> providerStations = new ArrayList<>();

    @OneToMany(mappedBy = "chargingLocation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChargerUnitEntity> chargerUnits = new ArrayList<>();

    public ChargingLocationEntity() {
    }

    public ChargingLocationEntity(String id, String name, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<ProviderStationEntity> getProviderStations() {
        return providerStations;
    }

    public void setProviderStations(List<ProviderStationEntity> providerStations) {
        this.providerStations = providerStations;
    }

    public void addProviderStation(ProviderStationEntity station) {
        providerStations.add(station);
        station.setChargingLocation(this);
    }

    public void removeProviderStation(ProviderStationEntity station) {
        providerStations.remove(station);
        station.setChargingLocation(null);
    }

    public List<ChargerUnitEntity> getChargerUnits() {
        return chargerUnits;
    }

    public void setChargerUnits(List<ChargerUnitEntity> chargerUnits) {
        this.chargerUnits = chargerUnits;
    }

    public void addChargerUnit(ChargerUnitEntity unit) {
        chargerUnits.add(unit);
        unit.setChargingLocation(this);
    }

    public void removeChargerUnit(ChargerUnitEntity unit) {
        chargerUnits.remove(unit);
        unit.setChargingLocation(null);
    }
}
