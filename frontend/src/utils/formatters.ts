import type { Connector, ConnectorType, LiveStatus, PowerSharingInfo } from '../types/charging';

export function getStatusBadge(status: LiveStatus): {
  label: string;
  bgClass: string;
  textClass: string;
  borderClass: string;
  dotClass: string;
} {
  switch (status) {
    case 'AVAILABLE':
      return {
        label: 'Voľný',
        bgClass: 'bg-emerald-950/80',
        textClass: 'text-emerald-400',
        borderClass: 'border-emerald-500/40',
        dotClass: 'bg-emerald-400 shadow-[0_0_8px_rgba(52,211,153,0.8)]',
      };
    case 'OCCUPIED':
      return {
        label: 'Obsadený',
        bgClass: 'bg-amber-950/80',
        textClass: 'text-amber-400',
        borderClass: 'border-amber-500/40',
        dotClass: 'bg-amber-400 shadow-[0_0_8px_rgba(251,191,36,0.8)]',
      };
    case 'OUT_OF_ORDER':
      return {
        label: 'Mimo prevádzky',
        bgClass: 'bg-rose-950/80',
        textClass: 'text-rose-400',
        borderClass: 'border-rose-500/40',
        dotClass: 'bg-rose-400 shadow-[0_0_8px_rgba(244,63,94,0.8)]',
      };
    case 'RESERVED':
      return {
        label: 'Rezervovaný',
        bgClass: 'bg-blue-950/80',
        textClass: 'text-blue-400',
        borderClass: 'border-blue-500/40',
        dotClass: 'bg-blue-400',
      };
    case 'OFFLINE':
    case 'UNKNOWN':
    default:
      return {
        label: 'Nedostupný',
        bgClass: 'bg-slate-900/80',
        textClass: 'text-slate-400',
        borderClass: 'border-slate-700/40',
        dotClass: 'bg-slate-400',
      };
  }
}

export function getConnectorTypeLabel(type: ConnectorType): string {
  switch (type) {
    case 'CCS':
      return 'CCS Combo 2';
    case 'CHAdeMO':
      return 'CHAdeMO';
    case 'TYPE_2':
      return 'Type 2 (Mennekes)';
    case 'TYPE_1':
      return 'Type 1 (J1772)';
    case 'SCHUKO':
      return 'Schuko 230V';
    default:
      return 'Neznámy konektor';
  }
}

export function getSharingStatusLabel(sharing: PowerSharingInfo): {
  label: string;
  description: string;
  isShared: boolean;
} {
  if (sharing.status === 'SHARED' || sharing.status === 'DYNAMIC_SHARED') {
    const total = sharing.totalStandPowerKw ? `${sharing.totalStandPowerKw} kW` : 'zdieľaný';
    return {
      label: 'Zdieľaný výkon',
      description: `Stojan dynamicky delí max. ${total} medzi aktívne vozidlá.`,
      isShared: true,
    };
  }
  if (sharing.status === 'STANDALONE') {
    return {
      label: 'Samostatný výkon',
      description: 'Plný výkon je dedikovaný pre tento konektor.',
      isShared: false,
    };
  }
  return {
    label: 'Štandardné zapojenie',
    description: 'Informácie o zdieľaní výkonu nie sú explicitne evidované.',
    isShared: false,
  };
}

export function getMaxLocationPower(connectors: Connector[]): number {
  if (!connectors || connectors.length === 0) return 0;
  return Math.max(...connectors.map(c => c.maxPowerKw || 0));
}

export function getGoogleMapsUrl(lat: number, lng: number, label?: string): string {
  return `https://www.google.com/maps/dir/?api=1&destination=${lat},${lng}${label ? `&destination_place_id=${encodeURIComponent(label)}` : ''}`;
}

export function getWazeUrl(lat: number, lng: number): string {
  return `https://waze.com/ul?ll=${lat},${lng}&navigate=yes`;
}
