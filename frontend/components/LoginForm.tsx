'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import FormInput from './FormInput';

export default function LoginForm() {
  const router = useRouter();
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ password }),
      });
      if (!res.ok) {
        setError('Incorrect password');
        return;
      }
      router.push('/');
    } catch {
      setError('Could not reach the server');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="bg-[#1a2e47] border border-[#2a4060] rounded-xl p-8 w-full max-w-sm shadow-2xl flex flex-col gap-6">
      <h1 className="text-white font-black uppercase tracking-widest text-center text-xl">Admin Login</h1>
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <FormInput label="Password" type="password" value={password} onChange={setPassword} required />
        {error && <p className="text-red-400 text-xs font-bold tracking-wide">{error}</p>}
        <button
          type="submit"
          disabled={loading}
          className="bg-blue-600 hover:bg-blue-500 disabled:opacity-50 text-white font-black uppercase tracking-widest text-sm py-2 rounded-md transition-colors"
        >
          {loading ? 'Signing in...' : 'Sign In'}
        </button>
      </form>
    </div>
  );
}