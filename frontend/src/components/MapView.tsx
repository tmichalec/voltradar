import React, { useEffect, useRef } from 'react';
import L from 'leaflet';
import type { ChargingLocation } from '../types/charging';
import { getMaxLocationPower } from '../utils/formatters';
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

      const distinctTypes = Array.from(new Set(allConnectors.map((c) => c.type)));
      const hasType2 = distinctTypes.includes('TYPE_2');
      const hasCcs = distinctTypes.includes('CCS');

      const plugBadges = [];
      if (hasCcs) plugBadges.push('CCS');
      if (hasType2) plugBadges.push('Type 2');

      const plugLabel = plugBadges.join(' • ');

      const customHtml = `
        <div class="custom-pin relative flex flex-col items-center cursor-pointer transition-transform duration-200 ${selectedClass}">
          <div class="flex flex-col items-center px-2.5 py-1 rounded-2xl bg-gradient-to-r ${bgGradient} text-white font-bold text-[11px] shadow-lg border border-white/20"
               style="box-shadow: 0 4px 14px ${shadowColor}; min-width: 58px;">
            <div class="flex items-center gap-1">
              <span class="w-2 h-2 rounded-full ${statusDot} shadow-sm shrink-0"></span>
              <span>${maxPower > 0 ? `${maxPower} kW` : 'EV'}</span>
            </div>
            ${plugLabel ? `<span class="text-[9px] font-medium text-white/90 leading-tight tracking-tight mt-0.5">${plugLabel}</span>` : ''}
          </div>
          <div class="w-2 h-2 bg-slate-900 rotate-45 -mt-1 border-r border-b border-white/20"></div>
        </div>
      `;

      const icon = L.divIcon({
        className: 'custom-leaflet-marker',
        html: customHtml,
        iconSize: [68, 38],
        iconAnchor: [34, 38],
      });

      const marker = L.marker([location.coordinates.latitude, location.coordinates.longitude], {
        icon,
      });

      const typeSummary = distinctTypes
        .map((t) => {
          const count = allConnectors.filter((c) => c.type === t).length;
          const maxP = Math.max(
            ...allConnectors.filter((c) => c.type === t).map((c) => c.maxPowerKw || 0)
          );
          const name = t === 'TYPE_2' ? 'Type 2 (AC)' : t;
          return `${count}x ${name} ${maxP} kW`;
        })
        .join('<br/>');

      marker.bindTooltip(
        `<div class="text-xs p-1">
          <div class="font-bold text-slate-100 text-[13px] mb-0.5">${location.name}</div>
          <div class="text-[11px] text-emerald-400 font-medium mb-1">${availableCount}/${totalCount} voľných</div>
          <div class="text-[10px] text-slate-300 font-mono">${typeSummary}</div>
        </div>`,
        { direction: 'top', offset: [0, -32], opacity: 0.95 }
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
    if (!map || !selectedLocation) return;

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
