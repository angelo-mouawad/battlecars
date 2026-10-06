'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Player } from '@/types/types';
import PlayerService, { errorText } from '@/services/PlayerService';

type ModalMode = 'change-password' | 'delete' | null;

interface ModalState {
  mode: ModalMode;
  player: Player | null;
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
  deletePassword: string;
  error: string;
}

const emptyModal = (): ModalState => ({
  mode: null, player: null,
  oldPassword: '', newPassword: '', confirmPassword: '',
  deletePassword: '', error: '',
});

export default function ManagePage() {
  const router = useRouter();
  const [players, setPlayers] = useState<Player[]>([]);
  const [loading, setLoading] = useState(true);
  const [modal, setModal] = useState<ModalState>(emptyModal());
  const [successes, setSuccesses] = useState<Record<number, string>>({});
  const [creating, setCreating] = useState(false);
  const [newName, setNewName] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [createError, setCreateError] = useState('');
  const [loadError, setLoadError] = useState('');

  useEffect(() => {
    PlayerService.getAllPlayers()
      .then(setPlayers)
      .catch(err => { setPlayers([]); setLoadError(errorText(err, 'Could not load players')); })
      .finally(() => setLoading(false));
  }, []);

  const handleChangePassword = async () => {
    const { player, oldPassword, newPassword, confirmPassword } = modal;
    if (!player) return;
    if (!newPassword.trim()) {
      setModal(prev => ({ ...prev, error: 'New password cannot be empty' }));
      return;
    }
    if (newPassword !== confirmPassword) {
      setModal(prev => ({ ...prev, error: 'Passwords do not match' }));
      return;
    }
    try {
      if (!(await PlayerService.login(player.name, oldPassword))) {
        setModal(prev => ({ ...prev, error: 'Old password is incorrect' }));
        return;
      }
      await PlayerService.changePassword(player.name, newPassword.trim());
      setSuccesses(prev => ({ ...prev, [player.id]: 'Password updated' }));
      setTimeout(() => setSuccesses(prev => ({ ...prev, [player.id]: '' })), 2000);
      setModal(emptyModal());
    } catch (err) {
      setModal(prev => ({ ...prev, error: errorText(err, 'Failed to update password') }));
    }
  };

  const handleDelete = async () => {
    const { player, deletePassword } = modal;
    if (!player) return;
    try {
      if (!(await PlayerService.login(player.name, deletePassword))) {
        setModal(prev => ({ ...prev, error: 'Wrong password' }));
        return;
      }
      await PlayerService.deletePlayer(player.id);
      setPlayers(prev => prev.filter(p => p.id !== player.id));
      setModal(emptyModal());
    } catch (err) {
      setModal(prev => ({ ...prev, error: errorText(err, 'Failed to delete player') }));
    }
  };

  const handleCreate = async () => {
    if (!newName.trim() || !newPassword.trim()) return;
    try {
      const created = await PlayerService.createPlayer(newName.trim(), newPassword.trim());
      setPlayers(prev => [...prev, created]);
      setNewName('');
      setNewPassword('');
      setCreateError('');
      setCreating(false);
    } catch (err) {
      setCreateError(errorText(err, 'Failed to create player'));
    }
  };

  return (
    <div className="min-h-screen bg-[#0d1b2e] flex flex-col items-center justify-center gap-4 text-white px-4 py-12">
      <div className="w-full max-w-lg flex items-center justify-between">
        <h1 className="text-3xl font-black uppercase tracking-widest">Manage Players</h1>
        <button
          onClick={() => { setCreating(prev => !prev); setCreateError(''); }}
          className="bg-blue-600 hover:bg-blue-500 text-white text-xs font-bold px-4 py-2 rounded-md"
        >
          {creating ? 'Cancel' : '+ Add Player'}
        </button>
      </div>

      {creating && (
        <div className="w-full max-w-lg bg-[#1a2e47] border border-[#2a4060] rounded-2xl px-6 py-4 flex flex-col gap-3">
          <input
            type="text"
            placeholder="Name"
            value={newName}
            onChange={e => setNewName(e.target.value)}
            className="bg-[#0d1b2e] border border-[#2a4060] rounded-md px-3 py-2 text-white text-sm outline-none focus:border-blue-500"
          />
          <input
            type="password"
            placeholder="Password"
            value={newPassword}
            onChange={e => setNewPassword(e.target.value)}
            className="bg-[#0d1b2e] border border-[#2a4060] rounded-md px-3 py-2 text-white text-sm outline-none focus:border-blue-500"
          />
          {createError && <p className="text-red-400 text-xs font-bold">{createError}</p>}
          <button onClick={handleCreate} className="bg-blue-600 hover:bg-blue-500 text-white text-xs font-bold py-2 rounded-md">
            Create
          </button>
        </div>
      )}

      <div className="bg-[#1a2e47] border border-[#2a4060] rounded-2xl shadow-2xl w-full max-w-lg overflow-hidden">
        {loading ? (
          <p className="text-gray-400 text-sm text-center py-8">Loading...</p>
        ) : loadError ? (
          <p className="text-red-400 text-sm text-center py-8">{loadError}</p>
        ) : players.length === 0 ? (
          <p className="text-gray-400 text-sm text-center py-8">No players found.</p>
        ) : (
          players.map(player => (
            <div key={player.id} className="flex flex-col gap-1 px-6 py-4 border-b border-[#1e3d6b] last:border-0">
              <div className="flex items-center justify-between">
                <span className="font-bold text-sm">{player.name}</span>
                <div className="flex gap-2">
                  <button
                    onClick={() => setModal({ ...emptyModal(), mode: 'change-password', player })}
                    className="bg-blue-600 hover:bg-blue-500 text-white text-xs font-bold px-3 py-1.5 rounded-md"
                  >
                    Change Password
                  </button>
                  <button
                    onClick={() => setModal({ ...emptyModal(), mode: 'delete', player })}
                    className="bg-red-700 hover:bg-red-600 text-white text-xs font-bold px-3 py-1.5 rounded-md"
                  >
                    Delete
                  </button>
                </div>
              </div>
              {successes[player.id] && <p className="text-green-400 text-xs font-bold">{successes[player.id]}</p>}
            </div>
          ))
        )}
      </div>

      <button onClick={() => router.back()} className="text-xs text-gray-400 hover:text-white underline">
        ← Back
      </button>

      {modal.mode && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 px-4">
          <div className="bg-[#1a2e47] border border-[#2a4060] rounded-2xl p-6 w-full max-w-sm flex flex-col gap-4">
            {modal.mode === 'change-password' && (
              <>
                <h2 className="text-sm font-black uppercase tracking-widest">Change Password, {modal.player?.name}</h2>
                <input
                  type="password"
                  placeholder="Old password"
                  value={modal.oldPassword}
                  onChange={e => setModal(prev => ({ ...prev, oldPassword: e.target.value, error: '' }))}
                  className="bg-[#0d1b2e] border border-[#2a4060] rounded-md px-3 py-2 text-white text-sm outline-none focus:border-blue-500"
                />
                <input
                  type="password"
                  placeholder="New password"
                  value={modal.newPassword}
                  onChange={e => setModal(prev => ({ ...prev, newPassword: e.target.value, error: '' }))}
                  className="bg-[#0d1b2e] border border-[#2a4060] rounded-md px-3 py-2 text-white text-sm outline-none focus:border-blue-500"
                />
                <input
                  type="password"
                  placeholder="Confirm new password"
                  value={modal.confirmPassword}
                  onChange={e => setModal(prev => ({ ...prev, confirmPassword: e.target.value, error: '' }))}
                  className="bg-[#0d1b2e] border border-[#2a4060] rounded-md px-3 py-2 text-white text-sm outline-none focus:border-blue-500"
                />
                {modal.error && <p className="text-red-400 text-xs font-bold">{modal.error}</p>}
                <div className="flex gap-2">
                  <button onClick={handleChangePassword} className="flex-1 bg-blue-600 hover:bg-blue-500 text-white text-xs font-bold py-2 rounded-md">Update</button>
                  <button onClick={() => setModal(emptyModal())} className="flex-1 bg-gray-700 hover:bg-gray-600 text-white text-xs font-bold py-2 rounded-md">Cancel</button>
                </div>
              </>
            )}
            {modal.mode === 'delete' && (
              <>
                <h2 className="text-sm font-black uppercase tracking-widest">Delete, {modal.player?.name}</h2>
                <p className="text-xs text-gray-400">Enter the player&apos;s password to confirm deletion.</p>
                <input
                  type="password"
                  placeholder="Password"
                  value={modal.deletePassword}
                  onChange={e => setModal(prev => ({ ...prev, deletePassword: e.target.value, error: '' }))}
                  className="bg-[#0d1b2e] border border-[#2a4060] rounded-md px-3 py-2 text-white text-sm outline-none focus:border-blue-500"
                />
                {modal.error && <p className="text-red-400 text-xs font-bold">{modal.error}</p>}
                <div className="flex gap-2">
                  <button onClick={handleDelete} className="flex-1 bg-red-700 hover:bg-red-600 text-white text-xs font-bold py-2 rounded-md">Delete</button>
                  <button onClick={() => setModal(emptyModal())} className="flex-1 bg-gray-700 hover:bg-gray-600 text-white text-xs font-bold py-2 rounded-md">Cancel</button>
                </div>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  );
}