import { NextResponse } from 'next/server';
import { timingSafeEqual } from 'crypto';
import { sessionToken } from '@/lib/auth';

export async function POST(request: Request) {
  const adminPassword = process.env.APP_PASSWORD;
  if (!adminPassword) {
    return NextResponse.json({ error: 'APP_PASSWORD is not configured on the server' }, { status: 500 });
  }

  let password: unknown;
  try {
    ({ password } = await request.json());
  } catch {
    return NextResponse.json({ error: 'Request body must be JSON' }, { status: 400 });
  }

  const given = Buffer.from(typeof password === 'string' ? password : '');
  const expected = Buffer.from(adminPassword);
  const passwordValid = given.length === expected.length && timingSafeEqual(given, expected);

  if (!passwordValid) {
    return NextResponse.json({ error: 'Invalid password' }, { status: 401 });
  }

  const token = await sessionToken(adminPassword);

  const response = NextResponse.json({ ok: true });
  response.cookies.set('session', token, {
    httpOnly: true,
    sameSite: 'strict',
    path: '/',
    secure: process.env.NODE_ENV === 'production',
    maxAge: 60 * 60 * 12,
  });
  return response;
}
