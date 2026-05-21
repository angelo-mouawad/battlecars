import type { Metadata } from 'next';
export const metadata: Metadata = { title: 'BB | Manage Players' };
export default function ManageLayout({ children }: { children: React.ReactNode }) {
  return <>{children}</>;
}