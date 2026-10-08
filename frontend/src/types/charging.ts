export type LiveStatus = 'AVAILABLE' | 'OCCUPIED' | 'OUT_OF_ORDER' | 'RESERVED' | 'OFFLINE' | 'UNKNOWN';

export type ConnectorType = 'CCS' | 'CHAdeMO' | 'TYPE_2' | 'TYPE_1' | 'SCHUKO' | 'UNKNOWN';

export type CurrentType = 'AC' | 'DC' | 'UNKNOWN';

export type ConfidenceLevel = 'CONFIRMED' | 'INFERRED' | 'UNKNOWN';

export type SharingStatus = 'STANDALONE' | 'SHARED' | 'DYNAMIC_SHARED' | 'UNKNOWN';

export type CpoProvider = 'ZSE_DRIVE' | 'GREENWAY' | 'EON' | 'IONITY' | 'UNKNOWN';

export interface GeoCoordinates {
  latitude: number;
  longitude: number;
}

export interface Address {
  street?: string;
  city?: string;
  postalCode?: string;
  countryCode?: string;
}

export interface PowerSharingInfo {
  status: SharingStatus;
  confidence: ConfidenceLevel;
  advertisedPowerKw?: number;
  totalStandPowerKw?: number;
  effectiveAvailablePowerKw?: number;
  activeSessionsOnStand?: number;
}

export interface Connector {
  id: string;
  evseId: string;
  type: ConnectorType;
  currentType: CurrentType;
  maxPowerKw: number;
  liveStatus: LiveStatus;
  powerSharing: PowerSharingInfo;
  publicPricePerKwh?: number;
  lastStatusUpdate?: string;
  freeParkingMinutes?: number | null;
}

export interface ProviderStation {
  providerStationId: string;
  provider: CpoProvider;
  name: string;
  coordinates: GeoCoordinates;
  address: Address;
  rawProviderType?: string;
  connectors: Connector[];
  rawJsonPayload?: string;
}

export interface ChargerUnit {
  id: string;
  label: string;
  confidence: ConfidenceLevel;
  sharingStatus: SharingStatus;
  totalPowerKw: number;
  evseIds: string[];
  verifiedBy?: string;
  notes?: string;
}

export interface LocationMetadata {
  providerStationsCount: number;
  confirmedChargerUnitsCount?: number | null;
  totalConnectorsCount: number;
  availableConnectorsCount: number;
  ccsConnectorsCount: number;
  availableCcsConnectorsCount?: number;
  type2ConnectorsCount: number;
  availableType2ConnectorsCount?: number;
  maxPowerKw?: number;
  maxCcsPowerKw?: number;
  maxType2PowerKw?: number;
}

export interface ChargingLocation {
  id: string;
  name: string;
  coordinates: GeoCoordinates;
  address: Address;
  providerStations: ProviderStation[];
  chargerUnits: ChargerUnit[];
  metadata?: LocationMetadata;
  updatedAt?: string;
}

export interface FilterState {
  searchQuery: string;
  onlyAvailable: boolean;
  minPowerKw: number;
  connectorTypes: ConnectorType[];
  onlyShared: boolean;
}
