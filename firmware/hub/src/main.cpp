#include <Arduino.h>
#include <WiFi.h>
#include <WebSocketsClient.h>
#include <RCSwitch.h>
#include <Wire.h>
#include <SSD1306Ascii.h>
#include <SSD1306AsciiWire.h>
#include <ArduinoJson.h>
#include <esp_bt.h>
#include <BattleCarsProtocol.h>

#if __has_include("config.h")
#include "config.h"
#else
#error "Missing include/config.h. Copy include/config.example.h to include/config.h and fill it in."
#endif

#ifndef WS_USE_SSL
#define WS_USE_SSL 1
#endif

using namespace BattleCars;

const uint8_t TX_PIN_CAR1 = 15;
const uint8_t TX_PIN_CAR2 = 4;
const uint8_t RX_PIN = 2;

const uint8_t COMMAND_REPEATS = 3;
const uint8_t STOP_REPEATS = 5;
const unsigned long ORIENTATION_COOLDOWN_MS = 1000;
const unsigned long HIT_COOLDOWN_MS = 500;
const unsigned long WIFI_RETRY_MS = 5000;
const unsigned long WIFI_RESTART_AFTER_MS = 60000;
const size_t MAX_COMMAND_LENGTH = 16;

struct CarState {
  unsigned long lastOrientationAt;
  unsigned long lastHitAt;
  bool flipped;
};

RCSwitch radioTX1;
RCSwitch radioTX2;
RCSwitch radioRX;
WebSocketsClient webSocket;
SSD1306AsciiWire display;

CarState cars[2] = {{0, 0, false}, {0, 0, false}};
bool backendConnected = false;
unsigned long lastWifiAttempt = 0;
unsigned long wifiLostAt = 0;

void show(const char *line1, const char *line2 = "") {
  display.clear();
  display.println(line1);
  if (line2[0] != '\0') {
    display.println(line2);
  }
}

RCSwitch &radioFor(uint8_t carId) {
  return carId == 1 ? radioTX1 : radioTX2;
}

void sendToCar(uint8_t carId, Command command) {
  unsigned long code = commandCode(carId, command);
  uint8_t repeats = command == CMD_STOP ? STOP_REPEATS : COMMAND_REPEATS;
  for (uint8_t i = 0; i < repeats; i++) {
    radioFor(carId).send(code, RADIO_BIT_LENGTH);
  }

  char label[24];
  snprintf(label, sizeof(label), "Car%u %s", carId, commandName(command));
  show("CMD:", label);
  Serial.printf("Sent RC code %lu (%s)\n", code, label);
}

void stopAllCars() {
  sendToCar(1, CMD_STOP);
  sendToCar(2, CMD_STOP);
}

bool parseCommand(const char *text, uint8_t &carId, Command &command) {
  if ((text[0] != '1' && text[0] != '2') || text[1] != ':') {
    return false;
  }
  carId = text[0] - '0';
  const char *name = text + 2;

  if (strcmp(name, "UP") == 0) command = CMD_UP;
  else if (strcmp(name, "DOWN") == 0) command = CMD_DOWN;
  else if (strcmp(name, "LEFT") == 0) command = CMD_LEFT;
  else if (strcmp(name, "RIGHT") == 0) command = CMD_RIGHT;
  else if (strcmp(name, "STOP") == 0) command = CMD_STOP;
  else if (strcmp(name, "HAMMER") == 0) command = CMD_HAMMER;
  else return false;

  return !(command == CMD_HAMMER && carId != 1);
}

void handleBackendText(const uint8_t *payload, size_t length) {
  char text[MAX_COMMAND_LENGTH + 1];
  size_t used = 0;
  for (size_t i = 0; i < length && used < MAX_COMMAND_LENGTH; i++) {
    char c = (char)payload[i];
    if (c != '"' && c != ' ' && c != '\r' && c != '\n' && c != '\t') {
      text[used++] = c;
    }
  }
  text[used] = '\0';

  uint8_t carId;
  Command command;
  if (length > MAX_COMMAND_LENGTH + 2 || !parseCommand(text, carId, command)) {
    Serial.printf("Ignoring unknown command (%u bytes)\n", (unsigned)length);
    show("Unknown CMD", text);
    return;
  }

  sendToCar(carId, command);
}

void sendCarData(uint8_t carId, bool hit) {
  JsonDocument doc;
  doc["carId"] = carId;
  doc["hit"] = hit;
  doc["orientation"] = cars[carId - 1].flipped ? "flipped" : "upright";

  char json[96];
  size_t written = serializeJson(doc, json, sizeof(json));
  if (written == 0 || written >= sizeof(json)) {
    Serial.println("Could not serialize car data");
    return;
  }

  if (!backendConnected) {
    Serial.printf("Backend offline, dropped: %s\n", json);
    return;
  }

  webSocket.sendTXT(json);
  Serial.printf("Sent: %s\n", json);
}

void webSocketEvent(WStype_t type, uint8_t *payload, size_t length) {
  switch (type) {
    case WStype_CONNECTED:
      backendConnected = true;
      Serial.println("Connected to backend");
      show("Backend", "connected");
      break;

    case WStype_DISCONNECTED:
      if (backendConnected) {
        Serial.println("Backend connection lost, stopping both cars");
        stopAllCars();
      }
      backendConnected = false;
      show("Backend lost", "Reconnecting");
      break;

    case WStype_TEXT:
      handleBackendText(payload, length);
      break;

    case WStype_ERROR:
      Serial.println("WebSocket error");
      break;

    default:
      break;
  }
}

void handleRadio() {
  if (!radioRX.available()) {
    return;
  }

  unsigned long code = radioRX.getReceivedValue();
  radioRX.resetAvailable();
  unsigned long now = millis();

  for (uint8_t carId = 1; carId <= 2; carId++) {
    CarState &car = cars[carId - 1];

    if (code == uprightCode(carId) || code == flippedCode(carId)) {
      car.flipped = code == flippedCode(carId);
      if (now - car.lastOrientationAt > ORIENTATION_COOLDOWN_MS) {
        car.lastOrientationAt = now;
        sendCarData(carId, false);
      }
      return;
    }

    if (code == hitCode(carId)) {
      if (now - car.lastHitAt > HIT_COOLDOWN_MS) {
        car.lastHitAt = now;
        sendCarData(carId, true);
      }
      return;
    }
  }
}

void keepWifiAlive() {
  if (WiFi.status() == WL_CONNECTED) {
    wifiLostAt = 0;
    return;
  }

  unsigned long now = millis();
  if (wifiLostAt == 0) {
    wifiLostAt = now;
    Serial.println("WiFi lost");
    show("WiFi lost", "Reconnecting");
  }

  if (now - lastWifiAttempt >= WIFI_RETRY_MS) {
    lastWifiAttempt = now;
    WiFi.reconnect();
  }

  if (now - wifiLostAt >= WIFI_RESTART_AFTER_MS) {
    Serial.println("WiFi did not come back, restarting");
    ESP.restart();
  }
}

void connectWifi() {
  WiFi.mode(WIFI_STA);
  WiFi.setAutoReconnect(true);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  WiFi.setTxPower(WIFI_POWER_8_5dBm);
  show("Connecting", "WiFi...");

  unsigned long startedAt = millis();
  while (WiFi.status() != WL_CONNECTED) {
    if (millis() - startedAt >= WIFI_RESTART_AFTER_MS) {
      Serial.println("\nCould not join WiFi, restarting");
      show("WiFi failed", "Restarting");
      delay(1000);
      ESP.restart();
    }
    delay(500);
    Serial.print(".");
  }

  Serial.printf("\nWiFi connected: %s\n", WiFi.localIP().toString().c_str());
  show("WiFi OK!", WiFi.localIP().toString().c_str());
  delay(1000);
}

void setup() {
  Serial.begin(115200);

  setCpuFrequencyMhz(80);
  btStop();
  esp_bt_controller_disable();

  radioTX1.enableTransmit(TX_PIN_CAR1);
  radioTX2.enableTransmit(TX_PIN_CAR2);
  radioRX.enableReceive(digitalPinToInterrupt(RX_PIN));
  radioTX1.setRepeatTransmit(2);
  radioTX2.setRepeatTransmit(2);

  Wire.begin();
  display.begin(&Adafruit128x64, 0x3C);
  display.setFont(Arial14);
  show("Battle Cars", "Starting...");

  connectWifi();

#if WS_USE_SSL
  webSocket.beginSSL(WS_HOST, WS_PORT, WS_PATH);
#else
  webSocket.begin(WS_HOST, WS_PORT, WS_PATH);
#endif
  webSocket.onEvent(webSocketEvent);
  webSocket.setReconnectInterval(3000);
  webSocket.enableHeartbeat(15000, 3000, 2);

  show("Connecting", "Backend...");
}

void loop() {
  keepWifiAlive();
  webSocket.loop();
  handleRadio();
}
