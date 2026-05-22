import { useEffect, useRef, useCallback } from 'react';

const WS_URL = process.env.NEXT_PUBLIC_WS_URL || 'wss://team-01-en-backend-itip-en-01.apps.okd.ucll.cloud/ws';

interface UseWebSocketOptions {
  onMessage?: (data: unknown) => void;
}

export function useWebSocket(options?: UseWebSocketOptions) {
  const ws = useRef<WebSocket | null>(null);
  // Keep a stable ref to the latest onMessage callback
  // so we never need to reconnect when it changes
  const onMessageRef = useRef(options?.onMessage);
  onMessageRef.current = options?.onMessage;

  useEffect(() => {
    ws.current = new WebSocket(WS_URL);

    ws.current.onopen = () => console.log('Connected to backend');
    ws.current.onclose = () => console.log('Disconnected');
    ws.current.onerror = (e) => console.error('WebSocket error', e);

    ws.current.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        onMessageRef.current?.(data);
      } catch {
        // Not JSON — raw string message (e.g. status messages), ignore
      }
    };

    return () => ws.current?.close();
  }, []); // Only connect once — never reconnect on prop changes

  const sendCommand = useCallback((cmd: string) => {
    if (ws.current?.readyState === WebSocket.OPEN) {
      ws.current.send(cmd);
    }
  }, []);

  return { sendCommand };
}