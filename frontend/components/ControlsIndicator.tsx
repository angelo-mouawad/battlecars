import React from 'react';

interface ControlsIndicatorProps {
  player?: 1 | 2;
}

const ControlsIndicator: React.FC<ControlsIndicatorProps> = ({ player }) => {
  return (
    <div className="bg-[#0d1b2e] text-white p-4 shadow-lg w-fit h-full flex flex-col">
      <h2 className="text-s font-black tracking-widest text-center mb-33">CONTROLS</h2>
      <div className="flex flex-col items-center gap-1">
        <h3 className='text-xs font-black text-gray-400 mb-3 text-center'>MOVEMENT</h3>
        <div className="bg-gray-700 w-8 h-8 flex items-center justify-center rounded text-sm font-bold">{player === 1 ? "W" : "↑"}</div>
        <div className="flex gap-1">
          <div className="bg-gray-700 w-8 h-8 flex items-center justify-center rounded text-sm font-bold">{player === 1 ? "A" : "←"}</div>
          <div className="bg-gray-700 w-8 h-8 flex items-center justify-center rounded text-sm font-bold">{player === 1 ? "S" : "↓"}</div>
          <div className="bg-gray-700 w-8 h-8 flex items-center justify-center rounded text-sm font-bold">{player === 1 ? "D" : "→"}</div>
        </div>
      </div>
    </div>
  );
};

export default ControlsIndicator;
