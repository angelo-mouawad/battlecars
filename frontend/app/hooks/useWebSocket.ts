import { useEffect, useRef } from 'react';

const WS_URL = process.env.NEXT_PUBLIC_WS_URL || 'wss://team-01-en-backend-itip-en-01.apps.okd.ucll.cloud/ws';

export function useWebSocket(onMessage?: (data: string) => void) {
  const ws = useRef<WebSocket | null>(null);
  const onMessageRef = useRef(onMessage);

  useEffect(() => {
    onMessageRef.current = onMessage;
  });

  useEffect(() => {
    ws.current = new WebSocket(WS_URL);
    ws.current.onopen = () => console.log('Connected to backend');
    ws.current.onclose = () => console.log('Disconnected');
    ws.current.onerror = (e) => console.error('WebSocket error', e);
    ws.current.onmessage = (e) => onMessageRef.current?.(e.data);

    return () => ws.current?.close();
  }, []);

  const sendCommand = (cmd: string) => {
    if (ws.current?.readyState === WebSocket.OPEN) {
      ws.current.send(cmd);
    }
  };

  return { sendCommand };
}