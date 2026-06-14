#include <Arduino.h>
#include <RCSwitch.h>
#include <Wire.h>
#include <SSD1306Ascii.h>
#include <SSD1306AsciiWire.h>

#define pinReceiver 2
#define pinTransmitter 12

#define IN1 3
#define IN2 4
#define IN3 5
#define IN4 6
#define IN5 7
#define IN6 8
#define IN7 10
#define IN8 11

#define MPU_ADDR 0x68
#define REG_PWR_MGMT_1 0x6B
#define REG_ACCEL_CONFIG 0x1C
#define REG_ACCEL_XOUT_H 0x3B

const int hitThreshold = 25000;
#define HIT_COOLDOWN 2000
#define FLIP_THRESHOLD 14000
#define COMMAND_TIMEOUT 1200

int16_t baseX, baseY, baseZ;

RCSwitch receiver = RCSwitch();
RCSwitch transmitter = RCSwitch();
SSD1306AsciiWire display;

bool hitOccurred = false;
unsigned long lastHitTime = 0;
unsigned long lastCommandTime = 0;
unsigned long lastDriveCommandTime = 0;

void writeRegister(uint8_t reg, uint8_t value) {
  Wire.beginTransmission(MPU_ADDR);
  Wire.write(reg);
  Wire.write(value);
  Wire.endTransmission();
}

void readAccel(int16_t &x, int16_t &y, int16_t &z) {
  Wire.beginTransmission(MPU_ADDR);
  Wire.write(REG_ACCEL_XOUT_H);
  Wire.endTransmission(false);
  Wire.requestFrom(MPU_ADDR, 6);
  x = (Wire.read() << 8) | Wire.read();
  y = (Wire.read() << 8) | Wire.read();
  z = (Wire.read() << 8) | Wire.read();
}

void showMessage(String msg) {
  display.clear();
  display.println(msg);
  Serial.println("Display: " + msg);
}

void motorStop() {
  digitalWrite(IN1, LOW); digitalWrite(IN2, LOW);
  digitalWrite(IN3, LOW); digitalWrite(IN4, LOW);
  digitalWrite(IN5, LOW); digitalWrite(IN6, LOW);
  digitalWrite(IN7, LOW); digitalWrite(IN8, LOW);
}

void motorForward() {
  digitalWrite(IN1, HIGH); digitalWrite(IN2, LOW);
  digitalWrite(IN3, HIGH); digitalWrite(IN4, LOW);
  digitalWrite(IN5, HIGH); digitalWrite(IN6, LOW);
  digitalWrite(IN7, HIGH); digitalWrite(IN8, LOW);
}

void motorBackward() {
  digitalWrite(IN1, LOW); digitalWrite(IN2, HIGH);
  digitalWrite(IN3, LOW); digitalWrite(IN4, HIGH);
  digitalWrite(IN5, LOW); digitalWrite(IN6, HIGH);
  digitalWrite(IN7, LOW); digitalWrite(IN8, HIGH);
}

void motorRight() {
  digitalWrite(IN1, LOW);  digitalWrite(IN2, HIGH);
  digitalWrite(IN3, HIGH); digitalWrite(IN4, LOW);
  digitalWrite(IN5, LOW);  digitalWrite(IN6, HIGH);
  digitalWrite(IN7, HIGH); digitalWrite(IN8, LOW);
}

void motorLeft() {
  digitalWrite(IN1, HIGH); digitalWrite(IN2, LOW);
  digitalWrite(IN3, LOW);  digitalWrite(IN4, HIGH);
  digitalWrite(IN5, HIGH); digitalWrite(IN6, LOW);
  digitalWrite(IN7, LOW);  digitalWrite(IN8, HIGH);
}

void sendTelemetry() {
  int16_t x, y, z;
  readAccel(x, y, z);

  bool isFlipped = z < -FLIP_THRESHOLD;
  if (isFlipped) {
    transmitter.send(92, 24);
    showMessage("FLIPPED!");
  } else {
    transmitter.send(91, 24);
    showMessage("Upright");
  }

  if (hitOccurred) {
    transmitter.send(51, 24);
    hitOccurred = false;
    Serial.println("Hit sent");
  }

  Serial.println("Telemetry sent");
}

void setup() {
  Serial.begin(9600);

  pinMode(IN1, OUTPUT); pinMode(IN2, OUTPUT);
  pinMode(IN3, OUTPUT); pinMode(IN4, OUTPUT);
  pinMode(IN5, OUTPUT); pinMode(IN6, OUTPUT);
  pinMode(IN7, OUTPUT); pinMode(IN8, OUTPUT);
  motorStop();

  Wire.begin();
  display.begin(&Adafruit128x64, 0x3C);
  display.setFont(Arial14);
  display.clear();
  display.println("Car 1 ON!");

  receiver.enableReceive(0);
  transmitter.enableTransmit(pinTransmitter);

  writeRegister(REG_PWR_MGMT_1, 0x00);
  writeRegister(REG_ACCEL_CONFIG, 0x00);
  delay(100);

  readAccel(baseX, baseY, baseZ);
  Serial.print("Baseline X:"); Serial.print(baseX);
  Serial.print(" Y:"); Serial.print(baseY);
  Serial.print(" Z:"); Serial.println(baseZ);

  display.clear();
  display.println("Waiting...");
}

void loop() {
  unsigned long now = millis();

  // auto stop if no command received recently
  static bool autoStopped = false;
  static bool telemetrySentOnStop = false;

  if (now - lastCommandTime > COMMAND_TIMEOUT) {
    motorStop();
    if (!autoStopped) {
      autoStopped = true;
      telemetrySentOnStop = false;
    }
    if (!telemetrySentOnStop) {
      telemetrySentOnStop = true;
      lastDriveCommandTime = 0;
      sendTelemetry();
      Serial.println("Auto stop telemetry sent");
    }
  } else {
    autoStopped = false;
    telemetrySentOnStop = false;
  }

  // handle incoming drive commands
  if (receiver.available()) {
    unsigned long code = receiver.getReceivedValue();

    if (code == 11 || code == 12 || code == 13 ||
        code == 14 || code == 15) {
      Serial.println("Got code: " + String(code));

      if (code == 11) { motorForward();  showMessage("UP");    lastCommandTime = now; lastDriveCommandTime = now; }
      if (code == 12) { motorBackward(); showMessage("DOWN");  lastCommandTime = now; lastDriveCommandTime = now; }
      if (code == 13) { motorLeft();     showMessage("LEFT");  lastCommandTime = now; lastDriveCommandTime = now; }
      if (code == 14) { motorRight();    showMessage("RIGHT"); lastCommandTime = now; lastDriveCommandTime = now; }
      if (code == 15) { motorStop(); showMessage("STOP"); lastCommandTime = now; lastDriveCommandTime = 0; sendTelemetry(); }
    }

    receiver.resetAvailable();
    while (receiver.available()) {
      receiver.resetAvailable();
    }
  }

  // hit detection — always active
  if (now - lastHitTime >= HIT_COOLDOWN) {
    int16_t x, y, z;
    readAccel(x, y, z);
    int16_t dx = x - baseX;
    int16_t dy = y - baseY;
    int16_t dz = z - baseZ;
    int magnitude = abs(dx) + abs(dy) + abs(dz);

    if (magnitude > hitThreshold) {
      lastHitTime = now;
      hitOccurred = true;
      showMessage("HIT!");
      Serial.println("Hit stored");

      if (now - lastCommandTime > COMMAND_TIMEOUT) {
        sendTelemetry();
      }
    }
  }
}