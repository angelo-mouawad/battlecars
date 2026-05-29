'use client';
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useWebSocket } from '@/app/hooks/useWebSocket';

interface ControlsIndicatorProps {
  player?: 1 | 2;
  sendCommand?: (cmd: string) => void;
  forfeitLabel?: string;
  onForfeit?: () => void;
}

const directionMap: Record<string, string> = {
  'W': 'UP', '\u2191': 'UP',
  'A': 'LEFT', '\u2190': 'LEFT',
  'S': 'DOWN', '\u2193': 'DOWN',
  'D': 'RIGHT', '\u2192': 'RIGHT',
};

const ControlsIndicator: React.FC<ControlsIndicatorProps> = (props) => {
  const player = props.player ?? 1;
  const isPlayer2 = player === 2;

  const keys = isPlayer2
    ? { up: '\u2191', left: '\u2190', down: '\u2193', right: '\u2192' }
    : { up: 'W', left: 'A', down: 'S', right: 'D' };

  const keyMap = isPlayer2
    ? { ArrowUp: '\u2191', ArrowLeft: '\u2190', ArrowDown: '\u2193', ArrowRight: '\u2192' }
    : { w: 'W', a: 'A', s: 'S', d: 'D' };

  const [active, setActive] = useState<string | null>(null);
  const activeRef = useRef<string | null>(null);

  const handleMessage = useCallback((_data: string) => {}, []);
  const { sendCommand: wsSendCommand } = useWebSocket(handleMessage);
  const sendCommand = props.sendCommand ?? wsSendCommand;

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

  return (
    <div className="bg-[#0d1b2e] text-white p-4 shadow-lg flex flex-col justify-start w-[200px]">
      {/* Controls section */}
      <div className="flex flex-col items-center gap-1">
        <h2 className="text-s font-black tracking-widest text-center mb-4">CONTROLS</h2>
        <h3 className="text-xs font-black text-gray-400 mb-3 text-center">MOVEMENT</h3>
        <Key label={keys.up} />
        <div className="flex gap-1">
          <Key label={keys.left} />
          <Key label={keys.down} />
          <Key label={keys.right} />
        </div>
      </div>

      {/* Forfeit button */}
      {props.onForfeit && (
        <button
          onClick={props.onForfeit}
          className="mt-6 w-full px-3 py-2 rounded text-xs font-black uppercase tracking-widest bg-red-900 hover:bg-red-700 text-white transition-colors"
        >
          {props.forfeitLabel ?? 'Forfeit'}
        </button>
      )}
    </div>
  );
};

export default ControlsIndicator;