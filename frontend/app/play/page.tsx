'use client';

import { useEffect, useRef, useState, useCallback } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import ControlsIndicator from '@/components/ControlsIndicator';
import CommentaryTicker from '@/components/CommentaryTicker';
import Field from '@/components/Field';
import Footer from '@/components/Footer';
import Scoreboard from '@/components/Scoreboard';
import { apiFetch, apiFetchNow, getApiToken } from '@/services/api';

const WS_URL = process.env.NEXT_PUBLIC_WS_URL || 'ws://localhost:8080/ws';
const MAX_RECONNECT_DELAY_MS = 10000;
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

async function errorMessage(res: Response, fallback: string): Promise<string> {
  try {
    const body = await res.json();
    return typeof body?.error === 'string' ? body.error : fallback;
  } catch {
    return fallback;
  }
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
  const [socketOpen, setSocketOpen] = useState(false);
  const [hubConnected, setHubConnected] = useState<boolean | null>(null);
  const [matchError, setMatchError] = useState<string | null>(null);

  const [car1Pos, setCar1Pos] = useState<CarPosition>({ x: 25, y: 50 });
  const [car2Pos, setCar2Pos] = useState<CarPosition>({ x: 75, y: 50 });

  const [car1Hit, setCar1Hit] = useState(false);
  const [car2Hit, setCar2Hit] = useState(false);

  const ws = useRef<WebSocket | null>(null);
  const matchIdRef = useRef<number | null>(null);
  const audioContextRef = useRef<AudioContext | null>(null);
  const reconnectTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const reconnectAttemptsRef = useRef(0);
  const unmountedRef = useRef(false);

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

  const connectWebSocket = useCallback(function connect() {
    if (unmountedRef.current) return;

    const scheduleReconnect = () => {
      if (unmountedRef.current) return;
      const delay = Math.min(1000 * 2 ** reconnectAttemptsRef.current, MAX_RECONNECT_DELAY_MS);
      reconnectAttemptsRef.current += 1;
      reconnectTimerRef.current = setTimeout(connect, delay);
    };

    getApiToken().then(token => {
      // Skip if unmounted meanwhile, or if another connect already opened a socket.
      if (unmountedRef.current) return;
      if (ws.current && ws.current.readyState <= WebSocket.OPEN) return;
      let socket: WebSocket;
      try {
        socket = new WebSocket(`${WS_URL}?token=${encodeURIComponent(token)}`);
      } catch (err) {
        console.error('Could not open WebSocket:', err);
        scheduleReconnect();
        return;
      }
      ws.current = socket;

      ws.current.onopen = () => {
        reconnectAttemptsRef.current = 0;
        setSocketOpen(true);
        if (matchIdRef.current) {
          ws.current?.send(`MATCH_START:${matchIdRef.current}`);
        }
      };

      ws.current.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);

          if (data.type === 'HUB_STATUS') {
            setHubConnected(Boolean(data.connected));
          } else if (data.type === 'HIT_RESULT') {
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
          } else if (data.carId !== undefined) {
            // Raw ESP32 data, dispatch to the correct ControlsIndicator
            window.dispatchEvent(new CustomEvent(`car-status-${data.carId}`, { detail: data }));
          }
        } catch {
          // Not JSON, ignore
        }
      };

      ws.current.onerror = () => {};
      ws.current.onclose = () => {
        if (ws.current !== socket) return;
        setSocketOpen(false);
        scheduleReconnect();
      };
    }).catch(err => {
      console.error('Could not get a token for the WebSocket:', err);
      scheduleReconnect();
    });
  }, [triggerHitFlash]);

  const sendCommand = useCallback((cmd: string) => {
    if (ws.current?.readyState === WebSocket.OPEN) {
      ws.current.send(cmd);
    }
    updatePosition(cmd);
  }, [updatePosition]);

  const forfeitMatch = useCallback(async (winnerPlayerId: number, winnerName: string, winnerCarId: number) => {
    if (!matchIdRef.current) return;
    const savedId = matchIdRef.current;
    matchIdRef.current = null;
    try {
      const res = await apiFetch(`/api/matches/${savedId}/end`, {
        method: 'POST',
        body: JSON.stringify({ winnerPlayerId }),
      });
      if (!res.ok) throw new Error(await errorMessage(res, 'Could not end the match'));
      if (ws.current?.readyState === WebSocket.OPEN) {
        ws.current.send('MATCH_END');
      }
      setWinner({ name: winnerName, carId: winnerCarId });
    } catch (err) {
      console.error('Could not end match:', err);
    }
  }, []);

  // Cleanup on page refresh
  useEffect(() => {
    const handleUnload = () => {
      if (matchIdRef.current) {
        apiFetchNow(`/api/matches/${matchIdRef.current}/end`, {
          method: 'POST',
          body: JSON.stringify({ winnerPlayerId: player2Id }),
          keepalive: true,
        }).catch(() => {});
      }
    };
    window.addEventListener('beforeunload', handleUnload);
    return () => window.removeEventListener('beforeunload', handleUnload);
  }, [player2Id]);

  useEffect(() => {
    if (!player1Id || !player2Id) {
      router.push('/');
      return;
    }

    unmountedRef.current = false;
    connectWebSocket();

    const initMatch = async () => {
      try {
        const createRes = await apiFetch('/api/matches', {
          method: 'POST',
          body: JSON.stringify({
            player1Id: player1Id,
            car1Id: 1,
            player2Id: player2Id,
            car2Id: 2,
          }),
        });

        if (!createRes.ok) throw new Error(await errorMessage(createRes, 'Could not create the match'));
        const created = await createRes.json();
        const newMatchId: number = created.matchId;

        const startRes = await apiFetch(`/api/matches/${newMatchId}/start`, { method: 'POST' });
        if (!startRes.ok) throw new Error(await errorMessage(startRes, 'Could not start the match'));

        matchIdRef.current = newMatchId;
        if (ws.current?.readyState === WebSocket.OPEN) {
          ws.current.send(`MATCH_START:${newMatchId}`);
        }
        setGameReady(true);
      } catch (err) {
        console.error('Could not init match:', err);
        setMatchError(err instanceof Error && err.message ? err.message : 'Could not reach the server');
      }
    };

    initMatch();

    return () => {
      unmountedRef.current = true;
      if (reconnectTimerRef.current) clearTimeout(reconnectTimerRef.current);
      if (matchIdRef.current) {
        apiFetch(`/api/matches/${matchIdRef.current}/end`, {
          method: 'POST',
          body: JSON.stringify({ winnerPlayerId: player2Id }),
        }).catch(() => {});
      }
      if (ws.current?.readyState === WebSocket.OPEN) {
        ws.current.send('MATCH_END');
      }
      ws.current?.close();
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  if (matchError) {
    return (
      <div className="h-screen flex flex-col items-center justify-center bg-[#0d1b2e] text-white gap-6 px-6 text-center">
        <h1 className="text-2xl font-black uppercase tracking-widest text-red-400">Match could not start</h1>
        <p className="text-gray-300 text-sm max-w-md">{matchError}</p>
        <button
          onClick={() => router.replace('/')}
          className="px-8 py-3 rounded-md font-black uppercase tracking-widest text-sm bg-blue-600 hover:bg-blue-500 text-white transition-colors"
        >
          Back to lobby
        </button>
      </div>
    );
  }

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

      {gameReady && (!socketOpen || hubConnected === false) && (
        <div className="bg-red-600/90 text-white text-center text-xs font-black uppercase tracking-widest py-2">
          {!socketOpen ? 'Connection to the server lost, reconnecting...' : 'Cars are offline, check the hub'}
        </div>
      )}

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
          forfeitLabel={`${player1Name} Forfeits`}
          onForfeit={() => forfeitMatch(player2Id!, player2Name, 2)}
        />
        <Field
          car1Position={car1Pos}
          car2Position={car2Pos}
          car1Hit={car1Hit}
          car2Hit={car2Hit}
        />
        <ControlsIndicator
          player={2}
          sendCommand={sendCommand}
          forfeitLabel={`${player2Name} Forfeits`}
          onForfeit={() => forfeitMatch(player1Id!, player1Name, 1)}
        />
      </div>

      <CommentaryTicker lines={commentary} />

      <Footer />
    </div>
  );
}