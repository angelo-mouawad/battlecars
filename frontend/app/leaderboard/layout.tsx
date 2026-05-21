import type { Metadata } from 'next';
export const metadata: Metadata = { title: 'BB | LeaderBoard' };
export default function LeaderboardLayout({ children }: { children: React.ReactNode }) {
  return <>{children}</>;
}