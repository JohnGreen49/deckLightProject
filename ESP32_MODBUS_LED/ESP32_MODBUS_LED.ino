// ESP32 + WS2812B 9-LED strip controlled over WiFi via Modbus TCP.
//
// Combines:
//   - WiFi STA connect (see ESP32_WIFI_TEST)
//   - FastLED driver on GPIO 16, WS2812B / GRB (see ESP32_LED_TEST)
//   - Hand-rolled Modbus TCP slave on port 502 (no extra library)
//
// Holding register map (FC3 read, FC6 write single, FC16 write multiple):
//
//   HR 0..26 : 9 LEDs * 3 registers each (R, G, B), values 0..255
//              LED i red   = HR (3*i + 0)
//              LED i green = HR (3*i + 1)
//              LED i blue  = HR (3*i + 2)
//   HR 27    : master brightness, 0..255
//   HR 28    : master enable, 0=off (strip cleared), 1=on
//
// Any write commits the new state to the strip immediately. Values above 255
// are clipped. Reads of unmapped addresses return ILLEGAL DATA ADDRESS (0x02).
//
// Quick test from a Linux box on the same LAN (using mbpoll):
//   mbpoll -m tcp -a 1 -r 1 -c 27 -t 4 -1 <esp32-ip>            # read all RGB
//   mbpoll -m tcp -a 1 -r 1 -t 4 <esp32-ip> 255 0 0             # LED0 = red
//   mbpoll -m tcp -a 1 -r 28 -t 4 <esp32-ip> 64                 # brightness=64
//   mbpoll -m tcp -a 1 -r 29 -t 4 <esp32-ip> 1                  # enable=on
// (mbpoll uses 1-based register numbering; address 0 is register 1.)

#include <WiFi.h>
#include <FastLED.h>
#include <Preferences.h>

// ---------- User config ----------
// SSID and password live in NVS flash (namespace "wifi"), set over the serial
// console at 115200 8N1. See the serial command handler at the bottom of this
// file. Hostname is fixed.
const char* HOSTNAME  = "esp32-decklights";

#define NUM_LEDS    9
#define DATA_PIN    16
#define CHIPSET     WS2812B
#define COLOR_ORDER GRB

// Live WiFi credentials (loaded from NVS at boot, updated by serial commands).
String wifiSsid;
String wifiPass;
Preferences wifiPrefs;

const uint16_t MODBUS_PORT      = 502;
const uint8_t  MODBUS_UNIT_ID   = 1;     // accept this unit id (and 0xFF broadcast-style)
const unsigned long CONNECT_TIMEOUT_MS = 30000;
const unsigned long STATUS_INTERVAL_MS = 5000;
// ---------------------------------

// Register layout
static const uint16_t HR_RGB_BASE   = 0;
static const uint16_t HR_RGB_COUNT  = NUM_LEDS * 3;        // 27
static const uint16_t HR_BRIGHTNESS = HR_RGB_BASE + HR_RGB_COUNT; // 27
static const uint16_t HR_ENABLE     = HR_BRIGHTNESS + 1;         // 28
static const uint16_t HR_TOTAL      = HR_ENABLE + 1;             // 29

static uint16_t holding[HR_TOTAL] = {0};

CRGB leds[NUM_LEDS];

WiFiServer modbusServer(MODBUS_PORT);
WiFiClient modbusClient;   // single active client; new connections displace it

// Modbus exception codes
enum : uint8_t {
  MB_EX_ILLEGAL_FUNCTION  = 0x01,
  MB_EX_ILLEGAL_ADDRESS   = 0x02,
  MB_EX_ILLEGAL_VALUE     = 0x03,
  MB_EX_SLAVE_FAILURE     = 0x04,
};

// ---------- LED apply ----------
static void applyStrip() {
  uint8_t bright = (uint8_t)min<uint16_t>(holding[HR_BRIGHTNESS], 255);
  FastLED.setBrightness(bright);

  bool enabled = holding[HR_ENABLE] != 0;

  for (int i = 0; i < NUM_LEDS; i++) {
    uint8_t r = enabled ? (uint8_t)min<uint16_t>(holding[HR_RGB_BASE + 3 * i + 0], 255) : 0;
    uint8_t g = enabled ? (uint8_t)min<uint16_t>(holding[HR_RGB_BASE + 3 * i + 1], 255) : 0;
    uint8_t b = enabled ? (uint8_t)min<uint16_t>(holding[HR_RGB_BASE + 3 * i + 2], 255) : 0;
    leds[i] = CRGB(r, g, b);
  }
  FastLED.show();
}

// ---------- WiFi ----------
static const char* wifiStatusToString(wl_status_t s) {
  switch (s) {
    case WL_IDLE_STATUS:     return "IDLE";
    case WL_NO_SSID_AVAIL:   return "NO_SSID_AVAIL";
    case WL_SCAN_COMPLETED:  return "SCAN_COMPLETED";
    case WL_CONNECTED:       return "CONNECTED";
    case WL_CONNECT_FAILED:  return "CONNECT_FAILED";
    case WL_CONNECTION_LOST: return "CONNECTION_LOST";
    case WL_DISCONNECTED:    return "DISCONNECTED";
    default:                 return "UNKNOWN";
  }
}

static void loadCredentials() {
  wifiPrefs.begin("wifi", /*readOnly=*/true);
  wifiSsid = wifiPrefs.getString("ssid", "");
  wifiPass = wifiPrefs.getString("pass", "");
  wifiPrefs.end();
}

static void saveCredentials() {
  wifiPrefs.begin("wifi", /*readOnly=*/false);
  wifiPrefs.putString("ssid", wifiSsid);
  wifiPrefs.putString("pass", wifiPass);
  wifiPrefs.end();
}

static void clearCredentials() {
  wifiPrefs.begin("wifi", /*readOnly=*/false);
  wifiPrefs.clear();
  wifiPrefs.end();
  wifiSsid = "";
  wifiPass = "";
}

static bool connectWiFi() {
  if (wifiSsid.length() == 0) {
    Serial.println("No SSID configured. Use 'ssid <name>' and 'pass <secret>' over serial.");
    return false;
  }

  WiFi.mode(WIFI_STA);
  WiFi.setHostname(HOSTNAME);
  WiFi.begin(wifiSsid.c_str(), wifiPass.c_str());

  Serial.print("Connecting to ");
  Serial.print(wifiSsid);

  unsigned long start = millis();
  while (WiFi.status() != WL_CONNECTED &&
         millis() - start < CONNECT_TIMEOUT_MS) {
    delay(500);
    Serial.print('.');
  }
  Serial.println();

  if (WiFi.status() == WL_CONNECTED) {
    Serial.print("WiFi connected. IP: ");
    Serial.println(WiFi.localIP());
    Serial.print("Modbus TCP on port ");
    Serial.println(MODBUS_PORT);
    return true;
  }

  Serial.print("WiFi NOT connected. status=");
  Serial.println(wifiStatusToString(WiFi.status()));
  return false;
}

// ---------- Modbus TCP ----------
//
// MBAP header (7 bytes): TID(2) PID(2)=0 LEN(2) UID(1)
// then PDU: FC(1) + data
//
// We only support FC3 (read holding), FC6 (write single), FC16 (write multiple).

static void sendException(WiFiClient& c, const uint8_t mbap[7], uint8_t fc, uint8_t code) {
  uint8_t out[9];
  memcpy(out, mbap, 7);
  // length covers UID + FC + ExceptionCode = 3
  out[4] = 0x00;
  out[5] = 0x03;
  out[7] = fc | 0x80;
  out[8] = code;
  c.write(out, 9);
}

static bool readExact(WiFiClient& c, uint8_t* buf, size_t n, uint32_t timeoutMs = 1000) {
  uint32_t start = millis();
  size_t got = 0;
  while (got < n) {
    if (!c.connected()) return false;
    int avail = c.available();
    if (avail > 0) {
      int r = c.read(buf + got, n - got);
      if (r > 0) got += r;
    } else {
      if (millis() - start > timeoutMs) return false;
      delay(1);
    }
  }
  return true;
}

static void handleRequest(WiFiClient& c) {
  uint8_t mbap[7];
  if (!readExact(c, mbap, 7)) { c.stop(); return; }

  uint16_t pid = (mbap[2] << 8) | mbap[3];
  uint16_t len = (mbap[4] << 8) | mbap[5];
  uint8_t  uid = mbap[6];

  if (pid != 0 || len < 2 || len > 260) { c.stop(); return; }

  // PDU length = len - 1 (UID is counted in len)
  uint16_t pduLen = len - 1;
  uint8_t pdu[256];
  if (!readExact(c, pdu, pduLen)) { c.stop(); return; }

  // Accept our unit id, and 0 (often used as "any") and 0xFF (TCP-meaningful)
  if (uid != MODBUS_UNIT_ID && uid != 0 && uid != 0xFF) {
    return; // silently drop — not for us
  }

  uint8_t fc = pdu[0];
  bool dirty = false;

  switch (fc) {
    case 0x03: { // Read Holding Registers
      if (pduLen != 5) { sendException(c, mbap, fc, MB_EX_ILLEGAL_VALUE); return; }
      uint16_t addr = (pdu[1] << 8) | pdu[2];
      uint16_t qty  = (pdu[3] << 8) | pdu[4];
      if (qty < 1 || qty > 125) { sendException(c, mbap, fc, MB_EX_ILLEGAL_VALUE); return; }
      if ((uint32_t)addr + qty > HR_TOTAL) { sendException(c, mbap, fc, MB_EX_ILLEGAL_ADDRESS); return; }

      uint8_t out[7 + 2 + 2 * 125];
      memcpy(out, mbap, 7);
      uint16_t respLen = 1 /*UID*/ + 2 /*FC+BC*/ + 2 * qty;
      out[4] = (respLen >> 8) & 0xFF;
      out[5] = respLen & 0xFF;
      out[7] = fc;
      out[8] = (uint8_t)(2 * qty);
      for (uint16_t i = 0; i < qty; i++) {
        uint16_t v = holding[addr + i];
        out[9 + 2 * i] = (v >> 8) & 0xFF;
        out[10 + 2 * i] = v & 0xFF;
      }
      c.write(out, 9 + 2 * qty);
      break;
    }

    case 0x06: { // Write Single Register
      if (pduLen != 5) { sendException(c, mbap, fc, MB_EX_ILLEGAL_VALUE); return; }
      uint16_t addr = (pdu[1] << 8) | pdu[2];
      uint16_t val  = (pdu[3] << 8) | pdu[4];
      if (addr >= HR_TOTAL) { sendException(c, mbap, fc, MB_EX_ILLEGAL_ADDRESS); return; }

      holding[addr] = val;
      dirty = true;

      // Echo request as response
      uint8_t out[12];
      memcpy(out, mbap, 7);
      out[4] = 0x00;
      out[5] = 0x06;
      out[7] = fc;
      out[8] = (addr >> 8) & 0xFF;
      out[9] = addr & 0xFF;
      out[10] = (val >> 8) & 0xFF;
      out[11] = val & 0xFF;
      c.write(out, 12);
      break;
    }

    case 0x10: { // Write Multiple Registers
      if (pduLen < 6) { sendException(c, mbap, fc, MB_EX_ILLEGAL_VALUE); return; }
      uint16_t addr = (pdu[1] << 8) | pdu[2];
      uint16_t qty  = (pdu[3] << 8) | pdu[4];
      uint8_t  bc   = pdu[5];
      if (qty < 1 || qty > 123 || bc != 2 * qty || pduLen != (uint16_t)(6 + bc)) {
        sendException(c, mbap, fc, MB_EX_ILLEGAL_VALUE); return;
      }
      if ((uint32_t)addr + qty > HR_TOTAL) { sendException(c, mbap, fc, MB_EX_ILLEGAL_ADDRESS); return; }

      for (uint16_t i = 0; i < qty; i++) {
        uint16_t v = (pdu[6 + 2 * i] << 8) | pdu[7 + 2 * i];
        holding[addr + i] = v;
      }
      dirty = true;

      uint8_t out[12];
      memcpy(out, mbap, 7);
      out[4] = 0x00;
      out[5] = 0x06;
      out[7] = fc;
      out[8] = (addr >> 8) & 0xFF;
      out[9] = addr & 0xFF;
      out[10] = (qty >> 8) & 0xFF;
      out[11] = qty & 0xFF;
      c.write(out, 12);
      break;
    }

    default:
      sendException(c, mbap, fc, MB_EX_ILLEGAL_FUNCTION);
      return;
  }

  if (dirty) applyStrip();
}

static void serviceModbus() {
  // Accept a new client if one is waiting; replace any stale one.
  WiFiClient incoming = modbusServer.available();
  if (incoming) {
    if (modbusClient && modbusClient.connected()) {
      modbusClient.stop();
    }
    modbusClient = incoming;
    Serial.print("Modbus client connected: ");
    Serial.println(modbusClient.remoteIP());
  }

  if (modbusClient && modbusClient.connected()) {
    // Process all pending complete requests.
    while (modbusClient.connected() && modbusClient.available() >= 7) {
      handleRequest(modbusClient);
    }
  } else if (modbusClient) {
    modbusClient.stop();
  }
}

// ---------- Serial console ----------
//
// Line-based commands, terminated by LF or CR. Echoes back results so a host
// script can wait for the next prompt.
//
//   help                 list commands
//   show                 show stored ssid (password masked) and wifi status
//   ssid <value>         store new SSID (does not reconnect; run 'connect')
//   pass <value>         store new password (does not reconnect; run 'connect')
//   connect              save creds, drop current link, attempt connect
//   disconnect           drop the current link
//   clear                erase stored credentials
//   reboot               restart the ESP32

static String serialBuf;

static void startModbus() {
  modbusServer.begin();
  modbusServer.setNoDelay(true);
}

static void printPrompt() {
  Serial.print("> ");
}

static void cmdShow() {
  Serial.print("ssid: ");
  Serial.println(wifiSsid.length() ? wifiSsid : String("(unset)"));
  Serial.print("pass: ");
  Serial.println(wifiPass.length() ? String("(set, ") + wifiPass.length() + " chars)" : String("(unset)"));
  Serial.print("wifi: ");
  Serial.println(wifiStatusToString(WiFi.status()));
  if (WiFi.status() == WL_CONNECTED) {
    Serial.print("ip:   ");
    Serial.println(WiFi.localIP());
  }
}

static void cmdHelp() {
  Serial.println(F("commands:"));
  Serial.println(F("  help"));
  Serial.println(F("  show"));
  Serial.println(F("  ssid <value>"));
  Serial.println(F("  pass <value>"));
  Serial.println(F("  connect"));
  Serial.println(F("  disconnect"));
  Serial.println(F("  clear"));
  Serial.println(F("  reboot"));
}

static void handleLine(String line) {
  line.trim();
  if (line.length() == 0) { printPrompt(); return; }

  int sp = line.indexOf(' ');
  String cmd = sp < 0 ? line : line.substring(0, sp);
  String arg = sp < 0 ? String("") : line.substring(sp + 1);
  arg.trim();
  cmd.toLowerCase();

  if (cmd == "help") {
    cmdHelp();
  } else if (cmd == "show") {
    cmdShow();
  } else if (cmd == "ssid") {
    if (arg.length() == 0) { Serial.println("usage: ssid <value>"); }
    else { wifiSsid = arg; saveCredentials(); Serial.println("ssid saved."); }
  } else if (cmd == "pass") {
    if (arg.length() == 0) { Serial.println("usage: pass <value>"); }
    else { wifiPass = arg; saveCredentials(); Serial.println("pass saved."); }
  } else if (cmd == "connect") {
    WiFi.disconnect();
    delay(100);
    if (connectWiFi()) startModbus();
  } else if (cmd == "disconnect") {
    WiFi.disconnect();
    Serial.println("disconnected.");
  } else if (cmd == "clear") {
    clearCredentials();
    WiFi.disconnect();
    Serial.println("credentials cleared.");
  } else if (cmd == "reboot") {
    Serial.println("rebooting...");
    Serial.flush();
    ESP.restart();
  } else {
    Serial.print("unknown command: ");
    Serial.println(cmd);
    Serial.println("type 'help'");
  }
  printPrompt();
}

static void serviceSerial() {
  while (Serial.available() > 0) {
    char c = (char)Serial.read();
    if (c == '\r' || c == '\n') {
      if (serialBuf.length() > 0) {
        String line = serialBuf;
        serialBuf = "";
        handleLine(line);
      }
    } else if (serialBuf.length() < 200) {
      serialBuf += c;
    }
  }
}

// ---------- Setup / loop ----------
void setup() {
  Serial.begin(115200);
  delay(200);
  Serial.println();
  Serial.println("=== ESP32 Modbus TCP LED controller ===");

  FastLED.addLeds<CHIPSET, DATA_PIN, COLOR_ORDER>(leds, NUM_LEDS);
  FastLED.setBrightness(0);
  FastLED.clear(true);

  // Sane defaults: off, mid brightness, all black.
  holding[HR_BRIGHTNESS] = 32;
  holding[HR_ENABLE]     = 0;

  loadCredentials();

  if (connectWiFi()) {
    startModbus();
  } else {
    Serial.println("Type 'help' for serial config commands.");
  }
  printPrompt();
}

void loop() {
  static unsigned long lastPrint = 0;
  static unsigned long lastReconnect = 0;
  unsigned long now = millis();

  serviceSerial();

  if (WiFi.status() != WL_CONNECTED) {
    if (wifiSsid.length() > 0 && now - lastReconnect >= 10000) {
      lastReconnect = now;
      Serial.println("WiFi dropped, reconnecting...");
      WiFi.disconnect();
      WiFi.begin(wifiSsid.c_str(), wifiPass.c_str());
      delay(2000);
      if (WiFi.status() == WL_CONNECTED) {
        startModbus();
      }
    }
    return;
  }

  serviceModbus();

  if (now - lastPrint >= STATUS_INTERVAL_MS) {
    lastPrint = now;
    Serial.print("[");
    Serial.print(now / 1000);
    Serial.print("s] ip=");
    Serial.print(WiFi.localIP());
    Serial.print(" rssi=");
    Serial.print(WiFi.RSSI());
    Serial.print("dBm enable=");
    Serial.print(holding[HR_ENABLE]);
    Serial.print(" bright=");
    Serial.print(holding[HR_BRIGHTNESS]);
    Serial.print(" client=");
    Serial.println(modbusClient && modbusClient.connected() ? "yes" : "no");
  }
}
