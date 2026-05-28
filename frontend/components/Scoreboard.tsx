interface ScoreboardProps {
  player1?: string;
  player2?: string;
  health1?: number;
  health2?: number;
}

export default function Scoreboard({ player1 = "Unknown", player2 = "Unknown", health1 = 50, health2 = 50 }: ScoreboardProps) {
  return (
    <div className="flex items-center w-full bg-[#0d1b2e] text-white shadow-2xl">
      <div className="flex-shrink-0 px-3 py-2">
        <div className="w-14 h-14 rounded-full overflow-hidden bg-[#1a2e47] border border-[#2a4060] flex items-center justify-center">
          <img src="/img/bronco.png" alt="Team 1 logo" className="w-full h-full object-cover" />
        </div>
      </div>

      <div className="flex flex-col flex-1">
        <div className="flex items-center justify-center gap-4 py-2 px-4">
          <div className="flex flex-col items-end flex-1">
            <span className="text-base font-black uppercase tracking-widest">
              Bronco
            </span>
            <span className="text-xs text-gray-400 tracking-wider">{player1}</span>
          </div>
            <span className="text-2xl font-black w-12 text-center leading-none">{health1}</span>
            <span className="text-2xl font-black w-12 text-center leading-none">{health2}</span>
          <div className="flex flex-col items-start flex-1">
            <span className="text-base font-black uppercase tracking-widest">
              Magnitude
            </span>
            <span className="text-xs text-gray-400 tracking-wider">{player2}</span>
          </div>
        </div>

        <div className="flex items-center gap-0 pb-2 px-4">
          <div className="flex-1 h-[6px] bg-gray-800 rounded-sm mr-2"><div className="h-full bg-[#0066ff] rounded-sm transition-all" style={{ width: `${(health1 / 50) * 100}%` }} /></div>
          <div className="text-[11px] font-bold bg-[#1e3d6b] text-gray-300 px-3 py-[2px] rounded-sm tracking-widest">
            Round 1
          </div>
          <div className="flex-1 h-[6px] bg-gray-800 rounded-sm ml-2"><div className="h-full bg-[#ff0000] rounded-sm transition-all" style={{ width: `${(health2 / 50) * 100}%` }} /></div>
        </div>
      </div>

      <div className="flex-shrink-0 px-3 py-2">
        <div className="w-14 h-14 rounded-full overflow-hidden bg-[#1a2e47] border border-[#2a4060] flex items-center justify-center">
          <img src="/img/Magnitude.png" alt="Team 2 logo" className="w-full h-full object-cover" />
        </div>
      </div>
    </div>
  );
}