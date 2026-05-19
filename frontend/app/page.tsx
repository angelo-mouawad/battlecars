'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import Lobby from '@/components/Lobby';
import Footer from '@/components/Footer';

export default function Home() {
  const router = useRouter();

  useEffect(() => {
    const isAdmin = document.cookie.split('; ').find(c => c.startsWith('isAdmin='))?.split('=')[1];
    if (isAdmin !== 'true') {
      router.push('/login');
    }
  }, []);

  return (
    <>
      <Lobby />
      <Footer />
    </>
  );
}
