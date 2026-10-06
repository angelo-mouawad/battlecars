'use client';
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';

interface ControlsIndicatorProps {
  player?: 1 | 2;
  sendCommand?: (cmd: string) => void;
  forfeitLabel?: string;
  onForfeit?: () => void;
}

interface CarStatus {
  isMoving: boolean;
  hit: boolean;
  orientation: string;
}

const directionMap: Record<string, string> = {
  'W': 'UP', '\u2191': 'UP',
  'A': 'LEFT', '\u2190': 'LEFT',
  'S': 'DOWN', '\u2193': 'DOWN',
  'D': 'RIGHT', '\u2192': 'RIGHT',
};

const noop = () => {};

interface KeyProps {
  label: string;
  text: string;
  pressed: boolean;
  onPress: (label: string) => void;
  onRelease: (label: string) => void;
}

function Key({ label, text, pressed, onPress, onRelease }: KeyProps) {
  const cls = pressed ? 'bg-blue-500 scale-95' : 'bg-gray-700 hover:bg-gray-600';
  return (
    <div
      onMouseDown={e => { e.preventDefault(); onPress(label); }}
      onMouseUp={e => { e.preventDefault(); onRelease(label); }}
      onMouseLeave={e => { e.preventDefault(); if (pressed) onRelease(label); }}
      onTouchStart={e => { e.preventDefault(); onPress(label); }}
      onTouchEnd={e => { e.preventDefault(); onRelease(label); }}
      className={'w-8 h-8 flex items-center justify-center rounded text-sm font-bold cursor-pointer select-none transition-all ' + cls}
      style={{ userSelect: 'none' }}
    >
      {text}
    </div>
  );
}

const ControlsIndicator: React.FC<ControlsIndicatorProps> = (props) => {
  const player = props.player ?? 1;
  const isPlayer2 = player === 2;
  const hasHammer = player === 1;
  const sendCommand = props.sendCommand ?? noop;

  const keys = isPlayer2
    ? { up: '\u2191', left: '\u2190', down: '\u2193', right: '\u2192' }
    : { up: 'W', left: 'A', down: 'S', right: 'D' };

  const keyMap = useMemo((): Record<string, string> => (isPlayer2
    ? { ArrowUp: '\u2191', ArrowLeft: '\u2190', ArrowDown: '\u2193', ArrowRight: '\u2192' }
    : { w: 'W', a: 'A', s: 'S', d: 'D', e: 'HIT' }), [isPlayer2]);

  const [active, setActive] = useState<Set<string>>(new Set());
  const [hitActive, setHitActive] = useState(false);
  const [carStatus, setCarStatus] = useState<CarStatus>({
    isMoving: false,
    hit: false,
    orientation: 'upright',
  });
  const pressedRef = useRef<Set<string>>(new Set());

  useEffect(() => {
    if (!carStatus.hit) return;
    const timer = setTimeout(() => setCarStatus(prev => ({ ...prev, hit: false })), 3000);
    return () => clearTimeout(timer);
  }, [carStatus.hit]);

  const press = useCallback((label: string) => {
    if (label === 'HIT') {
      if (!hasHammer) return;
      setHitActive(true);
      sendCommand(`${player}:HAMMER`);
      return;
    }
    const direction = directionMap[label];
    if (!direction || pressedRef.current.has(label)) return;
    pressedRef.current.add(label);
    setActive(new Set(pressedRef.current));
    sendCommand(`${player}:${direction}`);
    setCarStatus(prev => ({ ...prev, isMoving: true }));
  }, [player, hasHammer, sendCommand]);

  const release = useCallback((label: string) => {
    if (label === 'HIT') {
      setHitActive(false);
      return;
    }
    if (!pressedRef.current.delete(label)) return;
    setActive(new Set(pressedRef.current));
    if (pressedRef.current.size === 0) {
      sendCommand(`${player}:STOP`);
      setCarStatus(prev => ({ ...prev, isMoving: false }));
    }
  }, [player, sendCommand]);

  useEffect(() => {
    const handler = (e: Event) => {
      const parsed = (e as CustomEvent<Partial<CarStatus> & { carId?: number }>).detail;
      if (!parsed || typeof parsed !== 'object') return;
      if (parsed.carId !== undefined && parsed.carId !== player) return;
      setCarStatus(prev => ({
        isMoving: parsed.isMoving ?? prev.isMoving,
        hit: parsed.hit ?? prev.hit,
        orientation: parsed.orientation ?? prev.orientation,
      }));
    };
    const eventName = `car-status-${player}`;
    window.addEventListener(eventName, handler);
    return () => window.removeEventListener(eventName, handler);
  }, [player]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      const mapped = keyMap[e.key];
      if (!mapped || e.repeat) return;
      e.preventDefault();
      press(mapped);
    };
    const handleKeyUp = (e: KeyboardEvent) => {
      const mapped = keyMap[e.key];
      if (!mapped) return;
      release(mapped);
    };
    window.addEventListener('keydown', handleKeyDown);
    window.addEventListener('keyup', handleKeyUp);
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      window.removeEventListener('keyup', handleKeyUp);
    };
  }, [keyMap, press, release]);

  const renderKey = (label: string) => (
    <Key
      label={label}
      text={label === 'HIT' ? 'E' : label}
      pressed={label === 'HIT' ? hitActive : active.has(label)}
      onPress={press}
      onRelease={release}
    />
  );

  return (
    <div className="bg-[#0d1b2e] text-white p-4 shadow-lg flex flex-col justify-between w-[200px] min-h-[200px]">
      {/* Movement */}
      <div className="flex flex-col items-center gap-1">
        <h2 className="text-s font-black tracking-widest text-center mb-4">CONTROLS</h2>
        <h3 className="text-xs font-black text-gray-400 mb-3 text-center">MOVEMENT</h3>
        {renderKey(keys.up)}
        <div className="flex gap-1">
          {renderKey(keys.left)}
          {renderKey(keys.down)}
          {renderKey(keys.right)}
        </div>
      </div>

      {hasHammer && (
        <div className="flex flex-col items-center mt-3">
          <h3 className="text-xs font-black text-gray-400 mb-1 text-center">HAMMER</h3>
          {renderKey('HIT')}
        </div>
      )}

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

      {/* Forfeit button */}
      {props.onForfeit && (
        <button
          onClick={props.onForfeit}
          className="mt-4 w-full px-3 py-2 rounded text-xs font-black uppercase tracking-widest bg-red-900 hover:bg-red-700 text-white transition-colors"
        >
          {props.forfeitLabel ?? 'Forfeit'}
        </button>
      )}
    </div>
  );
};

export default ControlsIndicator;