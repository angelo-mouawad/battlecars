'use client';

import { useSearchParams } from 'next/navigation';
import ControlsIndicator from '@/components/ControlsIndicator';
import Field from '@/components/Field';
import Footer from '@/components/Footer';
import Scoreboard from '@/components/Scoreboard';

export default function PlayPage() {
  const searchParams = useSearchParams();

  const player1Name = searchParams.get('p1name') ?? 'Unknown';
  const player2Name = searchParams.get('p2name') ?? 'Unknown';

  return (
    <div className="h-screen flex flex-col bg-[#0d1b2e]">
      <Scoreboard player1={player1Name} player2={player2Name} />
      <div className="flex flex-1 items-stretch">
        <ControlsIndicator player={1} />
        <Field />
        <ControlsIndicator player={2} />
      </div>
      <Footer />
    </div>
  );
}