'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import LoginForm from '@/components/LoginForm';

export default function LoginPage() {
  const router = useRouter();

  useEffect(() => {
    const isAdmin = document.cookie.split('; ').find(r => r.startsWith('isAdmin='))?.split('=')[1];
    if (isAdmin === 'true') router.replace('/');
  }, [router]);

  return (
    <div className="min-h-screen bg-[#0d1b2e] flex items-center justify-center">
      <LoginForm />
    </div>
  );
}