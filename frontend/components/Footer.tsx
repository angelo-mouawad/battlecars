'use client';

import { useRouter, usePathname } from 'next/navigation';

export default function Footer() {
  const router = useRouter();
  const pathname = usePathname();

  const handleLogout = () => {
    document.cookie = 'isAdmin=false; path=/; SameSite=Strict';
    router.push('/login');
  };

  return (
    <footer className="w-full h-[10vh] bg-[#0d1b2e] border-t border-[#1e3d6b] text-gray-500 text-xs font-bold uppercase tracking-widest flex items-center justify-between mt-6 px-6 py-3">
      <span>Battle Bots &copy; {new Date().getFullYear()}</span>
      <div className="flex gap-6">
        {pathname !== '/' && (
          <button
            onClick={() => router.push('/')}
            className="text-blue-400 hover:text-blue-300 transition-colors tracking-widest uppercase text-xs font-bold"
          >
            Back to Lobby
          </button>
        )}
        <button
          onClick={handleLogout}
          className="text-red-500 hover:text-red-400 transition-colors tracking-widest uppercase text-xs font-bold"
        >
          Logout
        </button>
      </div>
    </footer>
  );
}