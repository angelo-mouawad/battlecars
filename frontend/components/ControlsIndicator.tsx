'use client';
import React, { useEffect, useState } from 'react';

interface ControlsIndicatorProps {
  player?: 1 | 2;
}

const directionMap: Record<string, string> = {
  'W': 'UP', '\u2191': 'UP',
  'A': 'LEFT', '\u2190': 'LEFT',
  'S': 'DOWN', '\u2193': 'DOWN',
  'D': 'RIGHT', '\u2192': 'RIGHT',
};

async function sendKey(player: 1 | 2, label: string) {
  const direction = directionMap[label];
  if (!direction) return;
  await fetch(`${process.env.NEXT_PUBLIC_API_URL}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(player + ':' + direction),
  });
}

const ControlsIndicator: React.FC<ControlsIndicatorProps> = ({ player = 1 }) => {
  const isPlayer2 = player === 2;

  const keys = isPlayer2
    ? { up: '\u2191', left: '\u2190', down: '\u2193', right: '\u2192' }
    : { up: 'W', left: 'A', down: 'S', right: 'D' };

  const keyMap = isPlayer2
    ? { ArrowUp: '\u2191', ArrowLeft: '\u2190', ArrowDown: '\u2193', ArrowRight: '\u2192' }
    : { w: 'W', a: 'A', s: 'S', d: 'D' };

  const [active, setActive] = useState<string | null>(null);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      const mapped = keyMap[e.key as keyof typeof keyMap];
      if (!mapped) return;
      setActive(mapped);
      sendKey(player, mapped);
    };
    const handleKeyUp = () => setActive(null);
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
        onClick={() => sendKey(player, label)}
        className={'w-8 h-8 flex items-center justify-center rounded text-sm font-bold cursor-pointer select-none transition-all ' + cls}
      >
        {label}
      </div>
    );
  };

  return (
    <div className="bg-[#0d1b2e] text-white p-4 shadow-lg w-fit h-full flex flex-col">
      <h2 className="text-s font-black tracking-widest text-center mb-33">CONTROLS</h2>
      <div className="flex flex-col items-center gap-1">
        <h3 className="text-xs font-black text-gray-400 mb-3 text-center">MOVEMENT</h3>
        <Key label={keys.up} />
        <div className="flex gap-1">
          <Key label={keys.left} />
          <Key label={keys.down} />
          <Key label={keys.right} />
        </div>
      </div>
    </div>
  );
};

export default ControlsIndicator;
