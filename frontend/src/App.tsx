import React, { useState, useEffect, useMemo } from 'react';
import type { ChargingLocation, FilterState } from './types/charging';
import { api } from './services/api';
import { Navbar } from './components/Navbar';
import { FilterBar } from './components/FilterBar';
import { MapView } from './components/MapView';
import { LocationDrawer } from './components/LocationDrawer';
import {
  AlertCircle,
  Zap,
  CheckCircle2,
} from 'lucide-react';

export const App: React.FC = () => {
  const [locations, setLocations] = useState<ChargingLocation[]>([]);
  const [selectedLocation, setSelectedLocation] = useState<ChargingLocation | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isRefreshing, setIsRefreshing] = useState<boolean>(false);
  const [isRefreshingRetro, setIsRefreshingRetro] = useState<boolean>(false);
  const [isClearing, setIsClearing] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [notification, setNotification] = useState<string | null>(null);

  const [filters, setFilters] = useState<FilterState>({
    searchQuery: '',
    onlyAvailable: false,
    minPowerKw: 0,
    connectorTypes: [],
    onlyShared: false,
  });

  // Load locations on mount
  const loadLocations = async (forceFallback = false) => {
    setIsLoading(true);
    setError(null);
    try {
      let data = await api.getPersistedLocations();
      if ((!data || data.length === 0) && forceFallback) {
        // Ingest sample data if DB is initially unpopulated
        data = await api.getBratislavaLive();
      }
      setLocations(data || []);
    } catch (err: any) {
      console.error('Error loading locations:', err);
      setError(
        'Nepodarilo sa spojiť s backendovým serverom VoltRadar na porte 8080. Uistite sa, že backend beží.'
      );
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadLocations(true);
  }, []);

  // Trigger manual refresh (restricted to OC Retro)
  const handleRefresh = async () => {
    setIsRefreshing(true);
    try {
      const res = await api.triggerRetroRefresh();
      setNotification(`Lokalita OC Retro bola úspešne zaktualizovaná (${res.totalPersisted} lokalít)!`);
      const updated = await api.getPersistedLocations();
      setLocations(updated || []);
      const retroLoc = updated?.find(
        (l) =>
          l.name?.toLowerCase().includes('retro') ||
          l.providerStations?.some((s) => s.providerStationId === '79480' || s.providerStationId === '316067')
      );
      if (retroLoc) {
        setSelectedLocation(retroLoc);
      }
      setTimeout(() => setNotification(null), 4000);
    } catch (err: any) {
      console.error('Refresh error:', err);
      setNotification('Obnova lokality Retro zlyhala. Skontrolujte pripojenie k internetu a stav backendu.');
      setTimeout(() => setNotification(null), 5000);
    } finally {
      setIsRefreshing(false);
    }
  };

  // Trigger targeted refresh for Retro
  const handleRefreshRetro = async () => {
    setIsRefreshingRetro(true);
    try {
      const res = await api.triggerRetroRefresh();
      setNotification(`Lokalita OC Retro bola úspešne zaktualizovaná (${res.totalPersisted} lokalít)!`);
      const updated = await api.getPersistedLocations();
      setLocations(updated || []);
      // If user is currently looking at Retro or wants to see Retro, update selected location
      const retroLoc = updated?.find(
        (l) =>
          l.name?.toLowerCase().includes('retro') ||
          l.providerStations?.some((s) => s.providerStationId === '79480' || s.providerStationId === '316067')
      );
      if (retroLoc) {
        setSelectedLocation(retroLoc);
      }
      setTimeout(() => setNotification(null), 4000);
    } catch (err: any) {
      console.error('Retro refresh error:', err);
      setNotification('Obnova lokality Retro zlyhala. Skontrolujte pripojenie.');
      setTimeout(() => setNotification(null), 5000);
    } finally {
      setIsRefreshingRetro(false);
    }
  };

  // Clear all data from DB and cache
  const handleClearAll = async () => {
    if (!window.confirm('Naozaj chcete vymazať všetky uložené dáta z databázy aj cache?')) {
      return;
    }
    setIsClearing(true);
    try {
      await api.clearAllData();
      setLocations([]);
      setSelectedLocation(null);
      setNotification('Všetky dáta boli úspešne vymazané z databázy aj cache.');
      setTimeout(() => setNotification(null), 4000);
    } catch (err: any) {
      console.error('Clear error:', err);
      setNotification('Mazanie dát zlyhalo. Skontrolujte pripojenie k backendu.');
      setTimeout(() => setNotification(null), 5000);
    } finally {
      setIsClearing(false);
    }
  };

  // Filtered locations
  const filteredLocations = useMemo(() => {
    return locations.filter((loc) => {
      // 1. Text Search query
      if (filters.searchQuery.trim().length > 0) {
        const query = filters.searchQuery.toLowerCase().trim();
        const nameMatch = loc.name?.toLowerCase().includes(query);
        const streetMatch = loc.address?.street?.toLowerCase().includes(query);
        const cityMatch = loc.address?.city?.toLowerCase().includes(query);
        if (!nameMatch && !streetMatch && !cityMatch) {
          return false;
        }
      }

      const allConnectors = loc.providerStations?.flatMap((s) => s.connectors) || [];

      // 2. Only Available Filter
      if (filters.onlyAvailable) {
        const hasAvailable = allConnectors.some((c) => c.liveStatus === 'AVAILABLE');
        if (!hasAvailable) return false;
      }

      // 3. Min Power Filter
      if (filters.minPowerKw > 0) {
        const maxKw = Math.max(...allConnectors.map((c) => c.maxPowerKw || 0), 0);
        if (maxKw < filters.minPowerKw) return false;
      }

      // 4. Connector Types Filter
      if (filters.connectorTypes.length > 0) {
        const hasMatchingType = allConnectors.some((c) =>
          filters.connectorTypes.includes(c.type)
        );
        if (!hasMatchingType) return false;
      }

      // 5. Only Shared Power Filter
      if (filters.onlyShared) {
        const isShared = loc.chargerUnits?.some(
          (u) => u.sharingStatus === 'SHARED' || u.sharingStatus === 'DYNAMIC_SHARED'
        );
        if (!isShared) return false;
      }

      return true;
    });
  }, [locations, filters]);

  // Global KPIs
  const totalConnectors = useMemo(() => {
    return locations.reduce(
      (sum, loc) => sum + loc.providerStations.flatMap((s) => s.connectors).length,
      0
    );
  }, [locations]);

  const availableConnectors = useMemo(() => {
    return locations.reduce(
      (sum, loc) =>
        sum +
        loc.providerStations
          .flatMap((s) => s.connectors)
          .filter((c) => c.liveStatus === 'AVAILABLE').length,
      0
    );
  }, [locations]);

  return (
    <div className="flex flex-col h-screen w-screen bg-slate-950 text-slate-100 overflow-hidden font-sans select-none">
      {/* Top Navbar */}
      <Navbar
        totalLocations={locations.length}
        availableConnectors={availableConnectors}
        totalConnectors={totalConnectors}
        onRefresh={handleRefresh}
        isRefreshing={isRefreshing}
        onRefreshRetro={handleRefreshRetro}
        isRefreshingRetro={isRefreshingRetro}
        onClearAll={handleClearAll}
        isClearing={isClearing}
        searchQuery={filters.searchQuery}
        onSearchChange={(query) => setFilters({ ...filters, searchQuery: query })}
      />

      {/* Filter Bar */}
      <FilterBar
        filters={filters}
        onFilterChange={setFilters}
        filteredCount={filteredLocations.length}
        totalCount={locations.length}
      />

      {/* Main Map View Area */}
      <main className="flex-1 relative overflow-hidden">
        {/* Loading Spinner */}
        {isLoading && (
          <div className="absolute inset-0 z-30 bg-slate-950/80 backdrop-blur-sm flex flex-col items-center justify-center gap-3">
            <div className="relative">
              <div className="w-12 h-12 rounded-full border-2 border-emerald-500/20 border-t-emerald-400 animate-spin" />
              <Zap className="w-5 h-5 text-emerald-400 absolute inset-0 m-auto" />
            </div>
            <p className="text-sm font-medium text-slate-300">Načítavam nabíjacie stanice z PostgreSQL a Redis...</p>
          </div>
        )}

        {/* Error Banner */}
        {error && (
          <div className="absolute top-4 left-1/2 -translate-x-1/2 z-30 max-w-lg w-full px-4">
            <div className="bg-rose-950/90 border border-rose-500/50 backdrop-blur-md rounded-2xl p-4 shadow-2xl flex items-start gap-3 text-rose-200">
              <AlertCircle className="w-5 h-5 text-rose-400 shrink-0 mt-0.5" />
              <div className="flex-1 text-xs">
                <p className="font-semibold text-white mb-1">Chyba pripojenia k API</p>
                <p>{error}</p>
                <button
                  onClick={() => loadLocations(true)}
                  className="mt-2.5 px-3 py-1 bg-rose-600 hover:bg-rose-500 text-white rounded-lg font-medium transition-colors cursor-pointer"
                >
                  Skúsiť znova
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Notification Toast */}
        {notification && (
          <div className="absolute top-4 left-1/2 -translate-x-1/2 z-40 max-w-md w-full px-4 animate-in fade-in slide-in-from-top-2">
            <div className="bg-emerald-950/95 border border-emerald-500/60 backdrop-blur-md rounded-2xl px-4 py-3 shadow-2xl flex items-center gap-2.5 text-xs text-emerald-200">
              <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
              <span className="font-medium text-white">{notification}</span>
            </div>
          </div>
        )}

        {/* Interactive Map */}
        <MapView
          locations={filteredLocations}
          selectedLocation={selectedLocation}
          onSelectLocation={(loc) => setSelectedLocation(loc)}
        />

        {/* Selected Location Detail Drawer */}
        <LocationDrawer
          location={selectedLocation}
          onClose={() => setSelectedLocation(null)}
          onRefreshRetro={handleRefreshRetro}
          isRefreshingRetro={isRefreshingRetro}
        />
      </main>
    </div>
  );
};

export default App;
