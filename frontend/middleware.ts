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

export async function middleware(request: NextRequest) {
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

  return NextResponse.next();
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