CREATE TABLE IF NOT EXISTS charging_locations (
    id VARCHAR(128) NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    street VARCHAR(255),
    city VARCHAR(255),
    postal_code VARCHAR(64),
    country_code VARCHAR(16),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS provider_stations (
    id BIGSERIAL PRIMARY KEY,
    provider_station_id VARCHAR(128) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    name VARCHAR(255) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    street VARCHAR(255),
    city VARCHAR(255),
    postal_code VARCHAR(64),
    country_code VARCHAR(16),
    raw_provider_type VARCHAR(255),
    raw_json_payload TEXT,
    charging_location_id VARCHAR(128) NOT NULL REFERENCES charging_locations(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS charger_units (
    id VARCHAR(128) NOT NULL PRIMARY KEY,
    charging_location_id VARCHAR(128) NOT NULL REFERENCES charging_locations(id) ON DELETE CASCADE,
    label VARCHAR(255),
    confidence VARCHAR(32) NOT NULL,
    sharing_status VARCHAR(32) NOT NULL,
    total_power_kw NUMERIC(8, 2),
    verified_by VARCHAR(255),
    notes TEXT
);

CREATE TABLE IF NOT EXISTS charger_unit_evse_ids (
    charger_unit_id VARCHAR(128) NOT NULL REFERENCES charger_units(id) ON DELETE CASCADE,
    evse_id VARCHAR(128) NOT NULL
);

CREATE TABLE IF NOT EXISTS connectors (
    id UUID NOT NULL PRIMARY KEY,
    provider_station_id BIGINT NOT NULL REFERENCES provider_stations(id) ON DELETE CASCADE,
    evse_id VARCHAR(128) NOT NULL,
    connector_type VARCHAR(32) NOT NULL,
    current_type VARCHAR(16) NOT NULL,
    max_power_kw NUMERIC(8, 2) NOT NULL,
    public_price_per_kwh NUMERIC(8, 4),
    power_sharing_status VARCHAR(32) NOT NULL,
    power_sharing_confidence VARCHAR(32) NOT NULL,
    advertised_power_kw NUMERIC(8, 2),
    total_stand_power_kw NUMERIC(8, 2),
    effective_available_power_kw NUMERIC(8, 2),
    active_sessions_on_stand INTEGER NOT NULL DEFAULT 0,
    last_known_status VARCHAR(32) NOT NULL,
    last_status_update TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_provider_stations_loc_id ON provider_stations(charging_location_id);
CREATE INDEX IF NOT EXISTS idx_provider_stations_provider_station_id ON provider_stations(provider_station_id);
CREATE INDEX IF NOT EXISTS idx_charger_units_loc_id ON charger_units(charging_location_id);
CREATE INDEX IF NOT EXISTS idx_charger_unit_evse_ids_unit_id ON charger_unit_evse_ids(charger_unit_id);
CREATE INDEX IF NOT EXISTS idx_connectors_station_id ON connectors(provider_station_id);
CREATE INDEX IF NOT EXISTS idx_connectors_evse_id ON connectors(evse_id);
CREATE INDEX IF NOT EXISTS idx_charging_locations_coords ON charging_locations(latitude, longitude);
