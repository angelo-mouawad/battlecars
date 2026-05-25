import Lobby from '@/components/Lobby';
import Footer from '@/components/Footer';

export default function Home() {
  return (
    <div className="h-screen flex flex-col">
      <div className="flex-1 overflow-auto">
        <Lobby />
      </div>
      <Footer />
    </div>
  );
}