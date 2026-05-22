'use client';

interface ScoreboardProps {
  player1?: string;
  player2?: string;
  health1?: number;
  health2?: number;
}

const MAX_HEALTH = 50;

function HealthBar({ health, color, reverse = false }: { health: number; color: string; reverse?: boolean }) {
  const pct = Math.max(0, Math.min(100, (health / MAX_HEALTH) * 100));

  // Color shifts red as health drops
  const barColor = pct > 60 ? color : pct > 30 ? '#f59e0b' : '#ef4444';

  return (
    <div className="flex items-center gap-2 w-full" style={{ flexDirection: reverse ? 'row-reverse' : 'row' }}>
      <span className="text-xs font-black tabular-nums w-8" style={{ textAlign: reverse ? 'left' : 'right' }}>
        {health}
      </span>
      <div className="flex-1 h-[6px] bg-[#1a2e47] rounded-full overflow-hidden">
        <div
          className="h-full rounded-full transition-all duration-300"
          style={{
            width: `${pct}%`,
            backgroundColor: barColor,
            float: reverse ? 'right' : 'left',
          }}
        />
      </div>
    </div>
  );
}

export default function Scoreboard({
  player1 = 'Player 1',
  player2 = 'Player 2',
  health1 = MAX_HEALTH,
  health2 = MAX_HEALTH,
}: ScoreboardProps) {
  return (
    <div className="flex items-center w-full bg-[#0d1b2e] text-white shadow-2xl px-4 py-3 gap-4">

      {/* Left car avatar */}
      <div className="flex-shrink-0">
        <div className="w-12 h-12 rounded-full overflow-hidden bg-[#1a2e47] border border-[#2a4060] flex items-center justify-center">
          <img src="/img/bronco.png" alt="Car 1" className="w-full h-full object-cover" />
        </div>
      </div>

      {/* Left player info */}
      <div className="flex flex-col flex-1 gap-1">
        <div className="flex items-center gap-2">
          <span className="text-sm font-black uppercase tracking-widest text-[#0066ff] truncate">Bronco</span>
          <span className="text-xs text-gray-400 truncate">({player1})</span>
        </div>
        <HealthBar health={health1} color="#0066ff" />
      </div>

      {/* VS centre */}
      <div className="flex flex-col items-center flex-shrink-0 px-2">
        <span className="text-2xl font-black text-gray-600">VS</span>
        <span className="text-[10px] font-bold bg-[#1e3d6b] text-gray-300 px-2 py-[1px] rounded tracking-widest mt-1">
          Round 1
        </span>
      </div>

      {/* Right player info */}
      <div className="flex flex-col flex-1 gap-1 items-end">
        <div className="flex items-center gap-2 justify-end">
          <span className="text-xs text-gray-400 truncate">({player2})</span>
          <span className="text-sm font-black uppercase tracking-widest text-[#ff0000] truncate">Magnitude</span>
        </div>
        <HealthBar health={health2} color="#ff0000" reverse />
      </div>

      {/* Right car avatar */}
      <div className="flex-shrink-0">
        <div className="w-12 h-12 rounded-full overflow-hidden bg-[#1a2e47] border border-[#2a4060] flex items-center justify-center">
          <img src="/img/Magnitude.png" alt="Car 2" className="w-full h-full object-cover" />
        </div>
      </div>

    </div>
  );
}