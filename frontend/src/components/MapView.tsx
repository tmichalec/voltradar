import React, { useEffect, useRef } from 'react';
import L from 'leaflet';
import type { ChargingLocation } from '../types/charging';
import { getMaxLocationPower, getConnectorTypeStats } from '../utils/formatters';
import { Locate, Plus, Minus } from 'lucide-react';

interface MapViewProps {
  locations: ChargingLocation[];
  selectedLocation: ChargingLocation | null;
  onSelectLocation: (location: ChargingLocation) => void;
}

export const MapView: React.FC<MapViewProps> = ({
  locations,
  selectedLocation,
  onSelectLocation,
}) => {
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapInstanceRef = useRef<L.Map | null>(null);
  const markersLayerRef = useRef<L.LayerGroup | null>(null);
  const userMarkerRef = useRef<L.Marker | null>(null);

  // Initialize Map
  useEffect(() => {
    if (!mapContainerRef.current || mapInstanceRef.current) return;

    // Default center in Bratislava / Slovakia
    const map = L.map(mapContainerRef.current, {
      center: [48.1486, 17.1077],
      zoom: 12,
      zoomControl: false,
    });

    // OpenStreetMap high-reliability open tiles without API keys
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution:
        '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener noreferrer">OpenStreetMap</a> contributors',
      maxZoom: 19,
    }).addTo(map);

    const markersGroup = L.layerGroup().addTo(map);
    markersLayerRef.current = markersGroup;
    mapInstanceRef.current = map;

    // Handle initial and resize layout invalidation
    const timer = setTimeout(() => {
      map.invalidateSize();
    }, 100);

    const handleResize = () => {
      map.invalidateSize();
    };
    window.addEventListener('resize', handleResize);

    return () => {
      clearTimeout(timer);
      window.removeEventListener('resize', handleResize);
      map.remove();
      mapInstanceRef.current = null;
    };
  }, []);

  // Update Markers when locations change or selected location changes
  useEffect(() => {
    const map = mapInstanceRef.current;
    const markersGroup = markersLayerRef.current;
    if (!map || !markersGroup) return;

    markersGroup.clearLayers();

    locations.forEach((location) => {
      if (
        !location ||
        !location.coordinates ||
        typeof location.coordinates.latitude !== 'number' ||
        typeof location.coordinates.longitude !== 'number'
      ) {
        return;
      }

      const allConnectors = (location.providerStations || []).flatMap((s) => s.connectors || []);
      const maxPower = getMaxLocationPower(allConnectors);
      const availableCount = allConnectors.filter((c) => c.liveStatus === 'AVAILABLE').length;
      const totalCount = allConnectors.length;
      const isSelected = selectedLocation?.id === location.id;

      // Color logic based on status and power
      let bgGradient = 'from-emerald-600 to-teal-700';
      let shadowColor = 'rgba(16, 185, 129, 0.4)';
      let statusDot = 'bg-emerald-400';

      if (availableCount === 0 && totalCount > 0) {
        bgGradient = 'from-amber-600 to-orange-700';
        shadowColor = 'rgba(245, 158, 11, 0.4)';
        statusDot = 'bg-amber-400';
      } else if (totalCount === 0) {
        bgGradient = 'from-slate-700 to-slate-800';
        shadowColor = 'rgba(100, 116, 139, 0.3)';
        statusDot = 'bg-slate-400';
      }

      if (maxPower >= 150) {
        bgGradient = availableCount > 0 ? 'from-cyan-600 to-blue-700' : 'from-amber-600 to-orange-700';
        shadowColor = availableCount > 0 ? 'rgba(6, 182, 212, 0.5)' : shadowColor;
      }

      const selectedClass = isSelected
        ? 'ring-4 ring-white ring-offset-2 ring-offset-slate-900 scale-125 z-50 animate-bounce'
        : 'hover:scale-110';

      const connectorStats = getConnectorTypeStats(allConnectors);

      const plugBadgesHtml = connectorStats
        .map((s) => `<span class="text-[9px] font-medium text-white/90 leading-tight tracking-tight whitespace-nowrap">${s.shortLabel}: ${s.availableCount}/${s.totalCount}</span>`)
        .join('');

      const customHtml = `
        <div class="custom-pin relative flex flex-col items-center cursor-pointer transition-transform duration-200 ${selectedClass}">
          <div class="flex flex-col items-center px-2.5 py-1 rounded-2xl bg-gradient-to-r ${bgGradient} text-white font-bold text-[11px] shadow-lg border border-white/20"
               style="box-shadow: 0 4px 14px ${shadowColor}; min-width: 60px;">
            <div class="flex items-center gap-1">
              <span class="w-2 h-2 rounded-full ${statusDot} shadow-sm shrink-0"></span>
              <span>${maxPower > 0 ? `${maxPower} kW` : 'EV'}</span>
            </div>
            ${plugBadgesHtml ? `<div class="flex flex-col items-center mt-0.5 space-y-0.5">${plugBadgesHtml}</div>` : ''}
          </div>
          <div class="w-2 h-2 bg-slate-900 rotate-45 -mt-1 border-r border-b border-white/20"></div>
        </div>
      `;

      const icon = L.divIcon({
        className: 'custom-leaflet-marker',
        html: customHtml,
        iconSize: [80, 52],
        iconAnchor: [40, 52],
      });

      const marker = L.marker([location.coordinates.latitude, location.coordinates.longitude], {
        icon,
      });

      const typeSummary = connectorStats
        .map((s) => {
          const isDc = s.currentType === 'DC';
          const typeColor = isDc ? 'text-cyan-300' : 'text-indigo-300';
          const availColor = s.availableCount > 0 ? '#34d399' : '#f87171';
          return `<div class="flex items-center justify-between gap-3 text-[11px] py-0.5">
            <span class="font-medium ${typeColor}">${s.label} (${s.currentType}):</span>
            <span class="font-mono"><strong style="color: ${availColor}">${s.availableCount}/${s.totalCount} voľné</strong> &bull; <span class="text-amber-300 font-bold">${s.maxPowerKw} kW</span></span>
          </div>`;
        })
        .join('');

      const sortedStations = [...(location.providerStations || [])].sort(
        (a, b) =>
          (a.name || '').localeCompare(b.name || '', undefined, { numeric: true, sensitivity: 'base' }) ||
          (a.providerStationId || '').localeCompare(b.providerStationId || '', undefined, { numeric: true })
      );

      const standsSummary = sortedStations
        .map((st, idx) => {
          const stConnectors = st.connectors || [];
          const stStats = getConnectorTypeStats(stConnectors);
          const stTypeDetails = stStats
            .map((s) => {
              const isDc = s.currentType === 'DC';
              const typeColor = isDc ? 'text-cyan-300' : 'text-indigo-300';
              const badgeBg = isDc ? 'bg-cyan-950/40 border-cyan-800/40' : 'bg-indigo-950/40 border-indigo-800/40';
              const availColor = s.availableCount > 0 ? 'text-emerald-400' : 'text-rose-400';

              return `<div class="flex items-center justify-between gap-2 px-2 py-1 rounded text-[11px] ${badgeBg} border font-mono">
                <span class="font-bold ${typeColor}">${s.shortLabel} (${s.currentType}):</span>
                <span class="flex items-center gap-1.5">
                  <strong class="${availColor}">${s.availableCount}/${s.totalCount} voľné</strong>
                  <span class="text-slate-500">&bull;</span>
                  <span class="text-amber-300 font-bold">${s.maxPowerKw} kW</span>
                </span>
              </div>`;
            })
            .join('');

          return `<div class="bg-slate-900/90 rounded-lg p-2 border border-slate-700/60 space-y-1.5 shadow-sm">
            <div class="flex items-center justify-between gap-1.5 pb-1 border-b border-slate-800">
              <span class="px-1.5 py-0.5 text-[10px] font-bold tracking-wider uppercase rounded bg-purple-950/80 text-purple-300 border border-purple-700/50">
                Stojan #${idx + 1}
              </span>
              <span class="text-[10px] text-slate-400 font-normal truncate max-w-[140px]" title="${st.name}">${st.name}</span>
            </div>
            <div class="space-y-1">
              ${stTypeDetails || '<div class="text-[10px] text-slate-500 italic">Žiadne konektory</div>'}
            </div>
          </div>`;
        })
        .join('');

      marker.bindTooltip(
        `<div class="p-2.5 min-w-[260px] max-w-[320px] font-sans space-y-2">
          <div>
            <div class="font-bold text-white text-[13px] leading-snug">${location.name}</div>
            ${location.address?.street ? `<div class="text-[11px] text-slate-400 mt-0.5">${location.address.street}${location.address.city ? `, ${location.address.city}` : ''}</div>` : ''}
          </div>

          <div class="space-y-1 border-y border-slate-700/60 py-1.5">
            <div class="text-[10px] uppercase tracking-wider text-slate-400 font-semibold mb-0.5">Celková dostupnosť lokality:</div>
            ${typeSummary}
          </div>

          ${location.providerStations && location.providerStations.length > 0 ? `
            <div class="space-y-1.5 pt-0.5">
              <div class="text-[10px] uppercase tracking-wider text-purple-300 font-semibold flex items-center justify-between">
                <span>Rozpis podľa stojanov (${location.providerStations.length}):</span>
              </div>
              <div class="space-y-1.5">
                ${standsSummary}
              </div>
            </div>
          ` : ''}
        </div>`,
        { direction: 'top', offset: [0, -32], opacity: 0.98 }
      );

      marker.on('click', () => {
        onSelectLocation(location);
      });

      markersGroup.addLayer(marker);
    });

    // If locations exist and not selected, fit bounds or keep view
    if (locations.length > 0 && !selectedLocation) {
      const validPoints = locations
        .filter(
          (loc) =>
            loc?.coordinates &&
            typeof loc.coordinates.latitude === 'number' &&
            typeof loc.coordinates.longitude === 'number'
        )
        .map((loc) => [loc.coordinates.latitude, loc.coordinates.longitude] as [number, number]);

      if (validPoints.length > 0) {
        const bounds = L.latLngBounds(validPoints);
        if (bounds.isValid()) {
          map.fitBounds(bounds, { padding: [50, 50], maxZoom: 15 });
        }
      }
    }
  }, [locations, selectedLocation, onSelectLocation]);

  // Center on selected location
  useEffect(() => {
    const map = mapInstanceRef.current;
    if (
      !map ||
      !selectedLocation?.coordinates ||
      typeof selectedLocation.coordinates.latitude !== 'number' ||
      typeof selectedLocation.coordinates.longitude !== 'number'
    ) {
      return;
    }

    map.flyTo([selectedLocation.coordinates.latitude, selectedLocation.coordinates.longitude], 16, {
      duration: 1.2,
      easeLinearity: 0.25,
    });
  }, [selectedLocation]);

  // Geolocate user
  const handleLocateUser = () => {
    const map = mapInstanceRef.current;
    if (!map || !navigator.geolocation) return;

    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const { latitude, longitude } = pos.coords;
        map.flyTo([latitude, longitude], 14);

        if (userMarkerRef.current) {
          userMarkerRef.current.setLatLng([latitude, longitude]);
        } else {
          const userIcon = L.divIcon({
            className: 'user-loc-marker',
            html: `
              <div class="relative flex items-center justify-center">
                <span class="animate-ping absolute inline-flex h-8 w-8 rounded-full bg-blue-400 opacity-75"></span>
                <span class="relative inline-flex rounded-full h-4 w-4 bg-blue-500 border-2 border-white shadow-lg"></span>
              </div>
            `,
            iconSize: [32, 32],
            iconAnchor: [16, 16],
          });

          userMarkerRef.current = L.marker([latitude, longitude], { icon: userIcon }).addTo(map);
        }
      },
      (err) => {
        console.warn('Geolocation failed or denied', err);
      }
    );
  };

  const handleZoomIn = () => mapInstanceRef.current?.zoomIn();
  const handleZoomOut = () => mapInstanceRef.current?.zoomOut();

  return (
    <div className="relative w-full h-full bg-slate-950 overflow-hidden">
      {/* Map Container */}
      <div ref={mapContainerRef} className="w-full h-full z-10" />

      {/* Floating Map Controls */}
      <div className="absolute top-4 right-4 z-20 flex flex-col gap-2">
        {/* Zoom Controls */}
        <div className="bg-slate-900/90 backdrop-blur-md border border-slate-700/80 rounded-xl shadow-xl flex flex-col overflow-hidden">
          <button
            onClick={handleZoomIn}
            aria-label="Priblížiť mapu"
            className="p-2.5 text-slate-300 hover:text-white hover:bg-slate-800 transition-colors cursor-pointer"
          >
            <Plus className="w-4 h-4" />
          </button>
          <div className="h-[1px] bg-slate-800"></div>
          <button
            onClick={handleZoomOut}
            aria-label="Oddialiť mapu"
            className="p-2.5 text-slate-300 hover:text-white hover:bg-slate-800 transition-colors cursor-pointer"
          >
            <Minus className="w-4 h-4" />
          </button>
        </div>

        {/* Locate User Button */}
        <button
          onClick={handleLocateUser}
          title="Moja aktuálna GPS poloha"
          className="p-2.5 bg-slate-900/90 hover:bg-slate-800 backdrop-blur-md border border-slate-700/80 rounded-xl shadow-xl text-cyan-400 hover:text-cyan-300 transition-all cursor-pointer active:scale-95"
        >
          <Locate className="w-4 h-4" />
        </button>
      </div>

      {/* Map Legend */}
      <div className="absolute bottom-5 left-4 z-20 hidden md:flex items-center gap-4 px-3.5 py-2 bg-slate-900/90 backdrop-blur-md border border-slate-800 rounded-xl shadow-xl text-xs text-slate-300">
        <div className="flex items-center gap-1.5">
          <span className="w-2.5 h-2.5 rounded-full bg-cyan-400 shadow-[0_0_6px_rgba(6,182,212,0.8)]"></span>
          <span>Ultra-Fast (≥ 150 kW)</span>
        </div>
        <div className="flex items-center gap-1.5">
          <span className="w-2.5 h-2.5 rounded-full bg-emerald-400 shadow-[0_0_6px_rgba(52,211,153,0.8)]"></span>
          <span>Voľné nabíjanie</span>
        </div>
        <div className="flex items-center gap-1.5">
          <span className="w-2.5 h-2.5 rounded-full bg-amber-400"></span>
          <span>Všetko obsadené</span>
        </div>
      </div>
    </div>
  );
};
