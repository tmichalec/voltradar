import React, { useState, useMemo, useEffect } from 'react';
import type { ChargingLocation } from '../types/charging';
import {
  getStatusBadge,
  getSharingStatusLabel,
  getMaxLocationPower,
  getConnectorTypeStats,
  getGoogleMapsUrl,
  getWazeUrl,
  formatRelativeTime,
} from '../utils/formatters';
import {
  X,
  MapPin,
  RefreshCw,
  Clock,
  Navigation,
  ExternalLink,
  ShieldCheck,
  Share2,
  Copy,
  Check,
  ChevronDown,
  ChevronUp,
  Cpu,
  Layers,
  Radio,
  Server,
} from 'lucide-react';

interface LocationDrawerProps {
  location: ChargingLocation | null;
  onClose: () => void;
  onRefreshLocation?: (locationId: string) => void;
  isRefreshingLocation?: boolean;
}

export const LocationDrawer: React.FC<LocationDrawerProps> = ({
  location,
  onClose,
  onRefreshLocation,
  isRefreshingLocation = false,
}) => {
  const [copiedText, setCopiedText] = useState<string | null>(null);
  const [showRawDetails, setShowRawDetails] = useState(false);

  // Stable sorting of provider stations by name (natural numeric order) and ID, and connectors by EVSE
  const sortedStations = useMemo(() => {
    if (!location?.providerStations) return [];
    return [...location.providerStations]
      .map((station) => ({
        ...station,
        connectors: [...(station.connectors || [])].sort((a, b) =>
          (a.evseId || '').localeCompare(b.evseId || '', undefined, { numeric: true, sensitivity: 'base' })
        ),
      }))
      .sort(
        (a, b) =>
          (a.name || '').localeCompare(b.name || '', undefined, { numeric: true, sensitivity: 'base' }) ||
          (a.providerStationId || '').localeCompare(b.providerStationId || '', undefined, { numeric: true })
      );
  }, [location?.providerStations]);

  // Close on Escape key press
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [onClose]);

  if (!location) return null;

  const allConnectors = (location.providerStations || []).flatMap((s) => s.connectors || []);
  const connectorStats = getConnectorTypeStats(allConnectors);

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopiedText(text);
    setTimeout(() => setCopiedText(null), 2000);
  };

  const gmapsUrl = getGoogleMapsUrl(
    location.coordinates?.latitude,
    location.coordinates?.longitude,
    location.name
  );
  const wazeUrl = getWazeUrl(
    location.coordinates?.latitude,
    location.coordinates?.longitude
  );

  return (
    <div className="fixed top-14 right-0 bottom-0 w-full sm:w-[480px] md:w-[540px] bg-slate-900/95 backdrop-blur-xl border-l border-slate-800 shadow-2xl z-40 flex flex-col transition-all duration-300 ease-out text-slate-100 animate-in slide-in-from-right">
      {/* Header */}
      <div className="p-5 border-b border-slate-800 bg-gradient-to-b from-slate-900 to-slate-950/80">
        <div className="flex items-start justify-between gap-3">
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-1.5 flex-wrap">
              <span className="px-2 py-0.5 text-[10px] font-bold tracking-wider uppercase rounded-md bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
                ZSE Drive
              </span>
              {location.metadata?.confirmedChargerUnitsCount && location.metadata.confirmedChargerUnitsCount > 0 && (
                <span className="flex items-center gap-1 px-2 py-0.5 text-[10px] font-medium rounded-md bg-cyan-500/10 text-cyan-300 border border-cyan-500/30">
                  <ShieldCheck className="w-3 h-3 text-cyan-400" />
                  Overený power-sharing
                </span>
              )}
            </div>
            <h2 className="text-lg font-bold text-white tracking-tight leading-snug break-words">
              {location.name}
            </h2>
            <div className="flex items-center gap-1.5 text-xs text-slate-400 mt-1">
              <MapPin className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
              <span className="truncate">
                {location.address?.street
                  ? `${location.address.street}, ${location.address.city || ''}`
                  : `${location.coordinates?.latitude?.toFixed(4) ?? '0.0000'}, ${location.coordinates?.longitude?.toFixed(4) ?? '0.0000'}`}
              </span>
            </div>
          </div>

          <button
            onClick={onClose}
            aria-label="Zatvoriť"
            className="p-1.5 rounded-lg bg-slate-800/80 hover:bg-slate-700 text-slate-400 hover:text-white transition-colors cursor-pointer shrink-0"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Action Buttons (Navigation & Maps) */}
        <div className="grid grid-cols-2 gap-2 mt-4">
          <a
            href={gmapsUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center justify-center gap-1.5 px-3 py-2 bg-slate-800/80 hover:bg-slate-700 border border-slate-700/60 rounded-xl text-xs font-medium text-slate-200 hover:text-white transition-all shadow-sm"
          >
            <Navigation className="w-3.5 h-3.5 text-blue-400" />
            <span>Google Maps</span>
            <ExternalLink className="w-3 h-3 text-slate-500 ml-auto" />
          </a>
          <a
            href={wazeUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center justify-center gap-1.5 px-3 py-2 bg-slate-800/80 hover:bg-slate-700 border border-slate-700/60 rounded-xl text-xs font-medium text-slate-200 hover:text-white transition-all shadow-sm"
          >
            <Radio className="w-3.5 h-3.5 text-cyan-400" />
            <span>Waze navigácia</span>
            <ExternalLink className="w-3 h-3 text-slate-500 ml-auto" />
          </a>
        </div>

        {/* Sync Status and Single Refresh Button for Current Location */}
        <div className="mt-3 flex items-center justify-between gap-3 p-2.5 bg-slate-950/60 rounded-xl border border-slate-800/80">
          <div className="flex items-center gap-2 min-w-0 text-xs text-slate-400">
            <Clock className="w-3.5 h-3.5 text-slate-400 shrink-0" />
            <div className="truncate">
              <span className="text-slate-500 text-[10px] uppercase font-semibold block tracking-wider">Synchronizované</span>
              <span
                className="font-medium text-slate-200"
                title={location.updatedAt ? new Date(location.updatedAt).toLocaleString('sk-SK') : undefined}
              >
                {formatRelativeTime(location.updatedAt)}
              </span>
            </div>
          </div>

          {onRefreshLocation && (
            <button
              onClick={() => onRefreshLocation(location.id)}
              disabled={isRefreshingLocation}
              title="Aktualizovať dáta tejto lokality"
              className="flex items-center gap-1.5 px-3 py-1.5 bg-emerald-500/10 hover:bg-emerald-500/20 active:scale-95 border border-emerald-500/40 rounded-lg text-xs font-semibold text-emerald-300 hover:text-emerald-200 transition-all cursor-pointer shadow-sm disabled:opacity-50 shrink-0"
            >
              <RefreshCw className={`w-3.5 h-3.5 text-emerald-400 ${isRefreshingLocation ? 'animate-spin' : ''}`} />
              <span>{isRefreshingLocation ? 'Aktualizujem...' : 'Aktualizovať'}</span>
            </button>
          )}
        </div>

        {/* Key KPI Badges: Availability & Max Power separately for CCS and Type 2 */}
        <div className="mt-3 pt-3 border-t border-slate-800/80 space-y-2">
          <div className="flex items-center justify-between text-[11px] text-slate-400 font-medium">
            <span>Dostupnosť & Max. výkon podľa typu konektora:</span>
          </div>

          <div className={`grid gap-2 ${connectorStats.length > 1 ? 'grid-cols-2' : 'grid-cols-1'}`}>
            {connectorStats.map((stat) => {
              const isAvailable = stat.availableCount > 0;
              const isDc = stat.currentType === 'DC';
              const typeStands = (location.providerStations || []).filter((station) =>
                station.connectors?.some((c) => c.type === stat.type)
              ).length || 1;

              const standsLabel =
                typeStands === 1
                  ? '1 stojan'
                  : typeStands >= 2 && typeStands <= 4
                  ? `${typeStands} stojany`
                  : `${typeStands} stojanov`;

              return (
                <div
                  key={stat.type}
                  className={`rounded-xl p-2.5 border transition-all ${
                    isDc
                      ? 'bg-gradient-to-br from-cyan-950/40 to-slate-950/70 border-cyan-500/30'
                      : 'bg-gradient-to-br from-indigo-950/40 to-slate-950/70 border-indigo-500/30'
                  }`}
                >
                  <div className="flex items-center justify-between gap-1 mb-1.5">
                    <span className="text-xs font-bold text-white flex items-center gap-1.5">
                      <span className={`w-2 h-2 rounded-full ${isAvailable ? 'bg-emerald-400 shadow-[0_0_6px_#34d399]' : 'bg-amber-400'}`} />
                      {stat.shortLabel}
                    </span>
                    <div className="flex items-center gap-1.5">
                      <span className="text-[10px] font-mono px-1.5 py-0.5 rounded font-medium bg-purple-950/70 text-purple-300 border border-purple-800/40">
                        {standsLabel}
                      </span>
                      <span className={`text-[10px] font-mono px-1.5 py-0.5 rounded uppercase font-semibold ${isDc ? 'bg-cyan-950 text-cyan-300 border border-cyan-800/40' : 'bg-indigo-950 text-indigo-300 border border-indigo-800/40'}`}>
                        {stat.currentType}
                      </span>
                    </div>
                  </div>

                  <div className="flex items-baseline justify-between mt-1">
                    <div>
                      <div className="text-[10px] text-slate-400 uppercase">Max. výkon</div>
                      <div className="text-base font-bold text-cyan-300 font-mono">
                        {stat.maxPowerKw} <span className="text-xs font-normal text-slate-400">kW</span>
                      </div>
                    </div>
                    <div className="text-right">
                      <div className="text-[10px] text-slate-400 uppercase">Dostupnosť</div>
                      <div className={`text-base font-bold font-mono ${isAvailable ? 'text-emerald-400' : 'text-rose-400'}`}>
                        {stat.availableCount} <span className="text-xs font-normal text-slate-400">/ {stat.totalCount} voľné</span>
                      </div>
                    </div>
                  </div>

                  {stat.freeParkingMinutes != null && (
                    <div className="mt-2 pt-1.5 border-t border-slate-800/60 flex items-center justify-between text-[11px]">
                      <span className="text-slate-400 flex items-center gap-1">
                        <Clock className="w-3 h-3 text-emerald-400 shrink-0" />
                        Bezplatné parkovanie:
                      </span>
                      <span className="font-semibold text-emerald-300 font-mono">
                        {stat.freeParkingMinutes} min
                      </span>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {/* Scrollable Content */}
      <div className="flex-1 overflow-y-auto p-5 space-y-6">
        {/* Physical Charger Units (Power Sharing Stands - Overview if available) */}
        {location.chargerUnits && location.chargerUnits.length > 0 && (
          <div>
            <div className="flex items-center justify-between mb-2.5">
              <h3 className="text-xs font-semibold text-slate-300 uppercase tracking-wider flex items-center gap-1.5">
                <Cpu className="w-4 h-4 text-purple-400" />
                Fyzické stojany & Power Sharing
              </h3>
              <span className="text-[11px] text-purple-400/80 font-mono">
                {location.chargerUnits.length} {location.chargerUnits.length === 1 ? 'stojan' : 'stojany'}
              </span>
            </div>

            <div className="space-y-2.5">
              {location.chargerUnits.map((unit) => {
                const isShared = unit.sharingStatus === 'SHARED' || unit.sharingStatus === 'DYNAMIC_SHARED';
                return (
                  <div
                    key={unit.id}
                    className="bg-slate-950/60 border border-purple-500/20 rounded-xl p-3.5 relative overflow-hidden"
                  >
                    <div className="flex items-center justify-between mb-2">
                      <div className="flex items-center gap-2">
                        <span className="font-semibold text-sm text-purple-200">
                          {unit.label || unit.id}
                        </span>
                        {unit.confidence === 'CONFIRMED' && (
                          <span className="px-1.5 py-0.5 text-[9px] rounded bg-emerald-500/20 text-emerald-300 border border-emerald-500/40">
                            Potvrdené Gistom
                          </span>
                        )}
                      </div>
                      <span className="text-xs font-mono font-bold text-cyan-300 bg-cyan-950/60 px-2 py-0.5 rounded border border-cyan-800/40">
                        {unit.totalPowerKw} kW celkovo
                      </span>
                    </div>

                    <div className="text-xs text-slate-400 mb-2">
                      {isShared ? (
                        <div className="flex items-center gap-1.5 text-purple-300">
                          <Share2 className="w-3.5 h-3.5 text-purple-400 shrink-0" />
                          <span>Dynamické zdieľanie výkonu medzi pripojenými autami</span>
                        </div>
                      ) : (
                        <div className="flex items-center gap-1.5 text-slate-400">
                          <Layers className="w-3.5 h-3.5 text-slate-500 shrink-0" />
                          <span>Samostatný vyhradený stojan</span>
                        </div>
                      )}
                    </div>

                    {unit.evseIds && unit.evseIds.length > 0 && (
                      <div className="flex flex-wrap gap-1 mt-1 pt-2 border-t border-slate-800/60 text-[11px] font-mono text-slate-400">
                        <span className="text-slate-500 mr-1">EVSE:</span>
                        {unit.evseIds.map((evse) => (
                          <span
                            key={evse}
                            className="bg-slate-900 px-1.5 py-0.5 rounded border border-slate-800 text-slate-300"
                          >
                            {evse}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* Live Status Grouped by Charger Stand (Živý stav podľa stojanu) */}
        <div>
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-xs font-semibold text-slate-300 uppercase tracking-wider flex items-center gap-1.5">
              <Server className="w-4 h-4 text-emerald-400" />
              Živý stav konektorov podľa stojanu
            </h3>
            <span className="text-[11px] text-slate-400 font-mono">
              Spolu: {allConnectors.length} konektorov
            </span>
          </div>

          <div className="space-y-4">
            {sortedStations.map((station, stationIndex) => {
              const stationConnectors = station.connectors || [];
              const stationAvailable = stationConnectors.filter((c) => c.liveStatus === 'AVAILABLE').length;
              const stationMaxPower = getMaxLocationPower(stationConnectors);
              const stationStats = getConnectorTypeStats(stationConnectors);

              // Check if any charger unit matches this station
              const matchingUnit = location.chargerUnits?.find((u) =>
                u.evseIds?.some((evseId) => stationConnectors.some((c) => c.evseId === evseId))
              );

              return (
                <div
                  key={station.providerStationId || `station-${stationIndex}`}
                  className="bg-slate-950/70 border border-slate-800/90 rounded-2xl p-4 shadow-lg space-y-3"
                >
                  {/* Stand Header */}
                  <div className="flex items-start justify-between gap-2 border-b border-slate-800/80 pb-3">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="px-2 py-0.5 text-[10px] font-bold tracking-wider uppercase rounded bg-purple-500/10 text-purple-300 border border-purple-500/30">
                          Stojan #{stationIndex + 1}
                        </span>
                        <h4 className="text-sm font-bold text-white">
                          {station.name}
                        </h4>
                      </div>
                      <div className="text-xs text-slate-400 mt-1 flex items-center gap-2 flex-wrap">
                        <div className="flex items-center gap-1.5 font-mono">
                          <span className="text-slate-500 text-[11px]">ID stanice:</span>
                          <strong className="text-slate-300">{station.providerStationId}</strong>
                          <button
                            onClick={() => copyToClipboard(station.providerStationId)}
                            title="Kopírovať ID stanice"
                            className="p-1 hover:text-white transition-colors cursor-pointer text-slate-500 hover:text-slate-300"
                          >
                            {copiedText === station.providerStationId ? (
                              <Check className="w-3 h-3 text-emerald-400" />
                            ) : (
                              <Copy className="w-3 h-3" />
                            )}
                          </button>
                          <a
                            href={`https://zsedrive.sk/api/v4.7/stations/${station.providerStationId}`}
                            target="_blank"
                            rel="noopener noreferrer"
                            title="Otvoriť JSON stanice (ZSE API)"
                            className="p-1 hover:text-cyan-400 transition-colors cursor-pointer text-slate-500 hover:text-cyan-400 inline-flex items-center"
                          >
                            <ExternalLink className="w-3 h-3" />
                          </a>
                        </div>
                        {station.rawProviderType && (
                          <span className="text-slate-500">
                            &bull; <span className="text-slate-400">Typ:</span>{' '}
                            <strong className="text-slate-300">{station.rawProviderType}</strong>
                          </span>
                        )}
                      </div>
                    </div>

                    <div className="text-right shrink-0">
                      <span className="text-xs font-mono font-bold text-cyan-300 bg-cyan-950/60 px-2 py-0.5 rounded border border-cyan-800/40 block mb-1">
                        max {stationMaxPower} kW
                      </span>
                      <span className={`text-[11px] font-semibold ${stationAvailable > 0 ? 'text-emerald-400' : 'text-amber-400'}`}>
                        {stationAvailable}/{stationConnectors.length} voľné
                      </span>
                    </div>
                  </div>

                  {/* Power sharing note for stand if present */}
                  {matchingUnit && (
                    <div className="flex items-center gap-2 text-xs bg-purple-950/30 border border-purple-800/40 rounded-lg px-2.5 py-1.5 text-purple-200">
                      <Share2 className="w-3.5 h-3.5 text-purple-400 shrink-0" />
                      <span>{matchingUnit.label} &bull; Zdieľaný stojan (celkovo {matchingUnit.totalPowerKw} kW)</span>
                    </div>
                  )}

                  {/* Connectors on this stand grouped by connector type */}
                  <div className="space-y-3 pt-1">
                    {stationStats.map((stat) => {
                      const groupConnectors = stationConnectors.filter((c) => c.type === stat.type);
                      const isDc = stat.currentType === 'DC';
                      const isAvailable = stat.availableCount > 0;

                      return (
                        <div
                          key={stat.type}
                          className="bg-slate-900/70 rounded-xl p-3 border border-slate-800/90 space-y-2.5"
                        >
                          {/* Connector Type Group Header */}
                          <div className="flex items-center justify-between pb-2 border-b border-slate-800/80 gap-2 flex-wrap">
                            <div className="flex items-center gap-2 flex-wrap">
                              <span className={`w-2 h-2 rounded-full ${isAvailable ? 'bg-emerald-400 shadow-[0_0_6px_#34d399]' : 'bg-rose-400'}`} />
                              <span className={`text-xs font-bold ${isDc ? 'text-cyan-300' : 'text-indigo-300'}`}>
                                {stat.label}
                              </span>
                              <span
                                className={`text-[10px] font-mono px-1.5 py-0.2 rounded uppercase font-semibold ${
                                  isDc
                                    ? 'bg-cyan-950/80 text-cyan-400 border border-cyan-800/50'
                                    : 'bg-indigo-950/80 text-indigo-400 border border-indigo-800/50'
                                }`}
                              >
                                {stat.currentType}
                              </span>
                            </div>

                            <div className="flex items-center gap-2 text-xs font-mono">
                              <span className={`font-semibold ${isAvailable ? 'text-emerald-400' : 'text-rose-400'}`}>
                                {stat.availableCount}/{stat.totalCount} voľné
                              </span>
                              <span className="text-slate-600">&bull;</span>
                              <span className="font-bold text-amber-300 bg-amber-950/40 px-1.5 py-0.5 rounded border border-amber-800/40">
                                {stat.maxPowerKw} kW
                              </span>
                            </div>
                          </div>

                          {/* Individual Connectors in this type group */}
                          <div className="space-y-2">
                            {groupConnectors.map((connector) => {
                              const statusBadge = getStatusBadge(connector.liveStatus);
                              const sharingInfo = getSharingStatusLabel(connector.powerSharing);

                              return (
                                <div
                                  key={connector.id || connector.evseId}
                                  className={`bg-slate-950/80 border ${statusBadge.borderClass} rounded-lg p-2.5 transition-all hover:border-slate-600 shadow-sm`}
                                >
                                  {/* Status & EVSE Header */}
                                  <div className="flex items-center justify-between gap-2 mb-1.5">
                                    <div className="text-xs font-mono text-slate-300 flex items-center gap-1.5">
                                      <span className="text-slate-500 text-[11px]">EVSE:</span>
                                      <span className="font-semibold text-white">{connector.evseId}</span>
                                      <button
                                        onClick={() => copyToClipboard(connector.evseId)}
                                        title="Kopírovať EVSE ID"
                                        className="p-1 hover:text-white transition-colors cursor-pointer text-slate-500 hover:text-slate-300"
                                      >
                                        {copiedText === connector.evseId ? (
                                          <Check className="w-3 h-3 text-emerald-400" />
                                        ) : (
                                          <Copy className="w-3 h-3" />
                                        )}
                                      </button>
                                    </div>

                                    {/* Live Status Badge */}
                                    <div
                                      className={`flex items-center gap-1.5 px-2 py-0.5 rounded-full border ${statusBadge.bgClass} ${statusBadge.borderClass} ${statusBadge.textClass} text-[11px] font-semibold`}
                                    >
                                      <span className={`w-1.5 h-1.5 rounded-full ${statusBadge.dotClass}`} />
                                      <span>{statusBadge.label}</span>
                                    </div>
                                  </div>

                                  {/* Power & Pricing Info Grid */}
                                  <div className="grid grid-cols-2 gap-2 pt-1.5 border-t border-slate-900/80 text-xs">
                                    <div>
                                      <span className="text-slate-500 text-[10px] block uppercase tracking-wider">Výkon</span>
                                      <span className="font-bold text-cyan-300 text-xs font-mono">
                                        {connector.maxPowerKw} kW
                                      </span>
                                    </div>

                                    <div>
                                      <span className="text-slate-500 text-[10px] block uppercase tracking-wider">Verejná cena</span>
                                      <span className="font-bold text-emerald-400 text-xs font-mono">
                                        {connector.publicPricePerKwh != null && !isNaN(Number(connector.publicPricePerKwh))
                                          ? `${Number(connector.publicPricePerKwh).toFixed(2)} €/kWh`
                                          : 'Podľa cenníka'}
                                      </span>
                                    </div>
                                  </div>

                                  {/* Power sharing details */}
                                  {sharingInfo?.isShared && (
                                    <div className="mt-1.5 pt-1.5 border-t border-slate-900 flex items-center gap-1.5 text-[10px] text-purple-300 bg-purple-950/30 px-2 py-1 rounded border border-purple-800/30">
                                      <Share2 className="w-3 h-3 text-purple-400 shrink-0" />
                                      <span>{sharingInfo.description}</span>
                                    </div>
                                  )}
                                </div>
                              );
                            })}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Technical / Provider Metadata Collapsible */}
        <div className="border border-slate-800 rounded-xl overflow-hidden bg-slate-950/40">
          <button
            onClick={() => setShowRawDetails(!showRawDetails)}
            className="w-full px-4 py-3 flex items-center justify-between text-xs font-medium text-slate-400 hover:text-slate-200 hover:bg-slate-800/30 transition-colors cursor-pointer"
          >
            <span className="flex items-center gap-1.5">
              <Cpu className="w-3.5 h-3.5 text-slate-500" />
              Technické informácie poskytovateľa ({location.providerStations?.length || 0} staníc)
            </span>
            {showRawDetails ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
          </button>

          {showRawDetails && (
            <div className="p-4 border-t border-slate-800 space-y-3 text-xs bg-slate-950/80">
              {sortedStations.map((station) => (
                <div key={station.providerStationId} className="space-y-1 pb-3 border-b border-slate-800/60 last:border-0 last:pb-0">
                  <div className="flex justify-between items-center text-slate-300 font-medium">
                    <span>{station.name}</span>
                    <div className="flex items-center gap-1.5 font-mono text-[11px] text-slate-400">
                      <span>ID: {station.providerStationId}</span>
                      <button
                        onClick={() => copyToClipboard(station.providerStationId)}
                        title="Kopírovať ID stanice"
                        className="p-0.5 hover:text-white transition-colors cursor-pointer text-slate-500 hover:text-slate-300"
                      >
                        {copiedText === station.providerStationId ? (
                          <Check className="w-3 h-3 text-emerald-400" />
                        ) : (
                          <Copy className="w-3 h-3" />
                        )}
                      </button>
                      <a
                        href={`https://zsedrive.sk/api/v4.7/stations/${station.providerStationId}`}
                        target="_blank"
                        rel="noopener noreferrer"
                        title="Otvoriť JSON stanice (ZSE API)"
                        className="p-0.5 hover:text-cyan-400 transition-colors cursor-pointer text-slate-500 hover:text-cyan-400 inline-flex items-center"
                      >
                        <ExternalLink className="w-3 h-3" />
                      </a>
                    </div>
                  </div>
                  <div className="text-slate-500 text-[11px]">
                    Typ: <span className="text-slate-400">{station.rawProviderType || 'N/A'}</span> | GPS: {station.coordinates?.latitude ?? 'N/A'}, {station.coordinates?.longitude ?? 'N/A'}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
