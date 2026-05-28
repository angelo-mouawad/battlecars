'use client';

import { useEffect, useRef } from 'react';

interface CommentaryTickerProps {
  lines: string[];
}

export default function CommentaryTicker({ lines }: CommentaryTickerProps) {
  const bottomRef = useRef<HTMLDivElement>(null);

  // Scroll to bottom whenever a new line arrives
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [lines]);

  if (lines.length === 0) return null;

  return (
    <div className="w-full bg-[#080f1a] border-t border-[#1e3d6b] px-4 py-2 flex items-center gap-3 min-h-[48px]">
      {/* Mic icon */}
      <div className="flex-shrink-0 w-7 h-7 rounded-full bg-[#1e3d6b] flex items-center justify-center">
        <svg
          className="w-3.5 h-3.5 text-blue-400"
          fill="currentColor"
          viewBox="0 0 24 24"
        >
          <path d="M12 1a4 4 0 0 1 4 4v6a4 4 0 0 1-8 0V5a4 4 0 0 1 4-4zm6.5 9a1 1 0 0 1 1 1 7.5 7.5 0 0 1-15 0 1 1 0 0 1 2 0 5.5 5.5 0 0 0 11 0 1 1 0 0 1 1-1zM11 20h2v2h-2v-2z" />
        </svg>
      </div>

      {/* Commentary lines — newest at bottom, max 3 visible */}
      <div className="flex-1 overflow-hidden flex flex-col gap-0.5">
        {lines.slice(-3).map((line, i, arr) => {
          const isNewest = i === arr.length - 1;
          return (
            <p
              key={i}
              className={`text-sm leading-snug tracking-wide truncate transition-opacity duration-500 ${
                isNewest
                  ? 'text-white font-semibold opacity-100'
                  : 'text-gray-500 opacity-60 text-xs'
              }`}
            >
              {isNewest && (
                <span className="inline-block w-1.5 h-1.5 rounded-full bg-blue-400 mr-2 mb-0.5 animate-pulse" />
              )}
              {line}
            </p>
          );
        })}
        <div ref={bottomRef} />
      </div>
    </div>
  );
}
