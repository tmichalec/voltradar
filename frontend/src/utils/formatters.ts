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

export function getSharingStatusLabel(sharing?: PowerSharingInfo | null): {
  label: string;
  description: string;
  isShared: boolean;
} {
  if (!sharing) {
    return {
      label: 'Štandardné zapojenie',
      description: 'Informácie o zdieľaní výkonu nie sú explicitne evidované.',
      isShared: false,
    };
  }
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

export function getMaxLocationPower(connectors?: Connector[]): number {
  if (!connectors || connectors.length === 0) return 0;
  return Math.max(...connectors.map(c => Number(c?.maxPowerKw) || 0), 0);
}

export interface ConnectorTypeStats {
  type: ConnectorType;
  label: string;
  shortLabel: string;
  currentType: 'AC' | 'DC' | 'UNKNOWN';
  totalCount: number;
  availableCount: number;
  maxPowerKw: number;
  freeParkingMinutes?: number | null;
}

export function getConnectorTypeStats(connectors?: Connector[]): ConnectorTypeStats[] {
  if (!connectors || connectors.length === 0) return [];

  const typesOrder: ConnectorType[] = ['CCS', 'TYPE_2', 'CHAdeMO', 'TYPE_1', 'SCHUKO'];
  const validConnectors = connectors.filter((c): c is Connector => !!c && !!c.type);
  const presentTypes = Array.from(new Set(validConnectors.map(c => c.type)));

  // Sort by defined order
  presentTypes.sort((a, b) => {
    const idxA = typesOrder.indexOf(a);
    const idxB = typesOrder.indexOf(b);
    return (idxA === -1 ? 99 : idxA) - (idxB === -1 ? 99 : idxB);
  });

  return presentTypes.map(type => {
    const matching = validConnectors.filter(c => c.type === type);
    const available = matching.filter(c => c.liveStatus === 'AVAILABLE').length;
    const maxPower = Math.max(...matching.map(c => Number(c.maxPowerKw) || 0), 0);
    const currentType = matching[0]?.currentType || (type === 'TYPE_2' ? 'AC' : 'DC');
    const freeParking = matching.find(c => typeof c.freeParkingMinutes === 'number' && c.freeParkingMinutes > 0)?.freeParkingMinutes || null;

    let shortLabel = type === 'TYPE_2' ? 'Type 2' : type;

    return {
      type,
      label: getConnectorTypeLabel(type),
      shortLabel,
      currentType,
      totalCount: matching.length,
      availableCount: available,
      maxPowerKw: maxPower,
      freeParkingMinutes: freeParking,
    };
  });
}

export function getGoogleMapsUrl(lat?: number, lng?: number, label?: string): string {
  if (lat == null || lng == null) return '#';
  return `https://www.google.com/maps/dir/?api=1&destination=${lat},${lng}${label ? `&destination_place_id=${encodeURIComponent(label)}` : ''}`;
}

export function getWazeUrl(lat?: number, lng?: number): string {
  if (lat == null || lng == null) return '#';
  return `https://waze.com/ul?ll=${lat},${lng}&navigate=yes`;
}

export function formatRelativeTime(dateInput?: string | Date | null): string {
  if (!dateInput) return 'pred chvíľou';
  try {
    const date = typeof dateInput === 'string' ? new Date(dateInput) : dateInput;
    if (isNaN(date.getTime())) return 'pred chvíľou';

    const diffMs = Date.now() - date.getTime();
    const diffSec = Math.floor(diffMs / 1000);

    if (diffSec < 45) {
      return 'práve teraz';
    }
    const diffMin = Math.floor(diffSec / 60);
    if (diffMin < 60) {
      return `pred ${diffMin} min`;
    }
    const diffHours = Math.floor(diffMin / 60);
    if (diffHours < 24) {
      return `pred ${diffHours} hod`;
    }
    const diffDays = Math.floor(diffHours / 24);
    if (diffDays < 7) {
      return `pred ${diffDays} dňami`;
    }
    return date.toLocaleDateString('sk-SK', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' });
  } catch {
    return 'pred chvíľou';
  }
}

export function getGroupFreeParkingMinutes(connectors?: Connector[]): number | null {
  if (!connectors || connectors.length === 0) return null;
  const found = connectors.find(
    (c) => typeof c.freeParkingMinutes === 'number' && c.freeParkingMinutes > 0
  );
  return found ? found.freeParkingMinutes! : null;
}
