import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';
import { isValidSession } from '@/lib/auth';

const PROTECTED = ['/', '/play', '/manage', '/leaderboard'];

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
  const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
  const wsUrl = process.env.NEXT_PUBLIC_WS_URL || 'ws://localhost:8080/ws';

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
      connect-src 'self' http://localhost:8080 ws://localhost:3000 ws://localhost:8080;
    `.replace(/\s{2,}/g, ' ').trim()
  // React renders style={{...}} as inline style attributes, which a nonce cannot cover,
  // and browsers ignore 'unsafe-inline' when a nonce is listed. So styles allow inline,
  // while scripts keep the strict nonce rule.
  : `
      default-src 'self';
      script-src 'self' 'nonce-${nonce}' 'strict-dynamic';
      style-src 'self' 'unsafe-inline';
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