import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

const PROTECTED = ['/', '/play', '/manage', '/leaderboard'];

async function isValidSession(cookieValue: string | undefined): Promise<boolean> {
  const secret = process.env.ADMIN_SESSION_SECRET;
  if (!secret || !cookieValue) return false;
  try {
    const encoder = new TextEncoder();
    const key = await crypto.subtle.importKey(
      'raw', encoder.encode(secret),
      { name: 'HMAC', hash: 'SHA-256' },
      false, ['sign']
    );
    const signature = await crypto.subtle.sign('HMAC', key, encoder.encode('admin_session'));
    const expected = Array.from(new Uint8Array(signature))
      .map(b => b.toString(16).padStart(2, '0'))
      .join('');
    return expected === cookieValue;
  } catch {
    return false;
  }
}

export async function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const session = request.cookies.get('session')?.value;
  const authenticated = await isValidSession(session);

  const isProtected = PROTECTED.some(p =>
    p === '/' ? pathname === '/' : pathname.startsWith(p)
  );

  if (isProtected && !authenticated) {
    return NextResponse.redirect(new URL('/login', request.url));
  }

  if (pathname.startsWith('/login') && authenticated) {
    return NextResponse.redirect(new URL('/', request.url));
  }

  const isDev = process.env.NODE_ENV === 'development';
  const nonce = btoa(crypto.randomUUID());
  const apiUrl = process.env.NEXT_PUBLIC_API_URL;
  const wsUrl = process.env.NEXT_PUBLIC_WS_URL;

  const cspHeader = isDev ? `
      default-src 'self';
      script-src 'self' 'unsafe-eval' 'unsafe-inline';
      style-src 'self' 'unsafe-inline';
      img-src 'self' blob: data:;
      font-src 'self';
      object-src 'none';
      base-uri 'self';
      form-action 'self';
      frame-ancestors 'none';
      connect-src 'self' http://localhost:8080 ws://localhost:3000;
    `.replace(/\s{2,}/g, ' ').trim()
  : `
      default-src 'self';
      script-src 'self' 'nonce-${nonce}' 'strict-dynamic';
      style-src 'self' 'nonce-${nonce}';
      img-src 'self' blob: data:;
      font-src 'self';
      object-src 'none';
      base-uri 'self';
      form-action 'self';
      frame-ancestors 'none';
      upgrade-insecure-requests;
      connect-src 'self' ${apiUrl} ${wsUrl};
    `.replace(/\s{2,}/g, ' ').trim();

  const requestHeaders = new Headers(request.headers);
  requestHeaders.set('x-nonce', nonce);
  requestHeaders.set('Content-Security-Policy', cspHeader);

  const response = NextResponse.next({ request: { headers: requestHeaders } });
  response.headers.set('Content-Security-Policy', cspHeader);
  response.headers.set('X-Frame-Options', 'DENY');
  response.headers.set('X-Content-Type-Options', 'nosniff');
  return response;
}

export const config = {
  matcher: [
    {
      source: '/((?!api|_next/static|_next/image|favicon.ico).*)',
      missing: [
        { type: 'header', key: 'next-router-prefetch' },
        { type: 'header', key: 'purpose', value: 'prefetch' },
      ],
    },
  ],
};