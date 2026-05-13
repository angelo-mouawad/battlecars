'use client';
import { useWebSocket } from './hooks/useWebSocket';

export default function Home() {
  const { sendCommand } = useWebSocket();

  return (
    <main style={{ display: 'flex', gap: 60, padding: 40, background: '#111', minHeight: '100vh', color: 'white', justifyContent: 'center' }}>
      
      {/* Car 1 */}
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 10 }}>
        <h2>Car 1</h2>
        <button onClick={() => sendCommand('1:UP')}>UP</button>
        <div style={{ display: 'flex', gap: 10 }}>
          <button onClick={() => sendCommand('1:LEFT')}>LEFT</button>
          <button onClick={() => sendCommand('1:DOWN')}>DOWN</button>
          <button onClick={() => sendCommand('1:RIGHT')}>RIGHT</button>
        </div>
      </div>

      {/* Car 2 */}
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 10 }}>
        <h2>Car 2</h2>
        <button onClick={() => sendCommand('2:UP')}>UP</button>
        <div style={{ display: 'flex', gap: 10 }}>
          <button onClick={() => sendCommand('2:LEFT')}>LEFT</button>
          <button onClick={() => sendCommand('2:DOWN')}>DOWN</button>
          <button onClick={() => sendCommand('2:RIGHT')}>RIGHT</button>
        </div>
      </div>

    </main>
  );
}