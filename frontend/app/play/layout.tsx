import type { Metadata } from 'next';
export const metadata: Metadata = { title: 'Battle Bots' };
export default function PlayLayout({ children }: { children: React.ReactNode }) {
  return <>{children}</>;
}