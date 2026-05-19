'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import FormInput from './FormInput';

export default function LoginForm() {
  const router = useRouter();
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (password !== process.env.NEXT_PUBLIC_ADMIN_PASSWORD) {
      setError('Incorrect password');
      return;
    }
    document.cookie = 'isAdmin=true; path=/; SameSite=Strict';
    router.push('/');
  };

  return (
    <div className="bg-[#1a2e47] border border-[#2a4060] rounded-xl p-8 w-full max-w-sm shadow-2xl flex flex-col gap-6">
      <h1 className="text-white font-black uppercase tracking-widest text-center text-xl">Admin Login</h1>
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <FormInput label="Password" type="password" value={password} onChange={setPassword} required />
        {error && <p className="text-red-400 text-xs font-bold tracking-wide">{error}</p>}
        <button
          type="submit"
          className="bg-blue-600 hover:bg-blue-500 text-white font-black uppercase tracking-widest text-sm py-2 rounded-md transition-colors"
        >
          Sign In
        </button>
      </form>
    </div>
  );
}
