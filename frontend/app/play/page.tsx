'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import ControlsIndicator from '@/components/ControlsIndicator';
import Field from '@/components/Field';
import Footer from '@/components/Footer';
import Scoreboard from '@/components/Scoreboard';

export default function PlayPage() {
  const router = useRouter();

  useEffect(() => {
    const isAdmin = document.cookie.split('; ').find(c => c.startsWith('isAdmin='))?.split('=')[1];
    if (isAdmin !== 'true') {
      router.push('/login');
    }
  }, []);

  return (
    <div className="min-h-screen flex flex-col bg-[#0d1b2e]">
      <Scoreboard />
      <div className="flex flex-1 items-stretch">
        <ControlsIndicator player={1} />
        <Field />
        <ControlsIndicator player={2} />
      </div>
      <Footer />
    </div>
  );
}
