import React, { useState } from 'react';
import type { ChargingLocation } from '../types/charging';
import {
  getStatusBadge,
  getConnectorTypeLabel,
  getSharingStatusLabel,
  getMaxLocationPower,
  getGoogleMapsUrl,
  getWazeUrl,
} from '../utils/formatters';
import {
  X,
  MapPin,
  Zap,
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
} from 'lucide-react';

interface LocationDrawerProps {
  location: ChargingLocation | null;
  onClose: () => void;
  onRefreshRetro?: () => void;
  isRefreshingRetro?: boolean;
}

export const LocationDrawer: React.FC<LocationDrawerProps> = ({
  location,
  onClose,
  onRefreshRetro,
  isRefreshingRetro = false,
}) => {
  const [copiedEvse, setCopiedEvse] = useState<string | null>(null);
  const [showRawDetails, setShowRawDetails] = useState(false);

  if (!location) return null;

  const allConnectors = location.providerStations.flatMap((s) => s.connectors);
  const maxPower = getMaxLocationPower(allConnectors);
  const availableCount = allConnectors.filter((c) => c.liveStatus === 'AVAILABLE').length;

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopiedEvse(text);
    setTimeout(() => setCopiedEvse(null), 2000);
  };

  const gmapsUrl = getGoogleMapsUrl(
    location.coordinates.latitude,
    location.coordinates.longitude,
    location.name
  );
  const wazeUrl = getWazeUrl(
    location.coordinates.latitude,
    location.coordinates.longitude
  );

  return (
    <div className="fixed top-14 right-0 bottom-0 w-full sm:w-[460px] md:w-[500px] bg-slate-900/95 backdrop-blur-xl border-l border-slate-800 shadow-2xl z-40 flex flex-col transition-all duration-300 ease-out text-slate-100 animate-in slide-in-from-right">
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

        {/* Quick Refresh Button for Retro */}
        {onRefreshRetro && (location.name?.toLowerCase().includes('retro') ||
          location.providerStations?.some((s) => s.providerStationId === '79480' || s.providerStationId === '316067')) && (
          <button
            onClick={onRefreshRetro}
            disabled={isRefreshingRetro}
            className="w-full mt-2.5 flex items-center justify-center gap-2 px-3 py-2 bg-emerald-500/10 hover:bg-emerald-500/20 active:scale-98 border border-emerald-500/40 rounded-xl text-xs font-semibold text-emerald-300 hover:text-emerald-200 transition-all cursor-pointer shadow-sm disabled:opacity-50"
          >
            <Zap className={`w-3.5 h-3.5 text-emerald-400 ${isRefreshingRetro ? 'animate-bounce' : ''}`} />
            <span>{isRefreshingRetro ? 'Aktualizujem Retro zo ZSE API...' : '⚡ Aktualizovať iba Retro (rýchly sync)'}</span>
          </button>
        )}

        {/* Key KPI Badges */}
        <div className="grid grid-cols-3 gap-2 mt-3 pt-3 border-t border-slate-800/80 text-center">
          <div className="bg-slate-950/60 border border-slate-800 rounded-xl p-2">
            <div className="text-[10px] text-slate-400 uppercase tracking-wider font-medium">Max. Výkon</div>
            <div className="text-base font-bold text-cyan-400">{maxPower} <span className="text-xs font-normal">kW</span></div>
          </div>
          <div className="bg-slate-950/60 border border-slate-800 rounded-xl p-2">
            <div className="text-[10px] text-slate-400 uppercase tracking-wider font-medium">Dostupnosť</div>
            <div className={`text-base font-bold ${availableCount > 0 ? 'text-emerald-400' : 'text-rose-400'}`}>
              {availableCount} <span className="text-xs font-normal text-slate-400">/ {allConnectors.length}</span>
            </div>
          </div>
          <div className="bg-slate-950/60 border border-slate-800 rounded-xl p-2">
            <div className="text-[10px] text-slate-400 uppercase tracking-wider font-medium">Stojany</div>
            <div className="text-base font-bold text-purple-400">
              {location.chargerUnits?.length || location.providerStations?.length || 1}
            </div>
          </div>
        </div>
      </div>

      {/* Scrollable Content */}
      <div className="flex-1 overflow-y-auto p-5 space-y-6">
        {/* Physical Charger Units (Power Sharing Stands) */}
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

        {/* Connectors Section */}
        <div>
          <div className="flex items-center justify-between mb-2.5">
            <h3 className="text-xs font-semibold text-slate-300 uppercase tracking-wider flex items-center gap-1.5">
              <Zap className="w-4 h-4 text-emerald-400" />
              Konektory & Živý stav
            </h3>
            <span className="text-[11px] text-slate-400 font-mono">
              Spolu: {allConnectors.length}
            </span>
          </div>

          <div className="space-y-3">
            {allConnectors.map((connector) => {
              const statusBadge = getStatusBadge(connector.liveStatus);
              const sharingInfo = getSharingStatusLabel(connector.powerSharing);

              return (
                <div
                  key={connector.id}
                  className={`bg-slate-950/80 border ${statusBadge.borderClass} rounded-xl p-4 transition-all hover:border-slate-600 shadow-md`}
                >
                  {/* Status & Type Header */}
                  <div className="flex items-start justify-between gap-2 mb-2.5">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="text-sm font-bold text-white">
                          {getConnectorTypeLabel(connector.type)}
                        </span>
                        <span className="px-1.5 py-0.5 text-[10px] font-mono uppercase rounded bg-slate-800 text-slate-300 border border-slate-700">
                          {connector.currentType}
                        </span>
                      </div>
                      <div className="text-xs font-mono text-slate-400 mt-0.5 flex items-center gap-1">
                        <span>EVSE: {connector.evseId}</span>
                        <button
                          onClick={() => copyToClipboard(connector.evseId)}
                          title="Kopírovať EVSE ID"
                          className="p-1 hover:text-white transition-colors cursor-pointer"
                        >
                          {copiedEvse === connector.evseId ? (
                            <Check className="w-3 h-3 text-emerald-400" />
                          ) : (
                            <Copy className="w-3 h-3 text-slate-500" />
                          )}
                        </button>
                      </div>
                    </div>

                    {/* Live Status Badge */}
                    <div
                      className={`flex items-center gap-1.5 px-2.5 py-1 rounded-full border ${statusBadge.bgClass} ${statusBadge.borderClass} ${statusBadge.textClass} text-xs font-semibold`}
                    >
                      <span className={`w-2 h-2 rounded-full ${statusBadge.dotClass}`} />
                      <span>{statusBadge.label}</span>
                    </div>
                  </div>

                  {/* Power & Tariff Info Grid */}
                  <div className="grid grid-cols-2 gap-2 pt-2 border-t border-slate-800/80 text-xs">
                    <div>
                      <span className="text-slate-500 text-[11px] block">Max. výkon konektora</span>
                      <span className="font-bold text-cyan-300 text-sm">
                        {connector.maxPowerKw} kW
                      </span>
                    </div>

                    <div>
                      <span className="text-slate-500 text-[11px] block">Verejná cena</span>
                      <span className="font-bold text-emerald-400 text-sm">
                        {connector.publicPricePerKwh ? `${connector.publicPricePerKwh.toFixed(2)} €/kWh` : 'Podľa cenníka'}
                      </span>
                    </div>
                  </div>

                  {/* Power sharing details */}
                  {sharingInfo.isShared && (
                    <div className="mt-2.5 pt-2 border-t border-slate-900 flex items-center gap-1.5 text-[11px] text-purple-300 bg-purple-950/30 px-2 py-1.5 rounded-lg border border-purple-800/30">
                      <Share2 className="w-3 h-3 text-purple-400 shrink-0" />
                      <span>{sharingInfo.description}</span>
                    </div>
                  )}
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
              Technické informácie poskytovateľa ({location.providerStations.length} staníc)
            </span>
            {showRawDetails ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
          </button>

          {showRawDetails && (
            <div className="p-4 border-t border-slate-800 space-y-3 text-xs bg-slate-950/80">
              {location.providerStations.map((station) => (
                <div key={station.providerStationId} className="space-y-1 pb-3 border-b border-slate-800/60 last:border-0 last:pb-0">
                  <div className="flex justify-between items-center text-slate-300 font-medium">
                    <span>{station.name}</span>
                    <span className="text-slate-500 font-mono text-[11px]">ID: {station.providerStationId}</span>
                  </div>
                  <div className="text-slate-500 text-[11px]">
                    Typ: <span className="text-slate-400">{station.rawProviderType || 'N/A'}</span> | GPS: {station.coordinates.latitude}, {station.coordinates.longitude}
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
