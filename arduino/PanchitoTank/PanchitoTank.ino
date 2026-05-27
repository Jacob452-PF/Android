// ============================================================
// Panchito Explorer - Keyestudio Mini Tank Robot V2.0
// Bluetooth + velocidad + aviso de pared + bateria opcional
//
// Comandos desde la app:
//   F = adelante
//   B = atras
//   L = izquierda
//   R = derecha
//   S = stop
//   1 / V1 = velocidad baja
//   2 / V2 = velocidad media
//   3 / V3 = velocidad alta
//   Y = seguidor ultrasonico
//   A = modo automatico / evasion de obstaculos
//   U = evasion de obstaculos
//   X = seguidor de luz
//   M = escaneo de muros
// ============================================================

#define SCL_Pin A5
#define SDA_Pin A4
#define ML_Ctrl 13
#define ML_PWM  11
#define MR_Ctrl 12
#define MR_PWM   3
#define Trig     5
#define Echo     4
#define servoPin 9
#define light_L_Pin A1
#define light_R_Pin A2

// Para bateria real necesitas divisor resistivo hacia A3.
// Si no lo tienes conectado, deja ENABLE_BATTERY_READ en 0.
#define BATTERY_PIN A3
#define ENABLE_BATTERY_READ 0

const int OBSTACLE_STOP_CM = 20;
const int SPEED_LOW = 120;
const int SPEED_MED = 180;
const int SPEED_HIGH = 230;

// Ajusta estos valores si instalas medicion de bateria.
// Para 2 baterias 18650: 6.4V bajo, 8.4V lleno.
const float BATTERY_MIN_V = 6.4;
const float BATTERY_MAX_V = 8.4;
const float BATTERY_DIVIDER_RATIO = 2.0;

unsigned char start01[] = {0x01,0x02,0x04,0x08,0x10,0x20,0x40,0x80,0x80,0x40,0x20,0x10,0x08,0x04,0x02,0x01};
unsigned char front[]   = {0x00,0x00,0x00,0x00,0x00,0x24,0x12,0x09,0x12,0x24,0x00,0x00,0x00,0x00,0x00,0x00};
unsigned char back[]    = {0x00,0x00,0x00,0x00,0x00,0x24,0x48,0x90,0x48,0x24,0x00,0x00,0x00,0x00,0x00,0x00};
unsigned char left[]    = {0x00,0x00,0x00,0x00,0x00,0x00,0x44,0x28,0x10,0x44,0x28,0x10,0x44,0x28,0x10,0x00};
unsigned char right[]   = {0x00,0x10,0x28,0x44,0x10,0x28,0x44,0x10,0x28,0x44,0x00,0x00,0x00,0x00,0x00,0x00};
unsigned char STOP01[]  = {0x2E,0x2A,0x3A,0x00,0x02,0x3E,0x02,0x00,0x3E,0x22,0x3E,0x00,0x3E,0x0A,0x0E,0x00};
unsigned char clear[]   = {0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};

char bluetooth_val = 'S';
int velocidadPWM = SPEED_LOW;
int pulsewidth;
int flag = 0;
int left_light = 0;
int right_light = 0;
unsigned long lastTelemetryMs = 0;
unsigned long lastMotionReportMs = 0;
char lastReportedMove = '?';

void setup() {
  Serial.begin(9600);

  pinMode(Trig, OUTPUT);
  pinMode(Echo, INPUT);
  pinMode(ML_Ctrl, OUTPUT);
  pinMode(ML_PWM, OUTPUT);
  pinMode(MR_Ctrl, OUTPUT);
  pinMode(MR_PWM, OUTPUT);
  pinMode(servoPin, OUTPUT);
  pinMode(SCL_Pin, OUTPUT);
  pinMode(SDA_Pin, OUTPUT);
  pinMode(light_L_Pin, INPUT);
  pinMode(light_R_Pin, INPUT);
  pinMode(BATTERY_PIN, INPUT);

  procedure(90);
  matrix_display(clear);
  matrix_display(start01);
  Car_Stop(false);
}

void loop() {
  leerBluetooth();

  switch (bluetooth_val) {
    case 'F':
      Car_front();
      matrix_display(front);
      break;
    case 'B':
      Car_back();
      matrix_display(back);
      break;
    case 'L':
      Car_left();
      matrix_display(left);
      break;
    case 'R':
      Car_right();
      matrix_display(right);
      break;
    case 'S':
      Car_Stop(false);
      matrix_display(STOP01);
      break;
    case 'Y':
      matrix_display(start01);
      follow();
      bluetooth_val = 'S';
      break;
    case 'A':
    case 'U':
      matrix_display(start01);
      avoid();
      bluetooth_val = 'S';
      break;
    case 'X':
      matrix_display(start01);
      light_track();
      bluetooth_val = 'S';
      break;
    case 'M':
      scanWalls();
      bluetooth_val = 'S';
      break;
  }

  enviarTelemetriaCadaSegundo();
}

void leerBluetooth() {
  while (Serial.available() > 0) {
    char c = Serial.read();
    if (c == '\n' || c == '\r') continue;

    if (c == 'V' || c == 'v') {
      unsigned long start = millis();
      while (!Serial.available() && millis() - start < 50) {}
      if (Serial.available()) {
        char nivel = Serial.read();
        cambiarVelocidad(nivel);
      }
      continue;
    }

    if (c == '1' || c == '2' || c == '3') {
      cambiarVelocidad(c);
      continue;
    }

    bluetooth_val = c;
  }
}

void cambiarVelocidad(char nivel) {
  if (nivel == '1') velocidadPWM = SPEED_LOW;
  else if (nivel == '2') velocidadPWM = SPEED_MED;
  else if (nivel == '3') velocidadPWM = SPEED_HIGH;

  Serial.print("{\"speed\":");
  Serial.print(velocidadPWM);
  Serial.println("}");
}

void follow() {
  flag = 0;
  while (flag == 0) {
    float distance = checkdistance();

    if (distance >= 20 && distance <= 60) {
      Car_front(false);
      matrix_display(front);
    } else if (distance > 10 && distance < 20) {
      Car_Stop();
      matrix_display(STOP01);
    } else if (distance <= 10) {
      Car_back(false);
      matrix_display(back);
    } else {
      Car_Stop();
      matrix_display(STOP01);
    }

    revisarSalidaModo();
    enviarTelemetriaCadaSegundo();
  }
}

void avoid() {
  flag = 0;
  while (flag == 0) {
    int random2 = random(1, 100);
    float a = checkdistance();

    if (a < OBSTACLE_STOP_CM) {
      Car_Stop();
      delay(200);

      procedure(160);
      float a1 = 0;
      for (int j = 0; j < 5; j++) a1 = checkdistance();
      delay(100);

      procedure(20);
      float a2 = 0;
      for (int k = 0; k < 5; k++) a2 = checkdistance();
      delay(100);

      procedure(90);

      if (a1 < 50 || a2 < 50) {
        if (a1 > a2) {
          Car_left(true);
          delay(500);
        } else {
          Car_right(true);
          delay(500);
        }
      } else {
        if (random2 % 2 == 0) {
          Car_left(true);
          delay(500);
        } else {
          Car_right(true);
          delay(500);
        }
      }
    } else {
      Car_front(false);
    }

    revisarSalidaModo();
    enviarTelemetriaCadaSegundo();
  }
}

void light_track() {
  flag = 0;
  while (flag == 0) {
    left_light = analogRead(light_L_Pin);
    right_light = analogRead(light_R_Pin);

    if (left_light > 650 && right_light > 650) Car_front(false);
    else if (left_light > 650 && right_light <= 650) Car_left(false);
    else if (left_light <= 650 && right_light > 650) Car_right(false);
    else Car_Stop(false);

    revisarSalidaModo();
    enviarTelemetriaCadaSegundo();
  }
}

void scanWalls() {
  flag = 0;
  Car_Stop(false);

  while (flag == 0) {
    for (int ang = 20; ang <= 160; ang += 20) {
      procedure(ang);
      float dist = checkdistance();
      enviarScan(ang, dist);
      revisarSalidaModo();
      if (flag == 1) break;
      delay(80);
    }

    for (int ang = 160; ang >= 20; ang -= 20) {
      procedure(ang);
      float dist = checkdistance();
      enviarScan(ang, dist);
      revisarSalidaModo();
      if (flag == 1) break;
      delay(80);
    }

    enviarTelemetriaCadaSegundo();
  }

  procedure(90);
}

void revisarSalidaModo() {
  if (Serial.available()) {
    char c = Serial.read();
    if (c == 'S') {
      flag = 1;
      bluetooth_val = 'S';
      Car_Stop();
    } else if (c == '1' || c == '2' || c == '3') {
      cambiarVelocidad(c);
    } else {
      bluetooth_val = c;
    }
  }
}

float checkdistance() {
  digitalWrite(Trig, LOW);
  delayMicroseconds(2);
  digitalWrite(Trig, HIGH);
  delayMicroseconds(10);
  digitalWrite(Trig, LOW);

  unsigned long duration = pulseIn(Echo, HIGH, 30000);
  if (duration == 0) return 999;
  return duration / 58.00;
}

void procedure(int myangle) {
  for (int i = 0; i <= 25; i++) {
    pulsewidth = myangle * 11 + 500;
    digitalWrite(servoPin, HIGH);
    delayMicroseconds(pulsewidth);
    digitalWrite(servoPin, LOW);
    delay(20 - pulsewidth / 1000);
  }
}

void Car_front() {
  Car_front(true);
}

void Car_front(bool revisarObstaculo) {
  if (revisarObstaculo) {
    float d = checkdistance();
    if (d > 0 && d < OBSTACLE_STOP_CM) {
      Car_Stop();
      Serial.println("{\"event\":\"OBSTACLE\",\"moving\":false}");
      return;
    }
  }

  digitalWrite(MR_Ctrl, LOW);
  analogWrite(MR_PWM, velocidadPWM);
  digitalWrite(ML_Ctrl, LOW);
  analogWrite(ML_PWM, velocidadPWM);
  reportMotion('F', true);
}

void Car_back() {
  Car_back(true);
}

void Car_back(bool avisar) {
  digitalWrite(MR_Ctrl, HIGH);
  analogWrite(MR_PWM, velocidadPWM);
  digitalWrite(ML_Ctrl, HIGH);
  analogWrite(ML_PWM, velocidadPWM);
  if (avisar) reportMotion('B', true);
}

void Car_left() {
  Car_left(true);
}

void Car_left(bool avisar) {
  digitalWrite(MR_Ctrl, LOW);
  analogWrite(MR_PWM, velocidadPWM);
  digitalWrite(ML_Ctrl, HIGH);
  analogWrite(ML_PWM, velocidadPWM);
  if (avisar) reportMotion('L', true);
}

void Car_right() {
  Car_right(true);
}

void Car_right(bool avisar) {
  digitalWrite(MR_Ctrl, HIGH);
  analogWrite(MR_PWM, velocidadPWM);
  digitalWrite(ML_Ctrl, LOW);
  analogWrite(ML_PWM, velocidadPWM);
  if (avisar) reportMotion('R', true);
}

void Car_Stop() {
  Car_Stop(true);
}

void Car_Stop(bool avisar) {
  digitalWrite(MR_Ctrl, LOW);
  analogWrite(MR_PWM, 0);
  digitalWrite(ML_Ctrl, LOW);
  analogWrite(ML_PWM, 0);
  if (avisar) reportMotion('S', false);
}

void reportMotion(char move, bool moving) {
  unsigned long now = millis();
  if (move == lastReportedMove && now - lastMotionReportMs < 250) return;

  lastReportedMove = move;
  lastMotionReportMs = now;

  Serial.print("{\"event\":\"");
  Serial.print(moving ? "MOVING" : "STOP");
  Serial.print("\",\"moving\":");
  Serial.print(moving ? "true" : "false");
  Serial.print(",\"move\":\"");
  Serial.print(move);
  Serial.println("\"}");
}

void enviarTelemetriaCadaSegundo() {
  if (millis() - lastTelemetryMs < 1000) return;
  lastTelemetryMs = millis();

  int bat = leerBateriaPorcentaje();
  if (bat >= 0) {
    Serial.print("{\"b\":");
    Serial.print(bat);
    Serial.println("}");
  }
}

int leerBateriaPorcentaje() {
  if (ENABLE_BATTERY_READ == 0) return -1;

  int raw = analogRead(BATTERY_PIN);
  float voltagePin = raw * (5.0 / 1023.0);
  float voltageBattery = voltagePin * BATTERY_DIVIDER_RATIO;
  int pct = (int)((voltageBattery - BATTERY_MIN_V) * 100.0 / (BATTERY_MAX_V - BATTERY_MIN_V));
  return constrain(pct, 0, 100);
}

void enviarScan(int ang, float dist) {
  Serial.print("{\"scan\":{\"ang\":");
  Serial.print(ang);
  Serial.print(",\"dist\":");
  Serial.print(dist, 1);
  Serial.println("}}");
}

void matrix_display(unsigned char matrix_value[]) {
  IIC_start();
  IIC_send(0xc0);
  for (int i = 0; i < 16; i++) IIC_send(matrix_value[i]);
  IIC_end();
  IIC_start();
  IIC_send(0x8A);
  IIC_end();
}

void IIC_start() {
  digitalWrite(SCL_Pin, HIGH);
  delayMicroseconds(3);
  digitalWrite(SDA_Pin, HIGH);
  delayMicroseconds(3);
  digitalWrite(SDA_Pin, LOW);
  delayMicroseconds(3);
}

void IIC_send(unsigned char send_data) {
  for (char i = 0; i < 8; i++) {
    digitalWrite(SCL_Pin, LOW);
    delayMicroseconds(3);
    digitalWrite(SDA_Pin, (send_data & 0x01) ? HIGH : LOW);
    delayMicroseconds(3);
    digitalWrite(SCL_Pin, HIGH);
    delayMicroseconds(3);
    send_data >>= 1;
  }
}

void IIC_end() {
  digitalWrite(SCL_Pin, LOW);
  delayMicroseconds(3);
  digitalWrite(SDA_Pin, LOW);
  delayMicroseconds(3);
  digitalWrite(SCL_Pin, HIGH);
  delayMicroseconds(3);
  digitalWrite(SDA_Pin, HIGH);
  delayMicroseconds(3);
}
