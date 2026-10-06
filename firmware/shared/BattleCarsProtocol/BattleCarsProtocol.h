#pragma once

#include <stdint.h>

namespace BattleCars {

const uint8_t RADIO_BIT_LENGTH = 24;

enum Command : uint8_t {
  CMD_NONE = 0,
  CMD_UP = 1,
  CMD_DOWN = 2,
  CMD_LEFT = 3,
  CMD_RIGHT = 4,
  CMD_STOP = 5,
  CMD_HAMMER = 6
};

inline unsigned long commandCode(uint8_t carId, Command command) {
  return carId * 10UL + command;
}

inline bool decodeCommand(unsigned long code, uint8_t carId, Command &command) {
  if (code < carId * 10UL + CMD_UP || code > carId * 10UL + CMD_HAMMER) {
    return false;
  }
  command = static_cast<Command>(code - carId * 10UL);
  return true;
}

inline unsigned long uprightCode(uint8_t carId) { return 81UL + carId * 10UL; }
inline unsigned long flippedCode(uint8_t carId) { return 82UL + carId * 10UL; }
inline unsigned long hitCode(uint8_t carId) { return 41UL + carId * 10UL; }

inline const char *commandName(Command command) {
  switch (command) {
    case CMD_UP: return "UP";
    case CMD_DOWN: return "DOWN";
    case CMD_LEFT: return "LEFT";
    case CMD_RIGHT: return "RIGHT";
    case CMD_STOP: return "STOP";
    case CMD_HAMMER: return "HAMMER";
    default: return "NONE";
  }
}

}
