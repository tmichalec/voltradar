import React from 'react';
import { Zap, RefreshCw, Search, Database, Trash2 } from 'lucide-react';

interface NavbarProps {
  totalLocations: number;
  availableConnectors: number;
  totalConnectors: number;
  onRefresh: () => void;
  isRefreshing: boolean;
  onClearAll?: () => void;
  isClearing?: boolean;
  searchQuery: string;
  onSearchChange: (query: string) => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  totalLocations,
  availableConnectors,
  totalConnectors,
  onRefresh,
  isRefreshing,
  onClearAll,
  isClearing = false,
  searchQuery,
  onSearchChange,
}) => {
  return (
    <header className="bg-slate-900/90 backdrop-blur-md border-b border-slate-800 sticky top-0 z-30 px-4 py-3 flex flex-wrap items-center justify-between gap-3 shadow-xl">
      {/* Brand & Logo */}
      <div className="flex items-center gap-3">
        <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-emerald-500 via-teal-500 to-cyan-400 p-[2px] shadow-lg shadow-emerald-500/20">
          <div className="w-full h-full bg-slate-950 rounded-[10px] flex items-center justify-center">
            <Zap className="w-5 h-5 text-emerald-400 fill-emerald-400/30" />
          </div>
        </div>
        <div>
          <div className="flex items-center gap-2">
            <span className="font-bold text-lg tracking-tight bg-gradient-to-r from-white via-slate-100 to-slate-400 bg-clip-text text-transparent">
              VoltRadar
            </span>
            <span className="px-2 py-0.5 text-[10px] font-semibold tracking-wider uppercase rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
              Live MVP
            </span>
          </div>
          <p className="text-xs text-slate-400">EV Charging & Tariff Intelligence</p>
        </div>
      </div>

      {/* Global Search Bar */}
      <div className="flex-1 max-w-md mx-2">
        <div className="relative">
          <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none" />
          <input
            type="text"
            placeholder="Hľadať lokalitu, ulicu alebo mesto (napr. Aupark, Bratislava)..."
            value={searchQuery}
            onChange={(e) => onSearchChange(e.target.value)}
            className="w-full pl-9 pr-4 py-2 bg-slate-950/70 border border-slate-700/80 rounded-xl text-sm text-slate-200 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/50 focus:border-emerald-500 transition-all shadow-inner"
          />
        </div>
      </div>

      {/* Metrics & Actions */}
      <div className="flex items-center gap-3">
        {/* Live Counters */}
        <div className="hidden sm:flex items-center gap-2 px-3 py-1.5 bg-slate-950/60 border border-slate-800 rounded-xl text-xs">
          <div className="flex items-center gap-1.5 text-slate-300">
            <Database className="w-3.5 h-3.5 text-cyan-400" />
            <span className="font-semibold text-white">{totalLocations}</span>
            <span className="text-slate-400">lokalít</span>
          </div>
          <span className="text-slate-700">|</span>
          <div className="flex items-center gap-1.5 text-slate-300">
            <span className="w-2 h-2 rounded-full bg-emerald-400 shadow-[0_0_6px_rgba(52,211,153,0.8)]"></span>
            <span className="font-semibold text-emerald-400">{availableConnectors}</span>
            <span className="text-slate-400">/ {totalConnectors} voľných</span>
          </div>
        </div>

        {/* Clear All Button */}
        {onClearAll && (
          <button
            onClick={onClearAll}
            disabled={isRefreshing || isClearing}
            title="Vymazať všetky dáta z databázy a Redis cache"
            className="flex items-center gap-1.5 px-3 py-2 bg-slate-800 hover:bg-rose-950/60 active:scale-95 text-rose-300 hover:text-rose-200 border border-rose-500/30 text-xs font-medium rounded-xl shadow-sm disabled:opacity-50 disabled:pointer-events-none transition-all cursor-pointer"
          >
            <Trash2 className={`w-3.5 h-3.5 text-rose-400 ${isClearing ? 'animate-pulse' : ''}`} />
            <span>{isClearing ? 'Mazanie...' : 'Vymazať dáta'}</span>
          </button>
        )}

        {/* Sync Button (Stations displayed on the map) */}
        <button
          onClick={onRefresh}
          disabled={isRefreshing || isClearing}
          title="Synchronizovať stanice zobrazené na mape (ZSE API -> PostgreSQL + Redis)"
          className="flex items-center gap-2 px-3.5 py-2 bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 active:scale-95 text-white text-xs font-medium rounded-xl shadow-md shadow-emerald-950/50 disabled:opacity-50 disabled:pointer-events-none transition-all cursor-pointer whitespace-nowrap"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin' : ''}`} />
          <span>{isRefreshing ? 'Synchronizujem...' : '⚡ Synchronizovať stanice na mape'}</span>
        </button>
      </div>
    </header>
  );
};
