'use client';

import { useEffect, useRef, useState, useCallback } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import ControlsIndicator from '@/components/ControlsIndicator';
import Field from '@/components/Field';
import Footer from '@/components/Footer';
import Scoreboard from '@/components/Scoreboard';

const API_URL = process.env.NEXT_PUBLIC_API_URL || '';
const MAX_HEALTH = 50;

interface HitResult {
  type: string;
  matchId: number;
  hitCarId: number;
  damage: number;
  car1Health?: number;
  car2Health?: number;
  matchOver: boolean;
  winnerId?: number;
  winnerName?: string;
  winnerCarId?: number;
}

export default function PlayPage() {
  const searchParams = useSearchParams();

  // Player info from URL — set by Lobby
  const player1Id = searchParams.get('p1') ? Number(searchParams.get('p1')) : null;
  const player2Id = searchParams.get('p2') ? Number(searchParams.get('p2')) : null;
  const player1Name = searchParams.get('p1name') ?? 'Player 1';
  const player2Name = searchParams.get('p2name') ?? 'Player 2';

  const router = useRouter();

  // Game state
  const [health1, setHealth1] = useState(MAX_HEALTH);
  const [health2, setHealth2] = useState(MAX_HEALTH);
  const [winner, setWinner] = useState<{ name: string; carId: number } | null>(null);
  const [gameReady, setGameReady] = useState(false);

  const ws = useRef<WebSocket | null>(null);
  const matchIdRef = useRef<number | null>(null); // ref so WS handler always has latest value

  // -------------------------------------------------------------------------
  // WebSocket — connect once, handle incoming messages
  // -------------------------------------------------------------------------
  const connectWebSocket = useCallback(() => {
    const WS_URL = process.env.NEXT_PUBLIC_WS_URL || 'wss://team-01-en-backend-itip-en-01.apps.okd.ucll.cloud/ws';
    ws.current = new WebSocket(WS_URL);

    ws.current.onopen = () => {
      console.log('WS connected');
      // If we already have a matchId (e.g. after reconnect), re-send MATCH_START
      if (matchIdRef.current) {
        ws.current?.send(`MATCH_START:${matchIdRef.current}`);
      }
    };

    ws.current.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);

        if (data.type === 'HIT_RESULT') {
          const hit = data as HitResult;

          // Update health bars
          if (hit.car1Health !== undefined) setHealth1(hit.car1Health);
          if (hit.car2Health !== undefined) setHealth2(hit.car2Health);

          // Handle match over
          if (hit.matchOver && hit.winnerName && hit.winnerCarId !== undefined) {
            setWinner({ name: hit.winnerName, carId: hit.winnerCarId });
          }
        }
      } catch {
        // Not JSON — ignore (e.g. raw ESP32 movement data)
      }
    };

    ws.current.onerror = (e) => console.error('WS error', e);
    ws.current.onclose = () => console.log('WS disconnected');
  }, []);

  const sendCommand = useCallback((cmd: string) => {
    if (ws.current?.readyState === WebSocket.OPEN) {
      ws.current.send(cmd);
    }
  }, []);

  // -------------------------------------------------------------------------
  // On mount: auth check → create match → start match → tell WS → game ready
  // -------------------------------------------------------------------------

  useEffect(() => {
    // Need both player IDs from URL
    if (!player1Id || !player2Id) {
      router.push('/');
      return;
    }

    connectWebSocket();

    // Create match → start it → send MATCH_START over WS
    const initMatch = async () => {
      try {
        // Step 1: create the match
        const createRes = await fetch(`${API_URL}/api/matches`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            player1Id: player1Id,
            car1Id: 1,
            player2Id: player2Id,
            car2Id: 2,
          }),
        });

        if (!createRes.ok) throw new Error('Failed to create match');
        const created = await createRes.json();
        const newMatchId: number = created.matchId;

        // Step 2: start the match
        await fetch(`${API_URL}/api/matches/${newMatchId}/start`, { method: 'POST' });

        // Step 3: store match ID and tell the WebSocket handler
        matchIdRef.current = newMatchId;

        // WS might not be open yet — wait for it
        const sendStart = () => {
          if (ws.current?.readyState === WebSocket.OPEN) {
            ws.current.send(`MATCH_START:${newMatchId}`);
            setGameReady(true);
          } else {
            setTimeout(sendStart, 100);
          }
        };
        sendStart();

      } catch (err) {
        console.error('Could not init match:', err);
        // Don't block the UI — still let players play even if match creation fails
        setGameReady(true);
      }
    };

    initMatch();

    return () => {
      if (ws.current?.readyState === WebSocket.OPEN) {
        ws.current.send('MATCH_END');
      }
      ws.current?.close();
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // -------------------------------------------------------------------------
  // Winner screen overlay — shown when match ends
  // -------------------------------------------------------------------------
  if (winner) {
    const winnerIsPlayer1 = winner.carId === 1;
    return (
      <div className="h-screen flex flex-col items-center justify-center bg-[#0d1b2e] text-white gap-8">
        <div className="flex flex-col items-center gap-4">
          <img
            src={winnerIsPlayer1 ? '/img/bronco.png' : '/img/Magnitude.png'}
            alt="Winner"
            className="w-24 h-24 rounded-full border-4 border-yellow-400 object-cover"
          />
          <h1 className="text-5xl font-black uppercase tracking-widest text-yellow-400">
            {winner.name} Wins!
          </h1>
          <p className="text-gray-400 text-sm tracking-widest uppercase">
            {winnerIsPlayer1 ? player1Name : player2Name} is victorious
          </p>
        </div>

        <div className="flex gap-4">
          <button
            onClick={() => router.push('/')}
            className="px-8 py-3 rounded-md font-black uppercase tracking-widest text-sm bg-blue-600 hover:bg-blue-500 text-white transition-colors"
          >
            Play Again
          </button>
          <button
            onClick={() => router.push('/leaderboard')}
            className="px-8 py-3 rounded-md font-black uppercase tracking-widest text-sm bg-[#1e3d6b] hover:bg-[#2a5090] text-white transition-colors"
          >
            Leaderboard
          </button>
        </div>
      </div>
    );
  }

  // -------------------------------------------------------------------------
  // Main game UI
  // -------------------------------------------------------------------------
  return (
    <div className="h-screen flex flex-col bg-[#0d1b2e]">
      <Scoreboard
        player1={player1Name}
        player2={player2Name}
        health1={health1}
        health2={health2}
      />

      {!gameReady && (
        <div className="absolute inset-0 bg-[#0d1b2e]/80 flex items-center justify-center z-50">
          <p className="text-white text-sm font-black uppercase tracking-widest animate-pulse">
            Starting match...
          </p>
        </div>
      )}

      <div className="flex flex-1 items-stretch">
        <ControlsIndicator player={1} sendCommand={sendCommand} />
        <Field />
        <ControlsIndicator player={2} sendCommand={sendCommand} />
      </div>
      <Footer />
    </div>
  );
}