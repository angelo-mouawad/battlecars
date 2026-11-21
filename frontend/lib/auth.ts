// Server-side helpers for the site session and the backend API tokens.
// Uses Web Crypto so it runs in both the proxy and route handlers.

const encoder = new TextEncoder();

/** How long a backend API token lives. The backend refuses anything over an hour. */
export const API_TOKEN_TTL_SECONDS = 15 * 60;

async function hmacHex(secret: string, message: string): Promise<string> {
  const key = await crypto.subtle.importKey(
    'raw', encoder.encode(secret),
    { name: 'HMAC', hash: 'SHA-256' },
    false, ['sign'],
  );
  const signature = await crypto.subtle.sign('HMAC', key, encoder.encode(message));
  return Array.from(new Uint8Array(signature))
    .map(b => b.toString(16).padStart(2, '0'))
    .join('');
}

function safeEqual(a: string, b: string): boolean {
  if (a.length !== b.length) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i++) diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return diff === 0;
}

/** The value of the `session` cookie for someone who logged in with APP_PASSWORD. */
export function sessionToken(appPassword: string): Promise<string> {
  return hmacHex(appPassword, 'admin_session');
}

export async function isValidSession(cookieValue: string | undefined): Promise<boolean> {
  const appPassword = process.env.APP_PASSWORD;
  if (!appPassword || !cookieValue) return false;
  try {
    return safeEqual(await sessionToken(appPassword), cookieValue);
  } catch {
    return false;
  }
}

/**
 * Mints a short-lived token the browser sends to the backend.
 * Must match ApiTokens.java: `<expiry>.<hex HMAC-SHA256(API_SECRET, "battlecars-api:<expiry>")>`.
 */
export async function issueApiToken(apiSecret: string, now = Date.now()) {
  const expiry = Math.floor(now / 1000) + API_TOKEN_TTL_SECONDS;
  const signature = await hmacHex(apiSecret, `battlecars-api:${expiry}`);
  return { token: `${expiry}.${signature}`, expiresAt: expiry * 1000 };
}
