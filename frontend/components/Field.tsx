'use client';

interface CarPosition {
  x: number;
  y: number;
}

interface FieldProps {
  car1Position?: CarPosition;
  car2Position?: CarPosition;
  car1Hit?: boolean;
  car2Hit?: boolean;
}

export default function Field({
  car1Position = { x: 25, y: 50 },
  car2Position = { x: 75, y: 50 },
  car1Hit = false,
  car2Hit = false,
}: FieldProps) {
  return (
    <div className="relative w-full aspect-video self-start flex-1 bg-[#1a1a1a] border-8 border-[#2a2a2a] shadow-[0_0_40px_#000] overflow-hidden mt-10">

      <div className="absolute top-0 left-0 w-16 h-16 border-r-4 border-b-4 border-[#ff4400] opacity-60" />
      <div className="absolute top-0 right-0 w-16 h-16 border-l-4 border-b-4 border-[#ff4400] opacity-60" />
      <div className="absolute bottom-0 left-0 w-16 h-16 border-r-4 border-t-4 border-[#ff4400] opacity-60" />
      <div className="absolute bottom-0 right-0 w-16 h-16 border-l-4 border-t-4 border-[#ff4400] opacity-60" />

      <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
        <div className="w-full h-[2px] bg-[#333]" />
      </div>
      <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
        <div className="w-[2px] h-full bg-[#333]" />
      </div>

      <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
        <div className="w-32 h-32 rounded-full border-2 border-[#333]" />
      </div>

      <div className="absolute inset-0 grid grid-cols-12 pointer-events-none opacity-10">
        {Array.from({ length: 12 }).map((_, i) => (
          <div key={i} className={i % 2 === 0 ? "bg-white" : "bg-transparent"} />
        ))}
      </div>

      <div className="absolute bottom-3 left-1/2 -translate-x-1/2 text-[10px] font-black uppercase tracking-[0.3em] text-[#ff4400] opacity-50 select-none">
        Danger Zone
      </div>

      {/* Car 1 — blue, WASD */}
      <div
        className="absolute transition-all duration-100 pointer-events-none"
        style={{
          left: `${car1Position.x}%`,
          top: `${car1Position.y}%`,
          transform: 'translate(-50%, -50%)',
        }}
      >
        {car1Hit && (
          <div className="absolute inset-0 w-10 h-10 rounded-full bg-red-500 opacity-60 animate-ping -translate-x-1 -translate-y-1" />
        )}
        <div className={`w-8 h-8 rounded-full border-4 flex items-center justify-center text-[10px] font-black shadow-lg
          ${car1Hit
            ? 'bg-red-600 border-red-300 text-white'
            : 'bg-blue-600 border-blue-300 text-white'
          }`}
        >
          P1
        </div>
      </div>

      {/* Car 2 — orange, arrow keys */}
      <div
        className="absolute transition-all duration-100 pointer-events-none"
        style={{
          left: `${car2Position.x}%`,
          top: `${car2Position.y}%`,
          transform: 'translate(-50%, -50%)',
        }}
      >
        {car2Hit && (
          <div className="absolute inset-0 w-10 h-10 rounded-full bg-red-500 opacity-60 animate-ping -translate-x-1 -translate-y-1" />
        )}
        <div className={`w-8 h-8 rounded-full border-4 flex items-center justify-center text-[10px] font-black shadow-lg
          ${car2Hit
            ? 'bg-red-600 border-red-300 text-white'
            : 'bg-orange-500 border-orange-300 text-white'
          }`}
        >
          P2
        </div>
      </div>

    </div>
  );
}