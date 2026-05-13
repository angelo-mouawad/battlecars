export default function Field() {
  return (
    <div className="relative w-full aspect-[19/6] max-w-6xl mx-auto bg-[#1a1a1a] border-8 border-[#2a2a2a] shadow-[0_0_40px_#000] overflow-hidden h-[80vh]">

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

    </div>
  );
}
