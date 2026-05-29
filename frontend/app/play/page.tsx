'use client';

import { useEffect, useRef, useState, useCallback } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import ControlsIndicator from '@/components/ControlsIndicator';
import CommentaryTicker from '@/components/CommentaryTicker';
import Field from '@/components/Field';
import Footer from '@/components/Footer';
import Scoreboard from '@/components/Scoreboard';

const API_URL = process.env.NEXT_PUBLIC_API_URL || '';
const MAX_HEALTH = 50;

const MOVE_STEP = 5;
const MIN_POS = 5;
const MAX_POS = 95;

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

interface CarPosition {
  x: number;
  y: number;
}

function clamp(val: number, min: number, max: number) {
  return Math.min(Math.max(val, min), max);
}

export default function PlayPage() {
  const searchParams = useSearchParams();

  const player1Id = searchParams.get('p1') ? Number(searchParams.get('p1')) : null;
  const player2Id = searchParams.get('p2') ? Number(searchParams.get('p2')) : null;
  const player1Name = searchParams.get('p1name') ?? 'Player 1';
  const player2Name = searchParams.get('p2name') ?? 'Player 2';
  

  const router = useRouter();

  const [health1, setHealth1] = useState(MAX_HEALTH);
  const [health2, setHealth2] = useState(MAX_HEALTH);
  const [winner, setWinner] = useState<{ name: string; carId: number } | null>(null);
  const [gameReady, setGameReady] = useState(false);
  const [commentary, setCommentary] = useState<string[]>([]);

  const [car1Pos, setCar1Pos] = useState<CarPosition>({ x: 25, y: 50 });
  const [car2Pos, setCar2Pos] = useState<CarPosition>({ x: 75, y: 50 });

  const [car1Hit, setCar1Hit] = useState(false);
  const [car2Hit, setCar2Hit] = useState(false);

  const ws = useRef<WebSocket | null>(null);
  const matchIdRef = useRef<number | null>(null);

  const audioContextRef = useRef<AudioContext | null>(null);

  const updatePosition = useCallback((cmd: string) => {
    const parts = cmd.split(':');
    if (parts.length !== 2) return;
    const carId = parts[0];
    const direction = parts[1];
    if (direction === 'STOP') return;
    const setter = carId === '1' ? setCar1Pos : setCar2Pos;
    setter(prev => {
      switch (direction) {
        case 'UP':    return { ...prev, y: clamp(prev.y - MOVE_STEP, MIN_POS, MAX_POS) };
        case 'DOWN':  return { ...prev, y: clamp(prev.y + MOVE_STEP, MIN_POS, MAX_POS) };
        case 'LEFT':  return { ...prev, x: clamp(prev.x - MOVE_STEP, MIN_POS, MAX_POS) };
        case 'RIGHT': return { ...prev, x: clamp(prev.x + MOVE_STEP, MIN_POS, MAX_POS) };
        default:      return prev;
      }
    });
    
  }, []);

  const triggerHitFlash = useCallback((carId: number) => {
    const setter = carId === 1 ? setCar1Hit : setCar2Hit;
    setter(true);
    setTimeout(() => setter(false), 600);
  }, []);

  const connectWebSocket = useCallback(() => {
    const WS_URL = process.env.NEXT_PUBLIC_WS_URL || 'wss://team-01-en-backend-itip-en-01.apps.okd.ucll.cloud/ws';
    ws.current = new WebSocket(WS_URL);

    ws.current.onopen = () => {
      if (matchIdRef.current) {
        ws.current?.send(`MATCH_START:${matchIdRef.current}`);
      }
    };

    ws.current.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);

        if (data.type === 'HIT_RESULT') {
          const hit = data as HitResult;
          if (hit.car1Health !== undefined) setHealth1(hit.car1Health);
          if (hit.car2Health !== undefined) setHealth2(hit.car2Health);
          if (hit.hitCarId) triggerHitFlash(hit.hitCarId);
          if (hit.matchOver && hit.winnerName && hit.winnerCarId !== undefined) {
            matchIdRef.current = null;
            setWinner({ name: hit.winnerName, carId: hit.winnerCarId });
          }
        } else if (data.type === 'COMMENTARY') {
          setCommentary(prev => [...prev, data.text]);
        } else if (data.type === 'COMMENTARY_AUDIO') {
          (async () => {
            try {
              if (!audioContextRef.current) {
                audioContextRef.current = new AudioContext();
              }
              const audioContext = audioContextRef.current;
              if (audioContext.state === 'suspended') {
                await audioContext.resume();
              }
              const audioBytes = Uint8Array.from(atob(data.audio), c => c.charCodeAt(0));
              const audioBuffer = await audioContext.decodeAudioData(audioBytes.buffer);
              const source = audioContext.createBufferSource();
              source.buffer = audioBuffer;
              source.connect(audioContext.destination);
              source.start(0);
            } catch (e) {
              console.warn('Could not play commentary audio:', e);
            }
          })();
        }
      } catch {
      }
    };

    // Suppress error noise from React dev mode double mount
    ws.current.onerror = () => {};
    ws.current.onclose = () => {};
  }, [triggerHitFlash]);

  const sendCommand = useCallback((cmd: string) => {
    if (ws.current?.readyState === WebSocket.OPEN) {
      ws.current.send(cmd);
    }
    updatePosition(cmd);
  }, [updatePosition]);



  useEffect(() => {

    if (!player1Id || !player2Id) {
      router.push('/');
      return;
    }

    connectWebSocket();

    const initMatch = async () => {
      try {
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

        await fetch(`${API_URL}/api/matches/${newMatchId}/start`, { method: 'POST' });

        matchIdRef.current = newMatchId;

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
        setGameReady(true);
      }
    };

    initMatch();

    return () => {
      if (matchIdRef.current) {
        fetch(`${API_URL}/api/matches/${matchIdRef.current}/end`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ winnerPlayerId: player2Id }),
        }).catch(() => {});
      }
      if (ws.current?.readyState === WebSocket.OPEN) {
        ws.current.send('MATCH_END');
      }
      ws.current?.close();
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

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
            onClick={() => router.replace('/')}
            className="px-8 py-3 rounded-md font-black uppercase tracking-widest text-sm bg-blue-600 hover:bg-blue-500 text-white transition-colors"
          >
            Play Again
          </button>
          <button
            onClick={() => router.replace('/leaderboard')}
            className="px-8 py-3 rounded-md font-black uppercase tracking-widest text-sm bg-[#1e3d6b] hover:bg-[#2a5090] text-white transition-colors"
          >
            Leaderboard
          </button>
        </div>
      </div>
    );
  }

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

      <div className="flex flex-1 items-start">
        <ControlsIndicator
          player={1}
          sendCommand={sendCommand}
          matchIdRef={matchIdRef}
          wsRef={ws}
          setWinner={setWinner}
          playerId={player1Id}
          opponentId={player2Id}
          opponentName={player2Name}
        />
        <div className="flex-1 flex flex-col items-center gap-4 p-4">
          <Field
            car1Position={car1Pos}
            car2Position={car2Pos}
            car1Hit={car1Hit}
            car2Hit={car2Hit}
          />
          <CommentaryTicker lines={commentary} />
        </div>
        <ControlsIndicator
          player={2}
          sendCommand={sendCommand}
          matchIdRef={matchIdRef}
          wsRef={ws}
          setWinner={setWinner}
          playerId={player2Id}
          opponentId={player1Id}
          opponentName={player1Name}
        />
      </div>
      <Footer />
    </div>
  );
}