package sk.brutech.voltradar.persistence.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sk.brutech.voltradar.domain.model.ChargingLocation;
import sk.brutech.voltradar.persistence.entity.ChargerUnitEntity;
import sk.brutech.voltradar.persistence.entity.ChargingLocationEntity;
import sk.brutech.voltradar.persistence.entity.ConnectorEntity;
import sk.brutech.voltradar.persistence.entity.ProviderStationEntity;
import sk.brutech.voltradar.persistence.mapper.ChargingLocationEntityMapper;
import sk.brutech.voltradar.persistence.repository.ChargingLocationRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChargingLocationPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(ChargingLocationPersistenceService.class);

    private final ChargingLocationRepository locationRepository;

    public ChargingLocationPersistenceService(ChargingLocationRepository locationRepository) {
        this.locationRepository = Objects.requireNonNull(locationRepository, "locationRepository must not be null");
    }

    @Transactional
    public ChargingLocationEntity save(ChargingLocation location) {
        if (location == null) {
            return null;
        }

        ChargingLocationEntity incoming = ChargingLocationEntityMapper.toEntity(location);
        Optional<ChargingLocationEntity> existingOpt = locationRepository.findById(location.id());

        if (existingOpt.isPresent()) {
            ChargingLocationEntity existing = existingOpt.get();
            existing.setName(incoming.getName());
            existing.setLatitude(incoming.getLatitude());
            existing.setLongitude(incoming.getLongitude());
            existing.setStreet(incoming.getStreet());
            existing.setCity(incoming.getCity());
            existing.setPostalCode(incoming.getPostalCode());
            existing.setCountryCode(incoming.getCountryCode());
            existing.setUpdatedAt(Instant.now());

            updateProviderStations(existing, incoming.getProviderStations());
            updateChargerUnits(existing, incoming.getChargerUnits());

            return locationRepository.save(existing);
        } else {
            return locationRepository.save(incoming);
        }
    }

    private void updateProviderStations(ChargingLocationEntity existingLocation, List<ProviderStationEntity> incomingStations) {
        if (incomingStations == null || incomingStations.isEmpty()) {
            existingLocation.getProviderStations().clear();
            return;
        }

        Map<String, ProviderStationEntity> existingStationMap = existingLocation.getProviderStations().stream()
                .collect(Collectors.toMap(ProviderStationEntity::getProviderStationId, s -> s, (s1, s2) -> s1));

        Set<String> incomingStationIds = new HashSet<>();
        List<ProviderStationEntity> toAdd = new ArrayList<>();

        for (ProviderStationEntity incomingStation : incomingStations) {
            String psId = incomingStation.getProviderStationId();
            incomingStationIds.add(psId);

            ProviderStationEntity existingStation = existingStationMap.get(psId);
            if (existingStation != null) {
                existingStation.setProvider(incomingStation.getProvider());
                existingStation.setName(incomingStation.getName());
                existingStation.setLatitude(incomingStation.getLatitude());
                existingStation.setLongitude(incomingStation.getLongitude());
                existingStation.setStreet(incomingStation.getStreet());
                existingStation.setCity(incomingStation.getCity());
                existingStation.setPostalCode(incomingStation.getPostalCode());
                existingStation.setCountryCode(incomingStation.getCountryCode());
                existingStation.setRawProviderType(incomingStation.getRawProviderType());
                existingStation.setRawJsonPayload(incomingStation.getRawJsonPayload());

                updateConnectors(existingStation, incomingStation.getConnectors());
            } else {
                toAdd.add(incomingStation);
            }
        }

        existingLocation.getProviderStations().removeIf(s -> !incomingStationIds.contains(s.getProviderStationId()));

        for (ProviderStationEntity newStation : toAdd) {
            existingLocation.addProviderStation(newStation);
        }
    }

    private void updateConnectors(ProviderStationEntity existingStation, List<ConnectorEntity> incomingConnectors) {
        if (incomingConnectors == null || incomingConnectors.isEmpty()) {
            existingStation.getConnectors().clear();
            return;
        }

        Map<UUID, ConnectorEntity> existingConnectorMap = existingStation.getConnectors().stream()
                .collect(Collectors.toMap(ConnectorEntity::getId, c -> c, (c1, c2) -> c1));

        Set<UUID> incomingConnectorIds = new HashSet<>();
        List<ConnectorEntity> toAdd = new ArrayList<>();

        for (ConnectorEntity incomingConnector : incomingConnectors) {
            UUID id = incomingConnector.getId();
            incomingConnectorIds.add(id);

            ConnectorEntity existingConnector = existingConnectorMap.get(id);
            if (existingConnector != null) {
                existingConnector.setEvseId(incomingConnector.getEvseId());
                existingConnector.setType(incomingConnector.getType());
                existingConnector.setCurrentType(incomingConnector.getCurrentType());
                existingConnector.setMaxPowerKw(incomingConnector.getMaxPowerKw());
                existingConnector.setPublicPricePerKwh(incomingConnector.getPublicPricePerKwh());
                existingConnector.setPowerSharingStatus(incomingConnector.getPowerSharingStatus());
                existingConnector.setPowerSharingConfidence(incomingConnector.getPowerSharingConfidence());
                existingConnector.setAdvertisedPowerKw(incomingConnector.getAdvertisedPowerKw());
                existingConnector.setTotalStandPowerKw(incomingConnector.getTotalStandPowerKw());
                existingConnector.setEffectiveAvailablePowerKw(incomingConnector.getEffectiveAvailablePowerKw());
                existingConnector.setActiveSessionsOnStand(incomingConnector.getActiveSessionsOnStand());
                existingConnector.setLastKnownStatus(incomingConnector.getLastKnownStatus());
                existingConnector.setLastStatusUpdate(incomingConnector.getLastStatusUpdate());
            } else {
                toAdd.add(incomingConnector);
            }
        }

        existingStation.getConnectors().removeIf(c -> !incomingConnectorIds.contains(c.getId()));

        for (ConnectorEntity newConnector : toAdd) {
            existingStation.addConnector(newConnector);
        }
    }

    private void updateChargerUnits(ChargingLocationEntity existingLocation, List<ChargerUnitEntity> incomingUnits) {
        if (incomingUnits == null || incomingUnits.isEmpty()) {
            existingLocation.getChargerUnits().clear();
            return;
        }

        Map<String, ChargerUnitEntity> existingUnitMap = existingLocation.getChargerUnits().stream()
                .collect(Collectors.toMap(ChargerUnitEntity::getId, u -> u, (u1, u2) -> u1));

        Set<String> incomingUnitIds = new HashSet<>();
        List<ChargerUnitEntity> toAdd = new ArrayList<>();

        for (ChargerUnitEntity incomingUnit : incomingUnits) {
            String uId = incomingUnit.getId();
            incomingUnitIds.add(uId);

            ChargerUnitEntity existingUnit = existingUnitMap.get(uId);
            if (existingUnit != null) {
                existingUnit.setLabel(incomingUnit.getLabel());
                existingUnit.setConfidence(incomingUnit.getConfidence());
                existingUnit.setSharingStatus(incomingUnit.getSharingStatus());
                existingUnit.setTotalPowerKw(incomingUnit.getTotalPowerKw());
                existingUnit.setEvseIds(incomingUnit.getEvseIds() != null ? new ArrayList<>(incomingUnit.getEvseIds()) : new ArrayList<>());
                existingUnit.setVerifiedBy(incomingUnit.getVerifiedBy());
                existingUnit.setNotes(incomingUnit.getNotes());
            } else {
                toAdd.add(incomingUnit);
            }
        }

        existingLocation.getChargerUnits().removeIf(u -> !incomingUnitIds.contains(u.getId()));

        for (ChargerUnitEntity newUnit : toAdd) {
            existingLocation.addChargerUnit(newUnit);
        }
    }

    @Transactional
    public List<ChargingLocationEntity> saveAll(List<ChargingLocation> locations) {
        if (locations == null || locations.isEmpty()) {
            return List.of();
        }

        Map<String, ChargingLocation> uniqueLocations = new LinkedHashMap<>();
        for (ChargingLocation loc : locations) {
            if (loc != null && loc.id() != null) {
                uniqueLocations.put(loc.id(), loc);
            }
        }

        log.debug("Persisting {} charging locations to database", uniqueLocations.size());
        List<ChargingLocationEntity> saved = new ArrayList<>();
        for (ChargingLocation location : uniqueLocations.values()) {
            saved.add(save(location));
        }
        log.info("Successfully persisted {} charging locations to database", saved.size());
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<ChargingLocation> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return locationRepository.findById(id).map(ChargingLocationEntityMapper::toDomain);
    }

    @Transactional(readOnly = true)
    public List<ChargingLocation> findAll() {
        return locationRepository.findAllWithDetails().stream()
                .map(ChargingLocationEntityMapper::toDomain)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChargingLocation> findInBounds(double south, double north, double west, double east) {
        return locationRepository.findInBounds(south, north, west, east).stream()
                .map(ChargingLocationEntityMapper::toDomain)
                .toList();
    }
}
