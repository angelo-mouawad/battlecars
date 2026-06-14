#include <Arduino.h>
#include <WiFi.h>
#include <WebSocketsClient.h>
#include <RCSwitch.h>
#include <Wire.h>
#include <SSD1306Ascii.h>
#include <SSD1306AsciiWire.h>
#include <ArduinoJson.h>
#include <esp_bt.h>

#include "config.h"
const char* ssid     = WIFI_SSID;
const char* password = WIFI_PASSWORD;
const char* wsHost   = WS_HOST;
const int   wsPort   = WS_PORT;
const char* wsPath   = WS_PATH;

#define CAR1_UP      11
#define CAR1_DOWN    12
#define CAR1_LEFT    13
#define CAR1_RIGHT   14
#define CAR1_STOP    15
#define CAR1_HAMMER  16
#define CAR2_UP      21
#define CAR2_DOWN    22
#define CAR2_LEFT    23
#define CAR2_RIGHT   24
#define CAR2_STOP    25

#define CAR1_UPRIGHT  91
#define CAR1_FLIPPED  92
#define CAR2_UPRIGHT  101
#define CAR2_FLIPPED  102
#define CAR1_HIT  51
#define CAR2_HIT  61

#define HUB_COOLDOWN 1000
#define HIT_COOLDOWN_HUB 500

RCSwitch radioTX1 = RCSwitch();
RCSwitch radioTX2 = RCSwitch();
RCSwitch radioRX  = RCSwitch();
WebSocketsClient webSocket;
SSD1306AsciiWire display;

unsigned long lastCar1Code = 0;
unsigned long lastCar2Code = 0;
unsigned long lastCar1Hit = 0;
unsigned long lastCar2Hit = 0;

void sendCarData(int carId, bool hit, bool isFlipped) {
  StaticJsonDocument<128> doc;
  doc["carId"]       = carId;
  doc["hit"]         = hit;
  doc["orientation"] = isFlipped ? "flipped" : "upright";

  String json;
  serializeJson(doc, json);
  webSocket.sendTXT(json);
  Serial.println("Sent: " + json);
}

void sendToRadio(int code, String label) {
  display.clear();
  display.println("CMD:");
  display.println(label);

  if (code == CAR1_STOP || code == CAR2_STOP) {
    for (int i = 0; i < 5; i++) {
      if (code == CAR1_STOP) radioTX1.send(code, 24);
      else radioTX2.send(code, 24);
    }
  } else if (code >= 20 && code <= 29) {
    for (int i = 0; i < 3; i++) radioTX2.send(code, 24);
  } else {
    for (int i = 0; i < 3; i++) radioTX1.send(code, 24);
  }

  Serial.println("Sent RC code: " + String(code));
}

void webSocketEvent(WStype_t type, uint8_t* payload, size_t length) {
  switch (type) {
    case WStype_CONNECTED:
      Serial.println("Connected to backend");
      display.clear();
      display.println("WS Connected!");
      break;

    case WStype_DISCONNECTED:
      Serial.println("Disconnected");
      display.clear();
      display.println("WS Lost...");
      display.println("Reconnecting");
      break;

    case WStype_TEXT: {
      String msg = String((char*)payload);
      msg.replace("\"", "");
      msg.trim();
      Serial.println("Received command: " + msg);

      display.clear();
      display.println("CMD:");
      display.println(msg);

      if (msg == "1:UP")      sendToRadio(CAR1_UP,     "Car1 UP");
      if (msg == "1:DOWN")    sendToRadio(CAR1_DOWN,   "Car1 DOWN");
      if (msg == "1:LEFT")    sendToRadio(CAR1_LEFT,   "Car1 LEFT");
      if (msg == "1:RIGHT")   sendToRadio(CAR1_RIGHT,  "Car1 RIGHT");
      if (msg == "1:STOP")    sendToRadio(CAR1_STOP,   "Car1 STOP");
      if (msg == "1:HAMMER")  sendToRadio(CAR1_HAMMER, "Car1 HAMMER");
      if (msg == "2:UP")      sendToRadio(CAR2_UP,     "Car2 UP");
      if (msg == "2:DOWN")    sendToRadio(CAR2_DOWN,   "Car2 DOWN");
      if (msg == "2:LEFT")    sendToRadio(CAR2_LEFT,   "Car2 LEFT");
      if (msg == "2:RIGHT")   sendToRadio(CAR2_RIGHT,  "Car2 RIGHT");
      if (msg == "2:STOP")    sendToRadio(CAR2_STOP,   "Car2 STOP");
      break;
    }

    case WStype_ERROR:
      Serial.println("WebSocket error");
      break;

    default:
      break;
  }
}

void setup() {
  Serial.begin(115200);

  setCpuFrequencyMhz(80);
  btStop();
  esp_bt_controller_disable();

  radioTX1.enableTransmit(15);
  radioTX2.enableTransmit(4);
  radioRX.enableReceive(2);
  radioTX1.setRepeatTransmit(2);
  radioTX2.setRepeatTransmit(2);

  Wire.begin();
  display.begin(&Adafruit128x64, 0x3C);
  display.setFont(Arial14);
  display.clear();
  display.println("Battle Cars");
  display.println("Starting...");

  WiFi.begin(ssid, password);
  WiFi.setTxPower(WIFI_POWER_8_5dBm);
  display.clear();
  display.println("Connecting");
  display.println("WiFi...");

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  Serial.println("\nWiFi connected!");
  display.clear();
  display.println("WiFi OK!");
  display.println(WiFi.localIP().toString());
  delay(1000);

  webSocket.beginSSL(wsHost, wsPort, wsPath);
  webSocket.onEvent(webSocketEvent);
  webSocket.setReconnectInterval(3000);
  webSocket.enableHeartbeat(15000, 3000, 2);

  display.clear();
  display.println("Connecting");
  display.println("Backend...");
}

void loop() {
  webSocket.loop();

  if (radioRX.available()) {
    unsigned long code = radioRX.getReceivedValue();
    unsigned long now = millis();
    Serial.println("Received from car: " + String(code));

    if ((code == CAR1_UPRIGHT || code == CAR1_FLIPPED) &&
        now - lastCar1Code > HUB_COOLDOWN) {
      lastCar1Code = now;
      if (code == CAR1_UPRIGHT) sendCarData(1, false, false);
      if (code == CAR1_FLIPPED) sendCarData(1, false, true);
    }

    if (code == CAR1_HIT && now - lastCar1Hit > HIT_COOLDOWN_HUB) {
      lastCar1Hit = now;
      sendCarData(1, true, false);
    }

    if ((code == CAR2_UPRIGHT || code == CAR2_FLIPPED) &&
        now - lastCar2Code > HUB_COOLDOWN) {
      lastCar2Code = now;
      if (code == CAR2_UPRIGHT) sendCarData(2, false, false);
      if (code == CAR2_FLIPPED) sendCarData(2, false, true);
    }

    if (code == CAR2_HIT && now - lastCar2Hit > HIT_COOLDOWN_HUB) {
      lastCar2Hit = now;
      sendCarData(2, true, false);
    }

    radioRX.resetAvailable();
  }
}