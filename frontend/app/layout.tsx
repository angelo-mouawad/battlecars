import type { Metadata } from "next";
import { GeistSans } from "geist/font/sans";
import { GeistMono } from "geist/font/mono";
import "./globals.css";
import { headers } from 'next/headers';
import React from 'react';
export const dynamic = 'force-dynamic';

export const metadata: Metadata = {
  title: 'BB | Lobby',
  description: 'Battle Bots',
  icons: { icon: '/img/icon.png' },
};

export default async function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const listHeaders = await headers();
  const nonce = listHeaders.get('x-nonce') ?? undefined;
  return (
    <html lang="en" nonce={nonce} suppressHydrationWarning>
      <body
        className={`${GeistSans.variable} ${GeistMono.variable} antialiased`}
      >
        {children}
      </body>
    </html>
  );
}
