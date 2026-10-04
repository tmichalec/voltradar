package sk.brutech.voltradar.persistence.mapper;

import sk.brutech.voltradar.domain.model.Address;
import sk.brutech.voltradar.domain.model.ChargerUnit;
import sk.brutech.voltradar.domain.model.ChargingLocation;
import sk.brutech.voltradar.domain.model.Connector;
import sk.brutech.voltradar.domain.model.GeoCoordinates;
import sk.brutech.voltradar.domain.model.LiveStatus;
import sk.brutech.voltradar.domain.model.PowerSharingInfo;
import sk.brutech.voltradar.domain.model.ProviderStation;
import sk.brutech.voltradar.persistence.entity.ChargerUnitEntity;
import sk.brutech.voltradar.persistence.entity.ChargingLocationEntity;
import sk.brutech.voltradar.persistence.entity.ConnectorEntity;
import sk.brutech.voltradar.persistence.entity.ProviderStationEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class ChargingLocationEntityMapper {

    private ChargingLocationEntityMapper() {
    }

    public static ChargingLocationEntity toEntity(ChargingLocation domain) {
        if (domain == null) {
            return null;
        }

        ChargingLocationEntity entity = new ChargingLocationEntity();
        entity.setId(domain.id());
        entity.setName(domain.name());
        if (domain.coordinates() != null) {
            entity.setLatitude(domain.coordinates().latitude());
            entity.setLongitude(domain.coordinates().longitude());
        }
        if (domain.address() != null) {
            entity.setStreet(domain.address().street());
            entity.setCity(domain.address().city());
            entity.setPostalCode(domain.address().postalCode());
            entity.setCountryCode(domain.address().countryCode());
        }

        if (domain.providerStations() != null) {
            for (ProviderStation stationDomain : domain.providerStations()) {
                ProviderStationEntity stationEntity = toEntity(stationDomain);
                entity.addProviderStation(stationEntity);
            }
        }

        if (domain.chargerUnits() != null) {
            for (ChargerUnit unitDomain : domain.chargerUnits()) {
                ChargerUnitEntity unitEntity = toEntity(unitDomain);
                entity.addChargerUnit(unitEntity);
            }
        }

        return entity;
    }

    public static ProviderStationEntity toEntity(ProviderStation domain) {
        if (domain == null) {
            return null;
        }

        ProviderStationEntity entity = new ProviderStationEntity();
        entity.setProviderStationId(domain.providerStationId());
        entity.setProvider(domain.provider());
        entity.setName(domain.name());
        if (domain.coordinates() != null) {
            entity.setLatitude(domain.coordinates().latitude());
            entity.setLongitude(domain.coordinates().longitude());
        }
        if (domain.address() != null) {
            entity.setStreet(domain.address().street());
            entity.setCity(domain.address().city());
            entity.setPostalCode(domain.address().postalCode());
            entity.setCountryCode(domain.address().countryCode());
        }
        entity.setRawProviderType(domain.rawProviderType());
        entity.setRawJsonPayload(domain.rawJsonPayload());

        if (domain.connectors() != null) {
            for (Connector connectorDomain : domain.connectors()) {
                ConnectorEntity connectorEntity = toEntity(connectorDomain);
                entity.addConnector(connectorEntity);
            }
        }

        return entity;
    }

    public static ChargerUnitEntity toEntity(ChargerUnit domain) {
        if (domain == null) {
            return null;
        }

        ChargerUnitEntity entity = new ChargerUnitEntity();
        entity.setId(domain.id());
        entity.setLabel(domain.label());
        entity.setConfidence(domain.confidence());
        entity.setSharingStatus(domain.sharingStatus());
        entity.setTotalPowerKw(domain.totalPowerKw());
        if (domain.evseIds() != null) {
            entity.setEvseIds(new LinkedHashSet<>(domain.evseIds()));
        }
        entity.setVerifiedBy(domain.verifiedBy());
        entity.setNotes(domain.notes());

        return entity;
    }

    public static ConnectorEntity toEntity(Connector domain) {
        if (domain == null) {
            return null;
        }

        ConnectorEntity entity = new ConnectorEntity();
        entity.setId(domain.id());
        entity.setEvseId(domain.evseId());
        entity.setType(domain.type());
        entity.setCurrentType(domain.currentType());
        entity.setMaxPowerKw(domain.maxPowerKw());
        entity.setPublicPricePerKwh(domain.publicPricePerKwh());

        if (domain.powerSharing() != null) {
            entity.setPowerSharingStatus(domain.powerSharing().status());
            entity.setPowerSharingConfidence(domain.powerSharing().confidence());
            entity.setAdvertisedPowerKw(domain.powerSharing().advertisedPowerKw());
            entity.setTotalStandPowerKw(domain.powerSharing().totalStandPowerKw());
            entity.setEffectiveAvailablePowerKw(domain.powerSharing().effectiveAvailablePowerKw());
            entity.setActiveSessionsOnStand(domain.powerSharing().activeSessionsOnStand());
        }

        entity.setLastKnownStatus(domain.liveStatus() != null ? domain.liveStatus() : LiveStatus.UNKNOWN);
        entity.setLastStatusUpdate(domain.lastStatusUpdate() != null ? domain.lastStatusUpdate() : Instant.now());

        return entity;
    }

    public static ChargingLocation toDomain(ChargingLocationEntity entity) {
        if (entity == null) {
            return null;
        }

        GeoCoordinates coordinates = new GeoCoordinates(entity.getLatitude(), entity.getLongitude());
        Address address = new Address(
                entity.getStreet(),
                entity.getCity(),
                entity.getPostalCode(),
                entity.getCountryCode()
        );

        List<ProviderStation> providerStations = entity.getProviderStations() != null
                ? entity.getProviderStations().stream().map(ChargingLocationEntityMapper::toDomain).toList()
                : List.of();

        List<ChargerUnit> chargerUnits = entity.getChargerUnits() != null
                ? entity.getChargerUnits().stream().map(ChargingLocationEntityMapper::toDomain).toList()
                : List.of();

        return new ChargingLocation(
                entity.getId(),
                entity.getName(),
                coordinates,
                address,
                providerStations,
                chargerUnits,
                null
        );
    }

    public static ProviderStation toDomain(ProviderStationEntity entity) {
        if (entity == null) {
            return null;
        }

        GeoCoordinates coordinates = new GeoCoordinates(entity.getLatitude(), entity.getLongitude());
        Address address = new Address(
                entity.getStreet(),
                entity.getCity(),
                entity.getPostalCode(),
                entity.getCountryCode()
        );

        List<Connector> connectors = entity.getConnectors() != null
                ? entity.getConnectors().stream().map(ChargingLocationEntityMapper::toDomain).toList()
                : List.of();

        return new ProviderStation(
                entity.getProviderStationId(),
                entity.getProvider(),
                entity.getName(),
                coordinates,
                address,
                entity.getRawProviderType(),
                connectors,
                entity.getRawJsonPayload()
        );
    }

    public static ChargerUnit toDomain(ChargerUnitEntity entity) {
        if (entity == null) {
            return null;
        }

        List<String> evseIds = entity.getEvseIds() != null
                ? new ArrayList<>(entity.getEvseIds())
                : List.of();

        return new ChargerUnit(
                entity.getId(),
                entity.getLabel(),
                entity.getConfidence(),
                entity.getSharingStatus(),
                entity.getTotalPowerKw(),
                evseIds,
                entity.getVerifiedBy(),
                entity.getNotes()
        );
    }

    public static Connector toDomain(ConnectorEntity entity) {
        if (entity == null) {
            return null;
        }

        PowerSharingInfo powerSharing = new PowerSharingInfo(
                entity.getPowerSharingStatus(),
                entity.getPowerSharingConfidence(),
                entity.getAdvertisedPowerKw(),
                entity.getTotalStandPowerKw(),
                entity.getEffectiveAvailablePowerKw(),
                entity.getActiveSessionsOnStand()
        );

        return new Connector(
                entity.getId(),
                entity.getEvseId(),
                entity.getType(),
                entity.getCurrentType(),
                entity.getMaxPowerKw(),
                entity.getLastKnownStatus() != null ? entity.getLastKnownStatus() : LiveStatus.UNKNOWN,
                powerSharing,
                entity.getPublicPricePerKwh(),
                entity.getLastStatusUpdate()
        );
    }
}
