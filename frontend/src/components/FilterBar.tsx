import React from 'react';
import type { FilterState, ConnectorType } from '../types/charging';
import { Filter, Zap, CheckCircle2, Share2 } from 'lucide-react';

interface FilterBarProps {
  filters: FilterState;
  onFilterChange: (filters: FilterState) => void;
  filteredCount: number;
  totalCount: number;
}

export const FilterBar: React.FC<FilterBarProps> = ({
  filters,
  onFilterChange,
  filteredCount,
  totalCount,
}) => {
  const toggleConnectorType = (type: ConnectorType) => {
    const exists = filters.connectorTypes.includes(type);
    const updated = exists
      ? filters.connectorTypes.filter((t) => t !== type)
      : [...filters.connectorTypes, type];
    onFilterChange({ ...filters, connectorTypes: updated });
  };

  const setMinPower = (power: number) => {
    onFilterChange({
      ...filters,
      minPowerKw: filters.minPowerKw === power ? 0 : power,
    });
  };

  const resetFilters = () => {
    onFilterChange({
      searchQuery: '',
      onlyAvailable: false,
      minPowerKw: 0,
      connectorTypes: [],
      onlyShared: false,
    });
  };

  const hasActiveFilters =
    filters.onlyAvailable ||
    filters.minPowerKw > 0 ||
    filters.connectorTypes.length > 0 ||
    filters.onlyShared ||
    filters.searchQuery.length > 0;

  return (
    <div className="bg-slate-900/80 backdrop-blur border-b border-slate-800/80 px-4 py-2 flex flex-wrap items-center justify-between gap-2 text-xs z-20">
      {/* Filter Pills */}
      <div className="flex flex-wrap items-center gap-1.5">
        <span className="flex items-center gap-1 text-slate-400 font-medium mr-1">
          <Filter className="w-3.5 h-3.5 text-slate-500" />
          Filtre:
        </span>

        {/* Only Available */}
        <button
          onClick={() => onFilterChange({ ...filters, onlyAvailable: !filters.onlyAvailable })}
          className={`flex items-center gap-1.5 px-2.5 py-1 rounded-lg border transition-all cursor-pointer ${
            filters.onlyAvailable
              ? 'bg-emerald-500/20 text-emerald-300 border-emerald-500/50 shadow-sm shadow-emerald-900/30'
              : 'bg-slate-950/40 text-slate-400 border-slate-800 hover:border-slate-700 hover:text-slate-300'
          }`}
        >
          <CheckCircle2 className={`w-3.5 h-3.5 ${filters.onlyAvailable ? 'text-emerald-400' : 'text-slate-500'}`} />
          <span>Iba voľné</span>
        </button>

        {/* Ultra-Fast > 100 kW */}
        <button
          onClick={() => setMinPower(100)}
          className={`flex items-center gap-1.5 px-2.5 py-1 rounded-lg border transition-all cursor-pointer ${
            filters.minPowerKw === 100
              ? 'bg-cyan-500/20 text-cyan-300 border-cyan-500/50 shadow-sm shadow-cyan-900/30'
              : 'bg-slate-950/40 text-slate-400 border-slate-800 hover:border-slate-700 hover:text-slate-300'
          }`}
        >
          <Zap className={`w-3.5 h-3.5 ${filters.minPowerKw === 100 ? 'text-cyan-400' : 'text-slate-500'}`} />
          <span>UFC ≥ 100 kW</span>
        </button>

        {/* Fast DC ≥ 50 kW */}
        <button
          onClick={() => setMinPower(50)}
          className={`flex items-center gap-1.5 px-2.5 py-1 rounded-lg border transition-all cursor-pointer ${
            filters.minPowerKw === 50
              ? 'bg-blue-500/20 text-blue-300 border-blue-500/50 shadow-sm shadow-blue-900/30'
              : 'bg-slate-950/40 text-slate-400 border-slate-800 hover:border-slate-700 hover:text-slate-300'
          }`}
        >
          <span>DC ≥ 50 kW</span>
        </button>

        {/* CCS Connector */}
        <button
          onClick={() => toggleConnectorType('CCS')}
          className={`px-2.5 py-1 rounded-lg border font-mono transition-all cursor-pointer ${
            filters.connectorTypes.includes('CCS')
              ? 'bg-indigo-500/20 text-indigo-300 border-indigo-500/50 shadow-sm shadow-indigo-900/30'
              : 'bg-slate-950/40 text-slate-400 border-slate-800 hover:border-slate-700 hover:text-slate-300'
          }`}
        >
          CCS
        </button>

        {/* Type 2 Connector */}
        <button
          onClick={() => toggleConnectorType('TYPE_2')}
          className={`px-2.5 py-1 rounded-lg border font-mono transition-all cursor-pointer ${
            filters.connectorTypes.includes('TYPE_2')
              ? 'bg-indigo-500/20 text-indigo-300 border-indigo-500/50 shadow-sm shadow-indigo-900/30'
              : 'bg-slate-950/40 text-slate-400 border-slate-800 hover:border-slate-700 hover:text-slate-300'
          }`}
        >
          Type 2 (AC)
        </button>

        {/* Power Sharing Only */}
        <button
          onClick={() => onFilterChange({ ...filters, onlyShared: !filters.onlyShared })}
          className={`flex items-center gap-1.5 px-2.5 py-1 rounded-lg border transition-all cursor-pointer ${
            filters.onlyShared
              ? 'bg-purple-500/20 text-purple-300 border-purple-500/50 shadow-sm shadow-purple-900/30'
              : 'bg-slate-950/40 text-slate-400 border-slate-800 hover:border-slate-700 hover:text-slate-300'
          }`}
        >
          <Share2 className={`w-3.5 h-3.5 ${filters.onlyShared ? 'text-purple-400' : 'text-slate-500'}`} />
          <span>Zdieľaný výkon</span>
        </button>

        {/* Reset button */}
        {hasActiveFilters && (
          <button
            onClick={resetFilters}
            className="text-slate-500 hover:text-slate-300 underline ml-2 transition-colors cursor-pointer"
          >
            Zrušiť filtre
          </button>
        )}
      </div>

      {/* Result count */}
      <div className="text-slate-400 text-[11px]">
        Zobrazených <span className="font-semibold text-white">{filteredCount}</span> z {totalCount} lokalít
      </div>
    </div>
  );
};
