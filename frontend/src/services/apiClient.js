import axios from 'axios';

const TOKEN_KEY = 'velnora-access-token-v1';

// Keep local development usable when the optional .env.local file has not been
// loaded yet. Production still requires an explicit API URL.
const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim();
const apiBaseUrl = configuredBaseUrl || (import.meta.env.DEV ? 'http://localhost:8080/api' : '');

export const apiConfigured = Boolean(apiBaseUrl);

export function resolveMediaUrl(path) {
  if (!path) return '';
  if (/^(?:https?:|data:|blob:)/i.test(path)) return path;
  const appOrigin = typeof window === 'undefined' ? '' : window.location.origin;
  const apiOrigin = new URL(apiBaseUrl || appOrigin || '/', appOrigin || 'http://localhost').origin;
  return `${apiOrigin}/${String(path).replace(/^\/+/, '')}`;
}

const apiClient = axios.create({
  baseURL: apiBaseUrl,
  headers: {
    Accept: 'application/json',
  },
});

export function readAccessToken() {
  try { return sessionStorage.getItem(TOKEN_KEY) || ''; } catch { return ''; }
}

export function writeAccessToken(token) {
  try {
    if (token) sessionStorage.setItem(TOKEN_KEY, token);
    else sessionStorage.removeItem(TOKEN_KEY);
  } catch { /* Private browsing may disable session storage. */ }
}

apiClient.interceptors.request.use((config) => {
  const token = readAccessToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

apiClient.interceptors.response.use((response) => response, (error) => {
  if (error.response?.status === 401) {
    writeAccessToken('');
    if (typeof window !== 'undefined') window.dispatchEvent(new Event('velnora:session-expired'));
  }
  return Promise.reject(error);
});

export function apiErrorMessage(error, fallback = 'We could not complete that request. Please try again.') {
  const body = error?.response?.data;
  if (typeof body === 'string' && body.trim()) return body;
  if (body?.message) return body.message;
  if (body?.error) return body.error;
  if (body?.errors && typeof body.errors === 'object') return Object.values(body.errors).flat().join(' ');
  if (!error?.response) return 'The Velnora service could not be reached. Check your connection and try again.';
  return fallback;
}

export default apiClient;
