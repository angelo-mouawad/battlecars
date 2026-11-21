// Every call to the backend carries a short-lived token from /api/auth/token.

export const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

const REFRESH_MARGIN_MS = 60_000;

let cached: { token: string; expiresAt: number } | null = null;
let pending: Promise<string> | null = null;

const fetchToken = async (): Promise<string> => {
  let response: Response;
  try {
    response = await fetch('/api/auth/token', { cache: 'no-store' });
  } catch {
    throw new Error('Could not reach the server');
  }
  if (response.status === 401) {
    window.location.href = '/login';
    throw new Error('Your session has expired, log in again');
  }
  if (!response.ok) throw new Error('Could not authorise with the server');
  cached = await response.json();
  return cached!.token;
};

/** A valid token, fetched again shortly before the current one expires. */
export const getApiToken = (): Promise<string> => {
  if (cached && cached.expiresAt - Date.now() > REFRESH_MARGIN_MS) {
    return Promise.resolve(cached.token);
  }
  pending ??= fetchToken().finally(() => { pending = null; });
  return pending;
};

export const clearApiToken = () => {
  cached = null;
};

const withAuth = (init: RequestInit, token: string | null): RequestInit => {
  const headers = new Headers(init.headers);
  if (token) headers.set('Authorization', `Bearer ${token}`);
  if (init.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
  return { ...init, headers };
};

export const apiFetch = async (path: string, init: RequestInit = {}): Promise<Response> =>
  fetch(API_URL + path, withAuth(init, await getApiToken()));

/**
 * Sends right away with the token already in hand, for `beforeunload`
 * where there is no time to wait for a new one.
 */
export const apiFetchNow = (path: string, init: RequestInit = {}): Promise<Response> => {
  const token = cached && cached.expiresAt > Date.now() ? cached.token : null;
  return fetch(API_URL + path, withAuth(init, token));
};
