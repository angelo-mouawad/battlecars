'use client';
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useWebSocket } from '@/app/hooks/useWebSocket';

interface ControlsIndicatorProps {
  player?: 1 | 2;
}

interface CarStatus {
  speed: string;
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

const ControlsIndicator: React.FC<ControlsIndicatorProps> = ({ player = 1 }) => {
  const isPlayer2 = player === 2;

  const keys = isPlayer2
    ? { up: '\u2191', left: '\u2190', down: '\u2193', right: '\u2192' }
    : { up: 'W', left: 'A', down: 'S', right: 'D' };

  const keyMap = isPlayer2
    ? { ArrowUp: '\u2191', ArrowLeft: '\u2190', ArrowDown: '\u2193', ArrowRight: '\u2192' }
    : { w: 'W', a: 'A', s: 'S', d: 'D' };

  const [active, setActive] = useState<string | null>(null);
  const activeRef = useRef<string | null>(null);
  const [carStatus, setCarStatus] = useState<CarStatus>({ speed: 'stopped', isMoving: false, hit: false, hitStrength: 'none', orientation: 'unknown' });

  const handleMessage = useCallback((data: string) => {
    try {
      const parsed = JSON.parse(data);
      if (
        'speed' in parsed ||
        'isMoving' in parsed ||
        'hit' in parsed ||
        'orientation' in parsed
      ) {
        setCarStatus({
          speed: parsed.speed ?? 'unknown',
          isMoving: parsed.isMoving ?? false,
          hit: parsed.hit ?? false,
          hitStrength: parsed.hitStrength ?? 'none',
          orientation: parsed.orientation ?? 'unknown',
        });
      }
    } catch {
    }
  }, []);

  const { sendCommand } = useWebSocket(handleMessage);

  const press = (label: string) => {
    const direction = directionMap[label];
    if (!direction) return;
    activeRef.current = label;
    setActive(label);
    sendCommand(`${player}:${direction}`);
  };

  const release = () => {
    if (activeRef.current === null) return;
    activeRef.current = null;
    setActive(null);
    sendCommand(`${player}:STOP`);
    setCarStatus(prev => ({ ...prev, isMoving: false, speed: 'stopped' }));
  };

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      const mapped = keyMap[e.key as keyof typeof keyMap];
      if (!mapped) return;
      e.preventDefault();
      press(mapped);
    };
    const handleKeyUp = (e: KeyboardEvent) => {
      const mapped = keyMap[e.key as keyof typeof keyMap];
      if (!mapped) return;
      if (activeRef.current === mapped) release();
    };
    window.addEventListener('keydown', handleKeyDown);
    window.addEventListener('keyup', handleKeyUp);
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      window.removeEventListener('keyup', handleKeyUp);
    };
  }, [player]);

  const Key: React.FC<{ label: string }> = ({ label }) => {
    const isPressed = active === label;
    const cls = isPressed ? 'bg-blue-500 scale-95' : 'bg-gray-700 hover:bg-gray-600';
    return (
      <div
        onMouseDown={() => press(label)}
        onMouseUp={() => { if (activeRef.current === label) release(); }}
        onMouseLeave={() => { if (activeRef.current === label) release(); }}
        onTouchStart={e => { e.preventDefault(); press(label); }}
        onTouchEnd={() => { if (activeRef.current === label) release(); }}
        className={'w-8 h-8 flex items-center justify-center rounded text-sm font-bold cursor-pointer select-none transition-all ' + cls}
      >
        {label}
      </div>
    );
  };

  const speedColor =
    carStatus?.speed === 'stopped' ? 'text-gray-400' :
    carStatus?.speed === 'slow'    ? 'text-yellow-400' :
    carStatus?.speed === 'fast'    ? 'text-green-400' :
    'text-gray-500';

  return (
    <div className="bg-[#0d1b2e] text-white p-4 shadow-lg flex flex-col justify-center w-[200px]">
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

      {/* Car Status */}
      <div className="mt-4 border-t border-gray-700 pt-3">
        <h3 className="text-xs font-black text-gray-400 mb-2 text-center tracking-widest">CAR STATUS</h3>
        <div className="flex flex-col gap-1 text-xs">
            <div className="flex justify-between gap-4">
              <span className="text-gray-400">Speed</span>
              <span className={`font-bold capitalize ${speedColor}`}>{carStatus.speed}</span>
            </div>
            <div className="flex justify-between gap-4">
              <span className="text-gray-400">Moving</span>
              <span className={`font-bold ${carStatus.isMoving ? 'text-green-400' : 'text-gray-500'}`}>
                {carStatus.isMoving ? 'Yes' : 'No'}
              </span>
            </div>
            <div className="flex justify-between gap-4">
              <span className="text-gray-400">Hit</span>
              <span className={`font-bold ${carStatus.hit ? 'text-red-400' : 'text-gray-500'}`}>
                {carStatus.hit ? carStatus.hitStrength : 'None'}
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
    </div>
  );
};

export default ControlsIndicator;