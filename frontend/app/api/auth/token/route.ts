import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';
import { isValidSession, issueApiToken } from '@/lib/auth';

// Hands a logged-in browser a short-lived token for the backend API and WebSocket.
export async function GET(request: NextRequest) {
  if (!(await isValidSession(request.cookies.get('session')?.value))) {
    return NextResponse.json({ error: 'Not logged in' }, { status: 401 });
  }

  const apiSecret = process.env.API_SECRET;
  if (!apiSecret) {
    return NextResponse.json({ error: 'API_SECRET is not configured on the server' }, { status: 500 });
  }

  return NextResponse.json(await issueApiToken(apiSecret), {
    headers: { 'Cache-Control': 'no-store' },
  });
}
