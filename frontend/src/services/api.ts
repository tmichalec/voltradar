import type { ChargingLocation } from '../types/charging';

const API_BASE_URL = import.meta.env.VITE_API_URL || '';

export interface RefreshResponse {
  totalFetched: number;
  totalPersisted: number;
  durationMs: number;
  success: boolean;
  message?: string;
}

async function requestWithFallback(endpoint: string, options?: RequestInit): Promise<Response> {
  const primaryUrl = `${API_BASE_URL}${endpoint}`;
  try {
    const res = await fetch(primaryUrl, options);
    if (res.ok) {
      return res;
    }
    // If not OK and no explicit base URL is set, try direct localhost:8080 backend fallback
    if (!API_BASE_URL && (res.status === 404 || res.status === 502 || res.status === 504)) {
      const fallbackUrl = `http://localhost:8080${endpoint}`;
      const fallbackRes = await fetch(fallbackUrl, options);
      if (fallbackRes.ok) return fallbackRes;
    }
    return res;
  } catch (err) {
    if (!API_BASE_URL) {
      // Network error (proxy down / direct dev mode), try localhost:8080 directly
      const fallbackUrl = `http://localhost:8080${endpoint}`;
      return await fetch(fallbackUrl, options);
    }
    throw err;
  }
}

export const api = {
  /**
   * Fetches all aggregated locations stored in the PostgreSQL database,
   * enriched with live connector status from Redis cache.
   */
  async getPersistedLocations(): Promise<ChargingLocation[]> {
    const response = await requestWithFallback('/api/v1/ingestion/zse/persisted', {
      headers: {
        'Accept': 'application/json',
      },
    });

    if (!response.ok) {
      throw new Error(`Nepodarilo sa načítať lokality z databázy (HTTP ${response.status})`);
    }

    return response.json();
  },

  /**
   * Triggers a manual refresh of ZSE Drive charging stations, updating both
   * PostgreSQL database and Redis status cache.
   */
  async triggerRefresh(): Promise<RefreshResponse> {
    const response = await requestWithFallback('/api/v1/ingestion/zse/refresh', {
      method: 'POST',
      headers: {
        'Accept': 'application/json',
      },
    });

    if (!response.ok) {
      throw new Error(`Chyba pri obnovovaní staníc (HTTP ${response.status})`);
    }

    return response.json();
  },

  /**
   * Triggers a targeted refresh of only the OC Retro location (stations 79480, 316067).
   */
  async triggerRetroRefresh(): Promise<RefreshResponse> {
    const response = await requestWithFallback('/api/v1/ingestion/zse/refresh/retro', {
      method: 'POST',
      headers: {
        'Accept': 'application/json',
      },
    });

    if (!response.ok) {
      throw new Error(`Chyba pri obnovovaní lokality Retro (HTTP ${response.status})`);
    }

    return response.json();
  },

  /**
   * Clears all persisted data from database and cache.
   */
  async clearAllData(): Promise<void> {
    const response = await requestWithFallback('/api/v1/ingestion/zse/clear', {
      method: 'POST',
      headers: {
        'Accept': 'application/json',
      },
    });

    if (!response.ok) {
      throw new Error(`Chyba pri mazaní dát (HTTP ${response.status})`);
    }
  },

  /**
   * Direct ingest for Bratislava region as fallback or quick load.
   */
  async getBratislavaLive(): Promise<ChargingLocation[]> {
    const response = await requestWithFallback('/api/v1/ingestion/zse/bratislava', {
      headers: {
        'Accept': 'application/json',
      },
    });

    if (!response.ok) {
      throw new Error(`Nepodarilo sa načítať živé dáta pre Bratislavu (HTTP ${response.status})`);
    }

    return response.json();
  },
};
