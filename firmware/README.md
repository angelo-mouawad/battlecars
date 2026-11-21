# Firmware

| Folder | Board | What it does |
| --- | --- | --- |
| `hub/` | ESP32 DevKit | Joins WiFi, keeps a WebSocket open to the backend, turns commands into 433 MHz codes and sends car telemetry back |
| `car/` | Arduino Nano | Drives the motors, swings the hammer on car 1, detects hits and flips, and stops itself when commands stop arriving |
| `shared/` | both | `BattleCarsProtocol.h`, the radio codes both sides agree on |
| `wiring/` | | Wiring diagrams and notes on hardware risks |

## Flashing

Install [PlatformIO](https://platformio.org/install/cli), then:

```bash
cp hub/include/config.example.h hub/include/config.h   # fill in WiFi, backend address and HUB_TOKEN
pio run -d hub -t upload
pio run -d car -e car1 -t upload
pio run -d car -e car2 -t upload
```

Open the serial monitor with `pio device monitor -d hub` (115200 baud) or `pio device monitor -d car` (9600 baud) to see what each board is doing.
