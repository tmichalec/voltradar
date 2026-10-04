import http from 'node:http';

const BACKEND_URL = process.env.BACKEND_URL || 'http://localhost:8080/api/v1/ingestion/zse/persisted';
const TIMEOUT_MS = Number(process.env.TIMEOUT_MS) || 60_000;
const RETRY_INTERVAL_MS = 500;

console.log(`⏳ Čakám na pripravenosť backendu (${BACKEND_URL})...`);

const startTime = Date.now();

async function checkBackend() {
  try {
    const res = await fetch(BACKEND_URL, { signal: AbortSignal.timeout(1000) });
    if (res.status >= 200 && res.status < 500) {
      console.log(`✅ Backend je pripravený (HTTP ${res.status}). Spúšťam frontend...`);
      process.exit(0);
    }
  } catch (err) {
    // Backend ešte nie je dostupný
  }

  if (Date.now() - startTime > TIMEOUT_MS) {
    console.warn(`⚠️ Časový limit (${TIMEOUT_MS / 1000}s) vypršal. Spúšťam frontend bez potvrdenia backendu.`);
    process.exit(0);
  }

  setTimeout(checkBackend, RETRY_INTERVAL_MS);
}

checkBackend();
