'use client';

import { useEffect, useState } from 'react';
import { Player } from '@/data/players';

export default function LeaderboardPage() {
  const [players, setPlayers] = useState<Player[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch('/api/players')
      .then(r => r.json())
      .then((data: Player[]) => {
        setPlayers([...data].sort((a, b) => b.score - a.score));
        setLoading(false);
      });
  }, []);

  return (
    <div className="min-h-screen bg-[#0d1b2e] flex flex-col items-center justify-center gap-8 text-white px-4">
      <h1 className="text-3xl font-black uppercase tracking-widest">Leaderboard</h1>
      <div className="bg-[#1a2e47] border border-[#2a4060] rounded-2xl shadow-2xl w-full max-w-md overflow-hidden">
        <div className="grid grid-cols-3 text-xs font-black uppercase tracking-widest text-gray-400 px-6 py-3 border-b border-[#2a4060]">
          <span>#</span>
          <span>Name</span>
          <span className="text-right">Score</span>
        </div>
        {loading ? (
          <p className="text-gray-400 text-sm text-center py-6">Loading...</p>
        ) : (
          players.map((player, i) => (
            <div
              key={player.id}
              className={'grid grid-cols-3 px-6 py-4 border-b border-[#1e3d6b] last:border-0 ' + (i === 0 ? 'text-yellow-400' : i === 1 ? 'text-gray-300' : i === 2 ? 'text-orange-400' : 'text-white')}
            >
              <span className="font-black text-sm">{i + 1}</span>
              <span className="font-bold text-sm">{player.name}</span>
              <span className="text-right font-black text-sm">{player.score}</span>
            </div>
          ))
        )}
      </div>
    </div>
  );
}