import ControlsIndicator from "@/components/ControlsIndicator";
import Field from "@/components/Field";
import Scoreboard from "@/components/Scoreboard";

export default function Home() {
  const { sendCommand } = useWebSocket();

  return (
    <>
      <Scoreboard />
      <div className="flex h-[80vh]">
        <ControlsIndicator player={1}/>
        <Field />
        <ControlsIndicator player={2}/>
      </div>
    </>
  );
}