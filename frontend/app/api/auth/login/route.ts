import { NextResponse } from 'next/server';
import { createHmac, timingSafeEqual } from 'crypto';

export async function POST(request: Request) {
  const { password } = await request.json();

  const adminPassword = process.env.NEXT_PUBLIC_APP_PASSWORD;
  if (!adminPassword) {
    return NextResponse.json({ error: 'Server not configured' }, { status: 500 });
  }

  // Constant-time comparison to prevent timing attacks
  let passwordValid = false;
  try {
    const a = Buffer.from(password ?? '');
    const b = Buffer.from(adminPassword);
    passwordValid = a.length === b.length && timingSafeEqual(a, b);
  } catch {
    passwordValid = false;
  }

  if (!passwordValid) {
    return NextResponse.json({ error: 'Invalid password' }, { status: 401 });
  }

  const token = createHmac('sha256', adminPassword).update('admin_session').digest('hex');

  const response = NextResponse.json({ ok: true });
  response.cookies.set('session', token, {
    httpOnly: true,
    sameSite: 'strict',
    path: '/',
    secure: process.env.NODE_ENV === 'production',
  });
  return response;
}