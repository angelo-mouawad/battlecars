'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { Player } from '@/types/types';
import PlayerService from '@/services/PlayerService';

interface SlotState {
  selectedId: string;
  password: string;
  error: string;
  confirmed: boolean;
  creating: boolean;
  newName: string;
  newPassword: string;
}

const emptySlot = (): SlotState => ({
  selectedId: '', password: '', error: '', confirmed: false,
  creating: false, newName: '', newPassword: '',
});

export default function Lobby() {
  const router = useRouter();
  const [playerList, setPlayerList] = useState<Player[]>([]);
  const [loading, setLoading] = useState(true);
  const [slot1, setSlot1] = useState<SlotState>(emptySlot());
  const [slot2, setSlot2] = useState<SlotState>(emptySlot());

  useEffect(() => {
    PlayerService.getAllPlayers()
      .then(setPlayerList)
      .catch(() => setPlayerList([]))
      .finally(() => setLoading(false));
  }, []);

  const updateSlot = (n: 1 | 2, patch: Partial<SlotState>) => {
    if (n === 1) setSlot1(prev => ({ ...prev, ...patch }));
    else setSlot2(prev => ({ ...prev, ...patch }));
  };

  const confirmPassword = async (n: 1 | 2, slot: SlotState) => {
    const player = playerList.find(p => String(p.id) === slot.selectedId);
    if (!player) return;
    try {
      const ok = await PlayerService.login(player.name, slot.password);
      if (!ok) {
        updateSlot(n, { error: 'Wrong password' });
        return;
      }
      updateSlot(n, { confirmed: true, error: '' });
    } catch {
      updateSlot(n, { error: 'Could not reach the server' });
    }
  };

  const createPlayer = async (n: 1 | 2, slot: SlotState) => {
    if (!slot.newName.trim() || !slot.newPassword.trim()) return;
    try {
      const newPlayer = await PlayerService.createPlayer(slot.newName.trim(), slot.newPassword.trim());
      setPlayerList(prev => [...prev, newPlayer]);
      updateSlot(n, { creating: false, selectedId: String(newPlayer.id), confirmed: false, newName: '', newPassword: '', password: '', error: '' });
    } catch {
      updateSlot(n, { error: 'Failed to create player' });
    }
  };

  const canPlay = slot1.confirmed && slot2.confirmed && slot1.selectedId !== slot2.selectedId;

  const renderSlot = (n: 1 | 2, slot: SlotState) => {
    const player = playerList.find(p => String(p.id) === slot.selectedId);
    return (
      <div className="flex flex-col gap-3 w-52">
        <h2 className="text-xs font-black uppercase tracking-widest text-gray-400 text-center">Player {n}</h2>
        {!slot.creating ? (
          <>
            <select
              value={slot.selectedId}
              onChange={e => {
                if (e.target.value === '__new__') updateSlot(n, { creating: true, selectedId: '', confirmed: false });
                else updateSlot(n, { selectedId: e.target.value, confirmed: false, password: '', error: '' });
              }}
              disabled={loading}
              className="bg-[#0d1b2e] border border-[#2a4060] rounded-md px-3 py-2 text-white text-sm outline-none focus:border-blue-500 disabled:opacity-50"
            >
              <option value="">{loading ? 'Loading...' : 'Select player...'}</option>
              {playerList.map(p => (
                <option key={p.id} value={String(p.id)}>{p.name} ({p.score} pts)</option>
              ))}
              <option value="__new__">+ Create new player</option>
            </select>
            {slot.selectedId && !slot.confirmed && (
              <div className="flex flex-col gap-2">
                <input
                  type="password"
                  placeholder="Password"
                  value={slot.password}
                  onChange={e => updateSlot(n, { password: e.target.value, error: '' })}
                  className="bg-[#0d1b2e] border border-[#2a4060] rounded-md px-3 py-2 text-white text-sm outline-none focus:border-blue-500"
                />
                {slot.error && <p className="text-red-400 text-xs font-bold">{slot.error}</p>}
                <button
                  onClick={() => confirmPassword(n, slot)}
                  className="bg-blue-600 hover:bg-blue-500 text-white text-xs font-bold py-1 rounded-md"
                >
                  Confirm
                </button>
              </div>
            )}
            {slot.confirmed && player && (
              <div className="flex items-center justify-between bg-[#0d1b2e] border border-blue-500 rounded-md px-3 py-2">
                <span className="text-sm font-bold text-blue-400">{player.name}</span>
                <button onClick={() => updateSlot(n, emptySlot())} className="text-gray-500 hover:text-white text-xs">x</button>
              </div>
            )}
          </>
        ) : (
          <div className="flex flex-col gap-2">
            <input
              type="text"
              placeholder="Name"
              value={slot.newName}
              onChange={e => updateSlot(n, { newName: e.target.value })}
              className="bg-[#0d1b2e] border border-[#2a4060] rounded-md px-3 py-2 text-white text-sm outline-none focus:border-blue-500"
            />
            <input
              type="password"
              placeholder="Password"
              value={slot.newPassword}
              onChange={e => updateSlot(n, { newPassword: e.target.value })}
              className="bg-[#0d1b2e] border border-[#2a4060] rounded-md px-3 py-2 text-white text-sm outline-none focus:border-blue-500"
            />
            {slot.error && <p className="text-red-400 text-xs font-bold">{slot.error}</p>}
            <div className="flex gap-2">
              <button onClick={() => createPlayer(n, slot)} className="flex-1 bg-blue-600 hover:bg-blue-500 text-white text-xs font-bold py-1 rounded-md">Create</button>
              <button onClick={() => updateSlot(n, { creating: false, error: '' })} className="flex-1 bg-gray-700 hover:bg-gray-600 text-white text-xs font-bold py-1 rounded-md">Cancel</button>
            </div>
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="h-full bg-[#0d1b2e] flex flex-col items-center justify-center gap-10 text-white">
      <h1 className="text-4xl font-black uppercase tracking-[0.3em]">Battle Bots</h1>
      <div className="bg-[#1a2e47] border border-[#2a4060] rounded-2xl p-8 flex flex-col gap-8 shadow-2xl">
        <div className="flex items-start gap-8">
          {renderSlot(1, slot1)}
          <div className="text-3xl font-black text-gray-600 mt-8">VS</div>
          {renderSlot(2, slot2)}
        </div>
        <div className="flex gap-4">
          <button
            onClick={() => {
              if (!canPlay) return;
              const p1 = playerList.find(p => String(p.id) === slot1.selectedId);
              const p2 = playerList.find(p => String(p.id) === slot2.selectedId);
              router.push('/play?p1=' + slot1.selectedId + '&p2=' + slot2.selectedId + '&p1name=' + encodeURIComponent(p1?.name ?? 'Unknown') + '&p2name=' + encodeURIComponent(p2?.name ?? 'Unknown'));
            }}
            disabled={!canPlay}
            className={'flex-1 py-3 rounded-md font-black uppercase tracking-widest text-sm transition-colors ' + (canPlay ? 'bg-blue-600 hover:bg-blue-500 text-white' : 'bg-gray-700 text-gray-500 cursor-not-allowed')}
          >
            Play
          </button>
          <button
            onClick={() => router.push('/leaderboard')}
            className="flex-1 py-3 rounded-md font-black uppercase tracking-widest text-sm bg-[#1e3d6b] hover:bg-[#2a5090] text-white transition-colors"
          >
            Leaderboard
          </button>
        </div>
        <p
          onClick={() => router.push('/manage')}
          className="text-xs text-gray-500 hover:text-gray-300 underline cursor-pointer text-center"
        >
          Manage Players
        </p>
      </div>
    </div>
  );
}