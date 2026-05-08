// Minimal ESP32 WiFi connectivity test.
// Connects to the configured AP, prints status to serial, and stays online so
// you can verify reachability with `ping <ip>` from another machine on the LAN.
// ICMP echo replies are handled by the ESP32 lwIP stack automatically.

#include <WiFi.h>

const char* WIFI_SSID = "YOUR_SSID";
const char* WIFI_PASS = "YOUR_PASSWORD";

const char* HOSTNAME = "esp32-decklights";

const unsigned long CONNECT_TIMEOUT_MS = 30000;
const unsigned long STATUS_INTERVAL_MS = 5000;

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

static void printConnectionInfo() {
  Serial.print("SSID:     "); Serial.println(WiFi.SSID());
  Serial.print("BSSID:    "); Serial.println(WiFi.BSSIDstr());
  Serial.print("Channel:  "); Serial.println(WiFi.channel());
  Serial.print("IP:       "); Serial.println(WiFi.localIP());
  Serial.print("Gateway:  "); Serial.println(WiFi.gatewayIP());
  Serial.print("Subnet:   "); Serial.println(WiFi.subnetMask());
  Serial.print("DNS:      "); Serial.println(WiFi.dnsIP());
  Serial.print("MAC:      "); Serial.println(WiFi.macAddress());
  Serial.print("RSSI:     "); Serial.print(WiFi.RSSI()); Serial.println(" dBm");
  Serial.print("Hostname: "); Serial.println(WiFi.getHostname());
}

void setup() {
  Serial.begin(115200);
  delay(200);
  Serial.println();
  Serial.println("=== ESP32 WiFi connectivity test ===");

  WiFi.mode(WIFI_STA);
  WiFi.setHostname(HOSTNAME);
  WiFi.begin(WIFI_SSID, WIFI_PASS);

  Serial.print("Connecting to ");
  Serial.print(WIFI_SSID);

  unsigned long start = millis();
  while (WiFi.status() != WL_CONNECTED &&
         millis() - start < CONNECT_TIMEOUT_MS) {
    delay(500);
    Serial.print('.');
  }
  Serial.println();

  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("WiFi connected.");
    printConnectionInfo();
    Serial.println();
    Serial.print("Try: ping ");
    Serial.println(WiFi.localIP());
  } else {
    Serial.print("WiFi NOT connected. status=");
    Serial.println(wifiStatusToString(WiFi.status()));
    Serial.println("Check SSID/password, 2.4 GHz availability, and signal strength.");
  }
}

void loop() {
  static unsigned long lastPrint = 0;
  unsigned long now = millis();

  if (now - lastPrint >= STATUS_INTERVAL_MS) {
    lastPrint = now;
    wl_status_t s = WiFi.status();
    Serial.print("[");
    Serial.print(now / 1000);
    Serial.print("s] status=");
    Serial.print(wifiStatusToString(s));
    if (s == WL_CONNECTED) {
      Serial.print(" ip=");
      Serial.print(WiFi.localIP());
      Serial.print(" rssi=");
      Serial.print(WiFi.RSSI());
      Serial.print("dBm");
    }
    Serial.println();
  }

  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("Reconnecting...");
    WiFi.disconnect();
    WiFi.begin(WIFI_SSID, WIFI_PASS);
    delay(2000);
  }
}
