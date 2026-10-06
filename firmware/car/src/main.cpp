#include <Arduino.h>
#include <RCSwitch.h>
#include <Wire.h>
#include <SSD1306Ascii.h>
#include <SSD1306AsciiWire.h>
#include <BattleCarsProtocol.h>

#ifndef CAR_ID
#error "Build with -D CAR_ID=1 or -D CAR_ID=2 (use the car1 or car2 PlatformIO environment)"
#endif

#ifndef HAS_HAMMER
#define HAS_HAMMER 0
#endif

#if HAS_HAMMER
#include <Servo.h>
#endif

using namespace BattleCars;

const uint8_t RECEIVER_INTERRUPT = 0;
const uint8_t TRANSMITTER_PIN = 12;
const uint8_t SERVO_PIN = 9;
const uint8_t MOTOR_PINS[8] = {3, 4, 5, 6, 7, 8, 10, 11};

const uint8_t MPU_ADDR = 0x68;
const uint8_t REG_PWR_MGMT_1 = 0x6B;
const uint8_t REG_ACCEL_CONFIG = 0x1C;
const uint8_t REG_ACCEL_XOUT_H = 0x3B;

const long HIT_THRESHOLD = 25000;
const int16_t FLIP_THRESHOLD = 14000;
const unsigned long HIT_COOLDOWN_MS = 2000;
const unsigned long COMMAND_TIMEOUT_MS = 1200;
const unsigned long SENSOR_RETRY_MS = 2000;
const unsigned long HAMMER_SWING_MS = 300;
const unsigned long HAMMER_COOLDOWN_MS = 600;

RCSwitch receiver;
RCSwitch transmitter;
SSD1306AsciiWire display;

#if HAS_HAMMER
Servo hammerServo;
bool hammerOut = false;
unsigned long hammerStartedAt = 0;
#endif

bool sensorOk = false;
unsigned long lastSensorAttempt = 0;
int16_t baseX = 0, baseY = 0, baseZ = 0;

bool hitPending = false;
bool stoppedTelemetrySent = false;
unsigned long lastHitTime = 0;
unsigned long lastCommandTime = 0;
const char *shownMessage = "";

void showMessage(const char *msg) {
  if (strcmp(msg, shownMessage) == 0) {
    return;
  }
  shownMessage = msg;
  display.clear();
  display.println(msg);
  Serial.print(F("Display: "));
  Serial.println(msg);
}

bool writeRegister(uint8_t reg, uint8_t value) {
  Wire.beginTransmission(MPU_ADDR);
  Wire.write(reg);
  Wire.write(value);
  return Wire.endTransmission() == 0;
}

bool readAccel(int16_t &x, int16_t &y, int16_t &z) {
  Wire.beginTransmission(MPU_ADDR);
  Wire.write(REG_ACCEL_XOUT_H);
  if (Wire.endTransmission(false) != 0) {
    return false;
  }
  if (Wire.requestFrom(MPU_ADDR, (uint8_t)6) != 6) {
    return false;
  }
  uint8_t raw[6];
  for (uint8_t i = 0; i < 6; i++) {
    raw[i] = Wire.read();
  }
  x = (int16_t)((raw[0] << 8) | raw[1]);
  y = (int16_t)((raw[2] << 8) | raw[3]);
  z = (int16_t)((raw[4] << 8) | raw[5]);
  return true;
}

bool initSensor() {
  if (!writeRegister(REG_PWR_MGMT_1, 0x00) || !writeRegister(REG_ACCEL_CONFIG, 0x00)) {
    return false;
  }
  delay(100);
  if (!readAccel(baseX, baseY, baseZ)) {
    return false;
  }
  Serial.print(F("Baseline X:")); Serial.print(baseX);
  Serial.print(F(" Y:")); Serial.print(baseY);
  Serial.print(F(" Z:")); Serial.println(baseZ);
  return true;
}

void setMotors(const uint8_t levels[8]) {
  for (uint8_t i = 0; i < 8; i++) {
    digitalWrite(MOTOR_PINS[i], levels[i]);
  }
}

void motorStop()     { const uint8_t l[8] = {LOW, LOW, LOW, LOW, LOW, LOW, LOW, LOW};     setMotors(l); }
void motorForward()  { const uint8_t l[8] = {HIGH, LOW, HIGH, LOW, HIGH, LOW, HIGH, LOW}; setMotors(l); }
void motorBackward() { const uint8_t l[8] = {LOW, HIGH, LOW, HIGH, LOW, HIGH, LOW, HIGH}; setMotors(l); }
void motorRight()    { const uint8_t l[8] = {LOW, HIGH, HIGH, LOW, LOW, HIGH, HIGH, LOW}; setMotors(l); }
void motorLeft()     { const uint8_t l[8] = {HIGH, LOW, LOW, HIGH, HIGH, LOW, LOW, HIGH}; setMotors(l); }

void sendTelemetry() {
  if (sensorOk) {
    int16_t x, y, z;
    if (readAccel(x, y, z)) {
      bool flipped = z < -FLIP_THRESHOLD;
      transmitter.send(flipped ? flippedCode(CAR_ID) : uprightCode(CAR_ID), RADIO_BIT_LENGTH);
      showMessage(flipped ? "FLIPPED!" : "Upright");
    } else {
      sensorOk = false;
      showMessage("Sensor error");
    }
  }

  if (hitPending) {
    transmitter.send(hitCode(CAR_ID), RADIO_BIT_LENGTH);
    hitPending = false;
    Serial.println(F("Hit sent"));
  }
}

#if HAS_HAMMER
void startHammer(unsigned long now) {
  if (hammerOut || now - hammerStartedAt < HAMMER_COOLDOWN_MS) {
    return;
  }
  hammerServo.write(90);
  hammerOut = true;
  hammerStartedAt = now;
  showMessage("HAMMER!");
}

void updateHammer(unsigned long now) {
  if (hammerOut && now - hammerStartedAt >= HAMMER_SWING_MS) {
    hammerServo.write(0);
    hammerOut = false;
  }
}
#endif

void handleCommand(Command command, unsigned long now) {
  lastCommandTime = now;
  stoppedTelemetrySent = false;

  switch (command) {
    case CMD_UP:    motorForward();  showMessage("UP");    break;
    case CMD_DOWN:  motorBackward(); showMessage("DOWN");  break;
    case CMD_LEFT:  motorLeft();     showMessage("LEFT");  break;
    case CMD_RIGHT: motorRight();    showMessage("RIGHT"); break;
    case CMD_STOP:
      motorStop();
      showMessage("STOP");
      sendTelemetry();
      stoppedTelemetrySent = true;
      break;
    case CMD_HAMMER:
#if HAS_HAMMER
      startHammer(now);
#endif
      break;
    default:
      break;
  }
}

void checkForHit(unsigned long now) {
  if (!sensorOk || now - lastHitTime < HIT_COOLDOWN_MS) {
    return;
  }

  int16_t x, y, z;
  if (!readAccel(x, y, z)) {
    sensorOk = false;
    showMessage("Sensor error");
    return;
  }

  long magnitude = labs((long)x - baseX) + labs((long)y - baseY) + labs((long)z - baseZ);
  if (magnitude > HIT_THRESHOLD) {
    lastHitTime = now;
    hitPending = true;
    showMessage("HIT!");

    if (now - lastCommandTime > COMMAND_TIMEOUT_MS) {
      sendTelemetry();
    }
  }
}

void setup() {
  Serial.begin(9600);

  for (uint8_t i = 0; i < 8; i++) {
    pinMode(MOTOR_PINS[i], OUTPUT);
  }
  motorStop();

#if HAS_HAMMER
  hammerServo.attach(SERVO_PIN);
  hammerServo.write(0);
#endif

  Wire.begin();
  Wire.setWireTimeout(3000, true);
  display.begin(&Adafruit128x64, 0x3C);
  display.setFont(Arial14);
  showMessage(CAR_ID == 1 ? "Car 1 ON!" : "Car 2 ON!");

  receiver.enableReceive(RECEIVER_INTERRUPT);
  transmitter.enableTransmit(TRANSMITTER_PIN);

  sensorOk = initSensor();
  lastSensorAttempt = millis();
  showMessage(sensorOk ? "Waiting..." : "Sensor error");
}

void loop() {
  unsigned long now = millis();

  if (!sensorOk && now - lastSensorAttempt >= SENSOR_RETRY_MS) {
    lastSensorAttempt = now;
    sensorOk = initSensor();
    if (sensorOk) {
      showMessage("Sensor OK");
    }
  }

  if (now - lastCommandTime > COMMAND_TIMEOUT_MS) {
    motorStop();
    if (!stoppedTelemetrySent) {
      stoppedTelemetrySent = true;
      sendTelemetry();
    }
  }

  if (receiver.available()) {
    Command command;
    if (decodeCommand(receiver.getReceivedValue(), CAR_ID, command)) {
      handleCommand(command, now);
    }
    receiver.resetAvailable();
  }

#if HAS_HAMMER
  updateHammer(now);
#endif

  checkForHit(now);
}
