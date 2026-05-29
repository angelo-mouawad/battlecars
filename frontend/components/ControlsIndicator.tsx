'use client';
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useWebSocket } from '@/app/hooks/useWebSocket';

interface ControlsIndicatorProps {
  player?: 1 | 2;
  sendCommand?: (cmd: string) => void;
  matchIdRef?: React.MutableRefObject<number | null>;
  wsRef?: React.MutableRefObject<WebSocket | null>;
  setWinner?: (winner: { name: string; carId: number } | null) => void;
  playerId?: number | null;
  playerName?: string;
  opponentId?: number | null;
  opponentName?: string;
}

interface CarStatus {
  isMoving: boolean;
  hit: boolean;
  hitStrength: string;
  orientation: string;
}

const directionMap: Record<string, string> = {
  'W': 'UP', '\u2191': 'UP',
  'A': 'LEFT', '\u2190': 'LEFT',
  'S': 'DOWN', '\u2193': 'DOWN',
  'D': 'RIGHT', '\u2192': 'RIGHT',
};

const ControlsIndicator: React.FC<ControlsIndicatorProps> = ({
  player: playerProp,
  sendCommand: sendCommandProp,
  matchIdRef,
  wsRef,
  setWinner,
  playerId,
  playerName,
  opponentId,
  opponentName
}) => {
  const player = playerProp ?? 1;
  const isPlayer2 = player === 2;

  const keys = isPlayer2
    ? { up: '\u2191', left: '\u2190', down: '\u2193', right: '\u2192' }
    : { up: 'W', left: 'A', down: 'S', right: 'D' };

  const keyMap = isPlayer2
    ? { ArrowUp: '\u2191', ArrowLeft: '\u2190', ArrowDown: '\u2193', ArrowRight: '\u2192', p: 'HIT' }
    : { w: 'W', a: 'A', s: 'S', d: 'D', e: 'HIT' };
  // Hit button handler
  const triggerHit = () => {
    setCarStatus(prev => ({ ...prev, hit: true, hitStrength: 'hit' }));
    sendCommand(`${player}:HAMMER`);
  };

  // Forfeit logic
  const API_URL = process.env.NEXT_PUBLIC_API_URL || '';
  const handleForfeit = async () => {
    if (!matchIdRef || !wsRef || !setWinner || !playerId || !opponentId || !opponentName) return;
    const matchId = matchIdRef.current;
    if (!matchId) return;
    matchIdRef.current = null;
    try {
      await fetch(`${API_URL}/api/matches/${matchId}/end`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ winnerPlayerId: opponentId }),
      });
      if (wsRef.current?.readyState === WebSocket.OPEN) {
        wsRef.current.send('MATCH_END');
      }
      setWinner({ name: opponentName, carId: player === 1 ? 2 : 1 });
    } catch (err) {
      // eslint-disable-next-line no-console
      console.error('Could not end match:', err);
    }
  };

  const [active, setActive] = useState<Set<string>>(new Set());
  const [hitActive, setHitActive] = useState(false);
  const [carStatus, setCarStatus] = useState<CarStatus>({
    isMoving: false,
    hit: false,
    hitStrength: 'none',
    orientation: 'upright',
  });

  // Track if orientation has flipped
  useEffect(() => {
    if (carStatus.orientation === 'upright') return;
    if (carStatus.orientation !== 'flipped') {
      setCarStatus(prev => ({ ...prev, orientation: 'flipped' }));
    }
  }, [carStatus.orientation]);

  // Show 'Hit' for 3 seconds, then revert to 'Not Hit'
  useEffect(() => {
    if (carStatus.hit) {
      const timer = setTimeout(() => {
        setCarStatus(prev => ({ ...prev, hit: false, hitStrength: 'none' }));
      }, 3000);
      return () => clearTimeout(timer);
    }
  }, [carStatus.hit]);

  const handleMessage = useCallback((data: string) => {
    try {
      const parsed = JSON.parse(data);
      if (
        'isMoving' in parsed ||
        'hit' in parsed ||
        'orientation' in parsed
      ) {
        setCarStatus(prev => {
          let orientation = prev.orientation;
          if (parsed.orientation !== undefined) {
            if (parsed.orientation === 'upright') {
              orientation = 'upright';
            } else if (parsed.orientation !== 'upright') {
              orientation = 'flipped';
            }
          }
          return {
            isMoving: parsed.isMoving ?? false,
            hit: parsed.hit ?? false,
            hitStrength: parsed.hitStrength ?? 'none',
            orientation,
          };
        });
      }
    } catch {
    }
  }, []);

  const { sendCommand: wsSendCommand } = useWebSocket(handleMessage);
  const sendCommand = sendCommandProp ?? wsSendCommand;

  const press = (label: string) => {
    if (label === 'HIT') {
      setHitActive(true);
      triggerHit();
      return;
    }
    const direction = directionMap[label];
    if (!direction) return;
    setActive(prev => {
      const next = new Set(prev);
      next.add(label);
      return next;
    });
    sendCommand(`${player}:${direction}`);
    setCarStatus(prev => ({ ...prev, isMoving: true }));
  };

  const release = (label?: string) => {
    if (label === 'HIT') {
      setHitActive(false);
      return;
    }
    if (!label) return;
    setActive(prev => {
      const next = new Set(prev);
      next.delete(label);
      return next;
    });
    setTimeout(() => {
      setActive(prev => {
        if (prev.size === 0) {
          sendCommand(`${player}:STOP`);
          setCarStatus(cs => ({ ...cs, isMoving: false }));
        }
        return prev;
      });
    }, 0);
  };

  useEffect(() => {
    const down = new Set<string>();
    const handleKeyDown = (e: KeyboardEvent) => {
      const mapped = keyMap[e.key as keyof typeof keyMap];
      if (!mapped) return;
      if (down.has(mapped)) return; // prevent repeat
      down.add(mapped);
      e.preventDefault();
      press(mapped);
    };
    const handleKeyUp = (e: KeyboardEvent) => {
      const mapped = keyMap[e.key as keyof typeof keyMap];
      if (!mapped) return;
      down.delete(mapped);
      release(mapped);
    };
    window.addEventListener('keydown', handleKeyDown);
    window.addEventListener('keyup', handleKeyUp);
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      window.removeEventListener('keyup', handleKeyUp);
    };
    // eslint-disable-next-line
  }, [player]);

  const Key: React.FC<{ label: string }> = ({ label }) => {
    const isPressed = label === 'HIT' ? hitActive : active.has(label);
    const cls = isPressed ? 'bg-blue-500 scale-95' : 'bg-gray-700 hover:bg-gray-600';
    return (
      <div
        onMouseDown={e => { e.preventDefault(); press(label); }}
        onMouseUp={e => { e.preventDefault(); release(label); }}
        onMouseLeave={e => { e.preventDefault(); release(label); }}
        onTouchStart={e => { e.preventDefault(); press(label); }}
        onTouchEnd={e => { e.preventDefault(); release(label); }}
        className={'w-8 h-8 flex items-center justify-center rounded text-sm font-bold cursor-pointer select-none transition-all ' + cls}
        style={{ userSelect: 'none' }}
      >
        {label === 'HIT' ? (isPlayer2 ? 'P' : 'E') : label}
      </div>
    );
  };

  return (
    <div className="bg-[#0d1b2e] text-white p-4 shadow-lg flex flex-col justify-center w-50">
      <h2 className="text-s font-black tracking-widest text-center mb-4">CONTROLS</h2>
      <div className="flex flex-col items-center gap-1">
        <h3 className="text-xs font-black text-gray-400 mb-3 text-center">MOVEMENT</h3>
        <Key label={keys.up} />
        <div className="flex gap-1">
          <Key label={keys.left} />
          <Key label={keys.down} />
          <Key label={keys.right} />
        </div>
      </div>

      {/* Hit Button */}
      <h3 className="text-xs font-black text-gray-400 mb-1 text-center pt-3">HIT</h3>
      <div className="flex flex-col items-center">
        <Key label="HIT" />
      </div>

      {/* Car Status */}
      <div className="mt-4 border-t border-gray-700 pt-3">
        <h3 className="text-xs font-black text-gray-400 mb-2 text-center tracking-widest">CAR STATUS</h3>
        <div className="flex flex-col gap-1 text-xs">
          <div className="flex justify-between gap-4">
            <span className="text-gray-400">Moving</span>
            <span className={`font-bold ${carStatus.isMoving ? 'text-green-400' : 'text-gray-500'}`}>
              {carStatus.isMoving ? 'Yes' : 'No'}
            </span>
          </div>
          <div className="flex justify-between gap-4">
            <span className="text-gray-400">Hit</span>
            <span className={`font-bold ${carStatus.hit ? 'text-red-400' : 'text-gray-500'}`}>
              {carStatus.hit ? 'Hit' : 'Not Hit'}
            </span>
          </div>
          <div className="flex justify-between gap-4">
            <span className="text-gray-400">Orientation</span>
            <span className={`font-bold capitalize ${carStatus.orientation === 'upright' ? 'text-green-400' : 'text-orange-400'}`}>
              {carStatus.orientation}
            </span>
          </div>
        </div>
      </div>

      {/* Forfeit Button */}
      {matchIdRef && wsRef && setWinner && playerId && opponentId && opponentName && (
        <button
          onClick={handleForfeit}
          className="mt-4 w-full px-4 py-2 rounded text-xs font-black uppercase tracking-widest bg-red-900 hover:bg-red-700 text-white transition-colors"
        >
          Forfeit
        </button>
      )}
    </div>
  );
};

export default ControlsIndicator;